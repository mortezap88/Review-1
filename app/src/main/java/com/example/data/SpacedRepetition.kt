package com.example.data

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class SrsStage(
    val title: String,
    val shortName: String,
    val durationMillis: Long,
    val color: Color
) {
    TWO_MINUTES("2 Minutes", "2m", 2 * 60 * 1000L, Color(0xFFFF6384)),
    TWENTY_MINUTES("20 Minutes", "20m", 20 * 60 * 1000L, Color(0xFFE08552)),
    TWO_HOURS("2 Hours", "2h", 2 * 60 * 60 * 1000L, Color(0xFFD4A373)),
    ONE_DAY("1 Day", "1d", 24 * 60 * 60 * 1000L, Color(0xFF2DD4BF)),
    THREE_DAYS("3 Days", "3d", 3 * 24 * 60 * 60 * 1000L, Color(0xFF38BDF8)),
    ONE_WEEK("1 Week", "7d", 7 * 24 * 60 * 60 * 1000L, Color(0xFF60A5FA)),
    TWO_WEEKS("2 Weeks", "14d", 14 * 24 * 60 * 60 * 1000L, Color(0xFF818CF8)),
    ONE_MONTH("1 Month", "30d", 30 * 24 * 60 * 60 * 1000L, Color(0xFFC084FC));

    companion object {
        fun fromIndex(index: Int): SrsStage {
            val safeIndex = index.coerceIn(0, entries.size - 1)
            return entries[safeIndex]
        }
    }
}

data class DayReviewCount(
    val dayLabel: String, // "Today", "Fri", "Sat", etc.
    val count: Int,
    val dateMillis: Long
)

object SpacedRepetitionUtils {
    fun calculateNextReviewTime(currentStageIndex: Int): Long {
        val stage = SrsStage.fromIndex(currentStageIndex)
        return System.currentTimeMillis() + stage.durationMillis
    }

    fun calculateNextStage(currentStageIndex: Int, grade: ReviewGrade): Int {
        return when (grade) {
            ReviewGrade.AGAIN -> 0
            ReviewGrade.HARD -> currentStageIndex
            ReviewGrade.GOOD -> (currentStageIndex + 1).coerceAtMost(SrsStage.entries.size - 1)
            ReviewGrade.EASY -> (currentStageIndex + 2).coerceAtMost(SrsStage.entries.size - 1)
        }
    }

    fun getUpcomingSixDays(cards: List<Flashcard>): List<DayReviewCount> {
        val calendar = Calendar.getInstance()
        // Reset to start of today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val result = mutableListOf<DayReviewCount>()

        for (i in 0 until 6) {
            val dayStart = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = calendar.timeInMillis

            val label = if (i == 0) "Today" else dayFormat.format(Date(dayStart))
            val count = if (i == 0) {
                // Today counts cards due before end of today (including overdue)
                cards.count { it.nextReviewTimeMillis < dayEnd }
            } else {
                // Future days count cards due between start and end of that day
                cards.count { it.nextReviewTimeMillis in dayStart until dayEnd }
            }

            result.add(DayReviewCount(dayLabel = label, count = count, dateMillis = dayStart))
        }

        return result
    }
}

enum class ReviewGrade {
    AGAIN,
    HARD,
    GOOD,
    EASY
}
