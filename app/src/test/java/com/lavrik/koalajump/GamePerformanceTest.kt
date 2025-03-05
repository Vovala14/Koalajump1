package com.lavrik.koalajump

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.runtime.mutableStateOf
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
class GamePerformanceTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var gameState: GameState
    private lateinit var navController: TestNavHostController

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        gameState = GameState()

        navController = TestNavHostController(context)
    }

    @Test
    fun measureGameScreenComposition() {
        benchmarkRule.measureRepeated {
            composeTestRule.setContent {
                GameScreen(gameState = gameState, navController = navController)
            }
        }
    }

    @Test
    fun measureGameStateUpdates() {
        val score = mutableStateOf(0)

        composeTestRule.setContent {
            GameScreen(gameState = gameState, navController = navController)
        }

        benchmarkRule.measureRepeated {
            runWithTimingDisabled {
                // Update score outside timing window
                score.value += 10
            }

            // Measure the impact of state changes
            gameState.addPoints(10)
        }
    }
}