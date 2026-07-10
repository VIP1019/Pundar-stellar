package com.example.pundarapp.ui.screens.pay

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.BillStatus
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.R
import com.example.pundarapp.data.remote.AuthRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayScreen(navController: NavController) {
    // Read from AppState — auto-recomposes when bills are added
    val bills = AppState.bills

    val settledCount = bills.count { it.status == BillStatus.SETTLED }
    val pendingCount = bills.count { it.status == BillStatus.PENDING || it.status == BillStatus.PARTIAL }
    val monthTotal = bills.sumOf { it.yourShare }

    val currentName = AuthRepository.getCurrentUserName()
    val initials = currentName.take(2).uppercase()

    Scaffold(
        topBar = {
            PundarMainTopBar(
                userName = currentName,
                userInitials = initials,
                pundarScore = com.example.pundarapp.ui.data.SampleData.currentUser.pundarScore,
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
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
                Text("New Group Bill", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = PundarBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("PUNDAR Pay", style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold, color = PundarTextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Intelligent, transparent settlement of shared expenses",
                    style = MaterialTheme.typography.bodyMedium, color = PundarTextSecondary)
            }

            // Summary card — redesigned with your custom PNG drawables
            item {
                PundarCard(accentColor = PundarBlue) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left side: Total Balance info
                        Column {
                            Text(
                                text = "TOTAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = PundarTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "₱${String.format("%,.2f", monthTotal)}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = PundarBlue
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "This Month",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = PundarTextPrimary
                            )
                        }

                        // Right side: Settled and Pending stats
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Settled Column Item
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.settled),
                                    contentDescription = "Settled",
                                    modifier = Modifier.size(40.dp)
                                )
                                Column {
                                    Text(
                                        text = "SETTLED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PundarTextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$settledCount ${if (settledCount == 1) "BILL" else "BILLS"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PundarSuccess
                                    )
                                    Text(
                                        text = "View All",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PundarBlue,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { /* Filter list to settled */ }
                                    )
                                }
                            }

                            // Vertical Divider
                            VerticalDivider(
                                modifier = Modifier
                                    .height(48.dp)
                                    .padding(horizontal = 4.dp),
                                color = Color.LightGray.copy(alpha = 0.5f),
                                thickness = 1.dp
                            )

                            // Pending Column Item
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.pending),
                                    contentDescription = "Pending",
                                    modifier = Modifier.size(40.dp)
                                )
                                Column {
                                    Text(
                                        text = "PENDINGS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PundarTextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$pendingCount ${if (pendingCount == 1) "BILL" else "BILLS"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PundarWarning
                                    )
                                    Text(
                                        text = "View All",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PundarBlue,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { /* Filter list to pending */ }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent splits header
            item {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent Splits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { }) { Text("View All", color = PundarBlue) }
                }
            }

            if (bills.isEmpty()) {
                item {
                    PundarCard {
                        Text(
                            "No bills yet. Tap \"New Group Bill\" to get started!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PundarTextSecondary
                        )
                    }
                }
            }

            // Bill items
            items(bills.toList()) { bill ->
                PundarCard(
                    modifier = Modifier.clickable {
                        navController.navigate(Routes.billDetail(bill.id))
                    }
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(
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
                            Text(bill.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                            Text("₱ ${String.format("%,.2f", bill.totalAmount)}",
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Your share: ₱ ${String.format("%,.2f", bill.yourShare)}",
                                style = MaterialTheme.typography.bodySmall, color = PundarTextSecondary)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Verified, null, tint = PundarBlue, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Settlement builds your PUNDAR Score.", style = MaterialTheme.typography.bodySmall, color = PundarTextSecondary)
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}