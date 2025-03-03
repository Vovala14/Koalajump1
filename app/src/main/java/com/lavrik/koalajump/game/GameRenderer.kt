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
    private var powerUpKoalaImage: Bitmap? = null
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

                // Load the power-up koala image
                powerUpKoalaImage = try {
                    BitmapFactory.decodeResource(context.resources, R.raw.koala_powerup, options)
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading power-up koala image: ${e.message}")
                    null
                }

                // Load other assets
                treeImage = BitmapFactory.decodeResource(context.resources, R.drawable.tree, options)
                beerImage = BitmapFactory.decodeResource(context.resources, R.drawable.beer, options)

                // Initialize koala
                koala = AnimatedKoala(
                    context = context,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )

                // Set the power-up image if available
                powerUpKoalaImage?.let {
                    koala?.loadPowerUpSprite(it)
                }

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

                // Force another garbage collection after loading
                System.gc()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading assets", e)
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
        powerUpKoalaImage?.recycle()

        treeImage = null
        beerImage = null
        powerUpKoalaImage = null
        koala = null
        clouds = emptyList()
    }
}