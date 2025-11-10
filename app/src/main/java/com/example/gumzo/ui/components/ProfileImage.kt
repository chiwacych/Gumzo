package com.example.gumzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Reusable profile image component
 * Displays user profile picture or initials if no picture
 */
@Composable
fun ProfileImage(
    profileImageUrl: String?,
    userName: String,
    size: Dp = 40.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!profileImageUrl.isNullOrEmpty()) {
            // Display profile image
            AsyncImage(
                model = profileImageUrl,
                contentDescription = "Profile picture of $userName",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Display first letter of name as fallback
            Text(
                text = userName.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Profile image with border (for larger displays)
 */
@Composable
fun ProfileImageWithBorder(
    profileImageUrl: String?,
    userName: String,
    size: Dp = 80.dp,
    borderWidth: Dp = 2.dp,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(borderWidth, borderColor, CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!profileImageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = profileImageUrl,
                contentDescription = "Profile picture of $userName",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(borderWidth),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = userName.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Editable profile image with camera icon overlay
 */
@Composable
fun EditableProfileImage(
    profileImageUrl: String?,
    userName: String,
    size: Dp = 100.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        ProfileImageWithBorder(
            profileImageUrl = profileImageUrl,
            userName = userName,
            size = size,
            onClick = onClick
        )
        
        // Camera icon overlay
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(32.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 4.dp
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Change profile picture",
                modifier = Modifier.padding(6.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
