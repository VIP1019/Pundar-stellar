package com.example.pundarapp.ui.screens.pay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import android.content.Intent
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.stellar.PayStellarClient
import com.example.pundarapp.data.stellar.PaymentResult
import com.example.pundarapp.data.stellar.StellarWalletManager
import com.example.pundarapp.data.stellar.Stroops
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.BillStatus
import com.example.pundarapp.ui.data.parseCategorizedTitle
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailScreen(billId: String, navController: NavController) {
    val bill = AppState.bills.find { it.id == billId }
        ?: run { navController.navigateUp(); return }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val walletBalance = AppState.walletBalance.value
    val exchangeRate = AppState.getPhpRate()
    val totalXlm = bill.totalAmount / exchangeRate
    val yourShareXlm = bill.yourShare / exchangeRate
    val canAffordSettle = walletBalance >= yourShareXlm

    var showPinDialog by remember { mutableStateOf(false) }
    var mpinInput by remember { mutableStateOf("") }
    var isSettling by remember { mutableStateOf(false) }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSettling) { showPinDialog = false; mpinInput = "" } },
            containerColor = PundarTheme.colors.surfacePrimary,
            title = { Text("Confirm Bill Settlement", fontWeight = FontWeight.Bold, color = PundarTheme.colors.textPrimary) },
            text = {
                Column {
                    Text(
                        "Pay your share of ₱${String.format("%,.2f", bill.yourShare)} (≈ ${String.format("%,.4f", yourShareXlm)} XLM) for '${bill.name}'.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PundarTheme.colors.textSecondary
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = mpinInput,
                        onValueChange = { if (it.length <= 4) mpinInput = it },
                        label = { Text("Enter 4-digit MPIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = mpinInput.length == 4 && !isSettling,
                    colors = ButtonDefaults.buttonColors(containerColor = PundarTheme.colors.brandPrimary),
                    onClick = {
                        scope.launch {
                            isSettling = true
                            val senderSeed = AuthRepository.getCurrentUserEncryptedSeed().orEmpty()
                            if (senderSeed.isNotBlank()) {
                                val secret = try {
                                    StellarWalletManager.decryptSecretSeed(senderSeed, mpinInput)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Incorrect PIN.", Toast.LENGTH_SHORT).show()
                                    isSettling = false
                                    mpinInput = ""
                                    return@launch
                                }
                                val myPhone = AuthRepository.getCurrentUserPhone()
                                val counterpartyUsername = bill.members
                                    .firstOrNull { !it.name.equals("You", true) && it.username != myPhone && it.username.isNotBlank() }
                                    ?.username
                                val counterpartyKey = counterpartyUsername?.let { uname ->
                                    if (uname.startsWith("G") && uname.length == 56) uname
                                    else AuthRepository.findRegisteredUser(uname)?.stellarPublicKey
                                }?.takeIf { it.startsWith("G") && it.length == 56 }
                                    ?: StellarWalletManager.USDC_ISSUER

                                val stroops = Stroops.fromXlmString(String.format(java.util.Locale.US, "%.7f", yourShareXlm))
                                if (stroops == null || stroops <= 0) {
                                    Toast.makeText(context, "Invalid settlement amount.", Toast.LENGTH_SHORT).show()
                                    isSettling = false
                                    return@launch
                                }

                                when (val res = PayStellarClient.sendPayment(
                                    secret = secret,
                                    destination = counterpartyKey,
                                    amountStroops = stroops,
                                    memo = "Settle: ${bill.name}".take(28)
                                )) {
                                    is PaymentResult.Success -> {
                                        AppState.settleBill(bill.id)
                                        AppState.refreshWalletBalance()
                                        isSettling = false
                                        showPinDialog = false
                                        mpinInput = ""
                                        Toast.makeText(
                                            context,
                                            "Bill settled on Stellar! Ref: ${res.hash.take(10).uppercase()}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        navController.navigateUp()
                                    }
                                    is PaymentResult.Failure -> {
                                        isSettling = false
                                        Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                                    }
                                    is PaymentResult.Unknown -> {
                                        isSettling = false
                                        Toast.makeText(context, "Network timed out. Check your balance before retrying.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                AppState.settleBill(bill.id)
                                isSettling = false
                                showPinDialog = false
                                mpinInput = ""
                                Toast.makeText(context, "Bill settled! ₱${String.format("%,.2f", bill.yourShare)} deducted.", Toast.LENGTH_SHORT).show()
                                navController.navigateUp()
                            }
                        }
                    }
                ) {
                    Text(if (isSettling) "Settling..." else "Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSettling,
                    onClick = { showPinDialog = false; mpinInput = "" }
                ) {
                    Text("Cancel", color = PundarTheme.colors.textSecondary)
                }
            }
        )
    }

    val (cleanBillName, billCategory) = remember(bill.name) {
        parseCategorizedTitle(bill.name)
    }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = cleanBillName,
                onBack = { navController.navigateUp() },
                showMoreOptions = true
            )
        },
        containerColor = PundarTheme.colors.bgPrimary
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Summary Card ─────────────────────────────────────
            item {
                PundarAccentCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            StatusBadge(
                                text = when (bill.status) {
                                    BillStatus.SETTLED -> "Settled"
                                    BillStatus.PENDING -> "Pending"
                                    BillStatus.PARTIAL -> "Partial"
                                },
                                color = when (bill.status) {
                                    BillStatus.SETTLED -> PundarSuccess
                                    BillStatus.PENDING -> PundarWarning
                                    BillStatus.PARTIAL -> PundarInfo
                                }
                            )
                            if (!billCategory.isNullOrBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = billCategory,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = PundarTheme.colors.textSecondary
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = bill.date,
                                style = MaterialTheme.typography.bodySmall,
                                color = PundarTheme.colors.textSecondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.bodySmall,
                                color = PundarTheme.colors.textSecondary
                            )
                            Text(
                                text = "₱ ${String.format("%,.2f", bill.totalAmount)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = PundarTheme.colors.textPrimary
                            )
                            Text(
                                text = "(≈ ${String.format("%,.2f", totalXlm)} XLM)",
                                style = MaterialTheme.typography.bodySmall,
                                color = PundarTheme.colors.textSecondary
                            )
                            Text(
                                text = "Your share: ₱ ${String.format("%,.2f", bill.yourShare)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PundarTheme.colors.brandPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ── Members ──────────────────────────────────────────
            item {
                Text(
                    text = "Members (${bill.memberCount})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PundarTheme.colors.textPrimary
                )
            }

            if (bill.members.isNotEmpty()) {
                item {
                    PundarCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            bill.members.forEachIndexed { index, member ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (member.name == "You") PundarTheme.colors.brandPrimary.copy(alpha = 0.1f)
                                                else Color(member.avatarColor)
                                            )
                                    ) {
                                        Text(
                                            text = member.initials,
                                            color = if (member.name == "You") PundarTheme.colors.brandPrimary else PundarTheme.colors.surfacePrimary,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = member.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PundarTheme.colors.textPrimary
                                        )
                                        if (member.username.isNotEmpty()) {
                                            Text(
                                                text = member.username,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = PundarTheme.colors.textSecondary
                                            )
                                        }
                                    }
                                    Text(
                                        text = "₱ ${String.format("%,.2f", member.amount)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PundarTheme.colors.textPrimary
                                    )
                                }
                                if (index < bill.members.size - 1) {
                                    HorizontalDivider(color = PundarDivider)
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    PundarCard {
                        Text(
                            text = "${bill.memberCount} members split equally — ₱ ${String.format("%,.2f", bill.yourShare)} each",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PundarTheme.colors.textSecondary
                        )
                    }
                }
            }

            // ── Actions ──────────────────────────────────────────
            item {
                PundarPrimaryButton(
                    text = "Generate Payment QR",
                    onClick = { navController.navigate(Routes.billQr(bill.id)) }
                )
            }
            if (bill.status == BillStatus.PENDING || bill.status == BillStatus.PARTIAL) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PundarPrimaryButton(
                            text = if (isSettling) "Settling..." else "Settle My Share — ₱${String.format("%,.2f", bill.yourShare)} (≈ ${String.format("%,.2f", yourShareXlm)} XLM)",
                            enabled = canAffordSettle && !isSettling,
                            onClick = {
                                showPinDialog = true
                            }
                        )
                        if (!canAffordSettle) {
                            Text(
                                text = "Insufficient balance (${String.format("%,.2f", walletBalance)} XLM available)",
                                style = MaterialTheme.typography.labelSmall,
                                color = PundarWarning,
                                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item {
                PundarSecondaryButton(
                    text = "Share Split Summary",
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Pundar Bill: ${bill.name}")
                            putExtra(Intent.EXTRA_TEXT, "Hey! Our bill '${bill.name}' is ₱${String.format("%,.2f", bill.totalAmount)}. Your share is ₱${String.format("%,.2f", bill.yourShare)}. Please settle it on PUNDAR!")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Bill"))
                    }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
