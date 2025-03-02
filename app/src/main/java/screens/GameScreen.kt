package com.lavrik.koalajump.screens

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.lavrik.koalajump.GameState
import com.lavrik.koalajump.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// Constants for improved maintainability
private const val TAG = "GameScreen"
private const val DEBUG_MODE = false // Set to true to debug collisions
private const val FRAME_DELAY = 16L // ~60fps
private const val ANIMATION_DELAY = 70L
private const val COLLECTIBLE_RESPAWN_DELAY = 1000L
private const val TOTAL_FRAMES = 12
private const val OBSTACLE_COUNT = 3
private const val COLLECTIBLE_COUNT = 5
private const val SCORE_PER_COLLECTIBLE = 10
private const val PORTRAIT_GROUND_RATIO = 0.8f
private const val LANDSCAPE_GROUND_RATIO = 0.75f

@Composable
fun GameScreen(
    gameState: GameState,
    navController: NavController
) {
    Log.d(TAG, "GameScreen composition starts")

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Detect orientation
    val isPortrait = remember(configuration) {
        configuration.screenHeightDp > configuration.screenWidthDp
    }

    // Screen dimensions based on orientation
    val screenWidth = configuration.screenWidthDp.toFloat() * density.density
    val screenHeight = configuration.screenHeightDp.toFloat() * density.density

    // Adjust ground position based on orientation
    val groundY = if (isPortrait) {
        screenHeight * PORTRAIT_GROUND_RATIO
    } else {
        screenHeight * LANDSCAPE_GROUND_RATIO
    }

    // Game state
    var score by remember { mutableStateOf(0) }
    var lives by remember { mutableStateOf(3) }
    var showHitboxes by remember { mutableStateOf(DEBUG_MODE) }

    // Asset states
    var koalaSpriteSheet by remember { mutableStateOf<Bitmap?>(null) }
    var treeImage by remember { mutableStateOf<Bitmap?>(null) }
    var beerImage by remember { mutableStateOf<Bitmap?>(null) }

    // Game difficulty settings
    var gameSpeed by remember { mutableStateOf(10f) }

    // Cloud positions - different for portrait vs landscape
    val cloudPositions = remember(isPortrait, screenWidth, screenHeight) {
        List(5) {
            if (isPortrait) {
                Offset(
                    Random.nextFloat() * screenWidth,
                    Random.nextFloat() * groundY * 0.5f
                )
            } else {
                Offset(
                    Random.nextFloat() * screenWidth,
                    Random.nextFloat() * groundY * 0.7f
                )
            }
        }
    }

    // Load assets using BitmapFactory directly
    LaunchedEffect(Unit) {
        try {
            koalaSpriteSheet = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.koala_animation)
            treeImage = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.tree)
            beerImage = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.beer)
            Log.d(TAG, "Assets loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading assets: ${e.message}", e)
        }
    }

    // Animation and player properties - adjusted for orientation
    var currentFrame by remember { mutableStateOf(0) }

    // Position koala differently based on orientation
    var koalaX by remember(isPortrait, screenWidth) {
        mutableStateOf(
            if (isPortrait) screenWidth / 4f
            else screenWidth * 0.15f // More to the left in landscape
        )
    }

    var koalaY by remember { mutableStateOf(groundY - 100f) }
    var isJumping by remember { mutableStateOf(false) }

    // Adjust jump velocity based on orientation
    var jumpVelocity by remember(isPortrait) {
        mutableStateOf(
            if (isPortrait) -25f  // Higher initial velocity for portrait
            else -20f             // Lower initial velocity for landscape
        )
    }

    // Scale factor based on orientation
    val scaleFactor = remember(isPortrait) {
        if (isPortrait) 1f else 0.85f // Scale down objects slightly in landscape
    }

    // Game objects lists
    val obstacles = remember { mutableStateListOf<GameObject>() }
    val collectibles = remember { mutableStateListOf<GameObject>() }
    val allObjects = remember { mutableStateListOf<GameObject>() }

    // Bitmap paint object for clean rendering
    val bitmapPaint = remember {
        Paint().apply {
            isFilterBitmap = true
            isDither = true
            isAntiAlias = true
        }
    }

    // Create game objects when assets are loaded - adjusted for orientation
    LaunchedEffect(treeImage, beerImage, isPortrait, screenWidth, screenHeight, groundY) {
        if (treeImage == null || beerImage == null) return@LaunchedEffect

        // Clear previous objects
        obstacles.clear()
        collectibles.clear()
        allObjects.clear()

        // Calculate spacing based on orientation
        val timeSpacingInPixels = if (isPortrait) {
            1.2f * gameSpeed * 60 // Less spacing in portrait
        } else {
            1.8f * gameSpeed * 60 // More spacing in landscape
        }

        // Create obstacles with orientation-based spacing
        for (i in 0 until OBSTACLE_COUNT) {
            val obstacle = GameObject(
                image = treeImage!!,
                x = screenWidth + (i * timeSpacingInPixels),
                y = groundY - treeImage!!.height + 5f, // Slightly adjust height to fit ground better
                speed = gameSpeed,
                screenWidth = screenWidth,
                isObstacle = true,
                isPortrait = isPortrait // Make sure to include this parameter
            )
            obstacles.add(obstacle)
            allObjects.add(obstacle)
        }

        // Create collectibles with orientation-based distribution
        for (i in 0 until COLLECTIBLE_COUNT) {
            val collectible = GameObject(
                image = beerImage!!,
                x = screenWidth + (i * (if (isPortrait) 800f else 1200f)) + (if (isPortrait) 400f else 600f),
                y = groundY - 150f - (Random.nextFloat() * (if (isPortrait) 200f else 250f)),
                speed = gameSpeed,
                screenWidth = screenWidth,
                isObstacle = false,
                isPortrait = isPortrait // Make sure to include this parameter
            )
            collectibles.add(collectible)
            allObjects.add(collectible)
        }
    }

    // Initialize game state
    LaunchedEffect(Unit) {
        gameState.isGameActive.value = true
        score = 0
        lives = 3
        gameSpeed = 10f
    }

    // Animation loop with improved timing for smoother transitions
    LaunchedEffect(Unit) {
        while (gameState.isGameActive.value) {
            delay(ANIMATION_DELAY)
            currentFrame = (currentFrame + 1) % TOTAL_FRAMES
        }
    }

    // Game loop
    LaunchedEffect(koalaSpriteSheet, treeImage, beerImage, isPortrait) {
        // Wait for assets
        if (koalaSpriteSheet == null || allObjects.isEmpty()) return@LaunchedEffect

        // Set initial koala position
        val frameHeight = koalaSpriteSheet!!.height / TOTAL_FRAMES
        koalaY = groundY - frameHeight

        while (gameState.isGameActive.value) {
            // Update koala with jumping physics - adjusted for orientation
            if (isJumping) {
                koalaY += jumpVelocity
                // Different gravity based on orientation
                jumpVelocity += if (isPortrait) 1.5f else 1.2f

                // Check landing
                if (koalaY >= groundY - frameHeight) {
                    koalaY = groundY - frameHeight
                    isJumping = false
                    // Reset jump velocity based on orientation
                    jumpVelocity = if (isPortrait) -25f else -20f
                }
            }

            // Update all game objects
            for (obj in allObjects) {
                obj.update()
            }

            // Create a smaller hitbox for the koala (30% smaller on all sides)
            val koalaFrameHeight = koalaSpriteSheet!!.height / TOTAL_FRAMES
            val koalaWidth = koalaSpriteSheet!!.width.toFloat()

            // Calculate reduction amounts (15% on each side = 30% total reduction)
            val widthReduction = koalaWidth * 0.15f
            val heightReduction = koalaFrameHeight * 0.15f

            // Create a more precise hitbox for the koala
            val koalaHitbox = androidx.compose.ui.geometry.Rect(
                offset = Offset(
                    koalaX + widthReduction,  // Start 15% in from left
                    koalaY + heightReduction  // Start 15% in from top
                ),
                size = Size(
                    koalaWidth - (widthReduction * 2),  // 30% less width
                    koalaFrameHeight - (heightReduction * 2)  // 30% less height
                )
            )

            // Check obstacle collisions with improved hitbox
            for (obstacle in obstacles) {
                if (obstacle.isActive) {
                    // Create a more precise hitbox for the obstacle (also 30% smaller)
                    val obstacleWidth = obstacle.width.toFloat()
                    val obstacleHeight = obstacle.height.toFloat()

                    val obstacleWidthReduction = obstacleWidth * 0.15f
                    val obstacleHeightReduction = obstacleHeight * 0.15f

                    val obstacleBounds = obstacle.getBounds()
                    val preciseObstacleHitbox = androidx.compose.ui.geometry.Rect(
                        offset = Offset(
                            obstacleBounds.left + obstacleWidthReduction,
                            obstacleBounds.top + obstacleHeightReduction
                        ),
                        size = Size(
                            obstacleWidth - (obstacleWidthReduction * 2),
                            obstacleHeight - (obstacleHeightReduction * 2)
                        )
                    )

                    // Check collision with precise hitboxes
                    if (koalaHitbox.overlaps(preciseObstacleHitbox)) {
                        lives--
                        obstacle.resetPosition()

                        if (lives <= 0) {
                            gameState.updateScore(score)
                            gameState.endGame()
                            navController.navigate("gameOver")
                            break
                        }
                    }
                }
            }

            // Check collectible collisions with improved hitbox
            for (collectible in collectibles) {
                if (collectible.isActive) {
                    // We can use the regular bounds for collectibles - makes them easier to collect
                    if (koalaHitbox.overlaps(collectible.getBounds())) {
                        score += SCORE_PER_COLLECTIBLE
                        collectible.isActive = false

                        // Reactivate after delay
                        coroutineScope.launch {
                            delay(COLLECTIBLE_RESPAWN_DELAY)
                            collectible.resetPosition()
                            collectible.isActive = true
                        }
                    }
                }
            }

            delay(FRAME_DELAY) // ~60fps
        }
    }

    // Game UI - adjusted for orientation
    Box(modifier = Modifier.fillMaxSize()) {
        // Main game canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        if (!isJumping) {
                            isJumping = true
                            jumpVelocity = if (isPortrait) -25f else -20f
                        }
                    }
                }
        ) {
            // Draw white background
            drawRect(
                color = Color(0xFFFFFFFF), // White
                size = Size(size.width, size.height)
            )

            // Draw clouds - adjusted for orientation
            val cloudColor = Color(0xFFE6E6E6) // Light gray clouds
            cloudPositions.forEach { position ->
                drawCircle(
                    color = cloudColor,
                    radius = if (isPortrait) 30f else 40f,
                    center = position
                )
                drawCircle(
                    color = cloudColor,
                    radius = if (isPortrait) 25f else 30f,
                    center = Offset(position.x + 30f, position.y - 10f)
                )
                drawCircle(
                    color = cloudColor,
                    radius = if (isPortrait) 28f else 35f,
                    center = Offset(position.x - 25f, position.y + 5f)
                )
            }

            // Draw ground
            drawRect(
                color = Color(0xFF8B4513), // Brown
                topLeft = Offset(0f, groundY),
                size = Size(size.width, size.height - groundY)
            )

            // Draw game objects with appropriate scale factor
            allObjects.forEach { obj ->
                if (obj.isActive) {
                    drawContext.canvas.nativeCanvas.drawBitmap(
                        obj.image,
                        obj.x,
                        obj.y,
                        bitmapPaint // Use the clean paint object
                    )
                }
            }

            // Draw debug hitboxes if enabled
            if (showHitboxes) {
                // Draw koala hitbox
                val koalaFrameHeight = koalaSpriteSheet?.height?.div(TOTAL_FRAMES) ?: 0
                val koalaWidth = koalaSpriteSheet?.width?.toFloat() ?: 0f
                val widthReduction = koalaWidth * 0.15f
                val heightReduction = koalaFrameHeight * 0.15f

                drawRect(
                    color = Color.Red.copy(alpha = 0.5f),
                    topLeft = Offset(
                        koalaX + widthReduction,
                        koalaY + heightReduction
                    ),
                    size = Size(
                        koalaWidth - (widthReduction * 2),
                        koalaFrameHeight - (heightReduction * 2)
                    )
                )

                // Draw object hitboxes
                for (obj in allObjects) {
                    if (obj.isActive) {
                        val bounds = obj.getBounds()
                        val objWidthReduction = bounds.width * 0.15f
                        val objHeightReduction = bounds.height * 0.15f

                        val color = if (obstacles.contains(obj)) Color.Blue.copy(alpha = 0.5f)
                        else Color.Green.copy(alpha = 0.5f)

                        drawRect(
                            color = color,
                            topLeft = Offset(
                                bounds.left + objWidthReduction,
                                bounds.top + objHeightReduction
                            ),
                            size = Size(
                                bounds.width - (objWidthReduction * 2),
                                bounds.height - (objHeightReduction * 2)
                            )
                        )
                    }
                }
            }

            // Draw koala with improved frame isolation and orientation-based scaling
            koalaSpriteSheet?.let { spriteSheet ->
                // Calculate frame dimensions with a 1-pixel buffer between frames
                val frameHeight = spriteSheet.height / TOTAL_FRAMES

                // The source rectangle for the current frame
                val srcRect = Rect(
                    0,  // Left edge of sprite
                    currentFrame * frameHeight + 1,  // Add 1 pixel offset to prevent bleeding
                    spriteSheet.width,  // Right edge
                    (currentFrame + 1) * frameHeight - 1  // Subtract 1 pixel to avoid overlap
                )

                // The destination rectangle with orientation scaling
                val dstRect = RectF(
                    koalaX,
                    koalaY,
                    koalaX + spriteSheet.width * scaleFactor,
                    koalaY + frameHeight * scaleFactor - 2  // Adjust height to account for buffer
                )

                // Draw the current frame with clean rendering
                drawContext.canvas.nativeCanvas.drawBitmap(
                    spriteSheet,
                    srcRect,
                    dstRect,
                    bitmapPaint
                )
            }
        }

        // HUD - Position based on orientation
        if (isPortrait) {
            // Portrait HUD at top center
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = "Score: $score",
                        style = TextStyle(
                            color = Color.Black,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        text = "Lives: $lives",
                        style = TextStyle(
                            color = Color.Black,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Debug controls (only in debug mode)
                if (DEBUG_MODE) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showHitboxes,
                            onCheckedChange = { showHitboxes = it }
                        )

                        Text(
                            text = "Show Hitboxes",
                            style = TextStyle(
                                color = Color.Black,
                                fontSize = 16.sp
                            ),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        } else {
            // Landscape HUD at top right
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
            ) {
                Text(
                    text = "Score: $score",
                    style = TextStyle(
                        color = Color.Black,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Text(
                    text = "Lives: $lives",
                    style = TextStyle(
                        color = Color.Black,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Debug controls (only in debug mode)
                if (DEBUG_MODE) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showHitboxes,
                            onCheckedChange = { showHitboxes = it }
                        )

                        Text(
                            text = "Show Hitboxes",
                            style = TextStyle(
                                color = Color.Black,
                                fontSize = 16.sp
                            ),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            gameState.isGameActive.value = false
        }
    }
}