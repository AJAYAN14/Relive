package com.jian.relive.core.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.jian.relive.core.ui.composition.LocalHapticsEnabled
import com.jian.relive.core.ui.theme.ReliveParticleBlue
import com.jian.relive.core.ui.theme.ReliveParticleGold
import com.jian.relive.core.ui.theme.ReliveParticleGreen
import com.jian.relive.core.ui.theme.ReliveParticleLightBlue
import com.jian.relive.core.ui.theme.ReliveParticleOrange
import com.jian.relive.core.ui.theme.ReliveParticleRed
import com.jian.relive.core.ui.theme.ReliveParticleTeal
import kotlin.math.cos
import kotlin.math.sin

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val rotation: Float,
    val rotationSpeed: Float
)

@Composable
fun CelebrationConfetti(
    modifier: Modifier = Modifier,
    intensity: Int = 100
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current

    val particles = remember(intensity) {
        val colors = listOf(
            ReliveParticleRed,
            ReliveParticleTeal,
            ReliveParticleGold,
            ReliveParticleBlue,
            ReliveParticleGreen,
            ReliveParticleOrange,
            ReliveParticleLightBlue
        )
        List(intensity) {
            val angle = Math.random() * 2 * Math.PI
            val speed = (Math.random() * 15 + 10).toFloat() * (if (intensity > 100) 1.5f else 1f)
            ConfettiParticle(
                x = 0f,
                y = 0f,
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed - 15).toFloat(),
                color = colors.random(),
                rotation = (Math.random() * 360).toFloat(),
                rotationSpeed = (Math.random() * 10 - 5).toFloat()
            )
        }
    }

    val time = remember { Animatable(0f) }

    LaunchedEffect(intensity) {
        if (hapticsAllowed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        time.snapTo(0f)
        time.animateTo(
            targetValue = if (intensity > 100) 4f else 2.5f,
            animationSpec = tween(
                durationMillis = if (intensity > 100) 4000 else 2500,
                easing = LinearEasing
            )
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        val t = time.value

        particles.forEach { p ->
            val dx = p.vx * t * 40
            val dy = p.vy * t * 40 + 0.5f * 60f * t * t * 40

            val x = cx + dx
            val y = cy + dy
            val rotation = p.rotation + p.rotationSpeed * t * 10
            val alpha = (1f - t / (if (intensity > 100) 4f else 2.5f)).coerceIn(0f, 1f)

            if (y < size.height && alpha > 0) {
                rotate(rotation, pivot = Offset(x, y)) {
                    drawRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(x - 8, y - 4),
                        size = Size(16f, 8f)
                    )
                }
            }
        }
    }
}
