package com.lavrik.koalajump.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.lavrik.koalajump.R

/**
 * Simplified sound manager that works reliably with MP3 files
 */
class SoundManager(private val context: Context) {
    companion object {
        private const val TAG = "SoundManager"
    }

    // MediaPlayer instances for each sound
    private var jumpSound: MediaPlayer? = null
    private var collectSound: MediaPlayer? = null
    private var hitSound: MediaPlayer? = null
    private var gameOverSound: MediaPlayer? = null

    // Sound settings
    private var soundEnabled = true

    init {
        preloadSounds()
    }

    /**
     * Preload all game sounds using MediaPlayer
     */
    private fun preloadSounds() {
        try {
            // Create MediaPlayers for each sound
            if (resourceExists(R.raw.jump)) {
                jumpSound = MediaPlayer.create(context, R.raw.jump).apply {
                    setOnCompletionListener { it.seekTo(0) }
                }
                Log.d(TAG, "Jump sound loaded")
            }

            if (resourceExists(R.raw.collect)) {
                collectSound = MediaPlayer.create(context, R.raw.collect).apply {
                    setOnCompletionListener { it.seekTo(0) }
                }
                Log.d(TAG, "Collect sound loaded")
            }

            if (resourceExists(R.raw.hit)) {
                hitSound = MediaPlayer.create(context, R.raw.hit).apply {
                    setOnCompletionListener { it.seekTo(0) }
                }
                Log.d(TAG, "Hit sound loaded")
            }

            if (resourceExists(R.raw.game_over)) {
                gameOverSound = MediaPlayer.create(context, R.raw.game_over).apply {
                    setOnCompletionListener { it.seekTo(0) }
                }
                Log.d(TAG, "Game over sound loaded")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading sounds: ${e.message}")
        }
    }

    /**
     * Check if a resource exists
     */
    private fun resourceExists(resourceId: Int): Boolean {
        return try {
            context.resources.getResourceName(resourceId)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Resource not found: $resourceId")
            false
        }
    }

    /**
     * Play jump sound effect
     */
    fun playJumpSound() {
        playSound(jumpSound)
    }

    /**
     * Play collect sound effect
     */
    fun playCollectSound() {
        playSound(collectSound)
    }

    /**
     * Play hit sound effect
     */
    fun playHitSound() {
        playSound(hitSound)
    }

    /**
     * Play game over sound effect
     */
    fun playGameOverSound() {
        playSound(gameOverSound)
    }

    /**
     * Play a sound using MediaPlayer
     */
    private fun playSound(player: MediaPlayer?) {
        if (!soundEnabled || player == null) return

        try {
            if (player.isPlaying) {
                player.seekTo(0)
            } else {
                player.start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound: ${e.message}")
        }
    }

    /**
     * Enable or disable sounds
     */
    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
        Log.d(TAG, "Sound ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Release all sound resources
     */
    fun release() {
        try {
            jumpSound?.release()
            collectSound?.release()
            hitSound?.release()
            gameOverSound?.release()

            jumpSound = null
            collectSound = null
            hitSound = null
            gameOverSound = null

            Log.d(TAG, "Sound resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing sound resources: ${e.message}")
        }
    }
}