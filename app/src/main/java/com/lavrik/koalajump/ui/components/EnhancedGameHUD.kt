package com.lavrik.koalajump.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lavrik.koalajump.game.GameEnvironment

/**
 * Enhanced game HUD showing score, lives, level and environment
 * Lives displayed as hearts
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

        // Lives indicator with hearts
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lives: ",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Hearts for lives
            Row {
                for (i in 1..lives) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(horizontal = 2.dp)
                    ) {
                        HeartIcon(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Red
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
 * A heart icon drawn with custom path
 */
@Composable
fun HeartIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.Red
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Create a heart shape using Path
        val path = Path().apply {
            // Start at the top center dip of the heart
            moveTo(width / 2, height * 0.2f)

            // Left curve
            cubicTo(
                width * 0.2f, height * 0.0f,  // control point 1
                width * 0.0f, height * 0.4f,  // control point 2
                width * 0.3f, height * 0.7f   // end point
            )

            // Bottom point of the heart
            lineTo(width / 2, height * 0.95f)

            // Right side of the heart
            lineTo(width * 0.7f, height * 0.7f)

            // Right curve
            cubicTo(
                width * 1.0f, height * 0.4f,  // control point 1
                width * 0.8f, height * 0.0f,  // control point 2
                width / 2, height * 0.2f      // end point
            )

            close()
        }

        // Draw the heart
        drawPath(
            path = path,
            color = color,
            style = Fill
        )
    }
}