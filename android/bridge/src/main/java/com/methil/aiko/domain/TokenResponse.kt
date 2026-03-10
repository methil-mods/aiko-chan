package com.methil.aiko.domain

import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    val token: String? = null,
    val tokensPerSecond: Double? = null,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val tokensConsumed: Int? = null,
    val elapsedMs: Long? = null,
    val done: Boolean = false
)