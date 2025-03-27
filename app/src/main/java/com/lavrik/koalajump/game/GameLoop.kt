package com.lavrik.koalajump.game

import android.content.Context
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.lavrik.koalajump.GameState
import com.lavrik.koalajump.utils.SoundManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

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
        private const val OBSTACLE_SPEED = 16f // Increased from 12f to 16f for faster gameplay
        private const val ACCELERATION_PERIOD = 10000f // Time in ms for game to reach full speed
        private const val BOOSTER_DURATION = 5000L // 5 seconds of boost
        private const val COLLECTIBLE_ATTRACTION_RANGE = 200f // Range for collectible magnet effect

        // Increased spacing constants
        private const val BASE_OBSTACLE_SPACING = 650f // Increased from 400f for more space
        private const val OBSTACLE_SPACING_VARIATION = 300f // Increased from 200f for more variation

        // Vibration constants
        private const val COLLISION_VIBRATION_DURATION = 200L // 200ms vibration on collision
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
    private var gameStartTime: Long = 0

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
    private var isInvincible = false // Added for invincibility tracking
    private var gameSpeedMultiplier = 1.0f
    private var scoreMultiplier = 1 // Added for score multiplier

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
        gameStartTime = SystemClock.uptimeMillis()
        hasSpeedBoost = false
        isInvincible = false
        scoreMultiplier = 1

        // Set up koala position
        koalaX = screenWidth / 4f
        groundY = groundLevel
        koalaY = groundY - koalaHeight

        // Reset obstacles positions with greater spacing
        obstacles = arrayOf(
            screenWidth + BASE_OBSTACLE_SPACING,
            screenWidth + BASE_OBSTACLE_SPACING * 2,
            screenWidth + BASE_OBSTACLE_SPACING * 3
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
        val gameTime = SystemClock.uptimeMillis() - gameStartTime
        val startingSpeedFactor = minOf(1.0f, gameTime / ACCELERATION_PERIOD) // Ramps up to full speed
        val effectiveSpeed = OBSTACLE_SPEED * gameSpeedMultiplier * startingSpeedFactor

        for (i in obstacles.indices) {
            // Move obstacle left
            obstacles[i] -= effectiveSpeed

            // Reset obstacle when it goes off screen
            if (obstacles[i] < -100) { // Assuming obstacle width is around 100px
                // Find the furthest obstacle
                val furthestObstacle = obstacles.maxOrNull() ?: 1000f

                // Place beyond the furthest one with increased spacing
                val boostSpacingMultiplier = if (hasSpeedBoost) 1.75f else 1.0f
                obstacles[i] = furthestObstacle +
                        (BASE_OBSTACLE_SPACING * boostSpacingMultiplier) +
                        (Math.random() * OBSTACLE_SPACING_VARIATION).toFloat()
            }
        }
    }

    /**
     * Move collectibles from right to left
     */
    private fun moveCollectibles() {
        val gameTime = SystemClock.uptimeMillis() - gameStartTime
        val startingSpeedFactor = minOf(1.0f, gameTime / ACCELERATION_PERIOD) // Ramps up to full speed
        val effectiveSpeed = OBSTACLE_SPEED * gameSpeedMultiplier * startingSpeedFactor

        val newCollectibles = Array(collectibles.size) { i ->
            val (x, y, active) = collectibles[i]

            if (active) {
                var newX = x - effectiveSpeed
                var newY = y

                // Collectible magnet effect during boost
                if (hasSpeedBoost) {
                    // Calculate distance to koala
                    val dx = koalaX - newX
                    val dy = koalaY - newY
                    val distance = sqrt(dx * dx + dy * dy)

                    // If within range, move collectible toward koala
                    if (distance < COLLECTIBLE_ATTRACTION_RANGE) {
                        val attractionSpeed = 10f
                        val moveX = dx * attractionSpeed / distance
                        val moveY = dy * attractionSpeed / distance

                        newX += moveX
                        newY += moveY
                    }
                }

                // Reset collectible when it goes off screen
                if (newX < -50) { // Assuming collectible width is around 50px
                    // Find the furthest collectible
                    val furthestCollectible = collectibles.maxOfOrNull { it.first } ?: 1000f

                    // Place beyond the furthest one with spacing
                    Triple(
                        furthestCollectible + 600f + (Math.random() * 300).toFloat(), // Increased spacing
                        groundY - 120f - (Math.random() * 160f).toFloat(), // 20% lower for respawning
                        true
                    )
                } else {
                    Triple(newX, newY, active)
                }
            } else {
                Triple(x, y, active)
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
                // Apply score multiplier during boost
                val pointValue = gameState.currentEnvironment.value.collectibleValue * scoreMultiplier
                score += pointValue

                // Play the appropriate sound
                soundManager.playCollectSound()

                // Mark as collected
                collectibles[i] = Triple(x, y, false)

                // Handle booster collectibles
                val isBooster = (i == collectibles.size - 1) // Assume the last collectible is a booster
                if (isBooster) {
                    activateBooster()
                }
            }
        }
    }

    /**
     * Activate booster power-up
     */
    private fun activateBooster() {
        if (!hasSpeedBoost) {
            hasSpeedBoost = true
            isInvincible = true
            scoreMultiplier = 2
            gameSpeedMultiplier = 1.5f

            // Update koala's visual state
            gameState.getKoala()?.setPowerUpState(true)
            gameState.getKoala()?.setInvincibleState(true)

            // Cancel previous job if exists
            speedBoostJob?.cancel()

            // Play a special power-up sound
            // Note: Add a power-up sound to SoundManager
            soundManager.playCollectSound() // Replace with power-up sound when available

            // Schedule end of speed boost with proper coroutine scope
            speedBoostJob = coroutineScope.launch {
                delay(BOOSTER_DURATION) // 5 seconds
                hasSpeedBoost = false
                isInvincible = false
                scoreMultiplier = 1
                gameSpeedMultiplier = 1.0f

                // Update koala's visual state back to normal
                gameState.getKoala()?.setPowerUpState(false)
                gameState.getKoala()?.setInvincibleState(false)
            }
        } else {
            // If already boosting, extend the duration
            speedBoostJob?.cancel()

            speedBoostJob = coroutineScope.launch {
                delay(BOOSTER_DURATION) // 5 seconds
                hasSpeedBoost = false
                isInvincible = false
                scoreMultiplier = 1
                gameSpeedMultiplier = 1.0f

                // Update koala's visual state back to normal
                gameState.getKoala()?.setPowerUpState(false)
                gameState.getKoala()?.setInvincibleState(false)
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
        // If invincible, ignore collision
        if (isInvincible) {
            return
        }

        // Decrease lives
        lives--
        gameState.lives.value = lives

        // Play hit sound
        soundManager.playHitSound()

        // Add vibration when collision occurs
        if (gameState.vibrationEnabled.value) {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(
                    COLLISION_VIBRATION_DURATION,
                    VibrationEffect.DEFAULT_AMPLITUDE
                ))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(COLLISION_VIBRATION_DURATION)
            }
        }

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
     * Check if player is invincible
     */
    fun isInvincible(): Boolean = isInvincible

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