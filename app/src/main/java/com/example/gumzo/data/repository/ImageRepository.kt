package com.example.gumzo.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.gumzo.data.config.CloudinaryConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ImageRepository(private val context: Context) {

    init {
        // Initialize Cloudinary when repository is created
        Log.d("ImageRepository", "Initializing Cloudinary...")
        CloudinaryConfig.initialize(context)
    }

    /**
     * Convert content URI to a temporary file that Cloudinary can access
     */
    private fun uriToFile(uri: Uri): File {
        val contentResolver = context.contentResolver
        val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
        
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        
        return tempFile
    }

    /**
     * Upload profile picture and return the Cloudinary URL
     */
    suspend fun uploadProfilePicture(imageUri: Uri, userId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            var tempFile: File? = null
            try {
                Log.d("ImageRepository", "Uploading profile picture for user: $userId")
                
                // Convert URI to file
                tempFile = uriToFile(imageUri)
                Log.d("ImageRepository", "Converted URI to file: ${tempFile.absolutePath}")
                
                val url = CloudinaryConfig.uploadProfilePicture(
                    tempFile.absolutePath,
                    userId
                )
                Log.d("ImageRepository", "Profile picture uploaded: $url")
                Result.success(url)
            } catch (e: Exception) {
                Log.e("ImageRepository", "Profile picture upload failed: ${e.message}", e)
                Result.failure(e)
            } finally {
                // Clean up temp file
                tempFile?.delete()
            }
        }
    }

    /**
     * Upload chat image and return the Cloudinary URL
     */
    suspend fun uploadChatImage(imageUri: Uri, chatRoomId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            var tempFile: File? = null
            try {
                Log.d("ImageRepository", "Uploading chat image for room: $chatRoomId, uri: $imageUri")
                
                // Convert URI to file
                tempFile = uriToFile(imageUri)
                Log.d("ImageRepository", "Converted URI to file: ${tempFile.absolutePath}")
                
                val url = CloudinaryConfig.uploadChatImage(
                    tempFile.absolutePath,
                    chatRoomId
                )
                Log.d("ImageRepository", "Chat image uploaded: $url")
                Result.success(url)
            } catch (e: Exception) {
                Log.e("ImageRepository", "Chat image upload failed: ${e.message}", e)
                Result.failure(e)
            } finally {
                // Clean up temp file
                tempFile?.delete()
            }
        }
    }

    /**
     * Generic image upload
     */
    suspend fun uploadImage(imageUri: Uri, folder: String = "gumzo"): Result<String> {
        return withContext(Dispatchers.IO) {
            var tempFile: File? = null
            try {
                Log.d("ImageRepository", "Uploading generic image, uri: $imageUri")
                
                // Convert URI to file
                tempFile = uriToFile(imageUri)
                Log.d("ImageRepository", "Converted URI to file: ${tempFile.absolutePath}")
                
                val url = CloudinaryConfig.uploadImage(
                    tempFile.absolutePath,
                    folder
                )
                Log.d("ImageRepository", "Generic image uploaded: $url")
                Result.success(url)
            } catch (e: Exception) {
                Log.e("ImageRepository", "Generic image upload failed: ${e.message}", e)
                Result.failure(e)
            } finally {
                // Clean up temp file
                tempFile?.delete()
            }
        }
    }
}
