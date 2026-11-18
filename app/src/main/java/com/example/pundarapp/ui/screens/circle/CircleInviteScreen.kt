package com.example.pundarapp.ui.screens.circle

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.remote.CircleRepository
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.Circle
import com.example.pundarapp.ui.data.CircleContractRules
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme
import kotlinx.coroutines.launch
import java.util.Locale

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
    var declineReasonInput by remember { mutableStateOf("") }
    var agreedToAll12Clauses by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    val termsVersion = fullCircle?.termsVersion ?: invite.termsVersion
    val currency = fullCircle?.currency ?: invite.currency
    val fixedContribution = fullCircle?.contributionPerMonth ?: invite.monthlyContribution
    val scheduleText = fullCircle?.targetDate ?: invite.scheduleText
    val members = fullCircle?.members ?: emptyList()
    val participantCount = if (members.isNotEmpty()) members.size else invite.maxMembers.coerceAtLeast(1)
    val scheduledPayoutAmount = fixedContribution * participantCount

    val requiredXlm = if (currency.equals("PHP", ignoreCase = true)) {
        AppState.phpToXlm(fixedContribution)
    } else {
        fixedContribution
    }
    val minRequiredXlm = requiredXlm + CircleContractRules.NEW_USER_MIN_RESERVE_XLM
    val isBanned = AppState.isCircleBanned.value
    val hasMinBalance = AppState.walletBalance.value >= minRequiredXlm

    val contractClauses = remember(fixedContribution, currency, scheduleText, scheduledPayoutAmount) {
        CircleContractRules.getClauses(
            contributionFormatted = "${if (currency == "PHP") "₱" else ""}${String.format(Locale.US, "%,.2f", fixedContribution)} $currency / round",
            scheduleText = scheduleText,
            payoutOrderType = fullCircle?.payoutOrderType ?: "Predetermined Fixed Rotation",
            payoutFormatted = "${if (currency == "PHP") "₱" else ""}${String.format(Locale.US, "%,.2f", scheduledPayoutAmount)} $currency"
        )
    }

    if (showAcceptConfirmDialog) {
        Dialog(
            onDismissRequest = { if (!isProcessing) showAcceptConfirmDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 660.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0B1B3D))
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E2E5C)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Gavel,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Accept Circle Contract (v$termsVersion)",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        "${invite.circleName} • $participantCount Members",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { if (!isProcessing) showAcceptConfirmDialog = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Proposed Circle Summary", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFF0B2447))
                                    Text("• Fixed Contribution: ${String.format(Locale.US, "%,.2f", fixedContribution)} $currency / round", fontSize = 12.sp, color = Color(0xFF0F172A))
                                    Text("• Schedule: $scheduleText", fontSize = 12.sp, color = Color(0xFF0F172A))
                                    Text("• Total Participants: $participantCount Verified Members", fontSize = 12.sp, color = Color(0xFF0F172A))
                                    Text("• Scheduled Payout / Round: ${String.format(Locale.US, "%,.2f", scheduledPayoutAmount)} $currency", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                                }
                            }
                        }

                        items(contractClauses.size) { idx ->
                            val cl = contractClauses[idx]
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF0B2447)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                cl.code.uppercase(),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 10.sp,
                                                color = Color.White
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            cl.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        cl.description,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { if (!isProcessing) showAcceptConfirmDialog = false },
                                enabled = !isProcessing,
                                modifier = Modifier
                                    .weight(0.38f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold)
                            }
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
                                            showAcceptConfirmDialog = false
                                            actionError = result.exceptionOrNull()?.message ?: "Failed to record acceptance"
                                        }
                                    }
                                },
                                enabled = !isProcessing,
                                modifier = Modifier
                                    .weight(0.62f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B2447)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    if (isProcessing) "Recording..." else "Confirm & Join Circle",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeclineConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeclineConfirmDialog = false },
            containerColor = PundarTheme.colors.surfacePrimary,
            title = { Text("Decline Circle Invitation?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "If you decline, your status will be recorded as Declined and the Circle will remain inactive until the organizer replaces you or issues revised terms.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = declineReasonInput,
                        onValueChange = { declineReasonInput = it },
                        label = { Text("Reason for declining") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            AppState.rejectInvitation(invite, declineReasonInput)
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
                title = "Circle Invitation & Contract",
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { agreedToAll12Clauses = !agreedToAll12Clauses }
                    ) {
                        Checkbox(
                            checked = agreedToAll12Clauses,
                            onCheckedChange = { agreedToAll12Clauses = it }
                        )
                        Text(
                            "I agree to all 12 Circle Contract rules (a - l) and the proposed member roster.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PundarTheme.colors.textPrimary
                        )
                    }
                    Button(
                        onClick = { if (!isProcessing) showAcceptConfirmDialog = true },
                        enabled = agreedToAll12Clauses && !isProcessing && !isBanned && hasMinBalance,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B2447))
                    ) {
                        Text(
                            if (isProcessing) "Processing..." else "Accept Circle Terms (v$termsVersion)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
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

            // Eligibility Check Card (Strict Minimum Balance + Ban Status)
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isBanned || !hasMinBalance) Color(0xFFFEF2F2) else Color(0xFFECFDF5),
                    border = BorderStroke(
                        1.dp,
                        if (isBanned || !hasMinBalance) Color(0xFFFECACA) else Color(0xFFA7F3D0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = when {
                                isBanned -> "Circle Participation Banned: You cannot join Circles due to a previous failed payment."
                                !hasMinBalance -> "Strict Minimum Balance Required: Need ${String.format(Locale.US, "%,.2f", minRequiredXlm)} XLM to join."
                                else -> "Eligibility Verified • PUNDAR Score: ${AppState.pundarScore.intValue} pts • Min Balance Met"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isBanned || !hasMinBalance) Color(0xFF991B1B) else Color(0xFF065F46)
                        )
                        Text(
                            "Your Balance: ${String.format(Locale.US, "%,.2f", AppState.walletBalance.value)} XLM • Required Minimum: ${String.format(Locale.US, "%,.2f", minRequiredXlm)} XLM",
                            fontSize = 11.sp,
                            color = PundarTheme.colors.textSecondary
                        )
                    }
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
                    InviteTermRow("Fixed Contribution / Member", "${String.format(Locale.US, "%,.2f", fixedContribution)} $currency")
                    InviteTermRow("Contribution Schedule", scheduleText)
                    InviteTermRow("Total Participants", "$participantCount Verified Members")
                    InviteTermRow("Terms Version", "v$termsVersion")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = PundarDivider)
                    InviteTermRow(
                        "Scheduled Payout / Round",
                        "${String.format(Locale.US, "%,.2f", scheduledPayoutAmount)} $currency (${String.format(Locale.US, "%,.0f", fixedContribution)} x $participantCount)",
                        highlight = true
                    )
                }
            }

            // Full Rotating Payout Order & Member Consent
            item {
                Text(
                    "Rotating Payout Order & Member Consent",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PundarTheme.colors.textPrimary
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
                                        .background(Color(0xFF0B2447)),
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
                                        "Slot $roundNum Recipient · ${String.format(Locale.US, "%,.2f", scheduledPayoutAmount)} $currency",
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

            // 12 Mandatory Contract Clauses (a - l)
            item {
                PundarCard {
                    Text(
                        "12 Circle Smart Contract Terms (a - l)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PundarTheme.colors.textPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    contractClauses.forEach { cl ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                "${cl.code}. ${cl.title}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF0B2447)
                            )
                            Text(
                                cl.description,
                                fontSize = 11.sp,
                                color = PundarTheme.colors.textSecondary
                            )
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
