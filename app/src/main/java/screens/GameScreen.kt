package com.lavrik.koalajump.screens

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavController
import com.lavrik.koalajump.GameState
import com.lavrik.koalajump.R
import com.lavrik.koalajump.entities.AnimatedKoala
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
    var hasSpeedBoost by remember { mutableStateOf(false) }
    var gameRunning by remember { mutableStateOf(true) }
    var invincibleTime by remember { mutableStateOf(0L) } // Invincibility after hit

    // Create the animated koala
    val koala = remember {
        AnimatedKoala(context, screenWidth, screenHeight)
    }

    // Tree and beer dimensions - adjust these for smaller images
    val treeWidth = 40f
    val treeHeight = 60f
    val beerWidth = 30f
    val beerHeight = 30f

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
    val treeBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.tree)
    }

    val beerBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.beer)
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
            val effectiveSpeed = 12f * (if (hasSpeedBoost) 1.5f else 1.0f)
            val newObstacles = obstacles.copyOf()

            for (i in obstacles.indices) {
                newObstacles[i] -= effectiveSpeed

                // Reset obstacle when offscreen
                if (newObstacles[i] < -treeWidth) {
                    val furthestObstacle = newObstacles.maxOrNull() ?: screenWidth
                    newObstacles[i] = furthestObstacle + 400f + (Math.random() * 200).toFloat()
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
                val (x, y, active) = collectibles[i]

                if (active) {
                    // Move left if active
                    val newX = x - effectiveSpeed

                    // Create beer hitbox
                    val beerHitbox = Rect(
                        left = newX,
                        top = y,
                        right = newX + beerWidth,
                        bottom = y + beerHeight
                    )

                    // Check for collection using proper hitbox collision
                    if (checkRectOverlap(koalaHitbox, beerHitbox)) {
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
                            koala.setPowerUpState(true) // Set koala power-up state
                            coroutineScope.launch {
                                delay(5000)
                                hasSpeedBoost = false
                                koala.setPowerUpState(false) // Reset koala power-up state
                            }
                        }
                    } else if (newX < -beerWidth) {
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
                        if (!koala.isJumping) {
                            koala.jump()
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

            // Draw obstacles (smaller trees)
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

            // Draw collectibles (smaller beers)
            collectibles.forEach { (x, y, active) ->
                if (active) {
                    if (beerBitmap != null) {
                        // Create destination rectangle for scaled drawing
                        val dstRect = android.graphics.RectF(
                            x,
                            y,
                            x + beerWidth,
                            y + beerHeight
                        )

                        // Draw beer with scaling to match desired size
                        drawContext.canvas.nativeCanvas.drawBitmap(
                            beerBitmap,
                            null, // Use entire source bitmap
                            dstRect, // Scale to this destination rectangle
                            paint
                        )
                    } else {
                        // Fallback
                        drawCircle(
                            color = Color.Yellow,
                            radius = beerWidth/2,
                            center = Offset(x + beerWidth/2, y + beerHeight/2)
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

            // Debug: Draw collision boxes (uncomment for debugging)
            /*
            // Draw koala hitbox
            val koalaHitbox = koala.getBounds()
            drawRect(
                color = Color.Red.copy(alpha = 0.5f),
                topLeft = Offset(koalaHitbox.left, koalaHitbox.top),
                size = Size(koalaHitbox.width, koalaHitbox.height),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )

            // Draw tree hitboxes
            obstacles.forEach { obstacleX ->
                drawRect(
                    color = Color.Blue.copy(alpha = 0.5f),
                    topLeft = Offset(obstacleX, groundY - treeHeight),
                    size = Size(treeWidth, treeHeight),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }
            */
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
            koala.release()
            treeBitmap?.recycle()
            beerBitmap?.recycle()
            soundManager.release()
            gameState.isGameActive.value = false
            System.gc()
        }
    }
}