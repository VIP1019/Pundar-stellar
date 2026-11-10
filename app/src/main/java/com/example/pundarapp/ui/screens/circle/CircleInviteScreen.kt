package com.example.pundarapp.ui.screens.circle

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.remote.CircleRepository
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.Circle
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleInviteScreen(inviteId: String, navController: NavController) {
    val baseInvite = AppState.pendingInvitations.find { it.id == inviteId }
        ?: AppState.pendingInvitation.value
        ?: run { navController.navigateUp(); return }

    var invite by remember { mutableStateOf(baseInvite) }
    var fullCircle by remember { mutableStateOf<Circle?>(null) }

    LaunchedEffect(baseInvite.circleId) {
        invite = CircleRepository.refreshInvitation(baseInvite)
        fullCircle = CircleRepository.getCircleById(baseInvite.circleId, AuthRepository.getCurrentUserPhone())
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAcceptConfirmDialog by remember { mutableStateOf(false) }
    var showDeclineConfirmDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    val termsVersion = fullCircle?.termsVersion ?: invite.termsVersion
    val currency = fullCircle?.currency ?: invite.currency
    val fixedContribution = fullCircle?.contributionPerMonth ?: invite.monthlyContribution
    val scheduleText = fullCircle?.targetDate ?: invite.scheduleText
    val members = fullCircle?.members ?: emptyList()
    val participantCount = if (members.isNotEmpty()) members.size else invite.maxMembers.coerceAtLeast(1)
    val scheduledPayoutAmount = fixedContribution * participantCount

    if (showAcceptConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showAcceptConfirmDialog = false },
            containerColor = PundarTheme.colors.surfacePrimary,
            title = { Text("Accept Circle Terms (v$termsVersion)?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "You are explicitly opting in to the proposed Circle configuration for \"${invite.circleName}\":",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("• Fixed Contribution: ${String.format("%,.2f", fixedContribution)} $currency per round")
                    Text("• Schedule: $scheduleText")
                    Text("• Total Participants: $participantCount Verified Members")
                    Text("• Scheduled Payout / Round: ${String.format("%,.2f", scheduledPayoutAmount)} $currency")
                    Text(
                        "Note: Your acceptance applies specifically to Terms Version $termsVersion. The Circle remains inactive until every participant accepts these exact terms.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PundarTheme.colors.textSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessing = true
                        actionError = null
                        scope.launch {
                            val result = AppState.acceptInvitation(invite)
                            isProcessing = false
                            if (result.isSuccess) {
                                showAcceptConfirmDialog = false
                                Toast.makeText(
                                    context,
                                    "Accepted Terms v$termsVersion for ${invite.circleName}!",
                                    Toast.LENGTH_LONG
                                ).show()
                                navController.navigate(Routes.circleDetail(invite.circleId)) {
                                    popUpTo(Routes.CIRCLE) { inclusive = false }
                                }
                            } else {
                                actionError = result.exceptionOrNull()?.message ?: "Failed to record acceptance"
                            }
                        }
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = PundarTheme.colors.brandPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (isProcessing) "Recording..." else "Confirm Acceptance",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAcceptConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeclineConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeclineConfirmDialog = false },
            containerColor = PundarTheme.colors.surfacePrimary,
            title = { Text("Decline Circle Invitation?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "If you decline, your status will be recorded as Declined and the Circle will remain inactive. It cannot be activated unless the organizer removes the declined participant and all remaining members accept the revised terms.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            AppState.rejectInvitation(invite)
                            isProcessing = false
                            showDeclineConfirmDialog = false
                            Toast.makeText(
                                context,
                                "Invitation declined. Circle remains inactive.",
                                Toast.LENGTH_LONG
                            ).show()
                            navController.popBackStack()
                        }
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = PundarTheme.colors.accentRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm Decline", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeclineConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Circle Invitation Details",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarTheme.colors.bgPrimary,
        bottomBar = {
            Surface(
                color = PundarTheme.colors.surfacePrimary,
                border = BorderStroke(1.dp, PundarTheme.colors.borderPrimary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    actionError?.let { err ->
                        Text(
                            err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    }
                    PundarBlueButton(
                        text = if (isProcessing) "Processing..." else "Accept Circle Terms (v$termsVersion) ✓",
                        onClick = { if (!isProcessing) showAcceptConfirmDialog = true }
                    )
                    Spacer(Modifier.height(10.dp))
                    PundarSecondaryButton(
                        text = "Decline Invitation",
                        onClick = { if (!isProcessing) showDeclineConfirmDialog = true }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(shape = RoundedCornerShape(16.dp), color = PundarBlueSubtle) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.VerifiedUser, null, tint = PundarTheme.colors.brandPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Explicit Opt-In Required · Terms v$termsVersion",
                                style = MaterialTheme.typography.labelMedium,
                                color = PundarTheme.colors.brandPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        invite.circleName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = PundarTheme.colors.textPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Proposed by Organizer: ${invite.inviterName} (Score: ${invite.inviterScore})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PundarTheme.colors.textSecondary
                    )
                }
            }

            // Complete Circle Terms Card
            item {
                PundarCard {
                    Text(
                        "Complete Circle Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PundarTheme.colors.textPrimary
                    )
                    Spacer(Modifier.height(12.dp))
                    InviteTermRow("Fixed Contribution / Member", "${String.format("%,.2f", fixedContribution)} $currency")
                    InviteTermRow("Contribution Schedule", scheduleText)
                    InviteTermRow("Total Participants", "$participantCount Verified Members")
                    InviteTermRow("Terms Version", "v$termsVersion")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = PundarDivider)
                    InviteTermRow(
                        "Scheduled Payout / Round",
                        "${String.format("%,.2f", scheduledPayoutAmount)} $currency (${String.format("%,.0f", fixedContribution)} × $participantCount)",
                        highlight = true
                    )
                }
            }

            // Complete Participant List & Full Rotating Payout Order
            item {
                Text(
                    "Full Rotating Payout Order & Member Consent",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PundarTheme.colors.textPrimary
                )
                Text(
                    "Review who is participating and which member receives the scheduled payout in every round.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PundarTheme.colors.textSecondary
                )
            }

            if (members.isNotEmpty()) {
                itemsIndexed(members) { idx, m ->
                    val roundNum = m.payoutRound.takeIf { it > 0 } ?: (idx + 1)
                    val isMe = m.isYou || m.userId == AuthRepository.getCurrentUserPhone()
                    val statusColor = when (m.acceptanceStatus) {
                        "ACCEPTED" -> Color(0xFF059669)
                        "DECLINED" -> Color(0xFFDC2626)
                        else -> Color(0xFFD97706)
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            if (isMe) 1.5.dp else 1.dp,
                            if (isMe) PundarTheme.colors.brandPrimary else PundarTheme.colors.borderPrimary
                        ),
                        color = PundarTheme.colors.surfacePrimary
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PundarTheme.colors.brandPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("R$roundNum", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        if (isMe) "${m.name} (You)" else m.name,
                                        fontWeight = FontWeight.Bold,
                                        color = PundarTheme.colors.textPrimary
                                    )
                                    Text(
                                        "Round $roundNum Recipient · ${String.format("%,.2f", scheduledPayoutAmount)} $currency",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PundarTheme.colors.textSecondary
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = statusColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    if (m.acceptanceStatus == "ACCEPTED") "ACCEPTED v${m.acceptedTermsVersion}" else m.acceptanceStatus,
                                    color = statusColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteTermRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = PundarTheme.colors.textSecondary)
        Text(
            value,
            fontWeight = if (highlight) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (highlight) PundarTheme.colors.brandPrimary else PundarTheme.colors.textPrimary
        )
    }
}
