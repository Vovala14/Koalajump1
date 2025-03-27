package com.lavrik.koalajump.test

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lavrik.koalajump.BitmapManager
import com.lavrik.koalajump.GameState
import com.lavrik.koalajump.R
import com.lavrik.koalajump.entities.AnimatedKoala
import com.lavrik.koalajump.game.GameRenderer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Performance tests to catch regressions in the game's performance-critical code paths
 */
@RunWith(AndroidJUnit4::class)
class KoalaJumpPerformanceTests {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var context: Context
    private lateinit var bitmapManager: BitmapManager
    private lateinit var gameState: GameState
    private lateinit var gameRenderer: GameRenderer
    private lateinit var koala: AnimatedKoala

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        bitmapManager = BitmapManager(context)
        gameState = GameState(context)

        // Initialize with standard test sizes
        val testScreenWidth = 1080f
        val testScreenHeight = 1920f

        gameRenderer = GameRenderer(context, testScreenWidth, testScreenHeight)
        koala = AnimatedKoala(context, testScreenWidth, testScreenHeight, true)
    }

    @Test
    fun benchmark_bitmapLoading() {
        benchmarkRule.measureRepeated {
            // Test bitmap loading performance
            runWithTimingDisabled {
                // Reset between runs
                bitmapManager.clearCache()
                System.gc()
            }

            // Measure time to load a set of bitmaps
            val bitmap = bitmapManager.loadBitmapSync(R.drawable.tree)
            val bitmap2 = bitmapManager.loadBitmapSync(R.drawable.beer)
            val bitmap3 = bitmapManager.loadBitmapSync(R.drawable.koala_animation)
        }
    }

    @Test
    fun benchmark_koalaAnimationUpdate() {
        // Setup koala for testing
        benchmarkRule.measureRepeated {
            // Measure the performance of koala update
            koala.update()
        }
    }

    @Test
    fun benchmark_gameStateUpdateEnvironment() {
        benchmarkRule.measureRepeated {
            // Reset between runs
            runWithTimingDisabled {
                gameState.score.value = 0
            }

            // Test environment update performance as score increases
            for (i in 1..5) {
                gameState.score.value = i * 150 // Trigger different environments
                gameState.updateEnvironment(gameState.score.value)
            }
        }
    }

    @Test
    fun benchmark_bitmapScaling() {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        benchmarkRule.measureRepeated {
            // Get fresh bitmap for each test
            runWithTimingDisabled {
                System.gc()
            }

            // Load bitmap
            val srcBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.tree, options)

            // Scale bitmap - this is performance critical for the game
            val scaledBitmap = Bitmap.createScaledBitmap(srcBitmap,
                (srcBitmap.width * 1.5f).toInt(),
                (srcBitmap.height * 1.5f).toInt(),
                true)

            // Clean up
            runWithTimingDisabled {
                srcBitmap.recycle()
                scaledBitmap.recycle()
            }
        }
    }

    @Test
    fun benchmark_gameStateReset() {
        // Test the performance of resetting game state between games
        benchmarkRule.measureRepeated {
            gameState.resetForNewGame()
        }
    }

    @Test
    fun benchmark_multipleKoalaUpdates() {
        // Test multiple frames of animation update to simulate gameplay
        benchmarkRule.measureRepeated {
            // Run 10 frames to simulate actual gameplay
            repeat(10) {
                koala.update()
            }
        }
    }

    @Test
    fun benchmark_orientationChange() {
        // Set initial orientation
        runWithTimingDisabled {
            koala.updateOrientation(true) // Start in portrait
        }

        // Measure orientation change performance
        benchmarkRule.measureRepeated {
            // Toggle orientation
            koala.updateOrientation(!koala.isPortrait)

            // Update position for new orientation
            koala.updatePositionForNewOrientation()
        }
    }
}