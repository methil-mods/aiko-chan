package com.methil.aiko.data

import com.methil.aiko.domain.AuthResponse
import com.methil.aiko.domain.LoginRequest
import com.methil.aiko.domain.RegisterRequest
import com.methil.aiko.domain.UserProfile
import com.methil.aiko.service.AuthService

class AuthRepository(
    private val authService: AuthService,
    private val tokenManager: TokenManager
) {
    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        val result = authService.login(request)
        result.onSuccess {
            tokenManager.saveToken(it.token)
        }
        return result
    }

    suspend fun register(request: RegisterRequest): Result<String> {
        return authService.register(request)
    }

    suspend fun getProfile(token: String): Result<UserProfile> {
        return authService.getProfile(token)
    }

    suspend fun updateProfile(token: String, name: String, age: Int): Result<UserProfile> {
        return authService.updateProfile(token, name, age)
    }

    suspend fun uploadAvatar(token: String, fileName: String, fileBytes: ByteArray): Result<String> {
        return authService.uploadAvatar(token, fileName, fileBytes)
    }

    fun getToken(): String? {
        return tokenManager.getToken()
    }

    fun logout() {
        tokenManager.clearToken()
    }
}
