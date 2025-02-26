package com.lavrik.koalajump

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.lavrik.koalajump.screens.GameOverScreen
import com.lavrik.koalajump.screens.MainGameScreen
import com.lavrik.koalajump.screens.SelectCharacterScreen
import com.lavrik.koalajump.ui.theme.KoalaJumpTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("GAME_DEBUG", "MainActivity onCreate")

        setContent {
            val gameState = remember { GameState() }

            KoalaJumpTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        gameState.selectedCharacter == null -> {
                            Log.d("GAME_DEBUG", "Showing character selection screen")
                            SelectCharacterScreen(
                                selectedCharacter = gameState.selectedCharacter,
                                onCharacterSelected = { character ->
                                    Log.d("GAME_DEBUG", "Character $character selected")
                                    gameState.selectedCharacter = character
                                    gameState.isPlaying = true
                                    resetGame(gameState)
                                }
                            )
                        }
                        gameState.isGameOver -> {
                            Log.d("GAME_DEBUG", "Showing game over screen")
                            GameOverScreen(
                                score = gameState.score,
                                onRestart = {
                                    Log.d("GAME_DEBUG", "Restarting game")
                                    resetGame(gameState)
                                },
                                onMainMenu = {
                                    Log.d("GAME_DEBUG", "Going to main menu")
                                    gameState.selectedCharacter = null
                                    resetGame(gameState)
                                }
                            )
                        }
                        else -> {
                            Log.d("GAME_DEBUG", "Starting main game screen")
                            MainGameScreen(
                                gameState = gameState,
                                onGameOver = { score ->
                                    Log.d("GAME_DEBUG", "Game over with score: $score")
                                    gameState.isGameOver = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun resetGame(gameState: GameState) {
        Log.d("GAME_DEBUG", "Resetting game state")
        gameState.score = 0
        gameState.lives = 3
        gameState.isGameOver = false
        gameState.isJumping = false
        gameState.obstacles.clear()
        gameState.collectibles.clear()
        gameState.isPlaying = true
    }
}