package com.lavrik.koalajump.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.lavrik.koalajump.R
import com.lavrik.koalajump.game.GameEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages and caches environment-specific game assets
 */
class EnvironmentAssetManager(private val context: Context) {
    companion object {
        private const val TAG = "EnvironmentAssetManager"
    }

    // Cache for obstacle bitmaps - avoid reloading the same images
    private val obstacleCache = mutableMapOf<String, Bitmap>()

    // Cache for collectible bitmaps
    private val collectibleCache = mutableMapOf<String, Bitmap>()

    // Booster bitmap is shared across environments
    private var boosterBitmap: Bitmap? = null

    /**
     * Preload all environment assets asynchronously - call this at game startup
     */
    suspend fun preloadAllAssets() {
        withContext(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565 // Using RGB_565 for memory efficiency
                }

                // Load common booster
                boosterBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.booster, options)

                // Load all obstacle types
                loadObstacle("tree", R.drawable.tree, options)
                loadObstacle("cactus", R.drawable.cactus, options)
                loadObstacle("rock", R.drawable.rock, options)
                loadObstacle("shell", R.drawable.shell, options)
                loadObstacle("vine", R.drawable.vine, options)

                // Load all collectible types
                loadCollectible("beer", R.drawable.beer, options)
                loadCollectible("water", R.drawable.water, options)
                loadCollectible("coffee", R.drawable.coffee, options)
                loadCollectible("coconut", R.drawable.coconut, options)
                loadCollectible("fruit", R.drawable.fruit, options)

                Log.d(TAG, "All environment assets preloaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error preloading assets: ${e.message}")
            }
        }
    }

    /**
     * Get obstacle bitmap for a specific environment
     */
    fun getObstacleBitmap(environment: GameEnvironment): Bitmap? {
        val obstacleType = environment.obstacleType
        if (!obstacleCache.containsKey(obstacleType)) {
            // Load on demand if not in cache
            val resId = getObstacleResourceId(obstacleType)
            loadObstacleSynchronously(obstacleType, resId)
        }
        return obstacleCache[obstacleType]
    }

    /**
     * Get collectible bitmap for a specific environment
     */
    fun getCollectibleBitmap(environment: GameEnvironment): Bitmap? {
        val collectibleType = environment.collectibleType
        if (!collectibleCache.containsKey(collectibleType)) {
            // Load on demand if not in cache
            val resId = getCollectibleResourceId(collectibleType)
            loadCollectibleSynchronously(collectibleType, resId)
        }
        return collectibleCache[collectibleType]
    }

    /**
     * Get the booster bitmap
     */
    fun getBoosterBitmap(): Bitmap? {
        if (boosterBitmap == null) {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            boosterBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.booster, options)
        }
        return boosterBitmap
    }

    /**
     * Clean up resources
     */
    fun release() {
        obstacleCache.values.forEach { it.recycle() }
        collectibleCache.values.forEach { it.recycle() }
        boosterBitmap?.recycle()

        obstacleCache.clear()
        collectibleCache.clear()
        boosterBitmap = null

        System.gc()
    }

    /**
     * Get the resource ID for an obstacle type
     */
    private fun getObstacleResourceId(obstacleType: String): Int {
        return when (obstacleType) {
            "tree" -> R.drawable.tree
            "cactus" -> R.drawable.cactus
            "rock" -> R.drawable.rock
            "shell" -> R.drawable.shell
            "vine" -> R.drawable.vine
            else -> R.drawable.tree // Default fallback
        }
    }

    /**
     * Get the resource ID for a collectible type
     */
    private fun getCollectibleResourceId(collectibleType: String): Int {
        return when (collectibleType) {
            "beer" -> R.drawable.beer
            "water" -> R.drawable.water
            "coffee" -> R.drawable.coffee
            "coconut" -> R.drawable.coconut
            "fruit" -> R.drawable.fruit
            else -> R.drawable.beer // Default fallback
        }
    }

    /**
     * Load an obstacle bitmap asynchronously
     */
    private suspend fun loadObstacle(type: String, resId: Int, options: BitmapFactory.Options) {
        withContext(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeResource(context.resources, resId, options)
                obstacleCache[type] = bitmap
                Log.d(TAG, "Loaded obstacle: $type")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading obstacle $type: ${e.message}")
            }
        }
    }

    /**
     * Load a collectible bitmap asynchronously
     */
    private suspend fun loadCollectible(type: String, resId: Int, options: BitmapFactory.Options) {
        withContext(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeResource(context.resources, resId, options)
                collectibleCache[type] = bitmap
                Log.d(TAG, "Loaded collectible: $type")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading collectible $type: ${e.message}")
            }
        }
    }

    /**
     * Synchronous version for immediate needs
     */
    private fun loadObstacleSynchronously(type: String, resId: Int) {
        try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bitmap = BitmapFactory.decodeResource(context.resources, resId, options)
            obstacleCache[type] = bitmap
            Log.d(TAG, "Loaded obstacle synchronously: $type")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading obstacle $type synchronously: ${e.message}")
        }
    }

    /**
     * Synchronous version for immediate needs
     */
    private fun loadCollectibleSynchronously(type: String, resId: Int) {
        try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bitmap = BitmapFactory.decodeResource(context.resources, resId, options)
            collectibleCache[type] = bitmap
            Log.d(TAG, "Loaded collectible synchronously: $type")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading collectible $type synchronously: ${e.message}")
        }
    }
}