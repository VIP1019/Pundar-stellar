package com.example.pundarapp.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pundarapp.data.qr.QrPayload
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.remote.BillQrRepository
import com.example.pundarapp.data.remote.QrPaymentRepository
import com.example.pundarapp.data.remote.TransactionPinRepository
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.components.PundarPrimaryButton
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrSendConfirmScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val payload = AppState.pendingQrPayload.value

    var amount by remember { mutableStateOf(payload?.amount?.toString() ?: "") }
    var note by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showPinDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var recipientName by remember { mutableStateOf(payload?.displayName ?: "") }

    LaunchedEffect(payload?.userId) {
        val userId = payload?.userId ?: return@LaunchedEffect
        QrPaymentRepository.lookupRecipient(userId).onSuccess {
            recipientName = it.displayName
        }
    }

    if (payload?.type == QrPayload.TYPE_BILL_PAYMENT && payload.amount != null) {
        LaunchedEffect(payload.amount) {
            amount = payload.amount.toString()
        }
    }

    if (payload == null) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "Invalid QR session.", Toast.LENGTH_SHORT).show()
            navController.navigateUp()
        }
        return
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                isProcessing = false
                pin = ""
            },
            title = { Text("Enter PIN") },
            text = {
                Column {
                    Text("Confirm transfer with your 4-digit PIN.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4) pin = it },
                        label = { Text("PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pin.length == 4 && !isProcessing,
                    onClick = {
                        scope.launch {
                            isProcessing = true
                            val senderId = AuthRepository.getCurrentUserId()
                                ?: run {
                                    Toast.makeText(context, "Not logged in.", Toast.LENGTH_SHORT).show()
                                    isProcessing = false
                                    return@launch
                                }
                            val senderPhone = AuthRepository.getCurrentUserPhone()
                            val verify = TransactionPinRepository.verifyPin(senderPhone, pin)
                            if (verify.isFailure || verify.getOrNull() != true) {
                                Toast.makeText(context, "Incorrect PIN.", Toast.LENGTH_SHORT).show()
                                isProcessing = false
                                pin = ""
                                return@launch
                            }

                            val sendAmount = amount.toDoubleOrNull() ?: 0.0
                            val recipientResult = QrPaymentRepository.lookupRecipient(payload.userId)
                            val recipient = recipientResult.getOrElse {
                                Toast.makeText(context, it.message ?: "Recipient error", Toast.LENGTH_SHORT).show()
                                isProcessing = false
                                pin = ""
                                return@launch
                            }

                            val transfer = QrPaymentRepository.transferMoney(
                                senderId = senderId,
                                senderName = AuthRepository.getCurrentUserName(),
                                recipient = recipient,
                                amount = sendAmount,
                                notes = note.ifBlank { null },
                                qrPayload = payload
                            )

                            isProcessing = false
                            showPinDialog = false
                            pin = ""

                            transfer.onSuccess { ref ->
                                AppState.updateWalletBalance(AppState.walletBalance.value - sendAmount)
                                if (payload.type == QrPayload.TYPE_BILL_PAYMENT) {
                                    BillQrRepository.getRecord(payload.securityToken)?.billId?.let {
                                        AppState.markBillSettled(it)
                                    }
                                    AppState.homeActivities.add(
                                        0,
                                        QrPaymentRepository.homeActivityForBillPayment(
                                            payload.billRef ?: "Bill",
                                            sendAmount
                                        )
                                    )
                                } else {
                                    AppState.homeActivities.add(
                                        0,
                                        QrPaymentRepository.homeActivityForSend(recipient.displayName, sendAmount)
                                    )
                                }
                                AppState.requestHomeRefresh()
                                AppState.refreshNotifications()
                                AppState.pendingQrPayload.value = null
                                Toast.makeText(context, "Sent! Ref: $ref", Toast.LENGTH_LONG).show()
                                navController.navigateUp()
                            }.onFailure {
                                Toast.makeText(context, it.message ?: "Transfer failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialog = false
                    isProcessing = false
                    pin = ""
                }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = if (payload.type == QrPayload.TYPE_BILL_PAYMENT) "Pay Bill" else "Send Money",
                onBack = {
                    AppState.pendingQrPayload.value = null
                    navController.navigateUp()
                }
            )
        },
        containerColor = PundarBackground,
        bottomBar = {
            Column(Modifier.padding(16.dp)) {
                PundarPrimaryButton(
                    text = if (isProcessing) "Processing..." else "Continue",
                    enabled = !isProcessing,
                    isLoading = isProcessing,
                    onClick = {
                        val sendAmount = amount.toDoubleOrNull() ?: 0.0
                        if (sendAmount <= 0) {
                            Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                            return@PundarPrimaryButton
                        }
                        if (sendAmount > AppState.walletBalance.value) {
                            Toast.makeText(context, "Insufficient balance", Toast.LENGTH_SHORT).show()
                            return@PundarPrimaryButton
                        }
                        if (payload.userId == AuthRepository.getCurrentUserId()) {
                            Toast.makeText(context, "You cannot send to yourself.", Toast.LENGTH_SHORT).show()
                            return@PundarPrimaryButton
                        }
                        showPinDialog = true
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = PundarSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            color = PundarBlue
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(recipientName, fontWeight = FontWeight.Bold, color = PundarTextPrimary)
                        Text(payload.userId, color = PundarTextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (payload.type == QrPayload.TYPE_BILL_PAYMENT) {
                Text("Bill: ${payload.billRef ?: ""}", color = PundarTextSecondary)
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (₱)") },
                enabled = payload.type != QrPayload.TYPE_BILL_PAYMENT,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Text(
                "Balance: ₱${String.format("%,.2f", AppState.walletBalance.value)}",
                color = PundarTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
