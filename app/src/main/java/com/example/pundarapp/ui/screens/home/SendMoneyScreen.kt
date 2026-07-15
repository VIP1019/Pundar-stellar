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
import com.example.pundarapp.data.stellar.StellarWalletManager
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.HomeActivity
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendMoneyScreen(navController: NavController) {
    var recipientPhone by remember { mutableStateOf("") }
    var amount         by remember { mutableStateOf("") }
    var message        by remember { mutableStateOf("") }
    var isSending      by remember { mutableStateOf(false) }
    var showPinDialog  by remember { mutableStateOf(false) }
    var mpinInput      by remember { mutableStateOf("") }
    var errorMsg       by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // PIN confirmation dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false; isSending = false; mpinInput = "" },
            containerColor   = Navy800,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text("Confirm Transfer", fontWeight = FontWeight.Bold, color = TextWhite)
            },
            text = {
                Column {
                    Text(
                        "Sending ₱${String.format("%,.2f", amount.toDoubleOrNull() ?: 0.0)} to $recipientPhone",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSoft
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
                            unfocusedBorderColor    = NavyBorder,
                            focusedContainerColor   = Navy700,
                            unfocusedContainerColor = Navy700,
                            focusedTextColor        = TextWhite,
                            unfocusedTextColor      = TextWhite,
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
                                val users     = AuthRepository.searchUsers(recipientPhone)
                                val recipient = users.find { it.phone == recipientPhone.trim() }
                                if (recipient == null) {
                                    errorMsg  = "No PUNDAR user found with that number."
                                    isSending = false
                                    return@launch
                                }
                                val recipientPk = recipient.stellarPublicKey
                                if (recipientPk.isNullOrBlank()) {
                                    errorMsg  = "Recipient wallet not found."
                                    isSending = false
                                    return@launch
                                }
                                val senderPk   = AuthRepository.getCurrentUserStellarPublicKey() ?: ""
                                val senderSeed = AuthRepository.getCurrentUserEncryptedSeed() ?: ""
                                val result     = StellarWalletManager.sendPayment(
                                    senderPublicKey     = senderPk,
                                    senderEncryptedSeed = senderSeed,
                                    senderMpin          = mpinInput,
                                    recipientPublicKey  = recipientPk,
                                    amountXlm           = amount,
                                    memo                = message.take(28)
                                )
                                if (result.isSuccess) {
                                    AppState.refreshWalletBalance()
                                    val sentAmount = amount.toDoubleOrNull() ?: 0.0
                                    AppState.processPayRoundUp(
                                        sourceReference = "PAY-${UUID.randomUUID().toString().take(8).uppercase()}",
                                        sourceAmount = sentAmount,
                                        sourceLabel = "Send Money"
                                    )
                                    val activity = HomeActivity(
                                        icon      = "send",
                                        title     = "Sent to ${recipient.name}",
                                        subtitle  = message.ifBlank { "Money Transfer" },
                                        amount    = "-₱${String.format("%,.2f", amount.toDoubleOrNull() ?: 0.0)}",
                                        isPositive = false,
                                        module    = "Wallet"
                                    )
                                    AppState.homeActivities.add(0, activity)
                                    val uid = AuthRepository.getCurrentUserId()
                                    if (uid != null) HomeRepository.createActivity(uid, activity)
                                    AppState.requestHomeRefresh()
                                    Toast.makeText(context, "Money sent successfully!", Toast.LENGTH_SHORT).show()
                                    navController.navigateUp()
                                } else {
                                    errorMsg  = result.exceptionOrNull()?.message ?: "Transfer failed."
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
                    colors = ButtonDefaults.buttonColors(containerColor = Blue500)
                ) { Text("Confirm", color = White, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false; isSending = false; mpinInput = "" }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            PundarDetailTopBar(title = "Send Money", onBack = { navController.navigateUp() })
        },
        containerColor = Navy900,
        bottomBar = {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                PundarPrimaryButton(
                    text    = if (isSending) "Sending…" else "Send Money",
                    enabled = !isSending && recipientPhone.isNotBlank()
                              && (amount.toDoubleOrNull() ?: 0.0) > 0,
                    onClick = {
                        val sendAmt = amount.toDoubleOrNull() ?: 0.0
                        if (sendAmt <= 0) {
                            errorMsg = "Enter a valid amount."
                            return@PundarPrimaryButton
                        }
                        val roundUp = AppState.calculateRoundUpAmount(sendAmt)
                        if (sendAmt + roundUp > AppState.walletBalance.value) {
                            errorMsg = "Insufficient balance."
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
                        style = MaterialTheme.typography.labelSmall, color = Blue200.copy(0.75f))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        AppState.getDisplayBalance(),
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color      = White
                    )
                }
            }

            // Recipient field with QR scan shortcut
            Text("Recipient", style = MaterialTheme.typography.labelMedium,
                color = TextSoft, fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value         = recipientPhone,
                onValueChange = { recipientPhone = it },
                placeholder   = { Text("Mobile number (e.g. 09171234567)", color = TextDim) },
                leadingIcon   = { Icon(Icons.Filled.Person, null, tint = TextMuted) },
                trailingIcon  = {
                    // QR scan shortcut
                    IconButton(onClick = { navController.navigate(Routes.SCAN) }) {
                        Icon(Icons.Filled.QrCodeScanner, "Scan QR", tint = Blue400)
                    }
                },
                modifier        = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Blue400,
                    unfocusedBorderColor    = NavyBorder,
                    focusedContainerColor   = Navy700,
                    unfocusedContainerColor = Navy700,
                    focusedTextColor        = TextWhite,
                    unfocusedTextColor      = TextWhite,
                    cursorColor             = Blue400
                ),
                shape      = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Amount field
            Text("Amount", style = MaterialTheme.typography.labelMedium,
                color = TextSoft, fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value         = amount,
                onValueChange = {
                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = it
                },
                placeholder   = { Text("₱ 0.00", color = TextDim) },
                leadingIcon   = {
                    Text("₱", color = TextSoft, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp))
                },
                modifier        = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Blue400,
                    unfocusedBorderColor    = NavyBorder,
                    focusedContainerColor   = Navy700,
                    unfocusedContainerColor = Navy700,
                    focusedTextColor        = TextWhite,
                    unfocusedTextColor      = TextWhite,
                    cursorColor             = Blue400
                ),
                shape      = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Quick amount chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("100", "200", "500", "1000").forEach { preset ->
                    FilterChip(
                        selected = amount == preset,
                        onClick  = { amount = preset },
                        label    = { Text("₱$preset", fontSize = 11.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Blue500.copy(0.20f),
                            selectedLabelColor     = Blue300,
                            containerColor         = Navy700,
                            labelColor             = TextMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled         = true,
                            selected        = amount == preset,
                            borderColor     = NavyBorder,
                            selectedBorderColor = Blue400.copy(0.5f)
                        )
                    )
                }
            }

            // Message field
            Text("Note (Optional)", style = MaterialTheme.typography.labelMedium,
                color = TextSoft, fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value         = message,
                onValueChange = { if (it.length <= 50) message = it },
                placeholder   = { Text("What's this for?", color = TextDim) },
                modifier      = Modifier.fillMaxWidth(),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Blue400,
                    unfocusedBorderColor    = NavyBorder,
                    focusedContainerColor   = Navy700,
                    unfocusedContainerColor = Navy700,
                    focusedTextColor        = TextWhite,
                    unfocusedTextColor      = TextWhite,
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
                        .border(1.dp, Red500.copy(0.30f), RoundedCornerShape(10.dp))
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
                    .background(Blue500.copy(0.08f))
                    .border(1.dp, Blue400.copy(0.20f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.QrCodeScanner, null, tint = Blue400, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Tap the QR icon to scan recipient's code.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Blue300
                )
            }
        }
    }
}
