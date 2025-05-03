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
 * Manager for loading and caching environment-specific assets
 */
class EnvironmentAssetManager(private val context: Context) {
    companion object {
        private const val TAG = "EnvironmentAssetManager"
    }

    // Cache for obstacle bitmaps
    private val obstacleBitmaps = mutableMapOf<GameEnvironment, Bitmap?>()

    // Cache for collectible bitmaps
    private val collectibleBitmaps = mutableMapOf<GameEnvironment, Bitmap?>()

    // Bitmap for booster (shared across all environments)
    private var boosterBitmap: Bitmap? = null

    /**
     * Preload all environment assets
     */
    suspend fun preloadAllAssets() {
        withContext(Dispatchers.IO) {
            try {
                // Load booster bitmap
                boosterBitmap = loadBitmap(R.drawable.booster)

                // Load all environment-specific bitmaps
                GameEnvironment.values().forEach { environment ->
                    val obstacleResId = getObstacleResourceId(environment)
                    val collectibleResId = getCollectibleResourceId(environment)

                    obstacleBitmaps[environment] = loadBitmap(obstacleResId)
                    collectibleBitmaps[environment] = loadBitmap(collectibleResId)

                    Log.d(TAG, "Preloaded assets for ${environment.levelName}")
                }

                Log.d(TAG, "All environment assets preloaded")
            } catch (e: Exception) {
                Log.e(TAG, "Error preloading assets: ${e.message}", e)
            }
        }
    }

    /**
     * Get obstacle bitmap for the specified environment
     */
    fun getObstacleBitmap(environment: GameEnvironment): Bitmap? {
        // Return cached bitmap if available
        obstacleBitmaps[environment]?.let { return it }

        // Otherwise load and cache
        val resourceId = getObstacleResourceId(environment)
        val bitmap = loadBitmapSync(resourceId)
        obstacleBitmaps[environment] = bitmap

        return bitmap
    }

    /**
     * Get collectible bitmap for the specified environment
     */
    fun getCollectibleBitmap(environment: GameEnvironment): Bitmap? {
        // Return cached bitmap if available
        collectibleBitmaps[environment]?.let { return it }

        // Otherwise load and cache
        val resourceId = getCollectibleResourceId(environment)
        val bitmap = loadBitmapSync(resourceId)
        collectibleBitmaps[environment] = bitmap

        return bitmap
    }

    /**
     * Get booster bitmap (same for all environments)
     */
    fun getBoosterBitmap(): Bitmap? {
        if (boosterBitmap == null) {
            boosterBitmap = loadBitmapSync(R.drawable.booster)
        }
        return boosterBitmap
    }

    /**
     * Get the resource ID for obstacle based on environment
     */
    private fun getObstacleResourceId(environment: GameEnvironment): Int {
        return when (environment) {
            GameEnvironment.FOREST -> R.drawable.tree
            GameEnvironment.DESERT -> R.drawable.cactus
            GameEnvironment.MOUNTAINS -> R.drawable.rock
            GameEnvironment.BEACH -> R.drawable.shell
            GameEnvironment.JUNGLE -> R.drawable.vine
            GameEnvironment.DINOSAUR_VALLEY -> R.drawable.raptor
            GameEnvironment.HAUNTED_GRAVEYARD -> R.drawable.ghost
            GameEnvironment.VOLCANIC_CAVES -> R.drawable.volcano
        }
    }

    /**
     * Get the resource ID for collectible based on environment
     */
    private fun getCollectibleResourceId(environment: GameEnvironment): Int {
        return when (environment) {
            GameEnvironment.FOREST -> R.drawable.beer
            GameEnvironment.DESERT -> R.drawable.water
            GameEnvironment.MOUNTAINS -> R.drawable.coffee
            GameEnvironment.BEACH -> R.drawable.coconut
            GameEnvironment.JUNGLE -> R.drawable.fruit
            GameEnvironment.DINOSAUR_VALLEY -> R.drawable.egg
            GameEnvironment.HAUNTED_GRAVEYARD -> R.drawable.pumpkin
            GameEnvironment.VOLCANIC_CAVES -> R.drawable.crystal
        }
    }

    /**
     * Load bitmap asynchronously
     */
    private suspend fun loadBitmap(resourceId: Int): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                BitmapFactory.decodeResource(context.resources, resourceId, options)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading bitmap for resource $resourceId: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Load bitmap synchronously
     */
    private fun loadBitmapSync(resourceId: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            BitmapFactory.decodeResource(context.resources, resourceId, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error synchronously loading bitmap for resource $resourceId: ${e.message}", e)
            null
        }
    }

    /**
     * Release all bitmaps to free memory
     */
    fun release() {
        try {
            boosterBitmap?.recycle()
            boosterBitmap = null

            obstacleBitmaps.values.forEach { it?.recycle() }
            obstacleBitmaps.clear()

            collectibleBitmaps.values.forEach { it?.recycle() }
            collectibleBitmaps.clear()

            Log.d(TAG, "Environment assets released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing bitmaps: ${e.message}", e)
        }
    }
}