package com.example.pundarapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme

/**
 * Core icon container — consistent rounded square with tinted bg + border.
 * Used throughout the app for a unified icon language.
 */
@Composable
fun FuturisticIcon(
    icon:      ImageVector,
    tint:      Color,
    size:      Dp      = 44.dp,
    iconSize:  Dp      = 22.dp,
    shape:     androidx.compose.ui.graphics.Shape = RoundedCornerShape(14.dp),
    pulseGlow: Boolean = false,
    modifier:  Modifier = Modifier
) {
    val inf       = rememberInfiniteTransition(label = "iconPulse")
    val glowAlpha by if (pulseGlow) {
        inf.animateFloat(
            initialValue  = 0.3f, targetValue = 0.7f,
            animationSpec = infiniteRepeatable(tween(1200, easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)), RepeatMode.Reverse),
            label         = "ga"
        )
    } else remember { mutableStateOf(0.5f) }

    Box(
        modifier         = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(Brush.linearGradient(listOf(tint.copy(0.16f), tint.copy(0.08f))))
                .border(1.dp, Brush.linearGradient(listOf(tint.copy(glowAlpha), tint.copy(0.12f))), shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = tint,
                modifier           = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun FuturisticIconCircle(
    icon:     ImageVector,
    tint:     Color,
    size:     Dp = 40.dp,
    iconSize: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    FuturisticIcon(icon = icon, tint = tint, size = size, iconSize = iconSize,
        shape = CircleShape, modifier = modifier)
}

// ── Named icons ───────────────────────────────────────────────────

@Composable
fun Icon3DStar(size: Dp = 16.dp, modifier: Modifier = Modifier) {
    Icon(Icons.Filled.Star, "Score", tint = PundarTheme.colors.accentGold, modifier = modifier.size(size))
}

@Composable
fun Icon3DBell(size: Dp = 40.dp, pulse: Boolean = false, modifier: Modifier = Modifier) {
    FuturisticIcon(Icons.Filled.NotificationsActive, PundarTheme.colors.accentGold, size, (size.value * 0.5f).dp,
        CircleShape, pulse, modifier)
}

@Composable
fun Icon3DLock(size: Dp = 44.dp, modifier: Modifier = Modifier) {
    FuturisticIcon(Icons.Filled.Lock, Blue400, size, (size.value * 0.48f).dp,
        RoundedCornerShape(14.dp), modifier = modifier)
}

@Composable
fun Icon3DWarning(size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Icon(Icons.Filled.Warning, "Warning", tint = PundarTheme.colors.accentRed, modifier = modifier.size(size))
}

@Composable
fun Icon3DWallet(size: Dp = 56.dp, modifier: Modifier = Modifier) {
    FuturisticIcon(Icons.Filled.AccountBalanceWallet, Blue400, size, (size.value * 0.5f).dp,
        RoundedCornerShape(18.dp), pulseGlow = true, modifier = modifier)
}

@Composable
fun Icon3DReceipt(size: Dp = 56.dp, modifier: Modifier = Modifier) {
    FuturisticIcon(Icons.Filled.ReceiptLong, Blue400, size, (size.value * 0.5f).dp,
        RoundedCornerShape(18.dp), modifier = modifier)
}

@Composable
fun Icon3DCircleEmpty(size: Dp = 56.dp, modifier: Modifier = Modifier) {
    val inf      = rememberInfiniteTransition(label = "spin")
    val rotation by inf.animateFloat(0f, 360f,
        infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "r")
    FuturisticIcon(Icons.Filled.Refresh, PundarTheme.colors.brandLight, size, (size.value * 0.5f).dp,
        CircleShape, modifier = modifier.graphicsLayer(rotationZ = rotation))
}

@Composable
fun Icon3DTrending(size: Dp = 20.dp, positive: Boolean = true, modifier: Modifier = Modifier) {
    Icon(
        if (positive) Icons.Filled.TrendingUp else Icons.Filled.KeyboardArrowDown,
        if (positive) "Up" else "Down",
        tint     = if (positive) PundarTheme.colors.accentGreen else PundarTheme.colors.accentRed,
        modifier = modifier.size(size)
    )
}

@Composable
fun Icon3DRocket(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Icon(Icons.Filled.Send, "Launch", tint = PundarTheme.colors.accentGold, modifier = modifier.size(size))
}

@Composable
fun Icon3DClipboard(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Icon(Icons.Filled.Assignment, "Details", tint = Blue400, modifier = modifier.size(size))
}

@Composable
fun Icon3DTarget(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Icon(Icons.Filled.GpsFixed, "Target", tint = PundarTheme.colors.accentElectric, modifier = modifier.size(size))
}

@Composable
fun Icon3DMoney(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Icon(Icons.Filled.Savings, "Saved", tint = PundarTheme.colors.accentGreen, modifier = modifier.size(size))
}

@Composable
fun Icon3DGroup(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Icon(Icons.Filled.Groups, "Members", tint = Blue400, modifier = modifier.size(size))
}

@Composable
fun Icon3DCalendar(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Icon(Icons.Filled.DateRange, "Date", tint = PundarTheme.colors.accentElectric, modifier = modifier.size(size))
}

@Composable
fun Icon3DCheckBadge(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Icon(Icons.Filled.Verified, "Settled", tint = PundarTheme.colors.accentGreen, modifier = modifier.size(size))
}

@Composable
fun Icon3DClock(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Icon(Icons.Filled.AccessTime, "Pending", tint = PundarTheme.colors.accentOrange, modifier = modifier.size(size))
}
