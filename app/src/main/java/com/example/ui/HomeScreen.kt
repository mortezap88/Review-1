package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Flashcard
import com.example.data.SpacedRepetitionUtils
import com.example.data.SrsStage
import com.example.ui.theme.AccentPink
import com.example.ui.theme.BadgeGreenBg
import com.example.ui.theme.BadgeGreenBorder
import com.example.ui.theme.BadgeGreenText
import com.example.ui.theme.BgDark
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    cards: List<Flashcard>,
    onOpenAdd: () -> Unit,
    onOpenReview: (stageIndex: Int?) -> Unit
) {
    val upcomingDays by remember(cards) {
        derivedStateOf { SpacedRepetitionUtils.getUpcomingSixDays(cards) }
    }

    val totalDueNow by remember(cards) {
        derivedStateOf {
            val now = System.currentTimeMillis()
            cards.count { it.nextReviewTimeMillis <= now }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BgDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAdd,
                containerColor = PrimaryPurple,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(64.dp)
                    .testTag("add_flashcard_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Flashcard",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Upcoming Reviews Card
            item {
                UpcomingReviewsCard(
                    upcomingDays = upcomingDays,
                    totalDueNow = totalDueNow,
                    onStartAllDue = { onOpenReview(null) }
                )
            }

            // Section Title
            item {
                Text(
                    text = "Spaced Repetition Stages",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontSize = 17.sp
                    ),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // Stages list matching Image 2
            itemsIndexed(SrsStage.entries) { index, stage ->
                val now = System.currentTimeMillis()
                val dueInStage = cards.count { it.stageIndex == index && it.nextReviewTimeMillis <= now }
                val totalInStage = cards.count { it.stageIndex == index }

                StageRowItem(
                    stage = stage,
                    stageIndex = index,
                    dueCount = dueInStage,
                    totalCount = totalInStage,
                    onClick = { onOpenReview(index) }
                )
            }
        }
    }
}

@Composable
fun UpcomingReviewsCard(
    upcomingDays: List<com.example.data.DayReviewCount>,
    totalDueNow: Int,
    onStartAllDue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Upcoming Reviews",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 20.sp
                    )
                )

                if (totalDueNow > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BadgeGreenBg)
                            .border(1.dp, BadgeGreenBorder, RoundedCornerShape(12.dp))
                            .clickable { onStartAllDue() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Review",
                                tint = BadgeGreenText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Start ($totalDueNow)",
                                color = BadgeGreenText,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6-day column strip matching Image 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                upcomingDays.forEachIndexed { idx, day ->
                    DayColumn(
                        dayName = day.dayLabel,
                        count = day.count,
                        isToday = idx == 0
                    )
                }
            }
        }
    }
}

@Composable
fun DayColumn(
    dayName: String,
    count: Int,
    isToday: Boolean
) {
    Box(
        modifier = Modifier
            .width(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isToday) Color(0xFF221C38) else Color(0xFF141026))
            .border(
                1.dp,
                if (isToday) PrimaryPurple.copy(alpha = 0.5f) else Color(0xFF25203D),
                RoundedCornerShape(14.dp)
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayName,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = if (isToday) TextPrimary else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = if (count > 0) TextPrimary else TextMuted,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun StageRowItem(
    stage: SrsStage,
    stageIndex: Int,
    dueCount: Int,
    totalCount: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stage.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = stage.color,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            )

            if (dueCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(BadgeGreenBg)
                        .border(1.dp, BadgeGreenBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$dueCount due",
                        color = BadgeGreenText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            } else if (totalCount > 0) {
                Text(
                    text = "$totalCount total",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        }
    }
}
