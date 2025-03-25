package com.lavrik.koalajump.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.lavrik.koalajump.GameState

/**
 * Leaderboard screen showing high scores - with fixes for landscape mode
 */
@Composable
fun LeaderboardScreen(
    gameState: GameState,
    navController: NavController,
    onClose: () -> Unit
) {
    // Detect current orientation
    val configuration = LocalConfiguration.current
    val isPortrait = remember(configuration) {
        configuration.screenHeightDp > configuration.screenWidthDp
    }

    // Add scroll state for landscape mode
    val scrollState = rememberScrollState()

    // Create placeholder scores for now
    // Will be replaced with real data from GameInterface in the future
    val scores = remember {
        if (gameState.achievedHighScores.isEmpty()) {
            // If no real scores yet, use placeholders
            listOf(
                Score("Player 1", 500, "01/03"),
                Score("Player 2", 450, "02/15"),
                Score("Player 3", 400, "02/20"),
                Score("Player 4", 350, "02/25"),
                Score("Player 5", 300, "03/01")
            )
        } else {
            // Use real scores from game state
            gameState.achievedHighScores.mapIndexed { index, score ->
                Score("You", score, "Today")
            }
        }
    }

    // Background gradient
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF673AB7),  // Deep Purple
            Color(0xFF3F51B5)   // Indigo
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        // Main content card with adjusted sizing
        Card(
            modifier = Modifier
                .width(if (isPortrait) 320.dp else 500.dp)
                .heightIn(max = if (isPortrait) 600.dp else 320.dp) // Limit height in landscape
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .then(
                        if (!isPortrait) Modifier.verticalScroll(scrollState)
                        else Modifier
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Leaderboard",
                    fontSize = if (isPortrait) 28.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF673AB7),
                    modifier = Modifier.padding(bottom = if (isPortrait) 16.dp else 8.dp)
                )

                if (scores.isNotEmpty()) {
                    // Scores header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFF9575CD),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rank",
                            fontSize = if (isPortrait) 16.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(0.8f)
                        )

                        Text(
                            text = "Player",
                            fontSize = if (isPortrait) 16.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.weight(2f)
                        )

                        Text(
                            text = "Score",
                            fontSize = if (isPortrait) 16.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )

                        if (!isPortrait) {
                            Text(
                                text = "Date",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Scores list - adjust height for different orientations
                    if (isPortrait) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        ) {
                            itemsIndexed(scores) { index, score ->
                                ScoreRow(
                                    rank = index + 1,
                                    score = score,
                                    isPortrait = isPortrait,
                                    isCurrentUser = score.playerName == "You"
                                )
                            }
                        }
                    } else {
                        // In landscape, use a smaller height for the list
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp) // Smaller height for landscape
                        ) {
                            itemsIndexed(scores) { index, score ->
                                ScoreRow(
                                    rank = index + 1,
                                    score = score,
                                    isPortrait = isPortrait,
                                    isCurrentUser = score.playerName == "You"
                                )
                            }
                        }
                    }
                } else {
                    // No scores yet
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isPortrait) 200.dp else 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No scores yet!\nStart playing to set a high score.",
                            fontSize = if (isPortrait) 18.sp else 16.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Your best score
                if (gameState.highScore.value > 0) {
                    Spacer(modifier = Modifier.height(if (isPortrait) 16.dp else 8.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp), // Ensure minimum height
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8EAF6)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (isPortrait) 16.dp else 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Your Best Score",
                                fontSize = if (isPortrait) 16.sp else 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3F51B5)
                            )

                            Text(
                                text = "${gameState.highScore.value}",
                                fontSize = if (isPortrait) 28.sp else 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3F51B5)
                            )
                        }
                    }
                }

                // Close button - adjusted for landscape
                Spacer(modifier = Modifier.height(if (isPortrait) 24.dp else 12.dp))

                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .width(if (isPortrait) 200.dp else 140.dp)
                        .height(48.dp), // Reduced height slightly
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF673AB7)
                    )
                ) {
                    Text(
                        text = "Back",
                        fontSize = if (isPortrait) 18.sp else 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Data class for score entries
 */
data class Score(
    val playerName: String,
    val score: Int,
    val date: String
)

/**
 * Row in the leaderboard for a single score - adjusted for landscape mode
 */
@Composable
fun ScoreRow(
    rank: Int,
    score: Score,
    isPortrait: Boolean,
    isCurrentUser: Boolean
) {
    val backgroundColor = if (isCurrentUser) {
        Color(0xFFE8EAF6) // Light indigo for current user
    } else if (rank % 2 == 0) {
        Color(0xFFF5F5F5) // Light gray for even rows
    } else {
        Color.White // White for odd rows
    }

    val medalColor = when(rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = if (isPortrait) 12.dp else 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank with medal color for top 3
        Box(
            modifier = Modifier
                .size(if (isPortrait) 28.dp else 24.dp)
                .background(
                    color = if (rank <= 3) medalColor else Color.LightGray,
                    shape = RoundedCornerShape(if (isPortrait) 14.dp else 12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                fontSize = if (isPortrait) 14.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (rank <= 3) Color.White else Color.Black
            )
        }

        // Player name
        Text(
            text = score.playerName,
            fontSize = if (isPortrait) 16.sp else 14.sp,
            fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrentUser) Color(0xFF3F51B5) else Color.Black,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .weight(2f)
                .padding(start = 8.dp)
        )

        // Score
        Text(
            text = "${score.score}",
            fontSize = if (isPortrait) 16.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isCurrentUser) Color(0xFF3F51B5) else Color.Black,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )

        // Date (only in landscape)
        if (!isPortrait) {
            Text(
                text = score.date,
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}