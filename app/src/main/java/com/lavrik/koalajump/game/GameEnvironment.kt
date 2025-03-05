package com.lavrik.koalajump.game

import androidx.compose.ui.graphics.Color

/**
 * Defines different game environments that function as levels
 */
enum class GameEnvironment(
    val levelName: String,
    val backgroundColors: List<Color>,
    val groundColor: Color,
    val obstacleType: String,
    val collectibleType: String,
    val speedMultiplier: Float,
    val obstacleFrequency: Float,
    val collectibleValue: Int
) {
    FOREST(
        levelName = "Forest",
        backgroundColors = listOf(Color(0xFF87CEEB), Color(0xFF5D8AA8)),
        groundColor = Color(0xFF8B4513),
        obstacleType = "tree",
        collectibleType = "beer",
        speedMultiplier = 1.0f,
        obstacleFrequency = 1.0f,
        collectibleValue = 10
    ),

    DESERT(
        levelName = "Desert",
        backgroundColors = listOf(Color(0xFFF5DEB3), Color(0xFFD2B48C)),
        groundColor = Color(0xFFCD853F),
        obstacleType = "cactus",
        collectibleType = "water",
        speedMultiplier = 1.2f,
        obstacleFrequency = 0.9f,
        collectibleValue = 15
    ),

    MOUNTAINS(
        levelName = "Mountains",
        backgroundColors = listOf(Color(0xFF778899), Color(0xFF708090)),
        groundColor = Color(0xFF696969),
        obstacleType = "rock",
        collectibleType = "coffee",
        speedMultiplier = 1.4f,
        obstacleFrequency = 0.8f,
        collectibleValue = 20
    ),

    BEACH(
        levelName = "Beach",
        backgroundColors = listOf(Color(0xFF87CEEB), Color(0xFF00BFFF)),
        groundColor = Color(0xFFF4A460),
        obstacleType = "shell",
        collectibleType = "coconut",
        speedMultiplier = 1.6f,
        obstacleFrequency = 0.7f,
        collectibleValue = 25
    ),

    JUNGLE(
        levelName = "Jungle",
        backgroundColors = listOf(Color(0xFF006400), Color(0xFF228B22)),
        groundColor = Color(0xFF556B2F),
        obstacleType = "vine",
        collectibleType = "fruit",
        speedMultiplier = 1.8f,
        obstacleFrequency = 1.1f,
        collectibleValue = 30
    );

    companion object {
        private const val POINTS_PER_LEVEL = 300

        /**
         * Get environment based on player score
         */
        fun getEnvironmentForScore(score: Int): GameEnvironment {
            val level = (score / POINTS_PER_LEVEL) % values().size
            return values()[level]
        }

        /**
         * Get level number based on score (1-indexed)
         */
        fun getLevelForScore(score: Int): Int {
            return (score / POINTS_PER_LEVEL) + 1
        }
    }
}