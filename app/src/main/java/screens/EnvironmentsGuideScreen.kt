package com.lavrik.koalajump.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.lavrik.koalajump.game.GameEnvironment
import com.lavrik.koalajump.ui.components.AnimatedCloudsBackground

/**
 * A screen that shows all the environments in the game with their obstacles and collectibles
 * Now optimized for landscape mode
 */
@Composable
fun EnvironmentsGuideScreen(
    navController: NavController,
    onClose: () -> Unit
) {
    // Detect current orientation
    val configuration = LocalConfiguration.current
    val isPortrait = remember(configuration) {
        configuration.screenHeightDp > configuration.screenWidthDp
    }

    // Sky gradient with animated clouds background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF87CEEB),  // Sky blue at top
                        Color(0xFF5D8AA8)   // Deeper blue at bottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Add animated clouds to background
        AnimatedCloudsBackground()

        // Main content card
        Card(
            modifier = Modifier
                .fillMaxWidth(if (isPortrait) 0.9f else 0.95f)
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isPortrait) 16.dp else 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title - smaller in landscape
                Text(
                    text = "Game Environments",
                    fontSize = if (isPortrait) 28.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(bottom = if (isPortrait) 16.dp else 8.dp)
                )

                // Subtitle - optional in landscape
                if (isPortrait) {
                    Text(
                        text = "Each environment has unique obstacles and collectibles",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // List of environments
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(GameEnvironment.values()) { environment ->
                        EnvironmentCard(environment, isPortrait)
                    }
                }

                // Close button - adapted for landscape
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .padding(top = if (isPortrait) 16.dp else 8.dp)
                        .height(48.dp)
                        .width(if (isPortrait) 160.dp else 120.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    border = null
                ) {
                    Text(
                        text = "Back",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Card showing information about one environment - adapted for landscape mode
 */
@Composable
private fun EnvironmentCard(environment: GameEnvironment, isPortrait: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isPortrait) 8.dp else 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        if (isPortrait) {
            // Portrait layout - vertical (unchanged)
            PortraitEnvironmentCardContent(environment)
        } else {
            // Landscape layout - more compact
            LandscapeEnvironmentCardContent(environment)
        }
    }
}

@Composable
private fun PortraitEnvironmentCardContent(environment: GameEnvironment) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // Environment name and level
        Text(
            text = environment.levelName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = environment.groundColor.copy(alpha = 1f)
        )

        // Environment background preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(vertical = 8.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = environment.backgroundColors
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            // Ground
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .align(Alignment.BottomCenter)
                    .background(environment.groundColor)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Obstacle and collectible info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Obstacle info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Obstacle:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = environment.obstacleType.capitalize(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Red
                )
            }

            // Collectible info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Collectible:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = environment.collectibleType.capitalize(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4CAF50)
                )
            }

            // Points value
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Points:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${environment.collectibleValue}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9C27B0)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Speed info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Speed: ${environment.speedMultiplier}x",
                fontSize = 14.sp,
                color = Color.Gray
            )

            // Speed indicator
            val progressWidth = (environment.speedMultiplier / 2f) * 100
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(8.dp)
                    .background(Color.LightGray, RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .width(progressWidth.dp)
                        .height(8.dp)
                        .background(Color(0xFF9C27B0), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
private fun LandscapeEnvironmentCardContent(environment: GameEnvironment) {
    Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left column - name and preview
        Column(
            modifier = Modifier
                .weight(0.3f)
                .padding(end = 8.dp)
        ) {
            // Environment name
            Text(
                text = environment.levelName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = environment.groundColor.copy(alpha = 1f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Environment preview - smaller in landscape
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = environment.backgroundColors
                        ),
                        shape = RoundedCornerShape(6.dp)
                    )
            ) {
                // Ground
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(15.dp)
                        .align(Alignment.BottomCenter)
                        .background(environment.groundColor)
                )
            }
        }

        // Center column - obstacles and collectibles
        Column(
            modifier = Modifier
                .weight(0.4f)
                .padding(horizontal = 8.dp)
        ) {
            // More compact layout for landscape
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Obstacle
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Obstacle:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = environment.obstacleType.capitalize(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Red
                    )
                }

                // Collectible
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Collectible:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = environment.collectibleType.capitalize(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Speed info - more compact
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Speed: ${environment.speedMultiplier}x",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(0.4f)
                )

                // Smaller speed indicator
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .height(6.dp)
                        .background(Color.LightGray, RoundedCornerShape(3.dp))
                ) {
                    val progressWidth = environment.speedMultiplier / 2f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressWidth)
                            .height(6.dp)
                            .background(Color(0xFF9C27B0), RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        // Right column - points
        Column(
            modifier = Modifier
                .weight(0.3f)
                .padding(start = 8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "Points:",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = "${environment.collectibleValue}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9C27B0)
            )
        }
    }
}

// Extension function to capitalize the first letter
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}