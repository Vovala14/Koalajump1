package com.lavrik.koalajump.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import com.lavrik.koalajump.R
import com.lavrik.koalajump.entities.AnimatedKoala
import com.lavrik.koalajump.entities.Cloud
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameRenderer(
    private val context: Context,
    private val screenWidth: Float,
    private val screenHeight: Float
) {
    companion object {
        private const val TAG = "GameRenderer"
        private const val CLOUD_COUNT = 6
    }

    // Game assets
    private var koala: AnimatedKoala? = null
    private var treeImage: Bitmap? = null
    private var beerImage: Bitmap? = null
    private var boosterImage: Bitmap? = null
    private var clouds: List<Cloud> = emptyList()

    // Shared Paint object for optimized rendering
    private val paint = Paint().apply {
        isFilterBitmap = true
        isDither = true
        isAntiAlias = true
    }

    /**
     * Initialize all game assets asynchronously
     */
    suspend fun initializeAssets() {
        withContext(Dispatchers.IO) {
            try {
                // Start with garbage collection
                System.gc()

                // Use RGB_565 for all bitmaps (half memory usage compared to ARGB_8888)
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                    // Remove deprecated options
                }

                // Load standard assets
                treeImage = BitmapFactory.decodeResource(context.resources, R.drawable.tree, options)
                beerImage = BitmapFactory.decodeResource(context.resources, R.drawable.beer, options)

                // Load booster image
                boosterImage = BitmapFactory.decodeResource(context.resources, R.drawable.booster, options)

                // Initialize koala
                koala = AnimatedKoala(
                    context = context,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )

                // Initialize clouds
                clouds = Cloud.createClouds(
                    context = context,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    gameSpeed = 12f, // Base speed
                    count = CLOUD_COUNT
                )

                // Log asset sizes
                Log.d(TAG, "Tree image: ${treeImage?.width}x${treeImage?.height}, " +
                        "Memory: ${treeImage?.byteCount?.div(1024) ?: 0}KB")
                Log.d(TAG, "Beer image: ${beerImage?.width}x${beerImage?.height}, " +
                        "Memory: ${beerImage?.byteCount?.div(1024) ?: 0}KB")
                Log.d(TAG, "Booster image: ${boosterImage?.width}x${boosterImage?.height}, " +
                        "Memory: ${boosterImage?.byteCount?.div(1024) ?: 0}KB")

                // Force another garbage collection after loading
                System.gc()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading assets", e)
            }
        }
    }

    /**
     * Load environment-specific images
     */
    suspend fun loadEnvironmentImages(environment: GameEnvironment) {
        withContext(Dispatchers.IO) {
            try {
                // Use RGB_565 for all bitmaps to save memory
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }

                // Get the appropriate obstacle image based on environment
                val obstacleResId = when (environment.obstacleType) {
                    "tree" -> R.drawable.tree
                    "cactus" -> R.drawable.cactus
                    "rock" -> R.drawable.rock
                    "shell" -> R.drawable.shell
                    "vine" -> R.drawable.vine
                    else -> R.drawable.tree
                }

                // Get the appropriate collectible image based on environment
                val collectibleResId = when (environment.collectibleType) {
                    "beer" -> R.drawable.beer
                    "water" -> R.drawable.water
                    "coffee" -> R.drawable.coffee
                    "coconut" -> R.drawable.coconut
                    "fruit" -> R.drawable.fruit
                    else -> R.drawable.beer
                }

                // Recycle old bitmaps to free memory
                treeImage?.recycle()
                beerImage?.recycle()

                // Load new images
                treeImage = BitmapFactory.decodeResource(context.resources, obstacleResId, options)
                beerImage = BitmapFactory.decodeResource(context.resources, collectibleResId, options)

                Log.d(TAG, "Environment images loaded: ${environment.levelName}")
                System.gc() // Request garbage collection
            } catch (e: Exception) {
                Log.e(TAG, "Error loading environment images", e)
            }
        }
    }

    /**
     * Update clouds with new speed
     */
    fun updateClouds(baseSpeed: Float) {
        clouds.forEach { cloud ->
            cloud.updateSpeed(baseSpeed)
            cloud.update()
        }
    }

    /**
     * Get the koala character for gameplay physics
     */
    fun getKoala(): AnimatedKoala? = koala

    /**
     * Get the tree image for collision detection
     */
    fun getTreeImage(): Bitmap? = treeImage

    /**
     * Get the beer image for collision detection
     */
    fun getBeerImage(): Bitmap? = beerImage

    /**
     * Get the booster image for game rendering
     */
    fun getBoosterImage(): Bitmap? = boosterImage

    /**
     * Set koala power-up state
     */
    fun setKoalaPowerUpState(powered: Boolean) {
        koala?.setPowerUpState(powered)
    }

    /**
     * Release resources - safe method that doesn't use incubating APIs
     */
    fun releaseResources() {
        treeImage?.recycle()
        beerImage?.recycle()
        boosterImage?.recycle()

        treeImage = null
        beerImage = null
        boosterImage = null
        koala = null
        clouds = emptyList()
    }
}