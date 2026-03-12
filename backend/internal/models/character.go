package models

import (
	"time"

	"gorm.io/gorm"
)

// Character représente un personnage avec lequel l'utilisateur peut discuter
type Character struct {
	ID        uint           `gorm:"primaryKey" json:"id"`
	CreatedAt time.Time      `json:"-"`
	UpdatedAt time.Time      `json:"-"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`
	Name      string         `gorm:"not null" json:"name"`
	ModelName string         `gorm:"not null" json:"model_name"`
	Preprompt string         `gorm:"not null" json:"preprompt"`
	IsPublic  bool           `gorm:"default:false" json:"is_public"`
	ImageUrl  string         `json:"image_url"`
	NFCTag    *string        `gorm:"index" json:"nfc_tag"`
	Users     []User         `gorm:"many2many:user_characters;" json:"-"`
}
