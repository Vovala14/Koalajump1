package com.lavrik.koalajump

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        navController = TestNavHostController(context)

        // Set up the navigation graph
        composeTestRule.setContent {
            GameNavigation(navController = navController, gameState = GameState())
        }
    }

    @Test
    fun verifyStartDestination() {
        // Verify the start destination is the main menu
        val currentDestination = navController.currentBackStackEntry?.destination?.route
        assert(currentDestination == "mainMenu")
    }

    @Test
    fun navigateToGameScreen() {
        // Find and click the Start Game button
        composeTestRule.onNodeWithText("Start Game").performClick()

        // Verify navigation to game screen
        composeTestRule.waitForIdle()
        val currentDestination = navController.currentBackStackEntry?.destination?.route
        assert(currentDestination == "game")
    }

    @Test
    fun navigateToLeaderboard() {
        // Find and click the Leaderboard button
        composeTestRule.onNodeWithText("Leaderboard").performClick()

        // Verify navigation to leaderboard screen
        composeTestRule.waitForIdle()
        val currentDestination = navController.currentBackStackEntry?.destination?.route
        assert(currentDestination == "leaderboard")
    }
}