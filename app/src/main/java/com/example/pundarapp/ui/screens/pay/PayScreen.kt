package com.example.pundarapp.ui.screens.pay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.BillStatus
import com.example.pundarapp.ui.data.SampleData
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayScreen(navController: NavController) {
    Scaffold(
        topBar = {
            PundarMainTopBar(
                userInitials = SampleData.currentUser.initials,
                pundarScore = SampleData.currentUser.pundarScore
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Routes.PAY_NEW_BILL) },
                containerColor = PundarGold,
                contentColor = PundarTextPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Bill")
                Spacer(Modifier.width(8.dp))
                Text(
                    "New Group Bill",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = PundarBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "PUNDAR Pay",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = PundarTextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Intelligent, transparent settlement of shared expenses",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PundarTextSecondary
                )
            }

            // Summary card
            item {
                PundarCard(accentColor = PundarBlue) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PaySummaryItem("This Month", "₱ 4,826.67", PundarTextPrimary)
                        PaySummaryItem("Settled", "5 bills", PundarSuccess)
                        PaySummaryItem("Pending", "1 bill", PundarWarning)
                    }
                }
            }

            // Recent splits header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Splits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { }) {
                        Text("View All", color = PundarBlue)
                    }
                }
            }

            // Bill items
            items(SampleData.recentBills) { bill ->
                PundarCard(
                    modifier = Modifier.clickable { }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status icon
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    when (bill.status) {
                                        BillStatus.SETTLED -> PundarSuccessLight
                                        BillStatus.PENDING -> PundarWarningLight
                                        BillStatus.PARTIAL -> PundarInfoLight
                                    }
                                )
                        ) {
                            Icon(
                                imageVector = when (bill.status) {
                                    BillStatus.SETTLED -> Icons.Filled.CheckCircle
                                    BillStatus.PENDING -> Icons.Filled.Schedule
                                    BillStatus.PARTIAL -> Icons.Filled.PieChart
                                },
                                contentDescription = null,
                                tint = when (bill.status) {
                                    BillStatus.SETTLED -> PundarSuccess
                                    BillStatus.PENDING -> PundarWarning
                                    BillStatus.PARTIAL -> PundarInfo
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bill.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = when (bill.status) {
                                        BillStatus.SETTLED -> "Settled"
                                        BillStatus.PENDING -> "Pending"
                                        BillStatus.PARTIAL -> "Partial"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when (bill.status) {
                                        BillStatus.SETTLED -> PundarSuccess
                                        BillStatus.PENDING -> PundarWarning
                                        BillStatus.PARTIAL -> PundarInfo
                                    }
                                )
                                Text(
                                    text = " • ${bill.memberCount} members • ${bill.date}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PundarTextSecondary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "₱ ${String.format("%,.2f", bill.totalAmount)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Your share: ₱ ${String.format("%,.2f", bill.yourShare)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PundarTextSecondary
                            )
                        }
                    }
                }
            }

            // PUNDAR Score note
            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Verified,
                        contentDescription = null,
                        tint = PundarBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Settlement builds your PUNDAR Score.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PundarTextSecondary
                    )
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun PaySummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = PundarTextSecondary
        )
    }
}
