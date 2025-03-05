package com.lavrik.koalajump.entities

import android.content.Context
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import com.lavrik.koalajump.R
import java.io.InputStream

/**
 * Animated koala character using a standard GIF animation
 */
class AnimatedKoala(
    private val context: Context,
    val screenWidth: Float,
    val screenHeight: Float,
    var isPortrait: Boolean = true
) {
    companion object {
        private const val TAG = "AnimatedKoala"
        private const val HITBOX_REDUCTION_PERCENT = 0.15f

        // Physics constants - adjusted for 10% larger koala
        private const val PORTRAIT_JUMP_VELOCITY = -22.5f // Increased by 10%
        private const val LANDSCAPE_JUMP_VELOCITY = -22f // Increased by 10%
        private const val PORTRAIT_GRAVITY = 1.65f // Increased by 10%
        private const val LANDSCAPE_GRAVITY = 1.32f // Increased by 10%
        private const val PORTRAIT_GROUND_RATIO = 0.8f
        private const val LANDSCAPE_GROUND_RATIO = 0.75f

        // Fixed size dimensions - increased by 10%
        private const val TARGET_WIDTH = 55 // Increased from 50 to 55
        private const val TARGET_HEIGHT = 55 // Increased from 50 to 55
    }

    // GIF animation using Android's Movie class
    private var koalaAnimation: Movie? = null
    private var powerUpAnimation: Movie? = null
    private var animationStartTime: Long = 0

    // Position properties
    var x: Float = 0f
    var y: Float = 0f
    var width: Int = TARGET_WIDTH
    var height: Int = TARGET_HEIGHT

    // Jump properties
    var isJumping = false
    private var jumpVelocity = if (isPortrait) PORTRAIT_JUMP_VELOCITY else LANDSCAPE_JUMP_VELOCITY
    private var gravity = if (isPortrait) PORTRAIT_GRAVITY else LANDSCAPE_GRAVITY
    private var groundY: Float

    // Power-up state
    var isPoweredUp = false

    // Reusable Paint for drawing
    private val paint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = true
    }

    init {
        try {
            // Load the GIF animation using Movie class
            val inputStream: InputStream = context.resources.openRawResource(R.raw.koala_animation)
            koalaAnimation = Movie.decodeStream(inputStream)
            inputStream.close()

            if (koalaAnimation != null) {
                Log.d(TAG, "Successfully loaded koala GIF animation, duration: ${koalaAnimation?.duration()} ms")
            } else {
                Log.e(TAG, "Failed to load koala GIF animation")
            }

            // Initialize the animation start time
            animationStartTime = SystemClock.uptimeMillis()

        } catch (e: Exception) {
            Log.e(TAG, "Error loading koala GIF: ${e.message}")
            koalaAnimation = null
        }

        // Position the koala
        x = if (isPortrait) {
            screenWidth / 4f - width / 2f
        } else {
            screenWidth * 0.15f
        }

        // Set ground Y position
        groundY = if (isPortrait) {
            screenHeight * PORTRAIT_GROUND_RATIO - height
        } else {
            screenHeight * LANDSCAPE_GROUND_RATIO - height
        }
        y = groundY
    }

    /**
     * Update koala position and animation state
     */
    fun update() {
        // Handle jumping physics
        if (isJumping) {
            // Apply velocity
            y += jumpVelocity

            // Apply gravity
            jumpVelocity += gravity

            // Check if landed
            if (y >= groundY) {
                y = groundY
                isJumping = false
                jumpVelocity = if (isPortrait) PORTRAIT_JUMP_VELOCITY else LANDSCAPE_JUMP_VELOCITY
            }
        }

        // The GIF animation updates automatically based on time
    }

    /**
     * Draw the koala using the GIF animation
     */
    fun draw(drawScope: DrawScope) {
        val animation = if (isPoweredUp && powerUpAnimation != null) {
            powerUpAnimation
        } else {
            koalaAnimation
        }

        if (animation != null) {
            // Get current frame time (looping automatically)
            val now = SystemClock.uptimeMillis()
            val relTime = ((now - animationStartTime) % animation.duration()).toInt()

            // Set the current animation frame time
            animation.setTime(relTime)

            // Scale factor based on orientation
            val scaleFactor = if (isPortrait) 1f else 0.85f

            // Get the native canvas from the DrawScope
            val canvas = drawScope.drawContext.canvas.nativeCanvas

            // Save the canvas state
            canvas.save()

            // Calculate scaling needed
            val scaleX = (TARGET_WIDTH * scaleFactor) / animation.width()
            val scaleY = (TARGET_HEIGHT * scaleFactor) / animation.height()

            // Translate and scale
            canvas.translate(x, y)
            canvas.scale(scaleX, scaleY)

            // Draw the GIF frame
            animation.draw(canvas, 0f, 0f, paint)

            // Restore canvas
            canvas.restore()
        } else {
            // Fallback - draw a placeholder rectangle
            val debugColor = if (isPoweredUp)
                androidx.compose.ui.graphics.Color.Yellow
            else
                androidx.compose.ui.graphics.Color.Blue

            drawScope.drawRect(
                color = debugColor,
                topLeft = Offset(x, y),
                size = Size(width.toFloat(), height.toFloat())
            )
        }
    }

    /**
     * Draw the koala with power-up state
     */
    fun draw(drawScope: DrawScope, isPowerUpActive: Boolean) {
        val originalPoweredUp = isPoweredUp
        if (isPowerUpActive) isPoweredUp = true

        draw(drawScope)

        isPoweredUp = originalPoweredUp
    }

    /**
     * Initiate a jump if not already jumping
     */
    fun jump() {
        if (!isJumping) {
            isJumping = true
            jumpVelocity = if (isPortrait) PORTRAIT_JUMP_VELOCITY else LANDSCAPE_JUMP_VELOCITY
        }
    }

    /**
     * Get collision bounds for the koala with hitbox reduction for better gameplay
     */
    fun getBounds(): ComposeRect {
        val widthReduction = width * HITBOX_REDUCTION_PERCENT
        val heightReduction = height * HITBOX_REDUCTION_PERCENT

        return ComposeRect(
            offset = Offset(
                x + widthReduction,
                y + heightReduction
            ),
            size = Size(
                (width - widthReduction * 2).toFloat(),
                (height - heightReduction * 2).toFloat()
            )
        )
    }

    /**
     * Update orientation settings
     */
    fun updateOrientation(portrait: Boolean) {
        isPortrait = portrait

        // Reposition koala based on new orientation
        x = if (isPortrait) {
            screenWidth / 4f - width / 2f
        } else {
            screenWidth * 0.15f
        }

        // Update ground level
        groundY = if (isPortrait) {
            screenHeight * PORTRAIT_GROUND_RATIO - height
        } else {
            screenHeight * LANDSCAPE_GROUND_RATIO - height
        }

        // Reset jump parameters for new orientation
        jumpVelocity = if (isPortrait) PORTRAIT_JUMP_VELOCITY else LANDSCAPE_JUMP_VELOCITY
        gravity = if (isPortrait) PORTRAIT_GRAVITY else LANDSCAPE_GRAVITY
    }

    /**
     * Set power-up state to change koala appearance
     */
    fun setPowerUpState(powered: Boolean) {
        isPoweredUp = powered
    }

    /**
     * Load the power-up GIF animation
     */
    fun loadPowerUpSprite(powerUpResId: Int) {
        try {
            val inputStream: InputStream = context.resources.openRawResource(powerUpResId)
            powerUpAnimation = Movie.decodeStream(inputStream)
            inputStream.close()

            Log.d(TAG, "Successfully loaded power-up GIF animation")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading power-up GIF: ${e.message}")
            powerUpAnimation = null
        }
    }

    /**
     * Release resources
     */
    fun release() {
        // Movie objects don't need explicit cleanup
        koalaAnimation = null
        powerUpAnimation = null
    }
}