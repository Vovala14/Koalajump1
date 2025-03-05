package com.lavrik.koalajump

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Beautiful splash screen using only standard components
 */
class SplashActivity : AppCompatActivity() {
    companion object {
        private const val SPLASH_DURATION = 2000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a beautiful gradient background
        val backgroundGradient = GradientDrawable()
        backgroundGradient.colors = intArrayOf(
            Color.parseColor("#EEFFEE"),  // Light green at top
            Color.parseColor("#FFFFFF")   // White at bottom
        )
        backgroundGradient.orientation = GradientDrawable.Orientation.TOP_BOTTOM

        // Create main layout
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            background = backgroundGradient
            setPadding(0, 0, 0, 0)
        }

        // Create beautiful coffee cup logo
        val coffeeView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                (150 * resources.displayMetrics.density).toInt(),
                (150 * resources.displayMetrics.density).toInt()
            ).apply {
                gravity = Gravity.CENTER
                bottomMargin = (24 * resources.displayMetrics.density).toInt()
            }

            // Set the coffee cup drawable
            setImageDrawable(createCoffeeCupDrawable())
        }

        // Create a pulse animation for the coffee cup
        val pulseAnimation = AnimationSet(true).apply {
            val scaleAnim = ScaleAnimation(
                0.9f, 1.1f, 0.9f, 1.1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            )
            scaleAnim.duration = 1000
            scaleAnim.repeatMode = Animation.REVERSE
            scaleAnim.repeatCount = Animation.INFINITE

            val fadeAnim = AlphaAnimation(0.8f, 1.0f)
            fadeAnim.duration = 1000
            fadeAnim.repeatMode = Animation.REVERSE
            fadeAnim.repeatCount = Animation.INFINITE

            addAnimation(scaleAnim)
            addAnimation(fadeAnim)
        }
        coffeeView.startAnimation(pulseAnimation)

        // Create the game title with style
        val titleText = TextView(this).apply {
            text = "Koala Jump"
            textSize = 42f
            setTextColor(Color.parseColor("#4CAF50"))
            setShadowLayer(3f, 1f, 1f, Color.parseColor("#55000000"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                bottomMargin = (16 * resources.displayMetrics.density).toInt()
            }
        }

        // Create the studio name with style
        val studioText = TextView(this).apply {
            text = "Lavrik Game Studio"
            textSize = 24f
            setTextColor(Color.parseColor("#388E3C"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                bottomMargin = (32 * resources.displayMetrics.density).toInt()
            }
        }

        // Create styled progress bar container with rounded corners
        val progressBarContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }

            // Create rounded corners background
            val shape = GradientDrawable()
            shape.cornerRadius = 16 * resources.displayMetrics.density
            shape.setColor(Color.parseColor("#F5F5F5"))
            background = shape
            setPadding(
                (16 * resources.displayMetrics.density).toInt(),
                (12 * resources.displayMetrics.density).toInt(),
                (16 * resources.displayMetrics.density).toInt(),
                (12 * resources.displayMetrics.density).toInt()
            )
        }

        // Create a stylish progress bar
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                (250 * resources.displayMetrics.density).toInt(),
                (8 * resources.displayMetrics.density).toInt()
            )
            max = 100
            progress = 0
            progressDrawable.setColorFilter(
                Color.parseColor("#4CAF50"),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        // Add percentage text
        val percentText = TextView(this).apply {
            text = "0%"
            textSize = 14f
            setTextColor(Color.parseColor("#757575"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                topMargin = (8 * resources.displayMetrics.density).toInt()
            }
        }

        // Add "Loading game..." text
        val loadingText = TextView(this).apply {
            text = "Loading game..."
            textSize = 16f
            setTextColor(Color.parseColor("#388E3C"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                bottomMargin = (8 * resources.displayMetrics.density).toInt()
            }
        }

        // Add loading text, progress bar and percentage to container
        progressBarContainer.addView(loadingText)
        progressBarContainer.addView(progressBar)
        progressBarContainer.addView(percentText)

        // Add all views to root layout
        rootLayout.addView(coffeeView)
        rootLayout.addView(titleText)
        rootLayout.addView(studioText)
        rootLayout.addView(progressBarContainer)

        // Set content view
        setContentView(rootLayout)

        // Hide system UI for immersive experience
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        // Animate progress bar
        val handler = Handler(Looper.getMainLooper())
        val totalSteps = 100

        for (i in 1..totalSteps) {
            handler.postDelayed({
                val progress = i
                progressBar.progress = progress
                percentText.text = "$progress%"

                // When we reach 100%, launch the main activity
                if (progress >= 100) {
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                }
            }, i * (SPLASH_DURATION / totalSteps))
        }
    }

    /**
     * Creates a beautiful coffee cup drawable
     */
    private fun createCoffeeCupDrawable(): Drawable {
        // Custom drawable for coffee cup
        return object : Drawable() {
            private val cupPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = Color.parseColor("#795548") // Brown
            }

            private val coffeePaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = Color.parseColor("#5D4037") // Darker brown
            }

            private val highlightPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = Color.parseColor("#8D6E63") // Lighter brown
            }

            private val steamPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 5f
                color = Color.parseColor("#BBDEFB") // Light blue
                alpha = 180
            }

            override fun draw(canvas: Canvas) {
                val width = bounds.width().toFloat()
                val height = bounds.height().toFloat()

                // Cup dimensions
                val cupWidth = width * 0.7f
                val cupHeight = height * 0.6f
                val topOffset = height * 0.3f

                // Draw cup body
                val cupRect = RectF(
                    (width - cupWidth) / 2,
                    topOffset,
                    (width + cupWidth) / 2,
                    topOffset + cupHeight
                )
                canvas.drawRoundRect(cupRect, 20f, 20f, cupPaint)

                // Draw coffee inside the cup (slightly smaller than cup)
                val coffeeRect = RectF(
                    cupRect.left + 10,
                    cupRect.top + 10,
                    cupRect.right - 10,
                    cupRect.top + 35 // Just the top part of the cup
                )
                canvas.drawRoundRect(coffeeRect, 15f, 15f, coffeePaint)

                // Draw cup handle
                val handlePath = Path()
                val handleRight = cupRect.right + 20
                val handleTop = cupRect.top + cupHeight * 0.2f
                val handleBottom = cupRect.top + cupHeight * 0.6f

                handlePath.moveTo(cupRect.right, handleTop)
                handlePath.quadTo(
                    handleRight, handleTop,
                    handleRight, (handleTop + handleBottom) / 2
                )
                handlePath.quadTo(
                    handleRight, handleBottom,
                    cupRect.right, handleBottom
                )

                canvas.drawPath(handlePath, cupPaint)

                // Draw cup highlight (reflection)
                val highlightPath = Path()
                highlightPath.moveTo(cupRect.left + 15, cupRect.top + 15)
                highlightPath.lineTo(cupRect.left + 30, cupRect.bottom - 20)
                highlightPath.lineTo(cupRect.left + 45, cupRect.bottom - 20)
                highlightPath.lineTo(cupRect.left + 30, cupRect.top + 15)
                highlightPath.close()

                canvas.drawPath(highlightPath, highlightPaint)

                // Draw steam
                val steamBaseX = cupRect.centerX()
                val steamBaseY = coffeeRect.top - 5

                // First steam curve
                val steamPath1 = Path()
                steamPath1.moveTo(steamBaseX - 20, steamBaseY)
                steamPath1.quadTo(
                    steamBaseX - 30, steamBaseY - 20,
                    steamBaseX - 15, steamBaseY - 40
                )
                canvas.drawPath(steamPath1, steamPaint)

                // Second steam curve
                val steamPath2 = Path()
                steamPath2.moveTo(steamBaseX, steamBaseY - 5)
                steamPath2.quadTo(
                    steamBaseX + 10, steamBaseY - 30,
                    steamBaseX - 5, steamBaseY - 60
                )
                canvas.drawPath(steamPath2, steamPaint)

                // Third steam curve
                val steamPath3 = Path()
                steamPath3.moveTo(steamBaseX + 20, steamBaseY)
                steamPath3.quadTo(
                    steamBaseX + 35, steamBaseY - 25,
                    steamBaseX + 15, steamBaseY - 45
                )
                canvas.drawPath(steamPath3, steamPaint)

                // Draw a saucer under the cup
                val saucerRect = RectF(
                    cupRect.left - 20,
                    cupRect.bottom - 10,
                    cupRect.right + 20,
                    cupRect.bottom + 10
                )
                canvas.drawRoundRect(saucerRect, 15f, 15f, cupPaint)
            }

            override fun setAlpha(alpha: Int) {
                cupPaint.alpha = alpha
                coffeePaint.alpha = alpha
                highlightPaint.alpha = alpha
                steamPaint.alpha = (alpha * 0.7).toInt()
            }

            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
                cupPaint.colorFilter = colorFilter
                coffeePaint.colorFilter = colorFilter
                highlightPaint.colorFilter = colorFilter
                steamPaint.colorFilter = colorFilter
            }

            override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
        }
    }
}