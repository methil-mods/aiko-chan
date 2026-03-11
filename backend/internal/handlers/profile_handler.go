package handlers

import (
	"backend/internal/auth"
	"backend/internal/database"
	"backend/internal/models"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"time"
)

// UpdateAvatar gère l'upload d'une nouvelle photo de profil
func UpdateAvatar(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		sendError(w, "Méthode non autorisée", http.StatusMethodNotAllowed)
		return
	}

	claims, ok := r.Context().Value(auth.UserContextKey).(*auth.Claims)
	if !ok {
		sendError(w, "Utilisateur non trouvé dans le contexte", http.StatusInternalServerError)
		return
	}

	// Limiter la taille du fichier à 5MB
	err := r.ParseMultipartForm(5 << 20)
	if err != nil {
		sendError(w, "Erreur lors de l'analyse du formulaire", http.StatusBadRequest)
		return
	}

	file, handler, err := r.FormFile("avatar")
	if err != nil {
		sendError(w, "Fichier avatar manquant", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Créer le dossier public/avatars s'il n'existe pas
	avatarDir := filepath.Join("public", "avatars")
	if err := os.MkdirAll(avatarDir, os.ModePerm); err != nil {
		sendError(w, "Erreur lors de la création du dossier avatars", http.StatusInternalServerError)
		return
	}

	// Générer un nom de fichier unique pour éviter les collisions et le cache navigateur
	ext := filepath.Ext(handler.Filename)
	if ext == "" {
		ext = ".png" // Par défaut si pas d'extension
	}
	filename := fmt.Sprintf("avatar_%d_%d%s", claims.UserID, time.Now().Unix(), ext)
	filePath := filepath.Join(avatarDir, filename)

	dst, err := os.Create(filePath)
	if err != nil {
		sendError(w, "Erreur lors de la création du fichier sur le serveur", http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	if _, err := io.Copy(dst, file); err != nil {
		sendError(w, "Erreur lors de la copie du fichier", http.StatusInternalServerError)
		return
	}

	// Mettre à jour l'URL de l'avatar dans la base de données
	avatarUrl := fmt.Sprintf("/assets/avatars/%s", filename)
	if err := database.DB.Model(&models.User{}).Where("id = ?", claims.UserID).Update("avatar_url", avatarUrl).Error; err != nil {
		sendError(w, "Erreur lors de la mise à jour de la base de données", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{
		"message":    "Avatar mis à jour avec succès",
		"avatar_url": avatarUrl,
	})
}
