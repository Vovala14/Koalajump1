// GameOverScreen.kt with fix
package com.lavrik.koalajump.screens

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
import androidx.activity.ComponentActivity // Import ComponentActivity
import com.google.android.gms.ads.AdError

private const val TAG = "GameOverScreen"

/**
 * Simplified GameOverScreen with ad reward logic handled here.
 * FIXED: Properly handles continuation after ad reward
 */
@Composable
fun GameOverScreen(
    gameState: GameState,
    onRestart: () -> Unit, // Called for NEW GAME via "Play Again" button
    onMainMenu: () -> Unit,
    navController: NavController,
    gameInterface: GameInterface? = null,
    adManager: AdManager? = null
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

    // State for ad display
    var isShowingAd by remember { mutableStateOf(false) }
    // State to track ad availability
    val isAdAvailable by remember { derivedStateOf { adManager?.isAdReady?.value == true && adManager.isNetworkAvailable.value }}

    // Load ad when screen becomes visible
    LaunchedEffect(adManager) {
        adManager?.loadRewardedAd()
        Log.d(TAG, "Attempted to load rewarded ad on GameOverScreen launch.")
    }

    // Function to submit score
    fun submitScore(playerName: String) {
        if (scoreSubmitted) return

        Log.d(TAG, "Submitting score ${gameState.finalScore.value} for player: $playerName")
        // Submit score logic (using GameInterface or local saving)
        gameInterface?.submitFinalScore(gameState) ?: Log.d(TAG, "GameInterface not available, score only saved locally")
        scoreSubmitted = true
        Log.d(TAG, "Score submission processed.")
    }

    // Function to navigate back to game screen after ad reward
    fun continueAfterAdReward() {
        Log.d(TAG, "Continuing game after ad reward with score: ${gameState.score.value}, lives: ${gameState.lives.value}")
        try {
            // Navigate to game screen WITHOUT calling resetForNewGame()
            navController.navigate("game") {
                popUpTo("gameOver") { inclusive = true }
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error when continuing after ad: ${e.message}", e)
        }
    }

    // Function to handle watching ad and getting extra life
    fun watchAdForExtraLife() {
        val activity = context as? ComponentActivity // Use ComponentActivity
        if (activity != null && adManager != null && isAdAvailable && !isShowingAd) {
            watchAdPressed = true
            isShowingAd = true
            Log.i(TAG, "Attempting to show rewarded ad...")

            adManager.showRewardedAd(
                activity = activity,
                onRewarded = {
                    Log.i(TAG, "Rewarded Ad Success! Granting extra life and preparing for continuation.")
                    // Prepare GameState for continuation
                    gameState.addLife() // Grant the life
                    gameState.setTemporaryInvincibility(true) // Grant invincibility

                    // FIXED: Preserve score BEFORE attempting to continue
                    gameState.preserveScoreAndLevel() // Copies finalScore to score

                    // FIXED: Use continueFromAd() method instead of relying on just the flag
                    gameState.continueFromAd()

                    // FIXED: Use our local function for navigation after reward
                    continueAfterAdReward()

                    isShowingAd = false // Reset ad showing state after reward and navigation triggered
                    watchAdPressed = false
                },
                onAdClosed = {
                    Log.d(TAG, "Rewarded Ad Closed.")
                    isShowingAd = false
                    watchAdPressed = false
                    // If closed without reward, just stay on GameOverScreen
                    // Load a new ad for potential next attempt
                    adManager.loadRewardedAd()
                }
            )
        } else {
            Log.w(TAG, "Cannot show ad: Activity=$activity, AdManager=$adManager, isAdAvailable=$isAdAvailable, isShowingAd=$isShowingAd")
            watchAdPressed = false // Reset button press state if ad couldn't be shown
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

    // Sky gradient with animated clouds background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF87CEEB), Color(0xFF5D8AA8))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
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
            AnimatedGameOverTitle(isPortrait)
            Spacer(modifier = Modifier.height(if (isPortrait) 32.dp else 16.dp))

            // Layout adjustment for landscape
            val adButtonEnabled = isAdAvailable && !isShowingAd // Simplified derived state

            if (isPortrait) {
                PortraitGameOverContent(
                    gameState = gameState,
                    playAgainPressed = playAgainPressed,
                    mainMenuPressed = mainMenuPressed,
                    watchAdPressed = watchAdPressed,
                    adButtonEnabled = adButtonEnabled,
                    onPlayAgain = { // This is the "Play Again" button (starts new game)
                        playAgainPressed = true
                        gameState.resetForNewGame() // Full reset for new game
                        // Navigate to game screen for a fresh start
                        try {
                            navController.navigate("game") {
                                popUpTo("gameOver") { inclusive = true } // Remove game over screen
                                launchSingleTop = true
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Navigation error on Play Again: ${e.message}", e)
                        }
                    },
                    onMainMenu = { // Main menu button
                        mainMenuPressed = true
                        try {
                            navController.navigate("mainMenu") {
                                popUpTo(0) // Pop everything up to the start destination
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Navigation error on Main Menu: ${e.message}", e)
                        }
                    },
                    onWatchAd = { // Watch Ad button
                        watchAdForExtraLife()
                    }
                )
            } else {
                LandscapeGameOverContent(
                    gameState = gameState,
                    playAgainPressed = playAgainPressed,
                    mainMenuPressed = mainMenuPressed,
                    watchAdPressed = watchAdPressed,
                    adButtonEnabled = adButtonEnabled,
                    onPlayAgain = { // Play Again button (new game)
                        playAgainPressed = true
                        gameState.resetForNewGame()
                        try {
                            navController.navigate("game") {
                                popUpTo("gameOver") { inclusive = true }
                                launchSingleTop = true
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Navigation error on Play Again (Landscape): ${e.message}", e)
                        }
                    },
                    onMainMenu = { // Main Menu button
                        mainMenuPressed = true
                        try {
                            navController.navigate("mainMenu") {
                                popUpTo(0)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Navigation error on Main Menu (Landscape): ${e.message}", e)
                        }
                    },
                    onWatchAd = { // Watch Ad button
                        watchAdForExtraLife()
                    }
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
                    submitScore(name) // Submit score with the new name
                },
                onDismiss = {
                    // Only allow dismiss if it's not the first time setup
                    if (gamePreferences.hasPlayerName()) {
                        showNameDialog = false
                        submitScore(gamePreferences.getPlayerName() ?: "Player") // Submit with existing name
                    } else {
                        // If first time and dismissed, submit as "Player"
                        gamePreferences.setPlayerName("Player") // Set default name
                        showNameDialog = false
                        submitScore("Player")
                    }
                }
            )
        }
    } // End Root Box

    // Reset button visual states
    LaunchedEffect(playAgainPressed) { if (playAgainPressed) { delay(200); playAgainPressed = false } }
    LaunchedEffect(mainMenuPressed) { if (mainMenuPressed) { delay(200); mainMenuPressed = false } }
    LaunchedEffect(watchAdPressed) { if (watchAdPressed && !isShowingAd) { delay(200); watchAdPressed = false } }
}


// --- Portrait Content ---
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
        modifier = Modifier.width(280.dp).padding(bottom = 32.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCCFFFFFF)), // Slightly more opaque
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Final Score", fontSize = 18.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
            Text("${gameState.finalScore.value}", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
            Spacer(modifier = Modifier.height(16.dp))
            Text("High Score", fontSize = 18.sp, color = Color(0xFF9C27B0), fontWeight = FontWeight.Medium)
            Text("${gameState.highScore.value}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))

            if (gameState.finalScore.value > 0 && gameState.finalScore.value >= gameState.highScore.value) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("NEW HIGH SCORE!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800), textAlign = TextAlign.Center)
            }
        }
    }

    // --- Buttons ---
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp) // Consistent spacing
    ) {
        // Watch Ad Button
        Button(
            onClick = onWatchAd,
            modifier = Modifier.width(240.dp).height(60.dp).scale(if (watchAdPressed) 0.95f else 1f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9800), // Orange
                contentColor = Color.White,
                disabledContainerColor = Color.Gray.copy(alpha = 0.5f),
                disabledContentColor = Color.White.copy(alpha = 0.7f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp),
            enabled = adButtonEnabled
        ) {
            Text(
                text = if (adButtonEnabled) "Watch Ad for Extra Life" else "Ad Unavailable",
                fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
        }

        // Play Again Button (New Game)
        Button(
            onClick = onPlayAgain,
            modifier = Modifier.width(220.dp).height(60.dp).scale(if (playAgainPressed) 0.95f else 1f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), contentColor = Color.White),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp)
        ) {
            Text("Play Again", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Main Menu Button
        Button(
            onClick = onMainMenu,
            modifier = Modifier.width(220.dp).height(60.dp).scale(if (mainMenuPressed) 0.95f else 1f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF795548), contentColor = Color.White), // Brown color
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp)
        ) {
            Text("Main Menu", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}


// --- Landscape Content ---
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Score card (Left)
        Card(
            modifier = Modifier.weight(1f).padding(end = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xCCFFFFFF)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Final Score", fontSize = 16.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
                Text("${gameState.finalScore.value}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                Spacer(modifier = Modifier.height(8.dp))
                Text("High Score", fontSize = 16.sp, color = Color(0xFF9C27B0), fontWeight = FontWeight.Medium)
                Text("${gameState.highScore.value}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))

                if (gameState.finalScore.value > 0 && gameState.finalScore.value >= gameState.highScore.value) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("NEW HIGH SCORE!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800), textAlign = TextAlign.Center)
                }
            }
        }

        // Buttons column (Right)
        Column(
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp) // Consistent spacing
        ) {
            // Watch Ad Button
            Button(
                onClick = onWatchAd,
                modifier = Modifier.fillMaxWidth().height(56.dp).scale(if (watchAdPressed) 0.95f else 1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800), contentColor = Color.White,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp),
                enabled = adButtonEnabled
            ) {
                Text(
                    text = if (adButtonEnabled) "Watch Ad for Extra Life" else "Ad Unavailable",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                )
            }

            // Play Again Button
            Button(
                onClick = onPlayAgain,
                modifier = Modifier.fillMaxWidth().height(56.dp).scale(if (playAgainPressed) 0.95f else 1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp)
            ) {
                Text("Play Again", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            // Main Menu Button
            Button(
                onClick = onMainMenu,
                modifier = Modifier.fillMaxWidth().height(56.dp).scale(if (mainMenuPressed) 0.95f else 1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF795548), contentColor = Color.White), // Brown color
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp)
            ) {
                Text("Main Menu", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// --- Animated Title ---
@Composable
private fun AnimatedGameOverTitle(isPortrait: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "gameOverTitleAnim")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "titleScale"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = -1.5f, targetValue = 1.5f, // Subtle rotation
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
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
                color = Color(0xAA000000), // Darker shadow
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