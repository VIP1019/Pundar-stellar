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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.remote.TransactionRepository
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.BillStatus
import com.example.pundarapp.ui.theme.*
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
    
    val walletBalance = AppState.walletBalance.value
    val hasSufficientBalance = totalDebit <= walletBalance
    
    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Instant Settlement",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarBackground
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
                        color = PundarTextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Payment will be processed immediately from your wallet balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = PundarTextSecondary
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
                                    tint = PundarSuccess,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "No pending bills!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PundarTextPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "All your bills are settled.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PundarTextSecondary
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
                onConfirm = {
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
                            currentBalance = walletBalance,
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
                        AppState.walletBalance.value -= totalSettlement
                        selectedBillIds.forEach { billId -> AppState.settleBill(billId) }
                        val growInvestment = AppState.processPayRoundUp(
                            sourceReference = transactionRef,
                            sourceAmount = totalSettlement,
                            sourceLabel = "Instant Settlement"
                        )
                        AppState.requestHomeRefresh()
                        AppState.refreshNotifications()

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
                ambientColor = ElectricBlue.copy(0.2f), spotColor = ElectricBlue.copy(0.2f))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Navy800, Navy700)))
    ) {
        // Shimmer effect
        Box(Modifier.matchParentSize().background(
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(0.05f), Color.Transparent),
                start = Offset(sweepX, 0f), end = Offset(sweepX + 200f, 300f)
            )
        ))
        
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "WALLET BALANCE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color = ElectricBlue.copy(0.85f)
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
    val accent = if (isSelected) PremiumGoldWarm else ElectricBlue
    
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
                    uncheckedColor = PundarBorder
                )
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = bill.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PundarTextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${bill.memberCount} members · ${bill.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PundarTextSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "₱${String.format("%,.2f", bill.yourShare)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) PremiumGoldWarm else PundarTextPrimary
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
            .background(PundarSurface)
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
                        color = PundarTextSecondary
                    )
                    Text(
                        text = "₱${String.format("%,.2f", totalAmount)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = PundarTextPrimary
                    )
                    if (roundUpAmount > 0.0) {
                        Text(
                            text = "+ ₱${String.format("%,.2f", roundUpAmount)} Grow round-up",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricBlue
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
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PundarSurface,
        title = {
            Text(
                text = "Confirm Settlement",
                fontWeight = FontWeight.Bold,
                color = PundarTextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "You are about to settle $billCount ${if (billCount == 1) "bill" else "bills"}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PundarTextPrimary
                )
                Spacer(Modifier.height(16.dp))
                
                // Transaction details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Amount:", color = PundarTextSecondary)
                    Text(
                        "₱${String.format("%,.2f", totalAmount)}",
                        fontWeight = FontWeight.Bold,
                        color = PundarTextPrimary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Current Balance:", color = PundarTextSecondary)
                    Text(
                        "₱${String.format("%,.2f", walletBalance)}",
                        fontWeight = FontWeight.Bold,
                        color = PundarTextPrimary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Grow Round-up:", color = PundarTextSecondary)
                    Text(
                        "₱${String.format("%,.2f", roundUpAmount)}",
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("New Balance:", color = PundarTextSecondary)
                    Text(
                        "₱${String.format("%,.2f", walletBalance - totalAmount - roundUpAmount)}",
                        fontWeight = FontWeight.Bold,
                        color = if (hasSufficientBalance) PundarSuccess else WarningAmber
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
                onClick = onConfirm,
                enabled = hasSufficientBalance,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PundarBlue,
                    disabledContainerColor = PundarBorder
                )
            ) {
                Text("Confirm & Pay")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PundarTextSecondary)
            }
        }
    )
}

@Composable
private fun ProcessingDialog(stage: String) {
    AlertDialog(
        onDismissRequest = { },
        containerColor = PundarSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = PundarBlue,
                    strokeWidth = 3.dp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Processing...",
                    fontWeight = FontWeight.Bold,
                    color = PundarTextPrimary
                )
            }
        },
        text = {
            Text(
                text = stage,
                style = MaterialTheme.typography.bodyMedium,
                color = PundarTextSecondary
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
        containerColor = PundarSurface,
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
                        .shadow(12.dp, CircleShape, ambientColor = PundarSuccess.copy(0.4f))
                        .clip(CircleShape)
                        .background(PundarSuccess.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = PundarSuccess,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Settlement Successful!",
                    fontWeight = FontWeight.Bold,
                    color = PundarTextPrimary,
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
                    color = PundarTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                
                // Transaction details card
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PundarBackground)
                        .border(1.dp, PundarBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Amount Paid:", color = PundarTextSecondary, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "₱${String.format("%,.2f", totalAmount)}",
                                fontWeight = FontWeight.Bold,
                                color = PundarSuccess,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Reference:", color = PundarTextSecondary, style = MaterialTheme.typography.bodySmall)
                            Text(
                                transactionRef,
                                fontWeight = FontWeight.Bold,
                                color = PundarTextPrimary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grow Round-up:", color = PundarTextSecondary, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "₱${String.format("%,.2f", roundUpAmount)}",
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue,
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
                    Icon3DCheckBadge(size = 16.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Settlement builds your PUNDAR Score",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricBlue,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PundarBlue)
            ) {
                Text("Done")
            }
        }
    )
}
