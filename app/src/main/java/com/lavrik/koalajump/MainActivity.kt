package com.lavrik.koalajump

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.MobileAds
import com.lavrik.koalajump.screens.*
import com.lavrik.koalajump.ui.components.KoalaJumpTheme
import com.lavrik.koalajump.utils.AdManager


/**
 * Main activity for the game - now portrait only
 */
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    // Utility classes
    lateinit var bitmapManager: BitmapManager
    lateinit var performanceMonitor: PerformanceMonitor
    lateinit var gameInterface: GameInterface
    lateinit var adManager: AdManager

    // Game state - accessible throughout the app
    private lateinit var gameState: GameState

    // Double back press to exit
    private var backPressedOnce = false

    // Track current screen
    private var currentScreen = "mainMenu"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force portrait mode always
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Add crash handler for better debugging
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CRASH_HANDLER", "Uncaught exception: ${throwable.message}", throwable)
        }

        Log.d(TAG, "Starting app")

        // Initialize utilities
        bitmapManager = BitmapManager(this)
        performanceMonitor = PerformanceMonitor()
        gameInterface = GameInterface(this, this)
        adManager = AdManager(this)

        // Initialize AdMob
        initializeAdMob()

        // Initialize game state with context
        gameState = GameState(this)

        // FIXED: Create GamePreferences instance and verify high score integrity
        val gamePreferences = GamePreferences(this)
        gamePreferences.verifyHighScore()

        // Log the current high score for debugging
        Log.d(TAG, "App started with high score: ${gameState.highScore.value}")

        // Check for first launch
        val prefs = getSharedPreferences("game_prefs", MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("first_launch", true)

        if (isFirstLaunch) {
            // Set first_launch to false for next time
            prefs.edit().putBoolean("first_launch", false).apply()
        }

        setContent {
            KoalaJumpTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Pass gameState, showTutorial flag, and gameInterface to GameNavigation
                    GameNavigation(
                        gameState = gameState,
                        showTutorial = isFirstLaunch,
                        onScreenChange = { screen ->
                            // Update current screen
                            currentScreen = screen
                        },
                        gameInterface = gameInterface, // Pass gameInterface directly here
                        adManager = adManager
                    )
                }
            }
        }

        Log.d(TAG, "App started successfully")
    }

    /**
     * Initialize AdMob
     */
    private fun initializeAdMob() {
        try {
            // Initialize AdMob
            MobileAds.initialize(this) { initializationStatus ->
                val statusMap = initializationStatus.adapterStatusMap
                for (adapterClass in statusMap.keys) {
                    val status = statusMap[adapterClass]
                    Log.d(TAG, "Adapter name: ${adapterClass}, Description: ${status?.description}, " +
                            "Latency: ${status?.latency}")
                }

                // Initialize the AdManager after AdMob is ready
                adManager.initialize()
            }

            Log.d(TAG, "AdMob initialization started")
        } catch (e: Exception) {
            Log.e(TAG, "AdMob initialization error: ${e.message}", e)
        }
    }

    /**
     * Handle back button press with double-press to exit
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // FIXED: Implement double back press to exit
        if (backPressedOnce) {
            super.onBackPressed()
            return
        }

        // First press shows toast
        backPressedOnce = true
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()

        // Reset after 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            backPressedOnce = false
        }, 2000)
    }

    override fun onResume() {
        super.onResume()
        performanceMonitor.start()

        // Update ad network status
        adManager.updateNetworkStatus()
    }

    override fun onPause() {
        super.onPause()
        performanceMonitor.stop()

        // FIXED: Make sure high score is saved when app is paused
        if (gameState.highScore.value > 0) {
            val prefs = getSharedPreferences("game_prefs", MODE_PRIVATE)
            prefs.edit().putInt("high_score", gameState.highScore.value).commit()

            // Also save to backup
            val backupPrefs = getSharedPreferences("backup_game_prefs", MODE_PRIVATE)
            backupPrefs.edit().putInt("backup_high_score", gameState.highScore.value).commit()

            Log.d(TAG, "High score backed up on pause: ${gameState.highScore.value}")
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        bitmapManager.clearCache()
    }
}

/**
 * Main navigation component for the game
 * @param gameState The central game state to pass to all screens
 * @param showTutorial Flag indicating if the tutorial should be shown
 * @param onScreenChange Callback when navigation changes screens
 * @param gameInterface The interface for leaderboard/score submission
 * @param adManager The manager for AdMob ads
 */
@Composable
fun GameNavigation(
    gameState: GameState,
    showTutorial: Boolean,
    onScreenChange: (String) -> Unit,
    gameInterface: GameInterface,
    adManager: AdManager
) {
    Log.d("GameNavigation", "Starting navigation")

    val navController = rememberNavController()
    val tutorialCompleted = remember { mutableStateOf(!showTutorial) }

    // Track current navigation destination
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "mainMenu"

    // Monitor route changes
    LaunchedEffect(currentRoute) {
        onScreenChange(currentRoute)
    }

    // Remember the navigation controller to pass with gameState
    SetupNavigation(navController, gameState, tutorialCompleted, gameInterface, adManager)
}

/**
 * Setup navigation routes with proper parameter passing
 */
@Composable
private fun SetupNavigation(
    navController: NavHostController,
    gameState: GameState,
    tutorialCompleted: MutableState<Boolean>,
    gameInterface: GameInterface,
    adManager: AdManager
) {
    NavHost(
        navController = navController,
        startDestination = "mainMenu"
    ) {
        composable("mainMenu") {
            Log.d("Navigation", "Showing Main Menu")
            Box {
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
                    onShowEnvironments = {
                        Log.d("Navigation", "Environments Guide button clicked")
                        navController.navigate("environments")
                    }
                )

                // Show tutorial overlay if needed
                if (!tutorialCompleted.value) {
                    var tutorialStep by remember { mutableStateOf(0) }

                    TutorialOverlay(
                        currentStep = tutorialStep,
                        onNextStep = { tutorialStep++ },
                        onFinish = { tutorialCompleted.value = true }
                    )
                }
            }
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
            // Pass all required parameters to GameOverScreen including GameInterface and AdManager
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
                navController = navController,
                gameInterface = gameInterface, // Pass GameInterface directly
                adManager = adManager // Pass AdManager directly
            )
        }

        composable("leaderboard") {
            Log.d("Navigation", "Showing Leaderboard")
            // Pass gameState to LeaderboardScreen, remove gameInterface
            LeaderboardScreen(
                gameState = gameState,
                navController = navController,
                onClose = {
                    Log.d("Navigation", "Back button clicked")
                    navController.popBackStack()
                }
            )
        }

        composable("environments") {
            Log.d("Navigation", "Showing Environments Guide")
            EnvironmentsGuideScreen(
                navController = navController,
                onClose = {
                    Log.d("Navigation", "Environments Guide closed")
                    navController.popBackStack()
                }
            )
        }

        // FIXED: Use correct reference to SignInScreen
        composable("signIn") {
            Log.d("Navigation", "Showing Sign In Screen")

            // Use fully qualified name with the correct import
            screens.SignInScreen(
                gameState = gameState,
                navController = navController,
                onClose = {
                    Log.d("Navigation", "Sign In closed")
                    navController.popBackStack()
                }
            )
        }
    }
}