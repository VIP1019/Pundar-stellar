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

// ── Progress bar ─────────────────────────────────────────────────
@Composable
fun PundarProgressBar(
    progress:        Float,
    modifier:        Modifier = Modifier,
    color:           Color    = Blue400,
    backgroundColor: Color    = Navy600,
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
                        listOf(Color.Transparent, White.copy(0.18f), Color.Transparent),
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
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSoft)
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
    avatarColor:   Color   = Blue500,
    showNudge:     Boolean = false,
    isHighlighted: Boolean = false,
    modifier:      Modifier = Modifier
) {
    val statusColor = when (status) {
        ContributionStatus.PAID    -> Green400
        ContributionStatus.PENDING -> Orange500
        ContributionStatus.OVERDUE -> Red500
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
                    Brush.horizontalGradient(listOf(GoldBg, Navy800))
                else
                    Brush.linearGradient(listOf(Navy800, Navy700))
            )
            .border(1.dp, if (isHighlighted) Gold500.copy(0.3f) else Glass10, RoundedCornerShape(14.dp))
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
                Text(initials, color = White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold, color = TextWhite)
                    if (isYou) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(6.dp))
                                .background(BlueGlowSoft)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("You", style = MaterialTheme.typography.labelSmall,
                                color = Blue300, fontWeight = FontWeight.Bold)
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
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextWhite)
                Text("$sharePercent% share",
                    style = MaterialTheme.typography.bodySmall, color = TextMuted)
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
        score >= 800 -> Green400 to "Excellent"
        score >= 650 -> Blue400  to "Good"
        score >= 500 -> Orange500 to "Fair"
        else         -> TextMuted to "Building"
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
fun StatusBadge(text: String, color: Color = Green400, modifier: Modifier = Modifier) {
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
                fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(Modifier.weight(1f))
            StatusBadge("SECURED", Green400)
        }
        Spacer(Modifier.height(12.dp))
        Text("$contractAddress · $network", style = MaterialTheme.typography.bodySmall,
            color = TextMuted, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        Spacer(Modifier.height(8.dp))
        Text(
            "Funds are cryptographically locked in a Soroban smart contract.",
            style = MaterialTheme.typography.bodySmall, color = TextDim, lineHeight = 18.sp
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
        color      = TextWhite,
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
                listOf(Navy700, Navy600, Navy700),
                start = Offset(x, 0f), end = Offset(x + 300f, 0f)
            )
        )
    )
}
