package com.example.pundarapp.ui.screens.grow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.ActivityType
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.SampleData
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.data.remote.AuthRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowScreen(navController: NavController) {
    val user = SampleData.currentUser
    val portfolio by AppState.portfolio

    var showInvestDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showOptimizeDialog by remember { mutableStateOf(false) }
    var dialogAmount by remember { mutableStateOf("") }

    // Invest Dialog
    if (showInvestDialog) {
        AlertDialog(
            onDismissRequest = { showInvestDialog = false; dialogAmount = "" },
            title = { Text("Invest More", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("How much would you like to invest?",
                        style = MaterialTheme.typography.bodyMedium, color = PundarTextSecondary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = dialogAmount,
                        onValueChange = { dialogAmount = it },
                        label = { Text("Amount (₱)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PundarBlue, focusedLabelColor = PundarBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("💡 Funds will be auto-allocated per your current portfolio weights.",
                        style = MaterialTheme.typography.bodySmall, color = PundarTextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = dialogAmount.toDoubleOrNull() ?: 0.0
                        if (amount > 0) AppState.invest(amount)
                        showInvestDialog = false; dialogAmount = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PundarBlue),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Invest Now", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showInvestDialog = false; dialogAmount = "" }) { Text("Cancel") } }
        )
    }

    // Withdraw Dialog
    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false; dialogAmount = "" },
            title = { Text("Withdraw Funds", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Available: ₱ ${String.format("%,.2f", portfolio.totalValue)}",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = dialogAmount,
                        onValueChange = { dialogAmount = it },
                        label = { Text("Amount (₱)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PundarBlue, focusedLabelColor = PundarBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("⚠️ Withdrawal may take 1–3 business days to reflect in your account.",
                        style = MaterialTheme.typography.bodySmall, color = PundarWarning)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = dialogAmount.toDoubleOrNull() ?: 0.0
                        if (amount > 0 && amount <= portfolio.totalValue) AppState.withdraw(amount)
                        showWithdrawDialog = false; dialogAmount = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PundarError),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Withdraw", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showWithdrawDialog = false; dialogAmount = "" }) { Text("Cancel") } }
        )
    }

    // Optimize Dialog
    if (showOptimizeDialog) {
        AlertDialog(
            onDismissRequest = { showOptimizeDialog = false },
            title = { Text("Optimize Portfolio", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Based on your PUNDAR Score of ${user.pundarScore}, here's our recommendation:",
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    PundarAllocationBar("PH Equities", 80, PundarBlue)
                    PundarAllocationBar("Fixed Income", 20, PundarTextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text("This allocation is optimized for moderate growth with your current risk profile.",
                        style = MaterialTheme.typography.bodySmall, color = PundarTextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showOptimizeDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PundarGold, contentColor = PundarTextPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Apply Optimization", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showOptimizeDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            PundarGrowTopBar(
                userInitials = AuthRepository.getCurrentUserInitials(),
                pundarScore = user.pundarScore,
                onNotificationClick = { navController.navigate(Routes.NOTIFICATIONS) }
            )
        },
        containerColor = PundarBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Your Portfolio", style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold, color = PundarBlueDark)
                    PundarSmallButton(
                        text = "Optimize",
                        onClick = { showOptimizeDialog = true },
                        containerColor = PundarGold,
                        contentColor = PundarTextPrimary
                    )
                }
            }

            // Main Chart Card — professional yellow-bordered design
            item {
                PundarAccentCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Value section
                        Column(modifier = Modifier.padding(bottom = 16.dp)) {
                            Text(
                                text = "Total Assets Value",
                                style = MaterialTheme.typography.labelMedium,
                                color = PundarTextSecondary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "₱ ${String.format("%,.2f", portfolio.totalValue)}",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = PundarTextPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            StatusBadge(
                                text = "📈 +${portfolio.totalReturnPercent}% (₱ ${String.format("%,.0f", portfolio.totalReturnAmount)}) All time",
                                color = PundarGoldDark
                            )
                        }

                        // Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showInvestDialog = true },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PundarBlue)
                            ) {
                                Text("Invest More", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { showWithdrawDialog = true },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PundarTextPrimary)
                            ) {
                                Text("Withdraw", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Chart
                        PundarLineChart(
                            dataPoints = SampleData.portfolioChartData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
            }

            // Allocation
            item {
                Text("Allocation", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = PundarTextPrimary)
            }
            item {
                PundarCard {
                    PundarAllocationBar("PH Equities", portfolio.phEquitiesPercent, PundarBlue)
                    Spacer(Modifier.height(16.dp))
                    PundarAllocationBar("Fixed Income", portfolio.fixedIncomePercent, PundarTextSecondary)
                }
            }

            // Activity
            item {
                Text("Activity", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = PundarTextPrimary)
            }
            item {
                PundarCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        portfolio.activities.take(2).forEachIndexed { index, activity ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(
                                        when (activity.type) {
                                            ActivityType.AUTO_SWEEP -> PundarBlueSubtle
                                            ActivityType.DIVIDEND -> PundarGoldLight
                                            else -> PundarSurfaceVariant
                                        }
                                    )
                                ) {
                                    Icon(
                                        imageVector = when (activity.type) {
                                            ActivityType.AUTO_SWEEP -> Icons.Filled.Autorenew
                                            ActivityType.DIVIDEND -> Icons.Filled.LocalAtm
                                            else -> Icons.Filled.Tune
                                        },
                                        contentDescription = null,
                                        tint = when (activity.type) {
                                            ActivityType.AUTO_SWEEP -> PundarBlue
                                            ActivityType.DIVIDEND -> PundarGoldDark
                                            else -> PundarTextSecondary
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(activity.description, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(activity.date, style = MaterialTheme.typography.bodySmall, color = PundarTextSecondary)
                                }
                                Text(
                                    text = "${if (activity.isPositive) "+" else "-"}₱ ${String.format("%,.2f", activity.amount)}",
                                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                                    color = if (activity.type == ActivityType.DIVIDEND) PundarGoldDark else PundarBlue
                                )
                            }
                            if (index == 0) HorizontalDivider(color = PundarDivider)
                        }
                    }
                }
            }

            // Holdings
            item {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Holdings", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = PundarTextPrimary)
                    TextButton(onClick = { }) { Text("View All", color = PundarBlue) }
                }
            }
            item {
                PundarCard {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Asset", style = MaterialTheme.typography.labelSmall, color = PundarTextSecondary, modifier = Modifier.weight(1.5f))
                        Text("Shares", style = MaterialTheme.typography.labelSmall, color = PundarTextSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text("Value", style = MaterialTheme.typography.labelSmall, color = PundarTextSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text("Return", style = MaterialTheme.typography.labelSmall, color = PundarTextSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = PundarDivider)
                    Column {
                        portfolio.holdings.forEachIndexed { index, holding ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { navController.navigate(Routes.stockDetail(holding.ticker)) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.5f)) {
                                    Box(contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(36.dp).clip(CircleShape).background(PundarSurfaceVariant)) {
                                        Text(holding.ticker.take(2), style = MaterialTheme.typography.labelMedium, color = PundarBlue)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(holding.companyName, style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text(holding.ticker, style = MaterialTheme.typography.labelSmall, color = PundarTextSecondary)
                                    }
                                }
                                Text("${holding.shares}", style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                Text("₱ ${String.format("%,.0f", holding.value)}", style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                Text(
                                    text = "${if (holding.returnPercent >= 0) "+" else ""}${holding.returnPercent}%",
                                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                                    color = if (holding.returnPercent >= 0) PundarSuccess else PundarError,
                                    modifier = Modifier.weight(1f), textAlign = TextAlign.End
                                )
                            }
                            if (index < portfolio.holdings.size - 1) HorizontalDivider(color = PundarDivider)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
