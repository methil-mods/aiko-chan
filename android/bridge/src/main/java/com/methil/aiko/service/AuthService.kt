package com.methil.aiko.service

import com.methil.aiko.domain.AuthResponse
import com.methil.aiko.domain.LoginRequest
import com.methil.aiko.domain.RegisterRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import android.util.Log

@Serializable
private data class ErrorResponse(val error: String)

class AuthService(private val baseUrl: String) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private suspend fun parseError(response: okhttp3.Response): String = withContext(Dispatchers.IO) {
        try {
            val bodyString = response.body?.string()
            if (bodyString != null) {
                val errorResponse = json.decodeFromString<ErrorResponse>(bodyString)
                errorResponse.error
            } else {
                "Erreur serveur: ${response.code}"
            }
        } catch (e: Exception) {
            Log.e("AuthService", "Error parsing error response", e)
            "Erreur serveur: ${response.code}"
        }
    }

    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        val body = json.encodeToString(request).toRequestBody(mediaType)
        val httpRequest = Request.Builder()
            .url("$baseUrl/auth/login")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            executeRequest(httpRequest)
        }
    }

    suspend fun register(request: RegisterRequest): Result<String> {
        val body = json.encodeToString(request).toRequestBody(mediaType)
        val httpRequest = Request.Builder()
            .url("$baseUrl/auth/register")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(httpRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        Result.success("User created")
                    } else {
                        val errorMessage = parseError(response)
                        Result.failure(Exception(errorMessage))
                    }
                }
            } catch (e: IOException) {
                Log.e("AuthService", "Register network error", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getProfile(token: String): Result<com.methil.aiko.domain.UserProfile> {
        val httpRequest = Request.Builder()
            .url("$baseUrl/profile")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(httpRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                        val profile = json.decodeFromString<com.methil.aiko.domain.UserProfile>(bodyString)
                        Result.success(profile)
                    } else {
                        val errorMessage = parseError(response)
                        Result.failure(Exception(errorMessage))
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthService", "Network or parsing error", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getCharacters(token: String): Result<List<com.methil.aiko.domain.Character>> {
        val httpRequest = Request.Builder()
            .url("$baseUrl/characters")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(httpRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                        val characters = json.decodeFromString<List<com.methil.aiko.domain.Character>>(bodyString)
                        Result.success(characters)
                    } else {
                        val errorMessage = parseError(response)
                        Result.failure(Exception(errorMessage))
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthService", "Network or parsing error", e)
                Result.failure(e)
            }
        }
    }

    suspend fun updateProfile(token: String, name: String, age: Int): Result<com.methil.aiko.domain.UserProfile> {
        val payload = mapOf("name" to name, "age" to age)
        // Manual JSON for age update to avoid serialization complexity in bridge
        val bodyStr = "{\"name\":\"$name\",\"age\":$age}"
        val body = bodyStr.toRequestBody(mediaType)
        val httpRequest = Request.Builder()
            .url("$baseUrl/profile")
            .header("Authorization", "Bearer $token")
            .put(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(httpRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                        val profile = json.decodeFromString<com.methil.aiko.domain.UserProfile>(bodyString)
                        Result.success(profile)
                    } else {
                        val errorMessage = parseError(response)
                        Result.failure(Exception(errorMessage))
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthService", "Update profile error", e)
                Result.failure(e)
            }
        }
    }

    suspend fun uploadAvatar(token: String, fileName: String, fileBytes: ByteArray): Result<String> {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("avatar", fileName, fileBytes.toRequestBody("image/*".toMediaType()))
            .build()

        val httpRequest = Request.Builder()
            .url("$baseUrl/profile/avatar")
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(httpRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                        val jsonResponse = json.decodeFromString<Map<String, String>>(bodyString)
                        Result.success(jsonResponse["avatar_url"] ?: "")
                    } else {
                        val errorMessage = parseError(response)
                        Result.failure(Exception(errorMessage))
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthService", "Upload avatar error", e)
                Result.failure(e)
            }
        }
    }

    private suspend fun executeRequest(httpRequest: Request): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            client.newCall(httpRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                    val authResponse = json.decodeFromString<AuthResponse>(bodyString)
                    Result.success(authResponse)
                } else {
                    val errorMessage = parseError(response)
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
