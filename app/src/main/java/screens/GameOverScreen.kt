package com.lavrik.koalajump.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.lavrik.koalajump.GameState
import kotlinx.coroutines.delay
// Animation imports
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer

// Constants for UI elements
private val TITLE_FONT_SIZE = 40.sp
private val SCORE_BOX_SIZE = 120.dp
private val SCORE_LABEL_SIZE = 16.sp
private val SCORE_VALUE_SIZE = 32.sp
private val HIGH_SCORE_SIZE = 20.sp
private val BUTTON_HEIGHT = 56.dp
private val BUTTON_FONT_SIZE = 18.sp
private val LANDSCAPE_SCORE_BOX_SIZE = 100.dp

/**
 * Game over screen showing final score and options to restart or return to menu
 */
@Composable
fun GameOverScreen(
    gameState: GameState,
    onRestart: () -> Unit,
    onMainMenu: () -> Unit,
    navController: NavController
) {
    // Get score from game state - FIXED
    val finalScore = gameState.finalScore.value
    val highScore = gameState.highScore.value

    // Debug logging to troubleshoot
    Log.d("GameOverScreen", "Final Score: $finalScore, High Score: $highScore")

    // Animation states
    var showScoreAnimation by remember { mutableStateOf(false) }
    var showHighScoreAnimation by remember { mutableStateOf(false) }
    var showButtons by remember { mutableStateOf(false) }

    // Detect current orientation
    val configuration = LocalConfiguration.current
    val isPortrait = remember(configuration) {
        configuration.screenHeightDp > configuration.screenWidthDp
    }

    // Gradual animation sequence
    LaunchedEffect(Unit) {
        delay(300)
        showScoreAnimation = true
        delay(1000)
        showHighScoreAnimation = true
        delay(500)
        showButtons = true
    }

    // Use white background instead of gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (isPortrait) {
            // Portrait layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Game Over title
                GameOverTitle()

                Spacer(modifier = Modifier.height(16.dp))

                // Score display with animation
                AnimatedVisibility(
                    visible = showScoreAnimation,
                    initiallyVisible = false
                ) {
                    ScoreBox(
                        score = finalScore, // FIXED to use finalScore
                        label = "Your Score",
                        size = SCORE_BOX_SIZE,
                        backgroundColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                    )
                }

                // High Score Display with animation
                AnimatedVisibility(
                    visible = showHighScoreAnimation,
                    initiallyVisible = false
                ) {
                    // Changed to use ScoreBox for high score too
                    ScoreBox(
                        score = highScore,
                        label = "Best Score",
                        size = SCORE_BOX_SIZE,
                        backgroundColor = Color(0xFF9C27B0).copy(alpha = 0.2f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action buttons with animation
                AnimatedVisibility(
                    visible = showButtons,
                    initiallyVisible = false
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Restart Button
                        ActionButton(
                            text = "Play Again",
                            onClick = onRestart,
                            modifier = Modifier.fillMaxWidth(0.7f)
                        )

                        // Main Menu Button
                        ActionButton(
                            text = "Main Menu",
                            onClick = onMainMenu,
                            modifier = Modifier.fillMaxWidth(0.7f),
                            color = Color(0xFF9C27B0) // Purple
                        )
                    }
                }
            }
        } else {
            // Landscape layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - Game Over and Score
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GameOverTitle(fontSize = 36.sp, color = Color.Black)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Score display in landscape - horizontal arrangement
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AnimatedVisibility(
                            visible = showScoreAnimation,
                            initiallyVisible = false
                        ) {
                            // FIXED: Score Box - use game colored background
                            ScoreBox(
                                score = finalScore, // FIXED to use finalScore
                                label = "Your Score",
                                size = LANDSCAPE_SCORE_BOX_SIZE,
                                labelSize = 14.sp,
                                valueSize = 28.sp,
                                backgroundColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                            )
                        }

                        AnimatedVisibility(
                            visible = showHighScoreAnimation,
                            initiallyVisible = false
                        ) {
                            // High Score Box - use a different color
                            ScoreBox(
                                score = highScore,
                                label = "Best Score",
                                size = LANDSCAPE_SCORE_BOX_SIZE,
                                labelSize = 14.sp,
                                valueSize = 28.sp,
                                backgroundColor = Color(0xFF9C27B0).copy(alpha = 0.2f)
                            )
                        }
                    }
                }

                // Right side - Buttons
                AnimatedVisibility(
                    visible = showButtons,
                    initiallyVisible = false
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Restart Button
                        ActionButton(
                            text = "Play Again",
                            onClick = onRestart,
                            modifier = Modifier.fillMaxWidth(0.8f)
                        )

                        // Main Menu Button
                        ActionButton(
                            text = "Main Menu",
                            onClick = onMainMenu,
                            modifier = Modifier.fillMaxWidth(0.8f),
                            color = Color(0xFF9C27B0) // Purple
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated visibility effect with custom implementation
 */
@Composable
fun AnimatedVisibility(
    visible: Boolean,
    initiallyVisible: Boolean = true,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        label = "alphaAnimation"
    )

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        label = "scaleAnimation"
    )

    if (initiallyVisible || visible) {
        Box(
            modifier = Modifier
                .graphicsLayer(
                    alpha = alpha,
                    scaleX = scale,
                    scaleY = scale
                )
        ) {
            content()
        }
    }
}

/**
 * Game Over title
 */
@Composable
fun GameOverTitle(
    fontSize: androidx.compose.ui.unit.TextUnit = TITLE_FONT_SIZE,
    color: Color = Color.Black
) {
    Text(
        text = "GAME OVER",
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = color
    )
}

/**
 * Score display box
 */
@Composable
fun ScoreBox(
    score: Int,
    label: String = "Score",
    size: androidx.compose.ui.unit.Dp = SCORE_BOX_SIZE,
    labelSize: androidx.compose.ui.unit.TextUnit = SCORE_LABEL_SIZE,
    valueSize: androidx.compose.ui.unit.TextUnit = SCORE_VALUE_SIZE,
    backgroundColor: Color = Color(0xFF4CAF50).copy(alpha = 0.2f)
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(backgroundColor, RoundedCornerShape(size / 2))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = labelSize,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = score.toString(),
                fontSize = valueSize,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

/**
 * Action button component
 */
@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4CAF50) // Default green
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(BUTTON_HEIGHT),
        colors = ButtonDefaults.buttonColors(
            containerColor = color
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            fontSize = BUTTON_FONT_SIZE,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}