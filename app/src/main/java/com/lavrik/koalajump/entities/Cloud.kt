package com.lavrik.koalajump.entities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.lavrik.koalajump.R
import androidx.compose.ui.geometry.Rect
import kotlin.random.Random

/**
 * Cloud entity that creates a parallax background effect
 */
class Cloud(
    private val cloudImage: Bitmap?,
    var x: Float,
    var y: Float,
    var speed: Float,
    val screenWidth: Float,
    val screenHeight: Float,
    private val scale: Float = 1.0f,
    private val alpha: Int = 200, // Semi-transparent clouds
    val cloudType: Int = 0 // For visual variety
) {
    companion object {
        // Constants for cloud generation and movement
        private const val CLOUD_SPEED_FACTOR = 0.3f  // Clouds move slower than obstacles
        private const val CLOUD_VERTICAL_RANGE = 0.5f // Top 50% of the screen
        private const val MIN_CLOUD_SPACING = 300f
        private const val MAX_CLOUD_SPACING = 800f

        // Cloud sizes - variety for visual interest
        private val CLOUD_SCALES = arrayOf(0.7f, 1.0f, 1.3f)
        private val CLOUD_ALPHAS = arrayOf(160, 200, 240) // Different transparencies

        /**
         * Factory method to create a set of clouds
         */
        fun createClouds(
            context: Context,
            screenWidth: Float,
            screenHeight: Float,
            gameSpeed: Float,
            count: Int = 5
        ): List<Cloud> {
            // Use a single paint object for better performance
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            // Load cloud image (fallback to creating one if needed)
            val cloudBitmap = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.cloud, options)
            } catch (e: Exception) {
                generateCloudBitmap()
            }

            // Generate initial clouds
            return List(count) { index ->
                // Distribute clouds across the screen initially
                val spacing = screenWidth / (count - 1)
                val xPos = index * spacing * Random.nextFloat() * 0.5f + (spacing * index)

                // Randomize height within the top portion of the screen
                val yPos = Random.nextFloat() * (screenHeight * CLOUD_VERTICAL_RANGE)

                // Randomize cloud appearance
                val cloudType = Random.nextInt(0, 3)
                val scale = CLOUD_SCALES[cloudType]
                val alpha = CLOUD_ALPHAS[cloudType]

                // Create cloud with randomized properties
                Cloud(
                    cloudImage = cloudBitmap,
                    x = xPos,
                    y = yPos,
                    speed = gameSpeed * CLOUD_SPEED_FACTOR * (0.7f + (Random.nextFloat() * 0.6f)),
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    scale = scale,
                    alpha = alpha,
                    cloudType = cloudType
                )
            }
        }

        /**
         * Generate a cloud bitmap programmatically
         */
        fun generateCloudBitmap(width: Int = 150, height: Int = 80): Bitmap {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Semi-transparent white for the cloud - fixed to use android.graphics.Color
            val paint = Paint().apply {
                color = android.graphics.Color.WHITE
                alpha = 230
                isAntiAlias = true
                style = Paint.Style.FILL
            }

            // Draw main cloud body
            canvas.drawOval(10f, height/2f, width - 10f, height.toFloat(), paint)

            // Draw cloud puffs
            canvas.drawCircle(width * 0.2f, height * 0.4f, height * 0.4f, paint)
            canvas.drawCircle(width * 0.5f, height * 0.3f, height * 0.5f, paint)
            canvas.drawCircle(width * 0.8f, height * 0.4f, height * 0.4f, paint)

            return bitmap
        }
    }

    // The paint object with the alpha value set
    private val paint = Paint().apply {
        isAntiAlias = true
        isDither = true
        isFilterBitmap = true
        alpha = this@Cloud.alpha
    }

    /**
     * Update cloud position
     */
    fun update() {
        // Move cloud left
        x -= speed

        // Reset if moved off screen (loop clouds)
        if (x + (cloudImage?.width ?: 100) * scale < 0) {
            resetPosition()
        }
    }

    /**
     * Reset cloud to a new position off the right side of screen
     */
    private fun resetPosition() {
        // Place cloud just off the right edge of the screen
        x = screenWidth + Random.nextFloat() * MIN_CLOUD_SPACING

        // Randomize y position within the top half of the screen
        y = Random.nextFloat() * (screenHeight * CLOUD_VERTICAL_RANGE)
    }

    /**
     * Draw the cloud
     */
    fun draw(drawScope: DrawScope) {
        // If we have a cloud bitmap, use it
        if (cloudImage != null) {
            // Calculate dimensions based on scale
            val width = cloudImage.width * scale
            val height = cloudImage.height * scale

            // Draw the cloud with appropriate alpha
            drawScope.drawContext.canvas.nativeCanvas.drawBitmap(
                cloudImage,
                null,
                android.graphics.RectF(x, y, x + width, y + height),
                paint
            )
        } else {
            // Fallback to drawing a simple cloud shape if image is missing
            drawFallbackCloud(drawScope)
        }
    }

    /**
     * Draw a simple cloud shape as fallback if bitmap is not available
     */
    private fun drawFallbackCloud(drawScope: DrawScope) {
        // Simple cloud dimensions
        val width = 150f * scale
        val height = 80f * scale

        // Use a light blue color for the clouds
        val cloudColor = Color(0xD0FFFFFF).copy(alpha = this.alpha / 255f)

        // Draw a rounded rectangle for a simple cloud shape
        drawScope.drawOval(
            color = cloudColor,
            topLeft = Offset(x, y),
            size = Size(width, height)
        )

        // Add some "puffs" to make it more cloud-like
        drawScope.drawOval(
            color = cloudColor,
            topLeft = Offset(x + width * 0.2f, y - height * 0.3f),
            size = Size(width * 0.5f, height * 0.8f)
        )

        drawScope.drawOval(
            color = cloudColor,
            topLeft = Offset(x + width * 0.5f, y - height * 0.2f),
            size = Size(width * 0.4f, height * 0.7f)
        )
    }

    /**
     * Get cloud bounds for debugging
     */
    fun getBounds(): Rect {
        val width = (cloudImage?.width ?: 150) * scale
        val height = (cloudImage?.height ?: 80) * scale

        return Rect(
            left = x,
            top = y,
            right = x + width,
            bottom = y + height
        )
    }

    /**
     * Update cloud speed (for game acceleration)
     */
    fun updateSpeed(newBaseSpeed: Float) {
        speed = newBaseSpeed * CLOUD_SPEED_FACTOR * (0.7f + (cloudType * 0.15f))
    }
}