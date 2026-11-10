package com.example.pundarapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class PundarColors(
    val bgPrimary: Color,
    val bgSecondary: Color,
    val bgTertiary: Color,
    val surfacePrimary: Color,
    val surfaceSecondary: Color,
    val surfaceTertiary: Color,
    val borderPrimary: Color,
    
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDim: Color,
    
    val brandPrimary: Color,
    val brandSecondary: Color,
    val brandLight: Color,
    
    val accentElectric: Color,
    val accentGold: Color,
    val accentGreen: Color,
    val accentRed: Color,
    val accentOrange: Color,
    
    val glassSubtle: Color,
    val glassMedium: Color,
    val glassStrong: Color,
    val glassBorder: Color,
    
    val isLight: Boolean
)

val LocalPundarColors = staticCompositionLocalOf<PundarColors> { 
    error("No PundarColors provided")
}

object PundarTheme {
    val colors: PundarColors
        @Composable
        get() = LocalPundarColors.current
    
    val typography: Typography
        @Composable
        get() = MaterialTheme.typography
}

fun pundarDarkColors() = PundarColors(
    bgPrimary = Navy900,
    bgSecondary = Navy950,
    bgTertiary = Navy850,
    surfacePrimary = Navy800,
    surfaceSecondary = Navy700,
    surfaceTertiary = Navy600,
    borderPrimary = NavyBorder,
    
    textPrimary = TextWhite,
    textSecondary = TextSoft,
    textMuted = TextMuted,
    textDim = TextDim,
    
    brandPrimary = Blue500,
    brandSecondary = Blue600,
    brandLight = Blue300,
    
    accentElectric = Electric400,
    accentGold = Gold500,
    accentGreen = Green400,
    accentRed = Red500,
    accentOrange = Orange500,
    
    glassSubtle = Glass10,
    glassMedium = Glass15,
    glassStrong = Glass20,
    glassBorder = GlassBorderWhite,
    
    isLight = false
)

fun pundarLightColors() = PundarColors(
    // Backgrounds: crisp flat light background
    bgPrimary    = Color(0xFFF4F6F9),   // Light slate background
    bgSecondary  = Color(0xFFEBEFF5),
    bgTertiary   = Color(0xFFFFFFFF),   // Pure white top/card background

    // Surfaces: pure flat white cards with solid borders
    surfacePrimary   = Color(0xFFFFFFFF),   // Pure white cards
    surfaceSecondary = Color(0xFFF8FAFC),   // Soft flat slate
    surfaceTertiary  = Color(0xFFEEF2F6),   // Flat inputs/chips

    // Borders: sharp solid borders
    borderPrimary = Color(0xFFE2E8F0),

    // Text: rich dark navy & slate contrast
    textPrimary   = Color(0xFF0F172A),      // Slate 900
    textSecondary = Color(0xFF334155),      // Slate 700
    textMuted     = Color(0xFF64748B),      // Slate 500
    textDim       = Color(0xFF94A3B8),      // Slate 400

    // Brand: deep navy hero blue of PUNDAR
    brandPrimary   = Color(0xFF0B1B3D),     // Deep Dark Navy
    brandSecondary = Color(0xFF0F2450),     // Header Card Navy
    brandLight     = Color(0xFF2563EB),     // Electric Blue Accent

    // Accents matching screenshot
    accentElectric = Color(0xFF2563EB),     // Send Blue
    accentGold     = Color(0xFFF59E0B),     // Score Gold / Receive Orange
    accentGreen    = Color(0xFF10B981),     // Cash In Green
    accentRed      = Color(0xFFEF4444),     // Alert Red
    accentOrange   = Color(0xFFF97316),     // Due Soon Orange

    // Flat overlays
    glassSubtle  = Color(0xFFF1F5F9),
    glassMedium  = Color(0xFFE2E8F0),
    glassStrong  = Color(0xFFCBD5E1),
    glassBorder  = Color(0xFFE2E8F0),

    isLight = true
)

@Composable
fun PundarAppTheme(
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (com.example.pundarapp.ui.data.AppState.themeMode.value) {
        "LIGHT" -> false
        "DARK"  -> true
        else    -> systemDark
    }

    val colors = if (darkTheme) pundarDarkColors() else pundarLightColors()
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = colors.bgTertiary.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalPundarColors provides colors) {
        MaterialTheme(
            typography = Typography,
            content = content
        )
    }
}
