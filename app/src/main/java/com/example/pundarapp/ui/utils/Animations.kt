package com.example.pundarapp.ui.utils

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

// ── Animation Specs ─────────────────────────────────────────────

val FastAnimation = tween<Float>(300, easing = EaseOutCubic)
val MediumAnimation = tween<Float>(600, easing = EaseOutCubic)
val SlowAnimation = tween<Float>(1000, easing = EaseOutCubic)

// ── Entrance Animations ─────────────────────────────────────────

@Composable
fun scaleInAnimation(): Float {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = FastAnimation,
        label = "scaleIn"
    )
    return scale
}

@Composable
fun fadeInAnimation(): Float {
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = MediumAnimation,
        label = "fadeIn"
    )
    return alpha
}

@Composable
fun slideUpAnimation(): Float {
    val offset by animateFloatAsState(
        targetValue = 0f,
        animationSpec = MediumAnimation,
        label = "slideUp"
    )
    return offset
}

// ── Hover & Press Animations ────────────────────────────────────

@Composable
fun hoverScaleModifier(isPressed: Boolean = false): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = FastAnimation,
        label = "hoverScale"
    )
    return Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
}

// ── Pulse Animation ─────────────────────────────────────────────

@Composable
fun pulseAnimation(): Float {
    val pulse by animateFloatAsState(
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    return pulse
}

// ── Shimmer Animation (Loading) ──────────────────────────────────

@Composable
fun shimmerAnimation(): Float {
    val shimmer by animateFloatAsState(
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    return shimmer
}

// ── Float Animation for Cards ───────────────────────────────────

@Composable
fun floatAnimation(): Float {
    val float by animateFloatAsState(
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    return float
}
