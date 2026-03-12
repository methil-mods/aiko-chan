package handlers

import (
	"backend/internal/auth"
	"backend/internal/database"
	"bytes"
	"encoding/json"
	"io"
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"os"
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

		// Pour simplifier, on décode le JSON pour obtenir le model_name
		// Note: Dans une application réelle, on pourrait vouloir Bufferiser le body 
		// pour que le proxy puisse le relire, ou passer le character_id dans l'URL.
		// Ici, on va vérifier si l'utilisateur possède AU MOINS un personnage 
		// dont le model_name correspond à celui demandé.

		// Lecture du body pour inspection
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
			// Si pas de modèle spécifié, on laisse passer ou on bloque selon la politique
			h.Proxy.ServeHTTP(w, r)
			return
		}

		// Vérifier si l'utilisateur possède ce modèle
		var count int64
		database.DB.Table("user_characters").
			Joins("JOIN characters ON characters.id = user_characters.character_id").
			Where("user_characters.user_id = ? AND characters.model_name = ?", claims.UserID, requestedModel).
			Count(&count)

		if count == 0 {
			sendError(w, "Vous ne possédez pas ce personnage", http.StatusForbidden)
			return
		}
	}

	h.Proxy.ServeHTTP(w, r)
}
