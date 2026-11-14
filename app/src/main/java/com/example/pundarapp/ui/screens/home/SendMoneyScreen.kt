package com.example.pundarapp.ui.screens.home

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.remote.HomeRepository
import com.example.pundarapp.data.stellar.PayStellarClient
import com.example.pundarapp.data.stellar.PaymentResult
import com.example.pundarapp.data.stellar.StellarWalletManager
import com.example.pundarapp.data.stellar.Stroops
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.HomeActivity
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendMoneyScreen(navController: NavController) {
    var recipientPhone by remember { mutableStateOf("") }
    var amount         by remember { mutableStateOf("") }
    var isPhpMode      by remember { mutableStateOf(true) }
    var message        by remember { mutableStateOf("") }
    var isSending      by remember { mutableStateOf(false) }
    var showPinDialog  by remember { mutableStateOf(false) }
    var mpinInput      by remember { mutableStateOf("") }
    var errorMsg       by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var successRefMsg  by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    LaunchedEffect(errorMsg) {
        if (!errorMsg.isNullOrBlank()) {
            showErrorDialog = true
        }
    }

    if (showErrorDialog && !errorMsg.isNullOrBlank()) {
        PundarStatusDialog(
            title = "Transfer Error",
            message = errorMsg!!,
            isError = true,
            onDismiss = { showErrorDialog = false }
        )
    }

    if (successRefMsg != null) {
        PundarStatusDialog(
            title = "Transfer Completed",
            message = successRefMsg!!,
            isError = false,
            buttonText = "Done",
            onDismiss = {
                successRefMsg = null
                navController.navigateUp()
            }
        )
    }

    LaunchedEffect(Unit) {
        AppState.fetchExchangeRate()
    }

    val phpRate = AppState.getPhpRate()
    val rawAmount = amount.toDoubleOrNull() ?: 0.0
    val sendAmountPhp = if (isPhpMode) rawAmount else rawAmount * phpRate
    val sendAmountXlm = if (isPhpMode) AppState.phpToXlm(rawAmount) else rawAmount

    // PIN confirmation dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false; isSending = false; mpinInput = "" },
            containerColor   = PundarTheme.colors.surfacePrimary,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text("Confirm Transfer", fontWeight = FontWeight.Bold, color = PundarTheme.colors.textPrimary)
            },
            text = {
                Column {
                    Text(
                        "Sending ₱${String.format("%,.2f", sendAmountPhp)} (≈ ${String.format("%,.4f", sendAmountXlm)} XLM) to $recipientPhone",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PundarTheme.colors.textSecondary
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value                = mpinInput,
                        onValueChange        = { if (it.length <= 4) mpinInput = it },
                        label                = { Text("Enter MPIN") },
                        keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier             = Modifier.fillMaxWidth(),
                        colors               = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = Blue400,
                            unfocusedBorderColor    = PundarTheme.colors.borderPrimary,
                            focusedContainerColor   = PundarTheme.colors.surfaceSecondary,
                            unfocusedContainerColor = PundarTheme.colors.surfaceSecondary,
                            focusedTextColor        = PundarTheme.colors.textPrimary,
                            unfocusedTextColor      = PundarTheme.colors.textPrimary,
                            cursorColor             = Blue400
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = mpinInput.length == 4,
                    onClick = {
                        showPinDialog = false
                        isSending     = true
                        scope.launch {
                            try {
                                val target = recipientPhone.trim()
                                val recipientPk: String
                                val recipientDisplayName: String

                                // Only registered PUNDAR accounts (by phone or Stellar address) can receive
                                val recipient = AuthRepository.findRegisteredUser(target)
                                if (recipient == null) {
                                    errorMsg  = "No registered PUNDAR account found for that number."
                                    isSending = false
                                    return@launch
                                }
                                val pk = recipient.stellarPublicKey
                                if (pk.isNullOrBlank()) {
                                    errorMsg  = "Recipient wallet not found."
                                    isSending = false
                                    return@launch
                                }
                                recipientPk = pk
                                recipientDisplayName = recipient.name

                                val senderPk   = AuthRepository.getCurrentUserStellarPublicKey() ?: ""
                                val senderSeed = AuthRepository.getCurrentUserEncryptedSeed() ?: ""
                                val xlmFormatted = String.format(java.util.Locale.US, "%.7f", sendAmountXlm)
                                val stroops    = Stroops.fromXlmString(xlmFormatted)
                                if (stroops == null || stroops <= 0) {
                                    errorMsg  = "Enter a valid amount."
                                    isSending = false
                                    return@launch
                                }
                                val secret = try {
                                    StellarWalletManager.decryptSecretSeed(senderSeed, mpinInput)
                                } catch (e: Exception) {
                                    errorMsg  = "Incorrect PIN."
                                    isSending = false
                                    return@launch
                                }
                                val result = PayStellarClient.sendPayment(
                                    secret      = secret,
                                    destination = recipientPk,
                                    amountStroops = stroops,
                                    memo        = message
                                )
                                if (result is PaymentResult.Success) {
                                    AppState.refreshWalletBalance()
                                    AppState.processPayRoundUp(
                                        sourceReference = "PAY-${UUID.randomUUID().toString().take(8).uppercase()}",
                                        sourceAmount = sendAmountPhp,
                                        sourceLabel = "Send Money",
                                        mpin = mpinInput
                                    )
                                    val activity = HomeActivity(
                                        icon      = "send",
                                        title     = "Sent to $recipientDisplayName",
                                        subtitle  = "${message.ifBlank { "Money Transfer" }} (${String.format("%,.2f", sendAmountXlm)} XLM)",
                                        amount    = "-₱${String.format("%,.2f", sendAmountPhp)}",
                                        isPositive = false,
                                        module    = "Wallet"
                                    )
                                    AppState.addHomeActivity(activity)
                                    val uid = AuthRepository.getCurrentUserId()
                                    if (uid != null) HomeRepository.createActivity(uid, activity)
                                    AppState.incrementPundarScore(2) // +2 points for sending money
                                    AppState.requestHomeRefresh()
                                    isSending = false
                                    successRefMsg = "Successfully sent ₱${String.format("%,.2f", sendAmountPhp)} (≈ ${String.format("%,.4f", sendAmountXlm)} XLM) to $recipientDisplayName."
                                } else {
                                    errorMsg = when (result) {
                                        is PaymentResult.Failure -> result.message
                                        is PaymentResult.Unknown -> "Network timed out. Check your balance before retrying."
                                        else -> "Transfer failed."
                                    }
                                    isSending = false
                                }
                            } catch (e: Exception) {
                                errorMsg  = e.message ?: "An error occurred."
                                isSending = false
                            } finally {
                                mpinInput = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PundarTheme.colors.brandPrimary)
                ) { Text("Confirm", color = PundarTheme.colors.surfacePrimary, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false; isSending = false; mpinInput = "" }) {
                    Text("Cancel", color = PundarTheme.colors.textMuted)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            PundarDetailTopBar(title = "Send Money", onBack = { navController.navigateUp() })
        },
        containerColor = PundarTheme.colors.bgPrimary,
        bottomBar = {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                PundarPrimaryButton(
                    text    = if (isSending) "Sending…" else "Send Money",
                    enabled = !isSending && recipientPhone.isNotBlank()
                              && rawAmount > 0,
                    onClick = {
                        if (rawAmount <= 0 || sendAmountXlm <= 0) {
                            errorMsg = "Enter a valid amount."
                            return@PundarPrimaryButton
                        }
                        val roundUpPhp = AppState.calculateRoundUpAmount(sendAmountPhp)
                        val roundUpXlm = AppState.phpToXlm(roundUpPhp)
                        if (sendAmountXlm + roundUpXlm > AppState.walletBalance.value) {
                            errorMsg = "Insufficient balance (${String.format("%,.2f", AppState.walletBalance.value)} XLM ≈ ₱${String.format("%,.2f", AppState.xlmToPhp(AppState.walletBalance.value))} available)."
                            return@PundarPrimaryButton
                        }
                        errorMsg = null
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Balance card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF0E2260), Color(0xFF1A3680))))
                    .border(1.dp, Blue400.copy(0.28f), RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Text("Available Balance",
                        style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.75f))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (AppState.isBalanceHidden.value) "₱ •••••" else "₱ ${String.format("%,.2f", AppState.xlmToPhp(AppState.walletBalance.value))}",
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (AppState.isBalanceHidden.value) "••••• XLM" else "≈ ${String.format("%,.2f", AppState.walletBalance.value)} XLM  •  1 XLM = ₱${String.format("%,.2f", phpRate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(0.75f)
                    )
                }
            }

            // Recipient field with QR scan shortcut
            Text("Recipient", style = MaterialTheme.typography.labelMedium,
                color = PundarTheme.colors.textSecondary, fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value         = recipientPhone,
                onValueChange = { recipientPhone = it },
                placeholder   = { Text("Mobile number or Stellar address (G...)", color = PundarTheme.colors.textDim) },
                leadingIcon   = { Icon(Icons.Filled.Person, null, tint = PundarTheme.colors.textMuted) },
                trailingIcon  = {
                    // QR scan shortcut
                    IconButton(onClick = { navController.navigate(Routes.SCAN) }) {
                        Icon(Icons.Filled.QrCodeScanner, "Scan QR", tint = Blue400)
                    }
                },
                modifier        = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Blue400,
                    unfocusedBorderColor    = PundarTheme.colors.borderPrimary,
                    focusedContainerColor   = PundarTheme.colors.surfaceSecondary,
                    unfocusedContainerColor = PundarTheme.colors.surfaceSecondary,
                    focusedTextColor        = PundarTheme.colors.textPrimary,
                    unfocusedTextColor      = PundarTheme.colors.textPrimary,
                    cursorColor             = Blue400
                ),
                shape      = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Amount field
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isPhpMode) "Amount (PHP)" else "Amount (XLM)",
                    style = MaterialTheme.typography.labelMedium,
                    color = PundarTheme.colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isPhpMode) "Switch to XLM" else "Switch to PHP (₱)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Blue400,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        isPhpMode = !isPhpMode
                        amount = ""
                    }
                )
            }
            OutlinedTextField(
                value         = amount,
                onValueChange = {
                    val regex = if (isPhpMode) Regex("^\\d*\\.?\\d{0,2}$") else Regex("^\\d*\\.?\\d{0,7}$")
                    if (it.isEmpty() || it.matches(regex)) amount = it
                },
                placeholder   = { Text(if (isPhpMode) "₱ 0.00" else "0.00 XLM", color = PundarTheme.colors.textDim) },
                leadingIcon   = {
                    Text(
                        if (isPhpMode) "₱" else "XLM",
                        color = PundarTheme.colors.textSecondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                supportingText = {
                    if (rawAmount > 0.0) {
                        Text(
                            text = if (isPhpMode)
                                "≈ ${String.format("%,.4f", sendAmountXlm)} XLM will be sent on Stellar"
                            else
                                "≈ ₱${String.format("%,.2f", sendAmountPhp)} (at ₱${String.format("%,.2f", phpRate)} / XLM)",
                            color = PundarTheme.colors.brandPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                modifier        = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Blue400,
                    unfocusedBorderColor    = PundarTheme.colors.borderPrimary,
                    focusedContainerColor   = PundarTheme.colors.surfaceSecondary,
                    unfocusedContainerColor = PundarTheme.colors.surfaceSecondary,
                    focusedTextColor        = PundarTheme.colors.textPrimary,
                    unfocusedTextColor      = PundarTheme.colors.textPrimary,
                    cursorColor             = Blue400
                ),
                shape      = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Quick amount chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val presets = if (isPhpMode) listOf("100", "200", "500", "1000") else listOf("10", "25", "50", "100")
                presets.forEach { preset ->
                    FilterChip(
                        selected = amount == preset,
                        onClick  = { amount = preset },
                        label    = { Text(if (isPhpMode) "₱$preset" else "$preset XLM", fontSize = 11.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PundarTheme.colors.brandPrimary.copy(0.20f),
                            selectedLabelColor     = PundarTheme.colors.brandLight,
                            containerColor         = PundarTheme.colors.surfaceSecondary,
                            labelColor             = PundarTheme.colors.textMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled         = true,
                            selected        = amount == preset,
                            borderColor     = PundarTheme.colors.borderPrimary,
                            selectedBorderColor = Blue400.copy(0.5f)
                        )
                    )
                }
            }

            // Message field
            Text("Note (Optional)", style = MaterialTheme.typography.labelMedium,
                color = PundarTheme.colors.textSecondary, fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value         = message,
                onValueChange = { if (it.length <= 50) message = it },
                placeholder   = { Text("What's this for?", color = PundarTheme.colors.textDim) },
                modifier      = Modifier.fillMaxWidth(),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Blue400,
                    unfocusedBorderColor    = PundarTheme.colors.borderPrimary,
                    focusedContainerColor   = PundarTheme.colors.surfaceSecondary,
                    unfocusedContainerColor = PundarTheme.colors.surfaceSecondary,
                    focusedTextColor        = PundarTheme.colors.textPrimary,
                    unfocusedTextColor      = PundarTheme.colors.textPrimary,
                    cursorColor             = Blue400
                ),
                shape     = RoundedCornerShape(14.dp),
                maxLines  = 2
            )

            // Error
            if (errorMsg != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RedBg)
                        .border(1.dp, PundarTheme.colors.accentRed.copy(0.30f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon3DWarning(size = 14.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(errorMsg!!, color = Red400, style = MaterialTheme.typography.bodySmall)
                }
            }

            // QR scan tip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PundarTheme.colors.brandPrimary.copy(0.08f))
                    .border(1.dp, Blue400.copy(0.20f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.QrCodeScanner, null, tint = Blue400, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Tap the QR icon to scan recipient's code.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PundarTheme.colors.brandLight
                )
            }
        }
    }
}
