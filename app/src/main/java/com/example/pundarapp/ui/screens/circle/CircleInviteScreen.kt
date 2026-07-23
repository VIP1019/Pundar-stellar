package com.example.pundarapp.ui.screens.circle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.CircleRepository
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleInviteScreen(inviteId: String, navController: NavController) {
    val baseInvite = AppState.pendingInvitation.value
        ?: run { navController.navigateUp(); return }

    var invite by remember { mutableStateOf(baseInvite) }

    LaunchedEffect(baseInvite.circleId) {
        invite = CircleRepository.refreshInvitation(baseInvite)
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isJoining by remember { mutableStateOf(false) }
    var joinError by remember { mutableStateOf<String?>(null) }

    val isFull = invite.memberCount >= invite.maxMembers
    val remainingSlots = (invite.maxMembers - invite.memberCount).coerceAtLeast(0)

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Join ${invite.circleName}?", fontWeight = FontWeight.Bold) },
            text = {
                Text("You'll commit to contributing ₱ ${String.format("%,.0f", invite.monthlyContribution)} per month. This will be secured by a Soroban smart contract on the Stellar network.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        isJoining = true
                        joinError = null
                        scope.launch {
                            val result = AppState.acceptInvitation(invite)
                            isJoining = false
                            if (result.isSuccess) {
                                showConfirmDialog = false
                                navController.popBackStack(
                                    route = com.example.pundarapp.ui.navigation.Routes.CIRCLE,
                                    inclusive = false
                                )
                            } else {
                                joinError = result.exceptionOrNull()?.message
                                    ?: CircleRepository.MAX_MEMBER_LIMIT_MESSAGE
                                Toast.makeText(context, joinError, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isJoining && !isFull,
                    colors = ButtonDefaults.buttonColors(containerColor = PundarTheme.colors.brandPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Confirm & Join", color = PundarTheme.colors.surfacePrimary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "PUNDAR",
                onBack = { navController.navigateUp() },
                showHelp = true
            )
        },
        containerColor = PundarTheme.colors.bgPrimary,
        bottomBar = {
            Column(modifier = Modifier.background(PundarTheme.colors.surfacePrimary).padding(16.dp)) {
                if (isFull) {
                    Text(
                        CircleRepository.MAX_MEMBER_LIMIT_MESSAGE,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
                joinError?.let { err ->
                    Text(err, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                }
                PundarBlueButton(
                    text = if (isFull) "Paluwagan Full" else if (isJoining) "Joining..." else "Join Paluwagan →",
                    onClick = { if (!isFull) showConfirmDialog = true }
                )
                Spacer(Modifier.height(12.dp))
                PundarSecondaryButton(text = "View Detailed Schedule", onClick = { })
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = PundarBlueSubtle) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Payments, null, tint = PundarTheme.colors.brandPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Invitation Received", style = MaterialTheme.typography.labelMedium,
                            color = PundarTheme.colors.brandPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("You're Invited to Join:", style = MaterialTheme.typography.bodyMedium,
                    color = PundarTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(invite.circleName, style = MaterialTheme.typography.titleLarge,
                    color = PundarTheme.colors.brandPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Invited by ${invite.inviterName}",
                    style = MaterialTheme.typography.bodyMedium, color = PundarTheme.colors.textSecondary)
            }

            item {
                PundarAccentCard {
                    Text("Total Target Amount", style = MaterialTheme.typography.bodyMedium, color = PundarTheme.colors.textSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text("₱${String.format("%,.0f", invite.targetAmount)}",
                        style = MaterialTheme.typography.titleMedium, color = PundarTheme.colors.brandPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    PundarProgressBar(progress = invite.fundedPercent / 100f, color = PundarTheme.colors.brandPrimary, height = 6)
                    Spacer(Modifier.height(4.dp))
                    Text("${invite.fundedPercent}% funded",
                        style = MaterialTheme.typography.bodySmall, color = PundarTheme.colors.textSecondary,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PundarCard(modifier = Modifier.weight(1f)) {
                        Box(contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(PundarTheme.colors.brandPrimary)) {
                            Icon(Icons.Filled.Payments, null, tint = PundarTheme.colors.surfacePrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Monthly Contrib.", style = MaterialTheme.typography.bodyMedium, color = PundarTheme.colors.textSecondary)
                        Text("₱${String.format("%,.0f", invite.monthlyContribution)}",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    PundarCard(modifier = Modifier.weight(1f)) {
                        Box(contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(PundarBlueSubtle)) {
                            Icon(Icons.Filled.Group, null, tint = PundarTheme.colors.brandPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Members", style = MaterialTheme.typography.bodyMedium, color = PundarTheme.colors.textSecondary)
                        Text("${invite.memberCount} / ${invite.maxMembers}",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (remainingSlots == 0) "Paluwagan Full" else "$remainingSlots slots left",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (remainingSlots == 0) MaterialTheme.colorScheme.error else PundarTheme.colors.brandPrimary
                        )
                    }
                }
            }

            item {
                Text("Trust & Security", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = PundarTheme.colors.textPrimary, modifier = Modifier.fillMaxWidth())
            }

            item {
                PundarCard {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Box(contentAlignment = Alignment.Center,
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(invite.inviterAvatarColor))) {
                            Text(invite.inviterInitials, color = PundarTheme.colors.surfacePrimary,
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(invite.inviterName, style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(8.dp))
                                Surface(shape = RoundedCornerShape(4.dp), color = PundarTheme.colors.accentGold) {
                                    Text("ORGANIZER", style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.VerifiedUser, null, tint = PundarTheme.colors.brandPrimary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("PUNDAR Score: ${invite.inviterScore} (Excellent)",
                                    style = MaterialTheme.typography.labelMedium, color = PundarTheme.colors.brandPrimary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Successfully completed ${invite.inviterCirclesCompleted} Paluwagans.",
                                style = MaterialTheme.typography.bodySmall, color = PundarTheme.colors.textSecondary)
                        }
                    }
                }
            }

            item {
                PundarCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(8.dp), color = PundarTheme.colors.surfaceTertiary) {
                            Icon(Icons.Filled.Security, null, tint = PundarTheme.colors.textPrimary,
                                modifier = Modifier.padding(12.dp).size(24.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Soroban Smart Contract", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Funds are securely locked and automatically distributed.",
                                style = MaterialTheme.typography.bodySmall, color = PundarTheme.colors.textSecondary)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
