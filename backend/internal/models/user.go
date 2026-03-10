package models

import (
	"gorm.io/gorm"
)

// User représente un utilisateur dans la base de données
type User struct {
	gorm.Model
	Username string `gorm:"uniqueIndex;not null" json:"username"`
	Password string `gorm:"not null" json:"-"`
}
