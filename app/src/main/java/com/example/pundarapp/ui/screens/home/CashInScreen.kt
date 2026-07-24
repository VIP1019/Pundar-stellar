package com.example.pundarapp.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.HomeActivity
import com.example.pundarapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class CashInStep { METHOD, AMOUNT, CONFIRM, RECEIPT }

data class CashInMethod(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val fee: Double = 0.0,
    val processingTime: String = "Instant",
    val limit: String = "Up to ₱100,000/day"
)

// ═══════════════════════════════════════════════════════════════
//  CashInScreen
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashInScreen(navController: NavController) {
    var currentStep by remember { mutableStateOf(CashInStep.METHOD) }
    var selectedMethod by remember { mutableStateOf<CashInMethod?>(null) }
    var amount by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val refNumber = remember { "CI-${System.currentTimeMillis().toString().takeLast(8)}" }
    val timestamp = remember { System.currentTimeMillis() }

    val methods = listOf(
        CashInMethod(
            "bank", "Bank Transfer", "BDO, BPI, Metrobank, RCBC +more",
            Icons.Filled.AccountBalance, Color(0xFF1565C0),
            fee = 0.0, processingTime = "Instant (InstaPay)", limit = "Up to ₱50,000/day"
        ),
        CashInMethod(
            "ewallet", "E-Wallet", "GCash, Maya, GrabPay, ShopeePay",
            Icons.Filled.AccountBalanceWallet, Color(0xFF00875A),
            fee = 0.0, processingTime = "Instant", limit = "Up to ₱100,000/day"
        ),
        CashInMethod(
            "otc", "Over-the-Counter", "7-Eleven, Cebuana, LBC, SM, Palawan",
            Icons.Filled.Storefront, Color(0xFFE65100),
            fee = 15.0, processingTime = "Within 15 mins", limit = "Up to ₱50,000/day"
        ),
        CashInMethod(
            "card", "Debit / Credit Card", "Visa, Mastercard, JCB",
            Icons.Filled.CreditCard, Color(0xFF6200EA),
            fee = 0.0, processingTime = "Instant", limit = "Up to ₱100,000/day"
        ),
        CashInMethod(
            "qr", "QR Ph Deposit", "Scan via any bank or e-wallet app",
            Icons.Filled.QrCode, Color(0xFF0277BD),
            fee = 0.0, processingTime = "Instant", limit = "Up to ₱50,000/day"
        ),
        CashInMethod(
            "remittance", "Remittance", "Western Union, MoneyGram, Remitly",
            Icons.Filled.FlightLand, Color(0xFF558B2F),
            fee = 0.0, processingTime = "1–3 banking days", limit = "Up to ₱500,000/day"
        )
    )

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = when (currentStep) {
                    CashInStep.METHOD -> "Cash In"
                    CashInStep.AMOUNT -> "Enter Amount"
                    CashInStep.CONFIRM -> "Review & Confirm"
                    CashInStep.RECEIPT -> "Transaction Complete"
                },
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
        containerColor = PundarTheme.colors.bgPrimary,
        bottomBar = {
            if (currentStep != CashInStep.RECEIPT) {
                Surface(
                    shadowElevation = 8.dp,
                    color = PundarTheme.colors.surfacePrimary
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        // Step indicator
                        CashInStepIndicator(currentStep)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                when (currentStep) {
                                    CashInStep.METHOD -> currentStep = CashInStep.AMOUNT
                                    CashInStep.AMOUNT -> currentStep = CashInStep.CONFIRM
                                    CashInStep.CONFIRM -> {
                                        isProcessing = true
                                        scope.launch {
                                            delay(1800)
                                            val cashInAmt = amount.toDoubleOrNull() ?: 0.0
                                            if (cashInAmt > 0) {
                                                AppState.updateWalletBalance(AppState.walletBalance.value + cashInAmt)
                                                AppState.addHomeActivity(
                                                    HomeActivity(
                                                        icon = "account_balance_wallet",
                                                        title = "Cash In",
                                                        subtitle = "via ${selectedMethod?.name ?: ""}",
                                                        amount = "+₱${String.format("%,.2f", cashInAmt)}",
                                                        isPositive = true,
                                                        module = "Wallet",
                                                        timestamp = timestamp,
                                                        referenceId = refNumber
                                                    )
                                                )
                                                AppState.requestHomeRefresh()
                                            }
                                            isProcessing = false
                                            currentStep = CashInStep.RECEIPT
                                        }
                                    }
                                    else -> {}
                                }
                            },
                            enabled = when (currentStep) {
                                CashInStep.METHOD -> selectedMethod != null
                                CashInStep.AMOUNT -> (amount.toDoubleOrNull() ?: 0.0) >= 100
                                CashInStep.CONFIRM -> !isProcessing
                                else -> false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PundarTheme.colors.brandPrimary,
                                disabledContainerColor = PundarTheme.colors.surfaceTertiary
                            )
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Processing...", color = Color.White, fontWeight = FontWeight.Bold)
                            } else {
                                Text(
                                    text = when (currentStep) {
                                        CashInStep.METHOD -> "Continue"
                                        CashInStep.AMOUNT -> "Review Cash In"
                                        CashInStep.CONFIRM -> "Confirm Cash In"
                                        else -> ""
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "CashInFlow",
            modifier = Modifier.padding(padding)
        ) { step ->
            when (step) {
                CashInStep.METHOD -> MethodSelectionView(methods, selectedMethod) { selectedMethod = it }
                CashInStep.AMOUNT -> AmountEntryView(selectedMethod, amount) { amount = it }
                CashInStep.CONFIRM -> ConfirmationView(selectedMethod, amount)
                CashInStep.RECEIPT -> CashInReceiptView(selectedMethod, amount, refNumber, timestamp) { navController.navigateUp() }
            }
        }
    }
}

// ── Step Indicator ───────────────────────────────────────────────
@Composable
private fun CashInStepIndicator(currentStep: CashInStep) {
    val steps = listOf("Method", "Amount", "Confirm")
    val currentIndex = when (currentStep) {
        CashInStep.METHOD -> 0; CashInStep.AMOUNT -> 1; CashInStep.CONFIRM -> 2; else -> 2
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            val isActive = index == currentIndex
            val isDone = index < currentIndex
            Box(
                modifier = Modifier
                    .size(if (isActive) 28.dp else 22.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isDone -> PundarTheme.colors.brandPrimary
                            isActive -> PundarTheme.colors.brandPrimary
                            else -> PundarTheme.colors.surfaceTertiary
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                } else {
                    Text(
                        "${index + 1}",
                        color = if (isActive) Color.White else PundarTheme.colors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(
                            if (isDone) PundarTheme.colors.brandPrimary
                            else PundarTheme.colors.surfaceTertiary
                        )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  STEP 1: Method Selection
// ═══════════════════════════════════════════════════════════════
@Composable
private fun MethodSelectionView(
    methods: List<CashInMethod>,
    selectedMethod: CashInMethod?,
    onMethodSelected: (CashInMethod) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // Balance pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PundarTheme.colors.surfaceSecondary)
                    .border(1.dp, PundarTheme.colors.glassMedium, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.AccountBalanceWallet, null,
                    tint = PundarTheme.colors.brandPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("PUNDAR Wallet Balance", style = MaterialTheme.typography.labelSmall,
                        color = PundarTheme.colors.textMuted)
                    Text(AppState.getDisplayBalance(), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = PundarTheme.colors.textPrimary)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Select Cash In Channel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PundarTheme.colors.textPrimary)
            Text("All transactions are secured and encrypted.",
                style = MaterialTheme.typography.labelSmall,
                color = PundarTheme.colors.textMuted)
            Spacer(Modifier.height(12.dp))
        }

        items(methods.size) { index ->
            val method = methods[index]
            val isSelected = method.id == selectedMethod?.id
            val scale by animateFloatAsState(if (isSelected) 1.01f else 1f, label = "scale")

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayerHelper(scale)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onMethodSelected(method) },
                color = if (isSelected) method.color.copy(alpha = 0.08f)
                        else PundarTheme.colors.surfaceSecondary,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) method.color else PundarTheme.colors.glassMedium
                ),
                shadowElevation = if (isSelected) 4.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Method icon circle
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(method.color.copy(alpha = 0.12f))
                            .border(1.dp, method.color.copy(0.25f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(method.icon, null, tint = method.color, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(method.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PundarTheme.colors.textPrimary)
                        Text(method.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = PundarTheme.colors.textMuted,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Free badge
                            MethodBadge(
                                text = if (method.fee == 0.0) "FREE" else "₱${method.fee.toInt()} fee",
                                color = if (method.fee == 0.0) Color(0xFF00875A) else Color(0xFFE65100)
                            )
                            MethodBadge(text = method.processingTime, color = PundarTheme.colors.brandPrimary)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(method.color),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    } else {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null,
                            tint = PundarTheme.colors.textMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }

        item {
            // Security note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1B3A6B).copy(0.3f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Lock, null, tint = PundarTheme.colors.brandPrimary,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Your cash in is protected by 256-bit SSL encryption and monitored 24/7.",
                    style = MaterialTheme.typography.labelSmall,
                    color = PundarTheme.colors.textMuted,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun MethodBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(0.25f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

// ═══════════════════════════════════════════════════════════════
//  STEP 2: Amount Entry
// ═══════════════════════════════════════════════════════════════
@Composable
private fun AmountEntryView(
    selectedMethod: CashInMethod?,
    amount: String,
    onAmountChange: (String) -> Unit
) {
    val quickAmounts = listOf(100, 200, 500, 1000, 2000, 5000)
    val amt = amount.toDoubleOrNull() ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Method header pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(selectedMethod?.color?.copy(0.10f) ?: Color.Transparent)
                .border(1.dp, selectedMethod?.color?.copy(0.3f) ?: Color.Transparent, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(selectedMethod?.color?.copy(0.2f) ?: Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(selectedMethod?.icon ?: Icons.Filled.AccountBalance, null,
                    tint = selectedMethod?.color ?: Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Cashing in via", style = MaterialTheme.typography.labelSmall,
                    color = PundarTheme.colors.textMuted)
                Text(selectedMethod?.name ?: "", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = PundarTheme.colors.textPrimary)
            }
            Spacer(Modifier.weight(1f))
            MethodBadge(
                text = if ((selectedMethod?.fee ?: 0.0) == 0.0) "NO FEE"
                       else "+₱${selectedMethod?.fee?.toInt()} fee",
                color = if ((selectedMethod?.fee ?: 0.0) == 0.0) Color(0xFF00875A) else Color(0xFFE65100)
            )
        }

        Spacer(Modifier.height(32.dp))

        // Big amount display
        Text("Enter Amount", style = MaterialTheme.typography.labelMedium,
            color = PundarTheme.colors.textMuted, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PundarTheme.colors.surfaceSecondary)
                .border(
                    2.dp,
                    if (amt > 0 && amt < 100) ErrorRed else PundarTheme.colors.brandPrimary,
                    RoundedCornerShape(16.dp)
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = {
                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        onAmountChange(it)
                    }
                },
                prefix = {
                    Text("₱ ",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (amt > 0) PundarTheme.colors.brandPrimary else PundarTheme.colors.textMuted)
                },
                textStyle = MaterialTheme.typography.displaySmall.copy(
                    color = PundarTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                ),
                placeholder = {
                    Text("0.00", style = MaterialTheme.typography.displaySmall,
                        color = PundarTheme.colors.textDim, fontWeight = FontWeight.Bold)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Min amount warning
        AnimatedVisibility(visible = amt > 0 && amt < 100) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Minimum cash in amount is ₱100.00",
                    color = ErrorRed, style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Quick amount chips
        Text("Quick Select", style = MaterialTheme.typography.labelSmall,
            color = PundarTheme.colors.textMuted, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))

        val chunked = quickAmounts.chunked(3)
        chunked.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { quickAmt ->
                    val isSelected = amount == quickAmt.toString()
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onAmountChange(quickAmt.toString()) },
                        color = if (isSelected) PundarTheme.colors.brandPrimary.copy(0.15f)
                                else PundarTheme.colors.surfaceSecondary,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) PundarTheme.colors.brandPrimary else PundarTheme.colors.glassMedium
                        )
                    ) {
                        Text(
                            "₱${String.format("%,d", quickAmt)}",
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) PundarTheme.colors.brandPrimary else PundarTheme.colors.textPrimary
                        )
                    }
                }
                // Fill remaining space if row not full
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(12.dp))

        // Limit info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(PundarTheme.colors.surfaceSecondary)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Info, null, tint = PundarTheme.colors.textMuted,
                modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text("${selectedMethod?.processingTime ?: "Instant"} • ${selectedMethod?.limit ?: "Up to ₱100,000/day"}",
                style = MaterialTheme.typography.labelSmall, color = PundarTheme.colors.textMuted)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  STEP 3: Confirmation
// ═══════════════════════════════════════════════════════════════
@Composable
private fun ConfirmationView(
    selectedMethod: CashInMethod?,
    amount: String
) {
    val amt = amount.toDoubleOrNull() ?: 0.0
    val fee = selectedMethod?.fee ?: 0.0
    val total = amt + fee
    val userName = AuthRepository.getCurrentUserName().ifBlank { "PUNDAR User" }
    val userPhone = AuthRepository.getCurrentUserPhone()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Recipient card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = PundarTheme.colors.surfaceSecondary,
                border = BorderStroke(1.dp, PundarTheme.colors.glassMedium)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Sending to", style = MaterialTheme.typography.labelSmall,
                        color = PundarTheme.colors.textMuted)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(PundarTheme.colors.brandPrimary, PundarTheme.colors.accentElectric),
                                        Offset.Zero, Offset(44f, 44f)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                userName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(userName, style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = PundarTheme.colors.textPrimary)
                            Text(userPhone, style = MaterialTheme.typography.labelSmall,
                                color = PundarTheme.colors.textMuted)
                        }
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF00875A).copy(0.15f))
                                .border(1.dp, Color(0xFF00875A).copy(0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("VERIFIED", style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp, color = Color(0xFF00875A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            // Breakdown card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = PundarTheme.colors.surfaceSecondary,
                border = BorderStroke(1.dp, PundarTheme.colors.glassMedium)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Transaction Details",
                        style = MaterialTheme.typography.labelSmall,
                        color = PundarTheme.colors.textMuted)
                    Spacer(Modifier.height(14.dp))

                    ConfirmRow("Cash In Method", selectedMethod?.name ?: "")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp),
                        color = PundarTheme.colors.glassSubtle)
                    ConfirmRow("Amount", "₱${String.format("%,.2f", amt)}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp),
                        color = PundarTheme.colors.glassSubtle)
                    ConfirmRow("Service Fee",
                        if (fee == 0.0) "FREE" else "₱${String.format("%,.2f", fee)}",
                        valueColor = if (fee == 0.0) Color(0xFF00875A) else PundarTheme.colors.textPrimary)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp),
                        color = PundarTheme.colors.glassMedium, thickness = 1.dp)

                    // Total row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Amount",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PundarTheme.colors.textPrimary)
                        Text(
                            "₱${String.format("%,.2f", total)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = PundarTheme.colors.brandPrimary
                        )
                    }
                }
            }
        }

        item {
            // Processing time info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PundarTheme.colors.brandPrimary.copy(0.08f))
                    .border(1.dp, PundarTheme.colors.brandPrimary.copy(0.2f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.AccessTime, null,
                    tint = PundarTheme.colors.brandPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Processing Time", style = MaterialTheme.typography.labelSmall,
                        color = PundarTheme.colors.textMuted)
                    Text(selectedMethod?.processingTime ?: "Instant",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = PundarTheme.colors.textPrimary)
                }
            }
        }

        item {
            // Disclaimer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(WarningAmber.copy(alpha = 0.08f))
                    .border(1.dp, WarningAmber.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Filled.Info, null, tint = WarningAmber,
                    modifier = Modifier.size(16.dp).padding(top = 1.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "By tapping 'Confirm Cash In', you agree to PUNDAR's Terms of Service. " +
                    "This feature is in sandbox mode. No real money will be deducted.",
                    style = MaterialTheme.typography.labelSmall,
                    color = PundarTheme.colors.textMuted,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun ConfirmRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = PundarTheme.colors.textMuted)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (valueColor == Color.Unspecified) PundarTheme.colors.textPrimary else valueColor
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  STEP 4: Success Receipt
// ═══════════════════════════════════════════════════════════════
@Composable
private fun CashInReceiptView(
    selectedMethod: CashInMethod?,
    amount: String,
    refNumber: String,
    timestamp: Long,
    onDone: () -> Unit
) {
    val amt = amount.toDoubleOrNull() ?: 0.0
    val sdf = SimpleDateFormat("MMMM dd, yyyy  hh:mm a", Locale.getDefault())
    val dateStr = sdf.format(Date(timestamp))
    val successAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        successAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        // Success animation
        Box(
            modifier = Modifier
                .size(88.dp)
                .graphicsLayer(scaleX = successAnim.value, scaleY = successAnim.value)
                .clip(CircleShape)
                .background(Color(0xFF16C784).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF16C784),
                modifier = Modifier.size(52.dp))
        }

        Spacer(Modifier.height(16.dp))

        Text("Cash In Successful!", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black, color = PundarTheme.colors.textPrimary,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text("Your PUNDAR wallet has been credited.",
            style = MaterialTheme.typography.bodyMedium,
            color = PundarTheme.colors.textMuted, textAlign = TextAlign.Center)

        Spacer(Modifier.height(24.dp))

        // Amount badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF16C784).copy(0.12f))
                .border(1.dp, Color(0xFF16C784).copy(0.35f), RoundedCornerShape(20.dp))
                .padding(horizontal = 28.dp, vertical = 14.dp)
        ) {
            Text(
                "+₱${String.format("%,.2f", amt)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color(0xFF16C784)
            )
        }

        Spacer(Modifier.height(24.dp))

        // Receipt card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = PundarTheme.colors.surfaceSecondary,
            border = BorderStroke(1.dp, PundarTheme.colors.glassMedium)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("TRANSACTION RECEIPT",
                    style = MaterialTheme.typography.labelSmall,
                    color = PundarTheme.colors.textMuted,
                    letterSpacing = 2.sp)
                Spacer(Modifier.height(14.dp))

                ConfirmRow("Method", selectedMethod?.name ?: "")
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp),
                    color = PundarTheme.colors.glassSubtle)
                ConfirmRow("Amount Credited", "₱${String.format("%,.2f", amt)}",
                    valueColor = Color(0xFF16C784))
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp),
                    color = PundarTheme.colors.glassSubtle)
                ConfirmRow("Service Fee",
                    if ((selectedMethod?.fee ?: 0.0) == 0.0) "FREE"
                    else "₱${String.format("%,.2f", selectedMethod?.fee)}",
                    valueColor = Color(0xFF00875A))
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp),
                    color = PundarTheme.colors.glassSubtle)
                ConfirmRow("Date & Time", dateStr)
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp),
                    color = PundarTheme.colors.glassSubtle)
                ConfirmRow("Status", "COMPLETED", valueColor = Color(0xFF16C784))
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp),
                    color = PundarTheme.colors.glassMedium)

                // Reference number
                Column {
                    Text("Reference Number", style = MaterialTheme.typography.labelSmall,
                        color = PundarTheme.colors.textMuted)
                    Spacer(Modifier.height(4.dp))
                    SelectionContainer {
                        Text(refNumber,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PundarTheme.colors.brandPrimary,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PundarTheme.colors.brandPrimary)
        ) {
            Icon(Icons.Filled.Home, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Back to Home", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))

        Text("PUNDAR • Spend · Save · Grow · Together",
            style = MaterialTheme.typography.labelSmall,
            color = PundarTheme.colors.textDim,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center)
    }
}

// ── Modifier Extension helpers ──────────────────────────────────
@Composable
private fun Modifier.graphicsLayerHelper(scale: Float): Modifier =
    this.graphicsLayer(scaleX = scale, scaleY = scale)
