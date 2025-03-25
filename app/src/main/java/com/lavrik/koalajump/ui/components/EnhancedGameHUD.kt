package com.lavrik.koalajump.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lavrik.koalajump.game.GameEnvironment

/**
 * Enhanced game HUD showing score, lives, level and environment
 */
@Composable
fun EnhancedGameHUD(
    score: Int,
    level: Int,
    lives: Int,
    hasSpeedBoost: Boolean,
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

        // Lives indicator
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
                            .padding(2.dp)
                    ) {
                        Canvas(modifier = Modifier.size(20.dp)) {
                            drawCircle(
                                color = Color.Red,
                                radius = size.width / 2f
                            )
                        }
                    }
                }
            }
        }

        // Boost indicator if active
        if (hasSpeedBoost) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(
                        color = Color(0xDDFFD700),
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
        }
    }
}