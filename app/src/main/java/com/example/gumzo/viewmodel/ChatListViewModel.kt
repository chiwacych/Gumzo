package com.example.gumzo.viewmodel

                                    import android.util.Log
                                    import androidx.lifecycle.ViewModel
                                    import androidx.lifecycle.viewModelScope
                                    import com.example.gumzo.data.model.ChatRoom
                                    import com.example.gumzo.data.repository.ChatRepository
                                    import com.google.firebase.auth.FirebaseAuth
                                    import kotlinx.coroutines.flow.MutableStateFlow
                                    import kotlinx.coroutines.flow.StateFlow
                                    import kotlinx.coroutines.flow.catch
                                    import kotlinx.coroutines.launch
                                    import kotlinx.coroutines.Job

                                    class ChatListViewModel : ViewModel() {
                                        private val repository = ChatRepository()
                                        private val auth = FirebaseAuth.getInstance()

                                        private val _chatRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
                                        val chatRooms: StateFlow<List<ChatRoom>> = _chatRooms

                                        private val _isLoading = MutableStateFlow(false)
                                        val isLoading: StateFlow<Boolean> = _isLoading

                                        private val _error = MutableStateFlow<String?>(null)
                                        val error: StateFlow<String?> = _error

                                        private var chatRoomsJob: Job? = null

                                        init {
                                            // Only load if user is authenticated
                                            if (auth.currentUser != null) {
                                                loadChatRooms()
                                            }
                                        }

                                        private fun loadChatRooms() {
                                            // Don't start if user is not authenticated
                                            if (auth.currentUser == null) {
                                                _isLoading.value = false
                                                return
                                            }
                                            
                                            chatRoomsJob?.cancel()
                                            chatRoomsJob = viewModelScope.launch {
                                                _isLoading.value = true
                                                _error.value = null

                                                repository.getChatRooms()
                                                    .catch { e: Throwable ->
                                                        _error.value = e.message ?: "Failed to load chat rooms"
                                                        _isLoading.value = false
                                                        Log.e("ChatListViewModel", "Error loading rooms", e)
                                                    }
                                                    .collect { rooms: List<ChatRoom> ->
                                                        _chatRooms.value = rooms
                                                        _isLoading.value = false
                                                    }
                                            }
                                        }

                                        fun createChatRoom(name: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
                                            val userId = auth.currentUser?.uid ?: run {
                                                onError("User not logged in")
                                                return
                                            }

                                            viewModelScope.launch {
                                                repository.createChatRoom(name, userId)
                                                    .onSuccess { roomId: String ->
                                                        onSuccess(roomId)
                                                    }
                                                    .onFailure { e: Throwable ->
                                                        onError(e.message ?: "Failed to create room")
                                                    }
                                            }
                                        }

                                        fun deleteChatRoom(roomId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
                                            viewModelScope.launch {
                                                repository.deleteChatRoom(roomId)
                                                    .onSuccess {
                                                        onSuccess()
                                                    }
                                                    .onFailure { e: Throwable ->
                                                        onError(e.message ?: "Failed to delete room")
                                                    }
                                            }
                                        }

                                        fun signOut() {
                                            // Cancel the Firestore listener before signing out
                                            chatRoomsJob?.cancel()
                                            chatRoomsJob = null
                                            
                                            // Clear the state
                                            _chatRooms.value = emptyList()
                                            _error.value = null
                                            _isLoading.value = false
                                            
                                            // Sign out from Firebase
                                            auth.signOut()
                                        }
                                    }