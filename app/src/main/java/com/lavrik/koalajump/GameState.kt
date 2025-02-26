package com.lavrik.koalajump

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class GameState {
    var isPlaying by mutableStateOf(false)
    var score by mutableStateOf(0)
    var lives by mutableStateOf(3)
    var selectedCharacter by mutableStateOf<Int?>(null)
    var isGameOver by mutableStateOf(false)
    var isJumping by mutableStateOf(false)

    // Use proper MutableLists tracked by state
    private val _obstacles = mutableListOf<Obstacle>()
    private val _collectibles = mutableListOf<Collectible>()

    // Wrapper for obstacles to trigger recomposition
    var obstaclesState by mutableStateOf(0)
    var collectiblesState by mutableStateOf(0)

    val obstacles: MutableList<Obstacle> get() {
        return _obstacles
    }

    val collectibles: MutableList<Collectible> get() {
        return _collectibles
    }

    // Override add methods to trigger recomposition
    fun addObstacle(obstacle: Obstacle) {
        _obstacles.add(obstacle)
        obstaclesState++
        Log.d("GAME_DEBUG", "Added obstacle, count: ${_obstacles.size}")
    }

    fun addCollectible(collectible: Collectible) {
        _collectibles.add(collectible)
        collectiblesState++
        Log.d("GAME_DEBUG", "Added collectible, count: ${_collectibles.size}")
    }

    fun clearObstacles() {
        _obstacles.clear()
        obstaclesState++
        Log.d("GAME_DEBUG", "Cleared obstacles")
    }

    fun clearCollectibles() {
        _collectibles.clear()
        collectiblesState++
        Log.d("GAME_DEBUG", "Cleared collectibles")
    }
}

data class Obstacle(
    val x: Float,
    val y: Float,
    val type: String = "tree"
)

data class Collectible(
    val x: Float,
    val y: Float,
    val type: String // "beer" or "crown"
)