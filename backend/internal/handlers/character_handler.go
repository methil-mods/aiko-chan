package handlers

import (
	"backend/internal/auth"
	"backend/internal/database"
	"backend/internal/models"
	"encoding/json"
	"net/http"
)

// GetCharacters retourne la liste des personnages disponibles pour l'utilisateur
func GetCharacters(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		sendError(w, "Méthode non autorisée", http.StatusMethodNotAllowed)
		return
	}

	claims, ok := r.Context().Value(auth.UserContextKey).(*auth.Claims)
	if !ok {
		sendError(w, "Utilisateur non trouvé dans le contexte", http.StatusInternalServerError)
		return
	}

	var user models.User
	if err := database.DB.Preload("Characters").First(&user, claims.UserID).Error; err != nil {
		sendError(w, "Utilisateur non trouvé", http.StatusNotFound)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(user.Characters)
}

// UnlockCharacter débloque un personnage pour l'utilisateur via un tag NFC
func UnlockCharacter(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		sendError(w, "Méthode non autorisée", http.StatusMethodNotAllowed)
		return
	}

	claims, ok := r.Context().Value(auth.UserContextKey).(*auth.Claims)
	if !ok {
		sendError(w, "Utilisateur non trouvé dans le contexte", http.StatusInternalServerError)
		return
	}

	var req struct {
		NFCTag string `json:"nfc_tag"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendError(w, "Requête JSON invalide", http.StatusBadRequest)
		return
	}

	if req.NFCTag == "" {
		sendError(w, "Tag NFC requis", http.StatusBadRequest)
		return
	}

	var character models.Character
	if err := database.DB.Where("nfc_tag = ?", req.NFCTag).First(&character).Error; err != nil {
		sendError(w, "Aucun personnage associé à ce tag", http.StatusNotFound)
		return
	}

	var user models.User
	if err := database.DB.Preload("Characters").First(&user, claims.UserID).Error; err != nil {
		sendError(w, "Utilisateur non trouvé", http.StatusNotFound)
		return
	}

	// Vérifier si le personnage est déjà débloqué
	for _, c := range user.Characters {
		if c.ID == character.ID {
			w.Header().Set("Content-Type", "application/json")
			json.NewEncoder(w).Encode(map[string]interface{}{
				"message":        "Personnage déjà débloqué",
				"character_name": character.Name,
				"is_new":         false,
			})
			return
		}
	}

	// Débloquer le personnage
	if err := database.DB.Model(&user).Association("Characters").Append(&character); err != nil {
		sendError(w, "Erreur lors du déblocage du personnage", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"message":        "Personnage débloqué avec succès !",
		"character_name": character.Name,
		"image_url":      character.ImageUrl,
		"is_new":         true,
	})
}

// GetCharacter retourne un personnage spécifique
func GetCharacter(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		sendError(w, "Méthode non autorisée", http.StatusMethodNotAllowed)
		return
	}

	characterIDStr := r.URL.Query().Get("id")
	if characterIDStr == "" {
		sendError(w, "ID du personnage requis", http.StatusBadRequest)
		return
	}

	var character models.Character
	if err := database.DB.First(&character, characterIDStr).Error; err != nil {
		sendError(w, "Personnage non trouvé", http.StatusNotFound)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(character)
}
