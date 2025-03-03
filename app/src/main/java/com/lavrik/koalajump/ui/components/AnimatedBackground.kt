package com.lavrik.koalajump.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

@Composable
fun AnimatedCloudsBackground(
    cloudColor: Color = Color.White.copy(alpha = 0.7f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "clouds")

    // Create multiple cloud animations with different speeds
    val clouds = remember {
        List(6) { index ->
            CloudData(
                startX = Random.nextFloat() * 1200,
                y = 100f + (index * 80),
                speed = 20000 + (Random.nextFloat() * 40000).toLong(),
                scale = 0.7f + (Random.nextFloat() * 0.6f)
            )
        }
    }

    // Animate each cloud
    val cloudPositions = clouds.map { cloudData ->
        infiniteTransition.animateFloat(
            initialValue = -200f,
            targetValue = 1400f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = cloudData.speed.toInt(),
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "cloud${cloudData.y}"
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        clouds.forEachIndexed { index, cloudData ->
            val xPos = cloudPositions[index].value
            drawCloud(
                x = xPos,
                y = cloudData.y,
                scale = cloudData.scale,
                color = cloudColor
            )
        }
    }
}

private data class CloudData(
    val startX: Float,
    val y: Float,
    val speed: Long,
    val scale: Float
)

private fun DrawScope.drawCloud(
    x: Float,
    y: Float,
    scale: Float = 1f,
    color: Color
) {
    val width = 120f * scale
    val height = 60f * scale

    // Main cloud body
    drawOval(
        color = color,
        topLeft = Offset(x, y),
        size = androidx.compose.ui.geometry.Size(width, height)
    )

    // Cloud puffs
    drawCircle(
        color = color,
        radius = 40f * scale,
        center = Offset(x + width * 0.2f, y - 10f * scale)
    )

    drawCircle(
        color = color,
        radius = 50f * scale,
        center = Offset(x + width * 0.5f, y - 20f * scale)
    )

    drawCircle(
        color = color,
        radius = 40f * scale,
        center = Offset(x + width * 0.8f, y - 10f * scale)
    )
}