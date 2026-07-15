package com.example.pundarapp.ui.screens.home

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.PundarCard
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.components.PundarPrimaryButton
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.HomeActivity
import com.example.pundarapp.ui.theme.*
import java.util.UUID

enum class MobileNetwork(val displayName: String, val color: Color) {
    UNKNOWN("Select Network", SpaceMedium),
    GLOBE("Globe / TM", ElectricBlue),
    SMART("Smart / TNT", NeonGreen),
    DITO("DITO", ErrorRed)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyLoadScreen(navController: NavController) {
    var phoneNumber by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedNetwork by remember { mutableStateOf(MobileNetwork.UNKNOWN) }
    
    val context = LocalContext.current
    
    // Auto-detect network based on prefix
    LaunchedEffect(phoneNumber) {
        if (phoneNumber.length >= 4) {
            val prefix = phoneNumber.take(4)
            selectedNetwork = when {
                prefix in listOf("0917", "0927", "0915", "0916", "0926", "0905", "0906", "0935", "0936", "0937", "0945", "0953", "0954", "0955", "0956", "0965", "0966", "0967", "0975", "0977", "0978", "0979", "0995", "0997") -> MobileNetwork.GLOBE
                prefix in listOf("0918", "0919", "0920", "0921", "0928", "0929", "0939", "0947", "0949", "0998", "0999", "0907", "0908", "0909", "0910", "0912", "0930", "0938", "0946", "0948", "0950") -> MobileNetwork.SMART
                prefix in listOf("0991", "0992", "0993", "0994", "0895", "0896", "0897", "0898") -> MobileNetwork.DITO
                else -> MobileNetwork.UNKNOWN
            }
        } else {
            selectedNetwork = MobileNetwork.UNKNOWN
        }
    }

    val presetAmounts = listOf(50, 100, 200, 300, 500, 1000)

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
                    text = "Purchase Load",
                    enabled = phoneNumber.length == 11 && amount.isNotBlank() && selectedNetwork != MobileNetwork.UNKNOWN,
                    onClick = {
                        val loadAmount = amount.toDoubleOrNull() ?: 0.0
                        if (loadAmount <= 0) {
                            Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                            return@PundarPrimaryButton
                        }
                        val roundUp = AppState.calculateRoundUpAmount(loadAmount)
                        if (loadAmount + roundUp > AppState.walletBalance.value) {
                            Toast.makeText(context, "Insufficient balance", Toast.LENGTH_SHORT).show()
                            return@PundarPrimaryButton
                        }

                        // Deduct balance
                        AppState.walletBalance.value -= loadAmount
                        AppState.processPayRoundUp(
                            sourceReference = "LOAD-${UUID.randomUUID().toString().take(8).uppercase()}",
                            sourceAmount = loadAmount,
                            sourceLabel = "Buy Load"
                        )
                        
                        // Log activity
                        AppState.homeActivities.add(
                            0,
                            HomeActivity(
                                icon = "phone_android",
                                title = "Load: ${selectedNetwork.displayName}",
                                subtitle = "To: $phoneNumber",
                                amount = "-₱${String.format("%,.2f", loadAmount)}",
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Balance Card
            PundarCard {
                Text("Available Balance", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text(
                    AppState.getDisplayBalance(),
                    style = MaterialTheme.typography.headlineMedium, 
                    fontWeight = FontWeight.Bold, 
                    color = ElectricBlue
                )
            }

            // Mobile Number Input
            Column {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { 
                        if (it.length <= 11 && it.all { char -> char.isDigit() }) {
                            phoneNumber = it 
                        }
                    },
                    label = { Text("Mobile Number") },
                    placeholder = { Text("09XX XXX XXXX") },
                    leadingIcon = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { Toast.makeText(context, "Contacts coming soon", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Filled.ContactPhone, contentDescription = "Select Contact", tint = ElectricBlue)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = selectedNetwork.color.takeIf { it != SpaceMedium } ?: ElectricBlue,
                        focusedLabelColor = selectedNetwork.color.takeIf { it != SpaceMedium } ?: ElectricBlue
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                AnimatedVisibility(visible = selectedNetwork != MobileNetwork.UNKNOWN) {
                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp, start = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = selectedNetwork.color, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = selectedNetwork.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = selectedNetwork.color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Amount Selection
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select Amount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(presetAmounts) { preset ->
                        val isSelected = amount == preset.toString()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) ElectricBlue.copy(alpha = 0.2f) else SpaceMedium)
                                .border(1.dp, if (isSelected) ElectricBlue else SpaceBorder, RoundedCornerShape(12.dp))
                                .clickable { amount = preset.toString() }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "₱$preset",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) ElectricBlue else TextPrimary
                            )
                        }
                    }
                }

                Text("Or enter other amount", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { 
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amount = it
                        }
                    },
                    prefix = { Text("₱ ") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue, 
                        focusedLabelColor = ElectricBlue
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }
    }
}
