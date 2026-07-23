package com.example.pundarapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme

enum class BgAccent {
    Blue, Purple, Gold, Teal, Green, Red
}

@Composable
fun AnimatedBackground(
    accent: BgAccent = BgAccent.Blue,
    content: @Composable () -> Unit
) {
    val accentColor by animateColorAsState(
        targetValue = when (accent) {
            BgAccent.Blue   -> PundarTheme.colors.brandPrimary
            BgAccent.Purple -> Color(0xFF8B5CF6)
            BgAccent.Gold   -> PundarTheme.colors.accentGold
            BgAccent.Teal   -> Color(0xFF14B8A6)
            BgAccent.Green  -> Green500
            BgAccent.Red    -> PundarTheme.colors.accentRed
        },
        animationSpec = tween(1000),
        label = "accent"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val animValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "move"
    )

    val isLight = PundarTheme.colors.isLight
    val bgTop = PundarTheme.colors.bgTertiary
    val bgBottom = PundarTheme.colors.bgPrimary

    val blobAlpha1 = if (isLight) 0.08f else 0.15f
    val blobAlpha2 = if (isLight) 0.05f else 0.10f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgTop)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Base gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(bgTop, bgBottom)
                )
            )

            // Animated blob
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = blobAlpha1), Color.Transparent),
                    center = Offset(
                        x = width * (0.2f + 0.6f * animValue),
                        y = height * (0.1f + 0.2f * (1f - animValue))
                    ),
                    radius = width * 0.8f
                )
            )
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = blobAlpha2), Color.Transparent),
                    center = Offset(
                        x = width * (0.8f - 0.5f * animValue),
                        y = height * (0.8f - 0.3f * animValue)
                    ),
                    radius = width * 0.6f
                )
            )
        }
        content()
    }
}
