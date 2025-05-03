package com.lavrik.koalajump.screens

import androidx.compose.ui.ExperimentalComposeUiApi
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.MotionEvent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.lavrik.koalajump.GameState
import com.lavrik.koalajump.R
import com.lavrik.koalajump.entities.AnimatedKoala
import com.lavrik.koalajump.game.GameEnvironment
import com.lavrik.koalajump.ui.components.AnimatedCloudsBackground
import com.lavrik.koalajump.ui.components.EnhancedGameHUD
import com.lavrik.koalajump.ui.components.MovingBackgrounds
import com.lavrik.koalajump.utils.EnvironmentAssetManager
import com.lavrik.koalajump.utils.SoundManager
import kotlinx.coroutines.*
import kotlin.math.sin

private const val TAG = "GameScreen"

// Data class for collectibles: x, y, active, isBooster
data class Collectible(val x: Float, val y: Float, val active: Boolean, val isBooster: Boolean)

// Add object scale factor for better visibility
private const val OBJECT_SCALE_FACTOR = 1.8f

/**
 * Simple custom pause button that doesn't rely on material icons
 */
@Composable
fun PauseButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color.White.copy(alpha = 0.7f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Draw custom pause icon (two vertical bars)
        Canvas(modifier = Modifier.size(20.dp)) {
            // Left bar
            drawRect(
                color = Color.Black,
                topLeft = Offset(size.width * 0.25f, size.height * 0.2f),
                size = Size(size.width * 0.15f, size.height * 0.6f),
                style = Fill
            )

            // Right bar
            drawRect(
                color = Color.Black,
                topLeft = Offset(size.width * 0.6f, size.height * 0.2f),
                size = Size(size.width * 0.15f, size.height * 0.6f),
                style = Fill
            )
        }
    }
}

/**
 * Improved GameScreen with proper animation and game loop
 * Now portrait-only with vibration feedback and level transition fixes
 * Enhanced with variable booster duration and end warning
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameScreen(
    gameState: GameState,
    navController: NavController
) {
    // Setup
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Screen dimensions
    val screenWidth = configuration.screenWidthDp.toFloat() * density.density
    val screenHeight = configuration.screenHeightDp.toFloat() * density.density
    val groundY = screenHeight * 0.8f

    // Game state
    var score by remember { mutableStateOf(0) }
    var lives by remember { mutableStateOf(3) }
    var currentLevel by remember { mutableStateOf(1) }
    var hasSpeedBoost by remember { mutableStateOf(false) }
    var isInvincible by remember { mutableStateOf(false) } // Added for invincibility tracking
    var gameRunning by remember { mutableStateOf(true) }
    var invincibleTime by remember { mutableStateOf(0L) } // Invincibility after hit
    var gameSpeed by remember { mutableStateOf(16f) } // Track game speed

    // Booster management
    var isBoosterWarningActive by remember { mutableStateOf(false) }
    var boosterJob: Job? = null

    // Add flag to track when a level change was just detected
    var needsObstacleRepositioning by remember { mutableStateOf(false) }

    // Navigation tracking - prevent multiple navigation attempts
    var navigatedToGameOver by remember { mutableStateOf(false) }

    // Add pause state
    var isGamePaused by remember { mutableStateOf(false) }

    // Add game over state tracking
    var isGameOver by remember { mutableStateOf(false) }

    // Environment transition effect state
    var isTransitioning by remember { mutableStateOf(false) }
    var transitionAlpha by remember { mutableStateOf(0f) }
    var previousEnvironment by remember { mutableStateOf(gameState.currentEnvironment.value) }

    // Environment name display when transitioning (this replaces both notifications)
    var showEnvironmentInfo by remember { mutableStateOf(false) }
    var environmentInfoAlpha by remember { mutableStateOf(0f) }

    // Create the animated koala - portrait mode only
    val koala = remember {
        AnimatedKoala(context, screenWidth, screenHeight)
    }

    // Store koala in GameState for accessing from GameLoop
    LaunchedEffect(koala) {
        gameState.setKoala(koala)
    }

    // Tree, beer, and booster dimensions with scaling factor applied
    val treeWidth = 40f * OBJECT_SCALE_FACTOR
    val treeHeight = 60f * OBJECT_SCALE_FACTOR
    val beerWidth = 30f * OBJECT_SCALE_FACTOR
    val beerHeight = 30f * OBJECT_SCALE_FACTOR
    val boosterWidth = 35f * OBJECT_SCALE_FACTOR
    val boosterHeight = 35f * OBJECT_SCALE_FACTOR

    // Obstacles and collectibles - increased initial spacing
    var obstacles by remember { mutableStateOf(arrayOf(
        screenWidth + 650f,
        screenWidth + 1300f,
        screenWidth + 1950f
    )) }

    // Initial collectibles positioning with correct Y offsets
    val collectibles = remember { mutableStateListOf(
        Collectible(screenWidth + 600f, groundY - 130f, true, false),
        Collectible(screenWidth + 1200f, groundY - 110f, true, false),
        Collectible(screenWidth + 1800f, groundY - 90f, true, true) // This one is a booster
    ) }

    // Load game assets
    val soundManager = remember { SoundManager(context) }

    // Environment asset manager
    val environmentAssetManager = remember { EnvironmentAssetManager(context) }

    // Check if the current environment should have clouds
    val shouldShowClouds = remember(gameState.currentEnvironment.value) {
        val env = gameState.currentEnvironment.value
        env == GameEnvironment.FOREST || env == GameEnvironment.DESERT || env == GameEnvironment.BEACH
    }

    // Get appropriate cloud color based on environment
    val cloudColor = remember(gameState.currentEnvironment.value) {
        when (gameState.currentEnvironment.value) {
            GameEnvironment.FOREST -> Color.White.copy(alpha = 0.4f) // More faded for forest
            GameEnvironment.DESERT -> Color(0xFFFFFFEE).copy(alpha = 0.3f) // Yellowish tint, very faded for desert
            GameEnvironment.BEACH -> Color.White.copy(alpha = 0.5f) // Slightly more visible for beach
            else -> Color.White.copy(alpha = 0.4f) // Default
        }
    }

    // Preload assets when the game starts
    LaunchedEffect(Unit) {
        environmentAssetManager.preloadAllAssets()
    }

    // Environment-specific assets that update based on current environment
    val obstacleBitmap = remember(gameState.currentEnvironment.value) {
        environmentAssetManager.getObstacleBitmap(gameState.currentEnvironment.value)
            ?: BitmapFactory.decodeResource(context.resources, R.drawable.tree)
    }

    val collectibleBitmap = remember(gameState.currentEnvironment.value) {
        environmentAssetManager.getCollectibleBitmap(gameState.currentEnvironment.value)
            ?: BitmapFactory.decodeResource(context.resources, R.drawable.beer)
    }

    val boosterBitmap = remember {
        environmentAssetManager.getBoosterBitmap()
            ?: BitmapFactory.decodeResource(context.resources, R.drawable.booster)
    }

    // Update the environment name in log statements
    Log.d(TAG, "Using environment: ${gameState.currentEnvironment.value.levelName} with obstacle: ${gameState.currentEnvironment.value.obstacleType}, collectible: ${gameState.currentEnvironment.value.collectibleType}")

    // Paint object for drawing
    val paint = remember {
        Paint().apply {
            isFilterBitmap = true
            isAntiAlias = true
        }
    }

    // Game initialization
    LaunchedEffect(Unit) {
        gameState.resetForNewGame()
        soundManager.setSoundEnabled(gameState.soundEnabled.value)
    }

    // Add an effect to watch for level changes and reposition obstacles
    LaunchedEffect(gameState.currentLevel.value) {
        // Only do this for level changes after the game has started (not on initial level 1)
        if (gameState.currentLevel.value > 1) {
            Log.d(TAG, "Level change detected to level ${gameState.currentLevel.value} - repositioning obstacles")

            // Set flag to trigger obstacle repositioning in the next frame
            needsObstacleRepositioning = true
        }
    }

    // Track environment changes and trigger transition
    LaunchedEffect(gameState.currentEnvironment.value) {
        if (gameState.currentEnvironment.value != previousEnvironment) {
            // Start transition
            isTransitioning = true

            // Fade in white overlay
            val fadeInAnimation = TargetBasedAnimation(
                animationSpec = tween(300, easing = LinearEasing),
                typeConverter = Float.VectorConverter,
                initialValue = 0f,
                targetValue = 0.8f
            )

            var playTime = 0L
            val startTime = withFrameNanos { it }

            // Play fade-in animation
            while (playTime < fadeInAnimation.durationNanos) {
                playTime = withFrameNanos { it } - startTime
                transitionAlpha = fadeInAnimation.getValueFromNanos(playTime)

                // Allow frame to render
                yield()
            }

            // Update the previous environment reference
            previousEnvironment = gameState.currentEnvironment.value

            // Small pause at peak white
            delay(200)

            // Fade out white overlay
            val fadeOutAnimation = TargetBasedAnimation(
                animationSpec = tween(300, easing = LinearEasing),
                typeConverter = Float.VectorConverter,
                initialValue = 0.8f,
                targetValue = 0f
            )

            playTime = 0L
            val fadeOutStartTime = withFrameNanos { it }

            // Play fade-out animation
            while (playTime < fadeOutAnimation.durationNanos) {
                playTime = withFrameNanos { it } - fadeOutStartTime
                transitionAlpha = fadeOutAnimation.getValueFromNanos(playTime)

                // Allow frame to render
                yield()
            }

            // Reset transition state
            isTransitioning = false
            transitionAlpha = 0f
        }
    }

    // Track level changes and show notification - COMBINED with environment info
    LaunchedEffect(gameState.currentLevel.value) {
        // Don't show for the initial level
        if (gameState.currentLevel.value > 1) {
            // Show environment info with level details
            showEnvironmentInfo = true

            // Fade in animation
            val fadeInAnimation = TargetBasedAnimation(
                animationSpec = tween(500, easing = LinearEasing),
                typeConverter = Float.VectorConverter,
                initialValue = 0f,
                targetValue = 1f
            )

            var playTime = 0L
            val startTime = withFrameNanos { it }

            // Play fade-in animation
            while (playTime < fadeInAnimation.durationNanos) {
                playTime = withFrameNanos { it } - startTime
                environmentInfoAlpha = fadeInAnimation.getValueFromNanos(playTime)
                yield()
            }

            // Show info for 3 seconds
            delay(3000)

            // Fade out animation
            val fadeOutAnimation = TargetBasedAnimation(
                animationSpec = tween(500, easing = LinearEasing),
                typeConverter = Float.VectorConverter,
                initialValue = 1f,
                targetValue = 0f
            )

            playTime = 0L
            val fadeOutStartTime = withFrameNanos { it }

            // Play fade-out animation
            while (playTime < fadeOutAnimation.durationNanos) {
                playTime = withFrameNanos { it } - fadeOutStartTime
                environmentInfoAlpha = fadeOutAnimation.getValueFromNanos(playTime)
                yield()
            }

            // Reset notification state
            showEnvironmentInfo = false
            environmentInfoAlpha = 0f
        }
    }

    // Helper function for collision detection
    fun checkRectOverlap(rect1: Rect, rect2: Rect): Boolean {
        return rect1.left < rect2.right &&
                rect1.right > rect2.left &&
                rect1.top < rect2.bottom &&
                rect1.bottom > rect2.top
    }

    // UPDATED: Improved function to safely navigate to game over screen with delay for sound to play
    fun safeNavigateToGameOver() {
        if (!navigatedToGameOver) {
            navigatedToGameOver = true  // Set flag to prevent multiple navigation attempts

            // UPDATED: Increased delay to give time for the game over sound to play
            // before transitioning to the game over screen
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    navController.navigate("gameOver") {
                        popUpTo("game") { inclusive = true }
                    }
                    Log.d(TAG, "Successfully navigated to game over screen")
                } catch (e: Exception) {
                    Log.e(TAG, "Navigation error: ${e.message}", e)
                }
            }, 800)  // Increased from 300ms to 800ms to allow the sound to play
        } else {
            Log.d(TAG, "Navigation already triggered, skipping duplicate navigation")
        }
    }

    // Helper function for vibration
    fun vibrateDevice() {
        if (gameState.vibrationEnabled.value) {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        }
    }

    // Game physics loop
    LaunchedEffect(Unit) {
        while (gameRunning && gameState.isGameActive.value && !isGameOver) {
            try {
                // Check if obstacles need repositioning due to level change
                if (needsObstacleRepositioning) {
                    Log.d(TAG, "Repositioning obstacles for new level")

                    // Immediately push all obstacles far off-screen to the right
                    // Using 2.5 * screenWidth ensures they're well past the visible area
                    obstacles = Array(obstacles.size) { i ->
                        screenWidth + (i + 1) * 800f + (Math.random() * 500).toFloat()
                    }

                    // Reset the flag
                    needsObstacleRepositioning = false
                }

                // Only update game if not paused
                if (!isGamePaused) {
                    val currentTime = System.currentTimeMillis()

                    // Update the koala animation and physics
                    koala.update()

                    // Get koala hitbox
                    val koalaHitbox = koala.getBounds()

                    // Move obstacles - respect level transition invincibility
                    val isInLevelTransition = gameState.isLevelTransitionInvincible()

                    // Adjust movement speed during level transition (much slower)
                    val speedFactor = if (isInLevelTransition) {
                        0.3f // 30% normal speed during transition
                    } else {
                        1.0f // Normal speed otherwise
                    }

                    val effectiveSpeed = 16f * speedFactor * (if (hasSpeedBoost) 1.5f else 1.0f) *
                            gameState.currentEnvironment.value.speedMultiplier

                    // Update game speed for later use
                    gameSpeed = effectiveSpeed

                    val newObstacles = obstacles.copyOf()

                    for (i in obstacles.indices) {
                        // Move obstacle left
                        newObstacles[i] -= effectiveSpeed

                        // Reset obstacle when offscreen
                        if (newObstacles[i] < -100) { // Assuming obstacle width is around 100px
                            // Find the furthest obstacle
                            val furthestObstacle = newObstacles.maxOrNull() ?: 1000f

                            // Increase spacing between trees when booster is active
                            val baseSpacing = 650f  // Increased spacing
                            val randomVariation = (Math.random() * 300).toFloat()  // Increased variation

                            // Add 75% more space between trees when boosted
                            val boostSpacingMultiplier = if (hasSpeedBoost) 1.75f else 1.0f

                            // During level transition, push obstacles much further away
                            if (isInLevelTransition) {
                                newObstacles[i] = furthestObstacle + (baseSpacing * 4) +
                                        randomVariation + screenWidth
                            } else {
                                newObstacles[i] = furthestObstacle + (baseSpacing * boostSpacingMultiplier) +
                                        randomVariation
                            }
                        }

                        // Check for collision with koala - use gameState to check invincibility
                        if (!gameState.isLevelTransitionInvincible() && !isInvincible) {
                            // Create obstacle hitbox
                            val treeHitbox = Rect(
                                left = newObstacles[i],
                                top = groundY - treeHeight,
                                right = newObstacles[i] + treeWidth,
                                bottom = groundY
                            )

                            // Check for collision with koala - only if not invincible
                            if (currentTime > invincibleTime && !koala.isJumping &&
                                checkRectOverlap(koalaHitbox, treeHitbox)) {

                                // Try to decrease life - this now checks for level transition invincibility too
                                val stillAlive = gameState.decreaseLife()

                                // Only play hit sound and apply effects if actually lost a life
                                if (gameState.lives.value < lives) {
                                    lives = gameState.lives.value

                                    // Collision!
                                    soundManager.playHitSound()

                                    // Add vibration on collision
                                    vibrateDevice()

                                    // Set invincibility for 2 seconds
                                    invincibleTime = currentTime + 2000

                                    // Push obstacle away
                                    newObstacles[i] = screenWidth + 200f
                                }

                                // Check if game over
                                if (!stillAlive) {
                                    // Critical fix: Update game state and set isGameOver flag
                                    gameState.updateScore(score)
                                    gameState.endGame()
                                    gameRunning = false
                                    isGameOver = true

                                    // ADDED: Play game over sound before navigating
                                    soundManager.playGameOverSound()

                                    // Use the improved safe navigation function
                                    safeNavigateToGameOver()
                                    break
                                }
                            }
                        }
                    }

                    // Only continue if the game is still running
                    if (!gameRunning || isGameOver) {
                        break
                    }

                    obstacles = newObstacles

                    // Move collectibles - improved implementation with magnet effect
                    for (i in collectibles.indices) {
                        val collectible = collectibles[i]

                        if (collectible.active) {
                            // Default movement
                            var newX = collectible.x - effectiveSpeed
                            var newY = collectible.y

                            // Collectible magnet effect during boost
                            if (hasSpeedBoost) {
                                // Calculate distance to koala
                                val dx = koala.x - newX
                                val dy = koala.y - newY
                                val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                                // If within range, move collectible toward koala
                                if (distance < 200f) {
                                    val attractionSpeed = 10f
                                    val moveX = dx * attractionSpeed / distance
                                    val moveY = dy * attractionSpeed / distance

                                    newX += moveX
                                    newY += moveY
                                }
                            }

                            // Determine the collectible dimensions based on type
                            val collectibleWidth = if (collectible.isBooster) boosterWidth else beerWidth
                            val collectibleHeight = if (collectible.isBooster) boosterHeight else beerHeight

                            // Create collectible hitbox
                            val collectibleHitbox = Rect(
                                left = newX,
                                top = newY,
                                right = newX + collectibleWidth,
                                bottom = newY + collectibleHeight
                            )

                            // Check for collection using proper hitbox collision
                            if (checkRectOverlap(koalaHitbox, collectibleHitbox)) {
                                // Collected!
                                soundManager.playCollectSound()

                                // Apply score multiplier during boost
                                val pointValue = gameState.currentEnvironment.value.collectibleValue * (if (hasSpeedBoost) 2 else 1)

                                // FIX: Update both local score AND GameState score directly
                                score += pointValue
                                gameState.score.value = score

                                // Log score update for debugging
                                Log.d(TAG, "Collected item! Score now: $score, GameState score: ${gameState.score.value}")

                                // Update to inactive state
                                collectibles[i] = Collectible(newX, newY, false, collectible.isBooster)

                                // Handle booster collectibles
                                if (collectible.isBooster) {
                                    // Cancel any existing booster job
                                    boosterJob?.cancel()

                                    // Set booster state
                                    hasSpeedBoost = true
                                    isInvincible = true // Set invincibility when collected
                                    isBoosterWarningActive = false
                                    koala.setPowerUpState(true) // Set koala power-up state
                                    koala.setInvincibleState(true) // Set koala invincibility state

                                    // Start new booster job with warning
                                    boosterJob = coroutineScope.launch {
                                        try {
                                            // Wait until warning period (3 seconds in, 2 seconds before end)
                                            delay(3000)

                                            // Activate warning state
                                            isBoosterWarningActive = true

                                            // Wait for the final 2 seconds
                                            delay(2000)
                                        } finally {
                                            // Always ensure these states are reset
                                            hasSpeedBoost = false
                                            isInvincible = false
                                            isBoosterWarningActive = false
                                            koala.setPowerUpState(false)
                                            koala.setInvincibleState(false)
                                        }
                                    }
                                }

                                // Schedule respawn
                                coroutineScope.launch {
                                    delay(1000) // Wait a bit

                                    // Find furthest position
                                    val furthestX = collectibles.maxOf { it.x }

                                    // Respawn at new position with same type (booster or regular)
                                    val baseSpacing = 650f // Increased spacing to match obstacles
                                    val randomVariation = (Math.random() * 300).toFloat() // Increased variation

                                    // Add 75% more space when boosted (matching tree spacing)
                                    val boostSpacingMultiplier = if (hasSpeedBoost) 1.75f else 1.0f

                                    collectibles[i] = Collectible(
                                        furthestX + (baseSpacing * boostSpacingMultiplier) + randomVariation,
                                        groundY - 120f - (Math.random() * 160f).toFloat(),
                                        true,
                                        collectible.isBooster // Keep the same type
                                    )
                                }
                            } else if (newX < -collectibleWidth) {
                                // Reset if off screen
                                val furthestX = collectibles.maxOf { it.x }

                                val baseSpacing = 650f // Increased spacing to match obstacles
                                val randomVariation = (Math.random() * 300).toFloat() // Increased variation

                                // Add 75% more space when boosted
                                val boostSpacingMultiplier = if (hasSpeedBoost) 1.75f else 1.0f

                                collectibles[i] = Collectible(
                                    furthestX + (baseSpacing * boostSpacingMultiplier) + randomVariation,
                                    groundY - 120f - (Math.random() * 160f).toFloat(),
                                    true,
                                    collectible.isBooster // Keep the same type
                                )
                            } else {
                                // Just update position
                                collectibles[i] = Collectible(newX, newY, collectible.active, collectible.isBooster)
                            }
                        }
                    }

                    // FIX: Changed the order - first update environment/level check AFTER the score is updated
                    // Check if game environment/level has changed
                    val levelChanged = gameState.updateEnvironment(score)
                    if (levelChanged) {
                        // Additional safety - mark obstacles for repositioning next frame
                        needsObstacleRepositioning = true
                    }

                    // FIX: Get the updated values from gameState, but don't overwrite score
                    // as that would revert our collectible-based changes
                    lives = gameState.lives.value
                    currentLevel = gameState.currentLevel.value

                    // Check and update invincibility status from game state
                    isInvincible = isInvincible || gameState.isLevelTransitionInvincible()
                }

                // Final game over check at the end of each frame
                if (lives <= 0 && gameRunning && !isGameOver) {
                    gameState.updateScore(score)
                    gameState.endGame()
                    gameRunning = false
                    isGameOver = true

                    // ADDED: Play game over sound before navigating
                    soundManager.playGameOverSound()

                    // Use safe navigation function
                    safeNavigateToGameOver()
                    break
                }
            } catch (e: Exception) {
                // Catch any exceptions in the game loop to prevent crashes
                Log.e(TAG, "Game loop error: ${e.message}", e)
            }

            delay(33) // ~30fps
        }

        // One more check to ensure proper game over handling
        if (lives <= 0 && !navigatedToGameOver) {
            gameState.updateScore(score)
            gameState.endGame()
            isGameOver = true
            gameRunning = false

            // ADDED: Try to play game over sound again just in case
            if (lives <= 0 && !gameRunning) {
                soundManager.playGameOverSound()
            }

            // Final attempt to navigate using the improved function
            safeNavigateToGameOver()
        }
    }

    // Game UI
    Box(modifier = Modifier.fillMaxSize()) {
        // Draw base canvas with sky gradient and essential game elements
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // React immediately on touch down, not on release
                            if (!isGamePaused && !koala.isJumping) {
                                koala.jump()
                                soundManager.playJumpSound()
                            }
                            true // Event consumed
                        }
                        else -> false // Don't consume other events
                    }
                }
        ) {
            // Draw sky with current environment colors
            val environmentColors = gameState.currentEnvironment.value.backgroundColors
            drawRect(
                brush = Brush.verticalGradient(colors = environmentColors),
                size = this.size
            )

            // Draw ground with environment ground color
            drawRect(
                color = gameState.currentEnvironment.value.groundColor,
                topLeft = Offset(0f, groundY),
                size = Size(this.size.width, this.size.height - groundY)
            )

            // Draw green grass line
            drawRect(
                color = Color(0xFF4CAF50), // Green
                topLeft = Offset(0f, groundY),
                size = Size(this.size.width, 5f)
            )

            // Draw koala using AnimatedKoala
            val currentTime = System.currentTimeMillis()
            val isVisible = currentTime > invincibleTime || (currentTime / 200) % 2 == 0L

            if (isVisible || hasSpeedBoost) {
                // Draw koala using our AnimatedKoala class
                koala.draw(this)
            }

            // Draw level transition invincibility shield
            if (gameState.isLevelTransitionInvincible()) {
                // Draw a pulsing shield effect around koala
                val pulsePhase = (currentTime % 1000) / 1000f * (Math.PI * 2)
                val pulseFactor = 1.0f + (sin(pulsePhase) * 0.2f).toFloat()

                drawCircle(
                    color = Color(0x553399FF), // Semi-transparent blue shield
                    radius = koala.width * 1.5f * pulseFactor,
                    center = Offset(koala.x + koala.width / 2, koala.y + koala.height / 2)
                )

                // Draw a text indicator at the top of the screen
                val text = "LEVEL UP SHIELD ACTIVE"
                val paint = Paint().apply {
                    color = 0xFF3399FF.toInt() // Blue color
                    textSize = 24f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }

                drawContext.canvas.nativeCanvas.drawText(
                    text,
                    size.width / 2,
                    80f,
                    paint
                )
            }

            // Draw obstacles with proper environment-specific images
            obstacles.forEach { obstacleX ->
                if (obstacleBitmap != null) {
                    // Calculate target dimensions with scaling
                    val targetWidth = treeWidth
                    val targetHeight = treeHeight

                    // Create destination rectangle for scaled drawing
                    val dstRect = android.graphics.RectF(
                        obstacleX,
                        groundY - targetHeight, // Position from the ground up
                        obstacleX + targetWidth,
                        groundY
                    )

                    // Draw obstacle with scaling
                    drawContext.canvas.nativeCanvas.drawBitmap(
                        obstacleBitmap,
                        null,
                        dstRect,
                        paint
                    )
                }
            }

            // Draw collectibles with proper environment-specific images
            collectibles.forEach { collectible ->
                if (collectible.active) {
                    // Determine which bitmap and dimensions to use
                    val bitmap = if (collectible.isBooster) boosterBitmap else collectibleBitmap
                    val width = if (collectible.isBooster) boosterWidth else beerWidth
                    val height = if (collectible.isBooster) boosterHeight else beerHeight

                    if (bitmap != null) {
                        // Create destination rectangle for scaled drawing
                        val dstRect = android.graphics.RectF(
                            collectible.x,
                            collectible.y,
                            collectible.x + width,
                            collectible.y + height
                        )

                        // Draw with scaling to match desired size
                        drawContext.canvas.nativeCanvas.drawBitmap(
                            bitmap,
                            null, // Use entire source bitmap
                            dstRect, // Scale to this destination rectangle
                            paint
                        )
                    } else {
                        // Fallback with different colors based on type
                        drawCircle(
                            color = if (collectible.isBooster) Color.Red else Color.Yellow,
                            radius = width/2,
                            center = Offset(collectible.x + width/2, collectible.y + height/2)
                        )
                    }
                }
            }

            // Draw speed boost indicator if active
            if (hasSpeedBoost) {
                drawRect(
                    color = Color(0x88FFD700), // Semi-transparent gold
                    topLeft = Offset(0f, 0f),
                    size = Size(this.size.width, 10f)
                )

                // Draw warning indicator when booster is about to expire
                if (isBoosterWarningActive) {
                    // Flashing effect - alternate between visible and invisible
                    val shouldShowWarning = (System.currentTimeMillis() / 250) % 2 == 0L

                    if (shouldShowWarning) {
                        drawRect(
                            color = Color.Red.copy(alpha = 0.3f),
                            topLeft = Offset(0f, 0f),
                            size = Size(this.size.width, 10f)
                        )
                    }

                    // Also add a text warning that's more noticeable
                    val warningText = "BOOST ENDING!"
                    val warningPaint = Paint().apply {
                        color = android.graphics.Color.RED // Use Android's native Color class
                        textSize = 24f
                        isFakeBoldText = true
                        textAlign = Paint.Align.CENTER
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        warningText,
                        size.width / 2,
                        50f,
                        warningPaint
                    )
                }
            }

            // Draw transition overlay if transitioning
            if (isTransitioning) {
                drawRect(
                    color = Color.White.copy(alpha = transitionAlpha),
                    size = this.size
                )
            }

            // Draw pause overlay if paused
            if (isGamePaused) {
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    size = this.size
                )
            }
        }

        // Add environment-specific moving backgrounds
        if (!isGamePaused) {
            MovingBackgrounds(
                environment = gameState.currentEnvironment.value,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Add animated clouds as an overlay for selected environments (with low opacity)
        if (shouldShowClouds) {
            // Add faded clouds with environment-specific opacity
            AnimatedCloudsBackground(
                cloudColor = cloudColor,
                // Make clouds move a bit slower for better visual effect
                speedMultiplier = 0.7f
            )
        }

        // Enhanced HUD
        EnhancedGameHUD(
            score = score,
            level = currentLevel,
            lives = lives,
            hasSpeedBoost = hasSpeedBoost,
            isInvincible = isInvincible || gameState.isLevelTransitionInvincible(), // Include level transition invincibility
            environment = gameState.currentEnvironment.value
        )

        // Pause button positioned on the left side under the HUD
        Box(
            modifier = Modifier
                .padding(start = 16.dp, top = 90.dp)
                .align(Alignment.TopStart)
        ) {
            PauseButton(onClick = { isGamePaused = true })
        }

        // Environment info notification with level details (combined notification)
        if (showEnvironmentInfo) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val environment = gameState.currentEnvironment.value

                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .graphicsLayer(alpha = environmentInfoAlpha),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xCCFFFFFF) // Semi-transparent white
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 8.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Combined level up and environment notification
                        Text(
                            text = "LEVEL ${currentLevel}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${environment.levelName} Environment",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9C27B0)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Show what to avoid and what to collect
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Avoid:", fontSize = 16.sp)
                                Text(
                                    text = "${environment.obstacleType.capitalize()}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Red
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Collect:", fontSize = 16.sp)
                                Text(
                                    text = "${environment.collectibleType.capitalize()}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }

                        // Show points value
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Worth ${environment.collectibleValue} points each",
                            fontSize = 16.sp,
                            color = Color(0xFF333333)
                        )
                    }
                }
            }
        }

        // Pause dialog
        if (isGamePaused) {
            GamePauseDialog(
                onResume = { isGamePaused = false },
                onQuit = {
                    gameState.endGame()
                    gameRunning = false

                    // Use safer navigation approach
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            navController.navigate("mainMenu") {
                                popUpTo(0) // Pop everything up to the start
                            }
                            Log.d(TAG, "Successfully navigated to main menu from pause dialog")
                        } catch (e: Exception) {
                            Log.e(TAG, "Navigation error from pause dialog: ${e.message}", e)
                        }
                    }, 100)
                }
            )
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            gameRunning = false
            gameState.isGameActive.value = false
            koala.release()
            environmentAssetManager.release() // Release all environment assets
            soundManager.release()
            boosterJob?.cancel()

            // If game is over but navigation didn't happen, try once more
            if (isGameOver && !navigatedToGameOver) {
                Log.d(TAG, "Final cleanup - attempting navigation to game over")

                // Try to play game over sound again just in case
                if (lives <= 0 && !gameRunning) {
                    soundManager.playGameOverSound()
                }

                safeNavigateToGameOver()
            }

            System.gc()
        }
    }
}

/**
 * Extension function to capitalize the first letter
 */
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

/**
 * Renamed to GamePauseDialog to avoid naming conflicts
 */
@Composable
private fun GamePauseDialog(
    onResume: () -> Unit,
    onQuit: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* Do nothing to prevent dismiss on outside click */ }
    ) {
        Card(
            modifier = Modifier
                .width(280.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
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
                Text(
                    text = "Game Paused",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    border = null // Remove border to fix background color issue
                ) {
                    Text("Resume", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onQuit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9C27B0)
                    ),
                    border = null // Remove border to fix background color issue
                ) {
                    Text("Quit Game", fontSize = 16.sp)
                }
            }
        }
    }
}