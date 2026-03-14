package models

import (
	"gorm.io/gorm"
)

// Message représente un message dans une conversation
type Message struct {
	gorm.Model
	UserID      uint      `gorm:"index" json:"user_id"`
	CharacterID uint      `gorm:"index" json:"character_id"`
	Content     string    `gorm:"not null" json:"content"`
	Role        string    `gorm:"not null" json:"role"` // "user" ou "assistant"
	
	// Relations (Optionnel selon le besoin de JSON)
	User      User      `gorm:"foreignKey:UserID" json:"-"`
	Character Character `gorm:"foreignKey:CharacterID" json:"-"`
}
