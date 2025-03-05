package com.lavrik.koalajump

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GameStateTest {
    private lateinit var gameState: GameState

    @Before
    fun setup() {
        gameState = GameState()
    }

    @Test
    fun testInitialState() {
        // Test initial values
        assertFalse(gameState.isGameActive.value)
        assertEquals(0, gameState.score.value)
        assertEquals(0, gameState.finalScore.value)
        assertEquals(0, gameState.highScore.value)
        assertEquals(3, gameState.lives.value)
        assertEquals(1, gameState.currentLevel.value)
    }

    @Test
    fun testResetForNewGame() {
        // Set some values
        gameState.score.value = 50
        gameState.lives.value = 1
        gameState.currentLevel.value = 3

        // Reset game
        gameState.resetForNewGame()

        // Verify reset values
        assertTrue(gameState.isGameActive.value)
        assertEquals(0, gameState.score.value)
        assertEquals(3, gameState.lives.value)
        assertEquals(1, gameState.currentLevel.value)
    }

    @Test
    fun testDecreaseLife() {
        gameState.lives.value = 3
        val stillAlive = gameState.decreaseLife()
        assertEquals(2, gameState.lives.value)
        assertTrue(stillAlive)

        gameState.lives.value = 1
        val gameover = gameState.decreaseLife()
        assertEquals(0, gameState.lives.value)
        assertFalse(gameover)
    }

    @Test
    fun testAddPoints() {
        gameState.score.value = 0
        gameState.addPoints(10)
        assertEquals(10, gameState.score.value)

        gameState.addPoints(15)
        assertEquals(25, gameState.score.value)
    }

    @Test
    fun testUpdateHighScore() {
        gameState.highScore.value = 100
        gameState.score.value = 50
        gameState.endGame()

        // Score didn't beat high score
        assertEquals(100, gameState.highScore.value)

        gameState.score.value = 150
        gameState.endGame()

        // Score beat high score
        assertEquals(150, gameState.highScore.value)
    }

    @Test
    fun testIncreaseLevel() {
        gameState.currentLevel.value = 1
        gameState.gameSpeed.value = 12f

        gameState.increaseLevel()

        assertEquals(2, gameState.currentLevel.value)
        assertTrue(gameState.gameSpeed.value > 12f)
    }
}