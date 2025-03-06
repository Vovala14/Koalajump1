package com.lavrik.koalajump.screens

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.lavrik.koalajump.GameState
import com.lavrik.koalajump.R
import com.lavrik.koalajump.entities.AnimatedKoala
import com.lavrik.koalajump.game.GameEnvironment
import com.lavrik.koalajump.ui.components.EnhancedGameHUD
import com.lavrik.koalajump.utils.SoundManager
import kotlinx.coroutines.*

private const val TAG = "GameScreen"

// Data class for collectibles: x, y, active, isBooster
data class Collectible(val x: Float, val y: Float, val active: Boolean, val isBooster: Boolean)

/**
 * Improved GameScreen with proper animation and game loop
 */
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
    var gameRunning by remember { mutableStateOf(true) }
    var invincibleTime by remember { mutableStateOf(0L) } // Invincibility after hit

    // Environment transition effect state
    var isTransitioning by remember { mutableStateOf(false) }
    var transitionAlpha by remember { mutableStateOf(0f) }
    var previousEnvironment by remember { mutableStateOf(gameState.currentEnvironment.value) }

    // Level-up notification state
    var showLevelUpMessage by remember { mutableStateOf(false) }
    var levelUpMessageAlpha by remember { mutableStateOf(0f) }
    var levelUpEnvironment by remember { mutableStateOf("") }

    // Create the animated koala
    val koala = remember {
        AnimatedKoala(context, screenWidth, screenHeight)
    }

    // Tree, beer, and booster dimensions
    val treeWidth = 40f
    val treeHeight = 60f
    val beerWidth = 30f
    val beerHeight = 30f
    val boosterWidth = 35f
    val boosterHeight = 35f

    // Obstacles and collectibles
    var obstacles by remember { mutableStateOf(arrayOf(
        screenWidth + 400f,
        screenWidth + 800f,
        screenWidth + 1200f
    )) }

    // Important fix: Use MutableState for collectibles to ensure state updates
    // Include isBooster flag (true for booster, false for regular collectible)
    val collectibles = remember { mutableStateListOf(
        Collectible(screenWidth + 600f, groundY - 200f, true, false),
        Collectible(screenWidth + 1000f, groundY - 250f, true, false),
        Collectible(screenWidth + 1400f, groundY - 150f, true, true) // This one is a booster
    ) }

    // Load game assets
    val soundManager = remember { SoundManager(context) }

    // Load bitmap resources directly as Android Bitmaps
    val treeBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.tree)
    }

    val beerBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.beer)
    }

    val boosterBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.booster)
    }

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

    // Track level changes and show notification
    LaunchedEffect(gameState.currentLevel.value) {
        // Don't show for the initial level
        if (gameState.currentLevel.value > 1) {
            showLevelUpMessage = true
            levelUpEnvironment = gameState.currentEnvironment.value.levelName

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
                levelUpMessageAlpha = fadeInAnimation.getValueFromNanos(playTime)
                yield()
            }

            // Show message for a few seconds
            delay(2000)

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
                levelUpMessageAlpha = fadeOutAnimation.getValueFromNanos(playTime)
                yield()
            }

            // Reset notification state
            showLevelUpMessage = false
            levelUpMessageAlpha = 0f
        }
    }

    // Helper function for collision detection
    fun checkRectOverlap(rect1: Rect, rect2: Rect): Boolean {
        return rect1.left < rect2.right &&
                rect1.right > rect2.left &&
                rect1.top < rect2.bottom &&
                rect1.bottom > rect2.top
    }

    // Game physics loop
    LaunchedEffect(Unit) {
        while (gameRunning && gameState.isGameActive.value) {
            val currentTime = System.currentTimeMillis()

            // Update the koala animation and physics
            koala.update()

            // Get koala hitbox
            val koalaHitbox = koala.getBounds()

            // Move obstacles
            val effectiveSpeed = 12f * (if (hasSpeedBoost) 1.5f else 1.0f) * gameState.currentEnvironment.value.speedMultiplier
            val newObstacles = obstacles.copyOf()

            for (i in obstacles.indices) {
                newObstacles[i] -= effectiveSpeed

                // Reset obstacle when offscreen
                if (newObstacles[i] < -treeWidth) {
                    val furthestObstacle = newObstacles.maxOrNull() ?: screenWidth

                    // Increase spacing between trees when booster is active
                    val baseSpacing = 400f
                    val randomVariation = (Math.random() * 200).toFloat()

                    // Add 75% more space between trees when boosted
                    val boostSpacingMultiplier = if (hasSpeedBoost) 1.75f else 1.0f

                    newObstacles[i] = furthestObstacle + (baseSpacing * boostSpacingMultiplier) + randomVariation
                }

                // Create obstacle hitbox
                val treeHitbox = Rect(
                    left = newObstacles[i],
                    top = groundY - treeHeight,
                    right = newObstacles[i] + treeWidth,
                    bottom = groundY
                )

                // Check for collision with koala - only if not invincible
                if (currentTime > invincibleTime && !koala.isJumping && checkRectOverlap(koalaHitbox, treeHitbox)) {
                    // Collision!
                    soundManager.playHitSound()
                    lives--

                    // Set invincibility for 2 seconds
                    invincibleTime = currentTime + 2000

                    // Push obstacle away
                    newObstacles[i] = screenWidth + 200f

                    if (lives <= 0) {
                        gameState.updateScore(score)
                        gameState.endGame()
                        gameRunning = false
                        navController.navigate("gameOver")
                        break
                    }
                }
            }
            obstacles = newObstacles

            // Move collectibles - fixed implementation
            for (i in collectibles.indices) {
                val collectible = collectibles[i]

                if (collectible.active) {
                    // Move left if active
                    val newX = collectible.x - effectiveSpeed

                    // Determine the collectible dimensions based on type
                    val collectibleWidth = if (collectible.isBooster) boosterWidth else beerWidth
                    val collectibleHeight = if (collectible.isBooster) boosterHeight else beerHeight

                    // Create collectible hitbox
                    val collectibleHitbox = Rect(
                        left = newX,
                        top = collectible.y,
                        right = newX + collectibleWidth,
                        bottom = collectible.y + collectibleHeight
                    )

                    // Check for collection using proper hitbox collision
                    if (checkRectOverlap(koalaHitbox, collectibleHitbox)) {
                        // Collected!
                        soundManager.playCollectSound()
                        score += gameState.currentEnvironment.value.collectibleValue

                        // Update to inactive state
                        collectibles[i] = Collectible(newX, collectible.y, false, collectible.isBooster)

                        // If it's a booster, give speed boost
                        if (collectible.isBooster) {
                            hasSpeedBoost = true
                            koala.setPowerUpState(true) // Set koala power-up state
                            coroutineScope.launch {
                                delay(5000)
                                hasSpeedBoost = false
                                koala.setPowerUpState(false) // Reset koala power-up state
                            }
                        }

                        // Schedule respawn
                        coroutineScope.launch {
                            delay(1000) // Wait a bit

                            // Find furthest position
                            val furthestX = collectibles.maxOf { it.x }

                            // Respawn at new position with same type (booster or regular)
                            val baseSpacing = 600f
                            val randomVariation = (Math.random() * 400).toFloat()

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

                        val baseSpacing = 600f
                        val randomVariation = (Math.random() * 400).toFloat()

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
                        collectibles[i] = Collectible(newX, collectible.y, collectible.active, collectible.isBooster)
                    }
                }
            }

            // Update score and check for environment changes
            gameState.score.value = score
            gameState.updateEnvironment(score)
            currentLevel = gameState.currentLevel.value

            delay(33) // ~30fps
        }
    }

    // Game UI
    Box(modifier = Modifier.fillMaxSize()) {
        // Game canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        if (!koala.isJumping) {
                            koala.jump()
                            soundManager.playJumpSound()
                        }
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

            if (isVisible) {
                // Draw koala using our AnimatedKoala class
                koala.draw(this)
            }

            // Draw obstacles (trees)
            obstacles.forEach { obstacleX ->
                if (treeBitmap != null) {
                    // Create destination rectangle for scaled drawing
                    val dstRect = android.graphics.RectF(
                        obstacleX,
                        groundY - treeHeight,
                        obstacleX + treeWidth,
                        groundY
                    )

                    // Draw tree with scaling to match desired size
                    drawContext.canvas.nativeCanvas.drawBitmap(
                        treeBitmap,
                        null, // Use entire source bitmap
                        dstRect, // Scale to this destination rectangle
                        paint
                    )
                } else {
                    // Fallback
                    drawRect(
                        color = Color.Green,
                        topLeft = Offset(obstacleX, groundY - treeHeight),
                        size = Size(treeWidth, treeHeight)
                    )
                }
            }

            // Draw collectibles (beers and boosters)
            collectibles.forEach { collectible ->
                if (collectible.active) {
                    // Determine which bitmap and dimensions to use
                    val bitmap = if (collectible.isBooster) boosterBitmap else beerBitmap
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
            }

            // Draw transition overlay if transitioning
            if (isTransitioning) {
                drawRect(
                    color = Color.White.copy(alpha = transitionAlpha),
                    size = this.size
                )
            }
        }

        // Enhanced HUD
        EnhancedGameHUD(
            score = score,
            level = currentLevel,
            lives = lives,
            hasSpeedBoost = hasSpeedBoost,
            environment = gameState.currentEnvironment.value
        )

        // Level-up notification display
        if (showLevelUpMessage) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .graphicsLayer(alpha = levelUpMessageAlpha),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xCCFFFFFF)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 8.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "LEVEL UP!",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Entering $levelUpEnvironment",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Level $currentLevel",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9C27B0)
                        )
                    }
                }
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            gameRunning = false
            koala.release()
            treeBitmap?.recycle()
            beerBitmap?.recycle()
            boosterBitmap?.recycle()
            soundManager.release()
            gameState.isGameActive.value = false
            System.gc()
        }
    }
}