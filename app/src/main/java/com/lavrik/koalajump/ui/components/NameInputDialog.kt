package com.lavrik.koalajump.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Dialog for entering or changing player name for leaderboard
 */
@Composable
fun NameInputDialog(
    initialName: String = "",
    isFirstTime: Boolean = true,
    onNameSubmitted: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .width(300.dp)
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
                    text = if (isFirstTime) "Enter Your Name" else "Change Your Name",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = if (isFirstTime)
                        "Please enter your name for the leaderboard:"
                    else
                        "Update your display name for the leaderboard:",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                TextField(
                    value = name,
                    onValueChange = {
                        // Limit name length to 20 characters
                        if (it.length <= 20) name = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        // Material3 compatible color parameters with improved text visibility
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedIndicatorColor = Color(0xFF4CAF50),
                        unfocusedIndicatorColor = Color.Gray,
                        cursorColor = Color(0xFF4CAF50),
                        // Fix the text color to be darker and more visible
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    placeholder = { Text("Your name") }
                )

                // Character limit indicator
                Text(
                    text = "${name.length}/20",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 8.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Only show Cancel if it's not the first time (name change)
                    if (!isFirstTime) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray
                            )
                        ) {
                            Text("Cancel")
                        }
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onNameSubmitted(name.trim())
                            } else if (isFirstTime) {
                                // If it's first time and name is blank, use "Player" as default
                                onNameSubmitted("Player")
                            }
                        },
                        modifier = if (isFirstTime) Modifier.fillMaxWidth() else Modifier,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        enabled = true // Always enabled, will use default name if blank
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}