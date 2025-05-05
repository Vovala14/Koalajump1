package com.lavrik.koalajump

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.lavrik.koalajump.entities.AnimatedKoala
import com.lavrik.koalajump.game.GameEnvironment

/**
 * Central game state manager with improved state handling and persistence
 * Orientation handling removed - portrait mode only
 */
class GameState(private val context: Context) {
    companion object {
        private const val TAG = "GameState"
        private const val INITIAL_LIVES = 3
        private const val INITIAL_LEVEL = 1
        private const val INITIAL_SPEED = 16f  // Increased from 12f to 16f for faster gameplay
        private const val SPEED_INCREASE_PER_LEVEL = 1.2f  // Increased from 0.8f to 1.2f for more challenge
        private const val MAX_LIVES = 5
        private const val MAX_GAME_SPEED = 35f  // Increased from 25f to 35f to cap max speed

        // Preference keys
        private const val PREFS_NAME = "game_prefs"
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"

        // Level transition constants
        private const val LEVEL_TRANSITION_INVINCIBILITY_DURATION = 4000L // 4 seconds of invincibility on level up

        // Ad-related invincibility constants (NEW)
        private const val AD_REWARD_INVINCIBILITY_DURATION = 3000L // 3 seconds of invincibility after ad reward
    }

    // SharedPreferences for persistent storage
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Game status
    val isGameActive = mutableStateOf(false)

    // Game Over state - renamed to avoid clash
    private var _gameOverState = false
    val gameOverState: Boolean get() = _gameOverState

    // Flag to track if we're continuing after watching an ad (NEW)
    var continueFromGameOver = false

    // Scoring
    val score = mutableStateOf(0)
    val finalScore = mutableStateOf(0)
    val highScore = mutableStateOf(loadHighScore())

    // Achievement tracking
    private val _achievedHighScores = mutableListOf<Int>()
    val achievedHighScores: List<Int> get() = _achievedHighScores.toList()

    // Game difficulty
    val gameSpeed = mutableStateOf(INITIAL_SPEED)

    // Player lives
    val lives = mutableStateOf(INITIAL_LIVES)

    // Current game level
    val currentLevel = mutableStateOf(INITIAL_LEVEL)

    // Flag to track when a level transition is happening
    val isLevelTransitioning = mutableStateOf(false)
    val levelTransitionInvincibilityEnd = mutableStateOf(0L)

    // Temporary invincibility system (for ad rewards, etc.) (NEW)
    private val _isTemporarilyInvincible = mutableStateOf(false)
    val isTemporarilyInvincible: Boolean get() = _isTemporarilyInvincible.value
    private val temporaryInvincibilityEnd = mutableStateOf(0L)

    // Current environment
    val currentEnvironment = mutableStateOf(GameEnvironment.FOREST)

    // Recent collectibles for combo tracking
    private val _recentCollectTimes = mutableListOf<Long>()

    // Current combo counter
    val currentCombo = mutableStateOf(0)

    // Game settings - Load from SharedPreferences
    val soundEnabled = mutableStateOf(loadSoundSetting())
    val vibrationEnabled = mutableStateOf(loadVibrationSetting())

    // Koala reference for visual effects
    private var koala: AnimatedKoala? = null

    // Private methods to load settings from SharedPreferences with backup recovery
    private fun loadHighScore(): Int {
        // Try to load from primary location
        val highScore = prefs.getInt(KEY_HIGH_SCORE, 0)

        // If high score is 0, try to recover from backup
        if (highScore == 0) {
            try {
                val backupPrefs = context.getSharedPreferences("backup_game_prefs", Context.MODE_PRIVATE)
                val backupHighScore = backupPrefs.getInt("backup_high_score", 0)

                if (backupHighScore > 0) {
                    // Found a backup, restore it to primary location
                    Log.d(TAG, "Recovered high score $backupHighScore from backup")
                    prefs.edit().putInt(KEY_HIGH_SCORE, backupHighScore).commit()
                    return backupHighScore
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error accessing backup: ${e.message}")
            }
        }

        return highScore
    }

    private fun loadSoundSetting(): Boolean = prefs.getBoolean(KEY_SOUND_ENABLED, true)

    private fun loadVibrationSetting(): Boolean = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)

    /**
     * Set the koala reference for power-up effects
     */
    fun setKoala(newKoala: AnimatedKoala) {
        koala = newKoala
    }

    /**
     * Get the koala for visual effects
     */
    fun getKoala(): AnimatedKoala? {
        return koala
    }

    /**
     * Save sound setting to preferences
     */
    fun saveSoundSetting(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
        Log.d(TAG, "Sound setting saved: $enabled")
    }

    /**
     * Save vibration setting to preferences
     */
    fun saveVibrationSetting(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
        Log.d(TAG, "Vibration setting saved: $enabled")
    }

    /**
     * Set temporary invincibility for a short duration (NEW)
     * Used for ad rewards and other special cases
     */
    fun setTemporaryInvincibility(invincible: Boolean) {
        _isTemporarilyInvincible.value = invincible

        if (invincible) {
            // Calculate end time
            val endTime = System.currentTimeMillis() + AD_REWARD_INVINCIBILITY_DURATION
            temporaryInvincibilityEnd.value = endTime

            // Apply visual effect to koala if available
            koala?.setInvincibleState(true)

            // Automatically disable invincibility after the duration
            Handler(Looper.getMainLooper()).postDelayed({
                _isTemporarilyInvincible.value = false
                koala?.setInvincibleState(false)
                Log.d(TAG, "Temporary invincibility expired")
            }, AD_REWARD_INVINCIBILITY_DURATION)

            Log.d(TAG, "Temporary invincibility activated for ${AD_REWARD_INVINCIBILITY_DURATION}ms")
        } else {
            // Immediately end invincibility if requested
            temporaryInvincibilityEnd.value = 0
            koala?.setInvincibleState(false)
        }
    }

    /**
     * Check if player is currently invincible from any source (NEW)
     */
    fun isInvincible(): Boolean {
        return isLevelTransitionInvincible() || isTemporarilyInvincible
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
        currentEnvironment.value = GameEnvironment.FOREST
        gameSpeed.value = INITIAL_SPEED
        currentCombo.value = 0
        isLevelTransitioning.value = false
        levelTransitionInvincibilityEnd.value = 0L
        _isTemporarilyInvincible.value = false // NEW
        temporaryInvincibilityEnd.value = 0L // NEW
        _recentCollectTimes.clear()
        _gameOverState = false
        // Don't reset continueFromGameOver flag here - it's handled separately
    }

    /**
     * Update environment based on score
     * Returns true if a level change occurred
     */
    fun updateEnvironment(score: Int): Boolean {
        val previousLevel = currentLevel.value
        val newEnvironment = GameEnvironment.getEnvironmentForScore(score)
        currentLevel.value = GameEnvironment.getLevelForScore(score)

        // Check if level actually changed
        val levelChanged = previousLevel != currentLevel.value

        // If level changed, start transition effects
        if (levelChanged) {
            Log.d(TAG, "Level changed from $previousLevel to ${currentLevel.value}")
            startLevelTransition()
        }

        // Only update if environment changed
        if (newEnvironment != currentEnvironment.value) {
            currentEnvironment.value = newEnvironment
        }

        return levelChanged
    }

    /**
     * Starts the level transition effects
     */
    private fun startLevelTransition() {
        isLevelTransitioning.value = true

        // Set invincibility for 4 seconds on level transition
        val endTime = System.currentTimeMillis() + LEVEL_TRANSITION_INVINCIBILITY_DURATION
        levelTransitionInvincibilityEnd.value = endTime

        // Apply visual effects to koala
        koala?.setInvincibleState(true)

        Log.d(TAG, "Level transition started - invincible until $endTime")
    }

    /**
     * Checks if player is currently invincible due to level transition
     */
    fun isLevelTransitionInvincible(): Boolean {
        val currentTime = System.currentTimeMillis()
        val isInvincible = currentTime < levelTransitionInvincibilityEnd.value

        // If invincibility just ended, update state and visual
        if (!isInvincible && isLevelTransitioning.value) {
            isLevelTransitioning.value = false
            koala?.setInvincibleState(false)
            Log.d(TAG, "Level transition invincibility ended")
        }

        return isInvincible
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

        // Update environment based on new score
        updateEnvironment(score.value)
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

            // Save high score to preferences - FIXED: Use commit() for immediate persistence
            val success = prefs.edit().putInt(KEY_HIGH_SCORE, newScore).commit()
            Log.d(TAG, "New high score: ${highScore.value} saved to preferences. Success: $success")

            // Create a backup of the high score for redundancy
            try {
                val backupPrefs = context.getSharedPreferences("backup_game_prefs", Context.MODE_PRIVATE)
                backupPrefs.edit().putInt("backup_high_score", newScore).commit()
                Log.d(TAG, "High score backup created")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create high score backup: ${e.message}")
            }
        }
    }

    /**
     * Decrease lives
     * @return true if still alive, false if game over
     */
    fun decreaseLife(): Boolean {
        // MODIFIED: Check for invincibility from ANY source (level transition OR temporary)
        if (isInvincible()) {
            Log.d(TAG, "Hit while invincible - no life lost")
            return true
        }

        lives.value--
        Log.d(TAG, "Life lost. Remaining lives: ${lives.value}")

        // Reset combo on hit
        _recentCollectTimes.clear()
        currentCombo.value = 0

        // NEW: Provide brief invincibility after being hit
        setTemporaryInvincibility(true)

        // Check for game over
        if (lives.value <= 0) {
            _gameOverState = true
            endGame()
            return false
        }

        return lives.value > 0
    }

    /**
     * Add an extra life
     */
    fun addLife() {
        if (lives.value < MAX_LIVES) {
            lives.value++
            Log.d(TAG, "Life gained. Lives: ${lives.value}")

            // NEW: Give temporary invincibility when adding a life (for ad rewards)
            setTemporaryInvincibility(true)
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

        // Start transition effects
        startLevelTransition()
    }

    /**
     * End the game
     */
    fun endGame() {
        Log.d(TAG, "Game ended. Final score: ${score.value}")

        // Mark game as over and inactive
        _gameOverState = true
        isGameActive.value = false

        // Set final score
        finalScore.value = score.value

        // Make sure lives are set to 0 to ensure game over state is properly detected
        if (lives.value > 0) {
            lives.value = 0
        }

        // Update high score
        updateHighScore()
    }

    /**
     * Check if game is over - renamed to avoid declaration clash
     */
    fun checkGameOver(): Boolean {
        val gameOver = lives.value <= 0 || _gameOverState

        // If we detect game over condition but haven't marked game as over yet, do it now
        if (gameOver && isGameActive.value) {
            Log.d(TAG, "Game over condition detected - ending game")
            endGame()
        }

        return gameOver
    }

    /**
     * Update high score with improved persistence
     */
    private fun updateHighScore() {
        if (finalScore.value > highScore.value) {
            highScore.value = finalScore.value
            _achievedHighScores.add(finalScore.value)

            // Save high score to preferences with commit() for immediate write
            val success = prefs.edit().putInt(KEY_HIGH_SCORE, finalScore.value).commit()
            Log.d(TAG, "High score updated to: ${highScore.value}, saved successfully: $success")

            // Backup the high score to a second preference location for redundancy
            try {
                val backupPrefs = context.getSharedPreferences("backup_game_prefs", Context.MODE_PRIVATE)
                backupPrefs.edit().putInt("backup_high_score", finalScore.value).commit()
                Log.d(TAG, "High score backup created")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create high score backup: ${e.message}")
            }
        }
    }

    /**
     * Toggle sound settings
     */
    fun toggleSound() {
        soundEnabled.value = !soundEnabled.value

        // Save to preferences
        saveSoundSetting(soundEnabled.value)

        Log.d(TAG, "Sound ${if (soundEnabled.value) "enabled" else "disabled"}")
    }

    /**
     * Toggle vibration settings
     */
    fun toggleVibration() {
        vibrationEnabled.value = !vibrationEnabled.value

        // Save to preferences
        saveVibrationSetting(vibrationEnabled.value)

        Log.d(TAG, "Vibration ${if (vibrationEnabled.value) "enabled" else "disabled"}")
    }

    /**
     * Save boolean value to preferences (NEW)
     */
    fun saveBoolean(key: String, value: Boolean) {
        val sharedPreferences = context.getSharedPreferences("koala_jump_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    /**
     * Get boolean value from preferences (NEW)
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    /**
     * Check if we're continuing from an ad and clear the flag (NEW)
     */
    fun checkAndClearContinuingFromAd(): Boolean {
        val continuing = getBoolean("continuing_from_ad", false)
        if (continuing) {
            saveBoolean("continuing_from_ad", false)
        }
        return continuing
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
        currentEnvironment.value = GameEnvironment.FOREST
        gameSpeed.value = INITIAL_SPEED
        currentCombo.value = 0
        isLevelTransitioning.value = false
        levelTransitionInvincibilityEnd.value = 0L
        _isTemporarilyInvincible.value = false // NEW
        temporaryInvincibilityEnd.value = 0L // NEW
        _recentCollectTimes.clear()
        _gameOverState = false
        continueFromGameOver = false // NEW
    }

}