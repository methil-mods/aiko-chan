package handlers

import (
	"backend/internal/auth"
	"backend/internal/database"
	"backend/internal/models"
	"encoding/json"
	"net/http"

	"golang.org/x/crypto/bcrypt"
)

type AuthRequest struct {
	Username string `json:"username"`
	Name     string `json:"name"`
	Password string `json:"password"`
}

type ErrorResponse struct {
	Error string `json:"error"`
}

func sendError(w http.ResponseWriter, message string, code int) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	json.NewEncoder(w).Encode(ErrorResponse{Error: message})
}

// Register gère l'inscription d'un nouvel utilisateur
func Register(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		sendError(w, "Méthode non autorisée", http.StatusMethodNotAllowed)
		return
	}

	var req AuthRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendError(w, "Requête JSON invalide", http.StatusBadRequest)
		return
	}

	if req.Username == "" || req.Password == "" || req.Name == "" {
		sendError(w, "Pseudo, nom d'utilisateur et mot de passe requis", http.StatusBadRequest)
		return
	}

	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		sendError(w, "Erreur interne de hachage", http.StatusInternalServerError)
		return
	}

	user := models.User{
		Username: req.Username,
		Name:     req.Name,
		Password: string(hashedPassword),
	}

	if err := database.DB.Create(&user).Error; err != nil {
		// Vérification spécifique pour SQLite unique constraint
		if err.Error() == "UNIQUE constraint failed: users.username" {
			sendError(w, "Ce nom d'utilisateur est déjà utilisé", http.StatusConflict)
		} else {
			sendError(w, "Erreur lors de la création de l'utilisateur", http.StatusInternalServerError)
		}
		return
	}

	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]string{"message": "Utilisateur créé avec succès"})
}

// Login gère l'authentification et retourne un JWT
func Login(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		sendError(w, "Méthode non autorisée", http.StatusMethodNotAllowed)
		return
	}

	var req AuthRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendError(w, "Requête JSON invalide", http.StatusBadRequest)
		return
	}

	var user models.User
	if err := database.DB.Where("username = ?", req.Username).First(&user).Error; err != nil {
		sendError(w, "Ce nom d'utilisateur n'existe pas", http.StatusUnauthorized)
		return
	}

	if err := bcrypt.CompareHashAndPassword([]byte(user.Password), []byte(req.Password)); err != nil {
		sendError(w, "Mot de passe incorrect", http.StatusUnauthorized)
		return
	}

	token, err := auth.GenerateJWT(user.Username, user.Name, user.ID)
	if err != nil {
		sendError(w, "Erreur de génération de session", http.StatusInternalServerError)
		return
	}

	json.NewEncoder(w).Encode(map[string]string{"token": token})
}

// GetProfile retourne les informations de l'utilisateur connecté
func GetProfile(w http.ResponseWriter, r *http.Request) {
	claims, ok := r.Context().Value(auth.UserContextKey).(*auth.Claims)
	if !ok {
		sendError(w, "Utilisateur non trouvé dans le contexte", http.StatusInternalServerError)
		return
	}

	var user models.User
	if err := database.DB.First(&user, claims.UserID).Error; err != nil {
		sendError(w, "Utilisateur non trouvé dans la base", http.StatusNotFound)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(user)
}
