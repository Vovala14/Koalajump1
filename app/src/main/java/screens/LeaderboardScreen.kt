package com.lavrik.koalajump.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Leaderboard screen showing high scores
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
        // Main content card
        Card(
            modifier = Modifier
                .width(if (isPortrait) 320.dp else 500.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Leaderboard",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF673AB7),
                    modifier = Modifier.padding(bottom = 16.dp)
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "Player",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.weight(2f)
                        )

                        Text(
                            text = "Score",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )

                        if (!isPortrait) {
                            Text(
                                text = "Date",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Scores list
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
                    // No scores yet
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No scores yet!\nStart playing to set a high score.",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Your best score
                if (gameState.highScore.value > 0) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8EAF6)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Your Best Score",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3F51B5)
                            )

                            Text(
                                text = "${gameState.highScore.value}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3F51B5)
                            )
                        }
                    }
                }

                // Close button
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onClose,
                    modifier = Modifier.width(200.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF673AB7)
                    )
                ) {
                    Text(
                        text = "Back",
                        fontSize = 18.sp,
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
 * Row in the leaderboard for a single score
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank with medal color for top 3
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = if (rank <= 3) medalColor else Color.LightGray,
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (rank <= 3) Color.White else Color.Black
            )
        }

        // Player name
        Text(
            text = score.playerName,
            fontSize = 16.sp,
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
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isCurrentUser) Color(0xFF3F51B5) else Color.Black,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )

        // Date (only in landscape)
        if (!isPortrait) {
            Text(
                text = score.date,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}