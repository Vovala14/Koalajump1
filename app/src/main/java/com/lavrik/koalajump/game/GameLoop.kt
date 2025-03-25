package com.lavrik.koalajump.game

import android.content.Context
import android.util.Log
import com.lavrik.koalajump.GameState
import com.lavrik.koalajump.utils.SoundManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Improved game loop with basic movement and jumping physics
 */
class GameLoop(
    private val context: Context,
    private val gameState: GameState,
    private val soundManager: SoundManager
) {
    companion object {
        private const val TAG = "GameLoop"
        private const val FRAME_DELAY = 33L // ~30fps
        private const val JUMP_VELOCITY = -20f // Setting that worked well
        private const val GRAVITY = 1.6f // Gravity pulling down
        private const val OBSTACLE_SPEED = 12f // Speed of obstacles moving left
    }

    // Coroutine scope for launching background tasks
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var speedBoostJob: Job? = null

    // Game state
    private var score = 0
    private var lives = 3
    private var currentLevel = 1
    private var isGameActive = true
    private var isGameOver = false  // New flag to track game over state

    // Koala position and movement
    private var koalaX = 0f
    private var koalaY = 0f
    private var groundY = 0f
    private var koalaHeight = 100f

    // Jump mechanics
    private var isJumping = false
    private var jumpVelocity = JUMP_VELOCITY

    // Game mechanics
    private var hasSpeedBoost = false
    private var gameSpeedMultiplier = 1.0f

    // Game objects
    private var obstacles = arrayOf(500f, 800f, 1200f)
    private var collectibles = arrayOf(
        Triple(600f, 300f, true),
        Triple(900f, 350f, true),
        Triple(1300f, 400f, true)
    )

    /**
     * Initialize the game loop with screen dimensions
     */
    fun initialize(screenWidth: Float, screenHeight: Float, groundLevel: Float) {
        score = 0
        lives = 3
        currentLevel = 1
        isGameOver = false
        isGameActive = true

        // Set up koala position
        koalaX = screenWidth / 4f
        groundY = groundLevel
        koalaY = groundY - koalaHeight

        // Reset obstacles positions
        obstacles = arrayOf(
            screenWidth + 400f,
            screenWidth + 800f,
            screenWidth + 1200f
        )

        // Reset collectibles
        collectibles = arrayOf(
            Triple(screenWidth + 600f, groundY - 160f, true),  // 20% lower (was 200f)
            Triple(screenWidth + 1000f, groundY - 200f, true), // 20% lower (was 250f)
            Triple(screenWidth + 1400f, groundY - 120f, true)  // 20% lower (was 150f)
        )

        // Make sure game state is consistent
        gameState.resetForNewGame()
    }

    /**
     * Start the game loop with physics and movement
     */
    suspend fun start(onGameOver: () -> Unit) {
        isGameActive = true
        isGameOver = false
        gameState.isGameActive.value = true

        // Game loop
        while (isGameActive && gameState.isGameActive.value && !isGameOver) {
            try {
                // Update jumping physics
                updateJumpPhysics()

                // Move obstacles
                moveObstacles()

                // Move collectibles
                moveCollectibles()

                // Check for collectible collisions - simplified
                checkCollectibleCollisions()

                // Update game state values
                gameState.score.value = score
                gameState.lives.value = lives

                // Check for environment changes based on score
                checkForEnvironmentChange()

                // Check for game over
                if (lives <= 0 && !isGameOver) {
                    isGameOver = true
                    isGameActive = false
                    gameState.isGameActive.value = false

                    // Update final score
                    gameState.updateScore(score)
                    gameState.endGame()

                    Log.d(TAG, "Game over detected in game loop - calling onGameOver callback")

                    // Delay slightly to ensure state updates
                    delay(100)

                    // Call the game over callback on the main thread
                    try {
                        // Instead of using withContext which requires an additional import,
                        // we'll use the existing coroutineScope to launch on the Main dispatcher
                        coroutineScope.launch(Dispatchers.Main) {
                            try {
                                onGameOver()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in onGameOver callback: ${e.message}", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error calling game over callback: ${e.message}", e)
                    }

                    break
                }
            } catch (e: Exception) {
                // Catch any exceptions in the game loop to prevent crashes
                Log.e(TAG, "Error in game loop: ${e.message}", e)
            }

            // Add delay to control frame rate
            delay(FRAME_DELAY)
        }

        // Double-check game over condition when exiting loop
        if (lives <= 0 && !gameState.gameOverState) {
            gameState.updateScore(score)
            gameState.endGame()

            try {
                // Again using coroutineScope.launch instead of withContext
                coroutineScope.launch(Dispatchers.Main) {
                    try {
                        onGameOver()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in final onGameOver callback: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calling final game over callback: ${e.message}", e)
            }
        }
    }

    /**
     * Check for level changes based on score
     */
    private fun checkForEnvironmentChange() {
        gameState.updateEnvironment(score)

        // Apply environment effects
        val environment = gameState.currentEnvironment.value
        gameSpeedMultiplier = environment.speedMultiplier
    }

    /**
     * Update jumping physics for the koala
     */
    private fun updateJumpPhysics() {
        if (isJumping) {
            // Apply velocity to position
            koalaY += jumpVelocity

            // Apply gravity to velocity
            jumpVelocity += GRAVITY

            // Check if landed
            if (koalaY >= groundY - koalaHeight) {
                koalaY = groundY - koalaHeight
                isJumping = false
                jumpVelocity = JUMP_VELOCITY
            }
        }
    }

    /**
     * Move obstacles from right to left
     */
    private fun moveObstacles() {
        val effectiveSpeed = OBSTACLE_SPEED * gameSpeedMultiplier

        for (i in obstacles.indices) {
            // Move obstacle left
            obstacles[i] -= effectiveSpeed

            // Reset obstacle when it goes off screen
            if (obstacles[i] < -100) { // Assuming obstacle width is around 100px
                // Find the furthest obstacle
                val furthestObstacle = obstacles.maxOrNull() ?: 1000f

                // Place beyond the furthest one with spacing
                obstacles[i] = furthestObstacle + 400f + (Math.random() * 200).toFloat()
            }
        }
    }

    /**
     * Move collectibles from right to left
     */
    private fun moveCollectibles() {
        val effectiveSpeed = OBSTACLE_SPEED * gameSpeedMultiplier
        val newCollectibles = Array(collectibles.size) { i ->
            val (x, y, active) = collectibles[i]

            // Move collectible left if active
            val newX = if (active) x - effectiveSpeed else x

            // Reset collectible when it goes off screen
            if (newX < -50) { // Assuming collectible width is around 50px
                // Find the furthest collectible
                val furthestCollectible = collectibles.maxOfOrNull { it.first } ?: 1000f

                // Place beyond the furthest one with spacing
                Triple(
                    furthestCollectible + 400f + (Math.random() * 300).toFloat(),
                    groundY - 120f - (Math.random() * 160f).toFloat(), // 20% lower for respawning
                    true
                )
            } else {
                Triple(newX, y, active)
            }
        }
        collectibles = newCollectibles
    }

    /**
     * Simple collision detection for collectibles
     */
    private fun checkCollectibleCollisions() {
        // Very simple collision detection - can be improved
        for (i in collectibles.indices) {
            val (x, y, active) = collectibles[i]

            if (active && Math.abs(x - koalaX) < 70 && Math.abs(y - koalaY) < 70) {
                // Collision detected!
                // Use the collectible value from current environment
                score += gameState.currentEnvironment.value.collectibleValue
                soundManager.playCollectSound()

                // Mark as collected
                collectibles[i] = Triple(x, y, false)

                // Randomly trigger speed boost
                if (!hasSpeedBoost && Math.random() < 0.3) {
                    hasSpeedBoost = true
                    gameSpeedMultiplier = 1.5f

                    // Cancel previous job if exists
                    speedBoostJob?.cancel()

                    // Schedule end of speed boost with proper coroutine scope
                    speedBoostJob = coroutineScope.launch {
                        delay(5000) // 5 seconds
                        hasSpeedBoost = false
                        gameSpeedMultiplier = 1.0f
                    }
                }
            }
        }
    }

    /**
     * Handle a jump action
     */
    fun jump() {
        if (!isJumping) {
            isJumping = true
            jumpVelocity = JUMP_VELOCITY
            soundManager.playJumpSound()
        }
    }

    /**
     * Handle collision with obstacles
     */
    fun handleCollision() {
        // Decrease lives
        lives--
        gameState.lives.value = lives

        // Play hit sound
        soundManager.playHitSound()

        // Check for game over
        if (lives <= 0 && !isGameOver) {
            // Mark game as over
            isGameOver = true
            isGameActive = false
            gameState.isGameActive.value = false

            // Update score in game state
            gameState.updateScore(score)
            gameState.endGame()

            Log.d(TAG, "Game over in handleCollision - lives: $lives")
        }
    }

    /**
     * Get the current koala Y position
     */
    fun getKoalaY(): Float = koalaY

    /**
     * Get the current game state values
     */
    fun getGameState(): Triple<Int, Int, Int> {
        return Triple(score, lives, currentLevel)
    }

    /**
     * Check if speed boost is active
     */
    fun hasSpeedBoost(): Boolean = hasSpeedBoost

    /**
     * Get the current obstacles positions
     */
    fun getObstacles(): Array<Float> = obstacles

    /**
     * Get the current collectibles
     */
    fun getCollectibles(): Array<Triple<Float, Float, Boolean>> = collectibles

    /**
     * Force end the game and navigate to game over
     */
    fun forceGameOver() {
        if (!isGameOver) {
            lives = 0
            gameState.lives.value = 0
            isGameOver = true
            isGameActive = false
            gameState.isGameActive.value = false
            gameState.updateScore(score)
            gameState.endGame()
        }
    }

    /**
     * Stop the game loop
     */
    fun stop() {
        isGameActive = false
        speedBoostJob?.cancel()
    }
}