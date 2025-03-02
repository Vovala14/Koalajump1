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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lavrik.koalajump.screens.GameOverScreen
import com.lavrik.koalajump.screens.GameScreen
import com.lavrik.koalajump.screens.LeaderboardScreen
import com.lavrik.koalajump.screens.MainMenuScreen
import com.lavrik.koalajump.ui.theme.KoalaJumpTheme
// Use classes directly without renaming
import android.graphics.BitmapFactory
import android.util.LruCache
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ===== DIRECTLY EMBED THE BITMAP MANAGER HERE TO AVOID IMPORT ISSUES =====
/**
 * Simple utility to cache and load bitmaps
 */
class BitmapCache(private val context: ComponentActivity) {
    private val TAG = "BitmapCache"

    // Create a memory cache
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8

    private val bitmapCache = LruCache<Int, Bitmap>(cacheSize)

    suspend fun loadBitmap(resourceId: Int): Bitmap {
        // Check if already in cache
        bitmapCache.get(resourceId)?.let {
            return it
        }

        // Load bitmap
        return withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
            bitmapCache.put(resourceId, bitmap)
            bitmap
        }
    }

    fun clearCache() {
        bitmapCache.evictAll()
    }
}

// ===== DIRECTLY EMBED THE PERFORMANCE TRACKER HERE TO AVOID IMPORT ISSUES =====
/**
 * Simple FPS tracker
 */
class PerformanceMonitor {
    private val TAG = "PerformanceMonitor"
    private var frameCount = 0
    private var lastTime = System.currentTimeMillis()
    private var fps = 0

    fun countFrame() {
        frameCount++

        val now = System.currentTimeMillis()
        val elapsed = now - lastTime

        if (elapsed >= 1000) {
            fps = (frameCount * 1000 / elapsed).toInt()
            frameCount = 0
            lastTime = now

            Log.d(TAG, "FPS: $fps")
        }
    }

    fun getFps(): Int = fps
}

class MainActivity : ComponentActivity() {
    // Embedded classes instead of imported ones
    lateinit var bitmapCache: BitmapCache
    lateinit var performanceMonitor: PerformanceMonitor

    // Track current orientation
    private var isPortraitMode = true

    // User preference for orientation locking
    private var orientationLocked = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force portrait mode at startup
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        isPortraitMode = true

        // Add crash handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CRASH_HANDLER", "Uncaught exception: ${throwable.message}", throwable)
        }

        Log.d("MainActivity", "Starting app")

        // Initialize utilities
        bitmapCache = BitmapCache(this)
        performanceMonitor = PerformanceMonitor()

        setContent {
            KoalaJumpTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Pass the orientation toggle function to GameNavigation
                    GameNavigation(
                        toggleOrientationLock = { locked ->
                            orientationLocked = locked
                            updateOrientation()
                        }
                    )
                }
            }
        }

        Log.d("MainActivity", "App started successfully")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // If we're not locking orientation, allow the change
        if (!orientationLocked) {
            isPortraitMode = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
            Log.d("MainActivity", "Orientation changed to: ${if (isPortraitMode) "portrait" else "landscape"}")
        } else {
            // If we're locking, force back to portrait
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    // Method to update orientation based on current settings
    private fun updateOrientation() {
        if (orientationLocked) {
            // Force portrait if locked
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            isPortraitMode = true
            Log.d("MainActivity", "Orientation locked to portrait")
        } else {
            // Allow sensor-based orientation
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            Log.d("MainActivity", "Orientation unlocked - following sensor")
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        bitmapCache.clearCache()
    }
}

@Composable
fun GameNavigation(toggleOrientationLock: (Boolean) -> Unit = {}) {
    Log.d("GameNavigation", "Starting navigation")

    val navController = rememberNavController()
    val gameState = remember { GameState() }

    NavHost(
        navController = navController,
        startDestination = "mainMenu"
    ) {
        composable("mainMenu") {
            Log.d("Navigation", "Showing Main Menu")
            MainMenuScreen(
                onStartGame = {
                    Log.d("Navigation", "Start Game button clicked")
                    gameState.resetForNewGame()
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
                onToggleOrientation = {
                    // Pass orientation lock toggle to MainActivity
                    toggleOrientationLock(!it) // invert: true = portrait only, false = allow rotation
                }
            )
        }

        composable("game") {
            Log.d("Navigation", "Composing Game Screen")
            GameScreen(
                gameState = gameState,
                navController = navController
            )
        }

        composable("gameOver") {
            Log.d("Navigation", "Showing Game Over Screen")
            GameOverScreen(
                score = gameState.finalScore.value,
                highScore = gameState.highScore.value,
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
            LeaderboardScreen(
                navController = navController,
                onClose = {
                    Log.d("Navigation", "Back button clicked")
                    navController.popBackStack()
                }
            )
        }
    }
}