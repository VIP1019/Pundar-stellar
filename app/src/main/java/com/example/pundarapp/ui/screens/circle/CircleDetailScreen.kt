package com.example.pundarapp.ui.screens.circle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.ContributionStatus
import com.example.pundarapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleDetailScreen(circleId: String, navController: NavController) {
    val circle = AppState.circles.find { it.id == circleId }
        ?: run { navController.navigateUp(); return }
    val context = LocalContext.current
    val isMember = circle.members.any { it.isYou }

    var showContributeDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var contributeAmount by remember { mutableStateOf("") }

    // Contribute Dialog
    if (showContributeDialog) {
        AlertDialog(
            onDismissRequest = { showContributeDialog = false },
            title = { Text("Contribute to ${circle.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter the amount to contribute:", style = MaterialTheme.typography.bodyMedium, color = PundarTextSecondary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = contributeAmount,
                        onValueChange = { contributeAmount = it },
                        label = { Text("Amount (₱)") },
                        placeholder = { Text("${String.format("%,.0f", circle.contributionPerMonth)}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PundarBlue, focusedLabelColor = PundarBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = contributeAmount.toDoubleOrNull() ?: circle.contributionPerMonth
                        AppState.contributeToCircle(circleId, amount)
                        showContributeDialog = false
                        contributeAmount = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PundarGold, contentColor = PundarTextPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Confirm", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showContributeDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Share Dialog
    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Share Circle Details", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("📋 Circle: ${circle.name}", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("🎯 Target: ₱ ${String.format("%,.0f", circle.targetAmount)}", style = MaterialTheme.typography.bodyMedium)
                    Text("💰 Saved: ₱ ${String.format("%,.0f", circle.savedAmount)}", style = MaterialTheme.typography.bodyMedium)
                    Text("👥 Members: ${circle.memberCount}", style = MaterialTheme.typography.bodyMedium)
                    Text("📅 Target Date: ${circle.targetDate}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Invite link: pundar.app/circle/${circle.id}", style = MaterialTheme.typography.bodySmall, color = PundarBlue)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showShareDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PundarBlue),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Copy Link", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) { Text("Close") }
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
        containerColor = PundarBackground
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
                                color = if (circle.isActive) PundarBlue else PundarSuccess
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = PundarSurfaceVariant) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text("👥", fontSize = 12.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text("${circle.memberCount} Members",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold, color = PundarTextSecondary)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(circle.name, style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = PundarTextPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text("Target: ${circle.targetDate}",
                                style = MaterialTheme.typography.bodyMedium, color = PundarTextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Saved", style = MaterialTheme.typography.bodySmall, color = PundarTextSecondary)
                            Text("₱ ${String.format("%,.0f", circle.savedAmount)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = PundarBlue)
                            Text("of ₱ ${String.format("%,.0f", circle.targetAmount)}",
                                style = MaterialTheme.typography.labelSmall, color = PundarTextSecondary)
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    val progress = (circle.savedAmount / circle.targetAmount).toFloat().coerceIn(0f, 1f)
                    PundarProgressBar(progress = progress, color = PundarBlue)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${(progress * 100).toInt()}% Completed",
                            style = MaterialTheme.typography.labelMedium, color = PundarTextSecondary)
                        Text("₱ ${String.format("%,.0f", circle.targetAmount - circle.savedAmount)} remaining",
                            style = MaterialTheme.typography.labelMedium, color = PundarTextSecondary)
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (isMember) {
                            Button(
                                onClick = { showContributeDialog = true },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PundarGold, contentColor = PundarTextPrimary)
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
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PundarBlue)
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
                        fontWeight = FontWeight.Bold, color = PundarTextPrimary)
                    TextButton(onClick = { 
                        Toast.makeText(context, "No recent history found.", Toast.LENGTH_SHORT).show()
                    }) { Text("View History", color = PundarBlue) }
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
                                avatarColor = if (member.isYou) PundarBlue else androidx.compose.ui.graphics.Color(member.avatarColor),
                                showNudge = member.status != ContributionStatus.PAID && !member.isYou,
                                isHighlighted = member.status != ContributionStatus.PAID && !member.isYou
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
                        fontWeight = FontWeight.Bold, color = PundarTextPrimary)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.CalendarToday, null, tint = PundarBlue,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Monthly Due Date", style = MaterialTheme.typography.labelMedium,
                                color = PundarTextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("${circle.monthlyDueDay}th of every month",
                                style = MaterialTheme.typography.bodyMedium, color = PundarTextSecondary)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.Payments, null, tint = PundarBlue,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Contribution Amount", style = MaterialTheme.typography.labelMedium,
                                color = PundarTextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("₱ ${String.format("%,.0f", circle.contributionPerMonth)} / member / month",
                                style = MaterialTheme.typography.bodyMedium, color = PundarTextSecondary)
                        }
                    }
                }
            }
        }
    }
}
