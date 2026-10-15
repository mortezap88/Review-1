package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Flashcard
import com.example.data.FlashcardTypes
import com.example.data.ReviewGrade
import com.example.data.SrsStage
import com.example.network.ChatMessage
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BadgeGreenBg
import com.example.ui.theme.BadgeGreenBorder
import com.example.ui.theme.BadgeGreenText
import com.example.ui.theme.BgDark
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceInput
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ReviewScreen(
    cards: List<Flashcard>,
    currentIndex: Int,
    scenarioLoading: Boolean,
    scenarioOrQuestion: String?,
    reviewChatHistory: List<ChatMessage>,
    isReviewReplying: Boolean,
    onSendReviewReply: (String) -> Unit,
    onGradeCard: (Flashcard, ReviewGrade) -> Unit,
    onDeleteCard: (Flashcard) -> Unit,
    onSpeak: (String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val currentCard = cards.getOrNull(currentIndex)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = BgDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Drag handle or top indicator bar matching Image 3
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(PrimaryPurple.copy(alpha = 0.6f))
                )
            }

            // Top Bar with Stage Pills and Trash icon matching Image 3
            TopStageReviewBar(
                currentStageIndex = currentCard?.stageIndex ?: 0,
                onDelete = {
                    currentCard?.let { onDeleteCard(it) }
                },
                onBack = onClose
            )

            if (currentCard == null) {
                // Completed session view
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🎉",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "All Done for Now!",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You've completed your scheduled reviews.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onClose,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text("Return to Stages", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                when (currentCard.type) {
                    FlashcardTypes.CONVERSATION_LINGUISTIC,
                    FlashcardTypes.CONVERSATION_TECHNICAL -> {
                        ConversationReviewContent(
                            card = currentCard,
                            isLoading = scenarioLoading,
                            chatHistory = reviewChatHistory,
                            isReplying = isReviewReplying,
                            onSendReply = onSendReviewReply,
                            onGrade = { grade -> onGradeCard(currentCard, grade) }
                        )
                    }
                    FlashcardTypes.SPELLING -> {
                        SpellingReviewContent(
                            card = currentCard,
                            onSpeak = { onSpeak(currentCard.word) },
                            onGrade = { grade -> onGradeCard(currentCard, grade) }
                        )
                    }
                    FlashcardTypes.PRONUNCIATION -> {
                        PronunciationReviewContent(
                            card = currentCard,
                            onSpeak = { onSpeak(currentCard.word) },
                            onGrade = { grade -> onGradeCard(currentCard, grade) }
                        )
                    }
                    FlashcardTypes.IMAGE -> {
                        ImageReviewContent(
                            card = currentCard,
                            onSpeak = { onSpeak(currentCard.word) },
                            onGrade = { grade -> onGradeCard(currentCard, grade) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopStageReviewBar(
    currentStageIndex: Int,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextSecondary
            )
        }

        // Horizontal pills matching Image 3: [2m] [20m] [2h] [1d] [3d] [7d] [14d] [30d]
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SrsStage.entries.forEachIndexed { idx, stage ->
                val isCurrent = idx == currentStageIndex
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isCurrent) stage.color.copy(alpha = 0.2f) else Color(0xFF19142D))
                        .border(
                            width = if (isCurrent) 1.5.dp else 1.dp,
                            color = if (isCurrent) stage.color else Color(0xFF282147),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stage.shortName,
                        color = if (isCurrent) stage.color else TextMuted,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Card",
                tint = AccentPink.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun ConversationReviewContent(
    card: Flashcard,
    isLoading: Boolean,
    chatHistory: List<ChatMessage>,
    isReplying: Boolean,
    onSendReply: (String) -> Unit,
    onGrade: (ReviewGrade) -> Unit
) {
    var replyText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Thinking Pill matching Image 3
        if (isLoading) {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF20173A))
                    .border(1.dp, PrimaryPurple.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = PrimaryPurpleLight,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Thinking",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Chat flow of the review
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chatHistory) { msg ->
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
                            .padding(14.dp)
                            .widthIn(max = 300.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = if (isUser) Color.White else TextPrimary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            if (isReplying) {
                item {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF20173A))
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
                                text = "Evaluating your response...",
                                color = PrimaryPurpleLight,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Reply Input Bar + Mic button matching Image 3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = replyText,
                onValueChange = { replyText = it },
                placeholder = {
                    Text(
                        if (card.type == FlashcardTypes.CONVERSATION_LINGUISTIC) "What would you say?..." else "Explain the concept...",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceInput,
                    unfocusedContainerColor = SurfaceInput,
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = SurfaceCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                maxLines = 2
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Microphone or send button matching bottom-right circle in Image 3
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple)
                    .clickable {
                        if (replyText.isNotBlank()) {
                            onSendReply(replyText)
                            replyText = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (replyText.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                    contentDescription = "Voice / Send Action",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Grading Buttons
        ReviewGradingRow(onGrade = onGrade)
    }
}

@Composable
fun SpellingReviewContent(
    card: Flashcard,
    onSpeak: () -> Unit,
    onGrade: (ReviewGrade) -> Unit
) {
    var isRevealed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Spelling Review",
            color = PrimaryPurpleLight,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Speaker icon to play audio
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(SurfaceCard)
                .border(2.dp, PrimaryPurple, CircleShape)
                .clickable { onSpeak() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Play Audio",
                tint = PrimaryPurpleLight,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Listen to the word and spell it in your mind.",
            color = TextSecondary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isRevealed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceInput)
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = card.word,
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            ReviewGradingRow(onGrade = onGrade)
        } else {
            Button(
                onClick = { isRevealed = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .height(50.dp)
                    .fillMaxWidth(0.7f)
            ) {
                Text("Reveal Spelling", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun PronunciationReviewContent(
    card: Flashcard,
    onSpeak: () -> Unit,
    onGrade: (ReviewGrade) -> Unit
) {
    var isRevealed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Pronunciation Review",
            color = AccentTeal,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceCard)
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(20.dp))
                .padding(vertical = 36.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = card.word,
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Say the word aloud or in your mind.",
            color = TextSecondary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isRevealed) {
            Button(
                onClick = onSpeak,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF285E61)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hear Native Pronunciation", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            ReviewGradingRow(onGrade = onGrade)
        } else {
            Button(
                onClick = {
                    isRevealed = true
                    onSpeak()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .height(50.dp)
                    .fillMaxWidth(0.7f)
            ) {
                Text("Reveal & Check", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ImageReviewContent(
    card: Flashcard,
    onSpeak: () -> Unit,
    onGrade: (ReviewGrade) -> Unit
) {
    var isRevealed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Image Recognition",
            color = PrimaryPurpleLight,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceInput)
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (!card.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = card.imageUrl,
                    contentDescription = "Flashcard Target Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text("No Image Available", color = TextMuted)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "What is the word or term for this image?",
            color = TextSecondary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isRevealed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = card.word,
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onSpeak) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = PrimaryPurpleLight)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            ReviewGradingRow(onGrade = onGrade)
        } else {
            Button(
                onClick = { isRevealed = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .height(50.dp)
                    .fillMaxWidth(0.7f)
            ) {
                Text("Reveal Word", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ReviewGradingRow(onGrade: (ReviewGrade) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { onGrade(ReviewGrade.AGAIN) },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF381A25),
                contentColor = AccentPink
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Again", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("2m", fontSize = 10.sp, color = TextMuted)
            }
        }

        Button(
            onClick = { onGrade(ReviewGrade.HARD) },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF282038),
                contentColor = TextSecondary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Hard", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Keep", fontSize = 10.sp, color = TextMuted)
            }
        }

        Button(
            onClick = { onGrade(ReviewGrade.GOOD) },
            colors = ButtonDefaults.buttonColors(
                containerColor = BadgeGreenBg,
                contentColor = BadgeGreenText
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Good", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Next", fontSize = 10.sp, color = BadgeGreenText.copy(alpha = 0.7f))
            }
        }

        Button(
            onClick = { onGrade(ReviewGrade.EASY) },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E284A),
                contentColor = PrimaryPurpleLight
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Easy", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("+2 Stg", fontSize = 10.sp, color = PrimaryPurpleLight.copy(alpha = 0.7f))
            }
        }
    }
}
