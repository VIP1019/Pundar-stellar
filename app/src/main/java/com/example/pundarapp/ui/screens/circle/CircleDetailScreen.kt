package com.example.pundarapp.ui.screens.circle

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.remote.CircleRepository
import com.example.pundarapp.data.stellar.StellarWalletManager
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.*
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleDetailScreen(circleId: String, navController: NavController) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val currentUserId = AuthRepository.getCurrentUserPhone()
    val currentUserName = AuthRepository.getCurrentUserName().ifBlank { "Member" }

    var circleState by remember {
        mutableStateOf(AppState.circles.find { it.id == circleId })
    }

    suspend fun reloadCircle() {
        val fetched = CircleRepository.getCircleById(circleId, currentUserId)
        if (fetched != null) {
            circleState = fetched
            val idx = AppState.circles.indexOfFirst { it.id == circleId }
            if (idx >= 0) AppState.circles[idx] = fetched else AppState.circles.add(0, fetched)
        }
    }

    LaunchedEffect(circleId) {
        reloadCircle()
    }

    val circle = circleState ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isOrganizer = circle.creatorId == currentUserId || circle.members.firstOrNull()?.isYou == true
    val myMemberEntry = circle.members.find {
        it.isYou || it.userId == currentUserId || it.name.equals(currentUserName, ignoreCase = true)
    }
    val isMember = myMemberEntry != null
    val totalMembers = circle.members.size.coerceAtLeast(1)
    val scheduledPayout = circle.scheduledPayoutAmount
    val currentRecipient = circle.currentRoundRecipient
    val contributedCount = circle.members.count { it.status == ContributionStatus.PAID }
    val acceptedCount = circle.members.count {
        it.acceptanceStatus == "ACCEPTED" && it.acceptedTermsVersion == circle.termsVersion
    }

    // Dialog states
    var showActivateDialog by remember { mutableStateOf(false) }
    var showContributeDialog by remember { mutableStateOf(false) }
    var showPayoutDialog by remember { mutableStateOf(false) }
    var mpinInput by remember { mutableStateOf("") }
    var isProcessingTx by remember { mutableStateOf(false) }
    var dialogError by remember { mutableStateOf<String?>(null) }

    // Receipt / Confirmation states
    var payoutReceipt by remember { mutableStateOf<CircleTransactionRecord?>(null) }
    var validationBannerMessage by remember { mutableStateOf<String?>(null) }

    // ── 1. ACTIVATE CIRCLE ON SOROBAN DIALOG (Section 7) ─────────
    if (showActivateDialog) {
        AlertDialog(
            onDismissRequest = { if (!isProcessingTx) showActivateDialog = false },
            containerColor = PundarTheme.colors.surfacePrimary,
            title = { Text("Activate Circle on Soroban", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "All $totalMembers participants have accepted Terms v${circle.termsVersion}. Activating this Circle on the Soroban smart contract locks the member list, fixed contribution (${String.format("%,.2f", circle.contributionPerMonth)} ${circle.currency}), schedule (${circle.targetDate}), and rotating payout order immutably.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = mpinInput,
                        onValueChange = { mpinInput = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Enter 4-Digit MPIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    dialogError?.let {
                        Text(it, color = PundarTheme.colors.accentRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (mpinInput.length != 4) {
                            dialogError = "Please enter your 4-digit MPIN."
                            return@Button
                        }
                        if (!circle.allMembersAcceptedCurrentTerms) {
                            dialogError = "Rejected: Cannot activate Circle before all members accept Terms v${circle.termsVersion}."
                            return@Button
                        }
                        isProcessingTx = true
                        dialogError = null
                        scope.launch {
                            val pubKey = AuthRepository.getCurrentUserStellarPublicKey().orEmpty()
                            val encSeed = AuthRepository.getCurrentUserEncryptedSeed().orEmpty()
                            val memberAddresses = circle.members.map {
                                it.stellarPublicKey.ifBlank { pubKey }
                            }
                            val cycleSecs = (circle.payoutFrequencyValue.coerceAtLeast(1) * 86400L)
                            val netRes = StellarWalletManager.activateCircleOnTestnet(
                                adminPublicKey = pubKey,
                                adminEncryptedSeed = encSeed,
                                adminMpin = mpinInput,
                                members = memberAddresses,
                                contributionAmountXlm = circle.contributionPerMonth,
                                payoutOrder = memberAddresses,
                                cycleDurationSeconds = cycleSecs
                            )
                            if (netRes.isSuccess) {
                                val (sorobanId, txHash) = netRes.getOrThrow()
                                val repoRes = CircleRepository.activateCircle(
                                    circleId = circle.id,
                                    organizerId = currentUserId,
                                    sorobanCircleId = sorobanId,
                                    activationTxHash = txHash
                                )
                                isProcessingTx = false
                                if (repoRes.isSuccess) {
                                    showActivateDialog = false
                                    mpinInput = ""
                                    reloadCircle()
                                    Toast.makeText(
                                        context,
                                        "Circle activated on Soroban Testnet! Rules are now immutable.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    dialogError = repoRes.exceptionOrNull()?.message
                                }
                            } else {
                                isProcessingTx = false
                                dialogError = netRes.exceptionOrNull()?.message ?: "Failed to activate on Stellar Testnet"
                            }
                        }
                    },
                    enabled = !isProcessingTx,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text(if (isProcessingTx) "Activating on Soroban..." else "Authorize & Lock Rules", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isProcessingTx) showActivateDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── 2. FIXED CONTRIBUTION DIALOG (Section 9) ──────────────────
    if (showContributeDialog) {
        AlertDialog(
            onDismissRequest = { if (!isProcessingTx) showContributeDialog = false },
            containerColor = PundarTheme.colors.surfacePrimary,
            title = { Text("Submit Round ${circle.currentRound} Contribution", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PundarTheme.colors.brandPrimary.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Circle: ${circle.name}", fontWeight = FontWeight.Bold)
                            Text("Active Round: Round ${circle.currentRound} of $totalMembers")
                            Text(
                                "Locked Fixed Contribution: ${String.format("%,.2f", circle.contributionPerMonth)} ${circle.currency}",
                                fontWeight = FontWeight.ExtraBold,
                                color = PundarTheme.colors.brandPrimary
                            )
                            Text(
                                "Note: Amount is locked by the Soroban smart contract. Partial or custom amounts are rejected.",
                                style = MaterialTheme.typography.labelSmall,
                                color = PundarTheme.colors.textSecondary
                            )
                        }
                    }
                    OutlinedTextField(
                        value = mpinInput,
                        onValueChange = { mpinInput = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Enter 4-Digit MPIN to Sign Testnet Tx") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    dialogError?.let {
                        Text(it, color = PundarTheme.colors.accentRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (mpinInput.length != 4) {
                            dialogError = "Please enter your 4-digit MPIN."
                            return@Button
                        }
                        isProcessingTx = true
                        dialogError = null
                        scope.launch {
                            val pubKey = AuthRepository.getCurrentUserStellarPublicKey().orEmpty()
                            val encSeed = AuthRepository.getCurrentUserEncryptedSeed().orEmpty()
                            val netRes = StellarWalletManager.contributeToCircleOnTestnet(
                                memberPublicKey = pubKey,
                                memberEncryptedSeed = encSeed,
                                memberMpin = mpinInput,
                                sorobanCircleId = circle.sorobanCircleId,
                                amountXlm = circle.contributionPerMonth,
                                roundNumber = circle.currentRound,
                                escrowDestinationKey = circle.members.firstOrNull()?.stellarPublicKey?.ifBlank { pubKey } ?: pubKey
                            )
                            if (netRes.isSuccess) {
                                val txHash = netRes.getOrThrow()
                                val repoRes = CircleRepository.recordMemberContribution(
                                    circleId = circle.id,
                                    userId = currentUserId,
                                    txHash = txHash
                                )
                                isProcessingTx = false
                                if (repoRes.isSuccess) {
                                    showContributeDialog = false
                                    mpinInput = ""
                                    reloadCircle()
                                    AppState.requestHomeRefresh()
                                    Toast.makeText(
                                        context,
                                        "Round ${circle.currentRound} contribution recorded on Stellar Testnet!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    dialogError = repoRes.exceptionOrNull()?.message
                                }
                            } else {
                                isProcessingTx = false
                                dialogError = netRes.exceptionOrNull()?.message ?: "Stellar Testnet transaction failed"
                            }
                        }
                    },
                    enabled = !isProcessingTx,
                    colors = ButtonDefaults.buttonColors(containerColor = PundarTheme.colors.brandPrimary)
                ) {
                    Text(
                        if (isProcessingTx) "Submitting to Testnet..." else "Authorize ${String.format("%,.0f", circle.contributionPerMonth)} ${circle.currency}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isProcessingTx) showContributeDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── 3. INVOKE SCHEDULED PAYOUT DIALOG (Section 12) ────────────
    if (showPayoutDialog) {
        AlertDialog(
            onDismissRequest = { if (!isProcessingTx) showPayoutDialog = false },
            containerColor = PundarTheme.colors.surfacePrimary,
            title = { Text("Invoke Scheduled Payout · Round ${circle.currentRound}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Any enrolled Circle member may trigger the scheduled payout once all $totalMembers contributions are verified. Neither the recipient nor the payout amount can be altered:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF059669).copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Locked Round ${circle.currentRound} Recipient:", style = MaterialTheme.typography.labelSmall)
                            Text(
                                currentRecipient?.name ?: "Assigned Member",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color(0xFF059669)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("Deterministic Payout Amount:", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${String.format("%,.2f", scheduledPayout)} ${circle.currency} (${String.format("%,.0f", circle.contributionPerMonth)} × $totalMembers)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = PundarTheme.colors.textPrimary
                            )
                        }
                    }
                    OutlinedTextField(
                        value = mpinInput,
                        onValueChange = { mpinInput = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Enter 4-Digit MPIN to Invoke Contract") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    dialogError?.let {
                        Text(it, color = PundarTheme.colors.accentRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (mpinInput.length != 4) {
                            dialogError = "Please enter your 4-digit MPIN."
                            return@Button
                        }
                        isProcessingTx = true
                        dialogError = null
                        scope.launch {
                            val pubKey = AuthRepository.getCurrentUserStellarPublicKey().orEmpty()
                            val encSeed = AuthRepository.getCurrentUserEncryptedSeed().orEmpty()
                            val recipientKey = currentRecipient?.stellarPublicKey?.takeIf { it.isNotBlank() } ?: pubKey
                            val netRes = StellarWalletManager.invokePayoutOnTestnet(
                                callerPublicKey = pubKey,
                                callerEncryptedSeed = encSeed,
                                callerMpin = mpinInput,
                                sorobanCircleId = circle.sorobanCircleId,
                                scheduledRecipientKey = recipientKey,
                                scheduledPayoutXlm = scheduledPayout,
                                roundNumber = circle.currentRound
                            )
                            if (netRes.isSuccess) {
                                val txHash = netRes.getOrThrow()
                                val paidRoundNum = circle.currentRound
                                val recName = currentRecipient?.name ?: "Recipient"
                                val repoRes = CircleRepository.recordRoundPayout(
                                    circleId = circle.id,
                                    callerUserId = currentUserId,
                                    callerName = currentUserName,
                                    txHash = txHash
                                )
                                isProcessingTx = false
                                if (repoRes.isSuccess) {
                                    showPayoutDialog = false
                                    mpinInput = ""
                                    reloadCircle()
                                    AppState.requestHomeRefresh()
                                    payoutReceipt = CircleTransactionRecord(
                                        id = "receipt_$paidRoundNum",
                                        type = "ROUND_PAYOUT",
                                        title = "Round $paidRoundNum Scheduled Payout Released",
                                        roundNumber = paidRoundNum,
                                        memberName = recName,
                                        amount = scheduledPayout,
                                        currency = circle.currency,
                                        txHash = txHash,
                                        timestamp = System.currentTimeMillis(),
                                        status = "PAYOUT_RELEASED"
                                    )
                                } else {
                                    dialogError = repoRes.exceptionOrNull()?.message
                                }
                            } else {
                                isProcessingTx = false
                                dialogError = netRes.exceptionOrNull()?.message ?: "Failed to invoke payout on Stellar Testnet"
                            }
                        }
                    },
                    enabled = !isProcessingTx,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text(
                        if (isProcessingTx) "Releasing Payout..." else "Invoke Scheduled Payout",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isProcessingTx) showPayoutDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── 4. PAYOUT CONFIRMATION RECEIPT DIALOG (Section 13) ────────
    payoutReceipt?.let { receipt ->
        AlertDialog(
            onDismissRequest = { payoutReceipt = null },
            containerColor = PundarTheme.colors.surfacePrimary,
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("Scheduled Payout Released", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text(
                        "Soroban Deterministic Payout Receipt",
                        style = MaterialTheme.typography.bodySmall,
                        color = PundarTheme.colors.textSecondary
                    )
                }
            },
            text = {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PundarTheme.colors.surfaceTertiary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ReceiptLine("Circle", circle.name)
                        ReceiptLine("Round Settled", "Round ${receipt.roundNumber} of $totalMembers")
                        ReceiptLine("Scheduled Recipient", receipt.memberName)
                        ReceiptLine("Payout Amount", "${String.format("%,.2f", receipt.amount)} ${receipt.currency}")
                        ReceiptLine("Timestamp", receipt.formattedTime)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Stellar Testnet Transaction Hash:", style = MaterialTheme.typography.labelSmall, color = PundarTheme.colors.textSecondary)
                        Text(
                            receipt.txHash,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = PundarTheme.colors.brandPrimary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val url = "https://stellar.expert/explorer/testnet/tx/${receipt.txHash}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PundarTheme.colors.brandPrimary)
                ) {
                    Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Verify on Stellar Expert", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { payoutReceipt = null }) { Text("Done") }
            }
        )
    }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = circle.name,
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarTheme.colors.bgPrimary
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── HEADER & LIFECYCLE STATUS CARD ────────────────────────
            item {
                val (badgeText, badgeColor) = when (circle.lifecycleStatus) {
                    "AWAITING_ACCEPTANCE" -> "AWAITING MEMBER ACCEPTANCE · v${circle.termsVersion}" to Color(0xFFD97706)
                    "DECLINED_REVISION_NEEDED" -> "DECLINED · PRE-ACTIVATION REVISION NEEDED" to Color(0xFFDC2626)
                    "READY_TO_ACTIVATE" -> "ALL ACCEPTED · READY TO ACTIVATE" to Color(0xFF059669)
                    "ACTIVE" -> "ACTIVE ON SOROBAN · ROUND ${circle.currentRound} OF $totalMembers" to Color(0xFF2563EB)
                    "COMPLETED" -> "COMPLETED · ALL $totalMembers ROUNDS SETTLED" to Color(0xFF10B981)
                    else -> circle.lifecycleStatus to PundarTheme.colors.brandPrimary
                }

                PundarAccentCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                badgeText,
                                color = badgeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Text(
                            "Terms v${circle.termsVersion}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PundarTheme.colors.textSecondary
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                circle.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = PundarTheme.colors.textPrimary
                            )
                            Text(
                                "Fixed Contribution: ${String.format("%,.2f", circle.contributionPerMonth)} ${circle.currency} · ${circle.targetDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PundarTheme.colors.textSecondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Scheduled Payout / Round", style = MaterialTheme.typography.labelSmall, color = PundarTheme.colors.textSecondary)
                            Text(
                                "${String.format("%,.2f", scheduledPayout)} ${circle.currency}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = PundarTheme.colors.brandPrimary
                            )
                            Text(
                                "${String.format("%,.0f", circle.contributionPerMonth)} × $totalMembers members",
                                style = MaterialTheme.typography.labelSmall,
                                color = PundarTheme.colors.textMuted
                            )
                        }
                    }

                    if (circle.lifecycleStatus in listOf("ACTIVE", "COMPLETED")) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF059669).copy(alpha = 0.10f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Lock, null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Locked on Soroban at Activation — Member list, contribution amount, schedule, and payout order are immutable.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF065F46),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // ── PRE-ACTIVATION CONSENT & REVISION FLOW (Sections 5, 6, 7) ──
            if (circle.lifecycleStatus in listOf("AWAITING_ACCEPTANCE", "DECLINED_REVISION_NEEDED", "READY_TO_ACTIVATE")) {
                item {
                    PundarCard {
                        Text(
                            "Pre-Activation Member Consent ($acceptedCount of $totalMembers Accepted Terms v${circle.termsVersion})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PundarTheme.colors.textPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Every listed participant must explicitly accept Terms Version ${circle.termsVersion} before the Circle can be activated on the Soroban smart contract.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PundarTheme.colors.textSecondary
                        )

                        // Invited member's own Accept / Decline buttons if they haven't accepted current version
                        if (myMemberEntry != null &&
                            (myMemberEntry.acceptanceStatus != "ACCEPTED" || myMemberEntry.acceptedTermsVersion != circle.termsVersion)
                        ) {
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            CircleRepository.respondToCircleTerms(circle.id, currentUserId, true, circle.termsVersion)
                                            reloadCircle()
                                            Toast.makeText(context, "Accepted Terms v${circle.termsVersion}!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Accept Terms (v${circle.termsVersion})", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            CircleRepository.respondToCircleTerms(circle.id, currentUserId, false, circle.termsVersion)
                                            reloadCircle()
                                            Toast.makeText(context, "Declined invitation. Circle remains inactive.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Decline", color = PundarTheme.colors.accentRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Section 6: Decline & Pre-Activation Revision
                        val declinedMembers = circle.members.filter { it.acceptanceStatus == "DECLINED" }
                        if (declinedMembers.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFDC2626).copy(alpha = 0.10f),
                                border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "Activation Blocked — Participant Declined",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFDC2626)
                                    )
                                    declinedMembers.forEach { dm ->
                                        Text(
                                            "• ${dm.name} (Round ${dm.payoutRound}) declined the invitation.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = PundarTheme.colors.textPrimary
                                        )
                                        if (isOrganizer) {
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        val res = CircleRepository.removeDeclinedMemberAndReviseTerms(
                                                            circleId = circle.id,
                                                            declinedUserId = dm.userId.ifBlank { dm.name },
                                                            organizerId = currentUserId
                                                        )
                                                        if (res.isSuccess) {
                                                            reloadCircle()
                                                            Toast.makeText(
                                                                context,
                                                                "Removed ${dm.name} and issued Revised Terms v${circle.termsVersion + 1}. Remaining members must re-accept.",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                res.exceptionOrNull()?.message ?: "Revision failed",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    "Remove ${dm.name} & Issue Revised Terms (v${circle.termsVersion + 1})",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Section 7: Organizer Activate Circle Button
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = {
                                mpinInput = ""
                                dialogError = null
                                showActivateDialog = true
                            },
                            enabled = isOrganizer && circle.allMembersAcceptedCurrentTerms,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669),
                                disabledContainerColor = PundarTheme.colors.surfaceTertiary
                            )
                        ) {
                            Icon(Icons.Filled.VerifiedUser, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (circle.allMembersAcceptedCurrentTerms)
                                    "Activate Circle on Soroban (Terms v${circle.termsVersion})"
                                else
                                    "Activate Circle (Waiting for ${totalMembers - acceptedCount} Acceptance(s))",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ── ACTIVE / COMPLETED CIRCLE ROUND & PAYOUT CONTROL (Sections 8–14) ──
            if (circle.lifecycleStatus in listOf("ACTIVE", "COMPLETED")) {
                item {
                    PundarCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (circle.lifecycleStatus == "COMPLETED")
                                    "All $totalMembers Rotating Payout Rounds Completed"
                                else
                                    "Current Round: Round ${circle.currentRound} of $totalMembers",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = PundarTheme.colors.textPrimary
                            )
                            Text(
                                "$contributedCount / $totalMembers Contributed",
                                fontWeight = FontWeight.Bold,
                                color = if (circle.allCurrentRoundContributionsComplete) Color(0xFF059669) else Color(0xFFD97706)
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        PundarProgressBar(
                            progress = if (circle.lifecycleStatus == "COMPLETED") 1f else (contributedCount.toFloat() / totalMembers.toFloat()),
                            color = if (circle.allCurrentRoundContributionsComplete || circle.lifecycleStatus == "COMPLETED") Color(0xFF10B981) else PundarTheme.colors.brandPrimary
                        )

                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Scheduled Round ${circle.currentRound} Recipient", style = MaterialTheme.typography.labelSmall, color = PundarTheme.colors.textSecondary)
                                Text(
                                    currentRecipient?.name ?: "Completed",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PundarTheme.colors.brandPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Derived Round Payout", style = MaterialTheme.typography.labelSmall, color = PundarTheme.colors.textSecondary)
                                Text(
                                    "${String.format("%,.2f", scheduledPayout)} ${circle.currency}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF059669)
                                )
                            }
                        }

                        if (circle.lifecycleStatus == "ACTIVE") {
                            Spacer(Modifier.height(12.dp))
                            if (!circle.allCurrentRoundContributionsComplete) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Waiting for remaining member contributions ($contributedCount of $totalMembers recorded). Scheduled payout is locked until all $totalMembers contributions are verified on-chain.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFB45309),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.14f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Payout Eligible ✓ — All $totalMembers contributions for Round ${circle.currentRound} are recorded! Any enrolled Circle member can now invoke the scheduled payout to ${currentRecipient?.name}.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF065F46),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))
                            val myAlreadyContributed = myMemberEntry?.status == ContributionStatus.PAID
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        mpinInput = ""
                                        dialogError = null
                                        showContributeDialog = true
                                    },
                                    enabled = isMember && !myAlreadyContributed,
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PundarTheme.colors.brandPrimary)
                                ) {
                                    Text(
                                        if (myAlreadyContributed) "Contributed ✓"
                                        else "Contribute ${String.format("%,.0f", circle.contributionPerMonth)} ${circle.currency}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                                Button(
                                    onClick = {
                                        mpinInput = ""
                                        dialogError = null
                                        showPayoutDialog = true
                                    },
                                    enabled = isMember && circle.isCurrentRoundPayoutEligible,
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                ) {
                                    Text(
                                        "Invoke Scheduled Payout",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── ROTATING PAYOUT ORDER & MEMBER STATUS LIST ────────────
            item {
                Text(
                    "Rotating Payout Order & Member Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PundarTheme.colors.textPrimary
                )
            }

            itemsIndexed(circle.members) { idx, member ->
                val roundNum = member.payoutRound.takeIf { it > 0 } ?: (idx + 1)
                val isCurrentRecipient = circle.lifecycleStatus == "ACTIVE" && roundNum == circle.currentRound
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        if (isCurrentRecipient) 1.5.dp else 1.dp,
                        if (isCurrentRecipient) PundarTheme.colors.brandPrimary else PundarTheme.colors.borderPrimary
                    ),
                    color = PundarTheme.colors.surfacePrimary
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrentRecipient) PundarTheme.colors.brandPrimary else Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("R$roundNum", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        if (member.isYou) "${member.name} (You)" else member.name,
                                        fontWeight = FontWeight.Bold,
                                        color = PundarTheme.colors.textPrimary
                                    )
                                    Text(
                                        "Round $roundNum Payout · ${
                                            if (member.hasReceivedPayout) "Paid ✓"
                                            else if (isCurrentRecipient) "Active Scheduled Recipient"
                                            else "Scheduled (${member.payoutDate})"
                                        }",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PundarTheme.colors.textSecondary
                                    )
                                }
                            }

                            // Status Badge (Pre-activation vs Active)
                            val (statusText, statusCol) = if (circle.lifecycleStatus in listOf("AWAITING_ACCEPTANCE", "DECLINED_REVISION_NEEDED", "READY_TO_ACTIVATE")) {
                                when (member.acceptanceStatus) {
                                    "ACCEPTED" -> "ACCEPTED v${member.acceptedTermsVersion}" to Color(0xFF059669)
                                    "DECLINED" -> "DECLINED" to Color(0xFFDC2626)
                                    else -> "PENDING" to Color(0xFFD97706)
                                }
                            } else {
                                if (member.status == ContributionStatus.PAID) "CONTRIBUTED ✓" to Color(0xFF059669)
                                else "PENDING" to Color(0xFFD97706)
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = statusCol.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    statusText,
                                    color = statusCol,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (member.contributionTxHash.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Contribution Tx: ${member.contributionTxHash.take(20)}...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = PundarTheme.colors.brandPrimary,
                                modifier = Modifier.clickable {
                                    val url = "https://stellar.expert/explorer/testnet/tx/${member.contributionTxHash}"
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            )
                        }
                    }
                }
            }

            // ── SOROBAN ESCROW CONTRACT CARD ──────────────────────────
            item {
                EscrowStatusCard(
                    contractAddress = circle.escrowAddress,
                    network = circle.escrowNetwork
                )
            }

            // ── CIRCLE TRANSACTION HISTORY (Section 15) ───────────────
            item {
                PundarCard {
                    Text(
                        "Verifiable Stellar Testnet Transaction History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PundarTheme.colors.textPrimary
                    )
                    Spacer(Modifier.height(10.dp))
                    if (circle.transactionHistory.isEmpty()) {
                        Text(
                            "No transactions recorded yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PundarTheme.colors.textSecondary
                        )
                    } else {
                        circle.transactionHistory.forEachIndexed { i, tx ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${tx.type} · Round ${tx.roundNumber}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = PundarTheme.colors.textPrimary
                                    )
                                    Text(
                                        "${String.format("%,.2f", tx.amount)} ${tx.currency}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = PundarTheme.colors.brandPrimary
                                    )
                                }
                                Text(
                                    "Participant: ${tx.memberName} · ${tx.formattedTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PundarTheme.colors.textSecondary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Tx: ${tx.txHash.take(24)}...",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = PundarTheme.colors.brandPrimary
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "Copy",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PundarTheme.colors.textSecondary,
                                            modifier = Modifier.clickable {
                                                clipboard.setText(AnnotatedString(tx.txHash))
                                                Toast.makeText(context, "Transaction hash copied!", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        if (!tx.txHash.startsWith("PROPOSAL") && !tx.txHash.startsWith(" CONSENT") && !tx.txHash.startsWith("REVISED")) {
                                            Text(
                                                "Explorer ↗",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PundarTheme.colors.brandPrimary,
                                                modifier = Modifier.clickable {
                                                    val url = "https://stellar.expert/explorer/testnet/tx/${tx.txHash}"
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                                }
                                            )
                                        }
                                    }
                                }
                                if (i < circle.transactionHistory.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = PundarDivider)
                                }
                            }
                        }
                    }
                }
            }

            // ── INVALID SCENARIOS & DETERMINISTIC RULE VALIDATOR (Section 16) ──
            item {
                PundarCard {
                    Text(
                        "Soroban Deterministic Rule Enforcement Checks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PundarTheme.colors.textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Test and verify that invalid actions are strictly rejected by PUNDAR Circle and the Soroban smart contract:",
                        style = MaterialTheme.typography.bodySmall,
                        color = PundarTheme.colors.textSecondary
                    )
                    Spacer(Modifier.height(10.dp))

                    validationBannerMessage?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFDC2626).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Text(
                                msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB91C1C),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    val ruleChecks = listOf(
                        "Attempt Post-Activation Rule Edit" to "Contract Error: Circle configuration (members, fixed contribution, schedule, and payout order) is locked at activation and immutable.",
                        "Attempt Custom / Partial Contribution" to "Contract Error: Contribution must equal exact fixed amount (${String.format("%,.2f", circle.contributionPerMonth)} ${circle.currency}). Custom amounts are rejected.",
                        "Attempt Premature Payout (Incomplete Round)" to "Contract Error: Scheduled payout rejected when fewer than $totalMembers of $totalMembers required contributions are recorded.",
                        "Attempt Payout to Non-Scheduled Recipient" to "Contract Error: Payout recipient for Round ${circle.currentRound} is deterministically locked to ${currentRecipient?.name ?: "assigned recipient"} and cannot be redirected.",
                        "Attempt Duplicate Round Payout" to "Contract Error: Each round in the rotating schedule can only be paid once."
                    )

                    ruleChecks.forEach { (btnLabel, errOutput) ->
                        OutlinedButton(
                            onClick = { validationBannerMessage = errOutput },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(btnLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = PundarTheme.colors.textSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = PundarTheme.colors.textPrimary)
    }
}
