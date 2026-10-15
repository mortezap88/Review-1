package com.example.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.FlashcardTypes
import com.example.network.ChatMessage
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BadgeGreenBg
import com.example.ui.theme.BadgeGreenBorder
import com.example.ui.theme.BadgeGreenText
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceInput
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class AddCardTab(val title: String) {
    SPELLING("Spelling"),
    PRONUNCIATION("Pronunciation"),
    IMAGE("Image"),
    CONVERSATION("Conversation")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFlashcardSheet(
    onDismiss: () -> Unit,
    onAddCard: (
        type: String,
        word: String,
        imageUrl: String?,
        conversationHistory: List<ChatMessage>?,
        focusSummary: String?
    ) -> Unit,
    creationChatMessages: List<ChatMessage>,
    isCreationThinking: Boolean,
    onSendMessage: (text: String, imageBitmap: Bitmap?) -> Unit,
    onClearChat: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(AddCardTab.SPELLING) }

    // Inputs for regular tabs
    var wordInput by remember { mutableStateOf("") }
    var imageUrlInput by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            imageUrlInput = uri.toString()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            onClearChat()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = SurfaceDark,
        dragHandle = null,
        modifier = Modifier
            .fillMaxHeight(if (selectedTab == AddCardTab.CONVERSATION) 0.94f else 0.65f)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add New Flashcard",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    onClearChat()
                    onDismiss()
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Selector matching Image 1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF130F24))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AddCardTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) PrimaryPurple else Color.Transparent)
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.title,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                AddCardTab.SPELLING -> {
                    SpellingAddView(
                        word = wordInput,
                        onWordChange = { wordInput = it },
                        onCancel = onDismiss,
                        onAdd = {
                            if (wordInput.isNotBlank()) {
                                onAddCard(FlashcardTypes.SPELLING, wordInput, null, null, null)
                                onDismiss()
                            }
                        }
                    )
                }
                AddCardTab.PRONUNCIATION -> {
                    PronunciationAddView(
                        word = wordInput,
                        onWordChange = { wordInput = it },
                        onCancel = onDismiss,
                        onAdd = {
                            if (wordInput.isNotBlank()) {
                                onAddCard(FlashcardTypes.PRONUNCIATION, wordInput, null, null, null)
                                onDismiss()
                            }
                        }
                    )
                }
                AddCardTab.IMAGE -> {
                    ImageAddView(
                        word = wordInput,
                        onWordChange = { wordInput = it },
                        imageUri = selectedImageUri,
                        imageUrl = imageUrlInput,
                        onPickPhoto = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onImageUrlChange = {
                            imageUrlInput = it
                            selectedImageUri = null
                        },
                        onCancel = onDismiss,
                        onAdd = {
                            if (wordInput.isNotBlank()) {
                                onAddCard(
                                    FlashcardTypes.IMAGE,
                                    wordInput,
                                    selectedImageUri?.toString() ?: imageUrlInput,
                                    null,
                                    null
                                )
                                onDismiss()
                            }
                        }
                    )
                }
                AddCardTab.CONVERSATION -> {
                    ConversationCreationView(
                        messages = creationChatMessages,
                        isThinking = isCreationThinking,
                        onSendMessage = onSendMessage,
                        onSelectLinguistic = {
                            val lastTopic = creationChatMessages.lastOrNull { it.sender == "user" }?.text
                                ?: "Linguistic Element"
                            onAddCard(
                                FlashcardTypes.CONVERSATION_LINGUISTIC,
                                lastTopic.take(50),
                                null,
                                creationChatMessages,
                                "Linguistic Scenario"
                            )
                            onDismiss()
                        },
                        onSelectTechnical = {
                            val lastTopic = creationChatMessages.lastOrNull { it.sender == "user" }?.text
                                ?: "Technical Topic"
                            onAddCard(
                                FlashcardTypes.CONVERSATION_TECHNICAL,
                                lastTopic.take(50),
                                null,
                                creationChatMessages,
                                "Technical Question"
                            )
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SpellingAddView(
    word: String,
    onWordChange: (String) -> Unit,
    onCancel: () -> Unit,
    onAdd: () -> Unit
) {
    Column {
        OutlinedTextField(
            value = word,
            onValueChange = onWordChange,
            placeholder = { Text("Word (Front: Sound • Back: Spelling)", color = TextMuted) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("spelling_word_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceInput,
                unfocusedContainerColor = SurfaceInput,
                focusedBorderColor = PrimaryPurple,
                unfocusedBorderColor = SurfaceCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Initial Stage: 2 Minutes",
            color = AccentPink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = TextSecondary, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onAdd,
                enabled = word.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4C3B77),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("add_card_button")
            ) {
                Text("Add Card", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun PronunciationAddView(
    word: String,
    onWordChange: (String) -> Unit,
    onCancel: () -> Unit,
    onAdd: () -> Unit
) {
    Column {
        OutlinedTextField(
            value = word,
            onValueChange = onWordChange,
            placeholder = { Text("Word (Front: Text • Back: Pronunciation sound)", color = TextMuted) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pronunciation_word_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceInput,
                unfocusedContainerColor = SurfaceInput,
                focusedBorderColor = PrimaryPurple,
                unfocusedBorderColor = SurfaceCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Initial Stage: 2 Minutes",
            color = AccentPink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = TextSecondary, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onAdd,
                enabled = word.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4C3B77),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("add_card_button")
            ) {
                Text("Add Card", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun ImageAddView(
    word: String,
    onWordChange: (String) -> Unit,
    imageUri: Uri?,
    imageUrl: String?,
    onPickPhoto: () -> Unit,
    onImageUrlChange: (String) -> Unit,
    onCancel: () -> Unit,
    onAdd: () -> Unit
) {
    Column {
        // Box 1: Image Picker / Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceInput)
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
                .clickable { onPickPhoto() },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null || !imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUri ?: imageUrl,
                    contentDescription = "Selected Flashcard Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Pick Image",
                        tint = PrimaryPurpleLight,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to choose photo from gallery",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Box 2: Word text input
        OutlinedTextField(
            value = word,
            onValueChange = onWordChange,
            placeholder = { Text("Word to guess (e.g., Photosynthesis, Stethoscope)", color = TextMuted) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("image_word_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceInput,
                unfocusedContainerColor = SurfaceInput,
                focusedBorderColor = PrimaryPurple,
                unfocusedBorderColor = SurfaceCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Initial Stage: 2 Minutes",
            color = AccentPink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = TextSecondary, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onAdd,
                enabled = word.isNotBlank() && (imageUri != null || !imageUrl.isNullOrBlank()),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4C3B77),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("add_card_button")
            ) {
                Text("Add Card", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun ConversationCreationView(
    messages: List<ChatMessage>,
    isThinking: Boolean,
    onSendMessage: (text: String, imageBitmap: Bitmap?) -> Unit,
    onSelectLinguistic: () -> Unit,
    onSelectTechnical: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                attachedBitmap = bitmap
            } catch (e: Exception) {
                // Ignore failure
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Model Indicator pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF221A3D))
                    .border(1.dp, PrimaryPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PrimaryPurpleLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "gemini 3.5 flash lite (thinking low)",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Talk about what you're focusing on (grammar nuance, word in context, or technical mechanism). Attach images if needed. When ready, tap Linguistic or Technical below!",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            items(messages) { msg ->
                val isUser = msg.sender == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                )
                            )
                            .background(if (isUser) PrimaryPurple else SurfaceCard)
                            .border(1.dp, if (isUser) Color.Transparent else SurfaceCardBorder, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                            .widthIn(max = 280.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = if (isUser) Color.White else TextPrimary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF221A3D))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = PrimaryPurpleLight,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Thinking...",
                                    color = PrimaryPurpleLight,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Attached image preview if selected
        if (attachedBitmap != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceInput)
                ) {
                    // Preview indicator
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Photo attached",
                        tint = AccentTeal,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Image attached", color = AccentTeal, fontSize = 12.sp)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { attachedBitmap = null }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = TextMuted)
                }
            }
        }

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Attach image",
                    tint = PrimaryPurpleLight
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Discuss focus with AI...", color = TextMuted, fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceInput,
                    unfocusedContainerColor = SurfaceInput,
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = SurfaceCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                maxLines = 3
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank() || attachedBitmap != null) {
                        onSendMessage(inputText, attachedBitmap)
                        inputText = ""
                        attachedBitmap = null
                    }
                },
                enabled = !isThinking && (inputText.isNotBlank() || attachedBitmap != null)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (!isThinking && (inputText.isNotBlank() || attachedBitmap != null)) PrimaryPurpleLight else TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // MANDATORY ALWAYS-PRESENT 2 BUTTONS: LINGUISTIC & TECHNICAL
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onSelectLinguistic,
                enabled = messages.isNotEmpty() || inputText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B3BA7),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("save_linguistic_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Linguistic", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Scenario Review", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }

            Button(
                onClick = onSelectTechnical,
                enabled = messages.isNotEmpty() || inputText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF285E61),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("save_technical_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Technical", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Question Review", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}
