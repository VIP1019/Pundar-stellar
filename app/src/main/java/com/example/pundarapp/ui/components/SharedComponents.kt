package com.example.pundarapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pundarapp.ui.data.ContributionStatus
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme

// ── Progress bar ─────────────────────────────────────────────────
@Composable
fun PundarProgressBar(
    progress:        Float,
    modifier:        Modifier = Modifier,
    color:           Color    = Blue400,
    backgroundColor: Color    = PundarTheme.colors.surfaceTertiary,
    height:          Int      = 6
) {
    val anim by animateFloatAsState(
        targetValue   = progress.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)),
        label         = "prog"
    )
    val inf = rememberInfiniteTransition(label = "shimmer")
    val sx  by inf.animateFloat(
        initialValue  = -400f, targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "sx"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(height.dp))
            .background(backgroundColor)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(anim)
                .clip(RoundedCornerShape(height.dp))
                .background(Brush.horizontalGradient(listOf(color.copy(0.8f), color)))
        ) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(
                        listOf(Color.Transparent, PundarTheme.colors.surfacePrimary.copy(0.18f), Color.Transparent),
                        start = Offset(sx, 0f), end = Offset(sx + 180f, 0f)
                    )
                )
            )
        }
    }
}

@Composable
fun PundarAllocationBar(
    label:      String,
    percentage: Int,
    color:      Color,
    modifier:   Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = PundarTheme.colors.textSecondary)
            Text("$percentage%", style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(Modifier.height(6.dp))
        PundarProgressBar(progress = percentage / 100f, color = color, height = 5)
    }
}

// ── Member list item ──────────────────────────────────────────────
@Composable
fun MemberListItem(
    name:          String,
    initials:      String,
    amount:        Double,
    sharePercent:  Int,
    status:        ContributionStatus,
    isYou:         Boolean = false,
    avatarColor:   Color   = PundarTheme.colors.brandPrimary,
    showNudge:     Boolean = false,
    isHighlighted: Boolean = false,
    modifier:      Modifier = Modifier
) {
    val statusColor = when (status) {
        ContributionStatus.PAID    -> PundarTheme.colors.accentGreen
        ContributionStatus.PENDING -> PundarTheme.colors.accentOrange
        ContributionStatus.OVERDUE -> PundarTheme.colors.accentRed
    }
    val statusIcon = when (status) {
        ContributionStatus.PAID    -> Icons.Filled.CheckCircle
        ContributionStatus.PENDING -> Icons.Filled.Schedule
        ContributionStatus.OVERDUE -> Icons.Filled.Warning
    }
    val statusLabel = when (status) {
        ContributionStatus.PAID    -> "Paid"
        ContributionStatus.PENDING -> "Pending"
        ContributionStatus.OVERDUE -> "Overdue"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isHighlighted)
                    Brush.horizontalGradient(listOf(GoldBg, PundarTheme.colors.surfacePrimary))
                else
                    Brush.linearGradient(listOf(PundarTheme.colors.surfacePrimary, PundarTheme.colors.surfaceSecondary))
            )
            .border(1.dp, if (isHighlighted) PundarTheme.colors.accentGold.copy(0.3f) else PundarTheme.colors.glassSubtle, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(avatarColor, avatarColor.copy(0.6f))))
            ) {
                Text(initials, color = PundarTheme.colors.surfacePrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold, color = PundarTheme.colors.textPrimary)
                    if (isYou) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(6.dp))
                                .background(BlueGlowSoft)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("You", style = MaterialTheme.typography.labelSmall,
                                color = PundarTheme.colors.brandLight, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(statusLabel, style = MaterialTheme.typography.bodySmall, color = statusColor)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₱${String.format("%,.0f", amount)}",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PundarTheme.colors.textPrimary)
                Text("$sharePercent% share",
                    style = MaterialTheme.typography.bodySmall, color = PundarTheme.colors.textMuted)
            }
            if (showNudge) {
                Spacer(Modifier.width(8.dp))
                PundarSmallButton("Nudge", {}, containerColor = GoldBg, contentColor = Gold400)
            }
        }
    }
}

// ── Score chip ────────────────────────────────────────────────────
@Composable
fun PundarScoreChip(score: Int, modifier: Modifier = Modifier, label: String = "") {
    val (color, level) = when {
        score >= 800 -> PundarTheme.colors.accentGreen to "Excellent"
        score >= 650 -> Blue400  to "Good"
        score >= 500 -> PundarTheme.colors.accentOrange to "Fair"
        else         -> PundarTheme.colors.textMuted to "Building"
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(0.10f))
            .border(1.dp, color.copy(0.28f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon3DStar(size = 13.dp)
            Spacer(Modifier.width(5.dp))
            Text(
                text       = if (label.isNotEmpty()) label else "Score: $score · $level",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color      = color
            )
        }
    }
}

// ── Status badge ──────────────────────────────────────────────────
@Composable
fun StatusBadge(text: String, color: Color = PundarTheme.colors.accentGreen, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(0.12f))
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

// ── Escrow card ───────────────────────────────────────────────────
@Composable
fun EscrowStatusCard(contractAddress: String, network: String, modifier: Modifier = Modifier) {
    PundarCard(modifier = modifier, accentColor = Blue400) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon3DLock(size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Text("Soroban Escrow", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = PundarTheme.colors.textPrimary)
            Spacer(Modifier.weight(1f))
            StatusBadge("SECURED", PundarTheme.colors.accentGreen)
        }
        Spacer(Modifier.height(12.dp))
        Text("$contractAddress · $network", style = MaterialTheme.typography.bodySmall,
            color = PundarTheme.colors.textMuted, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        Spacer(Modifier.height(8.dp))
        Text(
            "Funds are cryptographically locked in a Soroban smart contract.",
            style = MaterialTheme.typography.bodySmall, color = PundarTheme.colors.textDim, lineHeight = 18.sp
        )
    }
}

// ── Section label ─────────────────────────────────────────────────
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text       = text,
        style      = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color      = PundarTheme.colors.textPrimary,
        modifier   = modifier
    )
}

// ── Skeleton shimmer ──────────────────────────────────────────────
@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "shim")
    val x   by inf.animateFloat(
        initialValue  = -600f, targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "x"
    )
    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(
            Brush.linearGradient(
                listOf(PundarTheme.colors.surfaceSecondary, PundarTheme.colors.surfaceTertiary, PundarTheme.colors.surfaceSecondary),
                start = Offset(x, 0f), end = Offset(x + 300f, 0f)
            )
        )
    )
}
