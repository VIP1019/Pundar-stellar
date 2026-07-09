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
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.components.PundarPrimaryButton
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.HomeActivity
import com.example.pundarapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendMoneyScreen(navController: NavController) {
    var amount by remember { mutableStateOf("") }
    var recipientPhone by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSending by remember { mutableStateOf(false) }

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
                        if (sendAmount > AppState.walletBalance.value) {
                            Toast.makeText(context, "Insufficient balance", Toast.LENGTH_SHORT).show()
                            return@PundarPrimaryButton
                        }

                        isSending = true
                        coroutineScope.launch {
                            // Check if recipient exists
                            val users = AuthRepository.searchUsers(recipientPhone)
                            val recipient = users.find { it.phone == recipientPhone.trim() }

                            if (recipient == null) {
                                Toast.makeText(context, "User not found with this number.", Toast.LENGTH_SHORT).show()
                                isSending = false
                                return@launch
                            }

                            // Deduct balance
                            AppState.walletBalance.value -= sendAmount
                            
                            // Log activity
                            AppState.homeActivities.add(
                                0,
                                HomeActivity(
                                    icon = "send",
                                    title = "Sent to ${recipient.name}",
                                    subtitle = message.ifBlank { "Send Money" },
                                    amount = "-₱ ${String.format("%,.2f", sendAmount)}",
                                    isPositive = false,
                                    module = "Wallet"
                                )
                            )

                            Toast.makeText(context, "Money sent successfully!", Toast.LENGTH_SHORT).show()
                            navController.navigateUp()
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
                    Text("₱ ${String.format("%,.2f", AppState.walletBalance.value)}", 
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = PundarBlue)
                }
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
