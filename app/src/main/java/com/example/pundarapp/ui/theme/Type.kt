package com.example.pundarapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

// System sans-serif (Roboto on Android — clean, professional, legible)
val PundarFontFamily = FontFamily.SansSerif

val Typography = Typography(
    // ── Display — Hero numbers (balance, large stats) ──────────
    displayLarge = TextStyle(
        fontFamily    = PundarFontFamily,
        fontWeight    = FontWeight.ExtraBold,
        fontSize      = 40.sp,
        lineHeight    = 48.sp,
        letterSpacing = (-1.0).sp
    ),
    displayMedium = TextStyle(
        fontFamily    = PundarFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 32.sp,
        lineHeight    = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    displaySmall = TextStyle(
        fontFamily    = PundarFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 26.sp,
        lineHeight    = 34.sp,
        letterSpacing = (-0.25).sp
    ),

    // ── Headline — Screen titles, section headers ───────────────
    headlineLarge = TextStyle(
        fontFamily    = PundarFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 24.sp,
        lineHeight    = 32.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineMedium = TextStyle(
        fontFamily    = PundarFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 20.sp,
        lineHeight    = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily    = PundarFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 18.sp,
        lineHeight    = 26.sp,
        letterSpacing = 0.sp
    ),

    // ── Title — Card titles, list headers ──────────────────────
    titleLarge = TextStyle(
        fontFamily    = PundarFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 17.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily    = PundarFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 15.sp,
        lineHeight    = 22.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily    = PundarFontFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 13.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // ── Body — Main content text ────────────────────────────────
    bodyLarge = TextStyle(
        fontFamily    = PundarFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily    = PundarFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontFamily    = PundarFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 18.sp,
        letterSpacing = 0.2.sp
    ),

    // ── Label — Chips, badges, buttons, captions ───────────────
    labelLarge = TextStyle(
        fontFamily    = PundarFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily    = PundarFontFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily    = PundarFontFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 10.sp,
        lineHeight    = 14.sp,
        letterSpacing = 0.5.sp
    )
)
