package com.example.gumzo.data.repository

    import com.example.gumzo.data.model.User
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.FirebaseFirestore
    import kotlinx.coroutines.tasks.await

    class AuthRepository {
        private val auth = FirebaseAuth.getInstance()
        private val firestore = FirebaseFirestore.getInstance()

        suspend fun signIn(email: String, password: String): Result<User> {
            return try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user ?: return Result.failure(Exception("User not found"))

                val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
                val user = userDoc.toObject(User::class.java) ?: User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: ""
                )

                updateUserOnlineStatus(firebaseUser.uid, true)
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        suspend fun signUp(email: String, password: String, displayName: String): Result<User> {
            return try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user ?: return Result.failure(Exception("Failed to create user"))

                val user = User(
                    uid = firebaseUser.uid,
                    email = email,
                    displayName = displayName,
                    isOnline = true
                )

                firestore.collection("users").document(firebaseUser.uid).set(user).await()
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        fun signOut() {
            val currentUser = auth.currentUser
            currentUser?.let {
                updateUserOnlineStatus(it.uid, false)
            }
            auth.signOut()
        }

        fun getCurrentUser() = auth.currentUser

        private fun updateUserOnlineStatus(uid: String, isOnline: Boolean) {
            firestore.collection("users").document(uid).update(
                mapOf(
                    "isOnline" to isOnline,
                    "lastSeen" to System.currentTimeMillis()
                )
            )
        }
    }