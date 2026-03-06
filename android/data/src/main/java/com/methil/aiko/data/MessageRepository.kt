package com.methil.aiko.data

import com.methil.aiko.service.MessageService
import com.methil.aiko.service.inmemory.InMemoryMessageService
import com.methil.aiko.domain.Message
import com.methil.aiko.domain.TokenResponse
import kotlinx.coroutines.flow.Flow

class MessageRepository {

    private val messageService: MessageService = InMemoryMessageService()

    fun getMessages(): List<Message> = messageService.getMessages()

    fun addMessage(message: Message) {
        messageService.addMessage(message)
    }

    fun streamChat(message: String): Flow<TokenResponse> {
        return messageService.streamChat(message)
    }
}