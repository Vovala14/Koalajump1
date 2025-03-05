package com.lavrik.koalajump.game

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/**
 * Handles all game physics and collision detection
 */
class GamePhysics(
    private val screenWidth: Float,
    private val screenHeight: Float
) {
    companion object {
        private const val HITBOX_REDUCTION = 0.3f // 30% smaller hitbox for character
        private const val OBSTACLE_HITBOX_REDUCTION = 0.2f // 20% smaller hitbox for obstacles
        private const val JUMP_VELOCITY = -20f // Setting that worked well
        private const val GRAVITY = 1.6f // Gravity value
        private const val MIN_OBSTACLE_SPACING = 400f // Minimum spacing between obstacles
    }

    /**
     * Calculate player hitbox for collision detection
     */
    fun getPlayerHitbox(
        koalaX: Float,
        koalaY: Float,
        koalaWidth: Int,
        koalaHeight: Int
    ): Rect {
        val mouseHitboxWidth = koalaWidth * (1 - HITBOX_REDUCTION)
        val mouseHitboxHeight = koalaHeight * (1 - HITBOX_REDUCTION)
        val mouseHitboxOffsetX = koalaX + (koalaWidth * HITBOX_REDUCTION / 2)
        val mouseHitboxOffsetY = koalaY + (koalaHeight * HITBOX_REDUCTION / 2)

        return Rect(
            offset = Offset(mouseHitboxOffsetX, mouseHitboxOffsetY),
            size = Size(mouseHitboxWidth, mouseHitboxHeight)
        )
    }

    /**
     * Calculate obstacle hitbox for collision detection
     */
    fun getObstacleHitbox(
        obstacleX: Float,
        groundY: Float,
        treeWidth: Int,
        treeHeight: Int
    ): Rect {
        val treeHitboxWidth = treeWidth * (1 - OBSTACLE_HITBOX_REDUCTION)
        val treeHitboxHeight = treeHeight * (1 - OBSTACLE_HITBOX_REDUCTION)
        val obstacleHitboxOffsetX = obstacleX + (treeWidth * OBSTACLE_HITBOX_REDUCTION / 2)
        val obstacleHitboxOffsetY = groundY - treeHitboxHeight

        return Rect(
            offset = Offset(obstacleHitboxOffsetX, obstacleHitboxOffsetY),
            size = Size(treeHitboxWidth, treeHitboxHeight)
        )
    }

    /**
     * Check for collision between player and obstacles
     * @return The index of the collided obstacle, or null if no collision
     */
    fun checkObstacleCollision(
        playerHitbox: Rect,
        obstacles: Array<Float>,
        treeImage: Bitmap,
        groundY: Float
    ): Int? {
        for (i in obstacles.indices) {
            val obstacleX = obstacles[i]

            // Skip offscreen obstacles
            if (obstacleX > screenWidth || obstacleX + treeImage.width < 0) continue

            val obstacleHitbox = getObstacleHitbox(
                obstacleX = obstacleX,
                groundY = groundY,
                treeWidth = treeImage.width,
                treeHeight = treeImage.height
            )

            // Check intersection
            if (playerHitbox.overlaps(obstacleHitbox)) {
                return i // Return the index of the collided obstacle
            }
        }

        return null // No collision
    }

    /**
     * Check for collision between player and collectibles
     * @return The index of the collected item, or null if no collision
     */
    fun checkCollectibleCollision(
        playerHitbox: Rect,
        collectibles: Array<Triple<Float, Float, Boolean>>,
        beerImage: Bitmap
    ): Int? {
        for (i in collectibles.indices) {
            val (collectibleX, collectibleY, active) = collectibles[i]

            // Skip inactive or offscreen collectibles
            if (!active || collectibleX > screenWidth || collectibleX + beerImage.width < 0) continue

            // Simple rectangular collision check
            val collectibleRect = Rect(
                offset = Offset(collectibleX, collectibleY),
                size = Size(beerImage.width.toFloat(), beerImage.height.toFloat())
            )

            // Check intersection
            if (playerHitbox.overlaps(collectibleRect)) {
                return i // Return the index of the collected item
            }
        }

        return null // No collision
    }

    /**
     * Update player jumping physics
     * @return Updated Y position and velocity
     */
    fun updateJumpPhysics(
        isJumping: Boolean,
        jumpVelocity: Float,
        mouseY: Float,
        groundY: Float,
        koalaHeight: Int
    ): Triple<Boolean, Float, Float> {
        if (!isJumping) {
            return Triple(false, jumpVelocity, mouseY)
        }

        // Apply velocity and gravity
        var newY = mouseY + jumpVelocity
        var newVelocity = jumpVelocity + GRAVITY
        var stillJumping = true

        // Check if landed
        if (newY > groundY - koalaHeight) {
            newY = groundY - koalaHeight
            stillJumping = false
            newVelocity = JUMP_VELOCITY
        }

        return Triple(stillJumping, newVelocity, newY)
    }

    /**
     * Generate new position for an obstacle that went offscreen
     */
    fun repositionOffscreenObstacle(obstacles: Array<Float>): Float {
        // Find the furthest obstacle
        val furthestObstacle = obstacles.maxOrNull() ?: screenWidth

        // Place beyond the furthest one with proper spacing
        return furthestObstacle + MIN_OBSTACLE_SPACING +
                (Math.random() * MIN_OBSTACLE_SPACING).toFloat()
    }

    /**
     * Generate new position for a collected item
     */
    fun repositionCollectible(collectibles: Array<Triple<Float, Float, Boolean>>, groundY: Float): Triple<Float, Float, Boolean> {
        // Find furthest collectible
        val furthestCollectible = collectibles.maxOfOrNull { it.first } ?: screenWidth

        // Place beyond furthest with proper spacing
        return Triple(
            furthestCollectible + MIN_OBSTACLE_SPACING +
                    (Math.random() * MIN_OBSTACLE_SPACING).toFloat(),
            groundY - 120f - (Math.random() * 160f).toFloat(), // 20% lower
            true
        )
    }

    /**
     * Get the initial jump velocity
     */
    fun getJumpVelocity(): Float = JUMP_VELOCITY
}