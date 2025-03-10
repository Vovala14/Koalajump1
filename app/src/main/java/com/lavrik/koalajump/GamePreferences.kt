package com.lavrik.koalajump

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Centralized preferences manager for KoalaJump game
 * Handles consistent persistence of all game settings
 */
class GamePreferences(private val context: Context) {
    companion object {
        private const val TAG = "GamePreferences"
        private const val PREFS_NAME = "game_prefs"

        // Preference keys
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_ALLOW_ROTATION = "allow_rotation"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_TOTAL_PLAYS = "total_plays"
        private const val KEY_IS_SIGNED_IN = "is_signed_in"
        private const val KEY_DISPLAY_NAME = "display_name"
    }

    // Get preferences instance
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Sound settings
    fun getSoundEnabled(): Boolean = prefs.getBoolean(KEY_SOUND_ENABLED, true)

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
        Log.d(TAG, "Sound ${if (enabled) "enabled" else "disabled"}")
    }

    // Vibration settings
    fun getVibrationEnabled(): Boolean = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
        Log.d(TAG, "Vibration ${if (enabled) "enabled" else "disabled"}")
    }

    // Rotation settings
    fun getAllowRotation(): Boolean = prefs.getBoolean(KEY_ALLOW_ROTATION, false)

    fun setAllowRotation(allow: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_ROTATION, allow).apply()
        Log.d(TAG, "Rotation ${if (allow) "allowed" else "disallowed"}")
    }

    // First launch tracking
    fun isFirstLaunch(): Boolean = prefs.getBoolean(KEY_FIRST_LAUNCH, true)

    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
        Log.d(TAG, "First launch completed")
    }

    // High score
    fun getHighScore(): Int = prefs.getInt(KEY_HIGH_SCORE, 0)

    fun setHighScore(score: Int) {
        val currentHighScore = getHighScore()
        if (score > currentHighScore) {
            prefs.edit().putInt(KEY_HIGH_SCORE, score).apply()
            Log.d(TAG, "New high score saved: $score")
        }
    }

    // Total plays counter
    fun incrementPlayCount() {
        val currentPlays = prefs.getInt(KEY_TOTAL_PLAYS, 0)
        prefs.edit().putInt(KEY_TOTAL_PLAYS, currentPlays + 1).apply()
        Log.d(TAG, "Total plays: ${currentPlays + 1}")
    }

    fun getTotalPlays(): Int = prefs.getInt(KEY_TOTAL_PLAYS, 0)

    // Sign-in state
    fun isSignedIn(): Boolean = prefs.getBoolean(KEY_IS_SIGNED_IN, false)

    fun setSignedIn(signedIn: Boolean) {
        prefs.edit().putBoolean(KEY_IS_SIGNED_IN, signedIn).apply()
        Log.d(TAG, "Sign-in state: $signedIn")
    }

    // Display name
    fun getDisplayName(): String = prefs.getString(KEY_DISPLAY_NAME, "Player") ?: "Player"

    fun setDisplayName(name: String) {
        prefs.edit().putString(KEY_DISPLAY_NAME, name).apply()
        Log.d(TAG, "Display name set to: $name")
    }

    // Helper methods for multiple settings
    fun resetAllSettings() {
        // Keep high score but reset all other settings
        val highScore = getHighScore()
        val totalPlays = getTotalPlays()

        prefs.edit().clear().apply()

        // Restore stats
        prefs.edit()
            .putInt(KEY_HIGH_SCORE, highScore)
            .putInt(KEY_TOTAL_PLAYS, totalPlays)
            .putBoolean(KEY_FIRST_LAUNCH, false)
            .apply()

        Log.d(TAG, "All settings reset to defaults (kept high score and play count)")
    }

    // Export settings as a map (useful for debugging)
    fun exportSettings(): Map<String, Any?> {
        return mapOf(
            "soundEnabled" to getSoundEnabled(),
            "vibrationEnabled" to getVibrationEnabled(),
            "allowRotation" to getAllowRotation(),
            "isFirstLaunch" to isFirstLaunch(),
            "highScore" to getHighScore(),
            "totalPlays" to getTotalPlays(),
            "isSignedIn" to isSignedIn(),
            "displayName" to getDisplayName()
        )
    }
}