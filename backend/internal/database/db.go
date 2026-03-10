package database

import (
	"log"
	"os"
	"time"

	"gorm.io/driver/sqlite"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

var DB *gorm.DB

// InitDB initialise la connexion à la base de données SQLite
func InitDB() {
	newLogger := logger.New(
		log.New(os.Stdout, "\r\n", log.LstdFlags), // io writer
		logger.Config{
			SlowThreshold:             time.Second,   // Slow SQL threshold
			LogLevel:                  logger.Silent, // Log level
			IgnoreRecordNotFoundError: true,           // Ignore ErrRecordNotFound error for logger
			ParameterizedQueries:      true,           // Don't include params in the SQL log
			Colorful:                  false,          // Disable color
		},
	)

	var err error
	DB, err = gorm.Open(sqlite.Open("aiko.db"), &gorm.Config{
		Logger: newLogger,
	})
	if err != nil {
		log.Fatalf("Échec de la connexion à la base de données : %v", err)
	}

	log.Println("\033[32mBase de données connectée avec succès\033[0m")
}
