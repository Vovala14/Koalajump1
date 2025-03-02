package com.lavrik.koalajump

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.atomic.AtomicInteger

/**
 * Simple performance monitoring utility
 */
class PerformanceTracker {
    private val TAG = "PerformanceTracker"
    private val frameCounter = AtomicInteger(0)
    private var lastTime = System.currentTimeMillis()
    private var fps = 0
    private val handler = Handler(Looper.getMainLooper())

    private val updateRunnable = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            val elapsed = now - lastTime

            if (elapsed > 0) {
                fps = ((frameCounter.get() * 1000) / elapsed).toInt()
                frameCounter.set(0)
                lastTime = now

                // Log memory usage
                val runtime = Runtime.getRuntime()
                val usedMemory = ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).toInt()
                val totalMemory = (runtime.totalMemory() / (1024 * 1024)).toInt()

                Log.d(TAG, "FPS: $fps, Memory: $usedMemory MB / $totalMemory MB")
            }

            handler.postDelayed(this, 1000)
        }
    }

    fun start() {
        handler.post(updateRunnable)
    }

    fun stop() {
        handler.removeCallbacks(updateRunnable)
    }

    fun countFrame() {
        frameCounter.incrementAndGet()
    }

    fun getFps(): Int = fps
}