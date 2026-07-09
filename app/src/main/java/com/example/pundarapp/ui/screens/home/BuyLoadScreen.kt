package com.example.pundarapp.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.components.PundarPrimaryButton
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.HomeActivity
import com.example.pundarapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyLoadScreen(navController: NavController) {
    var phoneNumber by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Buy Load",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarBackground,
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                PundarPrimaryButton(
                    text = "Buy Load",
                    enabled = phoneNumber.isNotBlank() && amount.isNotBlank(),
                    onClick = {
                        val loadAmount = amount.toDoubleOrNull() ?: 0.0
                        if (loadAmount <= 0) {
                            Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                            return@PundarPrimaryButton
                        }
                        if (loadAmount > AppState.walletBalance.value) {
                            Toast.makeText(context, "Insufficient balance", Toast.LENGTH_SHORT).show()
                            return@PundarPrimaryButton
                        }

                        // Deduct balance
                        AppState.walletBalance.value -= loadAmount
                        
                        // Log activity
                        AppState.homeActivities.add(
                            0,
                            HomeActivity(
                                icon = "phone_android",
                                title = "Load Purchase",
                                subtitle = "To: $phoneNumber",
                                amount = "-₱ ${String.format("%,.2f", loadAmount)}",
                                isPositive = false,
                                module = "Wallet"
                            )
                        )

                        Toast.makeText(context, "Load successfully purchased!", Toast.LENGTH_SHORT).show()
                        navController.navigateUp()
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
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Mobile Number") },
                placeholder = { Text("e.g. 09123456789") },
                leadingIcon = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PundarBlue, focusedLabelColor = PundarBlue),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Load Amount (₱)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PundarBlue, focusedLabelColor = PundarBlue),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }
    }
}
