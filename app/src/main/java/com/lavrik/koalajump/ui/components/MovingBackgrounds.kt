package com.lavrik.koalajump.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.lavrik.koalajump.game.GameEnvironment
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A component that displays animated moving backgrounds based on environment
 */
@Composable
fun MovingBackgrounds(
    environment: GameEnvironment,
    modifier: Modifier = Modifier
) {
    when (environment) {
        GameEnvironment.FOREST -> ForestBackground(modifier)
        GameEnvironment.DESERT -> DesertBackground(modifier)
        GameEnvironment.MOUNTAINS -> MountainsBackground(modifier)
        GameEnvironment.BEACH -> BeachBackground(modifier)
        GameEnvironment.JUNGLE -> JungleBackground(modifier)
        GameEnvironment.DINOSAUR_VALLEY -> DinosaurValleyBackground(modifier)
        GameEnvironment.HAUNTED_GRAVEYARD -> HauntedGraveyardBackground(modifier)
        GameEnvironment.VOLCANIC_CAVES -> VolcanicCavesBackground(modifier)
    }
}

/**
 * Forest background with falling leaves
 */
@Composable
private fun ForestBackground(modifier: Modifier = Modifier) {
    val leaves = remember {
        List(20) {
            Leaf(
                x = Random.nextFloat() * 1000f,
                y = Random.nextFloat() * -500f,
                size = 8f + Random.nextFloat() * 10f,
                speed = 1f + Random.nextFloat() * 2f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = -1f + Random.nextFloat() * 2f,
                horizontalSpeed = -0.3f + Random.nextFloat() * 0.6f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "leaves")

    // Animation value to drive updates
    val animationValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "leafFall"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Update leaves positions based on animation value
        leaves.forEach { leaf ->
            leaf.update(size.height)
            drawLeaf(leaf)
        }
    }
}

/**
 * Desert background with blowing sand
 */
@Composable
private fun DesertBackground(modifier: Modifier = Modifier) {
    val sandParticles = remember {
        List(60) {
            SandParticle(
                x = Random.nextFloat() * 1000f,
                y = 200f + Random.nextFloat() * 500f,
                size = 1f + Random.nextFloat() * 3f,
                speed = 3f + Random.nextFloat() * 4f,
                alpha = 0.2f + Random.nextFloat() * 0.4f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "sand")

    // Animation value to drive updates
    val animationValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sandDrift"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Update sand particles positions based on animation value
        sandParticles.forEach { particle ->
            particle.update(size.width)
            drawSandParticle(particle)
        }
    }
}

/**
 * Mountains background with falling snow
 */
@Composable
private fun MountainsBackground(modifier: Modifier = Modifier) {
    val snowflakes = remember {
        List(40) {
            Snowflake(
                x = Random.nextFloat() * 1000f,
                y = Random.nextFloat() * -500f,
                size = 2f + Random.nextFloat() * 4f,
                speed = 1f + Random.nextFloat() * 2f,
                horizontalSpeed = -0.3f + Random.nextFloat() * 0.6f,
                alpha = 0.5f + Random.nextFloat() * 0.5f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "snow")

    // Animation value to drive updates
    val animationValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snowFall"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Update snowflakes positions based on animation value
        snowflakes.forEach { snowflake ->
            snowflake.update(size.height)
            drawSnowflake(snowflake)
        }
    }
}

/**
 * Beach background with gentle waves
 */
@Composable
private fun BeachBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "waves")

    // Wave animation parameters
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Draw animated waves at the beach/water boundary
        // We'll position it at 80% of screen height, just above where ground starts
        val groundLevel = size.height * 0.8f
        val waveLevel = groundLevel - 10f

        drawWaves(waveLevel, wavePhase)
    }
}

/**
 * Jungle background with rain drops
 */
@Composable
private fun JungleBackground(modifier: Modifier = Modifier) {
    val raindrops = remember {
        List(60) {
            Raindrop(
                x = Random.nextFloat() * 1000f,
                y = Random.nextFloat() * -500f,
                length = 10f + Random.nextFloat() * 20f,
                speed = 10f + Random.nextFloat() * 10f,
                alpha = 0.3f + Random.nextFloat() * 0.3f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "rain")

    // Animation value to drive updates
    val animationValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainFall"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Update raindrops positions based on animation value
        raindrops.forEach { raindrop ->
            raindrop.update(size.height)
            drawRaindrop(raindrop)
        }
    }
}

/**
 * Dinosaur Valley background with falling debris
 */
@Composable
private fun DinosaurValleyBackground(modifier: Modifier = Modifier) {
    val debris = remember {
        List(30) {
            Debris(
                x = Random.nextFloat() * 1000f,
                y = Random.nextFloat() * -500f,
                size = 3f + Random.nextFloat() * 6f,
                speed = 2f + Random.nextFloat() * 3f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 5f,
                color = when(Random.nextInt(3)) {
                    0 -> Color(0xFFA0522D) // Brown
                    1 -> Color(0xFF8B4513) // Dark brown
                    else -> Color(0xFF5E2612) // Reddish brown
                }
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "debris")

    // Animation value to drive updates
    val animationValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "debrisFall"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Update debris positions based on animation value
        debris.forEach { debrisItem ->
            debrisItem.update(size.height)
            drawDebris(debrisItem)
        }
    }
}

/**
 * Haunted Graveyard background with floating ghosts and mist
 */
@Composable
private fun HauntedGraveyardBackground(modifier: Modifier = Modifier) {
    val mistPatches = remember {
        List(15) {
            MistPatch(
                x = Random.nextFloat() * 1000f,
                y = 300f + Random.nextFloat() * 300f,
                width = 100f + Random.nextFloat() * 150f,
                height = 40f + Random.nextFloat() * 60f,
                alpha = 0.1f + Random.nextFloat() * 0.2f,
                speed = 0.5f + Random.nextFloat() * 1f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mist")

    // Animation value to drive updates
    val animationValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mistMovement"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Update and draw mist patches
        mistPatches.forEach { mistPatch ->
            mistPatch.update(size.width)
            drawMistPatch(mistPatch)
        }
    }
}

/**
 * Volcanic Caves background with lava drips and smoke
 */
@Composable
private fun VolcanicCavesBackground(modifier: Modifier = Modifier) {
    val lavaDrops = remember {
        List(25) {
            LavaDrop(
                x = Random.nextFloat() * 1000f,
                y = Random.nextFloat() * -500f,
                size = 4f + Random.nextFloat() * 6f,
                speed = 3f + Random.nextFloat() * 4f,
                alpha = 0.6f + Random.nextFloat() * 0.4f
            )
        }
    }

    val smokeParticles = remember {
        List(20) {
            SmokeParticle(
                x = Random.nextFloat() * 1000f,
                y = 400f + Random.nextFloat() * 200f,
                size = 15f + Random.nextFloat() * 25f,
                alpha = 0.1f + Random.nextFloat() * 0.2f,
                speed = 0.5f + Random.nextFloat() * 1f,
                upwardSpeed = 0.5f + Random.nextFloat() * 1f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "volcanic")

    // Animation value to drive updates
    val animationValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lavaFall"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Draw ambient glow at the bottom
        drawRect(
            color = Color(0x33FF4500), // Semi-transparent orange-red
            topLeft = Offset(0f, size.height * 0.7f),
            size = Size(size.width, size.height * 0.3f)
        )

        // Update and draw lava drops
        lavaDrops.forEach { lavaDrop ->
            lavaDrop.update(size.height)
            drawLavaDrop(lavaDrop)
        }

        // Update and draw smoke particles
        smokeParticles.forEach { smokeParticle ->
            smokeParticle.update(size.width, size.height)
            drawSmokeParticle(smokeParticle)
        }
    }
}

// -------- Background Elements for Existing Environments --------

private data class Leaf(
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float,
    var rotation: Float,
    val rotationSpeed: Float,
    val horizontalSpeed: Float,
    val color: Color = Color(0xFF65A30D) // Green leaf color
) {
    fun update(maxHeight: Float) {
        y += speed
        x += horizontalSpeed
        rotation += rotationSpeed

        // Reset if off-screen
        if (y > maxHeight) {
            y = -size * 2
            x = Random.nextFloat() * 1000f
        }
    }
}

private data class SandParticle(
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float,
    val color: Color = Color(0xFFE6CCB2) // Sand color
) {
    fun update(maxWidth: Float) {
        x -= speed // Sand moves from right to left

        // Reset if off-screen
        if (x < -size) {
            x = maxWidth + size
            y = 200f + Random.nextFloat() * 500f
        }
    }
}

private data class Snowflake(
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float,
    val horizontalSpeed: Float,
    val alpha: Float,
    val color: Color = Color.White
) {
    fun update(maxHeight: Float) {
        y += speed
        x += horizontalSpeed + sin(y * 0.01f) * 0.5f // Add slight side-to-side movement

        // Reset if off-screen
        if (y > maxHeight) {
            y = -size * 2
            x = Random.nextFloat() * 1000f
        }
    }
}

private data class Raindrop(
    var x: Float,
    var y: Float,
    val length: Float,
    val speed: Float,
    val alpha: Float,
    val color: Color = Color(0xBB0099FF) // Semi-transparent blue
) {
    fun update(maxHeight: Float) {
        y += speed

        // Reset if off-screen
        if (y > maxHeight) {
            y = -length * 2
            x = Random.nextFloat() * 1000f
        }
    }
}

// -------- Background Elements for New Environments --------

private data class Debris(
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float,
    var rotation: Float,
    val rotationSpeed: Float,
    val color: Color
) {
    fun update(maxHeight: Float) {
        y += speed
        rotation += rotationSpeed

        // Reset if off-screen
        if (y > maxHeight) {
            y = -size * 2
            x = Random.nextFloat() * 1000f
        }
    }
}

private data class MistPatch(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val alpha: Float,
    val speed: Float
) {
    fun update(maxWidth: Float) {
        x -= speed

        // Reset if off-screen
        if (x + width < 0) {
            x = maxWidth
            y = 300f + Random.nextFloat() * 300f
        }
    }
}

private data class LavaDrop(
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float
) {
    fun update(maxHeight: Float) {
        y += speed

        // Reset if off-screen
        if (y > maxHeight) {
            y = -size * 2
            x = Random.nextFloat() * 1000f
        }
    }
}

private data class SmokeParticle(
    var x: Float,
    var y: Float,
    val size: Float,
    val alpha: Float,
    val speed: Float,
    val upwardSpeed: Float
) {
    fun update(maxWidth: Float, maxHeight: Float) {
        x -= speed
        y -= upwardSpeed // Move upward

        // Reset if off-screen
        if (x < -size || y < -size) {
            x = Random.nextFloat() * maxWidth
            y = maxHeight - Random.nextFloat() * 200f
        }
    }
}

// -------- Drawing Methods for Existing Environments --------

private fun DrawScope.drawLeaf(leaf: Leaf) {
    // Use translate and rotate within the DrawScope context
    translate(leaf.x, leaf.y) {
        rotate(leaf.rotation) {
            // Draw a simple leaf shape
            val path = Path().apply {
                moveTo(0f, -leaf.size)
                quadraticBezierTo(leaf.size, 0f, 0f, leaf.size)
                quadraticBezierTo(-leaf.size, 0f, 0f, -leaf.size)
                close()
            }

            drawPath(
                path = path,
                color = leaf.color.copy(alpha = 0.6f)
            )

            // Draw a simple stem
            drawLine(
                color = Color(0xFF92400E), // Brown stem
                start = Offset(0f, leaf.size),
                end = Offset(0f, leaf.size * 1.5f),
                strokeWidth = leaf.size * 0.2f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawSandParticle(particle: SandParticle) {
    drawCircle(
        color = particle.color.copy(alpha = particle.alpha),
        radius = particle.size,
        center = Offset(particle.x, particle.y)
    )
}

private fun DrawScope.drawSnowflake(snowflake: Snowflake) {
    drawCircle(
        color = snowflake.color.copy(alpha = snowflake.alpha),
        radius = snowflake.size,
        center = Offset(snowflake.x, snowflake.y)
    )
}

private fun DrawScope.drawWaves(yLevel: Float, phase: Float) {
    val waveWidth = size.width
    val waveHeight = 8f
    val segments = 10

    // Draw several overlapping wave lines
    for (waveOffset in 0..2) {
        val path = Path().apply {
            moveTo(0f, yLevel + waveOffset * 3f)

            for (i in 0..segments) {
                val x = i * (waveWidth / segments)
                val y = yLevel + waveOffset * 3f + sin(i * 0.5f + phase + waveOffset) * waveHeight
                lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = Color(0x880099FF).copy(alpha = 0.2f - (waveOffset * 0.05f)),
            style = Stroke(width = 3f - waveOffset * 0.5f)
        )
    }
}

private fun DrawScope.drawRaindrop(raindrop: Raindrop) {
    drawLine(
        color = raindrop.color.copy(alpha = raindrop.alpha),
        start = Offset(raindrop.x, raindrop.y),
        end = Offset(raindrop.x, raindrop.y + raindrop.length),
        strokeWidth = 1.5f,
        cap = StrokeCap.Round
    )
}

// -------- Drawing Methods for New Environments --------

private fun DrawScope.drawDebris(debris: Debris) {
    translate(debris.x, debris.y) {
        rotate(debris.rotation) {
            // Draw a simple rock/debris shape
            drawCircle(
                color = debris.color.copy(alpha = 0.7f),
                radius = debris.size,
                center = Offset(0f, 0f)
            )

            // Add some details to make it look like rock
            val detailPath = Path().apply {
                moveTo(-debris.size * 0.3f, -debris.size * 0.3f)
                lineTo(debris.size * 0.4f, -debris.size * 0.1f)
                lineTo(debris.size * 0.1f, debris.size * 0.4f)
                close()
            }

            drawPath(
                path = detailPath,
                color = debris.color.copy(alpha = 0.4f)
            )
        }
    }
}

private fun DrawScope.drawMistPatch(mistPatch: MistPatch) {
    // Draw an oval-shaped mist patch
    drawOval(
        color = Color.White.copy(alpha = mistPatch.alpha),
        topLeft = Offset(mistPatch.x, mistPatch.y),
        size = Size(mistPatch.width, mistPatch.height)
    )
}

private fun DrawScope.drawLavaDrop(lavaDrop: LavaDrop) {
    // Gradient from yellow to red for lava drop
    val innerColor = Color(0xFFFFD700) // Gold/yellow center
    val outerColor = Color(0xFFFF4500) // Orange-red outer

    // Draw main drop
    drawCircle(
        color = outerColor.copy(alpha = lavaDrop.alpha),
        radius = lavaDrop.size,
        center = Offset(lavaDrop.x, lavaDrop.y)
    )

    // Draw inner glow
    drawCircle(
        color = innerColor.copy(alpha = lavaDrop.alpha * 0.8f),
        radius = lavaDrop.size * 0.6f,
        center = Offset(lavaDrop.x, lavaDrop.y)
    )

    // Add tail effect for fast-falling lava
    drawLine(
        color = outerColor.copy(alpha = lavaDrop.alpha * 0.5f),
        start = Offset(lavaDrop.x, lavaDrop.y - lavaDrop.size),
        end = Offset(lavaDrop.x, lavaDrop.y),
        strokeWidth = lavaDrop.size * 0.5f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawSmokeParticle(smokeParticle: SmokeParticle) {
    // Draw smoke as a soft gray circle
    drawCircle(
        color = Color.Gray.copy(alpha = smokeParticle.alpha),
        radius = smokeParticle.size,
        center = Offset(smokeParticle.x, smokeParticle.y)
    )
}