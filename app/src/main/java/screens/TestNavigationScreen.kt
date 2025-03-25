package com.lavrik.koalajump.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.lavrik.koalajump.GameState

/**
 * A test screen to isolate navigation issues
 * Add this file to your project and then modify MainActivity.kt to use it
 */
@Composable
fun TestNavigationScreen(
    gameState: GameState,
    navController: NavController
) {
    val tag = "TestNavScreen"
    Log.d(tag, "TestNavigationScreen being composed")

    // Super simple column with just buttons
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Navigation Test Screen",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Test navigation to game
        Button(
            onClick = {
                Log.d(tag, "Game button clicked")
                try {
                    navController.navigate("game")
                    Log.d(tag, "Navigation to game successful")
                } catch (e: Exception) {
                    Log.e(tag, "Navigation error: ${e.message}", e)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Go to Game")
        }

        // Test navigation to main menu
        Button(
            onClick = {
                Log.d(tag, "Main Menu button clicked")
                try {
                    navController.navigate("mainMenu")
                    Log.d(tag, "Navigation to main menu successful")
                } catch (e: Exception) {
                    Log.e(tag, "Navigation error: ${e.message}", e)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Go to Main Menu")
        }

        // Test navigation to sign in
        Button(
            onClick = {
                Log.d(tag, "Sign In button clicked")
                try {
                    navController.navigate("signIn")
                    Log.d(tag, "Navigation to sign in successful")
                } catch (e: Exception) {
                    Log.e(tag, "Navigation error: ${e.message}", e)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Go to Sign In")
        }

        // Test navigation to leaderboard
        Button(
            onClick = {
                Log.d(tag, "Leaderboard button clicked")
                try {
                    navController.navigate("leaderboard")
                    Log.d(tag, "Navigation to leaderboard successful")
                } catch (e: Exception) {
                    Log.e(tag, "Navigation error: ${e.message}", e)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go to Leaderboard")
        }
    }
}