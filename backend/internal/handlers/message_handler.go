package handlers

import (
	"backend/internal/auth"
	"backend/internal/database"
	"backend/internal/models"
	"encoding/json"
	"net/http"
	"strconv"
)

// GetChatHistory renvoie l'historique des messages pour un personnage donné
func GetChatHistory(w http.ResponseWriter, r *http.Request) {
	claims, ok := r.Context().Value(auth.UserContextKey).(*auth.Claims)
	if !ok {
		sendError(w, "Non autorisé", http.StatusUnauthorized)
		return
	}

	characterIDStr := r.URL.Query().Get("character_id")
	if characterIDStr == "" {
		sendError(w, "ID du personnage requis", http.StatusBadRequest)
		return
	}

	characterID, err := strconv.ParseUint(characterIDStr, 10, 32)
	if err != nil {
		sendError(w, "ID du personnage invalide", http.StatusBadRequest)
		return
	}

	var messages []models.Message
	result := database.DB.Where("user_id = ? AND character_id = ?", claims.UserID, uint(characterID)).
		Order("created_at asc").
		Find(&messages)

	if result.Error != nil {
		sendError(w, "Erreur lors de la récupération de l'historique", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(messages)
}

// SaveMessage est une fonction utilitaire interne pour sauvegarder un message
func SaveMessage(userID uint, characterID uint, content string, role string) error {
	message := models.Message{
		UserID:      userID,
		CharacterID: characterID,
		Content:     content,
		Role:        role,
	}
	return database.DB.Create(&message).Error
}
