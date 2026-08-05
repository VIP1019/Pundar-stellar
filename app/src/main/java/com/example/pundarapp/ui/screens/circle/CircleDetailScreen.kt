package com.example.pundarapp.ui.screens.circle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.pundarapp.ui.components.CurrencyAmountInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.ui.components.*
import kotlinx.coroutines.launch
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.ContributionStatus
import com.example.pundarapp.ui.theme.*

import com.example.pundarapp.ui.theme.PundarTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleDetailScreen(circleId: String, navController: NavController) {
    val circle = AppState.circles.find { it.id == circleId }
        ?: run { navController.navigateUp(); return }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUserId = AuthRepository.getCurrentUserPhone()
    val isOwner = circle.creatorId == currentUserId
    val isMember = circle.members.any { it.isYou }
    val isFull = circle.isFull
    val remainingSlots = circle.remainingSlots

    LaunchedEffect(circleId, isOwner) {
        if (isOwner) AppState.refreshJoinRequests(circleId)
    }

    var showContributeDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var contributionMultiplier by remember { mutableStateOf(1) }
    var contributeError by remember { mutableStateOf("") }

    // Contribute Dialog
    if (showContributeDialog) {
        AlertDialog(
            onDismissRequest = { showContributeDialog = false },
            containerColor = PundarTheme.colors.surfacePrimary,
            titleContentColor = PundarTheme.colors.textPrimary,
            textContentColor = PundarTheme.colors.textPrimary,
            title = { Text("Contribute: ${circle.name}", fontWeight = FontWeight.Bold) },
            text = {
                val totalAmount = circle.contributionPerMonth * contributionMultiplier
                val rate = AppState.currentExchangeRate.doubleValue
                val totalXlm = if (circle.currency == "XLM") totalAmount else (totalAmount / rate)
                Column {
                    Text(
                        "Wallet Balance: ${String.format("%,.2f", AppState.walletBalance.value)} XLM",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (AppState.walletBalance.value >= totalXlm) PundarSuccess else PundarWarning
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    Text("Select number of contributions:", style = MaterialTheme.typography.bodyMedium, color = PundarTheme.colors.textSecondary)
                    Spacer(Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { if (contributionMultiplier > 1) contributionMultiplier-- },
                            modifier = Modifier.background(PundarTheme.colors.surfaceTertiary, CircleShape)
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease", tint = PundarTheme.colors.textPrimary)
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$contributionMultiplier",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = PundarTheme.colors.textPrimary
                            )
                            Text(
                                if (contributionMultiplier == 1) "Contribution" else "Contributions",
                                style = MaterialTheme.typography.labelSmall,
                                color = PundarTheme.colors.textSecondary
                            )
                        }
                        
                        IconButton(
                            onClick = { contributionMultiplier++ },
                            modifier = Modifier.background(PundarTheme.colors.surfaceTertiary, CircleShape)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase", tint = PundarTheme.colors.textPrimary)
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = PundarTheme.colors.borderPrimary)
                    Spacer(Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total to pay", fontWeight = FontWeight.SemiBold, color = PundarTheme.colors.textPrimary)
                        val displayTotal = if (circle.currency == "XLM") "${String.format("%,.2f", totalAmount)} XLM" else "${circle.currency} ${String.format("%,.0f", totalAmount)}"
                        Text(displayTotal, fontWeight = FontWeight.Bold, color = PundarTheme.colors.brandPrimary)
                    }
                    
                    if (contributeError.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(contributeError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val totalAmount = circle.contributionPerMonth * contributionMultiplier
                        val rate = AppState.currentExchangeRate.doubleValue
                        val xlmAmt = if (circle.currency == "XLM") totalAmount else (if (rate > 0) totalAmount / rate else 0.0)
                        val circleCurrencyAmt = if (circle.currency == "XLM") xlmAmt else (xlmAmt * rate)
                        val success = AppState.contributeToCircle(circleId, circleCurrencyAmt, xlmAmt)
                        if (success) {
                            val displayAmt = if (circle.currency == "XLM") "${String.format("%,.2f", xlmAmt)} XLM" else "${circle.currency} ${String.format("%,.0f", circleCurrencyAmt)}"
                            Toast.makeText(context, "$displayAmt contributed!", Toast.LENGTH_SHORT).show()
                            showContributeDialog = false
                            contributionMultiplier = 1
                        } else {
                            contributeError = "Insufficient balance!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PundarTheme.colors.accentGold, contentColor = PundarTheme.colors.surfacePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Confirm", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showContributeDialog = false }) { Text("Cancel", color = PundarTheme.colors.textSecondary) }
            }
        )
    }

    // Share Dialog
    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            containerColor = PundarTheme.colors.surfacePrimary,
            titleContentColor = PundarTheme.colors.textPrimary,
            textContentColor = PundarTheme.colors.textPrimary,
            title = { Text("Share Circle Details", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Circle: ${circle.name}", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Target: ₱ ${String.format("%,.0f", circle.targetAmount)}", style = MaterialTheme.typography.bodyMedium)
                    Text("Saved: ₱ ${String.format("%,.0f", circle.savedAmount)}", style = MaterialTheme.typography.bodyMedium)
                    Text("Members: ${circle.memberCount} / ${circle.maxMembers}", style = MaterialTheme.typography.bodyMedium)
                    Text("Target Date: ${circle.targetDate}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Invite link: pundar.app/circle/${circle.id}", style = MaterialTheme.typography.bodySmall, color = PundarTheme.colors.brandPrimary)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showShareDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PundarTheme.colors.brandPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Copy Link", color = PundarTheme.colors.surfacePrimary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) { Text("Close", color = PundarTheme.colors.textSecondary) }
            }
        )
    }

    Scaffold(
        topBar = {
            PundarCircleTopBar(
                onBack = { navController.navigateUp() },
                userInitials = com.example.pundarapp.ui.data.SampleData.currentUser.initials
            )
        },
        containerColor = PundarTheme.colors.bgPrimary
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header Card ──────────────────────────────────────
            item {
                PundarAccentCard {
                    Row(modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge(
                                text = if (circle.isActive) "Active" else "Completed",
                                color = if (circle.isActive) PundarTheme.colors.brandPrimary else PundarSuccess
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = PundarTheme.colors.surfaceTertiary) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Group, null, tint = PundarTheme.colors.textSecondary, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${circle.memberCount} / ${circle.maxMembers} Members",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold, color = PundarTheme.colors.textSecondary)
                                }
                            }
                            if (isFull) {
                                Spacer(Modifier.width(8.dp))
                                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer) {
                                    Text("Circle Full",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            } else {
                                Spacer(Modifier.width(8.dp))
                                Text("$remainingSlots slots left",
                                    style = MaterialTheme.typography.labelSmall, color = PundarTheme.colors.brandPrimary)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(circle.name, style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = PundarTheme.colors.textPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text("Target: ${circle.targetDate}",
                                style = MaterialTheme.typography.bodyMedium, color = PundarTheme.colors.textSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Saved", style = MaterialTheme.typography.bodySmall, color = PundarTheme.colors.textSecondary)
                            val displaySaved = if (!circle.isActive) circle.targetAmount else circle.savedAmount
                            Text(if (circle.currency == "XLM") "${String.format("%,.2f", displaySaved)} XLM" else "${circle.currency} ${String.format("%,.0f", displaySaved)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = PundarTheme.colors.brandPrimary)
                            Text(if (circle.currency == "XLM") "of ${String.format("%,.2f", circle.targetAmount)} XLM" else "of ${circle.currency} ${String.format("%,.0f", circle.targetAmount)}",
                                style = MaterialTheme.typography.labelSmall, color = PundarTheme.colors.textSecondary)
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    val progress = if (!circle.isActive) 1f else (circle.savedAmount / circle.targetAmount).toFloat().coerceIn(0f, 1f)
                    val displaySaved = if (!circle.isActive) circle.targetAmount else circle.savedAmount
                    
                    PundarProgressBar(progress = progress, color = PundarTheme.colors.brandPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (!circle.isActive) "100% Completed" else "${(progress * 100).toInt()}% Completed",
                            style = MaterialTheme.typography.labelMedium, color = PundarTheme.colors.textSecondary)
                        if (circle.isActive) {
                            Text(if (circle.currency == "XLM") "${String.format("%,.2f", circle.targetAmount - circle.savedAmount)} XLM remaining" else "${circle.currency} ${String.format("%,.0f", circle.targetAmount - circle.savedAmount)} remaining",
                                style = MaterialTheme.typography.labelMedium, color = PundarTheme.colors.textSecondary)
                        } else {
                            Text("All payouts complete", style = MaterialTheme.typography.labelMedium, color = PundarSuccess)
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (isMember && circle.isActive) {
                            Button(
                                onClick = { showContributeDialog = true },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PundarTheme.colors.accentGold, contentColor = PundarTheme.colors.textPrimary)
                            ) {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Contribute", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(
                            onClick = { showShareDialog = true },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PundarTheme.colors.brandPrimary)
                        ) {
                            Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Share Details", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // ── Members ──────────────────────────────────────────
            item {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Member Contributions", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = PundarTheme.colors.textPrimary)
                    TextButton(onClick = { 
                        Toast.makeText(context, "No recent history found.", Toast.LENGTH_SHORT).show()
                    }) { Text("View History", color = PundarTheme.colors.brandPrimary) }
                }
            }

            item {
                PundarCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        circle.members.forEachIndexed { index, member ->
                            MemberListItem(
                                name = member.name,
                                initials = member.initials,
                                amount = member.amount,
                                sharePercent = member.sharePercent,
                                status = member.status,
                                isYou = member.isYou,
                                avatarColor = if (member.isYou) PundarTheme.colors.brandPrimary else androidx.compose.ui.graphics.Color(member.avatarColor),
                                payoutDate = member.payoutDate,
                                showNudge = member.status != ContributionStatus.PAID && !member.isYou,
                                isHighlighted = member.status != ContributionStatus.PAID && !member.isYou,
                                payoutOrder = index + 1
                            )
                            if (index < circle.members.size - 1) {
                                HorizontalDivider(color = PundarDivider, modifier = Modifier.padding(horizontal = 12.dp))
                            }
                        }
                    }
                }
            }

            // ── Escrow ───────────────────────────────────────────
            item { EscrowStatusCard(contractAddress = circle.escrowAddress, network = circle.escrowNetwork) }

            // ── Rules ────────────────────────────────────────────
            item {
                PundarCard {
                    Text("Circle Rules", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = PundarTheme.colors.textPrimary)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.CalendarToday, null, tint = PundarTheme.colors.brandPrimary,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Payout Frequency", style = MaterialTheme.typography.labelMedium,
                                color = PundarTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Every ${circle.payoutFrequencyValue} ${circle.payoutFrequencyUnit}",
                                style = MaterialTheme.typography.bodyMedium, color = PundarTheme.colors.textSecondary)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.Payments, null, tint = PundarTheme.colors.brandPrimary,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Contribution Amount", style = MaterialTheme.typography.labelMedium,
                                color = PundarTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
                            Text(if (circle.currency == "XLM") "${String.format("%,.2f", circle.contributionPerMonth)} XLM / member / month" else "${circle.currency} ${String.format("%,.0f", circle.contributionPerMonth)} / member / month",
                                style = MaterialTheme.typography.bodyMedium, color = PundarTheme.colors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}
