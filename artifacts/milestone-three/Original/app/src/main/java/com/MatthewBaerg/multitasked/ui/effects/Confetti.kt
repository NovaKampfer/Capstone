package com.MatthewBaerg.multitasked.ui.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin
import kotlin.random.Random

private const val NUM_CONFETTI = 200
private const val ANIMATION_DURATION = 8000 // Animation lasts 8 seconds
private const val SPAWN_DURATION_FACTOR = 0.625f // Stagger spawn over first 5s (5000ms / 8000ms)

@Composable
fun ConfettiEffect(modifier: Modifier = Modifier, onFinished: () -> Unit) {
    val confetti = remember { List(NUM_CONFETTI) { index -> Confetto.create(index, NUM_CONFETTI) } }
    val animation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animation.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = ANIMATION_DURATION)
        )
        onFinished()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        confetti.forEach { it.draw(this, animation.value) }
    }
}

private data class Confetto(
    val initialXFactor: Float,
    val horizontalVelocity: Float,
    val color: Color,
    val size: Size,
    val flutterSinAmplitude: Float,
    val flutterSinFrequency: Float,
    val endYMargin: Float,
    val spawnTime: Float
) {
    companion object {
        fun create(index: Int, totalCount: Int) = Confetto(
            initialXFactor = Random.nextFloat(),
            horizontalVelocity = Random.nextFloat() * 500f - 250f,
            color = Color(
                red = Random.nextFloat(),
                green = Random.nextFloat(),
                blue = Random.nextFloat()
            ),
            size = Size(width = Random.nextFloat() * 15f + 10f, height = Random.nextFloat() * 8f + 8f),
            flutterSinAmplitude = Random.nextFloat() * 100f + 50f,
            flutterSinFrequency = Random.nextFloat() * 3f + 2f,
            endYMargin = Random.nextFloat() * 200f + 100f,
            // Linearly distribute spawn time based on particle index
            spawnTime = (index.toFloat() / totalCount) * SPAWN_DURATION_FACTOR
        )
    }

    fun draw(drawScope: DrawScope, totalProgress: Float) {
        drawScope.apply {
            if (totalProgress < spawnTime) return

            val particleLifetime = 1.0f - spawnTime
            val progressSinceSpawn = totalProgress - spawnTime
            val particleProgress = progressSinceSpawn / particleLifetime

            val flutter = (sin(particleProgress * Math.PI * 2 * flutterSinFrequency) * flutterSinAmplitude).toFloat()

            val totalVerticalDistance = this.size.height + this@Confetto.size.height + endYMargin
            val currentY = -this@Confetto.size.height + totalVerticalDistance * particleProgress

            val currentPosition = Offset(
                x = initialXFactor * this.size.width + horizontalVelocity * particleProgress + flutter,
                y = currentY
            )

            drawRect(
                color = color,
                topLeft = currentPosition,
                size = this@Confetto.size
            )
        }
    }
}
