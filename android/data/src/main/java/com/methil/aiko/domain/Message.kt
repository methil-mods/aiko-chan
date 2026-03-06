package com.methil.aiko.domain

data class Message(
    val text: String,
    val isAiko: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)