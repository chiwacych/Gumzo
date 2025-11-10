package com.example.gumzo.utils

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Image picker utilities for Compose
 */
object ImagePickerUtils {

//    Composable function to create an image picker launcher
//    Usage in your Composable:
//
//    val imagePickerLauncher = rememberImagePickerLauncher { uri ->
//        // Handle selected image URI
//        selectedImageUri = uri
//    }
//
//    Button(onClick = { imagePickerLauncher.launch("image/*") }) {
//        Text("Pick Image")
//    }


    @Composable
    fun rememberImagePickerLauncher(
        onImageSelected: (Uri?) -> Unit
    ) = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onImageSelected(uri)
    }

    /**
     * Check if URI is valid
     */
    fun isValidImageUri(context: Context, uri: Uri?): Boolean {
        if (uri == null) return false
        return try {
            context.contentResolver.openInputStream(uri)?.use {
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get file size from URI (in bytes)
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Convert bytes to human-readable format
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}