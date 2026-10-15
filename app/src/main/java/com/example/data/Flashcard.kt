package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object FlashcardTypes {
    const val SPELLING = "SPELLING"
    const val PRONUNCIATION = "PRONUNCIATION"
    const val IMAGE = "IMAGE"
    const val CONVERSATION_LINGUISTIC = "CONVERSATION_LINGUISTIC"
    const val CONVERSATION_TECHNICAL = "CONVERSATION_TECHNICAL"
}

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: String, // from FlashcardTypes
    val word: String, // Word, term, or topic
    val imageUrl: String? = null,
    val stageIndex: Int = 0, // 0 to 7
    val nextReviewTimeMillis: Long = System.currentTimeMillis(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val conversationHistoryJson: String? = null, // JSON representation of creation chat
    val focusSummary: String? = null, // Grammar rule, context, or technical concept
    val lastScenarioOrQuestion: String? = null // Generated scenario or question for review
)
