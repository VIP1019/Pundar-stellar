package com.example.pundarapp.ui.screens.pay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.BillStatus
import com.example.pundarapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailScreen(billId: String, navController: NavController) {
    val bill = AppState.bills.find { it.id == billId }
        ?: run { navController.navigateUp(); return }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = bill.name,
                onBack = { navController.navigateUp() },
                showMoreOptions = true
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
            // ── Summary Card ─────────────────────────────────────
            item {
                PundarAccentCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            StatusBadge(
                                text = when (bill.status) {
                                    BillStatus.SETTLED -> "Settled"
                                    BillStatus.PENDING -> "Pending"
                                    BillStatus.PARTIAL -> "Partial"
                                },
                                color = when (bill.status) {
                                    BillStatus.SETTLED -> PundarSuccess
                                    BillStatus.PENDING -> PundarWarning
                                    BillStatus.PARTIAL -> PundarInfo
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = bill.date,
                                style = MaterialTheme.typography.bodySmall,
                                color = PundarTextSecondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.bodySmall,
                                color = PundarTextSecondary
                            )
                            Text(
                                text = "₱ ${String.format("%,.2f", bill.totalAmount)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = PundarTextPrimary
                            )
                            Text(
                                text = "Your share: ₱ ${String.format("%,.2f", bill.yourShare)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PundarBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ── Members ──────────────────────────────────────────
            item {
                Text(
                    text = "Members (${bill.memberCount})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PundarTextPrimary
                )
            }

            if (bill.members.isNotEmpty()) {
                item {
                    PundarCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            bill.members.forEachIndexed { index, member ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (member.name == "You") PundarBlueSubtle
                                                else Color(member.avatarColor)
                                            )
                                    ) {
                                        Text(
                                            text = member.initials,
                                            color = if (member.name == "You") PundarBlue else Color.White,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = member.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (member.username.isNotEmpty()) {
                                            Text(
                                                text = member.username,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = PundarTextSecondary
                                            )
                                        }
                                    }
                                    Text(
                                        text = "₱ ${String.format("%,.2f", member.amount)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (index < bill.members.size - 1) {
                                    HorizontalDivider(color = PundarDivider)
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    PundarCard {
                        Text(
                            text = "${bill.memberCount} members split equally — ₱ ${String.format("%,.2f", bill.yourShare)} each",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PundarTextSecondary
                        )
                    }
                }
            }

            // ── Actions ──────────────────────────────────────────
            if (bill.status == BillStatus.PENDING || bill.status == BillStatus.PARTIAL) {
                item {
                    PundarPrimaryButton(
                        text = "Settle My Share — ₱ ${String.format("%,.2f", bill.yourShare)}",
                        onClick = { navController.navigateUp() }
                    )
                }
            }

            item {
                PundarSecondaryButton(
                    text = "Share Split Summary",
                    onClick = { }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
