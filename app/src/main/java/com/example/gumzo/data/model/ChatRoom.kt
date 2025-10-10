package com.example.gumzo.data.model

    data class ChatRoom(
        val id: String = "",
        val name: String = "",
        val createdBy: String = "",
        val participantIds: List<String> = emptyList(),
        val isPublic: Boolean = true,
        val createdAt: Long = 0L,
        val lastMessage: String = "",
        val lastMessageTime: Long = 0L
    )