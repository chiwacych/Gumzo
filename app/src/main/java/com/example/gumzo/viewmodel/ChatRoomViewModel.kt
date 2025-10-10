package com.example.gumzo.viewmodel

                                import androidx.lifecycle.ViewModel
                                import androidx.lifecycle.viewModelScope
                                import com.example.gumzo.data.model.Message
                                import com.example.gumzo.data.model.MessageType
                                import com.example.gumzo.data.repository.AuthRepository
                                import com.example.gumzo.data.repository.ChatRepository
                                import com.google.firebase.firestore.FirebaseFirestore
                                import kotlinx.coroutines.flow.MutableStateFlow
                                import kotlinx.coroutines.flow.StateFlow
                                import kotlinx.coroutines.launch

                                class ChatRoomViewModel(private val chatRoomId: String) : ViewModel() {
                                    private val chatRepository = ChatRepository()
                                    private val authRepository = AuthRepository()
                                    private val firestore = FirebaseFirestore.getInstance()

                                    private val _messages = MutableStateFlow<List<Message>>(emptyList())
                                    val messages: StateFlow<List<Message>> = _messages

                                    private val _isLoading = MutableStateFlow(false)
                                    val isLoading: StateFlow<Boolean> = _isLoading

                                    init {
                                        loadMessages()
                                    }

                                    private fun loadMessages() {
                                        viewModelScope.launch {
                                            chatRepository.getMessages(chatRoomId).collect { messageList: List<Message> ->
                                                _messages.value = messageList
                                            }
                                        }
                                    }

                                    fun sendMessage(text: String) {
                                        val currentUser = authRepository.getCurrentUser() ?: return
                                        if (text.isBlank()) return

                                        viewModelScope.launch {
                                            val message = Message(
                                                senderId = currentUser.uid,
                                                senderName = currentUser.displayName ?: currentUser.email ?: "Unknown",
                                                text = text,
                                                type = MessageType.TEXT
                                            )
                                            chatRepository.sendMessage(chatRoomId, message)
                                        }
                                    }

                                    fun updateTypingStatus(isTyping: Boolean) {
                                        val currentUser = authRepository.getCurrentUser() ?: return

                                        val typingStatusRef = firestore.collection("chatRooms")
                                            .document(chatRoomId)
                                            .collection("typingStatus")
                                            .document(currentUser.uid)

                                        typingStatusRef.set(mapOf("isTyping" to isTyping))
                                    }
                                }