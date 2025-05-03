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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.lavrik.koalajump.R
import java.io.InputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

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

        // Power-up visual effect constants - Super Saiyan style!
        private const val POWER_UP_AURA_ALPHA_OUTER = 0x55      // Semi-transparent outer aura
        private const val POWER_UP_AURA_ALPHA_INNER = 0x88      // Less transparent inner aura
        private const val POWER_UP_AURA_COLOR_PRIMARY = 0xFFFFD700  // Gold
        private const val POWER_UP_AURA_COLOR_SECONDARY = 0xFFFFA500  // Orange
        private const val POWER_UP_AURA_RADIUS_FACTOR = 1.6f    // Larger aura
        private const val POWER_UP_INNER_AURA_RADIUS_FACTOR = 1.3f // Inner aura
        private const val POWER_UP_SPEED_LINE_LENGTH_FACTOR = 3.0f // Longer speed lines
        private const val POWER_UP_SPEED_LINE_ALPHA = 0x66 // Semi-transparent
        private const val POWER_UP_PULSE_PERIOD = 300      // Faster pulsing
        private const val POWER_UP_PULSE_MAGNITUDE = 0.08f // 8% size variation
        private const val POWER_UP_SPARK_COUNT = 8       // Number of sparks
        private const val POWER_UP_FLARE_COUNT = 6       // Number of flame flares

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
     * Draw the koala using the GIF animation with enhanced Super Saiyan power-up effects
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

            // Add Super Saiyan power-up visual effects before drawing koala
            if (isPoweredUp) {
                val koalaCenter = Offset(x + width / 2, y + height / 2)

                // Calculate pulsing effect for aura
                val pulsePhase = (now % POWER_UP_PULSE_PERIOD) / POWER_UP_PULSE_PERIOD.toFloat() * (2 * PI)
                val pulseFactor = 1.0f + (sin(pulsePhase) * POWER_UP_PULSE_MAGNITUDE).toFloat()

                // Draw outer aura (golden glow)
                drawScope.drawCircle(
                    color = Color(POWER_UP_AURA_COLOR_PRIMARY).copy(alpha = POWER_UP_AURA_ALPHA_OUTER / 255f),
                    radius = width * POWER_UP_AURA_RADIUS_FACTOR * pulseFactor,
                    center = koalaCenter
                )

                // Draw inner aura (more intense)
                drawScope.drawCircle(
                    color = Color(POWER_UP_AURA_COLOR_SECONDARY).copy(alpha = POWER_UP_AURA_ALPHA_INNER / 255f),
                    radius = width * POWER_UP_INNER_AURA_RADIUS_FACTOR * pulseFactor,
                    center = koalaCenter
                )

                // Draw Super Saiyan flame-like aura spikes
                drawSuperSaiyanAura(drawScope, koalaCenter, now, pulseFactor)

                // Draw energy sparks around the koala
                drawEnergySparks(drawScope, koalaCenter, now)

                // Draw speed lines behind koala (enhanced)
                val speedLineLength = width * POWER_UP_SPEED_LINE_LENGTH_FACTOR
                for (i in 0 until 5) {
                    val lineY = y + height / 5 + (i * height / 5)
                    val lineLength = speedLineLength * (0.7f + Random.nextFloat() * 0.6f)
                    val lineAlpha = (POWER_UP_SPEED_LINE_ALPHA * (0.6f + Random.nextFloat() * 0.4f)).toInt()

                    drawScope.drawLine(
                        start = Offset(x - lineLength, lineY),
                        end = Offset(x, lineY),
                        color = Color.White.copy(alpha = lineAlpha / 255f),
                        strokeWidth = 2f + Random.nextFloat() * 3f
                    )
                }

                // Draw energy particles trailing the koala
                drawEnergyParticles(drawScope, koalaCenter, now)
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
     * Draw Super Saiyan flame-like aura around the koala
     */
    private fun drawSuperSaiyanAura(drawScope: DrawScope, center: Offset, now: Long, pulseFactor: Float) {
        // Create several flame-like spikes around the koala
        for (i in 0 until POWER_UP_FLARE_COUNT) {
            val angle = i * (2 * PI.toFloat() / POWER_UP_FLARE_COUNT)

            // Add time-based variation to each flare
            val timeOffset = sin((now / 100f) + i * 1.5f) * 0.3f
            val flareHeight = width * (1.2f + timeOffset) * pulseFactor

            // Create flame path
            val flamePath = Path().apply {
                val startX = center.x + cos(angle) * width * 0.8f
                val startY = center.y + sin(angle) * width * 0.8f

                val midX1 = center.x + cos(angle - 0.2f) * flareHeight * 0.7f
                val midY1 = center.y + sin(angle - 0.2f) * flareHeight * 0.7f

                val peakX = center.x + cos(angle) * flareHeight
                val peakY = center.y + sin(angle) * flareHeight

                val midX2 = center.x + cos(angle + 0.2f) * flareHeight * 0.7f
                val midY2 = center.y + sin(angle + 0.2f) * flareHeight * 0.7f

                moveTo(startX, startY)
                cubicTo(midX1, midY1, peakX, peakY, midX2, midY2)
                close()
            }

            // Draw the flame with gradient from gold to transparent
            val flareAlpha = (150 + sin(now / 200f + i) * 50).toInt().coerceIn(100, 200)
            drawScope.drawPath(
                path = flamePath,
                color = Color(POWER_UP_AURA_COLOR_PRIMARY).copy(alpha = flareAlpha / 255f),
                style = Stroke(width = 4f)
            )

            // Inner fill with less opacity
            drawScope.drawPath(
                path = flamePath,
                color = Color(POWER_UP_AURA_COLOR_PRIMARY).copy(alpha = (flareAlpha * 0.5f) / 255f)
            )
        }
    }

    /**
     * Draw energy sparks around the koala
     */
    private fun drawEnergySparks(drawScope: DrawScope, center: Offset, now: Long) {
        for (i in 0 until POWER_UP_SPARK_COUNT) {
            // Calculate position with time-based movement
            val angle = (i * (2 * PI.toFloat() / POWER_UP_SPARK_COUNT)) + (now / 1000f)
            val distance = width * (1.0f + sin(now / 200f + i) * 0.3f)

            // Different sizes for sparks
            val sparkSize = 3f + Random.nextFloat() * 4f

            // Position sparks around koala
            val sparkX = center.x + cos(angle) * distance
            val sparkY = center.y + sin(angle) * distance

            // Draw spark
            drawScope.drawCircle(
                color = Color.White,
                radius = sparkSize,
                center = Offset(sparkX, sparkY)
            )

            // Add glow effect to spark
            drawScope.drawCircle(
                color = Color(POWER_UP_AURA_COLOR_PRIMARY).copy(alpha = 0.5f),
                radius = sparkSize * 2f,
                center = Offset(sparkX, sparkY)
            )
        }
    }

    /**
     * Draw energy particles trailing the koala
     */
    private fun drawEnergyParticles(drawScope: DrawScope, center: Offset, now: Long) {
        // Generate some random particles behind the koala
        val particleCount = 6
        for (i in 0 until particleCount) {
            val particleAge = (now / 50 + i * 100) % 500
            val particleAlpha = 255 - (particleAge / 500f * 255).toInt()

            if (particleAlpha <= 0) continue

            val particleX = center.x - width * (0.8f + particleAge / 500f * 2f)
            val particleY = center.y - height * 0.25f + Random.nextFloat() * height * 0.5f
            val particleSize = 5f * (1f - particleAge / 500f)

            drawScope.drawCircle(
                color = Color(POWER_UP_AURA_COLOR_PRIMARY).copy(alpha = particleAlpha / 255f),
                radius = particleSize,
                center = Offset(particleX, particleY)
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