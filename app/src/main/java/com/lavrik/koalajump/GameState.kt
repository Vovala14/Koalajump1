package com.lavrik.koalajump

import android.util.Log
import androidx.compose.runtime.mutableStateOf

class GameState {
    // Game status
    val isGameActive = mutableStateOf(false)

    // Scoring
    val finalScore = mutableStateOf(0)

    val highScore = mutableStateOf(0)

    // Game difficulty
    val gameSpeed = mutableStateOf(10f)

    // Player lives
    val lives = mutableStateOf(3)

    // Current game level
    val currentLevel = mutableStateOf(1)

    // Reset game state for a new game
    fun resetForNewGame() {
        Log.d("GameState", "Resetting game state")
        isGameActive.value = true
        finalScore.value = 0
        lives.value = 3
        currentLevel.value = 1
        gameSpeed.value = 10f
    }

    // Update score during game
    fun updateScore(newScore: Int) {
        Log.d("GameState", "Updating score: $newScore")
        finalScore.value = newScore

        // Update high score if needed
        if (newScore > highScore.value) {
            highScore.value = newScore
            Log.d("GameState", "New high score: ${highScore.value}")
        }
    }

    // Decrease lives
    fun decreaseLife() {
        lives.value--
        Log.d("GameState", "Life lost. Remaining lives: ${lives.value}")
    }

    // Increase level
    fun increaseLevel() {
        currentLevel.value++
        // Optionally increase game speed with level
        gameSpeed.value += 0.5f
        Log.d("GameState", "Level up. Current level: ${currentLevel.value}")
    }

    // End the game
    fun endGame() {
        Log.d("GameState", "Game ended. Final score: ${finalScore.value}")
        isGameActive.value = false
        updateHighScore()
    }

    // Check if game is over
    fun isGameOver(): Boolean {
        val gameOver = lives.value <= 0
        if (gameOver) {
            Log.d("GameState", "Game over - no lives remaining")
        }
        return gameOver
    }

    // Update high score
    private fun updateHighScore() {
        if (finalScore.value > highScore.value) {
            highScore.value = finalScore.value
            Log.d("GameState", "High score updated to: ${highScore.value}")
        }
    }

    // Bonus methods for game mechanics
    fun collectPowerup() {
        // Example of a potential game mechanic
        lives.value = minOf(lives.value + 1, 5)
        Log.d("GameState", "Powerup collected. Lives: ${lives.value}")
    }

    // Reset everything to initial state
    fun fullReset() {
        Log.d("GameState", "Performing full game state reset")
        isGameActive.value = false
        finalScore.value = 0
        highScore.value = maxOf(highScore.value, finalScore.value)
        lives.value = 3
        currentLevel.value = 1
        gameSpeed.value = 10f
    }
}