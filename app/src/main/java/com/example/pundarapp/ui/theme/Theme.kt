package com.example.pundarapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PundarDarkColorScheme = darkColorScheme(
    primary              = Blue500,           // Royal blue CTA
    onPrimary            = White,
    primaryContainer     = BlueGlowSoft,
    onPrimaryContainer   = Blue200,

    secondary            = Gold500,           // Gold accent
    onSecondary          = Navy950,
    secondaryContainer   = GoldBg,
    onSecondaryContainer = Gold300,

    tertiary             = Green400,          // Success green
    onTertiary           = Navy950,
    tertiaryContainer    = GreenBg,
    onTertiaryContainer  = Green400,

    error                = Red500,
    onError              = White,
    errorContainer       = RedBg,
    onErrorContainer     = Red400,

    background           = Navy900,           // #090F1F
    onBackground         = TextWhite,

    surface              = Navy800,           // #101C34
    onSurface            = TextWhite,
    surfaceVariant       = Navy600,           // #1A2B50
    onSurfaceVariant     = TextSoft,

    outline              = NavyBorder,        // #243870
    outlineVariant       = Navy500,

    inverseSurface       = Gray100,
    inverseOnSurface     = Navy900,
    inversePrimary       = Blue600,

    surfaceTint          = Blue500,
    scrim                = Overlay
)

@Composable
fun PundarAppTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor     = Color.Transparent.toArgb()
            window.navigationBarColor = Navy950.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = PundarDarkColorScheme,
        typography  = Typography,
        content     = content
    )
}
