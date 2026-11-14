package com.example.pundarapp.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.data.qr.QrPayload
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.remote.BillQrRepository
import com.example.pundarapp.data.remote.HomeRepository
import com.example.pundarapp.data.remote.QrPaymentRepository
import com.example.pundarapp.data.remote.TransactionPinRepository
import com.example.pundarapp.data.stellar.PayStellarClient
import com.example.pundarapp.data.stellar.PaymentResult
import com.example.pundarapp.data.stellar.StellarWalletManager
import com.example.pundarapp.data.stellar.Stroops
import com.example.pundarapp.ui.components.FuturisticIcon
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.components.PundarPrimaryButton
import com.example.pundarapp.ui.components.PundarSecondaryButton
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.BillMember
import com.example.pundarapp.ui.data.BillStatus
import com.example.pundarapp.ui.data.GroupBill
import com.example.pundarapp.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class QrActionMode {
    SEND_PAYMENT,
    JOIN_PAY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrSendConfirmScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Cache the payload in local composition state so clearing AppState on completion
    // does not trigger a false "Invalid QR session" during exit recomposition.
    val payload = remember { AppState.pendingQrPayload.value }
    var isCompleted by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            AppState.pendingQrPayload.value = null
        }
    }

    if (payload == null) {
        LaunchedEffect(Unit) {
            if (!isCompleted) {
                Toast.makeText(context, "No QR session found.", Toast.LENGTH_SHORT).show()
                navController.navigateUp()
            }
        }
        return
    }

    // Determine initial mode based on payload metadata
    val isBillDetected = payload.type == QrPayload.TYPE_GROUP_BILL_JOIN ||
            payload.type == QrPayload.TYPE_BILL_PAYMENT ||
            !payload.billRef.isNullOrBlank()

    var activeMode by remember {
        mutableStateOf(if (isBillDetected) QrActionMode.JOIN_PAY else QrActionMode.SEND_PAYMENT)
    }

    val initialAmountPhp = remember(payload) {
        payload.amount?.takeIf { it > 0.0 }?.let { raw ->
            if (payload.currency.equals("XLM", ignoreCase = true)) AppState.xlmToPhp(raw) else raw
        }
    }

    var amount by remember {
        mutableStateOf(
            if (initialAmountPhp != null) String.format(java.util.Locale.US, "%.2f", initialAmountPhp) else ""
        )
    }
    var note by remember {
        mutableStateOf(if (payload.type == QrPayload.TYPE_STELLAR_ADDRESS) payload.billRef.orEmpty() else "")
    }
    var billTitle by remember {
        mutableStateOf(payload.billRef ?: if (payload.displayName != null) "Split with ${payload.displayName}" else "Group Split")
    }

    var pin by remember { mutableStateOf("") }
    var showPinDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var recipientName by remember { mutableStateOf(payload.displayName ?: "PUNDAR User") }
    var isJoinSettleNow by remember { mutableStateOf(false) }

    LaunchedEffect(payload.userId) {
        AppState.fetchExchangeRate()
        QrPaymentRepository.lookupRecipient(payload.userId).onSuccess {
            recipientName = it.displayName
        }
    }

    // PIN Verification Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                isProcessing = false
                pin = ""
            },
            title = {
                Text(
                    text = if (isJoinSettleNow) "Pay Share & Join" else "Confirm Payment",
                    fontWeight = FontWeight.Bold,
                    color = PundarTheme.colors.textPrimary
                )
            },
            text = {
                Column {
                    val dialogXlm = AppState.phpToXlm(amount.toDoubleOrNull() ?: 0.0)
                    Text(
                        text = if (isJoinSettleNow)
                            "Enter your 4-digit PIN to pay your share of ₱$amount (≈ ${String.format("%,.4f", dialogXlm)} XLM) and join the bill."
                        else
                            "Enter your 4-digit PIN to send ₱$amount (≈ ${String.format("%,.4f", dialogXlm)} XLM) to $recipientName.",
                        color = PundarTheme.colors.textSecondary
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4) pin = it },
                        label = { Text("4-digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pin.length == 4 && !isProcessing,
                    onClick = {
                        scope.launch {
                            isProcessing = true
                            val senderId = AuthRepository.getCurrentUserId() ?: AuthRepository.getCurrentUserPhone()
                            val senderPhone = AuthRepository.getCurrentUserPhone()
                            val senderSeed = AuthRepository.getCurrentUserEncryptedSeed().orEmpty()

                            // Decrypt Stellar seed or verify PIN strictly
                            val decryptedSecret: CharArray? = if (senderSeed.isNotBlank()) {
                                try {
                                    StellarWalletManager.decryptSecretSeed(senderSeed, pin)
                                } catch (_: Exception) {
                                    null
                                }
                            } else null

                            val isPinValid = if (senderSeed.isNotBlank()) {
                                decryptedSecret != null
                            } else {
                                val verify = TransactionPinRepository.verifyPin(senderPhone, pin)
                                (verify.isSuccess && verify.getOrNull() == true) ||
                                        (AuthRepository.verifyMpin(pin).getOrNull() == true)
                            }

                            if (!isPinValid) {
                                Toast.makeText(context, "Incorrect PIN.", Toast.LENGTH_SHORT).show()
                                isProcessing = false
                                pin = ""
                                return@launch
                            }

                            val sendAmount = amount.toDoubleOrNull() ?: 0.0
                            val sendAmountXlm = AppState.phpToXlm(sendAmount)
                            val recipientResult = QrPaymentRepository.lookupRecipient(payload.userId)
                            val recipient = recipientResult.getOrElse {
                                com.example.pundarapp.data.remote.QrRecipient(
                                    userId = payload.userId,
                                    displayName = recipientName,
                                    walletId = payload.walletId
                                )
                            }

                            val precondition = QrPaymentRepository.validateTransferPreconditions(
                                senderId = senderId,
                                recipient = recipient,
                                amount = sendAmount,
                                qrPayload = payload
                            )
                            if (precondition.isFailure) {
                                Toast.makeText(
                                    context,
                                    precondition.exceptionOrNull()?.message ?: "Validation failed",
                                    Toast.LENGTH_LONG
                                ).show()
                                isProcessing = false
                                showPinDialog = false
                                pin = ""
                                return@launch
                            }

                            // Resolve recipient Stellar public key (G...) across all QR payload / profile fields
                            val recipientStellarKey = listOfNotNull(
                                recipient.walletId,
                                payload.walletId,
                                recipient.userId,
                                payload.userId
                            ).firstOrNull { it.startsWith("G") && it.length == 56 }
                                ?: payload.username?.takeIf { it.isNotBlank() }?.let {
                                    AuthRepository.findRegisteredUser(it)?.stellarPublicKey
                                }?.takeIf { it.startsWith("G") && it.length == 56 }
                                ?: AuthRepository.findRegisteredUser(payload.userId)?.stellarPublicKey?.takeIf {
                                    it.startsWith("G") && it.length == 56
                                }

                            val memoText = (if (isJoinSettleNow) billTitle else note.ifBlank { payload.billRef ?: "PUNDAR QR" }).take(28)

                            val transfer: Result<String> = if (decryptedSecret != null && recipientStellarKey != null) {
                                val stroops = Stroops.fromXlmString(String.format(java.util.Locale.US, "%.7f", sendAmountXlm))
                                if (stroops == null || stroops <= 0) {
                                    Result.failure(Exception("Enter a valid amount."))
                                } else {
                                    when (val payRes = PayStellarClient.sendPayment(
                                        secret = decryptedSecret,
                                        destination = recipientStellarKey,
                                        amountStroops = stroops,
                                        memo = memoText
                                    )) {
                                        is PaymentResult.Success -> {
                                            // Sync Firestore metadata (bill_qrs PAID status, group_bills SETTLED, notifications)
                                            QrPaymentRepository.transferMoney(
                                                senderId = senderId,
                                                senderName = AuthRepository.getCurrentUserName(),
                                                recipient = recipient,
                                                amount = sendAmount,
                                                notes = if (isJoinSettleNow) billTitle else note.ifBlank { null },
                                                qrPayload = payload,
                                                onChainTxHash = payRes.hash
                                            )
                                        }
                                        is PaymentResult.Failure -> Result.failure(Exception(payRes.message))
                                        is PaymentResult.Unknown -> Result.failure(Exception("Network timed out. Check your balance before retrying."))
                                    }
                                }
                            } else {
                                QrPaymentRepository.transferMoney(
                                    senderId = senderId,
                                    senderName = AuthRepository.getCurrentUserName(),
                                    recipient = recipient,
                                    amount = sendAmount,
                                    notes = if (isJoinSettleNow) billTitle else note.ifBlank { null },
                                    qrPayload = payload
                                )
                            }

                            isProcessing = false
                            showPinDialog = false
                            val usedPin = pin
                            pin = ""

                            transfer.onSuccess { ref ->
                                isCompleted = true
                                AppState.updateWalletBalance((AppState.walletBalance.value - sendAmountXlm).coerceAtLeast(0.0))
                                AppState.refreshWalletBalance()
                                AppState.processPayRoundUp(
                                    sourceReference = ref,
                                    sourceAmount = sendAmount,
                                    sourceLabel = if (isJoinSettleNow) "Join & Pay" else "QR Payment",
                                    mpin = usedPin
                                )

                                if (isJoinSettleNow) {
                                    // Join bill and mark settled
                                    val totalParticipants = (payload.participantCount ?: 2).coerceAtLeast(1)
                                    val totalBillAmt = payload.totalBillAmount ?: (sendAmount * totalParticipants)
                                    val newBill = GroupBill(
                                        id = payload.transactionId ?: "BILL-${UUID.randomUUID().toString().take(8).uppercase()}",
                                        name = billTitle.ifBlank { "Group Bill (${recipient.displayName})" },
                                        totalAmount = totalBillAmt,
                                        memberCount = totalParticipants,
                                        status = BillStatus.SETTLED,
                                        date = "Today",
                                        yourShare = sendAmount,
                                        members = listOf(
                                            BillMember(name = recipient.displayName, username = recipient.userId, initials = recipient.displayName.take(2).uppercase(), amount = sendAmount),
                                            BillMember(name = AuthRepository.getCurrentUserName(), username = AuthRepository.getCurrentUserPhone(), initials = AuthRepository.getCurrentUserInitials(), amount = sendAmount)
                                        )
                                    )
                                    AppState.bills.add(0, newBill)
                                    val activity = QrPaymentRepository.homeActivityForBillPayment(
                                        billTitle.ifBlank { "Group Pay" },
                                        sendAmount
                                    )
                                    AppState.homeActivities.add(0, activity)
                                    HomeRepository.createActivity(senderId, activity)
                                    Toast.makeText(context, "Joined & Paid! Ref: ${ref.take(10)}", Toast.LENGTH_LONG).show()
                                } else if (payload.type == QrPayload.TYPE_BILL_PAYMENT) {
                                    BillQrRepository.getRecord(payload.securityToken)?.billId?.let {
                                        AppState.markBillSettled(it)
                                    }
                                    val activity = QrPaymentRepository.homeActivityForBillPayment(
                                        payload.billRef ?: "Bill",
                                        sendAmount
                                    )
                                    AppState.homeActivities.add(0, activity)
                                    HomeRepository.createActivity(senderId, activity)
                                    Toast.makeText(context, "Bill Paid! Ref: ${ref.take(10)}", Toast.LENGTH_LONG).show()
                                } else {
                                    val activity = QrPaymentRepository.homeActivityForSend(recipient.displayName, sendAmount)
                                    AppState.homeActivities.add(0, activity)
                                    HomeRepository.createActivity(senderId, activity)
                                    Toast.makeText(context, "Payment Sent! Ref: ${ref.take(10)}", Toast.LENGTH_LONG).show()
                                }

                                AppState.requestHomeRefresh()
                                AppState.refreshNotifications()
                                navController.navigateUp()
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Transfer failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Confirm", fontWeight = FontWeight.Bold, color = PundarTheme.colors.brandPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialog = false
                    isProcessing = false
                    pin = ""
                }) {
                    Text("Cancel", color = PundarTheme.colors.textSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = if (activeMode == QrActionMode.JOIN_PAY) "Join a Pay" else "Send Payment",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarTheme.colors.bgPrimary,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PundarTheme.colors.bgPrimary)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activeMode == QrActionMode.JOIN_PAY) {
                    // Two clear options when joining a bill:
                    // 1. Join & Pay Now (Primary)
                    // 2. Join Bill (Pay Later) (Secondary)
                    PundarPrimaryButton(
                        text = if (isProcessing) "Processing..." else "Join & Pay Now (₱${amount.ifBlank { "0.00" }})",
                        enabled = !isProcessing,
                        isLoading = isProcessing,
                        onClick = {
                            val shareAmount = amount.toDoubleOrNull() ?: 0.0
                            if (shareAmount <= 0) {
                                Toast.makeText(context, "Please enter a valid share amount.", Toast.LENGTH_SHORT).show()
                                return@PundarPrimaryButton
                            }
                            if (AppState.phpToXlm(shareAmount) > AppState.walletBalance.value) {
                                Toast.makeText(context, "Insufficient balance to settle now.", Toast.LENGTH_SHORT).show()
                                return@PundarPrimaryButton
                            }
                            isJoinSettleNow = true
                            showPinDialog = true
                        }
                    )

                    PundarSecondaryButton(
                        text = "Join Bill (Pay Later)",
                        onClick = {
                            val shareAmount = amount.toDoubleOrNull() ?: 0.0
                            if (shareAmount <= 0) {
                                Toast.makeText(context, "Please enter a valid share amount.", Toast.LENGTH_SHORT).show()
                                return@PundarSecondaryButton
                            }
                            isCompleted = true
                            val totalParticipants = (payload.participantCount ?: 2).coerceAtLeast(1)
                            val totalBillAmt = payload.totalBillAmount ?: (shareAmount * totalParticipants)
                            val newBill = GroupBill(
                                id = payload.transactionId ?: "BILL-${UUID.randomUUID().toString().take(8).uppercase()}",
                                name = billTitle.ifBlank { "Group Bill ($recipientName)" },
                                totalAmount = totalBillAmt,
                                memberCount = totalParticipants,
                                status = BillStatus.PENDING,
                                date = "Today",
                                yourShare = shareAmount,
                                members = listOf(
                                    BillMember(name = recipientName, username = payload.userId, initials = recipientName.take(2).uppercase(), amount = shareAmount),
                                    BillMember(name = AuthRepository.getCurrentUserName(), username = AuthRepository.getCurrentUserPhone(), initials = AuthRepository.getCurrentUserInitials(), amount = shareAmount)
                                )
                            )
                            AppState.bills.add(0, newBill)
                            Toast.makeText(context, "Joined bill! Added to Upcoming Obligations.", Toast.LENGTH_LONG).show()
                            AppState.requestHomeRefresh()
                            navController.navigateUp()
                        }
                    )
                } else {
                    // Send Payment Mode Action
                    PundarPrimaryButton(
                        text = if (isProcessing) "Processing..." else "Send ₱${amount.ifBlank { "0.00" }}",
                        enabled = !isProcessing,
                        isLoading = isProcessing,
                        onClick = {
                            val sendAmount = amount.toDoubleOrNull() ?: 0.0
                            if (sendAmount <= 0) {
                                Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                                return@PundarPrimaryButton
                            }
                            if (AppState.phpToXlm(sendAmount) > AppState.walletBalance.value) {
                                Toast.makeText(context, "Insufficient balance", Toast.LENGTH_SHORT).show()
                                return@PundarPrimaryButton
                            }
                            val myPhone = AuthRepository.getCurrentUserPhone()
                            val myId = AuthRepository.getCurrentUserId()
                            val myPk = AuthRepository.getCurrentUserStellarPublicKey()
                            if ((myPhone.isNotBlank() && payload.userId == myPhone) ||
                                (myId != null && payload.userId == myId) ||
                                (!myPk.isNullOrBlank() && (payload.userId == myPk || payload.walletId == myPk))
                            ) {
                                Toast.makeText(context, "You cannot send money to yourself.", Toast.LENGTH_SHORT).show()
                                return@PundarPrimaryButton
                            }
                            isJoinSettleNow = false
                            showPinDialog = true
                        }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Selector Pill (Send Payment vs Join a Pay)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PundarTheme.colors.surfaceSecondary)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activeMode == QrActionMode.SEND_PAYMENT) PundarTheme.colors.brandPrimary else Color.Transparent)
                        .clickable { activeMode = QrActionMode.SEND_PAYMENT },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = if (activeMode == QrActionMode.SEND_PAYMENT) Color.White else PundarTheme.colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Send Payment",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (activeMode == QrActionMode.SEND_PAYMENT) Color.White else PundarTheme.colors.textSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activeMode == QrActionMode.JOIN_PAY) PundarTheme.colors.brandPrimary else Color.Transparent)
                        .clickable { activeMode = QrActionMode.JOIN_PAY },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = if (activeMode == QrActionMode.JOIN_PAY) Color.White else PundarTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Join a Pay",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (activeMode == QrActionMode.JOIN_PAY) Color.White else PundarTheme.colors.textSecondary
                        )
                    }
                }
            }

            // Overview Card
            Card(
                colors = CardDefaults.cardColors(containerColor = PundarTheme.colors.surfacePrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeMode == QrActionMode.JOIN_PAY) {
                        FuturisticIcon(
                            icon = Icons.Default.Receipt,
                            tint = PundarTheme.colors.brandPrimary,
                            size = 48.dp,
                            iconSize = 24.dp
                        )
                    } else {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).background(PundarBlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                recipientName.split(" ").let { parts ->
                                    if (parts.size == 1) parts[0].take(1).uppercase()
                                    else (parts.first().take(1) + parts.last().take(1)).uppercase()
                                },
                                fontWeight = FontWeight.Bold,
                                color = PundarTheme.colors.brandPrimary
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (activeMode == QrActionMode.JOIN_PAY) "Host: $recipientName" else recipientName,
                                fontWeight = FontWeight.Bold,
                                color = PundarTheme.colors.textPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = payload.userId,
                            color = PundarTheme.colors.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Surface(
                        color = if (activeMode == QrActionMode.JOIN_PAY) PundarTheme.colors.brandPrimary.copy(alpha = 0.1f) else PundarTheme.colors.accentGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (activeMode == QrActionMode.JOIN_PAY) "Group Bill" else "Direct",
                            color = if (activeMode == QrActionMode.JOIN_PAY) PundarTheme.colors.brandPrimary else PundarTheme.colors.accentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            val isAmountLocked = payload.amount != null && payload.amount > 0.0
            val isBillTitleLocked = !payload.billRef.isNullOrBlank()

            // Mode-Specific Form Fields
            if (activeMode == QrActionMode.JOIN_PAY) {
                // Bill title field
                OutlinedTextField(
                    value = billTitle,
                    onValueChange = { if (!isBillTitleLocked) billTitle = it },
                    readOnly = isBillTitleLocked,
                    label = { Text("Bill / Event Title") },
                    trailingIcon = if (isBillTitleLocked) {
                        { Icon(Icons.Filled.Lock, contentDescription = "Fixed Title", tint = PundarTheme.colors.brandPrimary, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PundarTheme.colors.brandPrimary,
                        focusedLabelColor = PundarTheme.colors.brandPrimary,
                        focusedTextColor = PundarTheme.colors.textPrimary,
                        unfocusedTextColor = PundarTheme.colors.textPrimary
                    )
                )

                // Share amount input (Non-editable if settled by host)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (!isAmountLocked) amount = it },
                    readOnly = isAmountLocked,
                    label = { Text("Your Share Amount (₱)") },
                    trailingIcon = if (isAmountLocked) {
                        {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = "Amount Settled by Host",
                                tint = PundarTheme.colors.brandPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null,
                    supportingText = if (isAmountLocked) {
                        {
                            val lockedPhp = initialAmountPhp ?: payload.amount ?: 0.0
                            val splitNote = if (payload.totalBillAmount != null && payload.participantCount != null) {
                                " (Total ₱${String.format(java.util.Locale.US, "%,.2f", payload.totalBillAmount)} ÷ ${payload.participantCount} people)"
                            } else ""
                            Text(
                                "Amount settled by host: ₱${String.format(java.util.Locale.US, "%,.2f", lockedPhp)} (≈ ${String.format(java.util.Locale.US, "%,.4f", AppState.phpToXlm(lockedPhp))} XLM)$splitNote — not editable",
                                color = PundarTheme.colors.brandPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PundarTheme.colors.brandPrimary,
                        focusedLabelColor = PundarTheme.colors.brandPrimary,
                        focusedTextColor = PundarTheme.colors.textPrimary,
                        unfocusedTextColor = PundarTheme.colors.textPrimary
                    )
                )

                // Quick Split Chips (Only available if amount was not preset by host)
                if (!isAmountLocked) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("50", "100", "250", "500").forEach { quickVal ->
                            OutlinedButton(
                                onClick = { amount = quickVal },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (amount == quickVal) PundarTheme.colors.brandPrimary else PundarTheme.colors.surfaceSecondary
                                )
                            ) {
                                Text("₱$quickVal", fontSize = 12.sp, color = PundarTheme.colors.textPrimary)
                            }
                        }
                    }
                }
            } else {
                // Direct Send Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (!isAmountLocked) amount = it },
                    readOnly = isAmountLocked,
                    label = { Text("Amount to Send (₱)") },
                    trailingIcon = if (isAmountLocked) {
                        {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = "Fixed Amount",
                                tint = PundarTheme.colors.brandPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null,
                    supportingText = if (isAmountLocked) {
                        {
                            val lockedPhp = initialAmountPhp ?: payload.amount ?: 0.0
                            Text(
                                "Amount fixed by recipient: ₱${String.format(java.util.Locale.US, "%,.2f", lockedPhp)} (≈ ${String.format(java.util.Locale.US, "%,.4f", AppState.phpToXlm(lockedPhp))} XLM)",
                                color = PundarTheme.colors.brandPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PundarTheme.colors.brandPrimary,
                        focusedLabelColor = PundarTheme.colors.brandPrimary,
                        focusedTextColor = PundarTheme.colors.textPrimary,
                        unfocusedTextColor = PundarTheme.colors.textPrimary
                    )
                )

                // Quick Amount Chips (Only available if amount was not preset)
                if (!isAmountLocked) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("50", "100", "200", "500").forEach { quickVal ->
                            OutlinedButton(
                                onClick = { amount = quickVal },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (amount == quickVal) PundarTheme.colors.brandPrimary else PundarTheme.colors.surfaceSecondary
                                )
                            ) {
                                Text("₱$quickVal", fontSize = 12.sp, color = PundarTheme.colors.textPrimary)
                            }
                        }
                    }
                }

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Memo (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PundarTheme.colors.brandPrimary,
                        focusedLabelColor = PundarTheme.colors.brandPrimary,
                        focusedTextColor = PundarTheme.colors.textPrimary,
                        unfocusedTextColor = PundarTheme.colors.textPrimary
                    )
                )
            }

            // Available Balance Card
            Card(
                colors = CardDefaults.cardColors(containerColor = PundarTheme.colors.surfaceSecondary.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available Balance",
                        color = PundarTheme.colors.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "₱${String.format("%,.2f", AppState.xlmToPhp(AppState.walletBalance.value))} (≈ ${String.format("%,.2f", AppState.walletBalance.value)} XLM)",
                        color = PundarTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
