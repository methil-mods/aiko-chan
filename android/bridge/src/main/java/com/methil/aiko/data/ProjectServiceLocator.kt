package com.methil.aiko.data

import android.content.Context
import com.methil.aiko.bridge.AikoConfig
import com.methil.aiko.service.AuthService
import com.methil.aiko.service.MessageService
import com.methil.aiko.service.inmemory.InMemoryMessageService

/**
 * Basic Service Locator to handle dependencies without adding heavy DI frameworks.
 */
object ProjectServiceLocator {
    
    private var authService: AuthService? = null
    private var tokenManager: TokenManager? = null
    private var authRepository: AuthRepository? = null
    private var characterRepository: CharacterRepository? = null
    private var messageRepository: MessageRepository? = null
    private var messageService: MessageService? = null

    fun provideAuthRepository(context: Context): AuthRepository {
        return authRepository ?: synchronized(this) {
            val repo = AuthRepository(
                provideAuthService(),
                provideTokenManager(context)
            )
            authRepository = repo
            repo
        }
    }

    fun provideCharacterRepository(): CharacterRepository {
        return characterRepository ?: synchronized(this) {
            val repo = CharacterRepository(
                AikoConfig.BASE_URL,
                provideAuthService()
            )
            characterRepository = repo
            repo
        }
    }

    fun provideMessageRepository(): MessageRepository {
        return messageRepository ?: synchronized(this) {
            val repo = MessageRepository(provideMessageService())
            messageRepository = repo
            repo
        }
    }

    fun provideTokenManager(context: Context): TokenManager {
        return tokenManager ?: synchronized(this) {
            val manager = TokenManager(context.applicationContext)
            tokenManager = manager
            manager
        }
    }

    private fun provideAuthService(): AuthService {
        return authService ?: synchronized(this) {
            val service = AuthService(AikoConfig.BASE_URL)
            authService = service
            service
        }
    }

    private fun provideMessageService(): MessageService {
        return messageService ?: synchronized(this) {
            val service = InMemoryMessageService()
            messageService = service
            service
        }
    }
}
