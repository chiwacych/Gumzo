package com.example.gumzo.data.model

data class TypingStatus(
    val userId: String = "",
    val userName: String = "",
    val isTyping: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
