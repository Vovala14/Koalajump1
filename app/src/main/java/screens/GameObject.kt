package com.lavrik.koalajump.screens

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.random.Random

/**
 * Game object class for obstacles and collectibles
 * Optimized for both portrait and landscape orientations
 */
class GameObject(
    val image: Bitmap,
    var x: Float,
    var y: Float,
    val speed: Float,
    var isActive: Boolean = true,
    private val screenWidth: Float,
    val isObstacle: Boolean = false,
    private val isPortrait: Boolean = true // Orientation parameter
) {
    // Image dimensions
    val width = image.width
    val height = image.height

    /**
     * Update the object position
     */
    fun update() {
        if (!isActive) return

        // Move left
        x -= speed

        // Reset if off screen
        if (x + width < 0) {
            resetPosition()
        }
    }

    /**
     * Reset position when off screen, optimized based on orientation
     */
    fun resetPosition() {
        if (isPortrait) {
            resetPositionPortrait()
        } else {
            resetPositionLandscape()
        }
    }

    /**
     * Reset position for portrait mode
     */
    private fun resetPositionPortrait() {
        // Position spacing optimized for portrait
        val baseSpacing = if (isObstacle) 300f else 200f

        if (isObstacle) {
            // Trees get moderate spacing in portrait
            x = screenWidth + Random.nextFloat() * baseSpacing + 200f
        } else {
            // Better collectible distribution for portrait
            x = screenWidth + Random.nextFloat() * 400f + 300f

            // Randomize vertical position for portrait (less extreme than landscape)
            if (x < screenWidth * 1.5f) {
                // Assume we're respawning after collection
                val groundLevel = y + height + 20f
                y = groundLevel - height - Random.nextFloat() * 200f
            }
        }
    }

    /**
     * Reset position for landscape mode
     */
    private fun resetPositionLandscape() {
        // Position beyond right edge of screen with time-based spacing
        // Trees should be at least 1.8 seconds apart at current speed for landscape
        val timeSpacingInPixels = 1.8f * speed * 60 // 1.8 seconds * speed * 60fps

        if (isObstacle) {
            // Trees get more space in landscape
            x = screenWidth + timeSpacingInPixels + Random.nextFloat() * 500f
        } else {
            // Better collectible distribution for landscape
            x = screenWidth + Random.nextFloat() * 600f + 400f

            // Randomize vertical position more in landscape
            if (x < screenWidth * 1.5f) {
                // Assume we're respawning after collection
                // We can adjust y more dramatically for collectibles
                // This makes gameplay more interesting in landscape
                val groundLevel = y + height + 30f  // Approximate ground level
                y = groundLevel - height - Random.nextFloat() * 300f
            }
        }
    }

    /**
     * Get bounds for collision detection
     */
    fun getBounds(): Rect {
        return Rect(
            offset = Offset(x, y),
            size = Size(width.toFloat(), height.toFloat())
        )
    }

    /**
     * Update the orientation of the GameObject
     */
    fun updateOrientation(portrait: Boolean) {
        // This method could be implemented if needed to handle orientation changes
        // during gameplay, but it's not currently used since objects are recreated
        // when orientation changes
    }
}