package com.lavrik.koalajump

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Optimized bitmap manager with better memory handling and performance
 */
class BitmapManager(private val context: Context) {
    companion object {
        private const val TAG = "BitmapManager"
        private const val CACHE_SIZE_FRACTION = 4 // 1/4 of available memory - increased for better performance
    }

    // Create a memory cache using 1/4 of available memory (more than before)
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / CACHE_SIZE_FRACTION

    // Cache implementation with better memory handling
    private val bitmapCache = object : LruCache<Int, Bitmap>(cacheSize) {
        override fun sizeOf(key: Int, bitmap: Bitmap): Int {
            // Return size in kilobytes
            return bitmap.byteCount / 1024
        }

        override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Bitmap, newValue: Bitmap?) {
            super.entryRemoved(evicted, key, oldValue, newValue)

            // Make sure to recycle bitmaps when they're removed from cache
            if (evicted && !oldValue.isRecycled) {
                try {
                    oldValue.recycle()
                } catch (e: Exception) {
                    // Ignore recycling errors
                }
            }
        }
    }

    /**
     * Load a bitmap from resources or cache - optimized for speed
     */
    suspend fun loadBitmap(resourceId: Int): Bitmap {
        // Check if already in cache
        synchronized(bitmapCache) {
            bitmapCache.get(resourceId)?.let {
                if (!it.isRecycled) {
                    return it
                }
                // If recycled, remove it and load a new one
                bitmapCache.remove(resourceId)
            }
        }

        // Load bitmap on IO thread if not in cache
        return withContext(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inMutable = false // Better for caching
                    inScaled = false // Prevent automatic scaling for better performance
                    inPurgeable = true // Allow system to reclaim memory if needed (on older devices)
                }

                val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)

                // Cache the bitmap
                synchronized(bitmapCache) {
                    bitmapCache.put(resourceId, bitmap)
                }

                Log.d(TAG, "Loaded bitmap: $resourceId, size: ${bitmap.byteCount / 1024}KB")
                bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Error loading bitmap for $resourceId", e)

                // Create a minimal fallback bitmap rather than crashing
                val fallbackBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                Log.d(TAG, "Using fallback bitmap")
                fallbackBitmap
            }
        }
    }

    /**
     * Load bitmap synchronously - for immediate needs
     * Use sparingly - the async version is preferred
     */
    fun loadBitmapSync(resourceId: Int): Bitmap {
        // Check cache first
        synchronized(bitmapCache) {
            bitmapCache.get(resourceId)?.let {
                if (!it.isRecycled) {
                    return it
                }
                bitmapCache.remove(resourceId)
            }
        }

        try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inScaled = false
            }

            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)

            // Cache the bitmap
            synchronized(bitmapCache) {
                bitmapCache.put(resourceId, bitmap)
            }

            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error in sync bitmap loading: $resourceId", e)

            // Return minimal fallback bitmap
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
    }

    /**
     * Preload a set of bitmaps in the background - helpful for performance
     */
    suspend fun preloadBitmaps(vararg resourceIds: Int) {
        for (resourceId in resourceIds) {
            try {
                loadBitmap(resourceId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to preload bitmap $resourceId", e)
            }
        }
    }

    /**
     * Clear the bitmap cache and recycle bitmaps to free memory
     */
    fun clearCache() {
        synchronized(bitmapCache) {
            bitmapCache.evictAll()
        }

        // Force garbage collection after clearing cache
        System.gc()

        Log.d(TAG, "Bitmap cache cleared")
    }

    /**
     * Remove a specific bitmap from cache
     */
    fun removeBitmap(resourceId: Int) {
        synchronized(bitmapCache) {
            bitmapCache.remove(resourceId)
        }
    }

    /**
     * Get current cache size
     */
    fun getCacheSize(): Int {
        synchronized(bitmapCache) {
            return bitmapCache.size()
        }
    }

    /**
     * Get current cache stats for diagnostics
     */
    fun getCacheStats(): String {
        synchronized(bitmapCache) {
            return "Size: ${bitmapCache.size()}KB / ${cacheSize}KB, " +
                    "Hit rate: ${bitmapCache.hitCount()}/${bitmapCache.missCount() + bitmapCache.hitCount()}"
        }
    }
}