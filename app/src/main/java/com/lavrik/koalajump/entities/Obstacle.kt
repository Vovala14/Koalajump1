package com.lavrik.koalajump.entities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import com.lavrik.koalajump.R
import kotlin.random.Random

class Obstacle(
    private val context: Context,
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val speed: Float = 10f
) {
    // Image properties
    private val bitmap: Bitmap
    var x: Float = 0f
    var y: Float = 0f
    val width: Int
    val height: Int

    init {
        // Load the tree/obstacle image
        bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.tree)

        width = bitmap.width
        height = bitmap.height

        // Position at the ground level
        y = screenHeight * 0.8f - height

        // Start off-screen
        resetPosition()
    }

    fun update() {
        // Move obstacle from right to left
        x -= speed

        // If moved off screen, reset
        if (x + width < 0) {
            resetPosition()
        }
    }

    fun draw(drawScope: DrawScope) {
        // Draw the obstacle
        drawScope.drawContext.canvas.nativeCanvas.drawBitmap(bitmap, x, y, null)
    }

    fun resetPosition() {
        // Position beyond the right edge of the screen with some random distance
        x = screenWidth + Random.nextInt(200, 800).toFloat()
    }

    fun getBounds(): Rect {
        return Rect(
            offset = Offset(x, y),
            size = Size(width.toFloat(), height.toFloat())
        )
    }
}