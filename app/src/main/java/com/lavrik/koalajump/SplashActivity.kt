package com.lavrik.koalajump

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Ultra-minimal splash screen to avoid any performance issues
 * Pure white background with just a progress bar
 */
class SplashActivity : AppCompatActivity() {
    companion object {
        private const val SPLASH_DURATION = 1000L // Just 1 second for faster loading
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a minimal layout with WHITE background
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(android.graphics.Color.WHITE) // Pure white
        }

        // Create the game title text
        val titleText = TextView(this).apply {
            text = "Koala Jump"
            textSize = 36f
            setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Green
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
                bottomMargin = (12 * resources.displayMetrics.density).toInt()
            }
        }

        // Add "Lavrik Game Studio" text
        val studioText = TextView(this).apply {
            text = "Lavrik Game Studio"
            textSize = 18f
            setTextColor(android.graphics.Color.GRAY) // Subtle gray color
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
                bottomMargin = (24 * resources.displayMetrics.density).toInt()
            }
        }

        // Create a simple progress bar (horizontal style)
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                (250 * resources.displayMetrics.density).toInt(), // Wider
                (20 * resources.displayMetrics.density).toInt()   // Taller
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
            max = 100
            progress = 0
        }

        // Add views to the layout
        rootLayout.addView(titleText)
        rootLayout.addView(studioText) // Add the studio name
        rootLayout.addView(progressBar)

        // Set the content view
        setContentView(rootLayout)

        // Set fullscreen
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN)

        // Animate the progress bar to indicate loading
        val handler = Handler(Looper.getMainLooper())
        val progressIncrement = 5

        for (i in 1..20) {
            handler.postDelayed({
                progressBar.progress = i * progressIncrement

                // When we reach 100%, launch the main activity
                if (i == 20) {
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }, i * (SPLASH_DURATION / 20))
        }
    }
}