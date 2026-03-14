package com.methil.aiko.service

import com.methil.aiko.domain.Message
import com.methil.aiko.domain.TokenResponse
import kotlinx.coroutines.flow.Flow

interface MessageService {
    fun getMessages(): List<Message>
    fun addMessage(message: Message)
    fun streamChat(message: String, jwtToken: String): Flow<TokenResponse>
    suspend fun fetchHistory(characterId: Int, jwtToken: String): List<Message>
    suspend fun fetchCharacter(characterId: Int, jwtToken: String): Character?
}