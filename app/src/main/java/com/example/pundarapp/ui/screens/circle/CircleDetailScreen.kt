package com.example.pundarapp.ui.screens.circle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.SampleData
import com.example.pundarapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleDetailScreen(circleId: String, navController: NavController) {
    // In a real app we'd fetch based on circleId
    val circle = SampleData.circles.find { it.id == circleId } ?: SampleData.circles.first()

    Scaffold(
        topBar = {
            PundarCircleTopBar(
                onBack = { navController.navigateUp() },
                userInitials = SampleData.currentUser.initials
            )
        },
        containerColor = PundarBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header Card ──────────────────────────────────────
            item {
                PundarAccentCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge(
                                text = if (circle.isActive) "Active" else "Completed",
                                color = if (circle.isActive) PundarBlue else PundarSuccess
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PundarSurfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "👥", fontSize = 12.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "${circle.memberCount} Members",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PundarTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = circle.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = PundarTextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Target: ${circle.targetDate}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PundarTextSecondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Saved",
                                style = MaterialTheme.typography.bodySmall,
                                color = PundarTextSecondary
                            )
                            Text(
                                text = "₱",
                                style = MaterialTheme.typography.titleMedium,
                                color = PundarBlue
                            )
                            Text(
                                text = String.format("%,.0f", circle.savedAmount),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = PundarBlue
                            )
                            Text(
                                text = "of ₱ ${String.format("%,.0f", circle.targetAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PundarTextSecondary
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    val progress = (circle.savedAmount / circle.targetAmount).toFloat()
                    val progressPercent = (progress * 100).toInt()

                    PundarProgressBar(
                        progress = progress,
                        color = PundarBlue
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$progressPercent% Completed",
                            style = MaterialTheme.typography.labelMedium,
                            color = PundarTextSecondary
                        )
                        Text(
                            text = "₱ ${String.format("%,.0f", circle.targetAmount - circle.savedAmount)} remaining",
                            style = MaterialTheme.typography.labelMedium,
                            color = PundarTextSecondary
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PundarGold,
                                contentColor = PundarTextPrimary
                            )
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Contribute",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        OutlinedButton(
                            onClick = { },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = null, // No border for this style, just icon and text
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PundarBlue
                            )
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Share Details",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ── Contributions ────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Member Contributions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PundarTextPrimary
                    )
                    TextButton(onClick = { }) {
                        Text("View History", color = PundarBlue)
                    }
                }
            }

            item {
                PundarCard(modifier = Modifier.padding(bottom = 0.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        circle.members.forEachIndexed { index, member ->
                            MemberListItem(
                                name = member.name,
                                initials = member.initials,
                                amount = member.amount,
                                sharePercent = member.sharePercent,
                                status = member.status,
                                isYou = member.isYou,
                                avatarColor = if (member.isYou) PundarBlue else Color(member.avatarColor),
                                showNudge = member.status != com.example.pundarapp.ui.data.ContributionStatus.PAID && !member.isYou,
                                isHighlighted = member.status != com.example.pundarapp.ui.data.ContributionStatus.PAID && !member.isYou
                            )
                            if (index < circle.members.size - 1) {
                                HorizontalDivider(color = PundarDivider, modifier = Modifier.padding(horizontal = 12.dp))
                            }
                        }
                    }
                }
            }

            // ── Escrow Status ────────────────────────────────────
            item {
                EscrowStatusCard(
                    contractAddress = circle.escrowAddress,
                    network = circle.escrowNetwork
                )
            }

            // ── Circle Rules ─────────────────────────────────────
            item {
                PundarCard {
                    Text(
                        text = "Circle Rules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PundarTextPrimary
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = PundarBlue,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Monthly Due Date",
                                style = MaterialTheme.typography.labelMedium,
                                color = PundarTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${circle.monthlyDueDay}th of every month",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PundarTextSecondary
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Filled.Payments,
                            contentDescription = null,
                            tint = PundarBlue,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Contribution Amount",
                                style = MaterialTheme.typography.labelMedium,
                                color = PundarTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "₱ ${String.format("%,.0f", circle.contributionPerMonth)} / member / month",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PundarTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
