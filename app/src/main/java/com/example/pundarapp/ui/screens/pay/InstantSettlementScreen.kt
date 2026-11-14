package com.example.pundarapp.ui.screens.pay

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.remote.TransactionRepository
import com.example.pundarapp.data.stellar.PayStellarClient
import com.example.pundarapp.data.stellar.PaymentResult
import com.example.pundarapp.data.stellar.StellarWalletManager
import com.example.pundarapp.data.stellar.Stroops
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.BillStatus
import com.example.pundarapp.ui.theme.*

import com.example.pundarapp.ui.theme.PundarTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

private val EaseOutBack   = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
private val EaseOutExpo   = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstantSettlementScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Get all pending bills
    val pendingBills = remember { 
        AppState.bills.filter { 
            it.status == BillStatus.PENDING || it.status == BillStatus.PARTIAL 
        }
    }
    
    var selectedBillIds by remember { mutableStateOf(setOf<String>()) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var processingStage by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var transactionRef by remember { mutableStateOf("") }
    
    val totalSettlement = remember(selectedBillIds) {
        pendingBills.filter { it.id in selectedBillIds }.sumOf { it.yourShare }
    }
    val roundUpAmount = remember(totalSettlement) {
        AppState.calculateRoundUpAmount(totalSettlement)
    }
    val totalDebit = totalSettlement + roundUpAmount
    
    val walletBalanceXlm = AppState.walletBalance.value
    val walletBalance = AppState.xlmToPhp(walletBalanceXlm)
    val hasSufficientBalance = totalDebit <= walletBalance
    
    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Instant Settlement",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarTheme.colors.bgPrimary
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Balance Card
                item {
                    WalletBalanceCard(walletBalance)
                }
                
                // Instructions
                item {
                    Text(
                        text = "Select bills to settle instantly",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PundarTheme.colors.textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Payment will be processed immediately from your wallet balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = PundarTheme.colors.textSecondary
                    )
                }
                
                // Pending Bills
                if (pendingBills.isEmpty()) {
                    item {
                        PundarCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Green400,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "No pending bills!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PundarTheme.colors.textPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "All your bills are settled.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PundarTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(pendingBills) { bill ->
                        SelectableBillCard(
                            bill = bill,
                            isSelected = bill.id in selectedBillIds,
                            onToggle = {
                                selectedBillIds = if (bill.id in selectedBillIds) {
                                    selectedBillIds - bill.id
                                } else {
                                    selectedBillIds + bill.id
                                }
                            }
                        )
                    }
                }
                
                // Bottom spacing for button
                item { Spacer(Modifier.height(80.dp)) }
            }
            
            // Settlement Summary Bottom Bar
            if (selectedBillIds.isNotEmpty()) {
                SettlementBottomBar(
                    totalAmount = totalSettlement,
                    roundUpAmount = roundUpAmount,
                    hasSufficientBalance = hasSufficientBalance,
                    onSettle = { showConfirmDialog = true },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        
        // Confirmation Dialog
        if (showConfirmDialog) {
            SettlementConfirmDialog(
                totalAmount = totalSettlement,
                roundUpAmount = roundUpAmount,
                billCount = selectedBillIds.size,
                walletBalance = walletBalance,
                hasSufficientBalance = hasSufficientBalance,
                onConfirm = { mpin ->
                    showConfirmDialog = false
                    isProcessing = true
                    scope.launch {
                        processingStage = "Validating payment..."
                        delay(400)

                        val userId = AuthRepository.getCurrentUserId()
                        if (userId == null) {
                            Toast.makeText(context, "Not logged in.", Toast.LENGTH_SHORT).show()
                            isProcessing = false
                            return@launch
                        }

                        val senderSeed = AuthRepository.getCurrentUserEncryptedSeed().orEmpty()
                        if (senderSeed.isNotBlank()) {
                            val secret = try {
                                StellarWalletManager.decryptSecretSeed(senderSeed, mpin)
                            } catch (_: Exception) {
                                isProcessing = false
                                Toast.makeText(context, "Incorrect PIN.", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            processingStage = "Submitting Stellar transaction..."
                            val settleXlm = AppState.phpToXlm(totalSettlement)
                            val stroops = Stroops.fromXlmString(String.format(Locale.US, "%.7f", settleXlm))
                            if (stroops == null || stroops <= 0) {
                                isProcessing = false
                                Toast.makeText(context, "Invalid settlement amount.", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            val myPhone = AuthRepository.getCurrentUserPhone()
                            val firstCounterparty = pendingBills
                                .filter { it.id in selectedBillIds }
                                .flatMap { it.members }
                                .firstOrNull { !it.name.equals("You", true) && it.username != myPhone && it.username.isNotBlank() }
                                ?.username
                            val destinationKey = firstCounterparty?.let { uname ->
                                if (uname.startsWith("G") && uname.length == 56) uname
                                else AuthRepository.findRegisteredUser(uname)?.stellarPublicKey
                            }?.takeIf { it.startsWith("G") && it.length == 56 }
                                ?: StellarWalletManager.USDC_ISSUER

                            when (val payRes = PayStellarClient.sendPayment(
                                secret = secret,
                                destination = destinationKey,
                                amountStroops = stroops,
                                memo = "Instant Settle (${selectedBillIds.size})"
                            )) {
                                is PaymentResult.Success -> { /* continue to Firestore sync */ }
                                is PaymentResult.Failure -> {
                                    isProcessing = false
                                    Toast.makeText(context, payRes.message, Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                is PaymentResult.Unknown -> {
                                    isProcessing = false
                                    Toast.makeText(context, "Network timed out. Check your balance before retrying.", Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                            }
                        }

                        val billsById = pendingBills.associate {
                            it.id to TransactionRepository.BillLite(
                                id = it.id,
                                status = it.status,
                                yourShare = it.yourShare,
                                name = it.name
                            )
                        }

                        processingStage = "Processing transaction..."
                        val result = TransactionRepository.settleBills(
                            userId = userId,
                            billIds = selectedBillIds,
                            currentBalance = walletBalanceXlm,
                            billsById = billsById
                        )

                        if (result.isFailure) {
                            isProcessing = false
                            Toast.makeText(
                                context,
                                result.exceptionOrNull()?.message ?: "Settlement failed",
                                Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }

                        processingStage = "Updating balance..."
                        transactionRef = result.getOrNull() ?: ""
                        selectedBillIds.forEach { billId -> AppState.settleBill(billId) }
                        AppState.refreshWalletBalance()
                        val growInvestment = AppState.processPayRoundUp(
                            sourceReference = transactionRef,
                            sourceAmount = totalSettlement,
                            sourceLabel = "Instant Settlement",
                            mpin = mpin
                        )
                        AppState.requestHomeRefresh()
                        AppState.refreshNotifications()
                        AppState.incrementPundarScore(5) // +5 points for bill settlement

                        processingStage = if (growInvestment != null) {
                            "Buying fractional ${growInvestment.ticker} shares..."
                        } else {
                            "Generating receipt..."
                        }
                        delay(400)

                        isProcessing = false
                        showSuccessDialog = true
                    }
                },
                onDismiss = { showConfirmDialog = false }
            )
        }
        
        // Processing Dialog
        if (isProcessing) {
            ProcessingDialog(stage = processingStage)
        }
        
        // Success Dialog
        if (showSuccessDialog) {
            SuccessDialog(
                totalAmount = totalSettlement,
                roundUpAmount = roundUpAmount,
                transactionRef = transactionRef,
                onDismiss = {
                    showSuccessDialog = false
                    navController.navigateUp()
                }
            )
        }
    }
}

@Composable
private fun WalletBalanceCard(balance: Double) {
    val infinite = rememberInfiniteTransition(label = "balance")
    val sweepX by infinite.animateFloat(
        initialValue = -500f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing)), label = "sweep"
    )
    
    Box(
        Modifier.fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(20.dp),
                ambientColor = Electric400.copy(0.2f), spotColor = Electric400.copy(0.2f))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(PundarTheme.colors.surfacePrimary, PundarTheme.colors.surfaceSecondary)))
    ) {
        // Shimmer effect
        Box(Modifier.matchParentSize().background(
            Brush.linearGradient(
                listOf(Color.Transparent, PundarTheme.colors.surfacePrimary.copy(0.05f), Color.Transparent),
                start = Offset(sweepX, 0f), end = Offset(sweepX + 200f, 300f)
            )
        ))
        
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Electric400,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "WALLET BALANCE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color = Electric400.copy(0.85f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "₱${String.format("%,.2f", balance)}",
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                color = TextOnDark,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectableBillCard(
    bill: com.example.pundarapp.ui.data.GroupBill,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val accent = if (isSelected) PremiumGoldWarm else Electric400
    
    PundarCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = PremiumGoldWarm,
                    uncheckedColor = NavyBorder
                )
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = bill.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PundarTheme.colors.textPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${bill.memberCount} members · ${bill.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PundarTheme.colors.textSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "₱${String.format("%,.2f", bill.yourShare)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) PremiumGoldWarm else PundarTheme.colors.textPrimary
            )
        }
    }
}

@Composable
private fun SettlementBottomBar(
    totalAmount: Double,
    roundUpAmount: Double,
    hasSufficientBalance: Boolean,
    onSettle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp)
            .background(PundarTheme.colors.surfacePrimary)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Settlement",
                        style = MaterialTheme.typography.bodySmall,
                        color = PundarTheme.colors.textSecondary
                    )
                    Text(
                        text = "₱${String.format("%,.2f", totalAmount)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = PundarTheme.colors.textPrimary
                    )
                    if (roundUpAmount > 0.0) {
                        Text(
                            text = "+ ₱${String.format("%,.2f", roundUpAmount)} Grow round-up",
                            style = MaterialTheme.typography.labelSmall,
                            color = Electric400
                        )
                    }
                }
                PundarPrimaryButton(
                    text = "Settle Now",
                    enabled = hasSufficientBalance,
                    onClick = onSettle
                )
            }
            
            if (!hasSufficientBalance) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Insufficient wallet balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun SettlementConfirmDialog(
    totalAmount: Double,
    roundUpAmount: Double,
    billCount: Int,
    walletBalance: Double,
    hasSufficientBalance: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var requiresMpin by remember { mutableStateOf(true) }
    var mpin by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        requiresMpin = !AuthRepository.getCurrentUserEncryptedSeed().isNullOrBlank()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PundarTheme.colors.surfacePrimary,
        title = {
            Text(
                text = "Confirm Settlement",
                fontWeight = FontWeight.Bold,
                color = PundarTheme.colors.textPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "You are about to settle $billCount ${if (billCount == 1) "bill" else "bills"}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PundarTheme.colors.textPrimary
                )
                Spacer(Modifier.height(16.dp))
                
                // Transaction details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Amount:", color = PundarTheme.colors.textSecondary)
                    Text(
                        "₱${String.format("%,.2f", totalAmount)} (≈ ${String.format("%,.2f", AppState.phpToXlm(totalAmount))} XLM)",
                        fontWeight = FontWeight.Bold,
                        color = PundarTheme.colors.textPrimary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Current Balance:", color = PundarTheme.colors.textSecondary)
                    Text(
                        "₱${String.format("%,.2f", walletBalance)}",
                        fontWeight = FontWeight.Bold,
                        color = PundarTheme.colors.textPrimary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Grow Round-up:", color = PundarTheme.colors.textSecondary)
                    Text(
                        "₱${String.format("%,.2f", roundUpAmount)}",
                        fontWeight = FontWeight.Bold,
                        color = Electric400
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("New Balance:", color = PundarTheme.colors.textSecondary)
                    Text(
                        "₱${String.format("%,.2f", walletBalance - totalAmount - roundUpAmount)}",
                        fontWeight = FontWeight.Bold,
                        color = if (hasSufficientBalance) Green400 else WarningAmber
                    )
                }

                if (hasSufficientBalance && requiresMpin) {
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = mpin,
                        onValueChange = { if (it.length <= 4) mpin = it },
                        label = { Text("Enter 4-digit MPIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                if (!hasSufficientBalance) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Insufficient balance. Please top up your wallet first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarningAmber
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(mpin) },
                enabled = hasSufficientBalance && (!requiresMpin || mpin.length == 4),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PundarTheme.colors.brandPrimary,
                    disabledContainerColor = NavyBorder
                )
            ) {
                Text("Confirm & Pay")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PundarTheme.colors.textSecondary)
            }
        }
    )
}

@Composable
private fun ProcessingDialog(stage: String) {
    AlertDialog(
        onDismissRequest = { },
        containerColor = PundarTheme.colors.surfacePrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = PundarTheme.colors.brandPrimary,
                    strokeWidth = 3.dp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Processing...",
                    fontWeight = FontWeight.Bold,
                    color = PundarTheme.colors.textPrimary
                )
            }
        },
        text = {
            Text(
                text = stage,
                style = MaterialTheme.typography.bodyMedium,
                color = PundarTheme.colors.textSecondary
            )
        },
        confirmButton = { }
    )
}

@Composable
private fun SuccessDialog(
    totalAmount: Double,
    roundUpAmount: Double,
    transactionRef: String,
    onDismiss: () -> Unit
) {
    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(300))
        scale.animateTo(1f, tween(400, easing = EaseOutBack))
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PundarTheme.colors.surfacePrimary,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer(
                            scaleX = scale.value,
                            scaleY = scale.value,
                            alpha = alpha.value
                        )
                        .shadow(12.dp, CircleShape, ambientColor = Green400.copy(0.4f))
                        .clip(CircleShape)
                        .background(Green400.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Green400,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Settlement Successful!",
                    fontWeight = FontWeight.Bold,
                    color = PundarTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your bills have been settled successfully.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PundarTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                
                // Transaction details card
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PundarTheme.colors.bgPrimary)
                        .border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Amount Paid:", color = PundarTheme.colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "₱${String.format("%,.2f", totalAmount)}",
                                fontWeight = FontWeight.Bold,
                                color = Green400,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Reference:", color = PundarTheme.colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                            Text(
                                transactionRef,
                                fontWeight = FontWeight.Bold,
                                color = PundarTheme.colors.textPrimary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grow Round-up:", color = PundarTheme.colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "₱${String.format("%,.2f", roundUpAmount)}",
                                fontWeight = FontWeight.Bold,
                                color = Electric400,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Green400, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Settlement builds your PUNDAR Score",
                        style = MaterialTheme.typography.bodySmall,
                        color = Electric400,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PundarTheme.colors.brandPrimary)
            ) {
                Text("Done")
            }
        }
    )
}
