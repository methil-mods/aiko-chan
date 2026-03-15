package com.methil.aiko.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.methil.aiko.data.CharacterRepository
import com.methil.aiko.data.AuthRepository
import com.methil.aiko.domain.Character
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val characters: List<Character> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val unlockResult: UnlockStatus? = null
)

data class UnlockStatus(
    val characterName: String,
    val isNew: Boolean
)

class MainViewModel(
    private val characterRepository: CharacterRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun loadCharacters() {
        val token = authRepository.getToken() ?: return
        
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        
        viewModelScope.launch {
            characterRepository.getCharacters(token)
                .onSuccess { list ->
                    _uiState.update { it.copy(characters = list, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message, isLoading = false) }
                }
        }
    }

    fun unlockCharacter(nfcTag: String) {
        val token = authRepository.getToken() ?: return
        
        viewModelScope.launch {
            characterRepository.unlockCharacter(token, nfcTag)
                .onSuccess { result ->
                    _uiState.update { it.copy(
                        unlockResult = UnlockStatus(result.characterName, result.isNew)
                    ) }
                    // Reload characters to show the new one
                    loadCharacters()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = "Échec du déverrouillage: ${error.message}") }
                }
        }
    }

    fun clearUnlockResult() {
        _uiState.update { it.copy(unlockResult = null) }
    }
    
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
