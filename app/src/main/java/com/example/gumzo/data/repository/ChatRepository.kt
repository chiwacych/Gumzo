package com.example.gumzo.data.repository

import com.example.gumzo.data.model.ChatRoom
import com.example.gumzo.data.model.Message
import com.example.gumzo.data.model.TypingStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()

    fun getChatRooms(): Flow<List<ChatRoom>> = callbackFlow {
        val listener = firestore.collection("chatRooms")
            .whereEqualTo("isPublic", true)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val rooms = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatRoom::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(rooms).isSuccess
            }

        awaitClose { listener.remove() }
    }

    suspend fun createChatRoom(name: String, userId: String): Result<String> {
        return try {
            val roomData = hashMapOf(
                "name" to name,
                "createdBy" to userId,
                "participantIds" to listOf(userId),
                "isPublic" to true,
                "createdAt" to System.currentTimeMillis(),
                "lastMessage" to "",
                "lastMessageTime" to System.currentTimeMillis()
            )

            val docRef = firestore.collection("chatRooms")
                .add(roomData)
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteChatRoom(roomId: String): Result<Unit> {
        return try {
            val messagesSnapshot = firestore.collection("chatRooms")
                .document(roomId)
                .collection("messages")
                .get()
                .await()

            messagesSnapshot.documents.forEach { it.reference.delete().await() }

            val typingStatusSnapshot = firestore.collection("chatRooms")
                .document(roomId)
                .collection("typingStatus")
                .get()
                .await()

            typingStatusSnapshot.documents.forEach { it.reference.delete().await() }

            firestore.collection("chatRooms")
                .document(roomId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMessages(chatRoomId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("chatRooms")
            .document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(messages).isSuccess
            }

        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(chatRoomId: String, message: Message) {
        firestore.collection("chatRooms")
            .document(chatRoomId)
            .collection("messages")
            .add(message)
            .await()

        firestore.collection("chatRooms")
            .document(chatRoomId)
            .update(
                mapOf(
                    "lastMessage" to message.text,
                    "lastMessageTime" to message.timestamp
                )
            )
            .await()
    }

    suspend fun deleteMessage(chatRoomId: String, messageId: String): Result<Unit> {
        return try {
            firestore.collection("chatRooms")
                .document(chatRoomId)
                .collection("messages")
                .document(messageId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChatRoom(chatRoomId: String): ChatRoom? {
        return try {
            val doc = firestore.collection("chatRooms")
                .document(chatRoomId)
                .get()
                .await()
            doc.toObject(ChatRoom::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            null
        }
    }

    fun updateTypingStatus(chatRoomId: String, userId: String, isTyping: Boolean) {
        firestore.collection("chatRooms")
            .document(chatRoomId)
            .collection("typingStatus")
            .document(userId)
            .set(mapOf("isTyping" to isTyping))
    }

    fun getTypingStatus(chatRoomId: String, currentUserId: String): Flow<List<TypingStatus>> = callbackFlow {
        val listener = firestore.collection("chatRooms")
            .document(chatRoomId)
            .collection("typingStatus")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val typingUsers = snapshot?.documents?.mapNotNull { doc ->
                    val isTyping = doc.getBoolean("isTyping") ?: false
                    val userId = doc.id
                    
                    // Exclude current user and only include actively typing users
                    if (userId != currentUserId && isTyping) {
                        val userName = doc.getString("userName") ?: "Someone"
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        
                        TypingStatus(
                            userId = userId,
                            userName = userName,
                            isTyping = isTyping,
                            timestamp = timestamp
                        )
                    } else {
                        null
                    }
                } ?: emptyList()

                trySend(typingUsers).isSuccess
            }

        awaitClose { listener.remove() }
    }
}