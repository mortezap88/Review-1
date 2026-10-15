package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Flashcard
import com.example.data.FlashcardTypes
import com.example.data.ReviewGrade
import com.example.data.SpacedRepetitionUtils
import com.example.data.SrsStage
import com.example.network.ChatMessage
import com.example.network.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val flashcardDao = database.flashcardDao()
    private val geminiRepo = GeminiRepository()
    val ttsHelper = TtsHelper(application)

    val allCards: StateFlow<List<Flashcard>> = flashcardDao.getAllCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Creation Chat State
    private val _creationChatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val creationChatMessages: StateFlow<List<ChatMessage>> = _creationChatMessages.asStateFlow()

    private val _isCreationThinking = MutableStateFlow(false)
    val isCreationThinking: StateFlow<Boolean> = _isCreationThinking.asStateFlow()

    // Active Review Session State
    private val _activeReviewList = MutableStateFlow<List<Flashcard>>(emptyList())
    val activeReviewList: StateFlow<List<Flashcard>> = _activeReviewList.asStateFlow()

    private val _currentReviewIndex = MutableStateFlow(0)
    val currentReviewIndex: StateFlow<Int> = _currentReviewIndex.asStateFlow()

    private val _reviewScenarioLoading = MutableStateFlow(false)
    val reviewScenarioLoading: StateFlow<Boolean> = _reviewScenarioLoading.asStateFlow()

    private val _currentScenarioOrQuestion = MutableStateFlow<String?>(null)
    val currentScenarioOrQuestion: StateFlow<String?> = _currentScenarioOrQuestion.asStateFlow()

    private val _reviewChatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val reviewChatHistory: StateFlow<List<ChatMessage>> = _reviewChatHistory.asStateFlow()

    private val _isReviewReplying = MutableStateFlow(false)
    val isReviewReplying: StateFlow<Boolean> = _isReviewReplying.asStateFlow()

    init {
        seedInitialCardsIfEmpty()
    }

    private fun seedInitialCardsIfEmpty() {
        viewModelScope.launch {
            if (flashcardDao.getCardCount() == 0) {
                val now = System.currentTimeMillis()
                val sampleChat = listOf(
                    ChatMessage(sender = "user", text = "I want to practice using 'Were it not for' in formal negotiation contexts."),
                    ChatMessage(sender = "model", text = "Excellent choice. 'Were it not for' is an inverted conditional expressing hypothetical necessity (e.g. 'Were it not for your team's dedication, we couldn't have closed the round'). It conveys polite yet firm emphasis.")
                )

                val initialCards = listOf(
                    Flashcard(
                        type = FlashcardTypes.SPELLING,
                        word = "Conscientious",
                        stageIndex = 0,
                        nextReviewTimeMillis = now - 1000L,
                        createdAtMillis = now
                    ),
                    Flashcard(
                        type = FlashcardTypes.PRONUNCIATION,
                        word = "Ubiquitous",
                        stageIndex = 0,
                        nextReviewTimeMillis = now - 500L,
                        createdAtMillis = now
                    ),
                    Flashcard(
                        type = FlashcardTypes.CONVERSATION_LINGUISTIC,
                        word = "Conditional Inversion: 'Were it not for...'",
                        stageIndex = 0,
                        nextReviewTimeMillis = now - 100L,
                        createdAtMillis = now,
                        conversationHistoryJson = geminiRepo.serializeChat(sampleChat),
                        focusSummary = "Grammatical structure for polite emphasis in formal discussions"
                    )
                )
                flashcardDao.insertAll(initialCards)
            }
        }
    }

    fun addFlashcard(
        type: String,
        word: String,
        imageUrl: String? = null,
        conversationHistory: List<ChatMessage>? = null,
        focusSummary: String? = null
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val initialDue = now + SrsStage.TWO_MINUTES.durationMillis
            val card = Flashcard(
                type = type,
                word = word.trim(),
                imageUrl = imageUrl,
                stageIndex = 0,
                nextReviewTimeMillis = initialDue,
                createdAtMillis = now,
                conversationHistoryJson = conversationHistory?.let { geminiRepo.serializeChat(it) },
                focusSummary = focusSummary
            )
            flashcardDao.insertCard(card)
            clearCreationChat()
        }
    }

    fun deleteCard(card: Flashcard) {
        viewModelScope.launch {
            flashcardDao.deleteCard(card)
            // If deleting during review, advance or remove from active list
            val updated = _activeReviewList.value.filter { it.id != card.id }
            _activeReviewList.value = updated
            if (_currentReviewIndex.value >= updated.size) {
                _currentReviewIndex.value = (updated.size - 1).coerceAtLeast(0)
            }
            loadActiveCardDetails()
        }
    }

    fun reviewCard(card: Flashcard, grade: ReviewGrade) {
        viewModelScope.launch {
            val nextStageIndex = SpacedRepetitionUtils.calculateNextStage(card.stageIndex, grade)
            val nextTime = SpacedRepetitionUtils.calculateNextReviewTime(nextStageIndex)
            val updatedCard = card.copy(
                stageIndex = nextStageIndex,
                nextReviewTimeMillis = nextTime
            )
            flashcardDao.updateCard(updatedCard)

            val nextIdx = _currentReviewIndex.value + 1
            _currentReviewIndex.value = nextIdx
            loadActiveCardDetails()
        }
    }

    // --- Creation Chat Methods ---
    fun sendCreationMessage(text: String, imageBitmap: Bitmap? = null) {
        val userMsg = ChatMessage(
            sender = "user",
            text = text,
            imageBase64 = imageBitmap?.let { GeminiRepository.bitmapToBase64(it) }
        )
        val updated = _creationChatMessages.value + userMsg
        _creationChatMessages.value = updated

        _isCreationThinking.value = true
        viewModelScope.launch {
            val result = geminiRepo.chatTurn(
                history = updated,
                systemInstruction = "You are an expert tutor helping a user pinpoint what they want to remember. They may focus on linguistic elements (grammar, vocabulary in context) or technical topics (anatomy, engineering mechanisms, processes). Help refine their target concept clearly and concisely."
            )
            _isCreationThinking.value = false
            result.onSuccess { reply ->
                val aiMsg = ChatMessage(sender = "model", text = reply)
                _creationChatMessages.value = _creationChatMessages.value + aiMsg
            }.onFailure { err ->
                val errorMsg = ChatMessage(sender = "model", text = "Note: ${err.localizedMessage ?: "Could not reach Gemini."} You can still tap Linguistic or Technical to save this item.")
                _creationChatMessages.value = _creationChatMessages.value + errorMsg
            }
        }
    }

    fun clearCreationChat() {
        _creationChatMessages.value = emptyList()
        _isCreationThinking.value = false
    }

    // --- Review Session Methods ---
    fun startReviewSession(stageIndex: Int? = null) {
        val now = System.currentTimeMillis()
        val cards = allCards.value
        val targetCards = if (stageIndex != null) {
            cards.filter { it.stageIndex == stageIndex }
        } else {
            cards.filter { it.nextReviewTimeMillis <= now }
        }

        _activeReviewList.value = targetCards
        _currentReviewIndex.value = 0
        loadActiveCardDetails()
    }

    private fun loadActiveCardDetails() {
        val index = _currentReviewIndex.value
        val list = _activeReviewList.value
        if (index !in list.indices) {
            _currentScenarioOrQuestion.value = null
            _reviewChatHistory.value = emptyList()
            return
        }

        val card = list[index]
        _reviewChatHistory.value = emptyList()

        if (card.type == FlashcardTypes.CONVERSATION_LINGUISTIC || card.type == FlashcardTypes.CONVERSATION_TECHNICAL) {
            _reviewScenarioLoading.value = true
            _currentScenarioOrQuestion.value = null

            viewModelScope.launch {
                val history = geminiRepo.deserializeChat(card.conversationHistoryJson)
                val result = if (card.type == FlashcardTypes.CONVERSATION_LINGUISTIC) {
                    geminiRepo.generateLinguisticScenario(history)
                } else {
                    geminiRepo.generateTechnicalQuestion(history)
                }

                _reviewScenarioLoading.value = false
                result.onSuccess { promptText ->
                    _currentScenarioOrQuestion.value = promptText
                    _reviewChatHistory.value = listOf(ChatMessage(sender = "model", text = promptText))
                }.onFailure {
                    val fallback = if (card.type == FlashcardTypes.CONVERSATION_LINGUISTIC) {
                        "Imagine you are in a team discussion where you need to apply '${card.word}'. What would you say?"
                    } else {
                        "Explain the key principles and mechanism behind '${card.word}' as discussed previously."
                    }
                    _currentScenarioOrQuestion.value = fallback
                    _reviewChatHistory.value = listOf(ChatMessage(sender = "model", text = fallback))
                }
            }
        } else {
            _reviewScenarioLoading.value = false
            _currentScenarioOrQuestion.value = null
        }
    }

    fun sendReviewReply(userReply: String) {
        if (userReply.isBlank()) return
        val userMsg = ChatMessage(sender = "user", text = userReply)
        val updated = _reviewChatHistory.value + userMsg
        _reviewChatHistory.value = updated

        val currentCard = _activeReviewList.value.getOrNull(_currentReviewIndex.value) ?: return

        _isReviewReplying.value = true
        viewModelScope.launch {
            val originalContext = geminiRepo.deserializeChat(currentCard.conversationHistoryJson)
            val combinedChat = originalContext + updated

            val systemPrompt = if (currentCard.type == FlashcardTypes.CONVERSATION_LINGUISTIC) {
                "You are an encouraging language tutor evaluating the student's response to your scenario. Highlight if they used the target structure or phrase naturally, offer warm feedback, and suggest slight enhancements if helpful."
            } else {
                "You are a technical mentor evaluating the student's answer. Assess accuracy, validate correct concepts, and constructively correct any misconceptions."
            }

            val result = geminiRepo.chatTurn(combinedChat, systemPrompt)
            _isReviewReplying.value = false
            result.onSuccess { reply ->
                _reviewChatHistory.value = _reviewChatHistory.value + ChatMessage(sender = "model", text = reply)
            }.onFailure {
                _reviewChatHistory.value = _reviewChatHistory.value + ChatMessage(sender = "model", text = "Good effort! Remember to keep the core structure clear in your response.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsHelper.shutdown()
    }
}
