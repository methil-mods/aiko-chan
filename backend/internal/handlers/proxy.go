package handlers

import (
	"backend/internal/auth"
	"backend/internal/database"
	"bytes"
	"context"
	"encoding/json"
	"io"
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"os"
	"strings"
)

// ProxyHandler gère la redirection des requêtes vers le backend Modal
type ProxyHandler struct {
	Proxy *httputil.ReverseProxy
	Target *url.URL
}

// NewProxyHandler initialise un nouveau handler de proxy
func NewProxyHandler() (*ProxyHandler, error) {
	modalURLStr := os.Getenv("MODAL_URL")
	if modalURLStr == "" {
		modalURLStr = "https://your-modal-app-url.modal.run"
	}

	target, err := url.Parse(modalURLStr)
	if err != nil {
		return nil, err
	}

	apiKey := os.Getenv("AIKO_API_KEY")

	proxy := httputil.NewSingleHostReverseProxy(target)
	
	originalDirector := proxy.Director
	proxy.Director = func(req *http.Request) {
		originalDirector(req)
		req.Host = target.Host
		if apiKey != "" {
			req.Header.Set("Authorization", "Bearer "+apiKey)
		}
		log.Printf("[Proxy] %s %s -> %s", req.Method, req.URL.Path, target.Host)
	}

	proxy.ModifyResponse = func(resp *http.Response) error {
		if resp.StatusCode == http.StatusOK && (resp.Request.URL.Path == "/chat/completions" || resp.Request.URL.Path == "/") {
			// On récupère les infos stockées dans le contexte (UserID et CharacterID)
			userID, _ := resp.Request.Context().Value("userID").(uint)
			characterID, _ := resp.Request.Context().Value("characterID").(uint)

			if userID != 0 && characterID != 0 {
				resp.Body = &captureReadCloser{
					ReadCloser:  resp.Body,
					userID:      userID,
					characterID: characterID,
				}
			}
		}
		return nil
	}

	return &ProxyHandler{
		Proxy:  proxy,
		Target: target,
	}, nil
}

func (h *ProxyHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	// Vérifier la possession du personnage si c'est une requête de chat
	if r.URL.Path == "/chat/completions" || r.URL.Path == "/" {
		claims, ok := r.Context().Value(auth.UserContextKey).(*auth.Claims)
		if !ok {
			sendError(w, "Non autorisé", http.StatusUnauthorized)
			return
		}

		var body map[string]interface{}
		if r.Body != nil {
			err := json.NewDecoder(r.Body).Decode(&body)
			if err != nil {
				sendError(w, "Requête invalide", http.StatusBadRequest)
				return
			}
			// Restaurer le body pour le proxy
			jsonBody, _ := json.Marshal(body)
			r.Body = io.NopCloser(bytes.NewBuffer(jsonBody))
			r.ContentLength = int64(len(jsonBody))
		}

		requestedModel, _ := body["model"].(string)
		if requestedModel == "" {
			h.Proxy.ServeHTTP(w, r)
			return
		}

		// Vérifier si l'utilisateur possède ce modèle et récupérer le CharacterID
		var character struct {
			ID uint
		}
		err := database.DB.Table("characters").
			Select("characters.id").
			Joins("JOIN user_characters ON user_characters.character_id = characters.id").
			Where("user_characters.user_id = ? AND characters.model_name = ?", claims.UserID, requestedModel).
			First(&character).Error

		if err != nil {
			sendError(w, "Vous ne possédez pas ce personnage", http.StatusForbidden)
			return
		}

		// Sauvegarder le message de l'utilisateur
		messages, ok := body["messages"].([]interface{})
		if ok && len(messages) > 0 {
			lastMsg, ok := messages[len(messages)-1].(map[string]interface{})
			if ok && lastMsg["role"] == "user" {
				content, _ := lastMsg["content"].(string)
				if content != "" {
					SaveMessage(claims.UserID, character.ID, content, "user")
				}
			}
		}

		// Passer l'ID au contexte pour ModifyResponse
		importCtx := r.Context()
		importCtx = context.WithValue(importCtx, "userID", claims.UserID)
		importCtx = context.WithValue(importCtx, "characterID", character.ID)
		r = r.WithContext(importCtx)
	}

	h.Proxy.ServeHTTP(w, r)
}

type captureReadCloser struct {
	io.ReadCloser
	userID      uint
	characterID uint
	buffer      bytes.Buffer
}

func (c *captureReadCloser) Read(p []byte) (n int, err error) {
	n, err = c.ReadCloser.Read(p)
	if n > 0 {
		c.buffer.Write(p[:n])
	}
	if err == io.EOF {
		// Analyser le buffer pour extraire le texte de l'assistant (format OpenAI SSE)
		fullText := ""
		lines := strings.Split(c.buffer.String(), "\n")
		for _, line := range lines {
			if strings.HasPrefix(line, "data: ") {
				data := strings.TrimPrefix(line, "data: ")
				if data == "[DONE]" {
					continue
				}
				var chunk struct {
					Choices []struct {
						Delta struct {
							Content string `json:"content"`
						} `json:"delta"`
					} `json:"choices"`
				}
				if err := json.Unmarshal([]byte(data), &chunk); err == nil {
					if len(chunk.Choices) > 0 {
						fullText += chunk.Choices[0].Delta.Content
					}
				}
			}
		}
		if fullText != "" {
			SaveMessage(c.userID, c.characterID, fullText, "assistant")
		}
	}
	return n, err
}
