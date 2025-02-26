package com.lavrik.koalajump.screens

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lavrik.koalajump.GameState
import com.lavrik.koalajump.Obstacle
import com.lavrik.koalajump.Collectible
import com.lavrik.koalajump.R
import kotlinx.coroutines.delay

@Composable
fun MainGameScreen(
    gameState: GameState,
    onGameOver: (Int) -> Unit
) {
    // Get screen width in pixels
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

    // Local mutable state to ensure recomposition
    var obstacles by remember { mutableStateOf(listOf<Obstacle>()) }
    var collectibles by remember { mutableStateOf(listOf<Collectible>()) }
    var gameSpeed by remember { mutableStateOf(15f) } // Increased speed from 5 to 15

    Log.d("GAME_DEBUG", "MainGameScreen composing with ${obstacles.size} obstacles and speed $gameSpeed. Screen width: ${configuration.screenWidthDp}dp")

    // Game loop
    LaunchedEffect(Unit) {
        Log.d("GAME_DEBUG", "Game loop started with screen width: $screenWidthPx")
        var lastTime = System.currentTimeMillis()
        var obstacleTimer = 0f
        var collectibleTimer = 0f

        while (true) {
            if (gameState.lives <= 0) {
                gameState.isGameOver = true
                onGameOver(gameState.score)
                break
            }

            delay(16) // ~60 FPS
            val currentTime = System.currentTimeMillis()
            val elapsed = (currentTime - lastTime) / 1000f
            lastTime = currentTime

            // Increase difficulty based on score
            gameSpeed = 15f + (gameState.score / 10f)

            // Move obstacles
            obstacles = obstacles.map {
                it.copy(x = it.x - gameSpeed)
            }.filter { it.x > -100f }

            // Update obstacle timer
            obstacleTimer += elapsed

            // Spawn obstacles - Now starting just off screen to the right
            if ((obstacles.isEmpty() || obstacles.all { it.x < screenWidthPx - 300 }) && obstacleTimer >= 2f) {
                Log.d("GAME_DEBUG", "Spawning new obstacle at timer: $obstacleTimer")
                obstacles = obstacles + Obstacle(
                    x = screenWidthPx + 60f, // Start just off the right edge of screen
                    y = 0f,
                    type = "tree"
                )
                obstacleTimer = 0f
            }

            // Move collectibles
            collectibles = collectibles.map {
                it.copy(x = it.x - gameSpeed)
            }.filter { it.x > -100f }

            // Update collectible timer
            collectibleTimer += elapsed

            // Spawn collectibles - Now starting just off screen to the right
            if (Math.random() < 0.02 && collectibles.size < 2 && collectibleTimer >= 1f) {
                Log.d("GAME_DEBUG", "Spawning new collectible")
                collectibles = collectibles + Collectible(
                    x = screenWidthPx + 40f, // Start just off the right edge of screen
                    y = -200f - (Math.random() * 100).toFloat(),
                    type = if (Math.random() < 0.7) "beer" else "crown"
                )
                collectibleTimer = 0f
            }

            // Collision detection
            val koalaX = 50f
            val koalaY = if (gameState.isJumping) -200f else 0f
            val koalaSize = 100f

            // Check obstacle collisions
            obstacles.forEach { obstacle ->
                if (checkCollision(
                        koalaX, koalaY, koalaSize,
                        obstacle.x, 0f, 60f
                    )) {
                    Log.d("GAME_DEBUG", "Collision with obstacle")
                    gameState.lives--
                    obstacles = obstacles.filter { it != obstacle }
                }
            }

            // Check collectible collisions
            collectibles.forEach { collectible ->
                if (checkCollision(
                        koalaX, koalaY, koalaSize,
                        collectible.x, collectible.y, 40f
                    )) {
                    Log.d("GAME_DEBUG", "Collected ${collectible.type}")
                    if (collectible.type == "crown") {
                        gameState.score += 5
                    } else {
                        gameState.score++
                    }
                    collectibles = collectibles.filter { it != collectible }
                }
            }
        }
    }

    // Main game UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                detectTapGestures {
                    Log.d("GAME_DEBUG", "Screen tapped at (${it.x}, ${it.y})!")
                    if (!gameState.isJumping) {
                        Log.d("GAME_DEBUG", "Jump started!")
                        gameState.isJumping = true
                    }
                }
            }
    ) {
        // Score and Lives
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Score: ${gameState.score}",
                fontSize = 18.sp,
                color = Color.Black
            )
            Row {
                repeat(gameState.lives) {
                    Text(
                        text = "❤️",
                        fontSize = 18.sp
                    )
                }
            }
        }

        // Character
        val animatedY by animateFloatAsState(
            targetValue = if (gameState.isJumping) -200f else 0f,
            animationSpec = tween(200), // Even faster animation (was 300)
            label = "jump",
            finishedListener = {
                Log.d("GAME_DEBUG", "Jump animation completed")
                gameState.isJumping = false
            }
        )

        Box(
            modifier = Modifier
                .size(100.dp)
                .offset(x = 50.dp, y = animatedY.dp)
                .align(Alignment.BottomStart)
                .padding(bottom = 80.dp)
        ) {
            Image(
                painter = painterResource(
                    id = when(gameState.selectedCharacter) {
                        1 -> R.drawable.koala_sport
                        2 -> R.drawable.koala_fitness
                        else -> R.drawable.koala_ninja
                    }
                ),
                contentDescription = "Koala",
                modifier = Modifier.fillMaxSize()
            )
        }

        // Draw obstacles
        obstacles.forEach { obstacle ->
            Log.d("GAME_DEBUG", "Drawing obstacle at x=${obstacle.x}")
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .offset(x = obstacle.x.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = (-140).dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tree),
                    contentDescription = "Tree",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Draw collectibles
        collectibles.forEach { collectible ->
            Log.d("GAME_DEBUG", "Drawing collectible at x=${collectible.x}, y=${collectible.y}, type=${collectible.type}")
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .offset(x = collectible.x.dp, y = collectible.y.dp)
                    .align(Alignment.BottomStart)
            ) {
                Image(
                    painter = painterResource(
                        id = if (collectible.type == "beer") R.drawable.beer else R.drawable.crown
                    ),
                    contentDescription = collectible.type,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Ground
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color.Green)
                .align(Alignment.BottomCenter)
        )
    }
}

private fun checkCollision(
    x1: Float, y1: Float, size1: Float,
    x2: Float, y2: Float, size2: Float
): Boolean {
    val padding = 20f
    return (x1 < x2 + size2 - padding &&
            x1 + size1 > x2 + padding &&
            y1 < y2 + size2 - padding &&
            y1 + size1 > y2 + padding)
}