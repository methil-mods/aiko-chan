package database

import (
	"log"

	"gorm.io/driver/sqlite"
	"gorm.io/gorm"
)

var DB *gorm.DB

// InitDB initialise la connexion à la base de données SQLite
func InitDB() {
	var err error
	DB, err = gorm.Open(sqlite.Open("aiko.db"), &gorm.Config{})
	if err != nil {
		log.Fatalf("Échec de la connexion à la base de données : %v", err)
	}

	log.Println("Base de données connectée avec succès")
}
