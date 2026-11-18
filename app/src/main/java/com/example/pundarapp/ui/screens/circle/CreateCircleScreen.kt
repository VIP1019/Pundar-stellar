package com.example.pundarapp.ui.screens.circle

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.pundarapp.data.qr.QrCodeGenerator
import com.example.pundarapp.data.qr.QrPayload
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.remote.CircleRepository
import com.example.pundarapp.ui.data.*
import com.example.pundarapp.ui.navigation.Routes
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val StitchNavy = Color(0xFF0B2447)
private val StitchDarkNavy = Color(0xFF0B1B3D)
private val StitchBlue = Color(0xFF1D4ED8)
private val StitchBlueLight = Color(0xFFEFF6FF)
private val StitchSurfaceBg = Color(0xFFF8FAFC)
private val StitchBorder = Color(0xFFE2E8F0)
private val StitchTextPrimary = Color(0xFF0F172A)
private val StitchTextSecondary = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCircleScreen(navController: NavController) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val currentUser by AuthRepository.currentUserState
    val myPhone = currentUser?.phone?.ifBlank { AuthRepository.getCurrentUserPhone() }
        ?: AuthRepository.getCurrentUserPhone()
    val myName = currentUser?.name?.ifBlank { AuthRepository.getCurrentUserName() }
        ?: AuthRepository.getCurrentUserName().ifBlank { "Organizer" }
    val myInitials = AuthRepository.getCurrentUserInitials().ifBlank { myName.take(2).uppercase() }
    val myPubKey = (currentUser?.stellarPublicKey?.ifBlank { AuthRepository.getCurrentUserStellarPublicKey().orEmpty() }
        ?: AuthRepository.getCurrentUserStellarPublicKey()).orEmpty()
    val myScore = AppState.pundarScore.intValue
    val isCurrentUserBanned = AppState.isCircleBanned.value

    // Wizard sub-step: 0 = Step 1 of 5 (Parameters), 1 = Step 1B (Invite Members / Member Allocation)
    var currentSubStep by remember { mutableIntStateOf(0) }

    var circleName by remember { mutableStateOf("") }
    var contributionAmount by remember { mutableStateOf("1000") }
    var selectedFrequency by remember { mutableStateOf("Bi-weekly") } // "Bi-weekly" or "Monthly"
    var selectedCurrency by remember { mutableStateOf("PHP") }
    var payoutOrderType by remember { mutableStateOf("Custom Order") }
    var targetCapacity by remember { mutableIntStateOf(2) }

    var showContractClausesDialog by remember { mutableStateOf(false) }
    var showConfirmContractSubmitDialog by remember { mutableStateOf(false) }
    var agreedToAll12Clauses by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Direct Stellar / Phone invite state in Step 1B
    var directIdentifierInput by remember { mutableStateOf("") }
    var isLookingUpDirect by remember { mutableStateOf(false) }
    var directLookupError by remember { mutableStateOf<String?>(null) }

    // Registered users loaded from Firestore + Frequent PUNDAR contacts
    var frequentContacts by remember { mutableStateOf<List<AuthRepository.UserData>>(emptyList()) }

    val selectedMembers = remember {
        mutableStateListOf(
            CircleMember(
                userId = myPhone,
                name = myName,
                initials = myInitials,
                sharePercent = 100,
                amount = 0.0,
                status = ContributionStatus.PENDING,
                isYou = true,
                avatarColor = 0xFF0B2447,
                stellarPublicKey = myPubKey,
                payoutRound = 1,
                acceptanceStatus = "ACCEPTED",
                acceptedTermsVersion = 1,
                pundarScore = myScore,
                isBanned = isCurrentUserBanned
            )
        )
    }

    LaunchedEffect(myName, myPhone, myInitials, myPubKey, myScore, isCurrentUserBanned) {
        if (selectedMembers.isNotEmpty() && selectedMembers[0].isYou) {
            selectedMembers[0] = selectedMembers[0].copy(
                userId = myPhone,
                name = myName,
                initials = myInitials,
                stellarPublicKey = myPubKey,
                pundarScore = myScore,
                isBanned = isCurrentUserBanned
            )
        }
    }

    fun computeRoundDate(roundNumber: Int, frequency: String): String {
        val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
        val today = LocalDate.now()
        val targetDate = if (frequency == "Bi-weekly") {
            today.plusWeeks((roundNumber * 2).toLong())
        } else {
            today.plusMonths(roundNumber.toLong())
        }
        return targetDate.format(formatter)
    }

    fun reindexPayoutOrder() {
        val count = selectedMembers.size.coerceAtLeast(1)
        val share = 100 / count
        for (i in selectedMembers.indices) {
            val roundNum = i + 1
            selectedMembers[i] = selectedMembers[i].copy(
                payoutRound = roundNum,
                sharePercent = share,
                payoutDate = computeRoundDate(roundNum, selectedFrequency)
            )
        }
        if (selectedMembers.size > targetCapacity) {
            targetCapacity = selectedMembers.size
        }
    }

    LaunchedEffect(selectedFrequency) {
        reindexPayoutOrder()
    }

    LaunchedEffect(Unit) {
        val dbUsers = AuthRepository.searchUsers("")
        frequentContacts = dbUsers.filter { it.phone.isNotBlank() && it.phone != myPhone }
    }

    val fixedContribution = contributionAmount.toDoubleOrNull() ?: 0.0
    val effectiveMemberCount = selectedMembers.size.coerceAtLeast(1)
    val displaySlotCount = maxOf(targetCapacity, selectedMembers.size).coerceAtLeast(2)
    val potPerRound = fixedContribution * maxOf(selectedMembers.size, 2)
    val totalCirclePool = potPerRound * maxOf(selectedMembers.size, 2)
    val scheduleText = if (selectedFrequency == "Bi-weekly") "Bi-weekly (15th & 30th)" else "Monthly (End of month)"
    val durationLabel = if (selectedFrequency == "Bi-weekly") {
        val months = (maxOf(selectedMembers.size, 2) + 1) / 2
        "$months Months (${maxOf(selectedMembers.size, 2)} rounds)"
    } else {
        "${maxOf(selectedMembers.size, 2)} Months (${maxOf(selectedMembers.size, 2)} rounds)"
    }

    // Strict Minimum Balance Calculation
    val requiredContributionXlm = if (selectedCurrency == "PHP") {
        AppState.phpToXlm(fixedContribution)
    } else {
        fixedContribution
    }
    val minRequiredBalanceXlm = requiredContributionXlm + CircleContractRules.NEW_USER_MIN_RESERVE_XLM
    val minRequiredBalancePhp = AppState.xlmToPhp(minRequiredBalanceXlm)
    val hasMinimumBalance = AppState.walletBalance.value >= minRequiredBalanceXlm

    val inviteIdPreview = remember(circleName) {
        "bb-${UUID.nameUUIDFromBytes(circleName.toByteArray()).toString().take(4)}"
    }
    val deepLinkUrl = "pundar.app/circle/inv/$inviteIdPreview"

    val qrBitmap: Bitmap? = remember(circleName, fixedContribution, selectedFrequency, myPhone) {
        try {
            val payload = QrPayload(
                type = QrPayload.TYPE_STELLAR_ADDRESS,
                version = 1,
                userId = myPhone.ifBlank { "circle_organizer" },
                displayName = myName,
                username = myPhone,
                walletId = myPubKey,
                amount = fixedContribution,
                billRef = "CIRCLE:$circleName|$selectedFrequency",
                transactionId = "CIR-${inviteIdPreview.uppercase()}",
                timestamp = System.currentTimeMillis(),
                securityToken = UUID.randomUUID().toString()
            )
            QrCodeGenerator.generateBitmap(payload, 420)
        } catch (_: Exception) {
            null
        }
    }

    fun tryAddUserToCircle(user: AuthRepository.UserData) {
        directLookupError = null
        validationError = null
        if (user.isCircleBanned) {
            val reason = user.circleBanReason.ifBlank { "previous failed Circle payment" }
            directLookupError = "Banned Member: ${user.name} cannot join any Circle ($reason)."
            Toast.makeText(context, directLookupError, Toast.LENGTH_LONG).show()
            return
        }
        if (user.walletBalance > 0.0 && user.walletBalance < minRequiredBalanceXlm) {
            directLookupError = "Strict Minimum Balance Not Met: ${user.name} has ${String.format("%,.2f", user.walletBalance)} XLM, but joining requires at least ${String.format("%,.2f", minRequiredBalanceXlm)} XLM (1 round + reserve)."
            Toast.makeText(context, directLookupError, Toast.LENGTH_LONG).show()
            return
        }
        if (selectedMembers.any { it.userId == user.phone || it.name.equals(user.name, ignoreCase = true) }) {
            directLookupError = "${user.name} is already allocated a slot in this Circle."
            return
        }
        val parts = user.name.trim().split(" ").filter { it.isNotBlank() }
        val initials = when {
            parts.size >= 2 -> (parts.first().take(1) + parts.last().take(1)).uppercase()
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "PU"
        }
        val nextRound = selectedMembers.size + 1
        selectedMembers.add(
            CircleMember(
                userId = user.phone.ifBlank { user.id },
                name = user.name,
                initials = initials,
                sharePercent = 0,
                amount = 0.0,
                status = ContributionStatus.PENDING,
                isYou = false,
                payoutDate = computeRoundDate(nextRound, selectedFrequency),
                avatarColor = 0xFF2563EB,
                stellarPublicKey = user.stellarPublicKey.orEmpty(),
                payoutRound = nextRound,
                acceptanceStatus = "PENDING",
                acceptedTermsVersion = 0,
                pundarScore = user.pundarScore,
                isBanned = user.isCircleBanned
            )
        )
        reindexPayoutOrder()
    }

    fun submitCircleProposal() {
        if (isCurrentUserBanned) {
            validationError = "You are currently banned from creating or joining Circles due to a failed payment."
            return
        }
        if (!hasMinimumBalance) {
            validationError = "Strict Minimum Balance Required: You need at least ${String.format("%,.2f", minRequiredBalanceXlm)} XLM (≈ ₱${String.format("%,.2f", minRequiredBalancePhp)}) in your wallet to create this Circle."
            return
        }
        isSubmitting = true
        validationError = null
        scope.launch {
            try {
                val proposalId = "circle_prop_${System.currentTimeMillis()}"
                val freqVal = if (selectedFrequency == "Bi-weekly") 2 else 1
                val freqUnit = if (selectedFrequency == "Bi-weekly") "Weeks" else "Months"
                val finalPayoutPerRound = fixedContribution * selectedMembers.size

                val configuredMembers = selectedMembers.mapIndexed { idx, m ->
                    val roundNum = idx + 1
                    m.copy(
                        payoutRound = roundNum,
                        payoutDate = computeRoundDate(roundNum, selectedFrequency),
                        amount = 0.0,
                        status = ContributionStatus.PENDING,
                        acceptanceStatus = if (m.isYou || m.userId == myPhone) "ACCEPTED" else "PENDING",
                        acceptedTermsVersion = if (m.isYou || m.userId == myPhone) 1 else 0
                    )
                }

                val createTxRecord = CircleTransactionRecord(
                    id = "tx_prop_${System.currentTimeMillis()}",
                    type = "PROPOSAL_CREATED",
                    title = "Circle Proposal Created (Terms v1 · 12 Clauses Locked)",
                    memberName = myName,
                    memberPhone = myPhone,
                    roundNumber = 1,
                    amount = finalPayoutPerRound,
                    currency = selectedCurrency,
                    txHash = "PROPOSAL_V1_PENDING_CONSENT",
                    timestamp = System.currentTimeMillis(),
                    status = "AWAITING_MEMBER_CONSENT"
                )

                val newCircle = Circle(
                    id = proposalId,
                    name = circleName.trim(),
                    targetAmount = finalPayoutPerRound,
                    savedAmount = 0.0,
                    targetDate = scheduleText,
                    memberCount = configuredMembers.size,
                    contributionPerMonth = fixedContribution,
                    members = configuredMembers,
                    isActive = false,
                    currency = selectedCurrency,
                    creatorId = myPhone,
                    creatorName = myName,
                    payoutFrequencyValue = freqVal,
                    payoutFrequencyUnit = freqUnit,
                    maxMembers = configuredMembers.size,
                    payoutOrderType = payoutOrderType,
                    lifecycleStatus = "AWAITING_ACCEPTANCE",
                    termsVersion = 1,
                    currentRound = 1,
                    sorobanCircleId = 0L,
                    activationTxHash = "",
                    paidRounds = emptyList(),
                    transactionHistory = listOf(createTxRecord)
                )

                val saveRes = CircleRepository.createCircle(newCircle, myPhone)
                if (saveRes.isFailure) {
                    throw saveRes.exceptionOrNull() ?: Exception("Failed to submit Circle proposal")
                }

                configuredMembers.filter { !it.isYou && it.userId != myPhone }.forEach { invitee ->
                    CircleRepository.sendInvitation(
                        circleId = proposalId,
                        inviteeUserId = invitee.userId.ifBlank { invitee.name },
                        inviteeName = invitee.name,
                        inviteeInitials = invitee.initials,
                        inviterName = myName,
                        inviterScore = myScore,
                        circleName = newCircle.name,
                        contributionAmount = fixedContribution,
                        memberCount = configuredMembers.size,
                        termsVersion = 1,
                        scheduleText = scheduleText,
                        currency = selectedCurrency
                    )
                }

                AppState.circles.removeAll { it.id == proposalId }
                AppState.circles.add(0, newCircle)
                AppState.addHomeActivity(
                    HomeActivity(
                        icon = "savings",
                        title = newCircle.name,
                        subtitle = "Circle Proposal Sent (Terms v1 · ${configuredMembers.size} members)",
                        amount = "₱${String.format(Locale.US, "%,.2f", finalPayoutPerRound)} / round",
                        isPositive = true,
                        module = "Circle"
                    )
                )

                showConfirmContractSubmitDialog = false
                Toast.makeText(
                    context,
                    "Circle proposal created! Proceeding to Step 2 of 5: Member Consent.",
                    Toast.LENGTH_LONG
                ).show()

                navController.navigate(Routes.circleDetail(proposalId)) {
                    popUpTo(Routes.CIRCLE) { inclusive = false }
                }
            } catch (e: Exception) {
                validationError = e.message ?: "Failed to submit Circle proposal"
            } finally {
                isSubmitting = false
            }
        }
    }

    // ── 12-CLAUSE TERMS & CONDITIONS MODAL (Modules 1 & 2 a–l) ───────
    if (showContractClausesDialog) {
        val clauses = CircleContractRules.getClauses(
            contributionFormatted = "₱${String.format(Locale.US, "%,.2f", fixedContribution)} / round",
            scheduleText = scheduleText,
            payoutOrderType = payoutOrderType,
            payoutFormatted = "₱${String.format(Locale.US, "%,.2f", fixedContribution * selectedMembers.size.coerceAtLeast(2))}"
        )
        Dialog(
            onDismissRequest = { showContractClausesDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 640.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Navy Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StitchDarkNavy)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E2E5C)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.VerifiedUser,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Smart Contract Terms",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        "12 Mandatory Soroban Clauses (a - l)",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showContractClausesDialog = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Scrollable Clauses List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(clauses.size) { i ->
                            val c = clauses[i]
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = StitchSurfaceBg,
                                border = BorderStroke(1.dp, StitchBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(StitchNavy),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                c.code.uppercase(),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                color = Color.White
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            c.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = StitchTextPrimary
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        c.description,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                        color = StitchTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Footer Button
                    Surface(
                        color = Color.White,
                        border = BorderStroke(1.dp, StitchBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            Button(
                                onClick = { showContractClausesDialog = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StitchNavy),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Close Contract Rules", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── CONFIRM & SIGN 12-CLAUSE PROPOSAL DIALOG ──────────────────────
    if (showConfirmContractSubmitDialog) {
        val clauses = CircleContractRules.getClauses(
            contributionFormatted = "₱${String.format(Locale.US, "%,.2f", fixedContribution)} / round",
            scheduleText = scheduleText,
            payoutOrderType = payoutOrderType,
            payoutFormatted = "₱${String.format(Locale.US, "%,.2f", fixedContribution * selectedMembers.size)}"
        )
        Dialog(
            onDismissRequest = { if (!isSubmitting) showConfirmContractSubmitDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 660.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Navy Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StitchDarkNavy)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E2E5C)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Gavel,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Confirm Circle Contract",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        "Terms v1 • ${selectedMembers.size} Verified Members",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { if (!isSubmitting) showConfirmContractSubmitDialog = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Review the 12 mandatory Circle rules (a - l) that every invited participant must explicitly accept before Soroban activation:",
                            fontSize = 12.sp,
                            color = StitchTextSecondary
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(clauses.size) { idx ->
                                val cl = clauses[idx]
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = StitchSurfaceBg,
                                    border = BorderStroke(1.dp, StitchBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(StitchNavy),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    cl.code.uppercase(),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 10.sp,
                                                    color = Color.White
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                cl.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = StitchTextPrimary
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            cl.description,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp,
                                            color = StitchTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StitchBlueLight,
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { agreedToAll12Clauses = !agreedToAll12Clauses }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = agreedToAll12Clauses,
                                    onCheckedChange = { agreedToAll12Clauses = it }
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "I agree to all 12 Circle Contract rules (a - l) and confirm my fixed rotating slot.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StitchTextPrimary
                                )
                            }
                        }

                        validationError?.let {
                            Text(
                                it,
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Aligned Bottom Action Row
                    Surface(
                        color = Color.White,
                        border = BorderStroke(1.dp, StitchBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { if (!isSubmitting) showConfirmContractSubmitDialog = false },
                                enabled = !isSubmitting,
                                modifier = Modifier
                                    .weight(0.36f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, StitchBorder)
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold, color = StitchTextPrimary)
                            }
                            Button(
                                onClick = { submitCircleProposal() },
                                enabled = agreedToAll12Clauses && !isSubmitting,
                                modifier = Modifier
                                    .weight(0.64f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StitchNavy),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    if (isSubmitting) "Submitting..." else "Issue & Invite (${selectedMembers.size})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (currentSubStep == 1) currentSubStep = 0
                                else navController.navigateUp()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = StitchTextPrimary
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (currentSubStep == 0) "Create Circle Flow" else "Invite Members",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = StitchTextPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (currentSubStep == 0) StitchBlue else Color(0xFF10B981))
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "STELLAR TESTNET",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchTextSecondary,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { showContractClausesDialog = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Filled.HelpOutline,
                                contentDescription = "View 12 Contract Rules",
                                tint = StitchTextSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(StitchNavy),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (currentSubStep == 0) Icons.Filled.Person else Icons.Filled.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = StitchSurfaceBg
    ) { padding ->
        if (currentSubStep == 0) {
            // ═════════════════════════════════════════════════════════════
            //  STEP 1 OF 5: PARAMETERS (Matches Stitch Screenshot 1)
            // ═════════════════════════════════════════════════════════════
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Step 1 of 5 Progress Header
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Step 1 of 5",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StitchTextSecondary
                            )
                            Text(
                                "PARAMETERS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StitchNavy,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (step in 1..5) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (step == 1) StitchNavy else Color(0xFFCBD5E1))
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Create Circle",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = StitchTextPrimary
                        )
                        Text(
                            "Set payout terms and sequence for your group paluwagan.",
                            fontSize = 13.sp,
                            color = StitchTextSecondary
                        )
                    }
                }

                // Standing & Strict Minimum Balance Verification Strip
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCurrentUserBanned) Color(0xFFFEF2F2)
                        else if (!hasMinimumBalance) Color(0xFFFFFBEB)
                        else Color(0xFFECFDF5),
                        border = BorderStroke(
                            1.dp,
                            if (isCurrentUserBanned) Color(0xFFFECACA)
                            else if (!hasMinimumBalance) Color(0xFFFDE68A)
                            else Color(0xFFA7F3D0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        isCurrentUserBanned -> "Circle Participation Banned (Failed Payment)"
                                        !hasMinimumBalance -> "Strict Minimum Balance Check: Need ${String.format(Locale.US, "%,.2f", minRequiredBalanceXlm)} XLM"
                                        else -> "Standing Verified • PUNDAR Score: $myScore pts"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = when {
                                        isCurrentUserBanned -> Color(0xFF991B1B)
                                        !hasMinimumBalance -> Color(0xFF92400E)
                                        else -> Color(0xFF065F46)
                                    }
                                )
                                Text(
                                    text = if (isCurrentUserBanned) {
                                        "Reason: ${AppState.circleBanReason.value.ifBlank { "Missed scheduled Circle contribution" }}. Rehabilitate on the Circle dashboard."
                                    } else {
                                        "Wallet: ${String.format(Locale.US, "%,.2f", AppState.walletBalance.value)} XLM • Min required to join/create: ${String.format(Locale.US, "%,.2f", minRequiredBalanceXlm)} XLM"
                                    },
                                    fontSize = 11.sp,
                                    color = StitchTextSecondary
                                )
                            }
                        }
                    }
                }

                // ── MAIN PARAMETERS CARD (Circle Name, Contribution, Frequency) ──
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, StitchBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Circle Name Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(StitchBlueLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Groups,
                                        contentDescription = null,
                                        tint = StitchNavy,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "CIRCLE NAME",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = StitchTextSecondary,
                                        letterSpacing = 0.8.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = circleName,
                                        onValueChange = { circleName = it },
                                        placeholder = { Text("Enter Circle Name (e.g. Monthly Paluwagan)", fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        trailingIcon = {
                                            Icon(
                                                Icons.Filled.Edit,
                                                contentDescription = null,
                                                tint = StitchTextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = StitchNavy,
                                            unfocusedBorderColor = StitchBorder,
                                            focusedTextColor = StitchTextPrimary,
                                            unfocusedTextColor = StitchTextPrimary
                                        )
                                    )
                                }
                            }

                            // Contribution Per Member Box
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = StitchSurfaceBg,
                                border = BorderStroke(1.dp, StitchBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "CONTRIBUTION PER MEMBER",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = StitchTextSecondary,
                                        letterSpacing = 0.8.sp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "₱",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = StitchNavy,
                                            modifier = Modifier.padding(bottom = 8.dp, end = 6.dp)
                                        )
                                        OutlinedTextField(
                                            value = contributionAmount,
                                            onValueChange = { raw ->
                                                val cleaned = raw.filter { it.isDigit() || it == '.' }
                                                if (cleaned.count { it == '.' } <= 1) {
                                                    contributionAmount = cleaned
                                                }
                                            },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            suffix = {
                                                Text(
                                                    "/ round",
                                                    fontSize = 13.sp,
                                                    color = StitchTextSecondary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color.White,
                                                unfocusedContainerColor = Color.White,
                                                focusedBorderColor = StitchNavy,
                                                unfocusedBorderColor = StitchBorder,
                                                focusedTextColor = StitchTextPrimary,
                                                unfocusedTextColor = StitchTextPrimary
                                            )
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Equal deposit required from each member each cycle (≈ ${String.format(Locale.US, "%,.2f", requiredContributionXlm)} XLM).",
                                        fontSize = 12.sp,
                                        color = StitchTextSecondary
                                    )
                                }
                            }

                            // Payout Frequency
                            Column {
                                Text(
                                    "PAYOUT FREQUENCY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StitchTextSecondary,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val isBiWeekly = selectedFrequency == "Bi-weekly"
                                    Surface(
                                        onClick = { selectedFrequency = "Bi-weekly" },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isBiWeekly) StitchNavy else Color.White,
                                        border = BorderStroke(1.dp, if (isBiWeekly) StitchNavy else StitchBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Bi-weekly",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = if (isBiWeekly) Color.White else StitchTextPrimary
                                                )
                                                Icon(
                                                    if (isBiWeekly) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                                    contentDescription = null,
                                                    tint = if (isBiWeekly) Color.White else StitchBorder,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "15th & 30th of month",
                                                fontSize = 11.sp,
                                                color = if (isBiWeekly) Color(0xFFCBD5E1) else StitchTextSecondary
                                            )
                                        }
                                    }

                                    val isMonthly = selectedFrequency == "Monthly"
                                    Surface(
                                        onClick = { selectedFrequency = "Monthly" },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isMonthly) StitchNavy else Color.White,
                                        border = BorderStroke(1.dp, if (isMonthly) StitchNavy else StitchBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Monthly",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = if (isMonthly) Color.White else StitchTextPrimary
                                                )
                                                Icon(
                                                    if (isMonthly) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                                    contentDescription = null,
                                                    tint = if (isMonthly) Color.White else StitchBorder,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "End of each month",
                                                fontSize = 11.sp,
                                                color = if (isMonthly) Color(0xFFCBD5E1) else StitchTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── POT PER ROUND & TOTAL CIRCLE POOL CARD ──────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, StitchBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "POT PER ROUND",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StitchTextSecondary,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "₱${String.format(Locale.US, "%,.2f", potPerRound)}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StitchNavy
                                )
                                Text(
                                    "₱${String.format(Locale.US, "%,.0f", fixedContribution)} x ${maxOf(selectedMembers.size, 2)} members",
                                    fontSize = 11.sp,
                                    color = StitchTextSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(44.dp)
                                    .background(StitchBorder)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "TOTAL CIRCLE POOL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StitchTextSecondary,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "₱${String.format(Locale.US, "%,.2f", totalCirclePool)}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StitchTextPrimary
                                )
                                Text(
                                    durationLabel,
                                    fontSize = 11.sp,
                                    color = StitchTextSecondary
                                )
                            }
                        }
                    }
                }

                // ── PAYOUT ROTATION SCHEDULE SECTION ────────────────────────
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Payout Rotation",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StitchTextPrimary
                                )
                                Text(
                                    "Schedule",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StitchTextPrimary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = StitchBlueLight
                                ) {
                                    Text(
                                        "${selectedMembers.size} Members",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchNavy,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        if (selectedMembers.size > 1) {
                                            val shuffled = selectedMembers.shuffled()
                                            selectedMembers.clear()
                                            selectedMembers.addAll(shuffled)
                                            reindexPayoutOrder()
                                        }
                                        payoutOrderType = "Random Payment Agreed by Participants"
                                        Toast.makeText(
                                            context,
                                            "Payout rotation randomized!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, StitchBorder),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Shuffle,
                                        contentDescription = null,
                                        tint = StitchNavy,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Randomize",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchNavy
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Tap arrows to customize order or randomize",
                                fontSize = 11.sp,
                                color = StitchTextSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, StitchBorder)
                            ) {
                                Text(
                                    if (payoutOrderType.startsWith("Random")) "Randomized" else "Custom Order",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchTextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Member Rotation List Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, StitchBorder)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            selectedMembers.forEachIndexed { idx, member ->
                                val roundNum = idx + 1
                                val roundDate = member.payoutDate.ifBlank { computeRoundDate(roundNum, selectedFrequency) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.DragIndicator,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (roundNum == 1) StitchBlueLight else StitchSurfaceBg
                                    ) {
                                        Text(
                                            "#$roundNum",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (roundNum == 1) StitchBlue else StitchTextSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(if (member.isYou) StitchNavy else StitchBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            member.initials,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (member.isYou) "You (${member.name})" else member.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = StitchTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Round $roundNum • $roundDate",
                                            fontSize = 11.sp,
                                            color = if (roundNum == 1) StitchBlue else StitchTextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    // Reorder arrows
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(
                                            onClick = {
                                                if (idx > 0) {
                                                    val prev = selectedMembers[idx - 1]
                                                    selectedMembers[idx - 1] = member
                                                    selectedMembers[idx] = prev
                                                    payoutOrderType = "Custom Order"
                                                    reindexPayoutOrder()
                                                }
                                            },
                                            enabled = idx > 0,
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.KeyboardArrowUp,
                                                contentDescription = "Move Up",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                if (idx < selectedMembers.size - 1) {
                                                    val next = selectedMembers[idx + 1]
                                                    selectedMembers[idx + 1] = member
                                                    selectedMembers[idx] = next
                                                    payoutOrderType = "Custom Order"
                                                    reindexPayoutOrder()
                                                }
                                            },
                                            enabled = idx < selectedMembers.size - 1,
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.KeyboardArrowDown,
                                                contentDescription = "Move Down",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "₱${String.format(Locale.US, "%,.0f", potPerRound)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = StitchNavy
                                    )
                                }
                                HorizontalDivider(color = StitchBorder)
                            }

                            // + Invite Member Button inside card
                            TextButton(
                                onClick = { currentSubStep = 1 },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.PersonAddAlt1,
                                    contentDescription = null,
                                    tint = StitchBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "+ Invite Member",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = StitchBlue
                                )
                            }
                        }
                    }
                }

                // ── CONTRACT-ENFORCED RULES CARD ────────────────────────────
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = StitchBlueLight,
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = StitchBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "CONTRACT-ENFORCED RULES",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StitchNavy,
                                    letterSpacing = 0.6.sp
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Contribution terms and fixed rotation are cryptographically locked upon activation. Automated disbursements and escrow powered by Stellar Soroban integrated with your PUNDAR Pay wallet.",
                                fontSize = 12.sp,
                                color = StitchTextSecondary
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { showContractClausesDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, StitchBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Gavel, contentDescription = null, tint = StitchBlue, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Review All 12 Circle Contract Clauses (a - l)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchBlue
                                )
                            }
                        }
                    }
                }

                // ── BOTTOM ACTION BUTTONS ───────────────────────────────────
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (circleName.isBlank()) {
                                    Toast.makeText(context, "Please enter a Circle name.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (fixedContribution <= 0.0) {
                                    Toast.makeText(context, "Contribution amount must be greater than zero.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                currentSubStep = 1
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StitchNavy)
                        ) {
                            Text(
                                "Continue to Invites",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Circle draft saved locally.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, StitchBorder),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                        ) {
                            Icon(
                                Icons.Filled.BookmarkBorder,
                                contentDescription = null,
                                tint = StitchTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Save Draft",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchTextPrimary
                            )
                        }
                    }
                }
            }
        } else {
            // ═════════════════════════════════════════════════════════════
            //  STEP 1B: INVITE MEMBERS / MEMBER ALLOCATION (Matches Screenshot 2)
            // ═════════════════════════════════════════════════════════════
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── STEP 1B SUMMARY HEADER CARD ─────────────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, StitchBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = StitchBlueLight
                                ) {
                                    Text(
                                        "STEP 1B • MEMBER ALLOCATION",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = StitchBlue,
                                        letterSpacing = 0.6.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = StitchSurfaceBg,
                                    border = BorderStroke(1.dp, StitchBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.Lock,
                                            contentDescription = null,
                                            tint = StitchBlue,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "Fixed Roster",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StitchTextSecondary
                                        )
                                    }
                                }
                            }

                            Text(
                                circleName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StitchTextPrimary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StitchSurfaceBg,
                                    border = BorderStroke(1.dp, StitchBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.Payments,
                                            contentDescription = null,
                                            tint = StitchBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "₱${String.format(Locale.US, "%,.2f", fixedContribution)} / round",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StitchTextPrimary
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StitchSurfaceBg,
                                    border = BorderStroke(1.dp, StitchBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.EventRepeat,
                                            contentDescription = null,
                                            tint = StitchBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "$selectedFrequency Cycle",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StitchTextPrimary
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StitchBlueLight,
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = StitchBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Invited members will receive an invite notification and review the immutable Soroban smart contract terms before opting in.",
                                        fontSize = 11.sp,
                                        color = StitchTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // ── CIRCLE CAPACITY & SLOTS CARD ────────────────────────────
                item {
                    val openSeats = (displaySlotCount - selectedMembers.size).coerceAtLeast(0)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, StitchBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "CIRCLE CAPACITY",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = StitchTextSecondary,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        "Allocated Slots: ${selectedMembers.size} of $displaySlotCount",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = StitchTextPrimary
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (targetCapacity > maxOf(2, selectedMembers.size)) {
                                                targetCapacity--
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "Decrease Capacity", tint = StitchTextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = StitchBlueLight
                                    ) {
                                        Text(
                                            "$openSeats Seats Open",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StitchBlue,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { if (targetCapacity < 10) targetCapacity++ },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Filled.AddCircleOutline, contentDescription = "Increase Capacity", tint = StitchBlue, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            // Segmented Slot Progress Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (i in 0 until displaySlotCount) {
                                    val filled = i < selectedMembers.size
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                when {
                                                    i == 0 -> StitchNavy
                                                    filled -> StitchBlue
                                                    else -> Color(0xFFE2E8F0)
                                                }
                                            )
                                    )
                                }
                            }

                            // 2-Column Grid of Slot Pills
                            val rows = (0 until displaySlotCount).chunked(2)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                rows.forEach { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        pair.forEach { slotIdx ->
                                            val slotNum = slotIdx + 1
                                            val member = selectedMembers.getOrNull(slotIdx)
                                            if (member != null) {
                                                val isOrg = member.isYou
                                                Surface(
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isOrg) StitchDarkNavy else StitchSurfaceBg,
                                                    border = BorderStroke(1.dp, if (isOrg) StitchDarkNavy else StitchBorder)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(22.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(if (isOrg) Color(0xFF1E293B) else StitchBlueLight),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                "$slotNum",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isOrg) Color.White else StitchBlue
                                                            )
                                                        }
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                            text = if (isOrg) "You (Organizer)" else member.name,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isOrg) Color.White else StitchTextPrimary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        if (isOrg) {
                                                            Icon(
                                                                Icons.Filled.VerifiedUser,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        } else {
                                                            Icon(
                                                                Icons.Filled.Close,
                                                                contentDescription = "Remove",
                                                                tint = StitchTextSecondary,
                                                                modifier = Modifier
                                                                    .size(16.dp)
                                                                    .clickable {
                                                                        selectedMembers.removeAt(slotIdx)
                                                                        reindexPayoutOrder()
                                                                    }
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                Surface(
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color(0xFFF8FAFC),
                                                    border = BorderStroke(1.dp, StitchBorder)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(22.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(Color(0xFFE2E8F0)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                "$slotNum",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = StitchTextSecondary
                                                            )
                                                        }
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                            "Open Slot",
                                                            fontSize = 12.sp,
                                                            color = StitchTextSecondary,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Icon(
                                                            Icons.Filled.PersonAddAlt,
                                                            contentDescription = null,
                                                            tint = Color(0xFF94A3B8),
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (pair.size == 1) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── DIRECT STELLAR OR FEDERATED ID / MOBILE LOOKUP CARD ─────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, StitchBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = StitchBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Direct Stellar or Federated ID",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = StitchTextPrimary
                                )
                            }
                            Text(
                                "Add wallet address or registered mobile number directly to smart contract whitelist authorization.",
                                fontSize = 12.sp,
                                color = StitchTextSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = directIdentifierInput,
                                    onValueChange = {
                                        directIdentifierInput = it
                                        directLookupError = null
                                    },
                                    placeholder = { Text("Enter Stellar public key or phone", fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Search, contentDescription = null, tint = StitchTextSecondary, modifier = Modifier.size(18.dp))
                                    },
                                    trailingIcon = {
                                        TextButton(
                                            onClick = {
                                                val clip = clipboard.getText()?.text.orEmpty()
                                                if (clip.isNotBlank()) directIdentifierInput = clip.trim()
                                            }
                                        ) {
                                            Text("Paste", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StitchBlue)
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = StitchNavy,
                                        unfocusedBorderColor = StitchBorder
                                    )
                                )

                                Button(
                                    onClick = {
                                        val query = directIdentifierInput.trim()
                                        if (query.isBlank() || isLookingUpDirect) return@Button
                                        scope.launch {
                                            isLookingUpDirect = true
                                            directLookupError = null
                                            val matchedContact = frequentContacts.find {
                                                it.phone.equals(query, ignoreCase = true) ||
                                                    it.name.equals(query, ignoreCase = true) ||
                                                    it.stellarPublicKey.equals(query, ignoreCase = true)
                                            } ?: AuthRepository.findRegisteredUser(query)
                                            isLookingUpDirect = false
                                            if (matchedContact == null) {
                                                directLookupError = "No registered PUNDAR account found for \"$query\"."
                                            } else {
                                                tryAddUserToCircle(matchedContact)
                                                if (directLookupError == null) {
                                                    directIdentifierInput = ""
                                                }
                                            }
                                        }
                                    },
                                    enabled = directIdentifierInput.isNotBlank() && !isLookingUpDirect,
                                    modifier = Modifier.height(54.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StitchDarkNavy)
                                ) {
                                    if (isLookingUpDirect) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                    } else {
                                        Text("+ Add", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }

                            directLookupError?.let { err ->
                                Text(
                                    err,
                                    fontSize = 11.sp,
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // ── SHAREABLE INVITATION CARD (QR + DEEP LINK) ──────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, StitchBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.QrCode2,
                                        contentDescription = null,
                                        tint = StitchBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Shareable Invitation Card",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = StitchTextPrimary
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = StitchSurfaceBg,
                                    border = BorderStroke(1.dp, StitchBorder)
                                ) {
                                    Text(
                                        "ENCRYPTED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = StitchTextSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, StitchBorder),
                                shadowElevation = 2.dp,
                                modifier = Modifier.size(140.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
                                    if (qrBitmap != null) {
                                        Image(
                                            bitmap = qrBitmap.asImageBitmap(),
                                            contentDescription = "Circle Invitation QR",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            Icons.Filled.QrCode,
                                            contentDescription = null,
                                            tint = StitchNavy,
                                            modifier = Modifier.size(84.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(
                                "CIRCLE DEEP LINK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StitchTextSecondary,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(Modifier.height(6.dp))

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StitchSurfaceBg,
                                border = BorderStroke(1.dp, StitchBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        deepLinkUrl,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = StitchTextPrimary
                                    )
                                    Icon(
                                        Icons.Filled.Link,
                                        contentDescription = null,
                                        tint = StitchTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(deepLinkUrl))
                                        Toast.makeText(context, "Circle invitation link copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, StitchBorder)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp), tint = StitchTextPrimary)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Copy Link", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StitchTextPrimary)
                                }

                                Button(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "You're invited to join '$circleName' on PUNDAR Circle (Stellar Soroban Testnet). Contribution: ₱${String.format(Locale.US, "%,.2f", fixedContribution)} / round ($selectedFrequency). Review & opt in: https://$deepLinkUrl"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Circle Invitation"))
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StitchBlueLight)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(15.dp), tint = StitchBlue)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Share / SMS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StitchBlue)
                                }
                            }
                        }
                    }
                }

                // ── FREQUENT PUNDAR CONTACTS (WITH PUNDAR POINTS & BAN CHECK) ──
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, StitchBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Contacts,
                                        contentDescription = null,
                                        tint = StitchBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Frequent PUNDAR Contacts",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = StitchTextPrimary
                                    )
                                }
                                Text(
                                    "RECENT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StitchTextSecondary
                                )
                            }
                            Text(
                                "Only registered and verified PUNDAR accounts are shown below. Members with failed payments are banned from joining.",
                                fontSize = 12.sp,
                                color = StitchTextSecondary
                            )

                            if (frequentContacts.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = StitchSurfaceBg,
                                    border = BorderStroke(1.dp, StitchBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "No other registered PUNDAR accounts loaded yet. Use the Direct Stellar or Phone lookup box above to add a registered user.",
                                        fontSize = 12.sp,
                                        color = StitchTextSecondary,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }

                            frequentContacts.forEach { contact ->
                                val isAdded = selectedMembers.any {
                                    it.userId == contact.phone || it.name.equals(contact.name, ignoreCase = true)
                                }
                                val isBanned = contact.isCircleBanned
                                val initials = contact.name.trim().split(" ")
                                    .filter { it.isNotBlank() }
                                    .let { parts ->
                                        if (parts.size >= 2) (parts.first().take(1) + parts.last().take(1)).uppercase()
                                        else contact.name.take(2).uppercase()
                                    }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isBanned) Color(0xFFFEF2F2) else StitchSurfaceBg,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isBanned) Color(0xFFFECACA) else StitchBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(if (isBanned) Color(0xFFDC2626) else StitchNavy),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                initials,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    contact.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = StitchTextPrimary
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isBanned) Color(0xFFFEE2E2) else Color(0xFFFEF3C7)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.Star,
                                                            contentDescription = null,
                                                            tint = if (isBanned) Color(0xFFDC2626) else Color(0xFFD97706),
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Spacer(Modifier.width(2.dp))
                                                        Text(
                                                            "${contact.pundarScore}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = if (isBanned) Color(0xFF991B1B) else Color(0xFFB45309)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                if (isBanned) "BANNED • ${contact.circleBanReason.ifBlank { "Failed previous Circle payment" }}"
                                                else "@${contact.name.lowercase().replace(" ", "")} • ${contact.phone}",
                                                fontSize = 11.sp,
                                                color = if (isBanned) Color(0xFFDC2626) else StitchTextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(Modifier.width(8.dp))

                                        when {
                                            isBanned -> {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color(0xFFFEE2E2),
                                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                                                ) {
                                                    Text(
                                                        "Banned",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF991B1B),
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                            isAdded -> {
                                                Surface(
                                                    onClick = {
                                                        selectedMembers.removeAll {
                                                            !it.isYou && (it.userId == contact.phone || it.name.equals(contact.name, ignoreCase = true))
                                                        }
                                                        reindexPayoutOrder()
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = StitchBlueLight,
                                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Filled.Check, contentDescription = null, tint = StitchBlue, modifier = Modifier.size(14.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Added", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StitchBlue)
                                                    }
                                                }
                                            }
                                            else -> {
                                                OutlinedButton(
                                                    onClick = { tryAddUserToCircle(contact) },
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, StitchBorder),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text("+ Invite", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StitchTextPrimary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── SOROBAN SMART CONTRACT VERIFICATION CARD ────────────────
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, StitchBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StitchDarkNavy),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Soroban Smart Contract Verification",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = StitchTextPrimary
                                )
                                Text(
                                    "All invited members must sign before activation. Unaccepted slots expire after 48h back to open capacity.",
                                    fontSize = 11.sp,
                                    color = StitchTextSecondary
                                )
                            }
                        }
                    }
                }

                // ── BOTTOM ACTION BUTTONS (STEP 1B) ─────────────────────────
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        validationError?.let { err ->
                            Text(
                                err,
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                if (selectedMembers.size < 2) {
                                    validationError = "Please allocate at least 2 verified participants to form a Circle."
                                    return@Button
                                }
                                validationError = null
                                agreedToAll12Clauses = true
                                showConfirmContractSubmitDialog = true
                            },
                            enabled = !isSubmitting && !isCurrentUserBanned,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StitchDarkNavy)
                        ) {
                            Text(
                                "Confirm Invited Members (${selectedMembers.size})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        OutlinedButton(
                            onClick = { currentSubStep = 0 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, StitchBorder),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = StitchTextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Back to Circle Parameters",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
