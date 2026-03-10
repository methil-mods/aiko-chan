package auth

import (
	"os"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

var jwtKey = []byte(os.Getenv("JWT_SECRET_KEY"))

// Claims définit les données contenues dans le token JWT
type Claims struct {
	Username string `json:"username"`
	Name     string `json:"name"`
	UserID   uint   `json:"user_id"`
	jwt.RegisteredClaims
}

// GenerateJWT crée un nouveau token pour un utilisateur
func GenerateJWT(username string, name string, userID uint) (string, error) {
	if len(jwtKey) == 0 {
		jwtKey = []byte("default_secret_key_change_me") // Fallback pour le dev
	}

	expirationTime := time.Now().Add(24 * time.Hour)
	claims := &Claims{
		Username: username,
		Name:     name,
		UserID:   userID,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(expirationTime),
		},
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString(jwtKey)
}

// ValidateJWT vérifie la validité d'un token et retourne les claims
func ValidateJWT(tokenStr string) (*Claims, error) {
	if len(jwtKey) == 0 {
		jwtKey = []byte("default_secret_key_change_me")
	}

	claims := &Claims{}
	token, err := jwt.ParseWithClaims(tokenStr, claims, func(token *jwt.Token) (interface{}, error) {
		return jwtKey, nil
	})

	if err != nil || !token.Valid {
		return nil, err
	}

	return claims, nil
}
