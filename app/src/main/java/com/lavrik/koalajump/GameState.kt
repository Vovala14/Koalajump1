package com.lavrik.koalajump

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.lavrik.koalajump.game.GameEnvironment

/**
 * Central game state manager with improved state handling and persistence
 */
class GameState(private val context: Context) {
    companion object {
        private const val TAG = "GameState"
        private const val INITIAL_LIVES = 3
        private const val INITIAL_LEVEL = 1
        private const val INITIAL_SPEED = 12f  // Increased from 10f for faster gameplay
        private const val SPEED_INCREASE_PER_LEVEL = 0.8f  // Increased from 0.5f for more challenge
        private const val MAX_LIVES = 5
        private const val MAX_GAME_SPEED = 25f  // New constant to cap max speed

        // Preference keys
        private const val PREFS_NAME = "game_prefs"
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_ALLOW_ROTATION = "allow_rotation"
    }

    // SharedPreferences for persistent storage
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Game status
    val isGameActive = mutableStateOf(false)

    // Orientation preference - using a private MutableLiveData with public accessor methods
    private val allowRotation = MutableLiveData(loadAllowRotation())

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

    // Current environment
    val currentEnvironment = mutableStateOf(GameEnvironment.FOREST)

    // Recent collectibles for combo tracking
    private val _recentCollectTimes = mutableListOf<Long>()

    // Current combo counter
    val currentCombo = mutableStateOf(0)

    // Game settings - Load from SharedPreferences
    val soundEnabled = mutableStateOf(loadSoundSetting())
    val vibrationEnabled = mutableStateOf(loadVibrationSetting())

    // Private methods to load settings from SharedPreferences
    private fun loadHighScore(): Int = prefs.getInt(KEY_HIGH_SCORE, 0)

    private fun loadSoundSetting(): Boolean = prefs.getBoolean(KEY_SOUND_ENABLED, true)

    private fun loadVibrationSetting(): Boolean = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)

    private fun loadAllowRotation(): Boolean = prefs.getBoolean(KEY_ALLOW_ROTATION, false)

    /**
     * Get the allow rotation value
     */
    fun getAllowRotation(): Boolean {
        return allowRotation.value ?: false
    }

    /**
     * Set allow rotation value and save to preferences
     */
    fun setAllowRotation(allow: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_ROTATION, allow).apply()
        allowRotation.value = allow
        Log.d(TAG, "Allow rotation set to: $allow and saved to preferences")
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
        currentEnvironment.value = GameEnvironment.FOREST
        gameSpeed.value = INITIAL_SPEED
        currentCombo.value = 0
        _recentCollectTimes.clear()
    }

    /**
     * Update environment based on score
     */
    fun updateEnvironment(score: Int) {
        val newEnvironment = GameEnvironment.getEnvironmentForScore(score)
        currentLevel.value = GameEnvironment.getLevelForScore(score)

        // Only update if environment changed
        if (newEnvironment != currentEnvironment.value) {
            currentEnvironment.value = newEnvironment
        }
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

            // Save high score to preferences
            prefs.edit().putInt(KEY_HIGH_SCORE, newScore).apply()

            Log.d(TAG, "New high score: ${highScore.value} saved to preferences")
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

            // Save high score to preferences
            prefs.edit().putInt(KEY_HIGH_SCORE, finalScore.value).apply()

            Log.d(TAG, "High score updated to: ${highScore.value} and saved to preferences")
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
        currentEnvironment.value = GameEnvironment.FOREST
        gameSpeed.value = INITIAL_SPEED
        currentCombo.value = 0
        _recentCollectTimes.clear()
    }
}