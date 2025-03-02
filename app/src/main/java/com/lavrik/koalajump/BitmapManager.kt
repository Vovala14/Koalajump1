package com.lavrik.koalajump

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages game bitmap assets for better performance
 */
class BitmapManager(private val context: Context) {
    private val TAG = "BitmapManager"

    // Create a memory cache using 1/8 of available memory
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8

    private val bitmapCache = object : LruCache<Int, Bitmap>(cacheSize) {
        override fun sizeOf(key: Int, bitmap: Bitmap): Int {
            // Return size in kilobytes
            return bitmap.byteCount / 1024
        }
    }

    suspend fun loadBitmap(resourceId: Int): Bitmap {
        // Check if already in cache
        synchronized(bitmapCache) {
            bitmapCache.get(resourceId)?.let {
                return it
            }
        }

        // Load bitmap on IO thread
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)

                // Cache the bitmap
                synchronized(bitmapCache) {
                    bitmapCache.put(resourceId, bitmap)
                }

                Log.d(TAG, "Loaded bitmap: $resourceId, size: ${bitmap.byteCount / 1024}KB")
                bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Error loading bitmap for $resourceId", e)
                throw e
            }
        }
    }

    fun clearCache() {
        synchronized(bitmapCache) {
            bitmapCache.evictAll()
        }
        Log.d(TAG, "Bitmap cache cleared")
    }
}