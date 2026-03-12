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
