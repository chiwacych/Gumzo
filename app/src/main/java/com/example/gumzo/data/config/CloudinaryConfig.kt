package com.example.gumzo.data.config

import android.content.Context
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.gumzo.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CloudinaryConfig {
    private var isInitialized = false
    private const val TAG = "CloudinaryConfig"

    /**
     * Initialize Cloudinary credentials
     */
    fun initialize(context: Context) {
        if (!isInitialized) {
            Log.d(TAG, "Initializing Cloudinary...")
            
            val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME
            val apiKey = BuildConfig.CLOUDINARY_API_KEY
            val apiSecret = BuildConfig.CLOUDINARY_API_SECRET
            
            val config = HashMap<String, String>().apply {
                put("cloud_name", cloudName)
                put("api_key", apiKey)
                put("api_secret", apiSecret)
            }
            
            try {
                MediaManager.init(context, config)
                isInitialized = true
                Log.d(TAG, "Cloudinary initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Cloudinary: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * Upload image to Cloudinary
     * @param imageUri Local URI of the image to upload
     * @param folder Folder name in Cloudinary ("profile_pictures" or "chat_images")
     * @return Cloudinary URL of the uploaded image
     */
    suspend fun uploadImage(imageUri: String, folder: String = "gumzo"): String {
        Log.d(TAG, "Starting upload for: $imageUri to folder: $folder")
        return suspendCancellableCoroutine { continuation ->
            val requestId = MediaManager.get().upload(imageUri)
                .option("folder", folder)
                .option("resource_type", "image")
                .option("transformation", "q_auto,f_auto") // Auto quality and format
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d(TAG, "Upload started: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        Log.d(TAG, "Upload progress: $bytes/$totalBytes")
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        Log.d(TAG, "Upload successful: $resultData")
                        val secureUrl = resultData["secure_url"] as? String
                        if (secureUrl != null) {
                            continuation.resume(secureUrl)
                        } else {
                            Log.e(TAG, "Upload successful but no URL returned")
                            continuation.resumeWithException(
                                Exception("Upload successful but no URL returned")
                            )
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e(TAG, "Upload error: ${error.description} (code: ${error.code})")
                        continuation.resumeWithException(
                            Exception("Upload failed: ${error.description}")
                        )
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w(TAG, "Upload rescheduled: ${error.description}")
                        continuation.resumeWithException(
                            Exception("Upload rescheduled: ${error.description}")
                        )
                    }
                })
                .dispatch()

            continuation.invokeOnCancellation {
                Log.d(TAG, "Upload cancelled: $requestId")
                // Cancel upload if coroutine is cancelled
                MediaManager.get().cancelRequest(requestId)
            }
        }
    }

    /**
     * Upload profile picture with optimizations
     */
    suspend fun uploadProfilePicture(imageUri: String, userId: String): String {
        return suspendCancellableCoroutine { continuation ->
            val requestId = MediaManager.get().upload(imageUri)
                .option("folder", "gumzo/profile_pictures")
                .option("public_id", "user_$userId") // Overwrites previous profile pic
                .option("resource_type", "image")
                .option("transformation", "c_fill,w_300,h_300,g_face,q_auto,f_auto") // Crop to face, 300x300
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val secureUrl = resultData["secure_url"] as? String
                        if (secureUrl != null) {
                            continuation.resume(secureUrl)
                        } else {
                            continuation.resumeWithException(
                                Exception("Upload successful but no URL returned")
                            )
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        continuation.resumeWithException(
                            Exception("Upload failed: ${error.description}")
                        )
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        continuation.resumeWithException(
                            Exception("Upload rescheduled: ${error.description}")
                        )
                    }
                })
                .dispatch()

            continuation.invokeOnCancellation {
                MediaManager.get().cancelRequest(requestId)
            }
        }
    }

    /**
     * Upload chat image
     */
    suspend fun uploadChatImage(imageUri: String, chatRoomId: String): String {
        return suspendCancellableCoroutine { continuation ->
            val timestamp = System.currentTimeMillis()
            val requestId = MediaManager.get().upload(imageUri)
                .option("folder", "gumzo/chat_images/$chatRoomId")
                .option("public_id", "img_$timestamp")
                .option("resource_type", "image")
                .option("transformation", "q_auto,f_auto,w_800") // Max width 800px
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val secureUrl = resultData["secure_url"] as? String
                        if (secureUrl != null) {
                            continuation.resume(secureUrl)
                        } else {
                            continuation.resumeWithException(
                                Exception("Upload successful but no URL returned")
                            )
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        continuation.resumeWithException(
                            Exception("Upload failed: ${error.description}")
                        )
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        continuation.resumeWithException(
                            Exception("Upload rescheduled: ${error.description}")
                        )
                    }
                })
                .dispatch()

            continuation.invokeOnCancellation {
                MediaManager.get().cancelRequest(requestId)
            }
        }
    }
}
