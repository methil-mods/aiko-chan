package com.methil.aiko.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.methil.aiko.domain.Message
import com.methil.aiko.data.MessageRepository
import com.methil.aiko.domain.TokenResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MessageUI(
    val text: String,
    val isAiko: Boolean,
    val isStreaming: Boolean = false,
    val isSearching: Boolean = false
)

data class ChatUiState(
    val messages: List<MessageUI> = emptyList(),
    val inputText: String = "",
    val isKeyboardOpen: Boolean = false,
    val currentStats: TokenResponse? = null,
    val errorMessage: String? = null
)

class MessageViewModel : ViewModel() {
    
    private val repository = MessageRepository()
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadMessages()
    }

    private fun loadMessages() {
        val savedMessages = repository.getMessages().map { 
            MessageUI(it.text, it.isAiko)
        }
        if (savedMessages.isEmpty()) {
            _uiState.update { it.copy(messages = listOf(
                MessageUI("Hello there! I'm Aiko.", true),
                MessageUI("How are you feeling today?", true)
            )) }
        } else {
            _uiState.update { it.copy(messages = savedMessages) }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun toggleKeyboard() {
        _uiState.update { it.copy(isKeyboardOpen = !it.isKeyboardOpen) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun sendMessage() {
        val userMessageText = _uiState.value.inputText
        if (userMessageText.isBlank()) return

        repository.addMessage(Message(userMessageText, false))

        _uiState.update { state ->
            state.copy(
                messages = state.messages + MessageUI(userMessageText, false),
                inputText = "",
                isKeyboardOpen = false
            )
        }

        // Add a "Searching..." placeholder for Aiko
        _uiState.update { state ->
            state.copy(
                messages = state.messages + MessageUI("", true, isStreaming = true, isSearching = true)
            )
        }

        viewModelScope.launch {
            var fullAikoResponse = ""
            var hasStartedReceivingTokens = false
            try {
                repository.streamChat(
                    message = userMessageText
                ).collect { response ->
                    if (!hasStartedReceivingTokens && response.token != null) {
                        hasStartedReceivingTokens = true
                        // Stop showing "Searching" animation as soon as the first token arrives
                        _uiState.update { state ->
                            val newMessages = state.messages.toMutableList()
                            val lastIndex = newMessages.indexOfLast { it.isAiko && it.isSearching }
                            if (lastIndex != -1) {
                                newMessages[lastIndex] = newMessages[lastIndex].copy(isSearching = false)
                            }
                            state.copy(messages = newMessages)
                        }
                    }

                    fullAikoResponse += (response.token ?: "")
                    updateLastAikoMessage(response)
                    
                    if (response.done) {
                        repository.addMessage(Message(fullAikoResponse, true))
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    errorMessage = "Oups, je n'arrive pas à me connecter au serveur... (Error: ${e.message})",
                    // Clean up the placeholder message on error
                    messages = it.messages.filterNot { msg -> msg.isStreaming }
                ) }
            }
        }
    }

    private fun updateLastAikoMessage(response: TokenResponse) {
        _uiState.update { state ->
            val newMessages = state.messages.toMutableList()
            val lastIndex = newMessages.indexOfLast { it.isAiko && it.isStreaming }
            
            if (lastIndex != -1) {
                val lastMsg = newMessages[lastIndex]
                val updatedToken = response.token ?: ""
                newMessages[lastIndex] = lastMsg.copy(
                    text = lastMsg.text + updatedToken,
                    isStreaming = !response.done
                )
            }
            
            state.copy(
                messages = newMessages,
                currentStats = response
            )
        }
    }
}