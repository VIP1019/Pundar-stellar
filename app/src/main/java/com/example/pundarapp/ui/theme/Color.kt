package com.example.pundarapp.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════
//  PUNDAR — Premium Fintech Design System
//  "Build Together. Grow Together."
//  Comparable to: GCash · Maya · GoTyme · SeaBank · Tonik
// ═══════════════════════════════════════════════════════════════════

// ── Page Backgrounds ─────────────────────────────────────────────
val Navy950          = Color(0xFF060B18)   // Deepest bg (below scaffold)
val Navy900          = Color(0xFF090F1F)   // Primary page background
val Navy850          = Color(0xFF0C1428)   // Secondary bg / drawer
val Navy800          = Color(0xFF101C34)   // Card background
val Navy700          = Color(0xFF152240)   // Elevated card
val Navy600          = Color(0xFF1A2B50)   // Input / secondary surface
val Navy500          = Color(0xFF1F3260)   // Hover / subtle surface
val NavyBorder       = Color(0xFF243870)   // Thin border on dark cards

// ── Royal Blue — Primary Brand ───────────────────────────────────
val Blue600          = Color(0xFF1A56DB)   // Primary CTA (deep royal blue)
val Blue500          = Color(0xFF2563EB)   // Brand blue
val Blue400          = Color(0xFF3B82F6)   // Mid blue
val Blue300          = Color(0xFF60A5FA)   // Light blue accent
val Blue200          = Color(0xFF93C5FD)   // Pale blue (text on dark)
val BlueGlow         = Color(0x332563EB)   // 20% blue — glow
val BlueGlowSoft     = Color(0x1A2563EB)   // 10% blue — soft tint
val BlueGlowStrong   = Color(0x4D2563EB)   // 30% blue — stronger glow

// ── Electric Accent ───────────────────────────────────────────────
val Electric400      = Color(0xFF38BDF8)   // Sky blue electric accent
val Electric300      = Color(0xFF7DD3FC)   // Soft electric
val ElectricGlow     = Color(0x2038BDF8)

// ── White / Light Surfaces ────────────────────────────────────────
val White            = Color(0xFFFFFFFF)
val Gray50           = Color(0xFFF8FAFC)
val Gray100          = Color(0xFFF1F5F9)
val Gray200          = Color(0xFFE2E8F0)
val Gray300          = Color(0xFFCBD5E1)
val Gray400          = Color(0xFF94A3B8)
val Gray500          = Color(0xFF64748B)
val Gray600          = Color(0xFF475569)

// ── Text ──────────────────────────────────────────────────────────
val TextWhite        = Color(0xFFF8FAFC)   // Primary text on dark
val TextSoft         = Color(0xFFCBD5E1)   // Secondary text
val TextMuted        = Color(0xFF94A3B8)   // Tertiary / placeholders
val TextDim          = Color(0xFF64748B)   // Very muted
val TextDisabled     = Color(0xFF334155)

// ── Success ───────────────────────────────────────────────────────
val Green500         = Color(0xFF22C55E)
val Green400         = Color(0xFF4ADE80)
val Green600         = Color(0xFF16A34A)
val GreenGlow        = Color(0x1A22C55E)
val GreenBg          = Color(0xFF0A2718)

// ── Warning ───────────────────────────────────────────────────────
val Orange500        = Color(0xFFF97316)
val Orange400        = Color(0xFFFB923C)
val OrangeGlow       = Color(0x1AF97316)
val OrangeBg         = Color(0xFF2A1200)

// ── Error ─────────────────────────────────────────────────────────
val Red500           = Color(0xFFEF4444)
val Red400           = Color(0xFFF87171)
val RedGlow          = Color(0x1AEF4444)
val RedBg            = Color(0xFF2A0A0A)

// ── Gold — Investment / Premium ───────────────────────────────────
val Gold500          = Color(0xFFEAB308)
val Gold400          = Color(0xFFFACC15)
val Gold300          = Color(0xFFFDE047)
val GoldWarm         = Color(0xFFD97706)
val GoldGlow         = Color(0x1AEAB308)
val GoldBg           = Color(0xFF1C1400)

// ── Glass / Overlay ───────────────────────────────────────────────
val Glass10          = Color(0x1AFFFFFF)   // 10% white glass
val Glass15          = Color(0x26FFFFFF)   // 15% white glass
val Glass20          = Color(0x33FFFFFF)   // 20% white glass
val GlassBorderWhite = Color(0x1AFFFFFF)   // Subtle white border
val GlassBorderBlue  = Color(0x332563EB)   // Blue-tinted glass border
val Overlay          = Color(0xCC060B18)

// ── Gradient Helpers ──────────────────────────────────────────────
val CardGradStart    = Navy800
val CardGradEnd      = Navy700
val WalletGradStart  = Color(0xFF0F2463)   // Deep royal blue card
val WalletGradMid    = Color(0xFF1A3580)
val WalletGradEnd    = Color(0xFF0A1840)

// ── Legacy aliases — all existing screens resolve automatically ───
val SpaceBlack            = Navy950
val SpaceNavy             = Navy900
val SpaceDeep             = Navy800
val SpaceDark             = Navy700
val SpaceMedium           = Navy600
val SpaceLight            = Navy500
val SpaceBorder           = NavyBorder
val ElectricBlue          = Blue400
val ElectricBlueDeep      = Blue600
val ElectricBlueDim       = Blue500
val NeonBlue              = Blue300
val PrimaryGlow           = BlueGlow
val PrimaryGlowSoft       = BlueGlowSoft
val NeonCyan              = Electric400
val NeonCyanDim           = Electric300
val CyanGlow              = ElectricGlow
val ElectricPurple        = Color(0xFF8B5CF6)
val NeonPurple            = Color(0xFFA78BFA)
val PurpleGlow            = Color(0x1A8B5CF6)
val PremiumGold           = Gold400
val PremiumGoldWarm       = Gold500
val PremiumGoldDim        = GoldWarm
val GoldGlow_             = GoldGlow
val NeonGreen             = Green400
val NeonGreenDim          = Green500
val GreenGlow_            = GreenGlow
val SuccessDeep           = Green600
val ErrorRed              = Red500
val ErrorRedDim           = Red500
val ErrorGlow             = RedGlow
val WarningAmber          = Orange500
val WarningGlow           = OrangeGlow
val TextPrimary           = TextWhite
val TextSecondary         = TextSoft
val TextTertiary          = TextMuted
val TextOnDark            = White
val GlassWhite            = Glass10
val GlassWhiteMid         = Glass15
val GlassBorder           = GlassBorderWhite
val GlassBorderBright     = Glass20
val GlassShimmer          = Color(0x08FFFFFF)
val GradStart             = CardGradStart
val GradMid               = Navy850
val GradEnd               = Navy950
val CardBlueStart         = WalletGradStart
val CardBlueEnd           = WalletGradEnd
val CardPurpleStart       = Color(0xFF1A0D3D)
val CardPurpleEnd         = Color(0xFF120928)
val CardGoldStart         = GoldBg
val CardGoldEnd           = Color(0xFF100C00)
val ChartBlue             = Blue400
val ChartGradStart        = Color(0x4D2563EB)
val ChartGradEnd          = Color(0x002563EB)
val PundarBlue            = Blue500
val PundarBlueDark        = Blue600
val PundarBlueLight       = Blue300
val PundarBlueSubtle      = Navy600
val PundarGold            = Gold500
val PundarGoldDark        = GoldWarm
val PundarGoldLight       = Gold300
val PundarYellow          = Gold400
val PundarYellowBg        = GoldBg
val PundarBackground      = Navy900
val PundarSurface         = Navy800
val PundarSurfaceVariant  = Navy600
val PundarTextPrimary     = TextWhite
val PundarTextSecondary   = TextSoft
val PundarTextTertiary    = TextMuted
val PundarTextOnPrimary   = White
val PundarTextOnGold      = Navy950
val PundarSuccess         = Green400
val PundarSuccessLight    = GreenBg
val PundarError           = Red500
val PundarErrorLight      = RedBg
val PundarWarning         = Orange500
val PundarWarningLight    = OrangeBg
val PundarInfo            = Blue400
val PundarInfoLight       = Navy600
val PundarBorder          = NavyBorder
val PundarDivider         = Navy500
val PundarChartLine       = Blue400
val PundarChartGradientStart = ChartGradStart
val PundarChartGradientEnd   = ChartGradEnd
val PundarOverlay         = Overlay
val PundarBadge           = Red500
// Stitch-era aliases kept for backward compat
val CharcoalBase          = Navy900
val CharcoalSurface       = Navy800
val CharcoalElevated      = Navy700
val CharcoalMid           = Navy600
val CharcoalLight         = Navy500
val HeaderDark            = Color(0xCC090F1F)
val BorderWhiteStrong     = GlassBorderWhite
val BorderWhiteSoft       = Color(0x0DFFFFFF)
val BorderWhiteMid        = Glass15
val BorderCharcoal        = NavyBorder
val BorderGlass           = Color(0x0AFFFFFF)
val AccentCyanBlue        = Blue300
val AccentCyanBlueDim     = Blue400
val AccentCyanBlueGlow    = BlueGlowSoft
val AccentCyanBlueMid     = BlueGlow
val AccentCyanBlueSoft    = BlueGlowSoft
val AccentGold            = Gold500
val AccentGoldDim         = GoldWarm
val AccentGoldGlow        = GoldGlow
val AccentGreen           = Green400
val AccentGreenDim        = Green500
val AccentGreenGlow       = GreenGlow
val AccentPurple          = Color(0xFF8B5CF6)
val AccentPurpleGlow      = Color(0x1A8B5CF6)
val SemanticError         = Red500
val SemanticErrorBg       = RedBg
val SemanticWarning       = Orange500
val SemanticSuccess       = Green400
val CardSurfaceStart      = Navy800
val CardSurfaceEnd        = Navy700
val CapsuleAccentFill     = BlueGlowSoft
