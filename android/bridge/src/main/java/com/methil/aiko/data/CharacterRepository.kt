package com.methil.aiko.data

import com.methil.aiko.bridge.AikoConfig
import com.methil.aiko.domain.Character
import com.methil.aiko.service.AuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.util.Log

class CharacterRepository(
    private val baseUrl: String,
    private val authService: AuthService
) {
    private val client = OkHttpClient()

    suspend fun getCharacters(token: String): Result<List<Character>> {
        return authService.getCharacters(token)
    }

    suspend fun unlockCharacter(token: String, nfcTag: String): Result<UnlockResult> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = """{"nfc_tag": "$nfcTag"}"""
            val body = jsonBody.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/characters/unlock")
                .post(body)
                .header("Authorization", "Bearer $token")
                .build()
            
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val json = JSONObject(responseBody)
                    Result.success(UnlockResult(
                        characterName = json.optString("character_name", "???"),
                        isNew = json.optBoolean("is_new", false)
                    ))
                } else if (response.code == 404) {
                    Result.failure(Exception("Tag inconnu"))
                } else {
                    Result.failure(Exception("Erreur de déverrouillage: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e("CharacterRepository", "Unlock error", e)
            Result.failure(e)
        }
    }
}

data class UnlockResult(
    val characterName: String,
    val isNew: Boolean
)
