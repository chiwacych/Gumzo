package com.example.gumzo.ui.chat

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gumzo.data.model.Message
import com.example.gumzo.viewmodel.ChatRoomViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    chatRoomId: String,
    chatRoomName: String,
    viewModel: ChatRoomViewModel,
    onNavigateBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val typingUsers by viewModel.typingUsers.collectAsState()
    val roomOwnerId by viewModel.roomOwnerId.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteRoomDialog by remember { mutableStateOf(false) }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isRoomOwner = currentUserId == roomOwnerId

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Auto-scroll when typing indicator appears
    LaunchedEffect(typingUsers.isNotEmpty()) {
        if (messages.isNotEmpty() && typingUsers.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Show delete room confirmation dialog
    if (showDeleteRoomDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteRoomDialog = false },
            title = { Text("Delete Chat Room") },
            text = { 
                Text("Are you sure you want to delete this chat room? This will delete all messages and cannot be undone.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteChatRoom()
                        showDeleteRoomDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteRoomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(chatRoomName)
                        if (typingUsers.isNotEmpty()) {
                            Text(
                                text = getTypingText(typingUsers),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isRoomOwner) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Chat Room", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    showDeleteRoomDialog = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        canDelete = viewModel.canDeleteMessage(message),
                        onDelete = { viewModel.deleteMessage(message) }
                    )
                }
            }

            // Show typing indicator between messages and input
            if (typingUsers.isNotEmpty()) {
                TypingIndicator(
                    typingUsers = typingUsers,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            MessageInput(
                text = messageText,
                onTextChange = {
                    messageText = it
                    viewModel.updateTypingStatus(it.isNotEmpty())
                },
                onSend = {
                    viewModel.sendMessage(messageText)
                    messageText = ""
                    viewModel.updateTypingStatus(false)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isOwnMessage = message.senderId == currentUserId
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Message") },
            text = { Text("Are you sure you want to delete this message?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isOwnMessage) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    color = if (isOwnMessage)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                )
                .combinedClickable(
                    enabled = canDelete,
                    onLongClick = {
                        if (canDelete) {
                            showDeleteDialog = true
                        }
                    },
                    onClick = { }
                )
                .padding(12.dp)
        ) {
            if (!isOwnMessage) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = formatTimestamp(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun MessageInput(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a message...") },
            maxLines = 3
        )
        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank()
        ) {
            Icon(Icons.Default.Send, contentDescription = "Send")
        }
    }
}

@Composable
fun TypingIndicator(
    typingUsers: List<com.example.gumzo.data.model.TypingStatus>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${typingUsers.firstOrNull()?.userName ?: "Someone"} is typing",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            TypingDots()
        }
    }
}

@Composable
fun TypingDots() {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "typing")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(600),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(600, delayMillis = 200),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(600, delayMillis = 400),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("•", color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = alpha1))
        Text("•", color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = alpha2))
        Text("•", color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = alpha3))
    }
}

fun getTypingText(typingUsers: List<com.example.gumzo.data.model.TypingStatus>): String {
    return when (typingUsers.size) {
        0 -> ""
        1 -> "${typingUsers[0].userName} is typing..."
        2 -> "${typingUsers[0].userName} and ${typingUsers[1].userName} are typing..."
        else -> "${typingUsers[0].userName} and ${typingUsers.size - 1} others are typing..."
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
