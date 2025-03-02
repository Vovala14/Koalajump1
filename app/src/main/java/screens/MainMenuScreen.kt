package com.lavrik.koalajump.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainMenuScreen(
    onStartGame: () -> Unit,
    onShowLeaderboard: () -> Unit,
    onToggleOrientation: (Boolean) -> Unit = {}
) {
    Log.d("MainMenuScreen", "Composing MainMenuScreen")

    // Detect current orientation
    val configuration = LocalConfiguration.current
    val isPortrait = remember(configuration) {
        configuration.screenHeightDp > configuration.screenWidthDp
    }

    // State to track button presses
    var startButtonPressed by remember { mutableStateOf(false) }

    // State for orientation toggle
    var allowRotation by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF)), // White background
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(if(isPortrait) 0.dp else 24.dp)
        ) {
            Text(
                text = "Koala Jump",
                fontSize = if (isPortrait) 36.sp else 48.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(if (isPortrait) 40.dp else 30.dp))

            if (isPortrait) {
                // Portrait layout - Vertical buttons
                Button(
                    onClick = {
                        Log.d("MainMenuScreen", "Start Game button clicked")
                        startButtonPressed = true
                        onStartGame()
                    },
                    modifier = Modifier
                        .width(200.dp)
                        .padding(vertical = 8.dp),
                    enabled = !startButtonPressed // Disable button after it's clicked
                ) {
                    Text("Start Game", fontSize = 18.sp)
                }

                Button(
                    onClick = {
                        Log.d("MainMenuScreen", "Leaderboard button clicked")
                        onShowLeaderboard()
                    },
                    modifier = Modifier
                        .width(200.dp)
                        .padding(vertical = 8.dp)
                ) {
                    Text("Leaderboard", fontSize = 18.sp)
                }

                // Add orientation toggle in portrait mode
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Allow Rotation",
                        fontSize = 16.sp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = allowRotation,
                        onCheckedChange = {
                            allowRotation = it
                            onToggleOrientation(it)
                            Log.d("MainMenuScreen", "Orientation toggle: $it")
                        }
                    )
                }
            } else {
                // Landscape layout - Horizontal buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Button(
                        onClick = {
                            Log.d("MainMenuScreen", "Start Game button clicked")
                            startButtonPressed = true
                            onStartGame()
                        },
                        modifier = Modifier
                            .width(180.dp)
                            .height(50.dp),
                        enabled = !startButtonPressed // Disable button after it's clicked
                    ) {
                        Text("Start Game", fontSize = 18.sp)
                    }

                    Button(
                        onClick = {
                            Log.d("MainMenuScreen", "Leaderboard button clicked")
                            onShowLeaderboard()
                        },
                        modifier = Modifier
                            .width(180.dp)
                            .height(50.dp)
                    ) {
                        Text("Leaderboard", fontSize = 18.sp)
                    }
                }

                // Add orientation toggle in landscape mode
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Allow Rotation",
                        fontSize = 16.sp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = allowRotation,
                        onCheckedChange = {
                            allowRotation = it
                            onToggleOrientation(it)
                            Log.d("MainMenuScreen", "Orientation toggle: $it")
                        }
                    )
                }
            }

            // Add a visual feedback text if button was pressed but navigation failed
            if (startButtonPressed) {
                DisposableEffect(Unit) {
                    // After 2 seconds, reset the button state if still on this screen
                    val handler = android.os.Handler(android.os.Looper.getMainLooper())
                    val runnable = Runnable {
                        startButtonPressed = false
                    }
                    handler.postDelayed(runnable, 2000)

                    onDispose {
                        handler.removeCallbacks(runnable)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Starting game...",
                    color = Color.Black,
                    fontSize = 16.sp
                )
            }
        }
    }
}