package com.lavrik.koalajump

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lavrik.koalajump.screens.GameOverScreen
import com.lavrik.koalajump.screens.GameScreen
import com.lavrik.koalajump.screens.LeaderboardScreen
import com.lavrik.koalajump.screens.EnhancedMainMenuScreen
import com.lavrik.koalajump.ui.components.KoalaJumpTheme

/**
 * Main activity for the game
 */
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    // Utility classes
    lateinit var bitmapManager: BitmapManager
    lateinit var performanceMonitor: PerformanceMonitor

    // Game state - accessible throughout the app
    private val gameState = GameState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force portrait mode at startup
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Add crash handler for better debugging
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CRASH_HANDLER", "Uncaught exception: ${throwable.message}", throwable)
        }

        Log.d(TAG, "Starting app")

        // Initialize utilities
        bitmapManager = BitmapManager(this)
        performanceMonitor = PerformanceMonitor()

        setContent {
            KoalaJumpTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Pass gameState to the GameNavigation composable
                    GameNavigation(gameState = gameState)
                }
            }
        }

        // Update orientation based on initial setting
        updateOrientation(gameState.getAllowRotation())

        // Listen for orientation changes
        gameState.observeAllowRotation(this) { allowRotation ->
            updateOrientation(allowRotation)
        }

        Log.d(TAG, "App started successfully")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        Log.d(TAG, "Configuration changed, orientation: " +
                if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait")

        // If we're not allowing rotation, force back to portrait
        if (!gameState.getAllowRotation() && newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    /**
     * Update orientation based on user preference
     */
    private fun updateOrientation(allowRotation: Boolean) {
        if (allowRotation) {
            // Allow sensor-based orientation
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            Log.d(TAG, "Orientation unlocked - following sensor")
        } else {
            // Force portrait only
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            Log.d(TAG, "Orientation locked to portrait")
        }
    }

    override fun onResume() {
        super.onResume()
        performanceMonitor.start()
    }

    override fun onPause() {
        super.onPause()
        performanceMonitor.stop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        bitmapManager.clearCache()
    }
}

/**
 * Main navigation component for the game
 * @param gameState The central game state to pass to all screens
 */
@Composable
fun GameNavigation(gameState: GameState) {
    Log.d("GameNavigation", "Starting navigation")

    val navController = rememberNavController()

    // Remember the navigation controller to pass with gameState
    SetupNavigation(navController, gameState)
}

/**
 * Setup navigation routes with proper parameter passing
 */
@Composable
private fun SetupNavigation(navController: NavHostController, gameState: GameState) {
    NavHost(
        navController = navController,
        startDestination = "mainMenu"
    ) {
        composable("mainMenu") {
            Log.d("Navigation", "Showing Main Menu")
            EnhancedMainMenuScreen(
                gameState = gameState,
                onStartGame = {
                    Log.d("Navigation", "Start Game button clicked")
                    try {
                        navController.navigate("game") {
                            popUpTo("mainMenu") { inclusive = false }
                        }
                        Log.d("Navigation", "Successfully navigated to game screen")
                    } catch (e: Exception) {
                        Log.e("Navigation", "Error navigating to game: ${e.message}", e)
                    }
                },
                onShowLeaderboard = {
                    Log.d("Navigation", "Leaderboard button clicked")
                    navController.navigate("leaderboard")
                },
                onToggleOrientation = { allowRotation ->
                    Log.d("Navigation", "Toggling orientation: allow=$allowRotation")
                    gameState.setAllowRotation(allowRotation)
                }
            )
        }

        composable("game") {
            Log.d("Navigation", "Composing Game Screen")
            // Pass gameState and navController to GameScreen
            GameScreen(
                gameState = gameState,
                navController = navController
            )
        }

        composable("gameOver") {
            Log.d("Navigation", "Showing Game Over Screen")
            // Pass all required parameters to GameOverScreen
            GameOverScreen(
                gameState = gameState,
                onRestart = {
                    Log.d("Navigation", "Play Again button clicked")
                    gameState.resetForNewGame()
                    navController.navigate("game") {
                        popUpTo("gameOver") { inclusive = true }
                    }
                },
                onMainMenu = {
                    Log.d("Navigation", "Main Menu button clicked")
                    navController.navigate("mainMenu") {
                        popUpTo("mainMenu") { inclusive = true }
                    }
                },
                navController = navController
            )
        }

        composable("leaderboard") {
            Log.d("Navigation", "Showing Leaderboard")
            // Pass gameState to LeaderboardScreen
            LeaderboardScreen(
                gameState = gameState,
                navController = navController,
                onClose = {
                    Log.d("Navigation", "Back button clicked")
                    navController.popBackStack()
                }
            )
        }
    }
}