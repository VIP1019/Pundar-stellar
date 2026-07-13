package com.example.pundarapp.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.remote.HomeRepository
import com.example.pundarapp.data.stellar.StellarWalletManager
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.components.PundarPrimaryButton
import com.example.pundarapp.ui.components.PundarSmallButton
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.HomeActivity
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.QrCodeScanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendMoneyScreen(navController: NavController) {
    var amount by rememberSaveable { mutableStateOf("") }
    var recipientPhone by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSending by remember { mutableStateOf(false) }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val pinVerified = savedStateHandle?.get<Boolean>("pin_verified") ?: false
    val pin = savedStateHandle?.get<String>("pin")

    LaunchedEffect(pinVerified) {
        if (pinVerified && pin != null) {
            savedStateHandle?.remove<Boolean>("pin_verified")
            savedStateHandle?.remove<String>("pin")
            
            isSending = true
            coroutineScope.launch {
                try {
                    // 1. Check if recipient exists
                    val users = AuthRepository.searchUsers(recipientPhone)
                    val recipient = users.find { it.phone == recipientPhone.trim() }

                    if (recipient == null) {
                        Toast.makeText(context, "Recipient not found.", Toast.LENGTH_SHORT).show()
                        isSending = false
                        return@launch
                    }
                    
                    val recipientPk = recipient.stellarPublicKey
                    if (recipientPk.isNullOrBlank()) {
                        Toast.makeText(context, "Recipient not found.", Toast.LENGTH_SHORT).show()
                        isSending = false
                        return@launch
                    }

                    // 2. Get sender info
                    val senderPk = AuthRepository.getCurrentUserStellarPublicKey() ?: ""
                    val senderEncryptedSeed = AuthRepository.getCurrentUserEncryptedSeed() ?: ""
                    
                    // 3. Send Payment
                    val result = StellarWalletManager.sendPayment(
                        senderPublicKey = senderPk,
                        senderEncryptedSeed = senderEncryptedSeed,
                        senderMpin = pin,
                        recipientPublicKey = recipientPk,
                        amountXlm = amount,
                        memo = "PAY"
                    )

                    if (result.isSuccess) {
                        val txHash = result.getOrNull() ?: ""
                        
                        // 4. Refresh balance
                        AppState.refreshWalletBalance()
                        
                        // 5. Log activity
                        val activity = HomeActivity(
                            icon = "send",
                            title = "Sent to ${recipient.name}",
                            subtitle = "Tx: ${txHash.take(8)}...",
                            amount = "-₱${amount}",
                            isPositive = false,
                            module = "Wallet"
                        )
                        AppState.homeActivities.add(0, activity)
                        
                        // Persist activity
                        val userId = AuthRepository.getCurrentUserId()
                        if (userId != null) {
                            HomeRepository.createActivity(userId, activity)
                        }

                        AppState.requestHomeRefresh()
                        
                        Toast.makeText(context, "Money sent successfully!", Toast.LENGTH_SHORT).show()
                        navController.navigateUp()
                    } else {
                        val errorMsg = result.exceptionOrNull()?.message ?: "Payment failed"
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        isSending = false
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
                    isSending = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Send Money",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarBackground,
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                PundarPrimaryButton(
                    text = if (isSending) "Sending..." else "Send Money",
                    enabled = amount.isNotBlank() && recipientPhone.isNotBlank() && !isSending,
                    onClick = {
                        val sendAmount = amount.toDoubleOrNull() ?: 0.0
                        if (sendAmount <= 0) {
                            Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                            return@PundarPrimaryButton
                        }
                        // We check balance again in real time before showing dialog
                        coroutineScope.launch {
                            val currentBalance = AppState.walletBalance.value
                            if (sendAmount > currentBalance) {
                                Toast.makeText(context, "Insufficient balance", Toast.LENGTH_SHORT).show()
                            } else {
                                navController.navigate(Routes.PIN_VERIFICATION)
                            }
                        }
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
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PundarSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Available Balance", style = MaterialTheme.typography.labelMedium, color = PundarTextSecondary)
                    Text(AppState.getDisplayBalance(),
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = PundarBlue)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PundarSmallButton(
                    text = "Scan QR",
                    onClick = { navController.navigate(Routes.SCAN) },
                    modifier = Modifier.weight(1f)
                )
                PundarSmallButton(
                    text = "Select Contact",
                    onClick = { Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (₱)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PundarBlue, focusedLabelColor = PundarBlue),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = recipientPhone,
                onValueChange = { recipientPhone = it },
                label = { Text("Recipient Mobile Number") },
                placeholder = { Text("e.g. 09123456789") },
                leadingIcon = { Icon(Icons.Filled.AccountCircle, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PundarBlue, focusedLabelColor = PundarBlue),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PundarBlue, focusedLabelColor = PundarBlue),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
