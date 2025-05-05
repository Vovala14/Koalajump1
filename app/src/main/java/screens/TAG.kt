package screens

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.lavrik.koalajump.GameState
import com.lavrik.koalajump.GamePreferences
import com.lavrik.koalajump.GameInterface
import com.lavrik.koalajump.ui.components.AnimatedCloudsBackground
import com.lavrik.koalajump.ui.components.NameInputDialog
import com.lavrik.koalajump.utils.AdManager
import kotlinx.coroutines.delay

private const val TAG = "GameOverScreen"

/**
 * Simplified GameOverScreen with only two working buttons, improved for landscape mode
 */
@Composable
fun GameOverScreen(
    gameState: GameState,
    onRestart: () -> Unit,
    onMainMenu: () -> Unit,
    navController: NavController,
    // Add a parameter for the GameInterface
    gameInterface: GameInterface? = null
) {
    // Debug logging
    Log.d(TAG, "GameOverScreen composing with score ${gameState.finalScore.value}")

    // Detect current orientation
    val configuration = LocalConfiguration.current
    val isPortrait = remember(configuration) {
        configuration.screenHeightDp > configuration.screenWidthDp
    }

    // Add scroll state for landscape mode
    val scrollState = rememberScrollState()

    // Track button press states for visual feedback
    var playAgainPressed by remember { mutableStateOf(false) }
    var mainMenuPressed by remember { mutableStateOf(false) }
    var watchAdPressed by remember { mutableStateOf(false) }

    // Add player name functionality
    val context = LocalContext.current
    val gamePreferences = remember { GamePreferences(context) }
    var showNameDialog by remember { mutableStateOf(false) }

    // Track if score has been submitted
    var scoreSubmitted by remember { mutableStateOf(false) }
    
    // Initialize ad manager
    val activity = LocalContext.current as? androidx.activity.ComponentActivity
    val adManager = remember { AdManager(context) }
    var isShowingAd by remember { mutableStateOf(false) }
    
    // Initialize AdManager on first composition
    LaunchedEffect(Unit) {
        adManager.initialize()
        adManager.updateNetworkStatus()
    }

    // Function to submit score
    fun submitScore(playerName: String) {
        if (scoreSubmitted) return

        Log.d(TAG, "Submitting score ${gameState.finalScore.value} for player: $playerName")

        // Submit to online leaderboard if GameInterface is available
        if (gameInterface != null) {
            gameInterface.submitFinalScore(gameState)
            scoreSubmitted = true
            Log.d(TAG, "Score submitted to online leaderboard")
        } else {
            Log.d(TAG, "GameInterface not available, score only saved locally")
            // Still mark as submitted to prevent duplicate attempts
            scoreSubmitted = true
        }
    }

    // Check if we need to show name dialog on first game over
    LaunchedEffect(Unit) {
        // If player already has a name, submit score immediately
        if (gamePreferences.hasPlayerName()) {
            submitScore(gamePreferences.getPlayerName() ?: "Player")
        } else {
            // Otherwise show the dialog to collect name first
            showNameDialog = true
        }
    }

    // Function to handle watching ad and getting extra life
    fun watchAdForExtraLife() {
        if (activity != null && adManager.isAdReady.value && adManager.isNetworkAvailable.value) {
            watchAdPressed = true
            isShowingAd = true
            
            adManager.showRewardedAd(
                activity = activity,
                onRewarded = {
                    // Add an extra life and restart
                    gameState.addLife()
                    gameState.resetForNewGame()
                    
                    // Navigate to game screen
                    try {
                        navController.navigate("game") {
                            popUpTo("gameOver") { inclusive = true }
                        }
                        Log.d(TAG, "Navigation to game after watching ad")
                    } catch (e: Exception) {
                        Log.e(TAG, "Navigation error after ad: ${e.message}", e)
                    }
                },
                onAdClosed = {
                    isShowingAd = false
                    watchAdPressed = false
                }
            )
        }
    }

    // Sky gradient with animated clouds background
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
        // Add animated clouds to background
        AnimatedCloudsBackground()

        // Main content - use scroll for landscape
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .then(if (!isPortrait) Modifier.verticalScroll(scrollState) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Game Over title - with animated effect
            AnimatedGameOverTitle(isPortrait)

            Spacer(modifier = Modifier.height(if (isPortrait) 32.dp else 16.dp))

            // Layout adjustment for landscape
            if (isPortrait) {
                // Portrait layout (vertical) - unchanged
                PortraitGameOverContent(
                    gameState = gameState,
                    playAgainPressed = playAgainPressed,
                    mainMenuPressed = mainMenuPressed,
                    watchAdPressed = watchAdPressed,
                    adButtonEnabled = adManager.isAdReady.value && adManager.isNetworkAvailable.value && !isShowingAd,
                    onPlayAgain = {
                        playAgainPressed = true
                        gameState.resetForNewGame()
                        try {
                            navController.navigate("game") {
                                popUpTo("gameOver") { inclusive = true }
                            }
                            Log.d(TAG, "Direct navigation to game executed")
                        } catch (e: Exception) {
                            Log.e(TAG, "Navigation error: ${e.message}", e)
                        }
                    },
                    onMainMenu = {
                        mainMenuPressed = true
                        try {
                            navController.navigate("mainMenu") {
                                popUpTo(0) // Pop everything up to the start destination
                            }
                            Log.d(TAG, "Direct navigation to main menu executed")
                        } catch (e: Exception) {
                            Log.e(TAG, "Navigation error: ${e.message}", e)
                        }
                    },
                    onWatchAd = { watchAdForExtraLife() }
                )
            } else {
                // Landscape layout (horizontal)
                LandscapeGameOverContent(
                    gameState = gameState,
                    playAgainPressed = playAgainPressed,
                    mainMenuPressed = mainMenuPressed,
                    watchAdPressed = watchAdPressed,
                    adButtonEnabled = adManager.isAdReady.value && adManager.isNetworkAvailable.value && !isShowingAd,
                    onPlayAgain = {
                        playAgainPressed = true
                        gameState.resetForNewGame()
                        try {
                            navController.navigate("game") {
                                popUpTo("gameOver") { inclusive = true }
                            }
                            Log.d(TAG, "Direct navigation to game executed")
                        } catch (e: Exception) {
                            Log.e(TAG, "Navigation error: ${e.message}", e)
                        }
                    },
                    onMainMenu = {
                        mainMenuPressed = true
                        try {
                            navController.navigate("mainMenu") {
                                popUpTo(0) // Pop everything up to the start destination
                            }
                            Log.d(TAG, "Direct navigation to main menu executed")
                        } catch (e: Exception) {
                            Log.e(TAG, "Navigation error: ${e.message}", e)
                        }
                    },
                    onWatchAd = { watchAdForExtraLife() }
                )
            }
        }

        // Show name input dialog if needed
        if (showNameDialog) {
            NameInputDialog(
                isFirstTime = !gamePreferences.hasPlayerName(),
                initialName = gamePreferences.getPlayerName() ?: "",
                onNameSubmitted = { name ->
                    gamePreferences.setPlayerName(name)
                    showNameDialog = false

                    // Submit score with the new name
                    submitScore(name)
                },
                onDismiss = {
                    // Only allow dismiss if it's not the first time setup
                    if (gamePreferences.hasPlayerName()) {
                        showNameDialog = false
                        // Submit score with existing name if dialog dismissed
                        submitScore(gamePreferences.getPlayerName() ?: "Player")
                    }
                }
            )
        }
    }

    // Reset button visual states
    LaunchedEffect(playAgainPressed) {
        if (playAgainPressed) {
            delay(300)
            playAgainPressed = false
        }
    }

    LaunchedEffect(mainMenuPressed) {
        if (mainMenuPressed) {
            delay(300)
            mainMenuPressed = false
        }
    }
    
    LaunchedEffect(watchAdPressed) {
        if (watchAdPressed && !isShowingAd) {
            delay(300)
            watchAdPressed = false
        }
    }
}

@Composable
private fun PortraitGameOverContent(
    gameState: GameState,
    playAgainPressed: Boolean,
    mainMenuPressed: Boolean,
    watchAdPressed: Boolean,
    adButtonEnabled: Boolean,
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit,
    onWatchAd: () -> Unit
) {
    // Score card
    Card(
        modifier = Modifier
            .width(280.dp)
            .padding(bottom = 32.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xBBFFFFFF) // Semi-transparent white
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Final score
            Text(
                text = "Final Score",
                fontSize = 18.sp,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "${gameState.finalScore.value}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // High score
            Text(
                text = "High Score",
                fontSize = 18.sp,
                color = Color(0xFF9C27B0),
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "${gameState.highScore.value}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )

            // If we have a new high score, show it
            if (gameState.finalScore.value >= gameState.highScore.value) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "NEW HIGH SCORE!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    
    // Watch Ad for Extra Life button
    Button(
        onClick = onWatchAd,
        modifier = Modifier
            .width(220.dp)
            .height(60.dp)
            .scale(if (watchAdPressed) 0.95f else 1f),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFF9800), // Orange
            contentColor = Color.White,
            disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 0.dp
        ),
        enabled = adButtonEnabled
    ) {
        Text(
            text = if (adButtonEnabled) "Watch Ad for Extra Life" else "Ad Not Available",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
    
    Spacer(modifier = Modifier.height(16.dp))

    // Play Again button with direct navigation
    Button(
        onClick = onPlayAgain,
        modifier = Modifier
            .width(220.dp)
            .height(60.dp)
            .scale(if (playAgainPressed) 0.95f else 1f),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4CAF50),
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 0.dp
        )
    ) {
        Text(
            text = "Play Again",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Main Menu button with direct navigation
    Button(
        onClick = onMainMenu,
        modifier = Modifier
            .width(220.dp)
            .height(60.dp)
            .scale(if (mainMenuPressed) 0.95f else 1f),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF9C27B0), // Different color to differentiate
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 0.dp
        )
    ) {
        Text(
            text = "Main Menu",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LandscapeGameOverContent(
    gameState: GameState,
    playAgainPressed: Boolean,
    mainMenuPressed: Boolean,
    watchAdPressed: Boolean,
    adButtonEnabled: Boolean,
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit,
    onWatchAd: () -> Unit
) {
    // Horizontal layout for landscape
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Score card
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xBBFFFFFF) // Semi-transparent white
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Final score
                Text(
                    text = "Final Score",
                    fontSize = 16.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "${gameState.finalScore.value}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // High score
                Text(
                    text = "High Score",
                    fontSize = 16.sp,
                    color = Color(0xFF9C27B0),
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "${gameState.highScore.value}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                // If we have a new high score, show it
                if (gameState.finalScore.value >= gameState.highScore.value) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "NEW HIGH SCORE!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Buttons column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Watch Ad for Extra Life button
            Button(
                onClick = onWatchAd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(if (watchAdPressed) 0.95f else 1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800), // Orange
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 0.dp
                ),
                enabled = adButtonEnabled
            ) {
                Text(
                    text = if (adButtonEnabled) "Watch Ad for Extra Life" else "Ad Not Available",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Play Again button
            Button(
                onClick = onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(if (playAgainPressed) 0.95f else 1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text(
                    text = "Play Again",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Menu button
            Button(
                onClick = onMainMenu,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(if (mainMenuPressed) 0.95f else 1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9C27B0),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text(
                    text = "Main Menu",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Animated "GAME OVER" title that matches MainMenuScreen's animated title style
 */
@Composable
private fun AnimatedGameOverTitle(isPortrait: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "gameOverAnimation")

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
        fontSize = if (isPortrait) 40.sp else 36.sp,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
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