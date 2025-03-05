package com.lavrik.koalajump.screens

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.lavrik.koalajump.GameState
import com.lavrik.koalajump.ui.components.AnimatedCloudsBackground
import kotlinx.coroutines.delay

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
 * Enhanced Game over screen showing final score with beautiful visual effects
 */
@Composable
fun GameOverScreen(
    gameState: GameState,
    onRestart: () -> Unit,
    onMainMenu: () -> Unit,
    navController: NavController
) {
    // Get score from game state
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

    // Use the same sky gradient as MainMenu with animated clouds
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF87CEEB),  // Sky blue at top
                        Color(0xFF5D8AA8)   // Deeper blue at bottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Add animated clouds to background (same as MainMenu)
        AnimatedCloudsBackground()

        if (isPortrait) {
            // Portrait layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Game Over title with animation
                EnhancedGameOverTitle()

                Spacer(modifier = Modifier.height(16.dp))

                // Score display with animation
                AnimatedVisibility(
                    visible = showScoreAnimation,
                    initiallyVisible = false
                ) {
                    EnhancedScoreBox(
                        score = finalScore,
                        label = "Your Score",
                        backgroundColor = Color(0xFF4CAF50),
                        size = SCORE_BOX_SIZE
                    )
                }

                // High Score Display with animation
                AnimatedVisibility(
                    visible = showHighScoreAnimation,
                    initiallyVisible = false
                ) {
                    EnhancedScoreBox(
                        score = highScore,
                        label = "Best Score",
                        backgroundColor = Color(0xFF9C27B0),
                        size = SCORE_BOX_SIZE
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
                        EnhancedActionButton(
                            text = "Play Again",
                            onClick = onRestart,
                            modifier = Modifier.width(220.dp)
                        )

                        // Main Menu Button
                        EnhancedActionButton(
                            text = "Main Menu",
                            onClick = onMainMenu,
                            modifier = Modifier.width(220.dp),
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
                    EnhancedGameOverTitle(fontSize = 36.sp)

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
                            EnhancedScoreBox(
                                score = finalScore,
                                label = "Your Score",
                                size = LANDSCAPE_SCORE_BOX_SIZE,
                                labelSize = 14.sp,
                                valueSize = 28.sp,
                                backgroundColor = Color(0xFF4CAF50)
                            )
                        }

                        AnimatedVisibility(
                            visible = showHighScoreAnimation,
                            initiallyVisible = false
                        ) {
                            EnhancedScoreBox(
                                score = highScore,
                                label = "Best Score",
                                size = LANDSCAPE_SCORE_BOX_SIZE,
                                labelSize = 14.sp,
                                valueSize = 28.sp,
                                backgroundColor = Color(0xFF9C27B0)
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
                        EnhancedActionButton(
                            text = "Play Again",
                            onClick = onRestart,
                            modifier = Modifier.width(200.dp)
                        )

                        // Main Menu Button
                        EnhancedActionButton(
                            text = "Main Menu",
                            onClick = onMainMenu,
                            modifier = Modifier.width(200.dp),
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
 * Enhanced Game Over title with animation effects
 */
@Composable
fun EnhancedGameOverTitle(
    fontSize: androidx.compose.ui.unit.TextUnit = TITLE_FONT_SIZE
) {
    // Animation effects similar to main menu title
    val infiniteTransition = rememberInfiniteTransition(label = "titleAnimation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleScale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleRotation"
    )

    Text(
        text = "GAME OVER",
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        style = androidx.compose.ui.text.TextStyle(
            shadow = androidx.compose.ui.graphics.Shadow(
                color = Color(0x99000000),
                offset = androidx.compose.ui.geometry.Offset(3f, 3f),
                blurRadius = 5f
            )
        ),
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            rotationZ = rotation
        }
    )
}

/**
 * Enhanced Score box with card design
 */
@Composable
fun EnhancedScoreBox(
    score: Int,
    label: String = "Score",
    size: androidx.compose.ui.unit.Dp = SCORE_BOX_SIZE,
    labelSize: androidx.compose.ui.unit.TextUnit = SCORE_LABEL_SIZE,
    valueSize: androidx.compose.ui.unit.TextUnit = SCORE_VALUE_SIZE,
    backgroundColor: Color = Color(0xFF4CAF50)
) {
    // Animation for the score card
    val infiniteTransition = rememberInfiniteTransition(label = "scoreBoxAnimation")
    val elevation by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "elevationAnimation"
    )

    Card(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                shadowElevation = elevation
            },
        shape = RoundedCornerShape(size / 4),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    fontSize = labelSize,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = score.toString(),
                    fontSize = valueSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Enhanced Action button with animation effects
 */
@Composable
fun EnhancedActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4CAF50) // Default green
) {
    var isPressed by remember { mutableStateOf(false) }

    Button(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = modifier
            .height(BUTTON_HEIGHT)
            .scale(if (isPressed) 0.95f else 1f)
            .graphicsLayer {
                shadowElevation = if (isPressed) 0f else 8f
            },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 0.dp
        )
    ) {
        Text(
            text = text,
            fontSize = BUTTON_FONT_SIZE,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }

    // Reset button state after animation
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}