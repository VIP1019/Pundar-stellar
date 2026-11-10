package com.example.pundarapp.ui.screens.pay

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.pundarapp.data.qr.QrCodeGenerator
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.*
import com.example.pundarapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupBillScreen(navController: NavController) {
    // ── Wizard Step State (1..5) ──────────────────────────────────
    var currentStep by remember { mutableIntStateOf(1) }

    // ── Step 1 & 2 Inputs ──────────────────────────────────────────
    var merchantName by remember { mutableStateOf("Balangay Cafe & Co.") }
    var merchantRef by remember { mutableStateOf("BAL-9021-PH") }
    var merchantAddress by remember { mutableStateOf("balangay*pundar.site") }
    var isQrPhMerchant by remember { mutableStateOf(true) }

    var totalAmountInput by remember { mutableStateOf("850") }
    var expenseNameInput by remember { mutableStateOf("Balangay Cafe & Co.") }
    var isFiatMode by remember { mutableStateOf(true) } // PHP mode

    // ── Step 3 Split Mode ─────────────────────────────────────────
    var splitModeIndex by remember { mutableIntStateOf(0) } // 0 = Equal, 1 = Itemized

    // Selected group members (starts with "You" + sample friends)
    val selectedMembers = remember {
        mutableStateListOf(
            BillMember("You", "@currentuser", "YOU", 0.0, 0xFF0B192C),
            BillMember("Maria Santos", "@maria", "MP", 0.0, 0xFF10B981),
            BillMember("Alex Kumar", "@alexk", "AK", 0.0, 0xFF8B5CF6)
        )
    }

    val itemizedAmounts = remember { mutableStateMapOf<String, String>() }

    // Dialog state handlers
    var showScanQrDialog by remember { mutableStateOf(false) }
    var showEnterNumberDialog by remember { mutableStateOf(false) }
    var showGenerateGroupQrDialog by remember { mutableStateOf(false) }
    var showAddPeopleDialog by remember { mutableStateOf(false) }

    // Reference ID generated for payment
    var generatedRefNo by remember { mutableStateOf("001318387") }
    var completedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Calculation helpers
    val rawTotal = if (splitModeIndex == 0) {
        totalAmountInput.toDoubleOrNull() ?: 0.0
    } else {
        itemizedAmounts.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
    }

    val totalInPhp = if (isFiatMode) rawTotal else rawTotal * AppState.currentExchangeRate.doubleValue
    val totalInXlm = if (isFiatMode) {
        if (AppState.currentExchangeRate.doubleValue > 0) rawTotal / AppState.currentExchangeRate.doubleValue else 0.0
    } else rawTotal

    val sharePerMemberPhp = if (splitModeIndex == 0 && selectedMembers.isNotEmpty() && totalInPhp > 0)
        totalInPhp / selectedMembers.size else 0.0

    val yourSharePhp = if (splitModeIndex == 0) sharePerMemberPhp
    else {
        val rawShare = itemizedAmounts["You"]?.toDoubleOrNull() ?: 0.0
        if (isFiatMode) rawShare else rawShare * AppState.currentExchangeRate.doubleValue
    }

    Scaffold(
        topBar = {
            if (currentStep < 5) {
                Surface(
                    color = Color.White,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
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
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFF0F172A)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (currentStep == 1) "Identify Recipient" else "Send Confirm",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "PUNDAR",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF64748B),
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { navController.navigateUp() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF0F172A))
                                }
                                Spacer(Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0B192C)),
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
        containerColor = Color(0xFFF4F6F9)
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
                            onSelectMerchant = { name, ref, addr ->
                                merchantName = name
                                merchantRef = ref
                                merchantAddress = addr
                                expenseNameInput = name
                            },
                            onOpenScanQr = { showScanQrDialog = true },
                            onOpenEnterNumber = { showEnterNumberDialog = true },
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
                            onRemoveMember = { m -> if (m.name != "You") selectedMembers.remove(m) },
                            onNext = { currentStep = 3 }
                        )

                        3 -> Step3ReviewAndSplit(
                            splitModeIndex = splitModeIndex,
                            onSplitModeChange = { splitModeIndex = it },
                            selectedMembers = selectedMembers,
                            totalInPhp = totalInPhp,
                            sharePerMemberPhp = sharePerMemberPhp,
                            itemizedAmounts = itemizedAmounts,
                            onNext = { currentStep = 4 }
                        )

                        4 -> Step4ConfirmSend(
                            merchantName = merchantName,
                            merchantRef = merchantRef,
                            expenseName = expenseNameInput,
                            totalInPhp = totalInPhp,
                            totalInXlm = totalInXlm,
                            yourSharePhp = yourSharePhp,
                            members = selectedMembers,
                            onConfirm = {
                                generatedRefNo = String.format("%09d", Random().nextInt(1000000000))
                                completedTimestamp = System.currentTimeMillis()

                                val newBill = GroupBill(
                                    id = "bill_$generatedRefNo",
                                    name = expenseNameInput.ifBlank { merchantName },
                                    totalAmount = totalInXlm,
                                    memberCount = selectedMembers.size,
                                    status = BillStatus.PENDING,
                                    date = "Today",
                                    yourShare = yourSharePhp / AppState.currentExchangeRate.doubleValue,
                                    members = selectedMembers.toList()
                                )
                                AppState.addBill(newBill)

                                currentStep = 5
                            }
                        )

                        5 -> Step5InvoiceReceipt(
                            merchantName = merchantName.ifBlank { "Balangay Cafe & Co." },
                            merchantRef = merchantRef,
                            isQrPh = isQrPhMerchant,
                            amountPhp = totalInPhp,
                            members = selectedMembers,
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
    if (showScanQrDialog) {
        ScanQrModalDialog(
            onDismiss = { showScanQrDialog = false },
            onQrScanned = { scannedName, scannedRef ->
                merchantName = scannedName
                merchantRef = scannedRef
                expenseNameInput = scannedName
                showScanQrDialog = false
            }
        )
    }

    if (showEnterNumberDialog) {
        EnterNumberModalDialog(
            onDismiss = { showEnterNumberDialog = false },
            onConfirmNumber = { number, name ->
                merchantName = name.ifBlank { "Merchant ($number)" }
                merchantRef = "REF-$number"
                expenseNameInput = merchantName
                showEnterNumberDialog = false
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
    onSelectMerchant: (String, String, String) -> Unit,
    onOpenScanQr: () -> Unit,
    onOpenEnterNumber: () -> Unit,
    onNext: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Scan QR, 1 = Phone
    var manualSearchQuery by remember { mutableStateOf("") }

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
                        onClick = { activeTab = 0; onOpenScanQr() },
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
                        onClick = { activeTab = 1; onOpenEnterNumber() },
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

        // ── DARK BLUE QR SCANNER VIEWPORT BOX ──────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B192C)) // Dark Blue Palette
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
                                        .background(Color(0xFF38BDF8))
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "AUTO-DETECT ACTIVE",
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
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.FlashOn, contentDescription = "Flash", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Image, contentDescription = "Gallery", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Scanner Viewfinder Area with Corner Brackets
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF020617))
                            .clickable { onOpenScanQr() },
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing corner brackets & laser beam line
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 4.dp.toPx()
                            val bracketLen = 24.dp.toPx()
                            val pad = 30.dp.toPx()
                            val cornerColor = Color(0xFF38BDF8) // Electric Blue Corner Accent

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

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.QrCode2,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Align code inside frame",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFCBD5E1)
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
                            Icon(Icons.Filled.Person, contentDescription = null, tint = Color(0xFF0B192C), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Manual Merchant Search", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                        Text("Phone or Federated ID", fontSize = 11.sp, color = Color(0xFF64748B))
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = manualSearchQuery,
                        onValueChange = { manualSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search merchant or enter phone...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF64748B)) },
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

                    Spacer(Modifier.height(18.dp))

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
                                    onAmountChange((current + add).toInt().toString())
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
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B192C))
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
    totalInPhp: Double,
    sharePerMemberPhp: Double,
    itemizedAmounts: MutableMap<String, String>,
    onNext: () -> Unit
) {
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

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Bill Amount", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text(
                            "₱${String.format("%,.2f", totalInPhp)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Share Per Person", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text(
                            "₱${String.format("%,.2f", sharePerMemberPhp)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                    }
                }
            }
        }

        item {
            Text(
                "Individual Contributions",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        items(selectedMembers) { member ->
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
                        Text(
                            "₱${String.format("%,.2f", sharePerMemberPhp)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    } else {
                        OutlinedTextField(
                            value = itemizedAmounts[member.name] ?: "",
                            onValueChange = { itemizedAmounts[member.name] = it },
                            modifier = Modifier.width(110.dp),
                            placeholder = { Text("0.00") },
                            prefix = { Text("₱ ") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0B192C),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B192C))
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
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                    ConfirmDetailRow("Group Participants", "${members.size} Friends")
                    ConfirmDetailRow("Your Portion", "₱${String.format("%,.2f", yourSharePhp)}")
                    ConfirmDetailRow("Payment Rail", "Stellar Network / PUNDAR Pay")
                }
            }
        }

        Button(
            onClick = onConfirm,
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
    members: List<BillMember>,
    refNo: String,
    timestamp: Long,
    onClose: () -> Unit
) {
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
                .weight(1f),
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

            Text("₱${String.format("%,.2f", amountPhp)}", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))

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

            Spacer(Modifier.height(32.dp))

            Text("Ref No. $refNo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Text(fullDateStr, fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 2.dp))
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

@Composable
private fun ScanQrModalDialog(
    onDismiss: () -> Unit,
    onQrScanned: (String, String) -> Unit
) {
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
                Text("Scan Receiver QR Code", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("Point your camera at QR Ph or PUNDAR QR", fontSize = 12.sp, color = Color(0xFF64748B))

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier.size(200.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0B192C)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.QrCode2, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(72.dp))
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { onQrScanned("Balangay Cafe & Co.", "BAL-9021-PH") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B192C))
                ) {
                    Text("Select Balangay Cafe & Co.")
                }

                Spacer(Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        }
    }
}

@Composable
private fun EnterNumberModalDialog(
    onDismiss: () -> Unit,
    onConfirmNumber: (String, String) -> Unit
) {
    var phoneInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Enter Recipient Number / ID", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mobile / Account Number") },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Merchant Name (Optional)") },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirmNumber(phoneInput, nameInput) },
                        enabled = phoneInput.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B192C))
                    ) {
                        Text("Confirm")
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
    val qrBitmap = remember(expenseName) {
        try { QrCodeGenerator.generateBitmap("PUNDAR_GROUP_BILL:$expenseName", 400) } catch (e: Exception) { null }
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
                Text("Friends can scan to join '$expenseName'", fontSize = 12.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)

                Spacer(Modifier.height(16.dp))

                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Group QR Code",
                        modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp))
                    )
                }

                Spacer(Modifier.height(16.dp))

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
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }

    val sampleSuggestions = listOf(
        BillMember("John Dela Cruz", "@johnd", "JD", 0.0, 0xFFEC4899),
        BillMember("Sarah Tan", "@saraht", "ST", 0.0, 0xFFF59E0B),
        BillMember("Carlos Reyes", "@carlosr", "CR", 0.0, 0xFF10B981)
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Add Recipient to Group", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Friend Name") },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mobile Number / Username") },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                Text("Quick Add Friends:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))

                sampleSuggestions.filter { existingMembers.none { m -> m.name == it.name } }.forEach { suggestion ->
                    Surface(
                        onClick = { onAddMember(suggestion) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(30.dp).clip(CircleShape).background(Color(suggestion.avatarColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(suggestion.initials, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(suggestion.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val initials = nameInput.take(2).uppercase().ifBlank { "FB" }
                            onAddMember(BillMember(nameInput, phoneInput, initials, 0.0, 0xFF0B192C))
                        },
                        enabled = nameInput.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B192C))
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}
