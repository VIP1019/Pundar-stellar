package com.example.pundarapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pundarapp.ui.data.ContributionStatus
import com.example.pundarapp.ui.theme.*

// ── Progress Bar ────────────────────────────────────────────────

@Composable
fun PundarProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = PundarBlue,
    backgroundColor: Color = PundarSurfaceVariant,
    height: Int = 8
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(height.dp / 2))
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(height.dp / 2))
                .background(color)
        )
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
                color = PundarTextPrimary
            )
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = PundarTextPrimary
            )
        }
        Spacer(Modifier.height(4.dp))
        PundarProgressBar(
            progress = percentage / 100f,
            color = color,
            height = 6
        )
    }
}

// ── Member List Item ────────────────────────────────────────────

@Composable
fun MemberListItem(
    name: String,
    initials: String,
    amount: Double,
    sharePercent: Int,
    status: ContributionStatus,
    isYou: Boolean = false,
    avatarColor: Color = PundarTextSecondary,
    showNudge: Boolean = false,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isHighlighted) PundarYellowBg else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarColor)
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            // Name and status
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = PundarTextPrimary
                    )
                    if (isYou) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "(You)",
                            style = MaterialTheme.typography.bodySmall,
                            color = PundarTextSecondary
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (status) {
                            ContributionStatus.PAID -> Icons.Filled.Check
                            ContributionStatus.PENDING -> Icons.Filled.Schedule
                            ContributionStatus.OVERDUE -> Icons.Filled.Warning
                        },
                        contentDescription = null,
                        tint = when (status) {
                            ContributionStatus.PAID -> PundarSuccess
                            ContributionStatus.PENDING -> PundarWarning
                            ContributionStatus.OVERDUE -> PundarError
                        },
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = when (status) {
                            ContributionStatus.PAID -> "Paid this month"
                            ContributionStatus.PENDING -> "Pending (Due Today)"
                            ContributionStatus.OVERDUE -> "Overdue"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (status) {
                            ContributionStatus.PAID -> PundarSuccess
                            ContributionStatus.PENDING -> PundarWarning
                            ContributionStatus.OVERDUE -> PundarError
                        }
                    )
                }
            }

            // Amount and share
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₱ ${String.format("%,.0f", amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PundarTextPrimary
                )
                Text(
                    text = "$sharePercent% Share",
                    style = MaterialTheme.typography.bodySmall,
                    color = PundarTextSecondary
                )
            }

            if (showNudge) {
                Spacer(Modifier.width(8.dp))
                PundarSmallButton(
                    text = "🔔 Nudge",
                    onClick = { },
                    containerColor = PundarGoldLight,
                    contentColor = PundarTextPrimary
                )
            }
        }
    }
}

// ── Score Chip ──────────────────────────────────────────────────

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
        else -> "Building"
    }
    val color = when {
        score >= 800 -> PundarSuccess
        score >= 650 -> PundarBlue
        score >= 500 -> PundarWarning
        else -> PundarTextSecondary
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(text = "⭐", fontSize = 14.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (label.isNotEmpty()) label else "PUNDAR Score: $score ($level)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

// ── Status Badge ────────────────────────────────────────────────

@Composable
fun StatusBadge(
    text: String,
    color: Color = PundarSuccess,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// ── Escrow Card ─────────────────────────────────────────────────

@Composable
fun EscrowStatusCard(
    contractAddress: String,
    network: String,
    modifier: Modifier = Modifier
) {
    PundarCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🔒", fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Soroban Escrow Status",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Contract State",
                    style = MaterialTheme.typography.bodySmall,
                    color = PundarTextSecondary
                )
                Text(
                    text = "$contractAddress ($network)",
                    style = MaterialTheme.typography.bodySmall,
                    color = PundarTextSecondary,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            StatusBadge("SECURED", PundarSuccess)
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Funds are cryptographically locked in a Soroban smart contract. They can only be released when the target amount is met and consensus is reached by members.",
            style = MaterialTheme.typography.bodySmall,
            color = PundarTextSecondary,
            lineHeight = 18.sp
        )
    }
}
