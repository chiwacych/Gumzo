package com.example.gumzo.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gumzo.data.model.ChatRoom
import com.example.gumzo.viewmodel.ChatListViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel,
    onChatRoomClick: (String, String) -> Unit,
    onSignOut: () -> Unit
) {
    val chatRooms by viewModel.chatRooms.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Rooms") },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create Room")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null -> {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                chatRooms.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No chat rooms yet")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap + to create one", style = MaterialTheme.typography.bodySmall)
                    }
                }
                else -> {
                    LazyColumn {
                        items(chatRooms) { chatRoom ->
                            ChatRoomItem(
                                chatRoom = chatRoom,
                                onClick = { onChatRoomClick(chatRoom.id, chatRoom.name) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

if (showDialog) {
    CreateChatRoomDialog(
        onDismiss = { showDialog = false },
        onCreate = { roomName ->
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                viewModel.createChatRoom(
                    roomName,
                    onSuccess = { roomId ->
                        showDialog = false
                    },
                    onError = { errorMessage ->
                        // Optionally show error to user
                        showDialog = false
                    }
                )
            }
        }
    )
}
    }
}

@Composable
fun ChatRoomItem(chatRoom: ChatRoom, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chatRoom.name,
                style = MaterialTheme.typography.titleMedium
            )
            if (chatRoom.lastMessage.isNotEmpty()) {
                Text(
                    text = chatRoom.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = formatTime(chatRoom.lastMessageTime),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CreateChatRoomDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var roomName by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Chat Room") },
        text = {
            Column {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = {
                        roomName = it
                        showError = false
                    },
                    label = { Text("Room Name") },
                    isError = showError && roomName.isBlank(),
                    singleLine = true
                )
                if (showError && roomName.isBlank()) {
                    Text(
                        text = "Room name cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (roomName.isNotBlank()) {
                        onCreate(roomName)
                    } else {
                        showError = true
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}