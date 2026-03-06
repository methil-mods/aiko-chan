package com.methil.aiko.service.inmemory

import android.util.Log
import com.methil.aiko.data.AikoConfig
import com.methil.aiko.domain.Message
import com.methil.aiko.domain.TokenResponse
import com.methil.aiko.service.MessageService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class InMemoryMessageService : MessageService {
    private val baseUrl = AikoConfig.BASE_URL
    private val defaultModel = AikoConfig.DEFAULT_MODEL
    
    private val messages = mutableListOf<Message>()
    
    // Cache the promptId in memory
    private val promptId: String = UUID.randomUUID().toString()
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(0, TimeUnit.MILLISECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    override fun getMessages(): List<Message> = messages.toList()

    override fun addMessage(message: Message) {
        messages.add(message)
    }

    override fun streamChat(message: String): Flow<TokenResponse> {
        val urlBuilder = baseUrl.toHttpUrlOrNull()?.newBuilder()?.addPathSegment("chat")?.addPathSegment("stream")
            ?: throw IllegalArgumentException("Invalid URL: $baseUrl")
        
        urlBuilder.addQueryParameter("promptId", promptId)
        urlBuilder.addQueryParameter("message", message)
        urlBuilder.addQueryParameter("model", defaultModel)

        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("Accept", "text/event-stream")
            .build()

        Log.d("AikoSSE", "Starting stream request: ${request.url}")

        return callbackFlow {
            val eventSourceListener = object : EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: Response) {
                    Log.d("AikoSSE", "Connection opened. HTTP Status: ${response.code}")
                }

                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    Log.d("AikoSSE", "Event received - Type: $type, Raw Data: $data")
                    if (type == "token" || type == null) {
                        try {
                            val response = json.decodeFromString<TokenResponse>(data)
                            Log.d("AikoSSE", "Parsed Token: '${response.token}', Done: ${response.done}")
                            trySend(response)
                            if (response.done) {
                                Log.d("AikoSSE", "Stream ended (done=true). Closing event source.")
                                eventSource.cancel()
                                channel.close()
                            }
                        } catch (e: Exception) {
                            Log.e("AikoSSE", "Failed to parse JSON: $data", e)
                        }
                    }
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                    Log.e("AikoSSE", "SSE Failure. Throwable: ${t?.message}, Response: ${response?.code}", t)
                    close(t ?: Exception("SSE Failure"))
                }

                override fun onClosed(eventSource: EventSource) {
                    Log.d("AikoSSE", "SSE Connection closed.")
                    channel.close()
                }
            }

            val eventSource = EventSources.createFactory(client).newEventSource(request, eventSourceListener)

            awaitClose {
                Log.d("AikoSSE", "Flow collection cancelled, cancelling EventSource.")
                eventSource.cancel()
            }
        }
    }
}
