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

class AnimatedKoala(
    private val context: Context,
    val screenWidth: Float,
    val screenHeight: Float
) {
    // Sprite sheet management
    private val spriteSheet: Bitmap
    private var currentFrame: Int = 0
    private val totalFrames: Int = 6  // Assuming 6 frames in koala_animation.png
    private val frameHeight: Int
    private val frameWidth: Int

    // Position and movement properties
    var x: Float = 0f
    var y: Float = 0f
    val width: Int
    val height: Int

    // Jump properties
    var isJumping = false
    private var jumpVelocity = -25f  // Negative means upward
    private val gravity = 1.5f
    private val groundY: Float

    // Animation timing
    private var lastFrameChangeTime: Long = 0
    private val frameDuration: Long = 100  // milliseconds per frame

    init {
        // Load the sprite sheet
        spriteSheet = BitmapFactory.decodeResource(context.resources, R.drawable.koala_animation)

        // Calculate the frame dimensions (assuming vertically stacked frames)
        frameHeight = spriteSheet.height / totalFrames
        frameWidth = spriteSheet.width

        // Set up koala dimensions
        width = frameWidth
        height = frameHeight

        // Position the koala at the left side of the screen
        x = screenWidth / 4f - width / 2f
        groundY = screenHeight * 0.8f - height  // 80% down the screen
        y = groundY

        // Initialize animation timing
        lastFrameChangeTime = System.currentTimeMillis()
    }

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
                jumpVelocity = -25f  // Reset jump velocity
            }
        }

        // Update animation frame
        val currentTime = System.currentTimeMillis()
        if (currentTime > lastFrameChangeTime + frameDuration) {
            // Move to next frame
            currentFrame = (currentFrame + 1) % totalFrames
            lastFrameChangeTime = currentTime
        }
    }

    // For Jetpack Compose drawing
    fun draw(drawScope: DrawScope) {
        // Using the nativeCanvas directly
        val canvas = drawScope.drawContext.canvas.nativeCanvas

        // Calculate the source rectangle for the current frame
        val srcRect = Rect(
            0,
            currentFrame * frameHeight,
            frameWidth,
            (currentFrame + 1) * frameHeight
        )

        // Calculate the destination rectangle
        val dstRect = RectF(
            x,
            y,
            x + width,
            y + height
        )

        // Draw the current frame
        canvas.drawBitmap(spriteSheet, srcRect, dstRect, null)
    }

    fun jump() {
        if (!isJumping) {
            isJumping = true
            jumpVelocity = -25f  // Reset jump velocity when starting a new jump
        }
    }

    // For collision detection with Compose Rect
    fun getBounds(): ComposeRect {
        return ComposeRect(
            offset = Offset(x, y),
            size = Size(width.toFloat(), height.toFloat())
        )
    }
}