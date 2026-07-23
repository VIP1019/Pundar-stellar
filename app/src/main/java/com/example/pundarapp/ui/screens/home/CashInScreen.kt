package com.example.pundarapp.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.HomeActivity
import com.example.pundarapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class CashInStep { METHOD, AMOUNT, CONFIRM, RECEIPT }

data class CashInMethod(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val fee: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashInScreen(navController: NavController) {
    var currentStep by remember { mutableStateOf(CashInStep.METHOD) }
    var selectedMethod by remember { mutableStateOf<CashInMethod?>(null) }
    var amount by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val methods = listOf(
        CashInMethod("bank", "Bank Transfer", "InstaPay / PESONet", Icons.Filled.AccountBalance, ElectricBlue),
        CashInMethod("ewallet", "E-Wallet", "GCash, Maya, GrabPay", Icons.Filled.AccountBalanceWallet, NeonGreen),
        CashInMethod("otc", "Over-the-Counter", "7-Eleven, Cebuana, SM", Icons.Filled.Storefront, WarningAmber),
        CashInMethod("card", "Debit / Credit Card", "Visa, Mastercard", Icons.Filled.CreditCard, PremiumGoldWarm),
        CashInMethod("qr", "QR Deposit", "Scan to fund", Icons.Filled.QrCodeScanner, NeonCyan)
    )

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Cash In",
                onBack = {
                    when (currentStep) {
                        CashInStep.METHOD -> navController.navigateUp()
                        CashInStep.AMOUNT -> currentStep = CashInStep.METHOD
                        CashInStep.CONFIRM -> currentStep = CashInStep.AMOUNT
                        CashInStep.RECEIPT -> navController.navigateUp()
                    }
                }
            )
        },
        containerColor = PundarBackground,
        bottomBar = {
            if (currentStep != CashInStep.RECEIPT) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PundarPrimaryButton(
                        text = when (currentStep) {
                            CashInStep.METHOD -> "Continue"
                            CashInStep.AMOUNT -> "Review Cash In"
                            CashInStep.CONFIRM -> if (isProcessing) "Processing..." else "Confirm Cash In"
                            else -> ""
                        },
                        enabled = when (currentStep) {
                            CashInStep.METHOD -> selectedMethod != null
                            CashInStep.AMOUNT -> (amount.toDoubleOrNull() ?: 0.0) >= 100
                            CashInStep.CONFIRM -> !isProcessing
                            else -> false
                        },
                        onClick = {
                            when (currentStep) {
                                CashInStep.METHOD -> currentStep = CashInStep.AMOUNT
                                CashInStep.AMOUNT -> currentStep = CashInStep.CONFIRM
                                CashInStep.CONFIRM -> {
                                    isProcessing = true
                                    scope.launch {
                                        delay(1500) // Simulate processing
                                        val cashInAmt = amount.toDoubleOrNull() ?: 0.0
                                        if (cashInAmt > 0) {
                                            AppState.updateWalletBalance(AppState.walletBalance.value + cashInAmt)
                                            AppState.requestHomeRefresh()
                                        }
                                        isProcessing = false
                                        currentStep = CashInStep.RECEIPT
                                    }
                                }
                                else -> {}
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut()
                },
                label = "CashInFlow"
            ) { step ->
                when (step) {
                    CashInStep.METHOD -> MethodSelectionView(methods, selectedMethod) { selectedMethod = it }
                    CashInStep.AMOUNT -> AmountEntryView(selectedMethod, amount) { amount = it }
                    CashInStep.CONFIRM -> ConfirmationView(selectedMethod, amount)
                    CashInStep.RECEIPT -> ReceiptView(selectedMethod, amount) { navController.navigateUp() }
                }
            }
        }
    }
}

@Composable
private fun MethodSelectionView(
    methods: List<CashInMethod>,
    selectedMethod: CashInMethod?,
    onMethodSelected: (CashInMethod) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Select Cash In Method",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(16.dp))
        }

        items(methods.size) { index ->
            val method = methods[index]
            val isSelected = method.id == selectedMethod?.id

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onMethodSelected(method) },
                color = if (isSelected) method.color.copy(alpha = 0.15f) else SpaceMedium,
                shape = RoundedCornerShape(16.dp),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, method.color)
                else androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(method.color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(method.icon, contentDescription = null, tint = method.color, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            method.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            method.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    if (isSelected) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = method.color)
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountEntryView(
    selectedMethod: CashInMethod?,
    amount: String,
    onAmountChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PundarCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(selectedMethod?.icon ?: Icons.Filled.AccountBalance, null, tint = selectedMethod?.color ?: ElectricBlue)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Cash in via", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(selectedMethod?.name ?: "", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
            }
        }

        Spacer(Modifier.height(48.dp))

        Text("Enter Amount", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = {
                if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                    onAmountChange(it)
                }
            },
            prefix = { Text("₱ ", style = MaterialTheme.typography.displayMedium, color = TextPrimary) },
            textStyle = MaterialTheme.typography.displayMedium.copy(color = TextPrimary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = SpaceBorder)
        Spacer(Modifier.height(16.dp))
        
        val amt = amount.toDoubleOrNull() ?: 0.0
        if (amt < 100 && amount.isNotEmpty()) {
            Text("Minimum cash in amount is ₱100", color = ErrorRed, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ConfirmationView(
    selectedMethod: CashInMethod?,
    amount: String
) {
    val amt = amount.toDoubleOrNull() ?: 0.0
    val fee = selectedMethod?.fee ?: 0.0
    val total = amt + fee

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Confirm Cash In", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(24.dp))

        PundarCard {
            DetailRow("Method", selectedMethod?.name ?: "")
            Spacer(Modifier.height(12.dp))
            DetailRow("Amount", "₱${String.format("%,.2f", amt)}")
            Spacer(Modifier.height(12.dp))
            DetailRow("Fee", if (fee == 0.0) "FREE" else "₱${String.format("%,.2f", fee)}")
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = SpaceBorder)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total to Pay", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    "₱${String.format("%,.2f", total)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Placeholder alert
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(WarningAmber.copy(alpha = 0.15f))
                .border(1.dp, WarningAmber.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Info, null, tint = WarningAmber)
            Spacer(Modifier.width(12.dp))
            Text(
                "Cash in via ${selectedMethod?.name} will be fully available in a future update. This is a placeholder.",
                style = MaterialTheme.typography.bodySmall,
                color = WarningAmber
            )
        }
    }
}

@Composable
private fun ReceiptView(
    selectedMethod: CashInMethod?,
    amount: String,
    onDone: () -> Unit
) {
    val amt = amount.toDoubleOrNull() ?: 0.0
    val refNumber = "CI-${System.currentTimeMillis().toString().takeLast(8)}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(NeonGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.CheckCircle, null, tint = NeonGreen, modifier = Modifier.size(48.dp))
        }
        
        Spacer(Modifier.height(24.dp))
        
        Text("Cash In Successful", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Your wallet has been credited.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        
        Spacer(Modifier.height(32.dp))
        
        PundarCard(modifier = Modifier.fillMaxWidth()) {
            DetailRow("Amount", "₱${String.format("%,.2f", amt)}")
            Spacer(Modifier.height(12.dp))
            DetailRow("Method", selectedMethod?.name ?: "")
            Spacer(Modifier.height(12.dp))
            DetailRow("Reference No.", refNumber)
            Spacer(Modifier.height(12.dp))
            DetailRow("Date", "Just now")
        }
        
        Spacer(Modifier.height(48.dp))
        
        PundarPrimaryButton(text = "Done", onClick = onDone)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
