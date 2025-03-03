package com.lavrik.koalajump.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavController
import com.lavrik.koalajump.GameState
import com.lavrik.koalajump.R
import com.lavrik.koalajump.ui.components.EnhancedGameHUD
import com.lavrik.koalajump.utils.SoundManager
import kotlinx.coroutines.*

private const val TAG = "GameScreen"

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
    var isJumping by remember { mutableStateOf(false) }
    var jumpVelocity by remember { mutableStateOf(-22f) }
    var koalaY by remember { mutableStateOf(groundY - 100f) }
    var hasSpeedBoost by remember { mutableStateOf(false) }
    var gameRunning by remember { mutableStateOf(true) }
    var koalaFrame by remember { mutableStateOf(0) }
    var invincibleTime by remember { mutableStateOf(0L) } // Invincibility after hit

    // Obstacles and collectibles
    var obstacles by remember { mutableStateOf(arrayOf(
        screenWidth + 400f,
        screenWidth + 800f,
        screenWidth + 1200f
    )) }

    // Important fix: Use MutableState for collectibles to ensure state updates
    val collectibles = remember { mutableStateListOf(
        Triple(screenWidth + 600f, groundY - 200f, true),
        Triple(screenWidth + 1000f, groundY - 250f, true),
        Triple(screenWidth + 1400f, groundY - 150f, true)
    ) }

    // Load game assets
    val soundManager = remember { SoundManager(context) }

    // Load bitmap resources directly as Android Bitmaps
    val koalaBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.koala_animation)
    }

    val treeBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.tree)
    }

    val beerBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.beer)
    }

    // Create frame source bitmap
    val koalaFrameBitmap = remember { mutableStateOf<Bitmap?>(null) }

    // Game initialization
    LaunchedEffect(Unit) {
        gameState.resetForNewGame()
        soundManager.setSoundEnabled(gameState.soundEnabled.value)
    }

    // Animation frame updater
    LaunchedEffect(Unit) {
        while (gameRunning) {
            koalaFrame = (koalaFrame + 1) % 12 // Assuming 12 frames in animation

            // Update the frame bitmap
            if (koalaBitmap != null) {
                val frameHeight = koalaBitmap.height / 12 // 12 frames
                val frameWidth = koalaBitmap.width

                // Extract the current frame
                koalaFrameBitmap.value = Bitmap.createBitmap(
                    koalaBitmap,
                    0, koalaFrame * frameHeight, frameWidth, frameHeight
                )
            }

            delay(100) // 10 fps animation
        }
    }

    // Game physics loop
    LaunchedEffect(Unit) {
        while (gameRunning && gameState.isGameActive.value) {
            val currentTime = System.currentTimeMillis()

            // Update jumping physics
            if (isJumping) {
                koalaY += jumpVelocity
                jumpVelocity += 1.6f // Gravity

                // Check if landed
                if (koalaY >= groundY - 100f) {
                    koalaY = groundY - 100f
                    isJumping = false
                    jumpVelocity = -22f
                }
            }

            // Move obstacles
            val effectiveSpeed = 12f * (if (hasSpeedBoost) 1.5f else 1.0f)
            val newObstacles = obstacles.copyOf()

            for (i in obstacles.indices) {
                newObstacles[i] -= effectiveSpeed

                // Reset obstacle when offscreen
                if (newObstacles[i] < -100f) {
                    val furthestObstacle = newObstacles.maxOrNull() ?: screenWidth
                    newObstacles[i] = furthestObstacle + 400f + (Math.random() * 200).toFloat()
                }

                // Check for collision with koala - only if not invincible
                if (currentTime > invincibleTime &&
                    Math.abs(newObstacles[i] - screenWidth/4f) < 50f &&
                    Math.abs(koalaY - (groundY - 100f)) < 50f && !isJumping) {
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
                val (x, y, active) = collectibles[i]

                if (active) {
                    // Move left if active
                    val newX = x - effectiveSpeed

                    // Check for collection
                    if (Math.abs(newX - screenWidth/4f) < 50f && Math.abs(y - koalaY) < 50f) {
                        // Collected!
                        soundManager.playCollectSound()
                        score += 10

                        // Update to inactive state
                        collectibles[i] = Triple(newX, y, false)

                        // Schedule respawn
                        coroutineScope.launch {
                            delay(1000) // Wait a bit

                            // Find furthest position
                            val furthestX = collectibles.maxOf { it.first }

                            // Respawn at new position
                            collectibles[i] = Triple(
                                furthestX + 600f + (Math.random() * 400).toFloat(),
                                groundY - 150f - (Math.random() * 200f).toFloat(),
                                true
                            )
                        }

                        // 20% chance of speed boost
                        if (!hasSpeedBoost && Math.random() < 0.2) {
                            hasSpeedBoost = true
                            coroutineScope.launch {
                                delay(5000)
                                hasSpeedBoost = false
                            }
                        }
                    } else if (newX < -50f) {
                        // Reset if off screen
                        val furthestX = collectibles.maxOf { it.first }
                        collectibles[i] = Triple(
                            furthestX + 600f + (Math.random() * 400).toFloat(),
                            groundY - 150f - (Math.random() * 200f).toFloat(),
                            true
                        )
                    } else {
                        // Just update position
                        collectibles[i] = Triple(newX, y, active)
                    }
                }
            }

            // Update score
            gameState.score.value = score

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
                        if (!isJumping) {
                            isJumping = true
                            jumpVelocity = -22f
                            soundManager.playJumpSound()
                        }
                    }
                }
        ) {
            // Draw sky
            drawRect(
                color = Color.White,
                size = this.size
            )

            // Draw ground
            drawRect(
                color = Color(0xFF8B4513), // Brown
                topLeft = androidx.compose.ui.geometry.Offset(0f, groundY),
                size = androidx.compose.ui.geometry.Size(this.size.width, this.size.height - groundY)
            )

            // Draw green grass line
            drawRect(
                color = Color(0xFF4CAF50), // Green
                topLeft = androidx.compose.ui.geometry.Offset(0f, groundY),
                size = androidx.compose.ui.geometry.Size(this.size.width, 5f)
            )

            // Draw koala - with animation bitmap if available
            // Make koala flash if invincible
            val currentTime = System.currentTimeMillis()
            val isVisible = currentTime > invincibleTime || (currentTime / 200) % 2 == 0L

            if (isVisible && koalaFrameBitmap.value != null) {
                val frameBitmap = koalaFrameBitmap.value!!

                // Draw using nativeCanvas
                drawContext.canvas.nativeCanvas.drawBitmap(
                    frameBitmap,
                    screenWidth / 4f - frameBitmap.width / 2f,
                    koalaY,
                    android.graphics.Paint().apply {
                        isFilterBitmap = true
                        isAntiAlias = true
                    }
                )
            } else if (isVisible) {
                // Fallback if bitmap is not available
                drawCircle(
                    color = Color.Gray,
                    radius = 40f,
                    center = androidx.compose.ui.geometry.Offset(screenWidth / 4f, koalaY + 50f)
                )
            }

            // Draw obstacles
            obstacles.forEach { obstacleX ->
                if (treeBitmap != null) {
                    drawContext.canvas.nativeCanvas.drawBitmap(
                        treeBitmap,
                        obstacleX,
                        groundY - treeBitmap.height,
                        android.graphics.Paint()
                    )
                } else {
                    // Fallback
                    drawRect(
                        color = Color.Green,
                        topLeft = androidx.compose.ui.geometry.Offset(obstacleX, groundY - 80f),
                        size = androidx.compose.ui.geometry.Size(40f, 80f)
                    )
                }
            }

            // Draw collectibles
            collectibles.forEach { (x, y, active) ->
                if (active) {
                    if (beerBitmap != null) {
                        drawContext.canvas.nativeCanvas.drawBitmap(
                            beerBitmap,
                            x,
                            y,
                            android.graphics.Paint()
                        )
                    } else {
                        // Fallback
                        drawCircle(
                            color = Color.Yellow,
                            radius = 20f,
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                    }
                }
            }

            // Draw speed boost indicator if active
            if (hasSpeedBoost) {
                drawRect(
                    color = Color(0x88FFD700), // Semi-transparent gold
                    topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(this.size.width, 10f)
                )
            }
        }

        // Enhanced HUD
        EnhancedGameHUD(
            score = score,
            level = currentLevel,
            lives = lives,
            hasSpeedBoost = hasSpeedBoost
        )
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            gameRunning = false
            koalaFrameBitmap.value?.recycle()
            koalaBitmap?.recycle()
            treeBitmap?.recycle()
            beerBitmap?.recycle()
            soundManager.release()
            gameState.isGameActive.value = false
            System.gc()
        }
    }
}