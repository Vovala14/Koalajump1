package com.lavrik.koalajump

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.atomic.AtomicInteger

/**
 * Utility class for monitoring and reporting performance metrics
 * such as FPS (frames per second) and memory usage
 */
class PerformanceMonitor {
    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val UPDATE_INTERVAL_MS = 1000L
    }

    // Frame counter using atomic integer for thread safety
    private val frameCounter = AtomicInteger(0)
    private var lastTime = System.currentTimeMillis()
    private var fps = 0

    // Handler for scheduling updates
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    // Memory tracking
    private var peakMemoryUsage = 0

    /**
     * Update runnable for periodic performance reporting
     */
    private val updateRunnable = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            val elapsed = now - lastTime

            if (elapsed > 0) {
                // Calculate FPS
                fps = ((frameCounter.get() * 1000) / elapsed).toInt()
                frameCounter.set(0)
                lastTime = now

                // Monitor memory usage
                val runtime = Runtime.getRuntime()
                val usedMemory = ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).toInt()
                val totalMemory = (runtime.totalMemory() / (1024 * 1024)).toInt()

                // Track peak memory usage
                if (usedMemory > peakMemoryUsage) {
                    peakMemoryUsage = usedMemory
                }

                // Log performance metrics
                Log.d(TAG, "FPS: $fps, Memory: $usedMemory MB / $totalMemory MB, Peak: $peakMemoryUsage MB")
            }

            if (isRunning) {
                handler.postDelayed(this, UPDATE_INTERVAL_MS)
            }
        }
    }

    /**
     * Start performance monitoring
     */
    fun start() {
        if (!isRunning) {
            isRunning = true
            lastTime = System.currentTimeMillis()
            frameCounter.set(0)
            handler.post(updateRunnable)
            Log.d(TAG, "Performance monitoring started")
        }
    }

    /**
     * Stop performance monitoring
     */
    fun stop() {
        isRunning = false
        handler.removeCallbacks(updateRunnable)
        Log.d(TAG, "Performance monitoring stopped")
    }

    /**
     * Count a rendered frame
     */
    fun countFrame() {
        frameCounter.incrementAndGet()
    }

    /**
     * Get the current FPS
     * @return Current frames per second
     */
    fun getFps(): Int = fps

    /**
     * Get peak memory usage in MB
     * @return Peak memory usage in megabytes
     */
    fun getPeakMemoryUsage(): Int = peakMemoryUsage

    /**
     * Reset peak memory tracking
     */
    fun resetPeakMemory() {
        peakMemoryUsage = 0
    }

    /**
     * Get current memory usage in MB
     * @return Current memory usage in megabytes
     */
    fun getCurrentMemoryUsage(): Int {
        val runtime = Runtime.getRuntime()
        return ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).toInt()
    }
}