package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AddFlashcardSheet
import com.example.ui.HomeScreen
import com.example.ui.MainViewModel
import com.example.ui.ReviewScreen
import com.example.ui.theme.BgDark
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark
                ) {
                    FlashRecallApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun FlashRecallApp(viewModel: MainViewModel) {
    val cards by viewModel.allCards.collectAsStateWithLifecycle()
    val creationChatMessages by viewModel.creationChatMessages.collectAsStateWithLifecycle()
    val isCreationThinking by viewModel.isCreationThinking.collectAsStateWithLifecycle()

    val activeReviewList by viewModel.activeReviewList.collectAsStateWithLifecycle()
    val currentReviewIndex by viewModel.currentReviewIndex.collectAsStateWithLifecycle()
    val reviewScenarioLoading by viewModel.reviewScenarioLoading.collectAsStateWithLifecycle()
    val currentScenarioOrQuestion by viewModel.currentScenarioOrQuestion.collectAsStateWithLifecycle()
    val reviewChatHistory by viewModel.reviewChatHistory.collectAsStateWithLifecycle()
    val isReviewReplying by viewModel.isReviewReplying.collectAsStateWithLifecycle()

    var showAddSheet by remember { mutableStateOf(false) }
    var inReviewMode by remember { mutableStateOf(false) }

    if (inReviewMode) {
        ReviewScreen(
            cards = activeReviewList,
            currentIndex = currentReviewIndex,
            scenarioLoading = reviewScenarioLoading,
            scenarioOrQuestion = currentScenarioOrQuestion,
            reviewChatHistory = reviewChatHistory,
            isReviewReplying = isReviewReplying,
            onSendReviewReply = { reply -> viewModel.sendReviewReply(reply) },
            onGradeCard = { card, grade -> viewModel.reviewCard(card, grade) },
            onDeleteCard = { card -> viewModel.deleteCard(card) },
            onSpeak = { text -> viewModel.ttsHelper.speak(text) },
            onClose = { inReviewMode = false }
        )
    } else {
        HomeScreen(
            cards = cards,
            onOpenAdd = { showAddSheet = true },
            onOpenReview = { stageIndex ->
                viewModel.startReviewSession(stageIndex)
                inReviewMode = true
            }
        )
    }

    if (showAddSheet) {
        AddFlashcardSheet(
            onDismiss = { showAddSheet = false },
            onAddCard = { type, word, imageUrl, conversationHistory, focusSummary ->
                viewModel.addFlashcard(
                    type = type,
                    word = word,
                    imageUrl = imageUrl,
                    conversationHistory = conversationHistory,
                    focusSummary = focusSummary
                )
            },
            creationChatMessages = creationChatMessages,
            isCreationThinking = isCreationThinking,
            onSendMessage = { text, bitmap ->
                viewModel.sendCreationMessage(text, bitmap)
            },
            onClearChat = { viewModel.clearCreationChat() }
        )
    }
}
