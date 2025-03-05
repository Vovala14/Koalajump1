package com.lavrik.koalajump.entities

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.random.Random

/**
 * Optimized game object for obstacles and collectibles
 */
class GameObject(
    val image: Bitmap,
    var x: Float,
    var y: Float,
    var speed: Float, // Changed to var for speed updates
    var isActive: Boolean = true,
    private val screenWidth: Float,
    val isObstacle: Boolean = false,
    private val isPortrait: Boolean = true
) {
    companion object {
        // Constants for object positioning - moved to companion for better memory usage
        private const val PORTRAIT_OBSTACLE_MIN_SPACING = 200f
        private const val PORTRAIT_OBSTACLE_RANDOM_SPACING = 300f
        private const val LANDSCAPE_OBSTACLE_TIME_FACTOR = 1.8f
        private const val LANDSCAPE_OBSTACLE_RANDOM_SPACING = 500f

        private const val PORTRAIT_COLLECTIBLE_MIN_SPACING = 300f
        private const val PORTRAIT_COLLECTIBLE_RANDOM_SPACING = 400f
        private const val LANDSCAPE_COLLECTIBLE_MIN_SPACING = 400f
        private const val LANDSCAPE_COLLECTIBLE_RANDOM_SPACING = 600f

        private const val PORTRAIT_VERTICAL_RANDOM_RANGE = 200f
        private const val LANDSCAPE_VERTICAL_RANDOM_RANGE = 300f

        private const val ASSUMED_FPS = 60f
        private const val HITBOX_REDUCTION_PERCENT = 0.15f // 15% reduction on each side = 30% total
    }

    // Image dimensions - calculate once
    val width = image.width
    val height = image.height

    // Optimize memory by creating paint object only once
    private val paint = Paint().apply {
        isFilterBitmap = true
        isDither = true
        isAntiAlias = true
    }

    // Cache hitbox reduction values for better performance
    private val widthReduction = width * HITBOX_REDUCTION_PERCENT
    private val heightReduction = height * HITBOX_REDUCTION_PERCENT

    /**
     * Update object position with optimized logic
     */
    fun update() {
        if (!isActive) return

        // Move left
        x -= speed

        // Reset if moved off screen
        if (x + width < 0) {
            resetPosition()
        }
    }

    /**
     * Update the speed of this object - used when increasing game difficulty
     */
    fun updateSpeed(newSpeed: Float) {
        speed = newSpeed
    }

    /**
     * Draw the object using Jetpack Compose - optimized to use cached paint
     */
    fun draw(drawScope: DrawScope) {
        if (!isActive) return

        // Draw the object with high-quality rendering
        drawScope.drawContext.canvas.nativeCanvas.drawBitmap(
            image,
            x,
            y,
            paint
        )
    }

    /**
     * Reset position based on orientation
     */
    fun resetPosition() {
        if (isPortrait) {
            resetPositionPortrait()
        } else {
            resetPositionLandscape()
        }
    }

    /**
     * Reset position for portrait mode - optimized logic
     */
    private fun resetPositionPortrait() {
        if (isObstacle) {
            // Trees get moderate spacing in portrait
            x = screenWidth + PORTRAIT_OBSTACLE_MIN_SPACING + Random.nextFloat() * PORTRAIT_OBSTACLE_RANDOM_SPACING
        } else {
            // Collectibles have different distribution
            x = screenWidth + PORTRAIT_COLLECTIBLE_MIN_SPACING + Random.nextFloat() * PORTRAIT_COLLECTIBLE_RANDOM_SPACING

            // Randomize vertical position for portrait (less extreme than landscape)
            if (x < screenWidth * 1.5f) {
                // Assume we're respawning after collection
                val groundLevel = y + height + 20f
                y = groundLevel - height - Random.nextFloat() * PORTRAIT_VERTICAL_RANDOM_RANGE
            }
        }
    }

    /**
     * Reset position for landscape mode - optimized logic
     */
    private fun resetPositionLandscape() {
        if (isObstacle) {
            // Trees should be at least 1.8 seconds apart at current speed for landscape
            val timeSpacingInPixels = LANDSCAPE_OBSTACLE_TIME_FACTOR * speed * ASSUMED_FPS

            // Trees get more space in landscape
            x = screenWidth + timeSpacingInPixels + Random.nextFloat() * LANDSCAPE_OBSTACLE_RANDOM_SPACING
        } else {
            // Better collectible distribution for landscape
            x = screenWidth + LANDSCAPE_COLLECTIBLE_MIN_SPACING + Random.nextFloat() * LANDSCAPE_COLLECTIBLE_RANDOM_SPACING

            // Randomize vertical position more in landscape
            if (x < screenWidth * 1.5f) {
                // More dramatic vertical positioning for landscape
                val groundLevel = y + height + 30f
                y = groundLevel - height - Random.nextFloat() * LANDSCAPE_VERTICAL_RANDOM_RANGE
            }
        }
    }

    /**
     * Get standard collision bounds
     */
    fun getBounds(): Rect {
        return Rect(
            offset = Offset(x, y),
            size = Size(width.toFloat(), height.toFloat())
        )
    }

    /**
     * Get reduced hitbox for more precise collision detection - using cached values
     */
    fun getPreciseHitbox(): Rect {
        return Rect(
            offset = Offset(
                x + widthReduction,
                y + heightReduction
            ),
            size = Size(
                width.toFloat() - (widthReduction * 2),
                height.toFloat() - (heightReduction * 2)
            )
        )
    }
}