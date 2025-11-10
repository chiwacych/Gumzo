package com.example.gumzo.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gumzo.data.model.User
import com.example.gumzo.data.repository.AuthRepository
import com.example.gumzo.data.repository.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()
    private var imageRepository: ImageRepository? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState
    
    fun initImageRepository(context: Context) {
        imageRepository = ImageRepository(context)
    }
    
    fun uploadProfilePicture(
        imageUri: Uri,
        userId: String,
        onComplete: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            imageRepository?.let { repo ->
                val result = repo.uploadProfilePicture(imageUri, userId)
                result.onSuccess { url ->
                    // Update user profile in Firestore
                    repository.updateProfilePicture(userId, url)
                }
                onComplete(result)
            } ?: run {
                onComplete(Result.failure(Exception("ImageRepository not initialized")))
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.signIn(email, password)
            _authState.value = if (result.isSuccess) {
                AuthState.Success(result.getOrNull()!!)
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.signUp(email, password, displayName)
            _authState.value = if (result.isSuccess) {
                AuthState.Success(result.getOrNull()!!)
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _authState.value = AuthState.Initial
    }

    fun checkAuthStatus() {
        val currentUser = repository.getCurrentUser()
        _authState.value = if (currentUser != null) {
            AuthState.Success(User(uid = currentUser.uid, email = currentUser.email ?: ""))
        } else {
            AuthState.Initial
        }
    }
}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}