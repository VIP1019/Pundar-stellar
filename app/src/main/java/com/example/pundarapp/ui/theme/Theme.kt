package com.example.pundarapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Futuristic Dark Color Scheme ────────────────────────────────
private val PundarDarkColorScheme = darkColorScheme(
    primary              = ElectricBlue,
    onPrimary            = SpaceBlack,
    primaryContainer     = CardBlueStart,
    onPrimaryContainer   = NeonBlue,

    secondary            = PremiumGoldWarm,
    onSecondary          = SpaceBlack,
    secondaryContainer   = CardGoldStart,
    onSecondaryContainer = PremiumGold,

    tertiary             = NeonGreen,
    onTertiary           = SpaceBlack,
    tertiaryContainer    = Color(0xFF0A2A1A),
    onTertiaryContainer  = NeonGreen,

    error                = ErrorRed,
    onError              = SpaceBlack,
    errorContainer       = Color(0xFF2A0A0A),
    onErrorContainer     = ErrorRed,

    background           = SpaceNavy,
    onBackground         = TextPrimary,

    surface              = SpaceDeep,
    onSurface            = TextPrimary,
    surfaceVariant       = SpaceMedium,
    onSurfaceVariant     = TextSecondary,

    outline              = SpaceBorder,
    outlineVariant       = SpaceLight,

    inverseSurface       = TextPrimary,
    inverseOnSurface     = SpaceDeep,
    inversePrimary       = ElectricBlueDim,

    surfaceTint          = ElectricBlue,
    scrim                = Color(0xCC050810),
)

@Composable
fun PundarAppTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = PundarDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = SpaceBlack.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars  = false  // white icons on dark bg
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
