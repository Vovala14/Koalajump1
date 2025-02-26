package com.lavrik.koalajump.utils

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.lavrik.koalajump.R

/**
 * Utility class for game-related helpers
 */
class GameUtils(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("koala_jump_prefs", Context.MODE_PRIVATE)

    private var jumpSound: MediaPlayer? = null
    private var collectSound: MediaPlayer? = null
    private var hitSound: MediaPlayer? = null
    private var gameOverSound: MediaPlayer? = null

    init {
        // Initialize sound effects - wrap in try-catch to handle missing resources
        try {
            // We'll initialize these if the raw resources exist
            // If you haven't created these audio files yet, the app won't crash
            if (resourceExists(R.raw.jump)) {
                jumpSound = MediaPlayer.create(context, R.raw.jump)
            }
            if (resourceExists(R.raw.collect)) {
                collectSound = MediaPlayer.create(context, R.raw.collect)
            }
            if (resourceExists(R.raw.hit)) {
                hitSound = MediaPlayer.create(context, R.raw.hit)
            }
            if (resourceExists(R.raw.game_over)) {
                gameOverSound = MediaPlayer.create(context, R.raw.game_over)
            }
        } catch (e: Exception) {
            Log.e("GameUtils", "Error initializing sound effects: ${e.message}", e)
        }
    }

    // Helper method to check if a resource exists
    private fun resourceExists(resourceId: Int): Boolean {
        return try {
            context.resources.getResourceName(resourceId)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Sound effects functions
    fun playJumpSound() {
        if (isSoundEnabled() && jumpSound != null) {
            jumpSound?.start()
        }
    }

    fun playCollectSound() {
        if (isSoundEnabled() && collectSound != null) {
            collectSound?.start()
        }
    }

    fun playHitSound() {
        if (isSoundEnabled() && hitSound != null) {
            hitSound?.start()
        }
    }

    fun playGameOverSound() {
        if (isSoundEnabled() && gameOverSound != null) {
            gameOverSound?.start()
        }
    }

    // Vibration effect
    fun vibrate(duration: Long = 50) {
        if (isVibrationEnabled()) {
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        duration,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } catch (e: Exception) {
                Log.e("GameUtils", "Error with vibration", e)
            }
        }
    }

    // Settings management
    fun isSoundEnabled(): Boolean {
        return sharedPreferences.getBoolean("sound_enabled", true)
    }

    fun setSoundEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("sound_enabled", enabled).apply()
    }

    fun isVibrationEnabled(): Boolean {
        return sharedPreferences.getBoolean("vibration_enabled", true)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("vibration_enabled", enabled).apply()
    }

    // High score management (local)
    fun saveLocalHighScore(score: Int) {
        val currentHighScore = getLocalHighScore()
        if (score > currentHighScore) {
            sharedPreferences.edit().putInt("high_score", score).apply()
        }
    }

    fun getLocalHighScore(): Int {
        return sharedPreferences.getInt("high_score", 0)
    }

    // Clean up resources
    fun release() {
        jumpSound?.release()
        collectSound?.release()
        hitSound?.release()
        gameOverSound?.release()

        jumpSound = null
        collectSound = null
        hitSound = null
        gameOverSound = null
    }
}