package com.lavrik.koalajump.test

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lavrik.koalajump.GameState
import com.lavrik.koalajump.screens.EnhancedGameHUD
import com.lavrik.koalajump.screens.EnhancedMainMenuScreen
import com.lavrik.koalajump.screens.GameOverScreen
import com.lavrik.koalajump.screens.TutorialOverlay
import com.lavrik.koalajump.game.GameEnvironment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KoalaJumpUiTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testMainMenuButtons() {
        // Create a GameState for testing
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val gameState = GameState(context)

        // Set up main menu
        composeTestRule.setContent {
            EnhancedMainMenuScreen(
                gameState = gameState,
                onStartGame = {},
                onShowLeaderboard = {},
                onShowEnvironments = {},
                onToggleOrientation = {}
            )
        }

        // Verify that main menu buttons exist
        composeTestRule.onNodeWithText("Start Game").assertExists()
        composeTestRule.onNodeWithText("Leaderboard").assertExists()
        composeTestRule.onNodeWithText("Environments").assertExists()
        composeTestRule.onNodeWithText("Settings").assertExists()

        // Verify buttons are clickable
        composeTestRule.onNodeWithText("Start Game").assertHasClickAction()
        composeTestRule.onNodeWithText("Settings").assertHasClickAction()

        // Click on Settings button
        composeTestRule.onNodeWithText("Settings").performClick()

        // Verify that settings panel appears
        composeTestRule.onNodeWithText("Allow Rotation").assertExists()
        composeTestRule.onNodeWithText("Sound Effects").assertExists()
        composeTestRule.onNodeWithText("Vibration").assertExists()
        composeTestRule.onNodeWithText("Done").assertExists()
    }

    @Test
    fun testGameHUD() {
        // Set up game HUD with test values
        composeTestRule.setContent {
            EnhancedGameHUD(
                score = 1234,
                level = 2,
                lives = 3,
                hasSpeedBoost = true,
                environment = GameEnvironment.DESERT
            )
        }

        // Verify score is displayed
        composeTestRule.onNodeWithText("Score: 1234").assertExists()

        // Verify level and environment are displayed
        composeTestRule.onNodeWithText("Desert - Level 2").assertExists()

        // Verify speed boost indicator is displayed
        composeTestRule.onNodeWithText("SPEED BOOST ACTIVE!").assertExists()

        // Change to no speed boost
        composeTestRule.setContent {
            EnhancedGameHUD(
                score = 1234,
                level = 2,
                lives = 3,
                hasSpeedBoost = false,
                environment = GameEnvironment.DESERT
            )
        }

        // Verify speed boost indicator is gone
        composeTestRule.onNodeWithText("SPEED BOOST ACTIVE!").assertDoesNotExist()
    }

    @Test
    fun testGameOverScreen() {
        // Create a GameState for testing
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val gameState = GameState(context)

        // Set some values
        gameState.finalScore.value = 1234
        gameState.highScore.value = 5678

        // Set up game over screen
        composeTestRule.setContent {
            val navController = rememberNavController()
            GameOverScreen(
                gameState = gameState,
                onRestart = {},
                onMainMenu = {},
                navController = navController
            )
        }

        // Verify game over text
        composeTestRule.onNodeWithText("GAME OVER").assertExists()

        // Verify score display
        composeTestRule.onNodeWithText("Final Score").assertExists()
        composeTestRule.onNodeWithText("1234").assertExists()

        // Verify high score display
        composeTestRule.onNodeWithText("High Score").assertExists()
        composeTestRule.onNodeWithText("5678").assertExists()

        // Verify buttons
        composeTestRule.onNodeWithText("Play Again").assertExists()
        composeTestRule.onNodeWithText("Main Menu").assertExists()

        // Verify buttons are clickable
        composeTestRule.onNodeWithText("Play Again").assertHasClickAction()
        composeTestRule.onNodeWithText("Main Menu").assertHasClickAction()
    }

    @Test
    fun testTutorialOverlay() {
        // Test first step of tutorial
        composeTestRule.setContent {
            TutorialOverlay(
                currentStep = 0,
                onNextStep = {},
                onFinish = {}
            )
        }

        // Verify tutorial header
        composeTestRule.onNodeWithText("How to Play").assertExists()
        composeTestRule.onNodeWithText("Step 1 of 4").assertExists()

        // Verify first step content exists
        composeTestRule.onNodeWithText("Welcome to Koala Jump! Tap anywhere to make your koala jump.").assertExists()

        // Verify navigation buttons at first step
        composeTestRule.onNodeWithText("Skip").assertExists()
        composeTestRule.onNodeWithText("Next").assertExists()

        // Test last step of tutorial
        composeTestRule.setContent {
            TutorialOverlay(
                currentStep = 3,
                onNextStep = {},
                onFinish = {}
            )
        }

        // Verify last step content exists
        composeTestRule.onNodeWithText("The game gets faster as your score increases. New environments will unlock as you progress!").assertExists()

        // Verify proper buttons on last step
        composeTestRule.onNodeWithText("Skip").assertDoesNotExist() // Skip shouldn't be on last page
        composeTestRule.onNodeWithText("Next").assertDoesNotExist() // Next shouldn't be on last page
        composeTestRule.onNodeWithText("Start Playing").assertExists() // Should have Start Playing instead
    }

    // If we could instantiate the game screen reliably, we'd add a test like this:
    /*
    @Test
    fun testGameInteractions() {
        // This would require more complex setup with mocked game components
        // to avoid actual rendering and game physics

        // Create a GameState for testing
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val gameState = GameState(context)

        // Set up game screen with mocked components
        composeTestRule.setContent {
            val navController = rememberNavController()
            GameScreen(
                gameState = gameState,
                navController = navController
            )
        }

        // Test pause button
        composeTestRule.onNodeWithContentDescription("Pause").performClick()

        // Verify pause dialog
        composeTestRule.onNodeWithText("Game Paused").assertExists()
        composeTestRule.onNodeWithText("Resume").assertExists()
        composeTestRule.onNodeWithText("Quit Game").assertExists()

        // Resume game
        composeTestRule.onNodeWithText("Resume").performClick()

        // Verify game is resumed (pause dialog gone)
        composeTestRule.onNodeWithText("Game Paused").assertDoesNotExist()

        // Test game area tap for jumping
        composeTestRule.onNodeWithTag("gameArea").performTouchInput {
            click(center)
        }

        // This would require waiting and verifying koala state changed
    }
    */
}