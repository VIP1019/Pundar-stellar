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
import com.example.pundarapp.data.remote.AuthRepository
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
    var isFiatMode by remember { mutableStateOf(true) } // PHP mode

    // ── Step 3 Split Mode ─────────────────────────────────────────
    var splitModeIndex by remember { mutableIntStateOf(0) } // 0 = Equal, 1 = Itemized

    // Selected group members (starts with "You" + sample friends)
    val selectedMembers = remember {
        mutableStateListOf(
            BillMember("You", "@currentuser", "YOU", 0.0, 0xFF0B192C)
        )
    }

    val itemizedAmounts = remember { mutableStateMapOf<String, String>() }

    // Dialog state handlers
    var showGenerateGroupQrDialog by remember { mutableStateOf(false) }
    var showAddPeopleDialog by remember { mutableStateOf(false) }

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

    val exchangeRate = AppState.currentExchangeRate.doubleValue.takeIf { it > 0.0 } ?: 56.50
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

                        val newBill = GroupBill(
                            id = "bill_$generatedRefNo",
                            name = expenseNameInput.ifBlank { merchantName },
                            totalAmount = totalInPhp,
                            memberCount = computedMembers.size,
                            status = BillStatus.PENDING,
                            date = "Today",
                            yourShare = yourSharePhp,
                            members = computedMembers
                        )
                        AppState.addBill(newBill)

                        val activity = HomeActivity(
                            icon = "send",
                            title = "Paid $merchantName",
                            subtitle = "Split bill · ${expenseNameInput.ifBlank { merchantName }}",
                            amount = "-₱${String.format(Locale.US, "%,.2f", yourSharePhp)}",
                            isPositive = false,
                            module = "Pay"
                        )
                        AppState.homeActivities.add(0, activity)
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
                            onSelectMerchant = { name, ref, addr, scannedAmount ->
                                merchantName = name
                                merchantRef = ref
                                merchantAddress = addr
                                expenseNameInput = name
                                recipientVerified = true
                                if (scannedAmount != null && scannedAmount > 0.0) {
                                    totalAmountInput = if (scannedAmount % 1.0 == 0.0) {
                                        scannedAmount.toLong().toString()
                                    } else {
                                        String.format(Locale.US, "%.2f", scannedAmount)
                                    }
                                }
                            },
                            onRecipientVerified = { user ->
                                merchantName = user.name
                                merchantRef = user.phone
                                merchantAddress = user.stellarPublicKey?.takeIf { it.isNotBlank() } ?: "${user.phone}*pundar.site"
                                expenseNameInput = user.name
                                recipientVerified = true
                                currentStep = 2
                            },
                            onNext = { currentStep = 2 }
                        )

                        2 -> Step2AmountAndBillSplit(
                            merchantName = merchantName,
                            merchantRef = merchantRef,
                            merchantAddress = merchantAddress,
                            totalAmountInput = totalAmountInput,
                            onAmountChange = { totalAmountInput = it },
                            totalInPhp = totalInPhp,
                            totalInXlm = totalInXlm,
                            selectedMembers = selectedMembers,
                            onOpenGenerateQr = { showGenerateGroupQrDialog = true },
                            onOpenAddPeople = { showAddPeopleDialog = true },
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
        GenerateGroupQrModalDialog(
            expenseName = expenseNameInput.ifBlank { "Group Expense" },
            onDismiss = { showGenerateGroupQrDialog = false }
        )
    }

    if (showAddPeopleDialog) {
        AddPeopleModalDialog(
            existingMembers = selectedMembers,
            onDismiss = { showAddPeopleDialog = false },
            onAddMember = { member ->
                if (selectedMembers.none { it.name == member.name }) {
                    selectedMembers.add(member)
                }
                showAddPeopleDialog = false
            }
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
    onSelectMerchant: (String, String, String, Double?) -> Unit,
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

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    fun handleScannedRaw(raw: String) {
        val decoded = QrPayload.decode(raw)
        scope.launch {
            val candidates = if (decoded != null) {
                listOfNotNull(decoded.walletId, decoded.username, decoded.userId)
            } else {
                listOf(raw.trim())
            }.filter { it.isNotBlank() }

            var user: AuthRepository.UserData? = null
            for (candidate in candidates) {
                user = AuthRepository.findRegisteredUser(candidate)
                if (user != null) break
            }

            if (user == null) {
                Toast.makeText(context, "This QR is not linked to a registered PUNDAR account.", Toast.LENGTH_LONG).show()
                kotlinx.coroutines.delay(1500)
                scanLocked.set(false)
            } else if (user.id == AuthRepository.currentUserState.value?.id) {
                Toast.makeText(context, "You can't send to your own account.", Toast.LENGTH_LONG).show()
                kotlinx.coroutines.delay(1500)
                scanLocked.set(false)
            } else {
                val ref = decoded?.billRef?.takeIf { it.isNotBlank() }
                    ?: decoded?.transactionId?.takeIf { it.isNotBlank() }
                    ?: user.phone
                val addr = user.stellarPublicKey?.takeIf { it.isNotBlank() } ?: "${user.phone}*pundar.site"
                onSelectMerchant(user.name, ref, addr, decoded?.amount)
                Toast.makeText(context, "Recipient verified: ${user.name}", Toast.LENGTH_SHORT).show()
                onNext()
            }
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
                    onSelectMerchant(user.name, user.phone, addr, null)
                    onNext()
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val image = InputImage.fromFilePath(context, uri)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val raw = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                    if (raw != null) {
                        handleScannedRaw(raw)
                    } else {
                        Toast.makeText(context, "No QR code found in image.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to scan image.", Toast.LENGTH_SHORT).show()
                }
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open selected image.", Toast.LENGTH_SHORT).show()
        }
    }

    // Reticle laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserLine"
    )

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
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B192C))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar inside Scanner Box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (hasCameraPermission) Color(0xFF10B981) else Color(0xFF38BDF8))
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (hasCameraPermission) "LIVE CAMERA ACTIVE" else "AUTO-DETECT READY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (torchEnabled) Color(0xFFF59E0B) else Color(0xFF1E293B))
                                    .clickable {
                                        val next = !torchEnabled
                                        torchEnabled = next
                                        cameraRef?.cameraControl?.enableTorch(next)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (torchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                    contentDescription = "Flash",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                                    .clickable {
                                        galleryLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Image, contentDescription = "Gallery", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Live Camera Viewfinder Area (square) with Corner Brackets & Laser Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF020617)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasCameraPermission) {
                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx).apply {
                                        scaleType = PreviewView.ScaleType.FILL_CENTER
                                    }
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }
                                        val resolutionSelector = ResolutionSelector.Builder()
                                            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
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
                                                            handleScannedRaw(barcode)
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
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    Icons.Filled.PhotoCamera,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Camera permission required for live QR scan",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFCBD5E1),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text("Allow Camera", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Drawing corner brackets & laser beam line over the live camera preview
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 4.dp.toPx()
                            val bracketLen = 24.dp.toPx()
                            val pad = 26.dp.toPx()
                            val cornerColor = Color(0xFF38BDF8)

                            // Top-Left
                            drawLine(cornerColor, Offset(pad, pad), Offset(pad + bracketLen, pad), stroke)
                            drawLine(cornerColor, Offset(pad, pad), Offset(pad, pad + bracketLen), stroke)

                            // Top-Right
                            drawLine(cornerColor, Offset(size.width - pad, pad), Offset(size.width - pad - bracketLen, pad), stroke)
                            drawLine(cornerColor, Offset(size.width - pad, pad), Offset(size.width - pad, pad + bracketLen), stroke)

                            // Bottom-Left
                            drawLine(cornerColor, Offset(pad, size.height - pad), Offset(pad + bracketLen, size.height - pad), stroke)
                            drawLine(cornerColor, Offset(pad, size.height - pad), Offset(pad, size.height - pad - bracketLen), stroke)

                            // Bottom-Right
                            drawLine(cornerColor, Offset(size.width - pad, size.height - pad), Offset(size.width - pad - bracketLen, size.height - pad), stroke)
                            drawLine(cornerColor, Offset(size.width - pad, size.height - pad), Offset(size.width - pad, size.height - pad - bracketLen), stroke)

                            // Horizontal laser beam
                            val beamY = pad + (size.height - 2 * pad) * laserY
                            drawLine(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color(0xFF38BDF8), Color.Transparent)
                                ),
                                start = Offset(pad, beamY),
                                end = Offset(size.width - pad, beamY),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        "Point at any QR Ph, PUNDAR Tag, or Stellar SEP-0007 code.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("✓ QR Ph Ready", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF60A5FA))
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("⚡ Near-Instant", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF93C5FD))
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
    totalAmountInput: String,
    onAmountChange: (String) -> Unit,
    totalInPhp: Double,
    totalInXlm: Double,
    selectedMembers: List<BillMember>,
    onOpenGenerateQr: () -> Unit,
    onOpenAddPeople: () -> Unit,
    onRemoveMember: (BillMember) -> Unit,
    onNext: () -> Unit
) {
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Invite Recipients to Split",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenGenerateQr,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF0B192C))
                        ) {
                            Icon(Icons.Filled.QrCode, contentDescription = null, tint = Color(0xFF0B192C), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Generate QR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B192C))
                        }

                        Button(
                            onClick = onOpenAddPeople,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B192C))
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Friends", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Current Members List
        item {
            Text(
                "Group Members (${selectedMembers.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
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
    expenseName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by AuthRepository.currentUserState
    val qrBitmap = remember(expenseName, currentUser?.id) {
        try {
            val uid = currentUser?.id ?: "group_host"
            val payload = QrCodeGenerator.createReceiveMoneyPayload(
                userId = uid,
                displayName = currentUser?.name ?: expenseName,
                username = currentUser?.phone,
                walletId = currentUser?.stellarPublicKey
            ).copy(billRef = expenseName)
            QrCodeGenerator.generateBitmap(payload, 512)
        } catch (_: Exception) {
            null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Group Join QR Code", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("Friends can scan to pay or join '$expenseName'", fontSize = 12.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)

                Spacer(Modifier.height(16.dp))

                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Group QR Code",
                        modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp))
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save")
                    }
                    OutlinedButton(
                        onClick = {
                            val bmp = qrBitmap ?: return@OutlinedButton
                            QrCodeGenerator.shareQrBitmap(
                                context = context,
                                bitmap = bmp,
                                shareText = "Scan to join/pay '$expenseName' on PUNDAR.",
                                chooserTitle = "Share Group QR"
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share")
                    }
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B192C))
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun AddPeopleModalDialog(
    existingMembers: List<BillMember>,
    onDismiss: () -> Unit,
    onAddMember: (BillMember) -> Unit
) {
    val scope = rememberCoroutineScope()
    var phoneInput by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val avatarColors = listOf(0xFFEC4899, 0xFFF59E0B, 0xFF10B981, 0xFF8B5CF6, 0xFF0EA5E9)

    fun verifyAndAdd() {
        val query = phoneInput.trim()
        if (query.isBlank() || isVerifying) return
        scope.launch {
            isVerifying = true
            errorMsg = null
            val user = AuthRepository.findRegisteredUser(query)
            isVerifying = false
            when {
                user == null -> errorMsg = "No registered PUNDAR account found. Only verified accounts can join a split."
                user.id == AuthRepository.currentUserState.value?.id -> errorMsg = "You're already in this split."
                existingMembers.any { it.name == user.name } -> errorMsg = "${user.name} is already in this split."
                else -> {
                    val parts = user.name.trim().split(" ").filter { it.isNotBlank() }
                    val initials = when {
                        parts.size >= 2 -> (parts.first().take(1) + parts.last().take(1)).uppercase()
                        parts.size == 1 -> parts[0].take(2).uppercase()
                        else -> "PU"
                    }
                    onAddMember(
                        BillMember(
                            user.name,
                            "@${user.phone}",
                            initials,
                            0.0,
                            avatarColors[existingMembers.size % avatarColors.size]
                        )
                    )
                }
            }
        }
    }

    Dialog(onDismissRequest = { if (!isVerifying) onDismiss() }) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFE0E7FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Invite to Split", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Spacer(Modifier.height(4.dp))
                Text(
                    "Only verified PUNDAR accounts can be added. Enter their registered mobile number.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(18.dp))

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it; errorMsg = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Registered Mobile Number") },
                    singleLine = true,
                    enabled = !isVerifying,
                    isError = errorMsg != null,
                    supportingText = errorMsg?.let { msg -> { Text(msg) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { verifyAndAdd() }),
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
                        onClick = onDismiss,
                        enabled = !isVerifying,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) { Text("Cancel", fontWeight = FontWeight.SemiBold) }

                    Button(
                        onClick = { verifyAndAdd() },
                        enabled = phoneInput.isNotBlank() && !isVerifying,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B192C))
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text("Verify & Add", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
