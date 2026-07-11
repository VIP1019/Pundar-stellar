package com.example.pundarapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pundarapp.ui.data.ContributionStatus
import com.example.pundarapp.ui.theme.*

// ── Futuristic Progress Bar ──────────────────────────────────────

@Composable
fun PundarProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = ElectricBlue,
    backgroundColor: Color = SpaceMedium,
    height: Int = 8
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = EaseOutCubic),
        label = "progress"
    )

    // Shimmer sweep on the fill
    val infiniteTransition = rememberInfiniteTransition(label = "barShimmer")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -300f, targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "shimmerX"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(height.dp / 2))
            .background(backgroundColor)
    ) {
        // Filled portion
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(height.dp / 2))
                .background(
                    Brush.linearGradient(
                        colors = listOf(color, color.copy(alpha = 0.7f), color)
                    )
                )
        ) {
            // Shimmer sweep overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.20f),
                                Color.Transparent
                            ),
                            start = Offset(shimmerX, 0f),
                            end   = Offset(shimmerX + 200f, 0f)
                        )
                    )
            )
        }

        // Glow tip dot
        if (animatedProgress > 0.02f) {
            Box(
                modifier = Modifier
                    .size((height + 4).dp)
                    .align(Alignment.CenterStart)
                    .offset(x = ((animatedProgress * 1f) - 0.01f).coerceAtLeast(0f).dp * 0)
                    .fillMaxWidth(animatedProgress)
                    .wrapContentWidth(Alignment.End)
                    .clip(CircleShape)
                    .background(color)
                    .shadow(4.dp, CircleShape, ambientColor = color.copy(0.6f), spotColor = color.copy(0.6f))
            )
        }
    }
}

@Composable
fun PundarAllocationBar(
    label: String,
    percentage: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(Modifier.height(6.dp))
        PundarProgressBar(progress = percentage / 100f, color = color, height = 6)
    }
}

// ── Member List Item ─────────────────────────────────────────────

@Composable
fun MemberListItem(
    name: String,
    initials: String,
    amount: Double,
    sharePercent: Int,
    status: ContributionStatus,
    isYou: Boolean = false,
    avatarColor: Color = ElectricBlueDim,
    showNudge: Boolean = false,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val highlightBrush = if (isHighlighted)
        Brush.horizontalGradient(listOf(GoldGlow, Color.Transparent))
    else
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))

    val statusColor = when (status) {
        ContributionStatus.PAID    -> NeonGreen
        ContributionStatus.PENDING -> WarningAmber
        ContributionStatus.OVERDUE -> ErrorRed
    }
    val statusIcon = when (status) {
        ContributionStatus.PAID    -> Icons.Filled.Check
        ContributionStatus.PENDING -> Icons.Filled.Schedule
        ContributionStatus.OVERDUE -> Icons.Filled.Warning
    }
    val statusLabel = when (status) {
        ContributionStatus.PAID    -> "Paid this month"
        ContributionStatus.PENDING -> "Pending (Due Today)"
        ContributionStatus.OVERDUE -> "Overdue"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(highlightBrush)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(GlassBorder, Color.Transparent)
                ),
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with gradient ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(avatarColor, avatarColor.copy(0.6f)))
                    )
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    if (isYou) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ElectricBlue.copy(0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "You",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₱ ${String.format("%,.0f", amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "$sharePercent% Share",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (showNudge) {
                Spacer(Modifier.width(8.dp))
                PundarSmallButton(
                    text = "Nudge",
                    onClick = {},
                    containerColor = GoldGlow,
                    contentColor   = PremiumGoldWarm
                )
            }
        }
    }
}

// ── Score Chip ───────────────────────────────────────────────────

@Composable
fun PundarScoreChip(
    score: Int,
    modifier: Modifier = Modifier,
    label: String = ""
) {
    val level = when {
        score >= 800 -> "Excellent"
        score >= 650 -> "Good"
        score >= 500 -> "Fair"
        else         -> "Building"
    }
    val color = when {
        score >= 800 -> NeonGreen
        score >= 650 -> ElectricBlue
        score >= 500 -> WarningAmber
        else         -> TextSecondary
    }

    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(10.dp), ambientColor = color.copy(0.3f), spotColor = color.copy(0.3f))
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon3DStar(size = 14.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (label.isNotEmpty()) label else "Score: $score · $level",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// ── Status Badge ─────────────────────────────────────────────────

@Composable
fun StatusBadge(
    text: String,
    color: Color = NeonGreen,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ── Escrow Card ──────────────────────────────────────────────────

@Composable
fun EscrowStatusCard(
    contractAddress: String,
    network: String,
    modifier: Modifier = Modifier
) {
    PundarCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon3DLock(size = 36.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Soroban Escrow",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.weight(1f))
            StatusBadge("SECURED", NeonGreen)
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "$contractAddress · $network",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Funds are cryptographically locked in a Soroban smart contract and can only be released when the target amount is met and all members reach consensus.",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            lineHeight = 18.sp
        )
    }
}

private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
