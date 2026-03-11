package models

import "gorm.io/gorm"

// Character représente un personnage avec lequel l'utilisateur peut discuter
type Character struct {
	gorm.Model
	Name      string `gorm:"not null" json:"name"`
	ModelName string `gorm:"not null" json:"model_name"`
	Preprompt string `gorm:"not null" json:"preprompt"`
}
