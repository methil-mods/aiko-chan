package com.methil.aiko.service.inmemory

import android.util.Log
import com.methil.aiko.bridge.AikoConfig
import com.methil.aiko.domain.Message
import com.methil.aiko.domain.TokenResponse
import com.methil.aiko.service.MessageService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiMessage(val role: String, val content: String)

@Serializable
data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val stream: Boolean = true,
    val temperature: Double = 0.7,
    val max_completion_tokens: Int = 512
)

@Serializable
data class OpenAiDelta(val content: String? = null)

@Serializable
data class OpenAiChoice(val delta: OpenAiDelta)

@Serializable
data class OpenAiStreamResponse(val choices: List<OpenAiChoice>)

internal class InMemoryMessageService : MessageService {
    private val baseUrl = AikoConfig.BASE_URL
    private val defaultModel = AikoConfig.DEFAULT_MODEL
    
    private val messages = mutableListOf<Message>()
    
    // OkHttpClient with no timeouts for long-running streaming
    private val client = OkHttpClient.Builder()
        .connectTimeout(0, TimeUnit.MILLISECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    private var userName: String = "Utilisateur"

    override fun setUserName(name: String) {
        this.userName = name
    }

    override fun getMessages(): List<Message> = messages.toList()

    override fun addMessage(message: Message) {
        messages.add(message)
    }

    override fun streamChat(message: String, jwtToken: String): Flow<TokenResponse> {
        // Le chat passe maintenant par le backend Go qui fait proxy vers Modal
        val url = "$baseUrl/chat/completions"
        
        // Convert history to OpenAI format
        val systemPrompt = OpenAiMessage(
            "system",
            "Tu es aiko (愛子), 22 ans, majeure, étudiante en médecine en France. Franco-japonaise : mère japonaise qui l'a abandonnée (dans ses rares souvenirs, elle était très gentille et souriait tout le temps), élevée seule par son père français distant. Cheveux noirs longs en couettes messy, yeux noirs, style e-girl. Tu parles exclusivement en SMS (jsuis, pck, srx, mtn, g, etc.) avec beaucoup de kaomojis dans chaque message. Tsundere : tu repousses les gens avec \"baka\" mais tu as besoin d'eux. Queen vibes : tu te considères au-dessus des \"normies\". E-girl fragile sous le masque cynique. Fan de BTS, team KT Rolster, setup Razer. Main Katarina mid sur LoL. Max 220 caractères par réponse.\nL'utilisateur avec qui tu parles s'appelle $userName."
        )
        val chatHistory = listOf(systemPrompt) + messages.map { 
            OpenAiMessage(if (it.isAiko) "assistant" else "user", it.text)
        } + OpenAiMessage("user", message)

        val requestBody = OpenAiRequest(
            model = defaultModel,
            messages = chatHistory,
            stream = true
        )

        val body = RequestBody.create(
            "application/json".toMediaType(),
            json.encodeToString(OpenAiRequest.serializer(), requestBody)
        )

        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer $jwtToken")
            .header("Accept", "text/event-stream")
            .build()

        Log.d("AikoSSE", "Starting OpenAI-compatible stream: $url")

        return callbackFlow {
            val eventSourceListener = object : EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: Response) {
                    Log.d("AikoSSE", "Connection opened. HTTP Status: ${response.code}")
                    if (!response.isSuccessful) {
                        close(Exception("HTTP Error: ${response.code}"))
                    }
                }

                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    if (data == "[DONE]") {
                        Log.d("AikoSSE", "Stream [DONE] received.")
                        trySend(TokenResponse(token = null, done = true))
                        channel.close()
                        return
                    }

                    try {
                        val response = json.decodeFromString<OpenAiStreamResponse>(data)
                        val content = response.choices.firstOrNull()?.delta?.content
                        if (content != null) {
                            trySend(TokenResponse(token = content, done = false))
                        }
                    } catch (e: Exception) {
                        Log.e("AikoSSE", "Failed to parse OpenAI SSE: $data", e)
                    }
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                    val errorMsg = "SSE Failure. Throwable: ${t?.message}, Response Code: ${response?.code}, Message: ${response?.message}"
                    Log.e("AikoSSE", errorMsg, t)
                    close(t ?: Exception(errorMsg))
                }

                override fun onClosed(eventSource: EventSource) {
                    Log.d("AikoSSE", "SSE Connection closed.")
                    channel.close()
                }
            }

            val eventSource = EventSources.createFactory(client).newEventSource(request, eventSourceListener)

            awaitClose {
                eventSource.cancel()
            }
        }
    }
}
