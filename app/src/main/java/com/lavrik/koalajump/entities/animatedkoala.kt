package com.lavrik.koalajump.entities

import android.content.Context
import android.graphics.Movie
import android.graphics.Paint
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import com.lavrik.koalajump.R
import java.io.InputStream
import kotlin.math.sin

/**
 * Animated koala character using a standard GIF animation
 * Portrait mode only
 */
class AnimatedKoala(
    private val context: Context,
    var screenWidth: Float,
    var screenHeight: Float
) {
    companion object {
        private const val TAG = "AnimatedKoala"
        private const val HITBOX_REDUCTION_PERCENT = 0.15f

        // Portrait physics constants only
        private const val JUMP_VELOCITY = -23.5f
        private const val GRAVITY = 1.8f

        // Portrait positioning constants
        private const val GROUND_RATIO = 0.78f    // 78% of screen height
        private const val X_RATIO = 0.25f         // 25% from left

        // Fixed size dimensions
        private const val TARGET_WIDTH = 83 // 50% larger than original
        private const val TARGET_HEIGHT = 83 // 50% larger than original

        // Power-up visual effect constants
        private const val POWER_UP_GLOW_ALPHA = 0x55      // Semi-transparent
        private const val POWER_UP_GLOW_COLOR = 0xFFAA00  // Orange
        private const val POWER_UP_GLOW_RADIUS_FACTOR = 1.2f  // Smaller glow
        private const val POWER_UP_SPEED_LINE_LENGTH_FACTOR = 2.0f
        private const val POWER_UP_SPEED_LINE_ALPHA = 0x44 // Semi-transparent
        private const val POWER_UP_PULSE_PERIOD = 500      // Milliseconds
        private const val POWER_UP_PULSE_MAGNITUDE = 0.05f // 5% size variation

        // Invincibility effect constants
        private const val INVINCIBILITY_FLASH_PERIOD = 200  // Milliseconds per flash cycle
        private const val INVINCIBILITY_SHIELD_COLOR = 0x3399CCFF // Light blue shield
        private const val INVINCIBILITY_SHIELD_ALPHA = 0x60 // More transparent
        private const val INVINCIBILITY_SHIELD_RADIUS_FACTOR = 1.1f // Just slightly larger than koala
    }

    // GIF animation using Android's Movie class
    private var koalaAnimation: Movie? = null
    private var powerUpAnimation: Movie? = null
    private var animationStartTime: Long = 0

    // Position properties
    var x: Float = 0f
    var y: Float = 0f
    var width: Int = TARGET_WIDTH
    var height: Int = TARGET_HEIGHT

    // Jump properties
    var isJumping = false
    private var jumpVelocity = JUMP_VELOCITY
    private var gravity = GRAVITY
    var groundY: Float

    // Power-up state
    var isPoweredUp = false
    var isInvincible = false // Property for invincibility status

    // Reusable Paint for drawing
    private val paint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = true
    }

    init {
        try {
            // Load the GIF animation using Movie class
            val inputStream: InputStream = context.resources.openRawResource(R.raw.koala_animation)
            koalaAnimation = Movie.decodeStream(inputStream)
            inputStream.close()

            if (koalaAnimation != null) {
                Log.d(TAG, "Successfully loaded koala GIF animation, duration: ${koalaAnimation?.duration()} ms")
            } else {
                Log.e(TAG, "Failed to load koala GIF animation")
            }

            // Initialize the animation start time
            animationStartTime = SystemClock.uptimeMillis()

        } catch (e: Exception) {
            Log.e(TAG, "Error loading koala GIF: ${e.message}")
            koalaAnimation = null
        }

        // Set horizontal position - portrait mode only
        x = screenWidth * X_RATIO

        // Set ground Y position
        groundY = screenHeight * GROUND_RATIO - height
        y = groundY

        Log.d(TAG, "Koala initialized at x=$x, y=$y, groundY=$groundY, screenHeight=$screenHeight")
    }

    /**
     * Update koala position and animation state
     */
    fun update() {
        // Handle jumping physics
        if (isJumping) {
            // Apply velocity
            y += jumpVelocity

            // Apply gravity
            jumpVelocity += gravity

            // Check if landed with proper ground position
            if (y >= groundY) {
                y = groundY
                isJumping = false
                jumpVelocity = JUMP_VELOCITY
            }
        }

        // The GIF animation updates automatically based on time
    }

    /**
     * Draw the koala using the GIF animation with power-up visual effects
     */
    fun draw(drawScope: DrawScope) {
        val animation = if (isPoweredUp && powerUpAnimation != null) {
            powerUpAnimation
        } else {
            koalaAnimation
        }

        if (animation != null) {
            // Get current frame time (looping automatically)
            val now = SystemClock.uptimeMillis()
            val relTime = ((now - animationStartTime) % animation.duration()).toInt()

            // Set the current animation frame time
            animation.setTime(relTime)

            // Scale factor - portrait mode only
            val scaleFactor = 1.5f

            // Get the native canvas from the DrawScope
            val canvas = drawScope.drawContext.canvas.nativeCanvas

            // Add invincibility shield effect (when invincible)
            if (isInvincible) {
                // Determine if koala should be visible based on flash timing
                val isVisible = ((now / INVINCIBILITY_FLASH_PERIOD) % 2 == 0L)

                // Draw invincibility shield (pulsing blue circle)
                val shieldPulse = 1.0f + (sin(now / 200.0 * Math.PI) * 0.05).toFloat()
                drawScope.drawCircle(
                    color = Color(INVINCIBILITY_SHIELD_COLOR).copy(alpha = INVINCIBILITY_SHIELD_ALPHA / 255f),
                    radius = width * INVINCIBILITY_SHIELD_RADIUS_FACTOR * shieldPulse,
                    center = Offset(x + width / 2, y + height / 2)
                )

                // If in flash-invisible state, return early to skip drawing koala
                if (!isVisible && !isPoweredUp) {
                    return
                }
            }

            // Add power-up visual effects before drawing koala
            if (isPoweredUp) {
                // Draw a glowing aura around koala
                drawScope.drawCircle(
                    color = Color(POWER_UP_GLOW_COLOR).copy(alpha = POWER_UP_GLOW_ALPHA / 255f),
                    radius = width * POWER_UP_GLOW_RADIUS_FACTOR,
                    center = Offset(x + width / 2, y + height / 2)
                )

                // Draw speed lines behind koala
                val speedLineLength = width * POWER_UP_SPEED_LINE_LENGTH_FACTOR
                drawScope.drawLine(
                    start = Offset(x - speedLineLength, y + height / 2),
                    end = Offset(x, y + height / 2),
                    color = Color.White.copy(alpha = POWER_UP_SPEED_LINE_ALPHA / 255f),
                    strokeWidth = 5f
                )
            }

            // Save the canvas state
            canvas.save()

            // Calculate scaling needed
            val scaleX = (TARGET_WIDTH * scaleFactor) / animation.width()
            val scaleY = (TARGET_HEIGHT * scaleFactor) / animation.height()

            // Apply pulsing effect for power-up
            val actualScaleX: Float
            val actualScaleY: Float

            if (isPoweredUp) {
                // Add pulsing effect
                val pulsePhase = (now % POWER_UP_PULSE_PERIOD) / POWER_UP_PULSE_PERIOD.toFloat() * (Math.PI * 2)
                val pulseFactor = 1.0f + (sin(pulsePhase) * POWER_UP_PULSE_MAGNITUDE).toFloat()
                actualScaleX = scaleX * pulseFactor
                actualScaleY = scaleY * pulseFactor
            } else {
                actualScaleX = scaleX
                actualScaleY = scaleY
            }

            // Translate and scale
            canvas.translate(x, y)
            canvas.scale(actualScaleX, actualScaleY)

            // Draw the GIF frame
            animation.draw(canvas, 0f, 0f, paint)

            // Restore canvas
            canvas.restore()
        } else {
            // Fallback - draw a placeholder rectangle
            val debugColor = if (isPoweredUp)
                Color.Yellow
            else
                Color.Blue

            drawScope.drawRect(
                color = debugColor,
                topLeft = Offset(x, y),
                size = Size(width.toFloat(), height.toFloat())
            )
        }
    }

    /**
     * Draw the koala with power-up state
     */
    fun draw(drawScope: DrawScope, isPowerUpActive: Boolean) {
        val originalPoweredUp = isPoweredUp
        if (isPowerUpActive) isPoweredUp = true

        draw(drawScope)

        isPoweredUp = originalPoweredUp
    }

    /**
     * Initiate a jump if not already jumping
     */
    fun jump() {
        if (!isJumping) {
            isJumping = true
            jumpVelocity = JUMP_VELOCITY
        }
    }

    /**
     * Get collision bounds for the koala with hitbox reduction for better gameplay
     */
    fun getBounds(): ComposeRect {
        val widthReduction = width * HITBOX_REDUCTION_PERCENT
        val heightReduction = height * HITBOX_REDUCTION_PERCENT

        return ComposeRect(
            offset = Offset(
                x + widthReduction,
                y + heightReduction
            ),
            size = Size(
                (width - widthReduction * 2).toFloat(),
                (height - heightReduction * 2).toFloat()
            )
        )
    }

    /**
     * Update position for new screen dimensions
     */
    fun updatePosition() {
        // Calculate new position based on screen dimensions
        x = screenWidth * X_RATIO

        // Recalculate ground position
        groundY = screenHeight * GROUND_RATIO - height

        // If not actively jumping, place on ground
        if (!isJumping) {
            y = groundY
            Log.d(TAG, "Koala placed on ground at y=$y")
        } else {
            // If jumping, maintain relative height position
            val jumpHeightPercent = (groundY - y) / (groundY - (y + jumpVelocity * 10))
            y = groundY - (jumpHeightPercent * groundY * 0.3f)
        }
    }

    /**
     * Set power-up state to change koala appearance
     */
    fun setPowerUpState(powered: Boolean) {
        isPoweredUp = powered
    }

    /**
     * Set invincibility state
     */
    fun setInvincibleState(invincible: Boolean) {
        isInvincible = invincible
    }

    /**
     * Load the power-up GIF animation
     */
    fun loadPowerUpSprite(powerUpResId: Int) {
        try {
            val inputStream: InputStream = context.resources.openRawResource(powerUpResId)
            powerUpAnimation = Movie.decodeStream(inputStream)
            inputStream.close()

            Log.d(TAG, "Successfully loaded power-up GIF animation")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading power-up GIF: ${e.message}")
            powerUpAnimation = null
        }
    }

    /**
     * Release resources
     */
    fun release() {
        // Movie objects don't need explicit cleanup
        koalaAnimation = null
        powerUpAnimation = null
    }
}