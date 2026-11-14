package com.example.pundarapp.ui.screens.pay

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.pundarapp.data.qr.QrCodeGenerator
import com.example.pundarapp.data.qr.QrPayload
import com.example.pundarapp.data.qr.QrScanHandler
import com.example.pundarapp.data.qr.QrScanResult
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.remote.QrPaymentRepository
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.*
import com.example.pundarapp.ui.theme.*
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.launch
import com.example.pundarapp.data.remote.HomeRepository
import com.example.pundarapp.data.stellar.PayStellarClient
import com.example.pundarapp.data.stellar.PaymentResult
import com.example.pundarapp.data.stellar.StellarWalletManager
import com.example.pundarapp.data.stellar.Stroops

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupBillScreen(navController: NavController) {
    // ── Wizard Step State (1..5) ──────────────────────────────────
    var currentStep by remember { mutableIntStateOf(1) }

    // ── Step 1 & 2 Inputs ──────────────────────────────────────────
    var merchantName by remember { mutableStateOf("") }
    var merchantRef by remember { mutableStateOf("") }
    var merchantAddress by remember { mutableStateOf("") }
    var isQrPhMerchant by remember { mutableStateOf(false) }
    // True only after the recipient was verified as a REGISTERED PUNDAR account
    var recipientVerified by remember { mutableStateOf(false) }

    var totalAmountInput by remember { mutableStateOf("850") }
    var expenseNameInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Utilities") }
    var activeWizardBillId by remember { mutableStateOf("BILL-${UUID.randomUUID().toString().take(8).uppercase()}") }
    var isFiatMode by remember { mutableStateOf(true) } // PHP mode

    // ── Step 3 Split Mode ─────────────────────────────────────────
    var splitModeIndex by remember { mutableIntStateOf(0) } // 0 = Equal, 1 = Itemized

    // Selected group members (starts with "You" + verified recipient)
    val selectedMembers = remember {
        mutableStateListOf(
            BillMember("You", "@currentuser", "YOU", 0.0, 0xFF0B192C)
        )
    }

    val itemizedAmounts = remember { mutableStateMapOf<String, String>() }

    // Dialog state handlers
    var showGenerateGroupQrDialog by remember { mutableStateOf(false) }

    // Real Stellar Testnet payment state
    val payScope = rememberCoroutineScope()
    val payContext = LocalContext.current
    var showPayPinDialog by remember { mutableStateOf(false) }
    var payPinInput by remember { mutableStateOf("") }
    var isPaying by remember { mutableStateOf(false) }
    var payError by remember { mutableStateOf<String?>(null) }
    var txHash by remember { mutableStateOf("") }
    var paidXlm by remember { mutableDoubleStateOf(0.0) }

    // Reference ID generated for payment
    var generatedRefNo by remember { mutableStateOf("001318387") }
    var completedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Target bill amount is ALWAYS determined by Step 2 input (never overwritten by itemized sum)
    val rawTotal = totalAmountInput.toDoubleOrNull() ?: 0.0

    val exchangeRate = AppState.getPhpRate()
    val totalInPhp = if (isFiatMode) rawTotal else rawTotal * exchangeRate
    val totalInXlm = if (isFiatMode) rawTotal / exchangeRate else rawTotal

    // Equal split helper with cent-accurate distribution so sum of shares == totalInPhp
    val equalSharesByMember = remember(totalInPhp, selectedMembers.size, selectedMembers.toList()) {
        val count = selectedMembers.size
        if (count == 0 || totalInPhp <= 0.0) {
            emptyMap()
        } else {
            val totalCents = kotlin.math.round(totalInPhp * 100.0).toLong()
            val baseCents = totalCents / count
            val remainderCents = (totalCents % count).toInt()
            selectedMembers.mapIndexed { index, member ->
                val memberCents = baseCents + if (index < remainderCents) 1L else 0L
                member.name to (memberCents / 100.0)
            }.toMap()
        }
    }

    val sharePerMemberPhp = if (selectedMembers.isNotEmpty() && totalInPhp > 0.0) {
        totalInPhp / selectedMembers.size
    } else 0.0

    // Itemized allocation metrics (in PHP)
    val allocatedItemizedPhp = selectedMembers.sumOf { member ->
        (itemizedAmounts[member.name]?.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    }
    val remainingItemizedPhp = totalInPhp - allocatedItemizedPhp
    val isItemizedMatched = totalInPhp > 0.0 &&
        kotlin.math.abs(remainingItemizedPhp) < 0.01 &&
        selectedMembers.all { (itemizedAmounts[it.name]?.toDoubleOrNull() ?: -1.0) >= 0.0 }

    // Compute final member list with exact assigned PHP amounts
    val computedMembers = remember(
        splitModeIndex,
        selectedMembers.toList(),
        equalSharesByMember,
        itemizedAmounts.toMap()
    ) {
        selectedMembers.map { member ->
            val memberPhp = if (splitModeIndex == 0) {
                equalSharesByMember[member.name] ?: sharePerMemberPhp
            } else {
                (itemizedAmounts[member.name]?.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
            }
            member.copy(amount = memberPhp)
        }
    }

    val yourSharePhp = computedMembers.find { it.name == "You" }?.amount ?: sharePerMemberPhp
    val yourShareXlm = if (exchangeRate > 0.0) yourSharePhp / exchangeRate else 0.0

    // Pays the user's share to the verified recipient on Stellar Testnet, then shows the receipt.
    fun submitStellarPayment(pin: String) {
        payScope.launch {
            isPaying = true
            payError = null
            try {
                val recipientKey = merchantAddress
                if (!(recipientKey.startsWith("G") && recipientKey.length == 56)) {
                    payError = "Recipient has no Stellar wallet. Pick a verified PUNDAR account."
                    return@launch
                }
                val stroops = Stroops.fromXlmString(String.format(Locale.US, "%.7f", yourShareXlm))
                if (stroops == null || stroops <= 0) {
                    payError = "Your share must be greater than zero."
                    return@launch
                }
                val senderSeed = AuthRepository.getCurrentUserEncryptedSeed().orEmpty()
                if (senderSeed.isBlank()) {
                    payError = "Your Stellar wallet is not set up."
                    return@launch
                }
                val secret = try {
                    StellarWalletManager.decryptSecretSeed(senderSeed, pin)
                } catch (e: Exception) {
                    payError = "Incorrect PIN."
                    return@launch
                }
                when (val result = PayStellarClient.sendPayment(secret, recipientKey, stroops, "PUNDAR Split")) {
                    is PaymentResult.Success -> {
                        txHash = result.hash
                        paidXlm = yourShareXlm
                        generatedRefNo = result.hash.take(12).uppercase()
                        completedTimestamp = System.currentTimeMillis()

                        val cleanExpenseTitle = expenseNameInput.trim()
                            .takeIf { it.isNotBlank() && !it.equals(merchantName, ignoreCase = true) }
                            ?: "Split with $merchantName"
                        val formattedBillName = if (cleanExpenseTitle.startsWith("[")) {
                            cleanExpenseTitle
                        } else {
                            "[$selectedCategory] $cleanExpenseTitle"
                        }

                        // Find any existing pending bill from this wizard session or matching title so we update it instead of creating a duplicate
                        val (parsedCleanTitle, _) = parseCategorizedTitle(formattedBillName)
                        val existingPendingBill = AppState.bills.firstOrNull { b ->
                            b.id == activeWizardBillId ||
                                (b.status != BillStatus.SETTLED &&
                                    parseCategorizedTitle(b.name).first.equals(parsedCleanTitle, ignoreCase = true))
                        }
                        val finalBillId = existingPendingBill?.id ?: activeWizardBillId

                        // Remove any pending/duplicate bills for this transaction
                        AppState.bills.removeAll { b ->
                            b.id == finalBillId ||
                                b.id == activeWizardBillId ||
                                (b.status != BillStatus.SETTLED &&
                                    parseCategorizedTitle(b.name).first.equals(parsedCleanTitle, ignoreCase = true))
                        }

                        val newBill = GroupBill(
                            id = finalBillId,
                            name = formattedBillName,
                            totalAmount = totalInPhp,
                            memberCount = computedMembers.size.coerceAtLeast(2),
                            status = BillStatus.SETTLED,
                            date = "Today",
                            yourShare = yourSharePhp,
                            members = computedMembers
                        )
                        AppState.addBill(newBill, recordActivity = false)
                        AppState.markBillSettled(finalBillId)

                        // Remove any pending activity for the same wizard bill ID so there is only 1 activity
                        AppState.homeActivities.removeAll { act ->
                            act.referenceId == finalBillId || act.referenceId == activeWizardBillId
                        }

                        val activity = HomeActivity(
                            icon = "send",
                            title = formattedBillName,
                            subtitle = "Paid to $merchantName • Split settled",
                            amount = "-₱${String.format(Locale.US, "%,.2f", yourSharePhp)}",
                            isPositive = false,
                            module = "Pay",
                            referenceId = result.hash
                        )
                        AppState.addHomeActivity(activity)
                        AuthRepository.getCurrentUserId()?.let { HomeRepository.createActivity(it, activity) }
                        AppState.refreshWalletBalance()
                        AppState.requestHomeRefresh()

                        showPayPinDialog = false
                        currentStep = 5
                    }
                    is PaymentResult.Failure -> payError = result.message
                    is PaymentResult.Unknown -> payError = "Network timed out. Check your balance before retrying."
                }
            } catch (e: Exception) {
                payError = e.message ?: "Payment failed."
            } finally {
                isPaying = false
                payPinInput = ""
            }
        }
    }

    Scaffold(
        topBar = {
            if (currentStep < 5) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .background(Color(0xFF0B1B3D))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (currentStep > 1) currentStep-- else navController.navigateUp()
                                    },
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF1E2E5C))
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (currentStep == 1) "Identify Recipient" else "Send Confirm",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "PUNDAR PAY",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8),
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { navController.navigateUp() },
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF1E2E5C))
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E2E5C))
                                        .border(1.dp, Color(0xFF3B82F6), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = "User", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = PundarTheme.colors.bgPrimary
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Step Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentStep) {
                        1 -> Step1IdentifyRecipient(
                            merchantName = merchantName,
                            merchantRef = merchantRef,
                            merchantAddress = merchantAddress,
                            isRecipientVerified = recipientVerified,
                            onSelectMerchant = { name, ref, addr, scannedAmount, scannedTotal, scannedCount, scannedBillId, scannedBillRef ->
                                merchantName = name
                                merchantRef = ref
                                merchantAddress = addr
                                if (!scannedBillId.isNullOrBlank()) {
                                    activeWizardBillId = scannedBillId
                                }
                                if (!scannedBillRef.isNullOrBlank()) {
                                    val (parsedTitle, parsedCat) = parseCategorizedTitle(scannedBillRef)
                                    expenseNameInput = parsedTitle
                                    if (!parsedCat.isNullOrBlank()) {
                                        selectedCategory = parsedCat
                                    }
                                }
                                recipientVerified = true
                                // Automatically include the verified recipient in selectedMembers
                                val parts = name.trim().split(" ").filter { it.isNotBlank() }
                                val initials = when {
                                    parts.size >= 2 -> (parts.first().take(1) + parts.last().take(1)).uppercase()
                                    parts.size == 1 -> parts[0].take(2).uppercase()
                                    else -> "PU"
                                }
                                if (selectedMembers.none { it.name.equals(name, ignoreCase = true) }) {
                                    selectedMembers.add(
                                        BillMember(
                                            name = name,
                                            username = if (ref.startsWith("@")) ref else "@$ref",
                                            initials = initials,
                                            amount = scannedAmount ?: 0.0,
                                            avatarColor = 0xFF2563EB
                                        )
                                    )
                                }
                                val targetTotal = scannedTotal?.takeIf { it > 0.0 }
                                    ?: if (scannedAmount != null && scannedAmount > 0.0) {
                                        scannedAmount * (scannedCount?.coerceAtLeast(2) ?: selectedMembers.size.coerceAtLeast(2))
                                    } else null
                                if (targetTotal != null && targetTotal > 0.0) {
                                    totalAmountInput = if (targetTotal % 1.0 == 0.0) {
                                        targetTotal.toLong().toString()
                                    } else {
                                        String.format(Locale.US, "%.2f", targetTotal)
                                    }
                                }
                            },
                            onRecipientVerified = { user ->
                                merchantName = user.name
                                merchantRef = user.phone
                                merchantAddress = user.stellarPublicKey?.takeIf { it.isNotBlank() } ?: "${user.phone}*pundar.site"
                                recipientVerified = true
                                val parts = user.name.trim().split(" ").filter { it.isNotBlank() }
                                val initials = when {
                                    parts.size >= 2 -> (parts.first().take(1) + parts.last().take(1)).uppercase()
                                    parts.size == 1 -> parts[0].take(2).uppercase()
                                    else -> "PU"
                                }
                                if (selectedMembers.none { it.name.equals(user.name, ignoreCase = true) }) {
                                    selectedMembers.add(
                                        BillMember(
                                            name = user.name,
                                            username = "@${user.phone}",
                                            initials = initials,
                                            amount = 0.0,
                                            avatarColor = 0xFF2563EB
                                        )
                                    )
                                }
                                currentStep = 2
                            },
                            onNext = { currentStep = 2 }
                        )

                        2 -> Step2AmountAndBillSplit(
                            merchantName = merchantName,
                            merchantRef = merchantRef,
                            merchantAddress = merchantAddress,
                            expenseNameInput = expenseNameInput,
                            onExpenseNameChange = { expenseNameInput = it },
                            selectedCategory = selectedCategory,
                            onCategoryChange = { selectedCategory = it },
                            totalAmountInput = totalAmountInput,
                            onAmountChange = { totalAmountInput = it },
                            totalInPhp = totalInPhp,
                            totalInXlm = totalInXlm,
                            selectedMembers = selectedMembers,
                            onOpenGenerateQr = { showGenerateGroupQrDialog = true },
                            onAddMember = { member ->
                                if (selectedMembers.none { it.name.equals(member.name, ignoreCase = true) }) {
                                    selectedMembers.add(member)
                                }
                            },
                            onRemoveMember = { m ->
                                if (m.name != "You") {
                                    selectedMembers.remove(m)
                                    itemizedAmounts.remove(m.name)
                                }
                            },
                            onNext = { currentStep = 3 }
                        )

                        3 -> Step3ReviewAndSplit(
                            splitModeIndex = splitModeIndex,
                            onSplitModeChange = { newMode ->
                                splitModeIndex = newMode
                                // Pre-fill itemized amounts with equal cent-accurate shares if empty
                                if (newMode == 1 && itemizedAmounts.values.all { it.isBlank() }) {
                                    selectedMembers.forEach { m ->
                                        val share = equalSharesByMember[m.name] ?: sharePerMemberPhp
                                        itemizedAmounts[m.name] = String.format(Locale.US, "%.2f", share)
                                    }
                                }
                            },
                            selectedMembers = selectedMembers,
                            equalSharesByMember = equalSharesByMember,
                            totalInPhp = totalInPhp,
                            sharePerMemberPhp = sharePerMemberPhp,
                            itemizedAmounts = itemizedAmounts,
                            allocatedItemizedPhp = allocatedItemizedPhp,
                            remainingItemizedPhp = remainingItemizedPhp,
                            isItemizedMatched = isItemizedMatched,
                            onEqualizeAll = {
                                selectedMembers.forEach { m ->
                                    val share = equalSharesByMember[m.name] ?: sharePerMemberPhp
                                    itemizedAmounts[m.name] = String.format(Locale.US, "%.2f", share)
                                }
                            },
                            onClearAll = {
                                itemizedAmounts.clear()
                            },
                            onNext = { currentStep = 4 }
                        )

                        4 -> Step4ConfirmSend(
                            merchantName = merchantName,
                            merchantRef = merchantRef,
                            expenseName = expenseNameInput,
                            totalInPhp = totalInPhp,
                            totalInXlm = totalInXlm,
                            yourSharePhp = yourSharePhp,
                            members = computedMembers,
                            yourShareXlm = yourShareXlm,
                            isProcessing = isPaying,
                            errorMessage = payError,
                            onConfirm = {
                                payError = null
                                payPinInput = ""
                                showPayPinDialog = true
                            }
                        )

                        5 -> Step5InvoiceReceipt(
                            merchantName = merchantName.ifBlank { "Recipient" },
                            merchantRef = merchantRef,
                            isQrPh = isQrPhMerchant,
                            amountPhp = totalInPhp,
                            yourSharePhp = yourSharePhp,
                            paidXlm = paidXlm,
                            txHash = txHash,
                            members = computedMembers,
                            refNo = generatedRefNo,
                            timestamp = completedTimestamp,
                            onClose = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────
    if (showPayPinDialog) {
        AlertDialog(
            onDismissRequest = { if (!isPaying) { showPayPinDialog = false; payPinInput = "" } },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Confirm with PIN", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = {
                Column {
                    Text(
                        "Pay ₱${String.format(Locale.US, "%,.2f", yourSharePhp)} (≈ ${String.format(Locale.US, "%.4f", yourShareXlm)} XLM) to $merchantName on Stellar Testnet.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = payPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) payPinInput = it },
                        label = { Text("4-digit PIN") },
                        singleLine = true,
                        enabled = !isPaying,
                        isError = payError != null,
                        supportingText = payError?.let { msg -> { Text(msg) } },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { submitStellarPayment(payPinInput) },
                    enabled = payPinInput.length == 4 && !isPaying,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B192C))
                ) {
                    if (isPaying) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Pay Now", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPayPinDialog = false; payPinInput = "" },
                    enabled = !isPaying
                ) { Text("Cancel") }
            }
        )
    }

    if (showGenerateGroupQrDialog) {
        val joiningCount = (selectedMembers.size - 1).coerceAtLeast(1)
        GenerateGroupQrModalDialog(
            activeBillId = activeWizardBillId,
            expenseName = expenseNameInput,
            initialCategory = selectedCategory,
            totalBillAmount = totalInPhp,
            initialJoiningCount = joiningCount,
            initialAmount = if (totalInPhp > 0.0) totalInPhp / (joiningCount + 1) else if (sharePerMemberPhp > 0.0) sharePerMemberPhp else 0.0,
            onSyncBillConfig = { syncedTitle, syncedCat, syncedTotalPhp, syncedJoiningCount ->
                expenseNameInput = syncedTitle
                selectedCategory = syncedCat
                if (syncedTotalPhp > 0.0) {
                    totalAmountInput = if (syncedTotalPhp % 1.0 == 0.0) {
                        syncedTotalPhp.toLong().toString()
                    } else {
                        String.format(Locale.US, "%.2f", syncedTotalPhp)
                    }
                }
                val targetTotalMembers = syncedJoiningCount + 1
                while (selectedMembers.size < targetTotalMembers) {
                    val idx = selectedMembers.size
                    selectedMembers.add(
                        BillMember(
                            name = "Participant $idx",
                            username = "",
                            initials = "P$idx",
                            amount = 0.0,
                            avatarColor = 0xFF10B981
                        )
                    )
                }
            },
            onDismiss = { showGenerateGroupQrDialog = false }
        )
    }
}

// ═════════════════════════════════════════════════════════════════
//  STEP 1: Identify Recipient (Matches Picture 1)
// ═════════════════════════════════════════════════════════════════
@Composable
private fun Step1IdentifyRecipient(
    merchantName: String,
    merchantRef: String,
    merchantAddress: String,
    isRecipientVerified: Boolean,
    onSelectMerchant: (
        name: String,
        ref: String,
        addr: String,
        scannedAmount: Double?,
        scannedTotal: Double?,
        scannedCount: Int?,
        scannedBillId: String?,
        scannedBillRef: String?
    ) -> Unit,
    onRecipientVerified: (AuthRepository.UserData) -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Scan QR, 1 = Phone
    var manualSearchQuery by remember { mutableStateOf("") }
    var isLookingUp by remember { mutableStateOf(false) }
    var lookupError by remember { mutableStateOf<String?>(null) }

    // Inline CameraX + ML Kit QR scanner state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val scannerOptions = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    }
    val barcodeScanner = remember { BarcodeScanning.getClient(scannerOptions) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanLocked = remember { AtomicBoolean(false) }
    var cameraRef by remember { mutableStateOf<Camera?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }

    var scannedResult by remember { mutableStateOf<String?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var scanMode by remember { mutableStateOf("camera") }

    LaunchedEffect(isProcessing, scannedResult) {
        scanLocked.set(isProcessing || scannedResult != null)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    suspend fun handleScannedRaw(raw: String) {
        isProcessing = true
        scanError = null
        try {
            when (val result = QrScanHandler.processRawQr(raw)) {
                is QrScanResult.Success -> {
                    val payload = result.payload
                    val myId = AuthRepository.getCurrentUserId()
                    val myPhone = AuthRepository.getCurrentUserPhone()
                    val myPk = AuthRepository.getCurrentUserStellarPublicKey()
                    if ((myPhone.isNotBlank() && payload.userId == myPhone) ||
                        (myId != null && payload.userId == myId) ||
                        (!myPk.isNullOrBlank() && (payload.userId == myPk || payload.walletId == myPk))
                    ) {
                        scanError = "You can't send to your own account."
                        scannedResult = "error"
                        return
                    }

                    val candidates = listOfNotNull(payload.walletId, payload.username, payload.userId)
                        .filter { it.isNotBlank() }
                    var matchedUser: AuthRepository.UserData? = null
                    for (candidate in candidates) {
                        matchedUser = AuthRepository.findRegisteredUser(candidate)
                        if (matchedUser != null) break
                    }

                    val recipientLookup = QrPaymentRepository.lookupRecipient(payload.userId).getOrNull()
                    val resolvedName = matchedUser?.name?.takeIf { it.isNotBlank() }
                        ?: recipientLookup?.displayName?.takeIf { it.isNotBlank() }
                        ?: payload.displayName?.takeIf { it.isNotBlank() }
                        ?: payload.username?.takeIf { it.isNotBlank() }
                        ?: "PUNDAR User"
                    val resolvedRef = payload.billRef?.takeIf { it.isNotBlank() }
                        ?: payload.transactionId?.takeIf { it.isNotBlank() }
                        ?: matchedUser?.phone?.takeIf { it.isNotBlank() }
                        ?: payload.username?.takeIf { it.isNotBlank() }
                        ?: payload.userId
                    val resolvedAddr = listOfNotNull(
                        matchedUser?.stellarPublicKey,
                        recipientLookup?.walletId,
                        payload.walletId,
                        payload.userId
                    ).firstOrNull { it.startsWith("G") && it.length == 56 }
                        ?: "${matchedUser?.phone ?: payload.userId}*pundar.site"

                    val amountPhp = payload.amount?.takeIf { it > 0.0 }?.let { rawAmt ->
                        if (payload.currency.equals("XLM", ignoreCase = true)) AppState.xlmToPhp(rawAmt) else rawAmt
                    }

                    onSelectMerchant(
                        resolvedName,
                        resolvedRef,
                        resolvedAddr,
                        amountPhp,
                        payload.totalBillAmount,
                        payload.participantCount,
                        payload.transactionId,
                        payload.billRef
                    )
                    scannedResult = null
                    scanError = null
                    scanLocked.set(false)
                    Toast.makeText(context, "Recipient verified: $resolvedName", Toast.LENGTH_SHORT).show()
                    onNext()
                }
                is QrScanResult.Error -> {
                    // Fallback if QR encodes a direct phone number or Stellar public key
                    val directUser = AuthRepository.findRegisteredUser(raw.trim())
                    if (directUser != null) {
                        if (directUser.id == AuthRepository.currentUserState.value?.id) {
                            scanError = "You can't send to your own account."
                            scannedResult = "error"
                        } else {
                            val addr = directUser.stellarPublicKey?.takeIf { it.isNotBlank() }
                                ?: "${directUser.phone}*pundar.site"
                            onSelectMerchant(directUser.name, directUser.phone, addr, null, null, null, null, null)
                            scannedResult = null
                            scanError = null
                            scanLocked.set(false)
                            Toast.makeText(context, "Recipient verified: ${directUser.name}", Toast.LENGTH_SHORT).show()
                            onNext()
                        }
                    } else {
                        scanError = result.message
                        scannedResult = "error"
                    }
                }
            }
        } catch (e: Exception) {
            scanError = e.message ?: QrScanHandler.INVALID_QR
            scannedResult = "error"
        } finally {
            isProcessing = false
        }
    }

    fun verifyManualRecipient() {
        val query = manualSearchQuery.trim()
        if (query.isBlank() || isLookingUp) return
        scope.launch {
            isLookingUp = true
            lookupError = null
            val user = AuthRepository.findRegisteredUser(query)
            isLookingUp = false
            when {
                user == null -> lookupError = "No registered PUNDAR account found for this number."
                user.id == AuthRepository.currentUserState.value?.id -> lookupError = "You can't send to your own account."
                else -> {
                    val addr = user.stellarPublicKey?.takeIf { it.isNotBlank() } ?: "${user.phone}*pundar.site"
                    onSelectMerchant(user.name, user.phone, addr, null, null, null, null, null)
                    onNext()
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scanMode = "gallery"
        isProcessing = true
        scanError = null
        scannedResult = "processing"
        try {
            val image = InputImage.fromFilePath(context, uri)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val raw = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                    if (raw == null) {
                        scanError = QrScanHandler.NO_QR_FOUND
                        scannedResult = "error"
                        isProcessing = false
                    } else {
                        scope.launch {
                            handleScannedRaw(raw)
                        }
                    }
                }
                .addOnFailureListener {
                    scanError = QrScanHandler.NO_QR_FOUND
                    scannedResult = "error"
                    isProcessing = false
                }
        } catch (_: Exception) {
            scanError = QrScanHandler.NO_QR_FOUND
            scannedResult = "error"
            isProcessing = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── TOP STEP HEADER ───────────────────────────────────────
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0B192C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("1", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Step 1: Identify Recipient",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                    Text("1 of 4", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B192C))
                }

                Spacer(Modifier.height(8.dp))

                // 4 Horizontal Progress Segments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Merchant", "Amount", "Round-Up", "Confirm").forEachIndexed { idx, label ->
                        val isActive = idx == 0
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isActive) Color(0xFF0B192C) else Color(0xFFE2E8F0))
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isActive) Color(0xFF0B192C) else Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // ── TOGGLE PILL BUTTONS ──────────────────────────────────
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFE2E8F0)
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    // Button 1: Scan QR Code
                    Surface(
                        onClick = {
                            activeTab = 0
                            scanLocked.set(false)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        color = if (activeTab == 0) Color(0xFF0B192C) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.PhotoCamera,
                                contentDescription = null,
                                tint = if (activeTab == 0) Color.White else Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Scan QR Code",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == 0) Color.White else Color(0xFF475569)
                            )
                        }
                    }

                    // Button 2: Stellar ID / Phone
                    Surface(
                        onClick = {
                            activeTab = 1
                            // Pause the scanner while the manual lookup panel is shown
                            scanLocked.set(true)
                            if (torchEnabled) {
                                torchEnabled = false
                                cameraRef?.cameraControl?.enableTorch(false)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        color = if (activeTab == 1) Color(0xFF0B192C) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Dialpad,
                                contentDescription = null,
                                tint = if (activeTab == 1) Color.White else Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Stellar ID / Phone",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == 1) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }

        // ── DARK BLUE QR SCANNER VIEWPORT BOX (DIRECT LIVE CAMERA) ─
        // When "Stellar ID / Phone" is selected, the Find Recipient panel replaces the scanner.
        if (activeTab == 1) item {
            FindRecipientPanel(
                onCancel = {
                    activeTab = 0
                    scanLocked.set(false)
                },
                onVerified = { user -> onRecipientVerified(user) }
            )
        }
        if (activeTab == 0) item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (hasCameraPermission) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                .background(Color.Black)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx).apply {
                                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                        scaleType = PreviewView.ScaleType.FILL_CENTER
                                    }
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }
                                        val resolutionSelector = ResolutionSelector.Builder()
                                            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                                            .build()
                                        val imageAnalyzer = ImageAnalysis.Builder()
                                            .setResolutionSelector(resolutionSelector)
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()
                                        imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                                            if (!scanLocked.get()) {
                                                processGroupBillImageProxy(barcodeScanner, imageProxy) { barcode ->
                                                    if (scanLocked.compareAndSet(false, true)) {
                                                        ContextCompat.getMainExecutor(ctx).execute {
                                                            scannedResult = barcode
                                                            scanMode = "camera"
                                                        }
                                                    }
                                                }
                                            } else {
                                                imageProxy.close()
                                            }
                                        }
                                        try {
                                            cameraProvider.unbindAll()
                                            val camera = cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                CameraSelector.DEFAULT_BACK_CAMERA,
                                                preview,
                                                imageAnalyzer
                                            )
                                            cameraRef = camera
                                            camera.cameraControl.enableTorch(torchEnabled)
                                        } catch (_: Exception) { }
                                    }, ContextCompat.getMainExecutor(ctx))
                                    previewView
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(230.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(24.dp),
                                    border = BorderStroke(2.dp, PundarTheme.colors.brandPrimary)
                                ) {}
                            }

                            // Torch toggle button (matches ScanScreen)
                            if (cameraRef?.cameraInfo?.hasFlashUnit() == true) {
                                Surface(
                                    onClick = {
                                        val next = !torchEnabled
                                        torchEnabled = next
                                        cameraRef?.cameraControl?.enableTorch(next)
                                    },
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.55f),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                        .size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                            contentDescription = "Toggle Flashlight",
                                            tint = if (torchEnabled) Gold500 else Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = PundarTheme.colors.textSecondary
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Camera permission is required to scan QR codes.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PundarTheme.colors.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                                    Text("Grant Permission")
                                }
                            }
                        }
                    }

                    // Bottom Action Section (matches ScanScreen)
                    Surface(
                        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when {
                                isProcessing -> {
                                    CircularProgressIndicator(
                                        color = PundarTheme.colors.brandPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        if (scanMode == "gallery") "Processing image..." else "Validating QR...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PundarTheme.colors.textSecondary
                                    )
                                }
                                scanError != null -> {
                                    Text(
                                        scanError!!,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = {
                                        scannedResult = null
                                        scanError = null
                                        isProcessing = false
                                        scanLocked.set(false)
                                    }) { Text("Try Again") }
                                }
                                scannedResult != null && scanError == null -> {
                                    LaunchedEffect(scannedResult) {
                                        val raw = scannedResult ?: return@LaunchedEffect
                                        if (raw == "processing" || raw == "error") return@LaunchedEffect
                                        handleScannedRaw(raw)
                                    }
                                    Text(
                                        "QR detected",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PundarTheme.colors.textPrimary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Validating recipient...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PundarTheme.colors.textSecondary
                                    )
                                }
                                else -> {
                                    Text(
                                        "Scan or Upload QR",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PundarTheme.colors.textPrimary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Supports Receive Money, Bill Payment, and Merchant QR codes.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PundarTheme.colors.textSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(20.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                if (!hasCameraPermission) {
                                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                                } else {
                                                    scannedResult = null
                                                    scanError = null
                                                    isProcessing = false
                                                    scanLocked.set(false)
                                                }
                                            },
                                            modifier = Modifier.weight(1f).height(72.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                                            border = BorderStroke(1.dp, ElectricBlue.copy(0.5f))
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                FuturisticIcon(
                                                    icon = Icons.Default.QrCodeScanner,
                                                    tint = ElectricBlue,
                                                    size = 36.dp,
                                                    iconSize = 18.dp,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                Spacer(Modifier.height(6.dp))
                                                Text("Scan QR", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                galleryLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            modifier = Modifier.weight(1f).height(72.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                            border = BorderStroke(1.dp, NeonCyan.copy(0.5f))
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                FuturisticIcon(
                                                    icon = Icons.Default.Image,
                                                    tint = NeonCyan,
                                                    size = 36.dp,
                                                    iconSize = 18.dp,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                Spacer(Modifier.height(6.dp))
                                                Text("Upload Image", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── MANUAL MERCHANT SEARCH SECTION ───────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Storefront, contentDescription = null, tint = Color(0xFF0B192C), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Recipient", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                        if (isRecipientVerified) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Verified, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Verified", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (isRecipientVerified) "$merchantName · $merchantRef"
                        else "No recipient yet. Scan a PUNDAR QR or enter a registered number.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isRecipientVerified) Color(0xFF1E3A8A) else Color(0xFF64748B)
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = manualSearchQuery,
                        onValueChange = { manualSearchQuery = it; lookupError = null },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Registered mobile number or Stellar address") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                        trailingIcon = {
                            if (isLookingUp) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                TextButton(
                                    onClick = { verifyManualRecipient() },
                                    enabled = manualSearchQuery.isNotBlank()
                                ) { Text("Verify", fontWeight = FontWeight.Bold) }
                            }
                        },
                        singleLine = true,
                        isError = lookupError != null,
                        supportingText = lookupError?.let { msg -> { Text(msg) } },
                        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { verifyManualRecipient() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF0B192C),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // ── BOTTOM ACTION BUTTON ──────────────────────────────────
        item {
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onNext,
                enabled = isRecipientVerified,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp), // Rounded Pill Button
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B192C))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Continue to Amount", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "Secured via Stellar Soroban Anchor Protocol",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
//  STEP 2: Amount & Bill Split (Matches Picture 2 with Dark Blue Palette)
// ═════════════════════════════════════════════════════════════════
@Composable
private fun Step2AmountAndBillSplit(
    merchantName: String,
    merchantRef: String,
    merchantAddress: String,
    expenseNameInput: String,
    onExpenseNameChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    totalAmountInput: String,
    onAmountChange: (String) -> Unit,
    totalInPhp: Double,
    totalInXlm: Double,
    selectedMembers: List<BillMember>,
    onOpenGenerateQr: () -> Unit,
    onAddMember: (BillMember) -> Unit,
    onRemoveMember: (BillMember) -> Unit,
    onNext: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var friendPhoneInput by remember { mutableStateOf("") }
    var isAddingFriend by remember { mutableStateOf(false) }
    var addFriendError by remember { mutableStateOf<String?>(null) }
    val avatarColors = remember { listOf(0xFFEC4899, 0xFFF59E0B, 0xFF10B981, 0xFF8B5CF6, 0xFF0EA5E9) }

    fun addParticipantByPhone() {
        val query = friendPhoneInput.trim()
        if (query.isBlank() || isAddingFriend) return
        scope.launch {
            isAddingFriend = true
            addFriendError = null
            val user = AuthRepository.findRegisteredUser(query)
            isAddingFriend = false
            when {
                user == null -> addFriendError = "No registered PUNDAR account found for this number."
                user.id == AuthRepository.currentUserState.value?.id -> addFriendError = "You're already included in this split."
                selectedMembers.any { it.name.equals(user.name, ignoreCase = true) } -> addFriendError = "${user.name} is already in this split."
                else -> {
                    val parts = user.name.trim().split(" ").filter { it.isNotBlank() }
                    val initials = when {
                        parts.size >= 2 -> (parts.first().take(1) + parts.last().take(1)).uppercase()
                        parts.size == 1 -> parts[0].take(2).uppercase()
                        else -> "PU"
                    }
                    onAddMember(
                        BillMember(
                            name = user.name,
                            username = "@${user.phone}",
                            initials = initials,
                            amount = 0.0,
                            avatarColor = avatarColors[selectedMembers.size % avatarColors.size]
                        )
                    )
                    friendPhoneInput = ""
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── TOP STEP PROGRESS BAR ──────────────────────────────────
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE0E7FF))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("STEP 2 OF 4", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E3A8A))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Amount & Bill Split",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                    Text("50% Completed", fontSize = 12.sp, color = Color(0xFF64748B))
                }

                Spacer(Modifier.height(8.dp))

                // Progress Bar (2 segments filled)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 1..4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (i <= 2) Color(0xFF0B192C) else Color(0xFFE2E8F0))
                        )
                    }
                }
            }
        }

        // ── MERCHANT CARD (Matching Picture 2 with Dark Blue Palette) ─────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Merchant avatar with checkmark
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Storefront, contentDescription = null, tint = Color(0xFF0B192C), modifier = Modifier.size(26.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0B192C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                merchantName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("★ 4.9", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "$merchantRef • $merchantAddress",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Storefront, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // ── PAYABLE TOTAL CARD (Matching Picture 2 with Dark Blue Palette) ────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "PAYABLE TOTAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    // Huge amount text with Dark Blue Peso sign
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "₱",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0B192C)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            String.format("%,.2f", totalInPhp),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    // Pill Badge: • ≈ 72.45 XLM | PHPX Anchored
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFEFF6FF))
                            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "≈ ${String.format("%,.2f", totalInXlm)} XLM  |  PHPX Anchored",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Bill / Expense Title input field
                    OutlinedTextField(
                        value = expenseNameInput,
                        onValueChange = onExpenseNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Bill / Expense Title (e.g. WiFi, Dinner)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF0B192C),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    // Category selector chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Food & Dining", "Utilities", "Rent", "Travel", "Other").forEach { cat ->
                            val isSelected = selectedCategory == cat
                            OutlinedButton(
                                onClick = { onCategoryChange(cat) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF0B192C) else Color(0xFFE2E8F0)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) Color(0xFFE0E7FF) else Color.White
                                )
                            ) {
                                Text(
                                    cat,
                                    fontSize = 11.sp,
                                    color = Color(0xFF0F172A),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Custom bill amount input field
                    OutlinedTextField(
                        value = totalAmountInput,
                        onValueChange = { raw ->
                            val cleaned = raw.filter { it.isDigit() || it == '.' }
                            if (cleaned.count { it == '.' } <= 1) {
                                val parts = cleaned.split('.')
                                if (parts.size <= 1 || parts[1].length <= 2) {
                                    onAmountChange(cleaned)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Enter Bill Amount (PHP)") },
                        prefix = { Text("₱ ", fontWeight = FontWeight.Bold, color = Color(0xFF0B192C)) },
                        trailingIcon = {
                            if (totalAmountInput.isNotEmpty()) {
                                IconButton(onClick = { onAmountChange("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF0B192C),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    // ── QUICK PRESET BUTTONS ROW (Dark Blue Theme) ────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("+₱50", "+₱100", "+₱500").forEach { preset ->
                            Surface(
                                onClick = {
                                    val add = preset.replace("+₱", "").toDoubleOrNull() ?: 0.0
                                    val current = totalAmountInput.toDoubleOrNull() ?: 0.0
                                    val updated = current + add
                                    onAmountChange(
                                        if (updated % 1.0 == 0.0) updated.toLong().toString()
                                        else String.format(Locale.US, "%.2f", updated)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEFF6FF)
                            ) {
                                Text(
                                    preset,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A8A),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = { onAmountChange("850") },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE0E7FF)
                        ) {
                            Text(
                                "Exact Bill",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0B192C),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── INVITE RECIPIENTS SECTION ─────────────────────────────
        item {
            val sharePerPerson = if (selectedMembers.isNotEmpty() && totalInPhp > 0.0) {
                totalInPhp / selectedMembers.size
            } else 0.0

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Invite Recipients to Split",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        if (sharePerPerson > 0.0) {
                            Surface(
                                color = Color(0xFFE0E7FF),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "₱${String.format(Locale.US, "%,.2f", sharePerPerson)} / person",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A8A),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Settle the amount needed from each user before generating the QR code or inviting.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(Modifier.height(12.dp))

                    // Inline add participant by mobile number (replaces the Invite to Split modal pop-up)
                    OutlinedTextField(
                        value = friendPhoneInput,
                        onValueChange = {
                            friendPhoneInput = it
                            addFriendError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Add Participant (Registered Mobile Number)") },
                        singleLine = true,
                        enabled = !isAddingFriend,
                        isError = addFriendError != null,
                        supportingText = addFriendError?.let { msg -> { Text(msg) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = { addParticipantByPhone() }
                        ),
                        trailingIcon = {
                            if (isAddingFriend) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                TextButton(
                                    onClick = { addParticipantByPhone() },
                                    enabled = friendPhoneInput.isNotBlank()
                                ) {
                                    Text("Add", fontWeight = FontWeight.Bold, color = Color(0xFF0B192C))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF0B192C),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onOpenGenerateQr,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF0B192C))
                    ) {
                        Icon(Icons.Filled.QrCode, contentDescription = null, tint = Color(0xFF0B192C), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Generate Invite QR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B192C))
                    }
                }
            }
        }

        // Current Members List
        item {
            val joiningCount = (selectedMembers.size - 1).coerceAtLeast(0)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "People Joining (Excluding You): $joiningCount • Total Splitting Bill: ${selectedMembers.size}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    "Note: You (the creator) are excluded from the joining count, but included in splitting the bill (+1).",
                    fontSize = 11.sp,
                    color = Color(0xFF2563EB),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        items(selectedMembers) { m ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(m.avatarColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(m.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(m.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF0F172A))
                        Text(m.username, fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    if (m.name != "You") {
                        IconButton(onClick = { onRemoveMember(m) }) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            val canProceed = totalInPhp > 0.0 && selectedMembers.isNotEmpty()
            if (!canProceed) {
                Text(
                    "Please enter a bill amount greater than ₱0.00 to continue.",
                    fontSize = 12.sp,
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Button(
                onClick = onNext,
                enabled = canProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0B192C),
                    disabledContainerColor = Color(0xFF94A3B8)
                )
            ) {
                Text("Continue to Review (Step 3) ➔", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
//  STEP 3: Review & Edit Split
// ═════════════════════════════════════════════════════════════════
@Composable
private fun Step3ReviewAndSplit(
    splitModeIndex: Int,
    onSplitModeChange: (Int) -> Unit,
    selectedMembers: List<BillMember>,
    equalSharesByMember: Map<String, Double>,
    totalInPhp: Double,
    sharePerMemberPhp: Double,
    itemizedAmounts: MutableMap<String, String>,
    allocatedItemizedPhp: Double,
    remainingItemizedPhp: Double,
    isItemizedMatched: Boolean,
    onEqualizeAll: () -> Unit,
    onClearAll: () -> Unit,
    onNext: () -> Unit
) {
    val canProceedStep3 = if (splitModeIndex == 0) {
        totalInPhp > 0.0 && selectedMembers.isNotEmpty()
    } else {
        isItemizedMatched
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Review & Adjust Split",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                "Choose equal or itemized contribution per member",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFE2E8F0)
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    listOf("Equal Split", "Itemized Split").forEachIndexed { idx, label ->
                        val isSelected = splitModeIndex == idx
                        Surface(
                            onClick = { onSplitModeChange(idx) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color.White else Color.Transparent
                        ) {
                            Text(
                                label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) Color(0xFF0B192C) else Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── BILL SUMMARY & ALLOCATION TRACKER CARD ────────────────
        item {
            val isItemized = splitModeIndex == 1
            val isExceeded = isItemized && remainingItemizedPhp < -0.005
            val isShort = isItemized && remainingItemizedPhp > 0.005

            val borderColor = when {
                !isItemized -> Color(0xFFE2E8F0)
                isItemizedMatched -> Color(0xFF10B981)
                isExceeded -> Color(0xFFEF4444)
                else -> Color(0xFFF59E0B)
            }
            val bgColor = when {
                !isItemized -> Color(0xFFF8FAFC)
                isItemizedMatched -> Color(0xFFECFDF5)
                isExceeded -> Color(0xFFFEF2F2)
                else -> Color(0xFFFFFBEB)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Target Bill Amount", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text(
                                "₱${String.format("%,.2f", totalInPhp)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                        }
                        if (!isItemized) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Share Per Person", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(
                                    "₱${String.format("%,.2f", sharePerMemberPhp)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Allocated Total", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(
                                    "₱${String.format("%,.2f", allocatedItemizedPhp)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        isItemizedMatched -> Color(0xFF059669)
                                        isExceeded -> Color(0xFFDC2626)
                                        else -> Color(0xFFD97706)
                                    }
                                )
                            }
                        }
                    }

                    if (isItemized) {
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = borderColor.copy(alpha = 0.35f))
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = when {
                                        isItemizedMatched -> Icons.Filled.CheckCircle
                                        isExceeded -> Icons.Filled.Error
                                        else -> Icons.Filled.Info
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        isItemizedMatched -> Color(0xFF059669)
                                        isExceeded -> Color(0xFFDC2626)
                                        else -> Color(0xFFD97706)
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = when {
                                        isItemizedMatched -> "Exact match! All ₱${String.format("%,.2f", totalInPhp)} allocated."
                                        isExceeded -> "Exceeds bill by ₱${String.format("%,.2f", -remainingItemizedPhp)}"
                                        isShort -> "Remaining to allocate: ₱${String.format("%,.2f", remainingItemizedPhp)}"
                                        else -> "Enter each member's exact share"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isItemizedMatched -> Color(0xFF065F46)
                                        isExceeded -> Color(0xFF991B1B)
                                        else -> Color(0xFF92400E)
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Quick helper actions for Itemized mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onEqualizeAll,
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF0B192C)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Split Equally", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B192C))
                            }
                            OutlinedButton(
                                onClick = onClearAll,
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF64748B)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Individual Contributions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                if (splitModeIndex == 1) {
                    Text(
                        "Max total: ₱${String.format("%,.2f", totalInPhp)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        items(selectedMembers) { member ->
            val othersAllocatedPhp = selectedMembers
                .filter { it.name != member.name }
                .sumOf { (itemizedAmounts[it.name]?.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0) }
            val maxAllowedForMember = (totalInPhp - othersAllocatedPhp).coerceAtLeast(0.0)
            val currentMemberVal = itemizedAmounts[member.name]?.toDoubleOrNull() ?: 0.0

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(member.avatarColor)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(member.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF0F172A))
                            Text(member.username, fontSize = 11.sp, color = Color(0xFF64748B))
                        }

                        if (splitModeIndex == 0) {
                            val exactEqualShare = equalSharesByMember[member.name] ?: sharePerMemberPhp
                            Text(
                                "₱${String.format("%,.2f", exactEqualShare)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        } else {
                            OutlinedTextField(
                                value = itemizedAmounts[member.name] ?: "",
                                onValueChange = { rawInput ->
                                    // Sanitize numeric input (max 2 decimal places)
                                    val cleaned = rawInput.filter { it.isDigit() || it == '.' }
                                    if (cleaned.count { it == '.' } <= 1) {
                                        val parts = cleaned.split('.')
                                        if (parts.size <= 1 || parts[1].length <= 2) {
                                            if (cleaned.isEmpty()) {
                                                itemizedAmounts[member.name] = ""
                                            } else {
                                                val parsed = cleaned.toDoubleOrNull()
                                                if (parsed != null) {
                                                    // Prevent entering an amount that pushes the total over the bill amount
                                                    if (parsed <= maxAllowedForMember + 0.005) {
                                                        itemizedAmounts[member.name] = cleaned
                                                    } else {
                                                        // Clamp to the exact remaining unallocated balance
                                                        itemizedAmounts[member.name] = String.format(Locale.US, "%.2f", maxAllowedForMember)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.width(125.dp),
                                placeholder = { Text("0.00") },
                                prefix = { Text("₱ ") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isItemizedMatched) Color(0xFF059669) else Color(0xFF0B192C),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // Show "Fill Remaining (₱X.XX)" pill when in Itemized mode and there is unallocated remainder
                    if (splitModeIndex == 1 && kotlin.math.abs(maxAllowedForMember - currentMemberVal) >= 0.01) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                onClick = {
                                    itemizedAmounts[member.name] = String.format(Locale.US, "%.2f", maxAllowedForMember)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEFF6FF)
                            ) {
                                Text(
                                    text = "Set to remaining ₱${String.format("%,.2f", maxAllowedForMember)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A8A),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            if (splitModeIndex == 1 && !isItemizedMatched) {
                Text(
                    text = if (remainingItemizedPhp > 0.0) {
                        "Allocate the remaining ₱${String.format("%,.2f", remainingItemizedPhp)} so the split matches ₱${String.format("%,.2f", totalInPhp)}."
                    } else {
                        "Reduce itemized amounts by ₱${String.format("%,.2f", -remainingItemizedPhp)} to match ₱${String.format("%,.2f", totalInPhp)}."
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFDC2626),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )
            }
            Button(
                onClick = onNext,
                enabled = canProceedStep3,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0B192C),
                    disabledContainerColor = Color(0xFF94A3B8)
                )
            ) {
                Text("Proceed to Confirm (Step 4) ➔", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
//  STEP 4: Confirm Payment
// ═════════════════════════════════════════════════════════════════
@Composable
private fun Step4ConfirmSend(
    merchantName: String,
    merchantRef: String,
    expenseName: String,
    totalInPhp: Double,
    totalInXlm: Double,
    yourSharePhp: Double,
    members: List<BillMember>,
    yourShareXlm: Double,
    isProcessing: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Confirm Payment",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                "Please review details before sending payment",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("TOTAL PAYMENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Text(
                                "₱${String.format("%,.2f", totalInPhp)}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "~ ${String.format("%,.2f", totalInXlm)} XLM",
                                fontSize = 13.sp,
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0B192C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = null, tint = Color.White)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(Modifier.height(16.dp))

                    ConfirmDetailRow("Merchant / Recipient", merchantName)
                    ConfirmDetailRow("Merchant Ref", merchantRef)
                    ConfirmDetailRow("Expense Name", expenseName)
                    ConfirmDetailRow("Group Participants", "${members.size} Members")
                    ConfirmDetailRow("Your Portion", "₱${String.format("%,.2f", yourSharePhp)}")
                    ConfirmDetailRow("You Pay (XLM)", "${String.format(Locale.US, "%.4f", yourShareXlm)} XLM")
                    ConfirmDetailRow("Payment Rail", "Stellar Testnet / PUNDAR Pay")

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "SPLIT BREAKDOWN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                    Spacer(Modifier.height(6.dp))
                    members.forEach { m ->
                        ConfirmDetailRow(m.name, "₱${String.format("%,.2f", m.amount)}")
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (errorMessage != null && !isProcessing) {
            Text(errorMessage, color = Color(0xFFDC2626), fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
        }

        Button(
            onClick = onConfirm,
            enabled = !isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B192C))
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Confirm & Send Payment", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ConfirmDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Color(0xFF64748B))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
    }
}

// ═════════════════════════════════════════════════════════════════
//  STEP 5: Invoice / Receipt Screen
// ═════════════════════════════════════════════════════════════════
@Composable
private fun Step5InvoiceReceipt(
    merchantName: String,
    merchantRef: String,
    isQrPh: Boolean,
    amountPhp: Double,
    yourSharePhp: Double,
    paidXlm: Double,
    txHash: String,
    members: List<BillMember>,
    refNo: String,
    timestamp: Long,
    onClose: () -> Unit
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val sdfTime = SimpleDateFormat("h:mm a", Locale.getDefault())
    val sdfDate = SimpleDateFormat("MMMM dd, yyyy '|' h:mm a", Locale.getDefault())
    val timeStr = sdfTime.format(Date(timestamp))
    val fullDateStr = sdfDate.format(Date(timestamp))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF0F172A), modifier = Modifier.size(24.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0B192C)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Success", tint = Color.White, modifier = Modifier.size(34.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFE2E8F0))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0B192C))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Payment Successful", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B192C))
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Payment Sent Successfully!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A), textAlign = TextAlign.Center)
            Text("Today, $timeStr •", fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 4.dp))

            Spacer(Modifier.height(16.dp))

            Text("₱${String.format("%,.2f", yourSharePhp)}", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
            Text(
                "${String.format(Locale.US, "%.4f", paidXlm)} XLM · Stellar Testnet",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF059669),
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Storefront, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(merchantName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("Ref: $merchantRef", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    if (isQrPh) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFDCFCE7)).padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("✓ QR Ph", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Group, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Group Bill Settlement", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        val count = (members.size - 1).coerceAtLeast(1)
                        Text("$count/${members.size} Friends notified via PUNDAR Pay", fontSize = 12.sp, color = Color(0xFF64748B))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                        members.take(3).forEach { m ->
                            Box(
                                modifier = Modifier.size(26.dp).clip(CircleShape).background(Color(m.avatarColor)).border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(m.initials, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // GCash-style transaction details
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ConfirmDetailRow("Bill Total", "₱${String.format("%,.2f", amountPhp)}")
                    ConfirmDetailRow("Amount Paid (your share)", "₱${String.format("%,.2f", yourSharePhp)}")
                    ConfirmDetailRow("Network Fee", "0.00001 XLM")
                    ConfirmDetailRow("Network", "Stellar Testnet")
                    ConfirmDetailRow("Ref No.", refNo)
                    ConfirmDetailRow("Date", fullDateStr)
                    if (txHash.isNotBlank()) {
                        ConfirmDetailRow("Transaction Hash", "${txHash.take(8)}…${txHash.takeLast(8)}")
                    }
                }
            }

            if (txHash.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { uriHandler.openUri("https://stellar.expert/explorer/testnet/tx/$txHash") },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(23.dp)
                ) {
                    Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("View on Stellar Expert", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B192C))
        ) {
            Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ═════════════════════════════════════════════════════════════════
//  MODAL DIALOGS
// ═════════════════════════════════════════════════════════════════

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processGroupBillImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onSuccess: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrBlank()) {
                        onSuccess(rawValue)
                        break
                    }
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}

@Composable
private fun FindRecipientPanel(
    onCancel: () -> Unit,
    onVerified: (AuthRepository.UserData) -> Unit
) {
    val scope = rememberCoroutineScope()
    var phoneInput by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun verify() {
        val query = phoneInput.trim()
        if (query.isBlank() || isVerifying) return
        scope.launch {
            isVerifying = true
            errorMsg = null
            val user = AuthRepository.findRegisteredUser(query)
            isVerifying = false
            when {
                user == null -> errorMsg = "No registered PUNDAR account found for this number."
                user.id == AuthRepository.currentUserState.value?.id -> errorMsg = "You can't send to your own account."
                else -> onVerified(user)
            }
        }
    }

    run {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFE0E7FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Dialpad, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Find Recipient",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Enter the registered PUNDAR mobile number or Stellar address.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(18.dp))

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it; errorMsg = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mobile / Account Number") },
                    singleLine = true,
                    enabled = !isVerifying,
                    isError = errorMsg != null,
                    supportingText = errorMsg?.let { msg -> { Text(msg) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { verify() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFF0B192C),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !isVerifying,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) { Text("Cancel", fontWeight = FontWeight.SemiBold) }

                    Button(
                        onClick = { verify() },
                        enabled = phoneInput.isNotBlank() && !isVerifying,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B192C))
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text("Verify", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenerateGroupQrModalDialog(
    activeBillId: String,
    expenseName: String,
    initialCategory: String = "Utilities",
    totalBillAmount: Double = 0.0,
    initialJoiningCount: Int = 1,
    initialAmount: Double = 0.0,
    onSyncBillConfig: (title: String, category: String, totalAmountPhp: Double, joiningCount: Int) -> Unit = { _, _, _, _ -> },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by AuthRepository.currentUserState
    var titleInput by remember { mutableStateOf(expenseName.ifBlank { "Group Bill Split" }) }
    var categoryInput by remember { mutableStateOf(initialCategory.ifBlank { "Utilities" }) }
    var peopleCountInput by remember { mutableStateOf(initialJoiningCount.coerceAtLeast(1).toString()) }
    var notesInput by remember { mutableStateOf("") }
    var amountInput by remember {
        mutableStateOf(if (initialAmount > 0.0) String.format(Locale.US, "%.2f", initialAmount) else "")
    }
    var isQrGenerated by remember { mutableStateOf(false) }

    val settledAmount = amountInput.toDoubleOrNull() ?: 0.0
    // Quantity of people joining excludes the creator; total splitting the bill includes the creator (+1)
    val peopleJoiningCount = (peopleCountInput.toIntOrNull() ?: 1).coerceAtLeast(1)
    val totalSplitCount = peopleJoiningCount + 1
    val effectiveTitle = remember(titleInput, categoryInput, notesInput) {
        val base = titleInput.trim().ifBlank { expenseName.ifBlank { "Group Split" } }
        val withCat = "[$categoryInput] $base"
        if (notesInput.trim().isNotEmpty()) "$withCat - ${notesInput.trim()}" else withCat
    }

    val qrBitmap = remember(effectiveTitle, currentUser?.id, settledAmount, totalSplitCount, isQrGenerated, activeBillId) {
        if (!isQrGenerated || settledAmount <= 0.0) null
        else {
            try {
                val uid = currentUser?.id ?: currentUser?.phone ?: "group_host"
                val payload = QrPayload(
                    type = QrPayload.TYPE_GROUP_BILL_JOIN,
                    version = 1,
                    userId = uid,
                    displayName = currentUser?.name ?: titleInput.ifBlank { expenseName },
                    username = currentUser?.phone,
                    walletId = currentUser?.stellarPublicKey,
                    amount = settledAmount,
                    totalBillAmount = if (totalBillAmount > 0.0) totalBillAmount else settledAmount * totalSplitCount,
                    participantCount = totalSplitCount,
                    billRef = effectiveTitle,
                    transactionId = activeBillId,
                    timestamp = System.currentTimeMillis(),
                    securityToken = UUID.randomUUID().toString()
                )
                QrCodeGenerator.generateBitmap(payload, 512)
            } catch (_: Exception) {
                null
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f).padding(12.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE0E7FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.QrCode, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (isQrGenerated) "Invite to Join Pay QR" else "Configure Invite QR Info",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isQrGenerated)
                        "Have participants scan this QR code to join and pay their locked share."
                    else
                        "Fill in the bill details, participants, and locked amount before generating the invite QR.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(14.dp))

                if (!isQrGenerated) {
                    // 1. Bill / Expense Title
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Bill / Expense Title *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF0B192C),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    Spacer(Modifier.height(10.dp))

                    // 2. Category Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Food & Dining", "Utilities", "Rent", "Travel", "Other").forEach { cat ->
                            val selected = categoryInput == cat
                            OutlinedButton(
                                onClick = { categoryInput = cat },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) Color(0xFF0B192C) else Color(0xFFE2E8F0)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) Color(0xFFE0E7FF) else Color.White
                                )
                            ) {
                                Text(cat, fontSize = 11.sp, color = Color(0xFF0F172A), fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // 3. Quantity of People Joining (Excluding Creator)
                    OutlinedTextField(
                        value = peopleCountInput,
                        onValueChange = { raw ->
                            val cleaned = raw.filter { ch -> ch.isDigit() }.take(2)
                            peopleCountInput = cleaned
                            val joining = cleaned.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            if (totalBillAmount > 0.0) {
                                amountInput = String.format(Locale.US, "%.2f", totalBillAmount / (joining + 1))
                            }
                        },
                        label = { Text("Quantity of People Joining (Excluding You) *") },
                        supportingText = {
                            Text(
                                "Excludes you (the creator). You are included in splitting the bill (+1) → Total splitting: $totalSplitCount people ($peopleJoiningCount joining + You).",
                                fontSize = 11.sp,
                                color = Color(0xFF1E3A8A)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF0B192C),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    // Quick Select Chips for Quantity of People Joining (Plain Numbers, No "p")
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("1", "2", "3", "4", "5", "6").forEach { qty ->
                            val isSelected = peopleCountInput == qty
                            OutlinedButton(
                                onClick = {
                                    peopleCountInput = qty
                                    val joining = qty.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                    if (totalBillAmount > 0.0) {
                                        amountInput = String.format(Locale.US, "%.2f", totalBillAmount / (joining + 1))
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF0B192C) else Color(0xFFE2E8F0)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) Color(0xFFE0E7FF) else Color.White
                                )
                            ) {
                                Text(
                                    text = qty,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // 4. Locked Amount Needed from User
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Locked Share Amount per User (₱) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF0B192C),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    // Quick Amount Chips
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val chips = if (initialAmount > 0.0) {
                            listOf(String.format(Locale.US, "%.0f", initialAmount), "100", "250", "500")
                        } else {
                            listOf("50", "100", "250", "500")
                        }
                        chips.distinct().take(4).forEach { chipVal ->
                            OutlinedButton(
                                onClick = { amountInput = chipVal },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (amountInput == chipVal) Color(0xFF0B192C) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Text("₱$chipVal", fontSize = 11.sp, color = Color(0xFF0F172A))
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // 5. Optional Notes / Instructions
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Notes / Instructions (Optional)") },
                        placeholder = { Text("e.g. Settle before 9 PM") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF0B192C),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                val currentPhone = currentUser?.phone?.takeIf { it.isNotBlank() } ?: currentUser?.id.orEmpty()
                                val currentName = currentUser?.name?.takeIf { it.isNotBlank() } ?: "You"
                                val currentInitials = AuthRepository.getCurrentUserInitials().ifBlank { "YO" }
                                val computedTotal = settledAmount * totalSplitCount
                                onSyncBillConfig(
                                    titleInput.trim().ifBlank { "Group Split" },
                                    categoryInput,
                                    computedTotal,
                                    peopleJoiningCount
                                )
                                val membersList = buildList {
                                    add(BillMember(name = "You ($currentName)", username = currentPhone, initials = currentInitials, amount = settledAmount))
                                    for (i in 1..peopleJoiningCount) {
                                        add(BillMember(name = "Participant $i", username = "", initials = "P$i", amount = settledAmount))
                                    }
                                }
                                AppState.bills.removeAll { it.id == activeBillId }
                                val createdBill = GroupBill(
                                    id = activeBillId,
                                    name = effectiveTitle,
                                    totalAmount = computedTotal,
                                    memberCount = totalSplitCount,
                                    status = BillStatus.PENDING,
                                    date = "Today",
                                    yourShare = settledAmount,
                                    members = membersList
                                )
                                AppState.addBill(createdBill, recordActivity = false)
                                AppState.homeActivities.removeAll { it.referenceId == activeBillId }
                                val activity = HomeActivity(
                                    icon = "payment",
                                    title = effectiveTitle,
                                    subtitle = "Pending • $peopleJoiningCount joining + You ($totalSplitCount splitting)",
                                    amount = "₱ ${String.format(Locale.US, "%,.2f", settledAmount)}",
                                    isPositive = false,
                                    module = "Pay",
                                    referenceId = activeBillId
                                )
                                AppState.addHomeActivity(activity)
                                scope.launch {
                                    val uid = AuthRepository.getCurrentUserId() ?: currentPhone
                                    if (uid.isNotBlank()) {
                                        HomeRepository.createActivity(uid, activity)
                                    }
                                    AppState.requestHomeRefresh()
                                }
                                isQrGenerated = true
                            },
                            enabled = settledAmount > 0.0 && titleInput.trim().isNotEmpty(),
                            modifier = Modifier.weight(1.3f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B192C))
                        ) {
                            Text("Generate QR", fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Group QR Code",
                        modifier = Modifier.size(190.dp).clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Text(
                            "Locked Share: ₱${String.format(Locale.US, "%,.2f", settledAmount)} • $peopleJoiningCount joining + You ($totalSplitCount splitting)\n${titleInput.trim().ifBlank { expenseName }} · $categoryInput",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isQrGenerated = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Edit Info", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                val bmp = qrBitmap ?: return@OutlinedButton
                                QrCodeGenerator.saveBitmapToGallery(
                                    context,
                                    bmp,
                                    "PUNDAR_GROUP_${System.currentTimeMillis()}.png"
                                ).onSuccess {
                                    Toast.makeText(context, "Saved to Pictures/PUNDAR", Toast.LENGTH_SHORT).show()
                                }.onFailure { e ->
                                    Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                val bmp = qrBitmap ?: return@OutlinedButton
                                QrCodeGenerator.shareQrBitmap(
                                    context = context,
                                    bitmap = bmp,
                                    shareText = "Join my pay for '$effectiveTitle' on PUNDAR. Locked Share: ₱${String.format(Locale.US, "%,.2f", settledAmount)}.",
                                    chooserTitle = "Share Group QR"
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share", fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B192C))
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
