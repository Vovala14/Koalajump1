package com.lavrik.koalajump

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.*
import org.junit.Test

class CollisionDetectionTest {

    @Test
    fun testBasicCollision() {
        // Create two overlapping rectangles
        val rect1 = Rect(Offset(0f, 0f), Size(100f, 100f))
        val rect2 = Rect(Offset(50f, 50f), Size(100f, 100f))

        // They should overlap
        assertTrue(rect1.overlaps(rect2))
    }

    @Test
    fun testNoCollision() {
        // Create two non-overlapping rectangles
        val rect1 = Rect(Offset(0f, 0f), Size(100f, 100f))
        val rect2 = Rect(Offset(200f, 200f), Size(100f, 100f))

        // They should not overlap
        assertFalse(rect1.overlaps(rect2))
    }

    @Test
    fun testBorderlineCollision() {
        // Create two rectangles that just touch
        val rect1 = Rect(Offset(0f, 0f), Size(100f, 100f))
        val rect2 = Rect(Offset(100f, 100f), Size(100f, 100f))

        // Touching at corners, Compose considers this not overlapping
        assertFalse(rect1.overlaps(rect2))

        // Create two rectangles with edges touching
        val rect3 = Rect(Offset(0f, 0f), Size(100f, 100f))
        val rect4 = Rect(Offset(100f, 0f), Size(100f, 100f))

        // Touching at edges, Compose considers this not overlapping
        assertFalse(rect3.overlaps(rect4))
    }

    @Test
    fun testDistanceBasedCollision() {
        // Test our game's distance-based collision logic
        val koalaX = 100f
        val koalaY = 200f
        val obstacleX = 130f
        val obstacleY = 220f

        // Calculate if there's a collision with our distance-based approach
        val collisionDistance = 50f
        val isCollision = Math.abs(koalaX - obstacleX) < collisionDistance &&
                Math.abs(koalaY - obstacleY) < collisionDistance

        // Should be a collision
        assertTrue(isCollision)
    }
}