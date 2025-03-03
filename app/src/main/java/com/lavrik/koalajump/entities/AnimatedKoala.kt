package com.lavrik.koalajump.entities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import com.lavrik.koalajump.R

/**
 * Animated koala character with orientation-aware mechanics
 */
class AnimatedKoala(
    private val context: Context,
    val screenWidth: Float,
    val screenHeight: Float,
    var isPortrait: Boolean = true  // Support for orientation
) {
    companion object {
        private const val TOTAL_FRAMES = 12
        private const val FRAME_DURATION_MS = 60
        private const val HITBOX_REDUCTION_PERCENT = 0.15f // 15% reduction on each side = 30% total

        // Physics constants
        private const val PORTRAIT_JUMP_VELOCITY = -25f
        private const val LANDSCAPE_JUMP_VELOCITY = -20f
        private const val PORTRAIT_GRAVITY = 1.5f
        private const val LANDSCAPE_GRAVITY = 1.2f
        private const val PORTRAIT_GROUND_RATIO = 0.8f
        private const val LANDSCAPE_GROUND_RATIO = 0.75f
    }

    // Sprite sheet management
    private val spriteSheet: Bitmap
    private var currentFrame: Int = 0
    private val frameHeight: Int
    private val frameWidth: Int

    // Power-up state and sprite
    var isPoweredUp = false
    private var powerUpSpriteSheet: Bitmap? = null

    // Position and movement properties
    var x: Float = 0f
    var y: Float = 0f
    val width: Int
    val height: Int

    // Jump properties
    var isJumping = false
    private var jumpVelocity = if (isPortrait) PORTRAIT_JUMP_VELOCITY else LANDSCAPE_JUMP_VELOCITY
    private val gravity = if (isPortrait) PORTRAIT_GRAVITY else LANDSCAPE_GRAVITY
    private var groundY: Float

    // Animation timing
    private var lastFrameChangeTime: Long = 0

    init {
        // Load the sprite sheet
        spriteSheet = BitmapFactory.decodeResource(context.resources, R.drawable.koala_animation)

        // Calculate the frame dimensions
        frameHeight = spriteSheet.height / TOTAL_FRAMES
        frameWidth = spriteSheet.width

        // Set up koala dimensions
        width = frameWidth
        height = frameHeight

        // Position the koala differently based on orientation
        x = if (isPortrait) {
            screenWidth / 4f - width / 2f  // Default position in portrait
        } else {
            screenWidth * 0.15f  // More to the left in landscape
        }

        // Set ground Y position based on orientation
        groundY = if (isPortrait) {
            screenHeight * PORTRAIT_GROUND_RATIO - height
        } else {
            screenHeight * LANDSCAPE_GROUND_RATIO - height
        }

        y = groundY

        // Initialize animation timing
        lastFrameChangeTime = System.currentTimeMillis()
    }

    /**
     * Update koala position and animation state
     */
    fun update() {
        // Handle jumping physics
        if (isJumping) {
            // Apply velocity
            y += jumpVelocity

            // Apply gravity (adjusted for orientation)
            jumpVelocity += gravity

            // Check if landed
            if (y >= groundY) {
                y = groundY
                isJumping = false
                jumpVelocity = if (isPortrait) PORTRAIT_JUMP_VELOCITY else LANDSCAPE_JUMP_VELOCITY
            }
        }

        // Update animation frame
        val currentTime = System.currentTimeMillis()
        if (currentTime > lastFrameChangeTime + FRAME_DURATION_MS) {
            // Move to next frame
            currentFrame = (currentFrame + 1) % TOTAL_FRAMES
            lastFrameChangeTime = currentTime
        }
    }

    /**
     * Draw the koala using Jetpack Compose
     */
    fun draw(drawScope: DrawScope) {
        // Scale factor based on orientation
        val scaleFactor = if (isPortrait) 1f else 0.85f

        // Using the nativeCanvas directly
        val canvas = drawScope.drawContext.canvas.nativeCanvas

        // Choose the appropriate sprite sheet based on power-up state
        val currentSprite = if (isPoweredUp && powerUpSpriteSheet != null)
            powerUpSpriteSheet!!
        else
            spriteSheet

        // Calculate the source rectangle for the current frame (with 1px padding to avoid bleeding)
        val srcRect = Rect(
            0,
            currentFrame * frameHeight + 1,
            frameWidth,
            (currentFrame + 1) * frameHeight - 1
        )

        // Calculate the destination rectangle with orientation scaling
        val dstRect = RectF(
            x,
            y,
            x + width * scaleFactor,
            y + height * scaleFactor - 2  // Adjust height to account for buffer
        )

        // Draw the current frame (using high-quality rendering)
        val paint = android.graphics.Paint().apply {
            isFilterBitmap = true
            isDither = true
            isAntiAlias = true
        }

        canvas.drawBitmap(currentSprite, srcRect, dstRect, paint)
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
        // Create a smaller hitbox (30% smaller overall - 15% on each side)
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
    }

    /**
     * Set power-up state to change koala appearance
     */
    fun setPowerUpState(powered: Boolean) {
        isPoweredUp = powered

        // Reset animation frame when changing states
        currentFrame = 0
        lastFrameChangeTime = System.currentTimeMillis()
    }

    /**
     * Load the power-up sprite sheet
     */
    fun loadPowerUpSprite(powerUpImage: Bitmap) {
        powerUpSpriteSheet = powerUpImage
    }
}