package handlers

import (
	"backend/internal/database"
	"backend/internal/models"
	"encoding/json"
	"net/http"
)

// GetCharacters retourne la liste des personnages disponibles
func GetCharacters(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		sendError(w, "Méthode non autorisée", http.StatusMethodNotAllowed)
		return
	}

	var characters []models.Character
	if err := database.DB.Find(&characters).Error; err != nil {
		sendError(w, "Erreur lors de la récupération des personnages", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(characters)
}
