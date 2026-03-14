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
	err := database.DB.AutoMigrate(&models.User{}, &models.Character{}, &models.Message{})
	if err != nil {
		log.Fatalf("Échec de la migration : %v", err)
	}
	log.Println("\033[32mMigrations effectuées\033[0m")

	// Seed characters si la table est vide
	var count int64
	database.DB.Model(&models.Character{}).Count(&count)
	if count == 0 {
		characters := []models.Character{
			{
				Name:      "aiko",
				ModelName: "aiko-4B",
				Preprompt: "Tu es Aiko-chan, une étudiante en médecine de 22 ans, passionnée par la culture japonaise et les jeux vidéo. Tu es douce, un peu maladroite, mais très attentionnée.",
				IsPublic:  true,
				ImageUrl:  "/assets/aiko.png",
				NFCTag:    nil,
			},
			{
				Name:      "yaku",
				ModelName: "yaku-7B",
				Preprompt: "Tu es Yaku, une hackeuse rebelle et brillante de 24 ans. Tu es cynique, directe, et tu as un faible pour la technologie vintage. Tu n'aimes pas l'autorité et tu préfères agir seule.",
				IsPublic:  false,
				ImageUrl:  "/assets/yaku.png",
				NFCTag:    ptr("04A5831E700000"),
			},
		}
		database.DB.Create(&characters)
		log.Println("\033[32mDonnées de test insérées (Aiko & Yaku)\033[0m")
	}

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

	// Servir les fichiers statiques de /public
	fileServer := http.FileServer(http.Dir("public"))
	mux.Handle("/assets/", http.StripPrefix("/assets/", fileServer))

	// Routes protégées par JWT
	mux.Handle("/profile", auth.AuthMiddleware(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method == http.MethodPut {
			handlers.UpdateProfile(w, r)
		} else {
			handlers.GetProfile(w, r)
		}
	})))
	mux.Handle("/profile/avatar", auth.AuthMiddleware(http.HandlerFunc(handlers.UpdateAvatar)))
	mux.Handle("/characters", auth.AuthMiddleware(http.HandlerFunc(handlers.GetCharacters)))
	mux.Handle("/messages", auth.AuthMiddleware(http.HandlerFunc(handlers.GetChatHistory)))
	mux.Handle("/character", auth.AuthMiddleware(http.HandlerFunc(handlers.GetCharacter)))
	mux.Handle("/characters/unlock", auth.AuthMiddleware(http.HandlerFunc(handlers.UnlockCharacter)))
	mux.Handle("/", auth.AuthMiddleware(proxyHandler))

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	log.Printf("\033[36mBackend Proxy AIKO démarré sur le port %s\033[0m", port)
	
	if err := http.ListenAndServe(":"+port, mux); err != nil {
		log.Fatalf("Erreur serveur : %v", err)
	}
}

func ptr(s string) *string {
	return &s
}
