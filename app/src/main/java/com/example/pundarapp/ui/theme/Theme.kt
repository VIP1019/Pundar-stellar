package com.example.pundarapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PundarColorScheme = lightColorScheme(
    primary = PundarBlue,
    onPrimary = PundarTextOnPrimary,
    primaryContainer = PundarBlueSubtle,
    onPrimaryContainer = PundarBlueDark,

    secondary = PundarGold,
    onSecondary = PundarTextOnGold,
    secondaryContainer = PundarYellowBg,
    onSecondaryContainer = PundarTextPrimary,

    tertiary = PundarSuccess,
    onTertiary = Color.White,
    tertiaryContainer = PundarSuccessLight,
    onTertiaryContainer = Color(0xFF14532D),

    error = PundarError,
    onError = Color.White,
    errorContainer = PundarErrorLight,
    onErrorContainer = Color(0xFF7F1D1D),

    background = PundarBackground,
    onBackground = PundarTextPrimary,

    surface = PundarSurface,
    onSurface = PundarTextPrimary,
    surfaceVariant = PundarSurfaceVariant,
    onSurfaceVariant = PundarTextSecondary,

    outline = PundarBorder,
    outlineVariant = PundarDivider,

    inverseSurface = PundarTextPrimary,
    inverseOnSurface = PundarSurface,
    inversePrimary = PundarBlueLight,

    surfaceTint = PundarBlue,
)

@Composable
fun PundarAppTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = PundarColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}