package com.lavrik.koalajump.screens

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lavrik.koalajump.GameState
import com.lavrik.koalajump.ui.MenuState
import com.lavrik.koalajump.ui.components.AnimatedCloudsBackground
import kotlinx.coroutines.delay

@Composable
fun EnhancedMainMenuScreen(
    gameState: GameState,
    onStartGame: () -> Unit,
    onShowLeaderboard: () -> Unit,
    onShowEnvironments: () -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d("MainMenuScreen", "Composing Enhanced MainMenuScreen")

    // State for menu UI
    var menuState by remember { mutableStateOf<MenuState>(MenuState.Idle) }

    // State for settings
    var soundEnabled by remember { mutableStateOf(gameState.soundEnabled.value) }
    var vibrationEnabled by remember { mutableStateOf(gameState.vibrationEnabled.value) }

    // Sky gradient with animated clouds background
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF87CEEB),  // Sky blue at top
                        Color(0xFF5D8AA8)   // Deeper blue at bottom
                    )
                )
            )
    ) {
        // Add animated clouds to background
        AnimatedCloudsBackground()

        when (menuState) {
            MenuState.Idle, MenuState.Starting -> {
                // Main menu content - use BoxWithConstraints to better position content
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val screenHeight = maxHeight

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Portrait layout - move title up, high score more visible
                        Spacer(modifier = Modifier.height(screenHeight * 0.07f)) // Space at top

                        // Animated title
                        AnimatedTitle()

                        // Add high score at the top if it exists
                        if (gameState.highScore.value > 0) {
                            Spacer(modifier = Modifier.height(16.dp))

                            // High score display - positioned higher
                            Card(
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(70.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xBBFFFFFF)
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 4.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Best Score",
                                        fontSize = 14.sp,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${gameState.highScore.value}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        maxLines = 1 // Ensure single line
                                    )
                                }
                            }
                        }

                        // Add flexible spacing to push buttons lower
                        Spacer(modifier = Modifier.height(screenHeight * 0.1f))

                        // Game buttons
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            EnhancedMenuButton(
                                text = "Start Game",
                                onClick = {
                                    Log.d("MainMenuScreen", "Start Game button clicked")
                                    menuState = MenuState.Starting
                                    gameState.resetForNewGame()
                                    onStartGame()
                                },
                                enabled = menuState == MenuState.Idle,
                                modifier = Modifier.width(220.dp)
                            )

                            EnhancedMenuButton(
                                text = "Leaderboard",
                                onClick = {
                                    Log.d("MainMenuScreen", "Leaderboard button clicked")
                                    onShowLeaderboard()
                                },
                                enabled = menuState == MenuState.Idle,
                                modifier = Modifier.width(220.dp)
                            )

                            EnhancedMenuButton(
                                text = "Environments",
                                onClick = {
                                    Log.d("MainMenuScreen", "Environments button clicked")
                                    onShowEnvironments()
                                },
                                enabled = menuState == MenuState.Idle,
                                modifier = Modifier.width(220.dp)
                            )

                            EnhancedMenuButton(
                                text = "Settings",
                                onClick = {
                                    Log.d("MainMenuScreen", "Settings button clicked")
                                    menuState = MenuState.ShowingSettings
                                },
                                enabled = menuState == MenuState.Idle,
                                modifier = Modifier.width(220.dp)
                            )
                        }

                        // Bottom space
                        Spacer(modifier = Modifier.weight(1f, fill = true))

                        // Starting game message
                        if (menuState == MenuState.Starting) {
                            LaunchedEffect(Unit) {
                                delay(2000)
                                menuState = MenuState.Idle
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Starting game...",
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                    }
                }
            }

            MenuState.ShowingSettings -> {
                // Settings panel
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EnhancedSettingsPanel(
                        soundEnabled = soundEnabled,
                        vibrationEnabled = vibrationEnabled,
                        onSoundChanged = {
                            soundEnabled = it
                            gameState.soundEnabled.value = it
                            gameState.saveSoundSetting(it)
                        },
                        onVibrationChanged = {
                            vibrationEnabled = it
                            gameState.vibrationEnabled.value = it
                            gameState.saveVibrationSetting(it)
                        },
                        onClose = {
                            menuState = MenuState.Idle
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedTitle() {
    val infiniteTransition = rememberInfiniteTransition(label = "titleAnimation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleScale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleRotation"
    )

    // Just the title text without any icon
    Text(
        text = "Koala Jump",
        fontSize = 40.sp,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        style = androidx.compose.ui.text.TextStyle(
            shadow = androidx.compose.ui.graphics.Shadow(
                color = Color(0x99000000),
                offset = androidx.compose.ui.geometry.Offset(3f, 3f),
                blurRadius = 5f
            )
        ),
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            rotationZ = rotation
        }
    )
}

@Composable
private fun EnhancedMenuButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Button(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = modifier
            .height(56.dp) // Slightly smaller in height
            .scale(if (isPressed) 0.95f else 1f)
            .graphicsLayer {
                shadowElevation = if (isPressed) 0f else 8f
            },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4CAF50),
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF8BC34A)
        ),
        border = null,
        enabled = enabled
    ) {
        Text(
            text = text,
            fontSize = 18.sp, // Slightly smaller font
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    // Reset button state after animation
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}

@Composable
private fun EnhancedSettingsPanel(
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    onSoundChanged: (Boolean) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(320.dp)
            .heightIn(max = 600.dp)
            .padding(16.dp),
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
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Settings toggles - no rotation option
            EnhancedSettingToggle(
                text = "Sound Effects",
                checked = soundEnabled,
                onToggle = onSoundChanged
            )

            EnhancedSettingToggle(
                text = "Vibration",
                checked = vibrationEnabled,
                onToggle = onVibrationChanged
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Close button
            Button(
                onClick = onClose,
                modifier = Modifier
                    .width(160.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                border = null
            ) {
                Text(
                    text = "Done",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EnhancedSettingToggle(
    text: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // Reduced padding
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            fontSize = 16.sp, // Slightly smaller font
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF4CAF50),
                checkedTrackColor = Color(0xFF8BC34A),
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
            )
        )
    }
}