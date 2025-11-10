package com.example.gumzo.ui.chat

import android.content.Context
import android.net.Uri
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gumzo.data.model.Message
import com.example.gumzo.data.model.MessageType
import com.example.gumzo.ui.components.ProfileImage
import com.example.gumzo.utils.ImagePickerUtils
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
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val typingUsers by viewModel.typingUsers.collectAsState()
    val roomOwnerId by viewModel.roomOwnerId.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteRoomDialog by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var showImagePreview by remember { mutableStateOf(false) }
    var previewImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageCaption by remember { mutableStateOf("") }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isRoomOwner = currentUserId == roomOwnerId
    
    // Image picker launcher - now shows preview instead of sending directly
    val imagePickerLauncher = ImagePickerUtils.rememberImagePickerLauncher { uri ->
        uri?.let {
            previewImageUri = it
            imageCaption = ""
            showImagePreview = true
        } ?: run {
            Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

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

    // Show image preview dialog with caption input
    if (showImagePreview && previewImageUri != null) {
        AlertDialog(
            onDismissRequest = { 
                showImagePreview = false
                previewImageUri = null
                imageCaption = ""
            },
            title = { Text("Send Image") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Image preview
                    AsyncImage(
                        model = previewImageUri,
                        contentDescription = "Image preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Caption input
                    OutlinedTextField(
                        value = imageCaption,
                        onValueChange = { imageCaption = it },
                        placeholder = { Text("Add a caption (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        previewImageUri?.let { uri ->
                            showImagePreview = false
                            isUploadingImage = true
                            Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()
                            
                            viewModel.sendImageMessage(uri, imageCaption) { result ->
                                isUploadingImage = false
                                result.onSuccess {
                                    Toast.makeText(context, "Image sent successfully!", Toast.LENGTH_SHORT).show()
                                }
                                result.onFailure { error ->
                                    Toast.makeText(
                                        context, 
                                        "Failed to send image: ${error.message}", 
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            
                            previewImageUri = null
                            imageCaption = ""
                        }
                    },
                    enabled = !isUploadingImage
                ) {
                    Text(if (isUploadingImage) "Sending..." else "Send")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showImagePreview = false
                        previewImageUri = null
                        imageCaption = ""
                    },
                    enabled = !isUploadingImage
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        onDelete = { viewModel.deleteMessage(message) },
                        onImageClick = { imageUrl ->
                            android.util.Log.d("ChatRoom", "onImageClick called with: $imageUrl")
                            selectedImageUrl = imageUrl
                            android.util.Log.d("ChatRoom", "selectedImageUrl set to: $selectedImageUrl")
                        },
                        onProfilePictureClick = { profileUrl ->
                            android.util.Log.d("ChatRoom", "onProfilePictureClick called with: $profileUrl")
                            selectedImageUrl = profileUrl
                        }
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
                },
                onImageClick = {
                    imagePickerLauncher.launch("image/*")
                },
                isUploadingImage = isUploadingImage
            )
        }
    }
        
        // Fullscreen image viewer - rendered on top of Scaffold
        selectedImageUrl?.let { imageUrl ->
            android.util.Log.d("ChatRoom", "Showing FullscreenImageViewer for: $imageUrl")
            FullscreenImageViewer(
                imageUrl = imageUrl,
                onDismiss = { 
                    android.util.Log.d("ChatRoom", "Dismissing fullscreen viewer")
                    selectedImageUrl = null 
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
    onDelete: () -> Unit,
    onImageClick: (String) -> Unit = {},
    onProfilePictureClick: (String) -> Unit = {}
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
        Row(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            // Show profile picture for other users (on the left)
            if (!isOwnMessage) {
                ProfileImage(
                    profileImageUrl = message.senderProfilePicture,
                    userName = message.senderName,
                    size = 32.dp,
                    onClick = if (!message.senderProfilePicture.isNullOrEmpty()) {
                        { onProfilePictureClick(message.senderProfilePicture) }
                    } else null,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            
            Column(
                modifier = Modifier
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
                
                // Display image if it's an image message
                if (message.type == MessageType.IMAGE && message.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Image from ${message.senderName}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .padding(bottom = if (message.text.isNotEmpty()) 8.dp else 0.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { 
                                android.util.Log.d("ChatRoom", "Image clicked: ${message.imageUrl}")
                                onImageClick(message.imageUrl) 
                            },
                        contentScale = ContentScale.Fit
                    )
                }
                
                // Display text if available
                if (message.text.isNotEmpty()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onImageClick: () -> Unit,
    isUploadingImage: Boolean = false
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            placeholder = { 
                Text(
                    if (isUploadingImage) "Uploading image..." 
                    else "Type a message..."
                ) 
            },
            trailingIcon = {
                Row {
                    // Emoji button - Opens system emoji keyboard
                    IconButton(
                        onClick = {
                            // Request focus on the text field to trigger keyboard
                            focusRequester.requestFocus()
                        },
                        enabled = !isUploadingImage
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEmotions,
                            contentDescription = "Add emoji",
                            tint = if (isUploadingImage) 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            else 
                                MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Image/Attach button
                    IconButton(
                        onClick = onImageClick,
                        enabled = !isUploadingImage
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Send image",
                            tint = if (isUploadingImage) 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            else 
                                MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Default
            ),
            maxLines = 3,
            enabled = !isUploadingImage
        )
        
        // Send button
        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank() && !isUploadingImage
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (text.isNotBlank() && !isUploadingImage)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
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

@Composable
fun FullscreenImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Fullscreen image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        
        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
