package com.lavrik.koalajump

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lavrik.koalajump.screens.EnhancedMainMenuScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainMenuScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var gameState: GameState
    private lateinit var navController: TestNavHostController

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        gameState = GameState()

        navController = TestNavHostController(context)

        // Start at the main menu screen
        composeTestRule.setContent {
            EnhancedMainMenuScreen(
                gameState = gameState,
                onStartGame = { /* Do nothing in test */ },
                onShowLeaderboard = { /* Do nothing in test */ }
            )
        }
    }

    @Test
    fun mainMenu_displaysTitle() {
        // Check the title is displayed
        composeTestRule.onNodeWithText("Koala Jump").assertIsDisplayed()
    }

    @Test
    fun mainMenu_displaysGameButtons() {
        // Check buttons are displayed
        composeTestRule.onNodeWithText("Start Game").assertIsDisplayed()
        composeTestRule.onNodeWithText("Leaderboard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun mainMenu_clickSettingsShowsSettingsPanel() {
        // Click settings button
        composeTestRule.onNodeWithText("Settings").performClick()

        // Check settings panel is displayed
        composeTestRule.onNodeWithText("Sound Effects").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vibration").assertIsDisplayed()
        composeTestRule.onNodeWithText("Allow Rotation").assertIsDisplayed()
    }

    @Test
    fun mainMenu_toggleSetting() {
        // Click settings button
        composeTestRule.onNodeWithText("Settings").performClick()

        // Find and toggle a switch (may need semantics matcher)
        composeTestRule.onAllNodesWithTag("sound_toggle")[0].performClick()

        // Verify state changed (this requires tag setup for switches)
    }
}