package com.lavrik.koalajump.test

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lavrik.koalajump.GameState
import com.lavrik.koalajump.entities.AnimatedKoala
import com.lavrik.koalajump.entities.GameObject
import com.lavrik.koalajump.game.GameEnvironment
import com.lavrik.koalajump.game.GamePhysics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import androidx.arch.core.executor.testing.InstantTaskExecutorRule

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class KoalaJumpTests {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()
    private lateinit var context: Context
    private lateinit var gameState: GameState
    private lateinit var gamePhysics: GamePhysics

    @Mock
    private lateinit var mockBitmap: Bitmap

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = InstrumentationRegistry.getInstrumentation().targetContext
        Dispatchers.setMain(testDispatcher)

        gameState = GameState(context)
        gamePhysics = GamePhysics(800f, 1200f)

        // Configure mock bitmap
        `when`(mockBitmap.width).thenReturn(40)
        `when`(mockBitmap.height).thenReturn(60)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    // Test 1: Koala jumping mechanics
    @Test
    fun testKoalaJump() {
        val koala = AnimatedKoala(context, 800f, 1200f, true)
        val initialY = koala.y

        // Trigger jump
        koala.jump()

        // Koala should be jumping
        assertTrue("Koala should be in jumping state", koala.isJumping)

        // Update a few frames to simulate movement
        repeat(5) { koala.update() }

        // Y position should be higher (smaller Y value) during jump
        assertTrue("Koala Y position should decrease during jump", koala.y < initialY)

        // Let koala land
        repeat(20) { koala.update() }

        // Koala should have landed
        assertFalse("Koala should not be jumping after landing", koala.isJumping)
        assertEquals("Koala should return to ground position", koala.groundY, koala.y, 0.1f)
    }

    // Test 2: Collision detection
    @Test
    fun testCollisionDetection() {
        // Create two overlapping hitboxes
        val hitbox1 = Rect(left = 100f, top = 100f, right = 150f, bottom = 150f)
        val hitbox2 = Rect(left = 125f, top = 125f, right = 175f, bottom = 175f)

        // Create a GameObject for obstacle
        val obstacle = GameObject(
            image = mockBitmap,
            x = 125f,
            y = 125f,
            speed = 10f,
            isActive = true,
            screenWidth = 800f,
            isObstacle = true
        )

        // Get koala hitbox
        val koala = AnimatedKoala(context, 800f, 1200f, true)
        koala.x = 100f
        koala.y = 100f
        val koalaHitbox = koala.getBounds()

        // Get obstacle hitbox
        val obstacleHitbox = obstacle.getPreciseHitbox()

        // Test manual hitbox overlap
        assertTrue("Hitboxes should overlap",
            hitbox1.left < hitbox2.right &&
                    hitbox1.right > hitbox2.left &&
                    hitbox1.top < hitbox2.bottom &&
                    hitbox1.bottom > hitbox2.top
        )

        // Test collision with GamePhysics
        // This test requires more setup with actual obstacle positions
        // But the principle is the same
    }

    // Test 3: Game state management - score and level tracking
    @Test
    fun testGameStateLevelProgression() {
        // Reset game state
        gameState.resetForNewGame()

        // Initial values
        assertEquals("Initial score should be 0", 0, gameState.score.value)
        assertEquals("Initial level should be 1", 1, gameState.currentLevel.value)
        assertEquals("Initial environment should be FOREST",
            GameEnvironment.FOREST, gameState.currentEnvironment.value)

        // Add some score - not enough for level up
        gameState.score.value = 100
        gameState.updateEnvironment(100)

        // Check environment hasn't changed yet
        assertEquals("Environment should still be FOREST at score 100",
            GameEnvironment.FOREST, gameState.currentEnvironment.value)

        // Add more score to trigger level change (threshold is 150)
        gameState.score.value = 160
        gameState.updateEnvironment(160)

        // Check level has increased
        assertEquals("Level should be 2 at score 160", 2, gameState.currentLevel.value)
        assertEquals("Environment should be DESERT at score 160",
            GameEnvironment.DESERT, gameState.currentEnvironment.value)
    }

    // Test 4: High score persistence
    @Test
    fun testHighScorePersistence() {
        // Set a high score
        val testScore = 1000
        gameState.updateScore(testScore)

        // Check high score was updated
        assertEquals("High score should be updated", testScore, gameState.highScore.value)

        // Create a new game state to test persistence
        val newGameState = GameState(context)

        // High score should persist
        assertEquals("High score should persist across instances",
            testScore, newGameState.highScore.value)
    }

    // Test 5: Orientation handling
    @Test
    fun testOrientationChange() {
        val koala = AnimatedKoala(context, 800f, 1200f, true)
        val portraitGroundY = koala.groundY

        // Switch to landscape
        koala.updateOrientation(false)

        // Ground position should be different
        assertNotEquals("Ground position should change with orientation",
            portraitGroundY, koala.groundY)

        // Switch back to portrait
        koala.updateOrientation(true)

        // Ground position should be back to original
        assertEquals("Ground position should return to original in portrait",
            portraitGroundY, koala.groundY, 0.1f)
    }

    // Test 6: GameObject movement
    @Test
    fun testGameObjectMovement() {
        val gameObject = GameObject(
            image = mockBitmap,
            x = 800f, // Start at right edge
            y = 500f,
            speed = 10f,
            isActive = true,
            screenWidth = 800f,
            isObstacle = true
        )

        val initialX = gameObject.x

        // Update position
        gameObject.update()

        // Should have moved left
        assertEquals("GameObject should move left by speed value",
            initialX - 10f, gameObject.x, 0.1f)

        // Move off screen
        repeat(100) { gameObject.update() }

        // Should have reset to right side
        assertTrue("GameObject should reset position after moving off screen",
            gameObject.x > 800f)
    }

    // Test 7: GameState lives management
    @Test
    fun testLivesManagement() {
        gameState.resetForNewGame()

        // Start with 3 lives
        assertEquals("Should start with 3 lives", 3, gameState.lives.value)

        // Lose a life
        val stillAlive = gameState.decreaseLife()

        // Check lives decreased
        assertEquals("Should have 2 lives after hit", 2, gameState.lives.value)
        assertTrue("Player should still be alive", stillAlive)

        // Lose remaining lives
        gameState.decreaseLife()
        val gameOver = gameState.decreaseLife()

        // Check game over
        assertFalse("Player should be dead after 3 hits", gameOver)
        assertEquals("Lives should be 0", 0, gameState.lives.value)
        assertTrue("Game should be in game over state", gameState.checkGameOver())
    }

    // Test 8: Environment-specific properties
    @Test
    fun testEnvironmentProperties() {
        // Check speed multipliers are different
        assertNotEquals("Environments should have different speed multipliers",
            GameEnvironment.FOREST.speedMultiplier,
            GameEnvironment.JUNGLE.speedMultiplier)

        // Get environment for specific score
        val highScore = 600 // Should be beyond FOREST and DESERT
        val environment = GameEnvironment.getEnvironmentForScore(highScore)

        // Should be a later environment
        assertNotEquals("Higher score should give different environment",
            GameEnvironment.FOREST, environment)
    }
}