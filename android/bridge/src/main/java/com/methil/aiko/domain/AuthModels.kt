package com.methil.aiko.domain

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val token: String
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val name: String,
    val password: String
)
