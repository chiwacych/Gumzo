package com.example.gumzo.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gumzo.data.model.Message
import com.example.gumzo.data.model.MessageType
import com.example.gumzo.data.model.TypingStatus
import com.example.gumzo.data.repository.AuthRepository
import com.example.gumzo.data.repository.ChatRepository
import com.example.gumzo.data.repository.ImageRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

class ChatRoomViewModel(private val chatRoomId: String) : ViewModel() {
    private val chatRepository = ChatRepository()
    private val authRepository = AuthRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private var imageRepository: ImageRepository? = null

                                    private val _messages = MutableStateFlow<List<Message>>(emptyList())
                                    val messages: StateFlow<List<Message>> = _messages

                                    private val _isLoading = MutableStateFlow(false)
                                    val isLoading: StateFlow<Boolean> = _isLoading

                                    private val _typingUsers = MutableStateFlow<List<TypingStatus>>(emptyList())
                                    val typingUsers: StateFlow<List<TypingStatus>> = _typingUsers

                                    private val _roomOwnerId = MutableStateFlow<String?>(null)
                                    val roomOwnerId: StateFlow<String?> = _roomOwnerId

    private var typingTimeoutJob: Job? = null
    private var messagesJob: Job? = null
    private var typingStatusJob: Job? = null
    
    fun initImageRepository(context: Context) {
        if (imageRepository == null) {
            Log.d("ChatRoomViewModel", "Initializing ImageRepository...")
            imageRepository = ImageRepository(context)
            Log.d("ChatRoomViewModel", "ImageRepository initialized successfully")
        } else {
            Log.d("ChatRoomViewModel", "ImageRepository already initialized")
        }
    }
    
    fun sendImageMessage(
        imageUri: Uri,
        caption: String = "",
        onComplete: (Result<Boolean>) -> Unit
    ) {
        Log.d("ChatRoomViewModel", "sendImageMessage called with uri: $imageUri")
        viewModelScope.launch {
            imageRepository?.let { repo ->
                Log.d("ChatRoomViewModel", "Starting image upload...")
                // Upload image first
                val uploadResult = repo.uploadChatImage(imageUri, chatRoomId)
                
                uploadResult.onSuccess { imageUrl ->
                    Log.d("ChatRoomViewModel", "Image uploaded successfully: $imageUrl")
                    val currentUser = authRepository.getCurrentUser()
                    if (currentUser != null) {
                        // Get user's profile picture
                        val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                        val profilePicture = userDoc.getString("profilePictureUrl") ?: ""
                        
                        // Send message with image URL
                        val message = Message(
                            senderId = currentUser.uid,
                            senderName = getFormattedDisplayName(currentUser),
                            text = caption,
                            imageUrl = imageUrl,
                            senderProfilePicture = profilePicture,
                            type = MessageType.IMAGE
                        )
                        chatRepository.sendMessage(chatRoomId, message)
                        Log.d("ChatRoomViewModel", "Image message sent successfully")
                        onComplete(Result.success(true))
                    } else {
                        Log.e("ChatRoomViewModel", "User not logged in")
                        onComplete(Result.failure(Exception("User not logged in")))
                    }
                }
                
                uploadResult.onFailure { error ->
                    Log.e("ChatRoomViewModel", "Image upload failed: ${error.message}", error)
                    onComplete(Result.failure(error))
                }
            } ?: run {
                Log.e("ChatRoomViewModel", "ImageRepository not initialized")
                onComplete(Result.failure(Exception("ImageRepository not initialized")))
            }
        }
    }                                    init {
                                        // Only load if user is authenticated
                                        if (authRepository.getCurrentUser() != null) {
                                            loadMessages()
                                            loadTypingStatus()
                                            loadChatRoom()
                                        }
                                    }

                                    // Helper function to format display name with email
                                    private fun getFormattedDisplayName(user: com.google.firebase.auth.FirebaseUser): String {
                                        val displayName = user.displayName
                                        val email = user.email
                                        
                                        return when {
                                            !displayName.isNullOrBlank() && !email.isNullOrBlank() -> "$displayName ($email)"
                                            !displayName.isNullOrBlank() -> displayName
                                            !email.isNullOrBlank() -> email
                                            else -> "Unknown User"
                                        }
                                    }

                                    private fun loadChatRoom() {
                                        viewModelScope.launch {
                                            val chatRoom = chatRepository.getChatRoom(chatRoomId)
                                            _roomOwnerId.value = chatRoom?.createdBy
                                        }
                                    }

                                    fun canDeleteMessage(message: Message): Boolean {
                                        val currentUserId = authRepository.getCurrentUser()?.uid ?: return false
                                        val isOwner = currentUserId == _roomOwnerId.value
                                        val isMessageSender = currentUserId == message.senderId
                                        
                                        return isOwner || isMessageSender
                                    }

                                    private fun loadMessages() {
                                        if (authRepository.getCurrentUser() == null) return
                                        
                                        messagesJob?.cancel()
                                        messagesJob = viewModelScope.launch {
                                            chatRepository.getMessages(chatRoomId).collect { messageList: List<Message> ->
                                                _messages.value = messageList
                                            }
                                        }
                                    }

                                    private fun loadTypingStatus() {
                                        val currentUserId = authRepository.getCurrentUser()?.uid ?: return
                                        
                                        typingStatusJob?.cancel()
                                        typingStatusJob = viewModelScope.launch {
                                            chatRepository.getTypingStatus(chatRoomId, currentUserId).collect { typingList ->
                                                _typingUsers.value = typingList
                                            }
                                        }
                                    }

    fun sendMessage(text: String) {
        val currentUser = authRepository.getCurrentUser() ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            // Get user's profile picture from Firestore
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
            val profilePicture = userDoc.getString("profilePictureUrl") ?: ""
            
            val message = Message(
                senderId = currentUser.uid,
                senderName = getFormattedDisplayName(currentUser),
                text = text,
                senderProfilePicture = profilePicture,
                type = MessageType.TEXT
            )
            chatRepository.sendMessage(chatRoomId, message)
        }
    }                                    fun deleteMessage(message: Message) {
                                        if (!canDeleteMessage(message)) return

                                        viewModelScope.launch {
                                            chatRepository.deleteMessage(chatRoomId, message.id)
                                        }
                                    }

                                    fun deleteChatRoom() {
                                        val currentUserId = authRepository.getCurrentUser()?.uid ?: return
                                        if (currentUserId != _roomOwnerId.value) return

                                        viewModelScope.launch {
                                            chatRepository.deleteChatRoom(chatRoomId)
                                        }
                                    }

                                    fun updateTypingStatus(isTyping: Boolean) {
                                        val currentUser = authRepository.getCurrentUser() ?: return

                                        // Cancel any existing timeout job
                                        typingTimeoutJob?.cancel()

                                        val typingStatusRef = firestore.collection("chatRooms")
                                            .document(chatRoomId)
                                            .collection("typingStatus")
                                            .document(currentUser.uid)

                                        val formattedName = getFormattedDisplayName(currentUser)

                                        if (isTyping) {
                                            // Set typing status with user name and timestamp
                                            typingStatusRef.set(
                                                mapOf(
                                                    "isTyping" to true,
                                                    "userName" to formattedName,
                                                    "timestamp" to System.currentTimeMillis()
                                                )
                                            )

                                            // Automatically clear typing status after 3 seconds of inactivity
                                            typingTimeoutJob = viewModelScope.launch {
                                                delay(3000)
                                                typingStatusRef.set(
                                                    mapOf(
                                                        "isTyping" to false,
                                                        "userName" to formattedName,
                                                        "timestamp" to System.currentTimeMillis()
                                                    )
                                                )
                                            }
                                        } else {
                                            // Clear typing status
                                            typingStatusRef.set(
                                                mapOf(
                                                    "isTyping" to false,
                                                    "userName" to formattedName,
                                                    "timestamp" to System.currentTimeMillis()
                                                )
                                            )
                                        }
                                    }

                                    override fun onCleared() {
                                        super.onCleared()
                                        // Clear typing status when leaving the chat
                                        updateTypingStatus(false)
                                        
                                        // Cancel all jobs
                                        typingTimeoutJob?.cancel()
                                        messagesJob?.cancel()
                                        typingStatusJob?.cancel()
                                    }
                                }