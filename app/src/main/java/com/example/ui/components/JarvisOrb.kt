package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.voice.VoiceState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JarvisOrb(
    voiceState: VoiceState,
    audioLevel: Float,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_orb")

    // Continuous rotation for outer tech ring
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Reverse rotation for inner ring
    val reverseRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reverse_rotation"
    )

    // Breathing pulse for idle/speaking
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val coreColor = when (voiceState) {
        VoiceState.LISTENING -> CyanPrimary
        VoiceState.PROCESSING -> Color(0xFFC084FC) // Radiant cursed lavender
        VoiceState.SPEAKING -> Color(0xFFF43F5E) // Cursed energy crimson glow
        VoiceState.ERROR -> Color(0xFFEF4444) // Red
        VoiceState.IDLE -> CyanDark
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2f * 0.72f

            // Dynamic scale influenced by audio level
            val dynamicScale = when (voiceState) {
                VoiceState.LISTENING -> 1f + (audioLevel * 0.45f)
                VoiceState.SPEAKING -> pulse * 1.05f
                VoiceState.PROCESSING -> pulse * 0.98f
                else -> pulse
            }

            val currentRadius = baseRadius * dynamicScale

            // 1. Soft radial glow background
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor.copy(alpha = 0.35f),
                        coreColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * 1.5f
                ),
                radius = currentRadius * 1.5f,
                center = center
            )

            // 2. Outer dashed orbital ring (rotating)
            val outerRadius = currentRadius * 1.25f
            drawArc(
                color = CyanGlow.copy(alpha = 0.4f),
                startAngle = rotation,
                sweepAngle = 100f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = androidx.compose.ui.geometry.Size(outerRadius * 2, outerRadius * 2)
            )
            drawArc(
                color = CyanGlow.copy(alpha = 0.4f),
                startAngle = rotation + 180f,
                sweepAngle = 100f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = androidx.compose.ui.geometry.Size(outerRadius * 2, outerRadius * 2)
            )

            // 3. Counter-rotating inner segmented arcs
            val midRadius = currentRadius * 1.1f
            for (i in 0 until 4) {
                val start = reverseRotation + (i * 90f) + 15f
                drawArc(
                    color = coreColor.copy(alpha = 0.65f),
                    startAngle = start,
                    sweepAngle = 60f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(center.x - midRadius, center.y - midRadius),
                    size = androidx.compose.ui.geometry.Size(midRadius * 2, midRadius * 2)
                )
            }

            // 4. Center Core Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        coreColor,
                        coreColor.copy(alpha = 0.6f)
                    ),
                    center = center,
                    radius = currentRadius * 0.65f
                ),
                radius = currentRadius * 0.6f,
                center = center
            )

            // 5. Audio wave bars inside core when listening or speaking
            val barCount = 7
            val maxBarHeight = currentRadius * 0.4f
            val spacing = (currentRadius * 0.8f) / (barCount - 1)
            val startX = center.x - (currentRadius * 0.4f)

            for (i in 0 until barCount) {
                val x = startX + (i * spacing)
                val distFromCenter = kotlin.math.abs(i - (barCount / 2))
                val heightFactor = 1f - (distFromCenter * 0.18f)

                val barHeight = when (voiceState) {
                    VoiceState.LISTENING -> (maxBarHeight * audioLevel * heightFactor).coerceAtLeast(6.dp.toPx())
                    VoiceState.SPEAKING -> (maxBarHeight * (pulse - 0.9f) * 5f * heightFactor).coerceAtLeast(8.dp.toPx())
                    VoiceState.PROCESSING -> (maxBarHeight * 0.35f * (sin((rotation + i * 40).toDouble() * Math.PI / 180).toFloat().coerceAtLeast(0.1f))).coerceAtLeast(4.dp.toPx())
                    else -> 4.dp.toPx()
                }

                drawLine(
                    color = Color.White.copy(alpha = 0.9f),
                    start = Offset(x, center.y - (barHeight / 2f)),
                    end = Offset(x, center.y + (barHeight / 2f)),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
