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

class Collectable(
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
    var isActive: Boolean = true

    init {
        // Load the beer/collectable image
        bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.beer)

        width = bitmap.width
        height = bitmap.height

        // Start off-screen and randomize position
        resetPosition()
    }

    fun update() {
        if (!isActive) return

        // Move collectable from right to left
        x -= speed

        // If moved off screen, reset
        if (x + width < 0) {
            resetPosition()
        }
    }

    fun draw(drawScope: DrawScope) {
        if (!isActive) return

        // Draw the collectable
        drawScope.drawContext.canvas.nativeCanvas.drawBitmap(bitmap, x, y, null)
    }

    fun resetPosition() {
        // Position horizontally beyond the right edge of the screen
        x = screenWidth + Random.nextInt(200, 1000).toFloat()

        // Position vertically in the upper 2/3 of the screen (for jumping to collect)
        val minY = screenHeight * 0.2f
        val maxY = screenHeight * 0.6f
        y = Random.nextFloat() * (maxY - minY) + minY

        isActive = true
    }

    fun collect() {
        isActive = false
    }

    fun getBounds(): Rect {
        return Rect(
            offset = Offset(x, y),
            size = Size(width.toFloat(), height.toFloat())
        )
    }
}