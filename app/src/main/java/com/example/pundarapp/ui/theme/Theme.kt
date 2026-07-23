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
    // Backgrounds: crisp white with very subtle blue tint - premium feel
    bgPrimary    = Color(0xFFF0F4FF),   // Very light blue-white
    bgSecondary  = Color(0xFFE8EEF8),   // Slightly deeper for gradient depth
    bgTertiary   = Color(0xFFFAFCFF),   // Near-pure white for top

    // Surfaces: pure white cards that pop off the background
    surfacePrimary   = Color(0xFFFFFFFF),   // Pure white cards
    surfaceSecondary = Color(0xFFF5F8FF),   // Very faint blue tint
    surfaceTertiary  = Color(0xFFEBF0FA),   // Subtle blue for inputs/chips

    // Borders: clearly visible but not harsh
    borderPrimary = Color(0xFFD0DAEE),      // Soft blue-gray

    // Text: strong contrast on white background
    textPrimary   = Color(0xFF0D1B3E),      // Deep navy (rich dark)
    textSecondary = Color(0xFF2D4278),      // Medium dark navy-blue
    textMuted     = Color(0xFF5B6E9B),      // Medium muted blue-gray
    textDim       = Color(0xFF8A9CC8),      // Dim blue-gray for captions

    // Brand: vibrant blue stays the same across themes
    brandPrimary   = Blue500,
    brandSecondary = Blue600,
    brandLight     = Blue300,

    // Accents: keep same vivid colors - they work well on both themes
    accentElectric = Blue500,
    accentGold     = Color(0xFFD97706),   // Darker gold so it shows on white
    accentGreen    = Color(0xFF059669),   // Darker green for readability
    accentRed      = Color(0xFFDC2626),
    accentOrange   = Color(0xFFEA580C),

    // Glass: dark glass for subtle overlays on white
    glassSubtle  = Color(0x08000080),     // 3% navy tint
    glassMedium  = Color(0x14000060),     // 8% navy
    glassStrong  = Color(0x22000050),     // 13% navy
    glassBorder  = Color(0xFFD0DAEE),     // Solid light blue border

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
