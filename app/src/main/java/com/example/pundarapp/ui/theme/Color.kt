package com.example.pundarapp.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════
//  PUNDAR — Futuristic Dark Design System  (International Edition)
//  Inspired by Revolut · Apple Pay · Nubank · Linear
// ═══════════════════════════════════════════════════════════════════

// ── Deep Space Backgrounds ──────────────────────────────────────
val SpaceBlack       = Color(0xFF050810)   // Deepest background
val SpaceNavy        = Color(0xFF080D1A)   // Primary background
val SpaceDeep        = Color(0xFF0C1220)   // Card background
val SpaceDark        = Color(0xFF111827)   // Elevated surface
val SpaceMedium      = Color(0xFF1A2235)   // Input/secondary surface
val SpaceLight       = Color(0xFF1E2A3D)   // Hover / subtle surface
val SpaceBorder      = Color(0xFF1F2D44)   // Border / divider

// ── Electric Blue — Primary Brand ──────────────────────────────
val ElectricBlue     = Color(0xFF00B4FF)   // Vivid electric blue
val ElectricBlueDeep = Color(0xFF0080FF)   // Deeper blue CTA
val ElectricBlueDim  = Color(0xFF1A6EB5)   // Muted blue
val NeonBlue         = Color(0xFF00D4FF)   // Neon glow accent
val PrimaryGlow      = Color(0x4400B4FF)   // Blue glow (68% alpha)
val PrimaryGlowSoft  = Color(0x2200B4FF)   // Soft blue glow

// ── Neon Cyan — Secondary Accent ───────────────────────────────
val NeonCyan         = Color(0xFF00F5FF)   // Vivid cyan
val NeonCyanDim      = Color(0xFF00C8D7)   // Dimmer cyan
val CyanGlow         = Color(0x3300F5FF)

// ── Electric Purple — Tertiary Accent ──────────────────────────
val ElectricPurple   = Color(0xFF8B5CF6)   // Violet accent
val NeonPurple       = Color(0xFFA855F7)   // Bright purple
val PurpleGlow       = Color(0x338B5CF6)

// ── Gold — Premium Tier ─────────────────────────────────────────
val PremiumGold      = Color(0xFFFFD700)   // Vivid gold
val PremiumGoldWarm  = Color(0xFFFFB830)   // Warm gold
val PremiumGoldDim   = Color(0xFFCC9A00)   // Deep gold
val GoldGlow         = Color(0x44FFD700)

// ── Neon Green — Success / Positive ────────────────────────────
val NeonGreen        = Color(0xFF00FF87)   // Vivid success green
val NeonGreenDim     = Color(0xFF00D66F)   // Dimmer success
val GreenGlow        = Color(0x3300FF87)
val SuccessDeep      = Color(0xFF10B981)   // Traditional success

// ── Semantic ────────────────────────────────────────────────────
val ErrorRed         = Color(0xFFFF4757)   // Vivid error
val ErrorRedDim      = Color(0xFFDC2626)   // Traditional error
val ErrorGlow        = Color(0x33FF4757)
val WarningAmber     = Color(0xFFFFB020)   // Warning
val WarningGlow      = Color(0x33FFB020)

// ── Text / On-Dark ──────────────────────────────────────────────
val TextPrimary      = Color(0xFFF1F5F9)   // Near-white text
val TextSecondary    = Color(0xFF94A3B8)   // Muted blue-grey
val TextTertiary     = Color(0xFF64748B)   // Very muted
val TextOnDark       = Color(0xFFFFFFFF)   // Pure white on dark
val TextDisabled     = Color(0xFF374151)   // Disabled text

// ── Glassmorphism ───────────────────────────────────────────────
val GlassWhite       = Color(0x0FFFFFFF)   // Glass bg base (6% white)
val GlassWhiteMid    = Color(0x1AFFFFFF)   // Glass bg mid (10% white)
val GlassBorder      = Color(0x26FFFFFF)   // Glass border (15% white)
val GlassBorderBright= Color(0x40FFFFFF)   // Brighter glass border (25%)
val GlassShimmer     = Color(0x0AFFFFFF)   // Shimmer highlight

// ── Gradient Stops (helpers) ────────────────────────────────────
val GradStart        = Color(0xFF0C1220)
val GradMid          = Color(0xFF0A1628)
val GradEnd          = Color(0xFF050810)

// ── Card Accent Gradients ───────────────────────────────────────
val CardBlueStart    = Color(0xFF0F2150)
val CardBlueEnd      = Color(0xFF1A3570)
val CardPurpleStart  = Color(0xFF1A0A3D)
val CardPurpleEnd    = Color(0xFF2D1560)
val CardGoldStart    = Color(0xFF2A1A00)
val CardGoldEnd      = Color(0xFF3D2600)

// ── Chart Colors ────────────────────────────────────────────────
val ChartBlue        = ElectricBlue
val ChartGradStart   = Color(0x6600B4FF)
val ChartGradEnd     = Color(0x0000B4FF)

// ── Legacy aliases (keep for compatibility with unmodified screens) ──
val PundarBlue            = ElectricBlueDeep
val PundarBlueDark        = Color(0xFF0050CC)
val PundarBlueLight       = NeonBlue
val PundarBlueSubtle      = SpaceMedium
val PundarGold            = PremiumGoldWarm
val PundarGoldDark        = PremiumGoldDim
val PundarGoldLight       = CardGoldEnd
val PundarYellow          = PremiumGold
val PundarYellowBg        = CardGoldEnd
val PundarBackground      = SpaceNavy
val PundarSurface         = SpaceDeep
val PundarSurfaceVariant  = SpaceMedium
val PundarTextPrimary     = TextPrimary
val PundarTextSecondary   = TextSecondary
val PundarTextTertiary    = TextTertiary
val PundarTextOnPrimary   = TextOnDark
val PundarTextOnGold      = SpaceBlack
val PundarSuccess         = NeonGreen
val PundarSuccessLight    = Color(0xFF0A2A1A)
val PundarError           = ErrorRed
val PundarErrorLight      = Color(0xFF2A0A0A)
val PundarWarning         = WarningAmber
val PundarWarningLight    = Color(0xFF2A1A00)
val PundarInfo            = ElectricBlue
val PundarInfoLight       = SpaceMedium
val PundarBorder          = SpaceBorder
val PundarDivider         = SpaceLight
val PundarChartLine       = ElectricBlue
val PundarChartGradientStart = ChartGradStart
val PundarChartGradientEnd   = ChartGradEnd
val PundarOverlay         = Color(0xCC000000)
val PundarBadge           = ErrorRed
