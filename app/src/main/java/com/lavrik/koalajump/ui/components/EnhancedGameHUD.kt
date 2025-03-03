package com.lavrik.koalajump.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun EnhancedGameHUD(
    score: Int,
    level: Int,
    lives: Int,
    hasSpeedBoost: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Score with animation when it changes
        var prevScore by remember { mutableStateOf(score) }
        val scoreScale by animateFloatAsState(
            targetValue = if (prevScore != score) 1.2f else 1f,
            label = "scoreScale"
        )

        LaunchedEffect(score) {
            if (prevScore != score) {
                delay(300)
                prevScore = score
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score with bouncy animation when it changes
            Box {
                Text(
                    text = "Score: $score",
                    style = TextStyle(
                        color = Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = Color.White,
                            offset = Offset(1f, 1f),
                            blurRadius = 3f
                        )
                    ),
                    modifier = Modifier.graphicsLayer {
                        scaleX = scoreScale
                        scaleY = scoreScale
                    }
                )
            }

            // Level indicator
            Box(
                modifier = Modifier
                    .background(
                        color = Color(0xAA4CAF50),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Level $level",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Lives as hearts
            Row {
                repeat(lives) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Life",
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Speed boost indicator
        if (hasSpeedBoost) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 40.dp)
                    .background(
                        color = Color(0xDDFFD700),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "SPEED BOOST!",
                    style = TextStyle(
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}