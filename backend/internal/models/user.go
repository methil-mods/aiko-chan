package models

import (
	"time"

	"gorm.io/gorm"
)

// User représente un utilisateur dans la base de données
type User struct {
	ID        uint           `gorm:"primaryKey" json:"id"`
	CreatedAt time.Time      `json:"-"`
	UpdatedAt time.Time      `json:"-"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`
	Username  string         `gorm:"uniqueIndex;not null" json:"username"`
	Name      string         `gorm:"not null" json:"name"`
	Password  string         `gorm:"not null" json:"-"`
	Age       int            `gorm:"default:20" json:"age"`
	AvatarUrl string         `gorm:"default:''" json:"avatar_url"`
	Characters []Character   `gorm:"many2many:user_characters;" json:"characters,omitempty"`
}
