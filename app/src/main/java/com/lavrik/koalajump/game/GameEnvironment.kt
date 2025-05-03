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
    val speedMultiplier: Float,  // Updated multipliers for better game progression
    val obstacleFrequency: Float,
    val collectibleValue: Int
) {
    FOREST(
        levelName = "Forest",
        backgroundColors = listOf(Color(0xFF87CEEB), Color(0xFF5D8AA8)),
        groundColor = Color(0xFF8B4513),
        obstacleType = "tree",
        collectibleType = "beer",
        speedMultiplier = 1.0f,  // Baseline speed (unchanged)
        obstacleFrequency = 1.0f,
        collectibleValue = 10
    ),

    DESERT(
        levelName = "Desert",
        backgroundColors = listOf(Color(0xFFF5DEB3), Color(0xFFD2B48C)),
        groundColor = Color(0xFFCD853F),
        obstacleType = "cactus",
        collectibleType = "water",
        speedMultiplier = 1.25f,  // Increased from 1.2f to 1.25f
        obstacleFrequency = 0.9f,
        collectibleValue = 15
    ),

    MOUNTAINS(
        levelName = "Mountains",
        backgroundColors = listOf(Color(0xFF778899), Color(0xFF708090)),
        groundColor = Color(0xFF696969),
        obstacleType = "rock",
        collectibleType = "coffee",
        speedMultiplier = 1.5f,  // Increased from 1.4f to 1.5f
        obstacleFrequency = 0.8f,
        collectibleValue = 20
    ),

    BEACH(
        levelName = "Beach",
        backgroundColors = listOf(Color(0xFF87CEEB), Color(0xFF00BFFF)),
        groundColor = Color(0xFFF4A460),
        obstacleType = "shell",
        collectibleType = "coconut",
        speedMultiplier = 1.75f,  // Increased from 1.6f to 1.75f
        obstacleFrequency = 0.7f,
        collectibleValue = 25
    ),

    JUNGLE(
        levelName = "Jungle",
        backgroundColors = listOf(Color(0xFF006400), Color(0xFF228B22)),
        groundColor = Color(0xFF556B2F),
        obstacleType = "vine",
        collectibleType = "fruit",
        speedMultiplier = 2.0f,  // Increased from 1.8f to 2.0f for a more dramatic final level
        obstacleFrequency = 1.1f,
        collectibleValue = 30
    ),

    // New environments
    DINOSAUR_VALLEY(
        levelName = "Dinosaur Valley",
        backgroundColors = listOf(Color(0xFFE6B800), Color(0xFFD28C00)), // Amber/gold gradient
        groundColor = Color(0xFFB9824F), // Sandy brown
        obstacleType = "raptor",
        collectibleType = "eggs",
        speedMultiplier = 2.25f, // Continuing the progression
        obstacleFrequency = 1.2f,
        collectibleValue = 35
    ),

    HAUNTED_GRAVEYARD(
        levelName = "Haunted Graveyard",
        backgroundColors = listOf(Color(0xFF483D8B), Color(0xFF2E2040)), // Dark purple/indigo
        groundColor = Color(0xFF3C3C3C), // Dark gray ground
        obstacleType = "ghost",
        collectibleType = "pumpkin",
        speedMultiplier = 2.5f,
        obstacleFrequency = 1.3f,
        collectibleValue = 40
    ),

    VOLCANIC_CAVES(
        levelName = "Volcanic Caves",
        backgroundColors = listOf(Color(0xFF8B0000), Color(0xFF3B0000)), // Dark red/black gradient
        groundColor = Color(0xFF454545), // Volcanic rock/ash
        obstacleType = "volcano",
        collectibleType = "crystal",
        speedMultiplier = 2.75f,
        obstacleFrequency = 1.4f,
        collectibleValue = 45
    );

    companion object {
        // Base points for the first level
        private const val BASE_POINTS = 300

        /**
         * Calculate the score threshold for a specific level
         * Level 1 (Forest): 300
         * Level 2 (Desert): 600
         * Level 3 (Mountains): 1,200
         * And so on, doubling each time
         */
        private fun getPointsThresholdForLevel(level: Int): Int {
            // Level is 1-indexed (Forest = level 1)
            return BASE_POINTS * (1 shl (level - 1))
        }

        /**
         * Calculate the total score needed to reach a specific level
         */
        private fun getTotalPointsForLevel(level: Int): Int {
            var total = 0
            for (i in 1 until level) {
                total += getPointsThresholdForLevel(i)
            }
            return total
        }

        /**
         * Get environment based on player score
         */
        fun getEnvironmentForScore(score: Int): GameEnvironment {
            // Calculate which level the player is on
            var level = 1
            var totalPoints = 0

            while (true) {
                val levelThreshold = getPointsThresholdForLevel(level)
                if (totalPoints + levelThreshold > score) {
                    break
                }

                totalPoints += levelThreshold
                level++

                // If we've gone through all environments, cycle back to the first one
                if (level > values().size) {
                    level = 1
                }
            }

            // Convert to 0-indexed for the enum
            val environmentIndex = (level - 1) % values().size
            return values()[environmentIndex]
        }

        /**
         * Get level number based on score (1-indexed)
         */
        fun getLevelForScore(score: Int): Int {
            // Calculate which level the player is on
            var level = 1
            var totalPoints = 0

            while (true) {
                val levelThreshold = getPointsThresholdForLevel(level)
                if (totalPoints + levelThreshold > score) {
                    break
                }

                totalPoints += levelThreshold
                level++
            }

            return level
        }
    }
}