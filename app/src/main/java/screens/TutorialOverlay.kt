package com.lavrik.koalajump.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * First-time player tutorial overlay that guides new players through the game
 * @param currentStep Current tutorial step to display
 * @param onNextStep Callback when user wants to proceed to next step
 * @param onFinish Callback when tutorial is completed
 */
@Composable
fun TutorialOverlay(
    currentStep: Int,
    onNextStep: () -> Unit,
    onFinish: () -> Unit
) {
    val tutorialTexts = listOf(
        "Welcome to Koala Jump! Tap anywhere to make your koala jump.",
        "Avoid obstacles by jumping over them. Each collision costs one life.",
        "Collect items for points. Special items give temporary speed boosts!",
        "The game gets faster as your score increases. New environments will unlock as you progress!"
    )

    val imageDescriptions = listOf(
        "Tap to jump",
        "Avoid obstacles",
        "Collect items",
        "Level up"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable {
                // Don't capture clicks - let buttons handle them
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(320.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "How to Play",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Step ${currentStep + 1} of ${tutorialTexts.size}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tutorial image placeholder - in a real implementation,
                // you would display actual tutorial images
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = imageDescriptions[currentStep],
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = tutorialTexts[currentStep],
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Skip button (only visible if not on last step)
                    if (currentStep < tutorialTexts.size - 1) {
                        TextButton(
                            onClick = onFinish,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Skip",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                        }

                        Button(
                            onClick = onNextStep,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            border = null // Remove border to fix background color issue
                        ) {
                            Text(
                                text = "Next",
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        // Only show the start button on the last step
                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = onFinish,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            border = null // Remove border to fix background color issue
                        ) {
                            Text(
                                text = "Start Playing",
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}