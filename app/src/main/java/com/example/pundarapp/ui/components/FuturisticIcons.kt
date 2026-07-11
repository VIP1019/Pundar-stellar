package com.example.pundarapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.pundarapp.ui.theme.*

// ═══════════════════════════════════════════════════════════════
//  PUNDAR Futuristic 3D Icon System
//  Every icon: layered glow + gradient ring + depth shadow
//  Drop-in replacements for every emoji in the app
// ═══════════════════════════════════════════════════════════════

/**
 * Core 3D icon box — gradient ring, inner glow, depth shadow,
 * optional pulse animation. Used by every named icon below.
 */
@Composable
fun FuturisticIcon(
    icon       : ImageVector,
    tint       : Color,
    size       : Dp  = 44.dp,
    iconSize   : Dp  = 22.dp,
    shape      : androidx.compose.ui.graphics.Shape = RoundedCornerShape(14.dp),
    pulseGlow  : Boolean = false,
    modifier   : Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "iconGlow")
    val glowAlpha by if (pulseGlow) {
        infinite.animateFloat(
            initialValue  = 0.35f, targetValue = 0.85f,
            animationSpec = infiniteRepeatable(tween(1200, easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)), RepeatMode.Reverse),
            label         = "gAlpha"
        )
    } else remember { mutableStateOf(0.55f) }

    Box(
        modifier         = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // ── Bloom glow layer (behind) ─────────────────────────
        Box(
            Modifier
                .size(size)
                .graphicsLayer(alpha = glowAlpha)
                .blur(6.dp)
                .clip(shape)
                .background(tint.copy(alpha = 0.35f))
        )
        // ── Main icon surface ─────────────────────────────────
        Box(
            Modifier
                .size(size)
                .shadow(
                    elevation    = 8.dp,
                    shape        = shape,
                    ambientColor = tint.copy(0.4f),
                    spotColor    = tint.copy(0.4f)
                )
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(tint.copy(0.28f), tint.copy(0.08f))
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(listOf(tint.copy(0.7f), tint.copy(0.2f))),
                    shape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner depth highlight (top-left lighter corner)
            Box(
                Modifier
                    .size(size)
                    .background(
                        Brush.linearGradient(
                            listOf(Color.White.copy(0.10f), Color.Transparent)
                        )
                    )
            )
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = tint,
                modifier           = Modifier.size(iconSize)
            )
        }
    }
}

// ── Small circular 3D icon (for lists/rows) ──────────────────────
@Composable
fun FuturisticIconCircle(
    icon     : ImageVector,
    tint     : Color,
    size     : Dp = 40.dp,
    iconSize : Dp = 18.dp,
    modifier : Modifier = Modifier
) {
    FuturisticIcon(
        icon     = icon,
        tint     = tint,
        size     = size,
        iconSize = iconSize,
        shape    = CircleShape,
        modifier = modifier
    )
}

// ════════════════════════════════════════════════════════════════
//  Named 3D icons — replacing every emoji in the app
// ════════════════════════════════════════════════════════════════

/** Replaces ⭐ — star / score badge icon */
@Composable
fun Icon3DStar(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Icon(
        imageVector        = Icons.Filled.Star,
        contentDescription = "Score",
        tint               = PremiumGoldWarm,
        modifier           = modifier.size(size)
    )
}

/** Replaces 🔔 — notification / nudge bell */
@Composable
fun Icon3DBell(size: Dp = 40.dp, pulse: Boolean = false, modifier: Modifier = Modifier) {
    FuturisticIcon(
        icon      = Icons.Filled.NotificationsActive,
        tint      = PremiumGoldWarm,
        size      = size,
        iconSize  = (size.value * 0.5f).dp,
        shape     = CircleShape,
        pulseGlow = pulse,
        modifier  = modifier
    )
}

/** Replaces 🔒 — lock / escrow / smart contract */
@Composable
fun Icon3DLock(size: Dp = 44.dp, modifier: Modifier = Modifier) {
    FuturisticIcon(
        icon     = Icons.Filled.Lock,
        tint     = ElectricBlue,
        size     = size,
        iconSize = (size.value * 0.48f).dp,
        shape    = RoundedCornerShape(14.dp),
        modifier = modifier
    )
}

/** Replaces ⚠ — warning / error alert */
@Composable
fun Icon3DWarning(size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Icon(
        imageVector        = Icons.Filled.Warning,
        contentDescription = "Warning",
        tint               = ErrorRed,
        modifier           = modifier.size(size)
    )
}

/** Replaces 💳 — empty wallet state */
@Composable
fun Icon3DWallet(size: Dp = 56.dp, modifier: Modifier = Modifier) {
    FuturisticIcon(
        icon      = Icons.Filled.AccountBalanceWallet,
        tint      = ElectricBlue,
        size      = size,
        iconSize  = (size.value * 0.5f).dp,
        shape     = RoundedCornerShape(18.dp),
        pulseGlow = true,
        modifier  = modifier
    )
}

/** Replaces 🧾 — empty bills state */
@Composable
fun Icon3DReceipt(size: Dp = 56.dp, modifier: Modifier = Modifier) {
    FuturisticIcon(
        icon     = Icons.Filled.ReceiptLong,
        tint     = ElectricBlue,
        size     = size,
        iconSize = (size.value * 0.5f).dp,
        shape    = RoundedCornerShape(18.dp),
        modifier = modifier
    )
}

/** Replaces 🌀 — empty circle state */
@Composable
fun Icon3DCircleEmpty(size: Dp = 56.dp, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "spinEmpty")
    val rotation by infinite.animateFloat(
        initialValue  = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label         = "spinR"
    )
    FuturisticIcon(
        icon     = Icons.Filled.Cyclone,
        tint     = NeonCyan,
        size     = size,
        iconSize = (size.value * 0.5f).dp,
        shape    = CircleShape,
        modifier = modifier.graphicsLayer(rotationZ = rotation)
    )
}

/** Replaces 📈 — trending up / portfolio return */
@Composable
fun Icon3DTrending(size: Dp = 20.dp, positive: Boolean = true, modifier: Modifier = Modifier) {
    if (positive) {
        Icon(Icons.Filled.TrendingUp,
            contentDescription = "Up", tint = NeonGreen, modifier = modifier.size(size))
    } else {
        Icon(Icons.Filled.KeyboardArrowDown,
            contentDescription = "Down", tint = ErrorRed, modifier = modifier.size(size))
    }
}

/** Replaces 🚀 — launch / create button label icon */
@Composable
fun Icon3DRocket(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Icon(
        imageVector        = Icons.Filled.RocketLaunch,
        contentDescription = "Launch",
        tint               = PremiumGoldWarm,
        modifier           = modifier.size(size)
    )
}

/** Replaces 📋 — clipboard / circle info */
@Composable
fun Icon3DClipboard(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Icon(
        imageVector        = Icons.Filled.Assignment,
        contentDescription = "Details",
        tint               = ElectricBlue,
        modifier           = modifier.size(size)
    )
}

/** Replaces 🎯 — target */
@Composable
fun Icon3DTarget(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Icon(
        imageVector        = Icons.Filled.TrackChanges,
        contentDescription = "Target",
        tint               = NeonCyan,
        modifier           = modifier.size(size)
    )
}

/** Replaces 💰 — saved/money */
@Composable
fun Icon3DMoney(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Icon(
        imageVector        = Icons.Filled.Savings,
        contentDescription = "Saved",
        tint               = NeonGreen,
        modifier           = modifier.size(size)
    )
}

/** Replaces 👥 — members/group */
@Composable
fun Icon3DGroup(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Icon(
        imageVector        = Icons.Filled.Groups,
        contentDescription = "Members",
        tint               = ElectricBlue,
        modifier           = modifier.size(size)
    )
}

/** Replaces 📅 — date/calendar */
@Composable
fun Icon3DCalendar(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Icon(
        imageVector        = Icons.Filled.CalendarMonth,
        contentDescription = "Date",
        tint               = NeonCyan,
        modifier           = modifier.size(size)
    )
}

/** Replaces ✅ — settled/verified check */
@Composable
fun Icon3DCheckBadge(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Icon(
        imageVector        = Icons.Filled.Verified,
        contentDescription = "Settled",
        tint               = NeonGreen,
        modifier           = modifier.size(size)
    )
}

/** Replaces ⏳ — pending/clock */
@Composable
fun Icon3DClock(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Icon(
        imageVector        = Icons.Filled.AccessTime,
        contentDescription = "Pending",
        tint               = WarningAmber,
        modifier           = modifier.size(size)
    )
}
