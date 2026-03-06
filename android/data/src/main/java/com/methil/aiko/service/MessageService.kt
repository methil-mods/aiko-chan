package com.methil.aiko.service

import com.methil.aiko.domain.Message
import com.methil.aiko.domain.TokenResponse
import kotlinx.coroutines.flow.Flow

interface MessageService {
    fun getMessages(): List<Message>
    fun addMessage(message: Message)
    fun streamChat(message: String): Flow<TokenResponse>
}