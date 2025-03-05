package com.lavrik.koalajump

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

/**
 * Central game state manager with improved state handling
 */
class GameState {
    companion object {
        private const val TAG = "GameState"
        private const val INITIAL_LIVES = 3
        private const val INITIAL_LEVEL = 1
        private const val INITIAL_SPEED = 12f  // Increased from 10f for faster gameplay
        private const val SPEED_INCREASE_PER_LEVEL = 0.8f  // Increased from 0.5f for more challenge
        private const val MAX_LIVES = 5
        private const val MAX_GAME_SPEED = 25f  // New constant to cap max speed
    }

    // Game status
    val isGameActive = mutableStateOf(false)

    // Orientation preference - using a private MutableLiveData with public accessor methods
    private val allowRotation = MutableLiveData(false)

    // Scoring
    val score = mutableStateOf(0)
    val finalScore = mutableStateOf(0)
    val highScore = mutableStateOf(0)

    // Achievement tracking
    private val _achievedHighScores = mutableListOf<Int>()
    val achievedHighScores: List<Int> get() = _achievedHighScores.toList()

    // Game difficulty
    val gameSpeed = mutableStateOf(INITIAL_SPEED)

    // Player lives
    val lives = mutableStateOf(INITIAL_LIVES)

    // Current game level
    val currentLevel = mutableStateOf(INITIAL_LEVEL)

    // Recent collectibles for combo tracking
    private val _recentCollectTimes = mutableListOf<Long>()

    // Current combo counter
    val currentCombo = mutableStateOf(0)

    // Game settings
    val soundEnabled = mutableStateOf(true)
    val vibrationEnabled = mutableStateOf(true)

    /**
     * Get the allow rotation value
     */
    fun getAllowRotation(): Boolean {
        return allowRotation.value ?: false
    }

    /**
     * Set allow rotation value
     */
    fun setAllowRotation(allow: Boolean) {
        allowRotation.value = allow
    }

    /**
     * Observe allow rotation changes
     */
    fun observeAllowRotation(owner: LifecycleOwner, observer: Observer<Boolean>) {
        allowRotation.observe(owner, observer)
    }

    /**
     * Reset game state for a new game
     */
    fun resetForNewGame() {
        Log.d(TAG, "Resetting game state")
        isGameActive.value = true
        score.value = 0
        finalScore.value = 0
        lives.value = INITIAL_LIVES
        currentLevel.value = INITIAL_LEVEL
        gameSpeed.value = INITIAL_SPEED
        currentCombo.value = 0
        _recentCollectTimes.clear()
    }

    /**
     * Add points to current score
     */
    fun addPoints(points: Int) {
        // Apply combo multiplier if applicable
        val comboMultiplier = calculateComboMultiplier()
        val adjustedPoints = (points * comboMultiplier).toInt()

        score.value += adjustedPoints
        Log.d(TAG, "Added $adjustedPoints points (base: $points, multiplier: $comboMultiplier)")
    }

    /**
     * Record a collectible being collected and update combo
     */
    fun recordCollectible() {
        val currentTime = System.currentTimeMillis()

        // Add to recent collectibles
        _recentCollectTimes.add(currentTime)

        // Remove collectibles older than 3 seconds
        val threshold = currentTime - 3000
        _recentCollectTimes.removeAll { it < threshold }

        // Update combo counter
        currentCombo.value = _recentCollectTimes.size

        // Add points with potential combo bonus
        addPoints(10)
    }

    /**
     * Calculate combo multiplier based on recent collectibles
     * Returns a multiplier between 1.0 and 2.0
     */
    private fun calculateComboMultiplier(): Float {
        // No combo if less than 2 recent collectibles
        if (_recentCollectTimes.size < 2) return 1.0f

        // Cap at 2.0x for 5 or more collectibles in combo
        return 1.0f + minOf(_recentCollectTimes.size * 0.2f, 1.0f)
    }

    /**
     * Update final score at end of game
     */
    fun updateScore(newScore: Int) {
        Log.d(TAG, "Updating final score: $newScore")
        finalScore.value = newScore

        // Update high score if needed
        if (newScore > highScore.value) {
            highScore.value = newScore
            _achievedHighScores.add(newScore)
            Log.d(TAG, "New high score: ${highScore.value}")
        }
    }

    /**
     * Decrease lives
     * @return true if still alive, false if game over
     */
    fun decreaseLife(): Boolean {
        lives.value--
        Log.d(TAG, "Life lost. Remaining lives: ${lives.value}")

        // Reset combo on hit
        _recentCollectTimes.clear()
        currentCombo.value = 0

        return lives.value > 0
    }

    /**
     * Add an extra life
     */
    fun addLife() {
        if (lives.value < MAX_LIVES) {
            lives.value++
            Log.d(TAG, "Life gained. Lives: ${lives.value}")
        }
    }

    /**
     * Increase level and adjust difficulty
     */
    fun increaseLevel() {
        currentLevel.value++

        // Increase game speed with level, but cap at maximum speed
        val newSpeed = minOf(gameSpeed.value + SPEED_INCREASE_PER_LEVEL, MAX_GAME_SPEED)
        gameSpeed.value = newSpeed

        Log.d(TAG, "Level up. Current level: ${currentLevel.value}, Speed: ${gameSpeed.value}")

        // Award bonus life at specific level milestones
        if (currentLevel.value % 5 == 0 && lives.value < MAX_LIVES) {
            lives.value++
            Log.d(TAG, "Bonus life awarded at level ${currentLevel.value}")
        }
    }

    /**
     * End the game
     */
    fun endGame() {
        Log.d(TAG, "Game ended. Final score: ${score.value}")
        isGameActive.value = false
        finalScore.value = score.value
        updateHighScore()
    }

    /**
     * Check if game is over
     */
    fun isGameOver(): Boolean {
        val gameOver = lives.value <= 0
        if (gameOver) {
            Log.d(TAG, "Game over - no lives remaining")
        }
        return gameOver
    }

    /**
     * Update high score
     */
    private fun updateHighScore() {
        if (finalScore.value > highScore.value) {
            highScore.value = finalScore.value
            _achievedHighScores.add(finalScore.value)
            Log.d(TAG, "High score updated to: ${highScore.value}")
        }
    }

    /**
     * Toggle sound settings
     */
    fun toggleSound() {
        soundEnabled.value = !soundEnabled.value
        Log.d(TAG, "Sound ${if (soundEnabled.value) "enabled" else "disabled"}")
    }

    /**
     * Toggle vibration settings
     */
    fun toggleVibration() {
        vibrationEnabled.value = !vibrationEnabled.value
        Log.d(TAG, "Vibration ${if (vibrationEnabled.value) "enabled" else "disabled"}")
    }

    /**
     * Toggle orientation locking
     */
    fun toggleOrientationLock() {
        setAllowRotation(!getAllowRotation())
        Log.d(TAG, "Orientation lock ${if (getAllowRotation()) "disabled" else "enabled"}")
    }

    /**
     * Reset everything to initial state
     */
    fun fullReset() {
        Log.d(TAG, "Performing full game state reset")
        isGameActive.value = false
        score.value = 0
        finalScore.value = 0
        highScore.value = maxOf(highScore.value, finalScore.value)
        lives.value = INITIAL_LIVES
        currentLevel.value = INITIAL_LEVEL
        gameSpeed.value = INITIAL_SPEED
        currentCombo.value = 0
        _recentCollectTimes.clear()
    }
}