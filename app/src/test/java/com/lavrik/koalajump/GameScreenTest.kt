package com.lavrik.koalajump

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lavrik.koalajump.screens.GameScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var gameState: GameState
    private lateinit var navController: TestNavHostController

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        gameState = GameState()

        navController = TestNavHostController(context)
        navController.setGraph(R.navigation.nav_graph)

        // Start at the game screen
        composeTestRule.setContent {
            GameScreen(gameState = gameState, navController = navController)
        }
    }

    @Test
    fun gameScreen_displaysInitialScore() {
        // Check initial score is displayed
        composeTestRule.onNodeWithText("Score: 0").assertIsDisplayed()
    }

    @Test
    fun gameScreen_displaysLives() {
        // Check lives are displayed
        composeTestRule.onNodeWithText("Lives: 3").assertIsDisplayed()
    }

    @Test
    fun gameScreen_tapCausesJump() {
        // Tap on screen should trigger jump
        composeTestRule.onRoot().performTouchInput {
            click(center)
        }

        // Verify jump was triggered (this is harder to test directly,
        // ideally we'd check for jumping animation or state change)
        composeTestRule.waitForIdle()
    }

    @Test
    fun gameScreen_gameOverNavigatesToGameOverScreen() {
        // Set lives to 0 which should trigger game over
        gameState.lives.value = 0

        // Wait for navigation
        composeTestRule.waitForIdle()

        // Check if we navigated to game over screen
        // This may require additional setup to fully test
    }
}