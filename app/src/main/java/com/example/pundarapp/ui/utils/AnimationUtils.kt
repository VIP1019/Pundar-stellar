package com.example.pundarapp.ui.utils

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.pundarapp.ui.theme.GlassShimmer
import com.example.pundarapp.ui.theme.SpaceMedium

// ── Shimmer Effect ───────────────────────────────────────────────
fun Modifier.shimmerEffect(
    shimmerColors: List<Color> = listOf(
        SpaceMedium,
        Color(0xFF2A3A55),
        SpaceMedium
    )
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -1000f,
        targetValue  = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start  = Offset(translateAnim - 200f, 0f),
            end    = Offset(translateAnim + 200f, 0f)
        )
    )
}

// ── Neon Glow Pulse ──────────────────────────────────────────────
@Composable
fun rememberPulseAlpha(
    minAlpha: Float = 0.5f,
    maxAlpha: Float = 1.0f,
    duration: Int = 1500
): State<Float> {
    val infinite = rememberInfiniteTransition(label = "pulse")
    return infinite.animateFloat(
        initialValue = minAlpha,
        targetValue  = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
}

// ── Floating Y Offset ────────────────────────────────────────────
@Composable
fun rememberFloatingOffset(
    amplitude: Float = 8f,
    duration: Int = 2200
): State<Float> {
    val infinite = rememberInfiniteTransition(label = "float")
    return infinite.animateFloat(
        initialValue = -amplitude,
        targetValue  = amplitude,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )
}

// ── Rotation ─────────────────────────────────────────────────────
@Composable
fun rememberSlowRotation(duration: Int = 10_000): State<Float> {
    val infinite = rememberInfiniteTransition(label = "rotate")
    return infinite.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )
}

// ── Shimmer sweep for card highlights ────────────────────────────
@Composable
fun rememberShimmerSweep(duration: Int = 2000): State<Float> {
    val infinite = rememberInfiniteTransition(label = "sweep")
    return infinite.animateFloat(
        initialValue = -1f,
        targetValue  = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepOffset"
    )
}

// ── EaseInOutSine approximation via cubic ────────────────────────
private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
