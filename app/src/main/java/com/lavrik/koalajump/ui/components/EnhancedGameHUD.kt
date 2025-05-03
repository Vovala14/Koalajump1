package com.lavrik.koalajump.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lavrik.koalajump.game.GameEnvironment

/**
 * Enhanced game HUD showing score, lives, level and environment
 * Lives displayed as beautiful hearts
 */
@Composable
fun EnhancedGameHUD(
    score: Int,
    level: Int,
    lives: Int,
    hasSpeedBoost: Boolean,
    isInvincible: Boolean, // Added parameter for invincibility state
    environment: GameEnvironment
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Score at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Score: $score",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${environment.levelName} - Level $level",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }

        // Add some space
        Spacer(modifier = Modifier.height(8.dp))

        // Lives indicator with beautiful hearts
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Lives: ",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            // Add space before hearts
            Spacer(modifier = Modifier.width(2.dp))

            // Hearts for lives
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (i in 1..lives) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .padding(1.dp)
                    ) {
                        HeartIcon(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Boost indicators if active
        if (hasSpeedBoost) {
            Spacer(modifier = Modifier.height(4.dp))

            // Speed boost indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(
                        color = Color(0xDDFFD700), // Gold color
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SPEED BOOST ACTIVE!",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Add a small space between indicators
            Spacer(modifier = Modifier.height(4.dp))

            // 2x points indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(
                        color = Color(0xDDFF4500), // Orange-red color
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "2x POINTS",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Only add invincibility indicator if invincible is true
            if (isInvincible) {
                // Add a small space between indicators
                Spacer(modifier = Modifier.height(4.dp))

                // Invincibility indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(
                            color = Color(0xDD3399FF), // Blue color for shield
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "INVINCIBLE",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * A beautiful heart icon drawn with smooth path and gradient
 */
@Composable
fun HeartIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.Red
) {
    // Create a subtle pulsing effect
    val infiniteTransition = rememberInfiniteTransition(label = "heartBeat")
    val scale = infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartScale"
    ).value

    // Create gradient colors for a more vibrant look
    val heartGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFF5252), // Lighter red at top
            Color(0xFFD50000)  // Deeper red at bottom
        )
    )

    // Create shadow color
    val shadowColor = Color.Black.copy(alpha = 0.2f)

    Canvas(modifier = modifier.scale(scale)) {
        val width = size.width
        val height = size.height

        // Create a heart shape for shadow
        val shadowPath = Path().apply {
            // Move to top center dip of the heart
            moveTo(width / 2, height * 0.3f)

            // Left curve
            cubicTo(
                width * 0.2f, height * 0.1f,  // control point 1
                width * 0.0f, height * 0.45f, // control point 2
                width * 0.3f, height * 0.8f   // end point
            )

            // Bottom point of the heart
            lineTo(width / 2, height * 0.95f)

            // Right side of the heart
            lineTo(width * 0.7f, height * 0.8f)

            // Right curve
            cubicTo(
                width * 1.0f, height * 0.45f, // control point 1
                width * 0.8f, height * 0.1f,  // control point 2
                width / 2, height * 0.3f      // end point
            )

            close()
        }

        // Draw shadow with offset
        // No translate function - just draw at offset coordinates
        drawPath(
            path = shadowPath,
            color = shadowColor,
            style = Fill
        )

        // Create a heart shape using Path
        val heartPath = Path().apply {
            // Move to top center dip of the heart
            moveTo(width / 2, height * 0.3f)

            // Left curve
            cubicTo(
                width * 0.2f, height * 0.1f,  // control point 1
                width * 0.0f, height * 0.45f, // control point 2
                width * 0.3f, height * 0.8f   // end point
            )

            // Bottom point of the heart
            lineTo(width / 2, height * 0.95f)

            // Right side of the heart
            lineTo(width * 0.7f, height * 0.8f)

            // Right curve
            cubicTo(
                width * 1.0f, height * 0.45f, // control point 1
                width * 0.8f, height * 0.1f,  // control point 2
                width / 2, height * 0.3f      // end point
            )

            close()
        }

        // Draw the heart with gradient
        drawPath(
            path = heartPath,
            brush = heartGradient,
            style = Fill
        )

        // Add subtle highlight at top left for 3D effect
        val highlightPath = Path().apply {
            moveTo(width * 0.35f, height * 0.3f)
            cubicTo(
                width * 0.25f, height * 0.2f,
                width * 0.15f, height * 0.3f,
                width * 0.2f, height * 0.4f
            )
            cubicTo(
                width * 0.3f, height * 0.3f,
                width * 0.4f, height * 0.3f,
                width * 0.35f, height * 0.3f
            )
            close()
        }

        drawPath(
            path = highlightPath,
            color = Color.White.copy(alpha = 0.3f),
            style = Fill
        )
    }
}