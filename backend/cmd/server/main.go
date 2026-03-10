package main

import (
	"backend/internal/auth"
	"backend/internal/database"
	"backend/internal/handlers"
	"backend/internal/models"
	"log"
	"net/http"
	"os"

	"github.com/joho/godotenv"
)

func main() {
	// Charger le .env si présent
	if err := godotenv.Load(); err != nil {
		log.Println("Note: aucun fichier .env trouvé")
	}

	// Initialisation de la base de données
	database.InitDB()
	
	// Migrations automatiques
	err := database.DB.AutoMigrate(&models.User{})
	if err != nil {
		log.Fatalf("Échec de la migration : %v", err)
	}
	log.Println("Migrations effectuées")

	// Initialisation du handler de proxy
	proxyHandler, err := handlers.NewProxyHandler()
	if err != nil {
		log.Fatalf("Échec de l'initialisation du proxy : %v", err)
	}

	// Configuration des routes
	mux := http.NewServeMux()
	
	// Routes publiques
	mux.HandleFunc("/auth/register", handlers.Register)
	mux.HandleFunc("/auth/login", handlers.Login)
	mux.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("OK"))
	})

	// Routes protégées par JWT
	mux.Handle("/", auth.AuthMiddleware(proxyHandler))

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	log.Printf("Backend Proxy AIKO démarré sur le port %s", port)
	
	if err := http.ListenAndServe(":"+port, mux); err != nil {
		log.Fatalf("Erreur serveur : %v", err)
	}
}
