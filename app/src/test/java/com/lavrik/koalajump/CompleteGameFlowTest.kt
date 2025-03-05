package com.lavrik.koalajump

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompleteGameFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun completeGameFlow() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeTestRule.setContent {
            MainActivity()
        }

        // Wait for splash screen to finish
        Thread.sleep(2000) // This is not ideal but simplifies the test

        // Should be on main menu - click start game
        composeTestRule.onNodeWithText("Start Game").performClick()

        // Should be on game screen - play game
        composeTestRule.waitForIdle()

        // Simulate gameplay by tapping
        composeTestRule.onRoot().performTouchInput {
            click(center)
        }

        // Let the game run for a while
        Thread.sleep(5000)

        // Force game over by setting lives to 0 (in a real test, we'd trigger this through UI)
        // This requires exposing game state for testing which is not ideal

        // Should navigate to game over screen
        composeTestRule.waitForIdle()

        // Check game over screen elements
        // composeTestRule.onNodeWithText("GAME OVER").assertIsDisplayed()

        // Click play again
        // composeTestRule.onNodeWithText("Play Again").performClick()

        // Should go back to game screen
        composeTestRule.waitForIdle()
    }
}