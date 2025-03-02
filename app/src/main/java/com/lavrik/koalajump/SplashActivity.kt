package com.lavrik.koalajump

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

/**
 * Splash screen activity optimized for landscape orientation
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a horizontal layout for landscape
        val rootLayout = LinearLayout(this)
        rootLayout.orientation = LinearLayout.HORIZONTAL
        rootLayout.gravity = android.view.Gravity.CENTER
        rootLayout.setBackgroundColor(android.graphics.Color.WHITE)

        // Create the studio text
        val studioText = TextView(this)
        studioText.text = "Lavrik Game Studio"
        studioText.textSize = 36f
        studioText.setTextColor(android.graphics.Color.BLACK)

        // For landscape, put the text and progress bar side by side with some spacing
        val textLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        textLayoutParams.marginEnd = (30 * resources.displayMetrics.density).toInt() // 30dp margin
        studioText.layoutParams = textLayoutParams

        // Create a progress bar
        val progressBar = android.widget.ProgressBar(this)
        val progressParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        progressBar.layoutParams = progressParams

        // Add views to the layout
        rootLayout.addView(studioText)
        rootLayout.addView(progressBar)

        // Set the content view
        setContentView(rootLayout)

        // Set fullscreen using modern APIs
        setFullscreen()

        // After delay, launch MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 2000) // 2 seconds
    }

    /**
     * Set fullscreen mode using the appropriate API based on Android version
     */
    private fun setFullscreen() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // For Android 11+ (API 30+)
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // For older versions, use WindowCompat from AndroidX
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, window.decorView)?.apply {
                hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}