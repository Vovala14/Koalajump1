package com.lavrik.koalajump.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import com.lavrik.koalajump.R

/**
 * Manages sound effects for the game
 */
class SoundManager(private val context: Context) {
    companion object {
        private const val TAG = "SoundManager"
    }

    // Sound pool for short sound effects
    private val soundPool: SoundPool by lazy {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(attributes)
            .build()
    }

    // MediaPlayer for longer sounds like background music or game over
    private var gameOverPlayer: MediaPlayer? = null

    // Sound IDs
    private var jumpSoundId: Int = -1
    private var collectSoundId: Int = -1
    private var hitSoundId: Int = -1

    // State
    private var soundEnabled = true

    init {
        try {
            // Load sound effects
            jumpSoundId = soundPool.load(context, R.raw.jump, 1)
            collectSoundId = soundPool.load(context, R.raw.collect, 1)
            hitSoundId = soundPool.load(context, R.raw.hit, 1)

            // Pre-load the game over sound (but don't play it yet)
            prepareGameOverSound()

            Log.d(TAG, "Sound effects loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading sound effects: ${e.message}")
        }
    }

    /**
     * Prepare the game over sound to minimize delay when playing
     */
    private fun prepareGameOverSound() {
        try {
            // Release any existing player first
            gameOverPlayer?.release()

            // Create a new player for the game over sound
            gameOverPlayer = MediaPlayer.create(context, R.raw.game_over)

            // Set to play only once
            gameOverPlayer?.isLooping = false

            // Set up completion listener to release resources
            gameOverPlayer?.setOnCompletionListener {
                it.reset()
            }

            Log.d(TAG, "Game over sound prepared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing game over sound: ${e.message}")
        }
    }

    /**
     * Play jump sound effect
     */
    fun playJumpSound() {
        if (soundEnabled) {
            soundPool.play(jumpSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    /**
     * Play collect sound effect
     */
    fun playCollectSound() {
        if (soundEnabled) {
            soundPool.play(collectSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    /**
     * Play hit sound effect
     */
    fun playHitSound() {
        if (soundEnabled) {
            soundPool.play(hitSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    /**
     * Play game over sound effect - plays once when all lives are lost
     */
    fun playGameOverSound() {
        if (soundEnabled && gameOverPlayer != null) {
            try {
                // Make sure we're not already playing
                if (gameOverPlayer?.isPlaying == true) {
                    gameOverPlayer?.stop()
                    gameOverPlayer?.reset()
                    prepareGameOverSound()
                }

                // Play the game over sound
                gameOverPlayer?.start()
                Log.d(TAG, "Game over sound playing")
            } catch (e: Exception) {
                Log.e(TAG, "Error playing game over sound: ${e.message}")

                // Try to recreate the player for next time
                prepareGameOverSound()
            }
        }
    }

    /**
     * Enable or disable sound effects
     */
    fun setSoundEnabled(enabled: Boolean) {
        Log.d(TAG, "Sound ${if (enabled) "enabled" else "disabled"}")
        soundEnabled = enabled

        // If sound is disabled, stop any ongoing game over sound
        if (!enabled && gameOverPlayer?.isPlaying == true) {
            gameOverPlayer?.stop()
        }
    }

    /**
     * Release resources
     */
    fun release() {
        try {
            soundPool.release()
            gameOverPlayer?.release()
            gameOverPlayer = null
            Log.d(TAG, "Sound resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing sound resources: ${e.message}")
        }
    }
}