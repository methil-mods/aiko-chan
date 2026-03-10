package com.methil.aiko.service

import com.methil.aiko.domain.AuthResponse
import com.methil.aiko.domain.LoginRequest
import com.methil.aiko.domain.RegisterRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class AuthService(private val baseUrl: String) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        val body = json.encodeToString(request).toRequestBody(mediaType)
        val httpRequest = Request.Builder()
            .url("$baseUrl/auth/login")
            .post(body)
            .build()

        return executeRequest(httpRequest)
    }

    suspend fun register(request: RegisterRequest): Result<String> {
        val body = json.encodeToString(request).toRequestBody(mediaType)
        val httpRequest = Request.Builder()
            .url("$baseUrl/auth/register")
            .post(body)
            .build()

        return try {
            client.newCall(httpRequest).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("User created")
                } else {
                    Result.failure(Exception("Registration failed: ${response.code}"))
                }
            }
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    private fun executeRequest(httpRequest: Request): Result<AuthResponse> {
        return try {
            client.newCall(httpRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: return Result.failure(Exception("Empty body"))
                    val authResponse = json.decodeFromString<AuthResponse>(bodyString)
                    Result.success(authResponse)
                } else {
                    Result.failure(Exception("Auth failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
