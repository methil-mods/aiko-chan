package com.methil.aiko.domain

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: Long,
    val username: String,
    val name: String
)

@Serializable
data class Character(
    val id: Long,
    val name: String,
    val model_name: String,
    val preprompt: String,
    val is_public: Boolean = false,
    val image_url: String? = null
)
