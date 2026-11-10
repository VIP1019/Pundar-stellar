package com.example.pundarapp.ui.screens.circle

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.remote.CircleRepository
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.*
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCircleScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var circleName by remember { mutableStateOf("") }
    var contributionAmount by remember { mutableStateOf("100") }
    var payoutFrequencyValue by remember { mutableStateOf("1") }
    var payoutFrequencyUnit by remember { mutableStateOf("Weeks") }
    var selectedCurrency by remember { mutableStateOf("XLM") }

    // Step 1 = Configuration (Section 2), Step 2 = Review Circle Terms (Section 3)
    var isReviewStep by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<CircleMember>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val selectedMembers = remember {
        val u = AuthRepository.currentUserState.value
        val myPhone = u?.phone?.ifBlank { AuthRepository.getCurrentUserPhone() } ?: AuthRepository.getCurrentUserPhone()
        val myName = u?.name?.ifBlank { AuthRepository.getCurrentUserName() } ?: AuthRepository.getCurrentUserName().ifBlank { "You" }
        val myPubKey = (u?.stellarPublicKey?.ifBlank { AuthRepository.getCurrentUserStellarPublicKey().orEmpty() }
            ?: AuthRepository.getCurrentUserStellarPublicKey()).orEmpty()
        mutableStateListOf(
            CircleMember(
                userId = myPhone,
                name = myName,
                initials = myName.take(2).uppercase(),
                sharePercent = 100,
                amount = 0.0,
                status = ContributionStatus.PENDING,
                isYou = true,
                avatarColor = 0xFF0052CC,
                stellarPublicKey = myPubKey,
                payoutRound = 1,
                acceptanceStatus = "ACCEPTED",
                acceptedTermsVersion = 1
            )
        )
    }

    fun reindexPayoutOrder() {
        val count = selectedMembers.size.coerceAtLeast(1)
        val share = 100 / count
        for (i in selectedMembers.indices) {
            selectedMembers[i] = selectedMembers[i].copy(
                payoutRound = i + 1,
                sharePercent = share
            )
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }
        isSearching = true
        kotlinx.coroutines.delay(250)
        val users = AuthRepository.searchUsers(searchQuery)
        val directUser = AuthRepository.findRegisteredUser(searchQuery)
        val combined = buildList {
            if (directUser != null) add(directUser)
            users.forEach { u -> if (none { it.phone == u.phone }) add(u) }
        }
        searchResults = combined.map { u ->
            CircleMember(
                userId = u.phone.ifBlank { u.id },
                name = u.name,
                initials = u.name.take(2).uppercase(),
                sharePercent = 0,
                amount = 0.0,
                status = ContributionStatus.PENDING,
                avatarColor = 0xFF2563EB,
                stellarPublicKey = u.stellarPublicKey.orEmpty(),
                payoutRound = selectedMembers.size + 1,
                acceptanceStatus = "PENDING",
                acceptedTermsVersion = 0
            )
        }.filter { contact ->
            selectedMembers.none { it.userId == contact.userId || it.name.equals(contact.name, ignoreCase = true) }
        }
        isSearching = false
    }

    val fixedContribution = contributionAmount.toDoubleOrNull() ?: 0.0
    val memberCount = selectedMembers.size
    val scheduledPayoutAmount = fixedContribution * memberCount
    val freqInt = payoutFrequencyValue.toIntOrNull() ?: 1
    val scheduleText = "Every $freqInt $payoutFrequencyUnit"

    val isFormValid = circleName.isNotBlank() && fixedContribution > 0.0 && memberCount >= 2 && freqInt > 0

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = if (isReviewStep) "Review Circle Terms" else "Create Savings Circle",
                onBack = {
                    if (isReviewStep) isReviewStep = false
                    else navController.navigateUp()
                }
            )
        },
        containerColor = PundarTheme.colors.bgPrimary,
        bottomBar = {
            Surface(
                color = PundarTheme.colors.surfacePrimary,
                border = BorderStroke(1.dp, PundarTheme.colors.borderPrimary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (validationError != null) {
                        Text(
                            validationError!!,
                            color = PundarTheme.colors.accentRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    if (!isReviewStep) {
                        PundarPrimaryButton(
                            text = "Review Circle Terms →",
                            enabled = isFormValid,
                            onClick = {
                                if (circleName.isBlank()) {
                                    validationError = "Please enter a Circle name."
                                    return@PundarPrimaryButton
                                }
                                if (fixedContribution <= 0.0) {
                                    validationError = "Fixed contribution amount must be greater than zero."
                                    return@PundarPrimaryButton
                                }
                                if (selectedMembers.size < 2) {
                                    validationError = "Add at least 2 verified participants to form a Circle."
                                    return@PundarPrimaryButton
                                }
                                reindexPayoutOrder()
                                validationError = null
                                isReviewStep = true
                            }
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isReviewStep = false },
                                enabled = !isSubmitting,
                                modifier = Modifier.weight(0.42f).height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.Edit, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Edit Proposal", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    isSubmitting = true
                                    validationError = null
                                    scope.launch {
                                        try {
                                            val currentUser = AuthRepository.currentUserState.value
                                            val organizerId = currentUser?.phone?.ifBlank { AuthRepository.getCurrentUserPhone() }
                                                ?: AuthRepository.getCurrentUserPhone()
                                            val organizerName = currentUser?.name?.ifBlank { AuthRepository.getCurrentUserName() }
                                                ?: AuthRepository.getCurrentUserName().ifBlank { "Organizer" }
                                            val organizerScore = currentUser?.pundarScore ?: 800

                                            val proposalId = "circle_prop_${System.currentTimeMillis()}"
                                            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")
                                            val today = java.time.LocalDate.now()

                                            val configuredMembers = selectedMembers.mapIndexed { idx, m ->
                                                val roundNum = idx + 1
                                                val payoutDate = when (payoutFrequencyUnit.lowercase()) {
                                                    "days" -> today.plusDays((freqInt * roundNum).toLong())
                                                    "weeks" -> today.plusWeeks((freqInt * roundNum).toLong())
                                                    else -> today.plusMonths((freqInt * roundNum).toLong())
                                                }
                                                m.copy(
                                                    payoutRound = roundNum,
                                                    payoutDate = payoutDate.format(formatter),
                                                    amount = 0.0,
                                                    status = ContributionStatus.PENDING,
                                                    acceptanceStatus = if (m.isYou || m.userId == organizerId) "ACCEPTED" else "PENDING",
                                                    acceptedTermsVersion = if (m.isYou || m.userId == organizerId) 1 else 0
                                                )
                                            }

                                            val allAcceptedAlready = configuredMembers.all { it.acceptanceStatus == "ACCEPTED" }
                                            val initialLifecycle = if (allAcceptedAlready) "READY_TO_ACTIVATE" else "AWAITING_ACCEPTANCE"

                                            val createTxRecord = CircleTransactionRecord(
                                                id = "tx_prop_${System.currentTimeMillis()}",
                                                type = "PROPOSAL_CREATED",
                                                title = "Circle Proposal Created (Terms v1)",
                                                memberName = organizerName,
                                                memberPhone = organizerId,
                                                roundNumber = 1,
                                                amount = scheduledPayoutAmount,
                                                currency = selectedCurrency,
                                                txHash = "PROPOSAL_V1_PENDING_CONSENT",
                                                timestamp = System.currentTimeMillis(),
                                                status = "AWAITING_MEMBER_CONSENT"
                                            )

                                            val newCircle = Circle(
                                                id = proposalId,
                                                name = circleName.trim(),
                                                targetAmount = scheduledPayoutAmount,
                                                savedAmount = 0.0,
                                                targetDate = scheduleText,
                                                memberCount = configuredMembers.size,
                                                contributionPerMonth = fixedContribution,
                                                members = configuredMembers,
                                                isActive = false,
                                                currency = selectedCurrency,
                                                creatorId = organizerId,
                                                creatorName = organizerName,
                                                payoutFrequencyValue = freqInt,
                                                payoutFrequencyUnit = payoutFrequencyUnit,
                                                maxMembers = configuredMembers.size,
                                                payoutOrderType = "Deterministic Fixed Order",
                                                lifecycleStatus = initialLifecycle,
                                                termsVersion = 1,
                                                currentRound = 1,
                                                sorobanCircleId = 0L,
                                                activationTxHash = "",
                                                paidRounds = emptyList(),
                                                transactionHistory = listOf(createTxRecord)
                                            )

                                            val saveRes = CircleRepository.createCircle(newCircle, organizerId)
                                            if (saveRes.isFailure) {
                                                throw saveRes.exceptionOrNull() ?: Exception("Failed to submit Circle proposal")
                                            }

                                            configuredMembers.filter { !it.isYou && it.userId != organizerId }.forEach { invitee ->
                                                CircleRepository.sendInvitation(
                                                    circleId = proposalId,
                                                    inviteeUserId = invitee.userId.ifBlank { invitee.name },
                                                    inviteeName = invitee.name,
                                                    inviteeInitials = invitee.initials,
                                                    inviterName = organizerName,
                                                    inviterScore = organizerScore
                                                )
                                            }

                                            AppState.circles.removeAll { it.id == proposalId }
                                            AppState.circles.add(0, newCircle)
                                            AppState.addHomeActivity(
                                                HomeActivity(
                                                    icon = "savings",
                                                    title = newCircle.name,
                                                    subtitle = "Circle Proposal Sent (Terms v1 · ${configuredMembers.size} members)",
                                                    amount = "${String.format("%,.0f", scheduledPayoutAmount)} $selectedCurrency/round",
                                                    isPositive = true,
                                                    module = "Circle"
                                                )
                                            )

                                            Toast.makeText(
                                                context,
                                                "Proposal submitted! Invitations sent to ${configuredMembers.size - 1} member(s).",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            navController.navigate(Routes.circleDetail(proposalId)) {
                                                popUpTo(Routes.CIRCLE) { inclusive = false }
                                            }
                                        } catch (e: Exception) {
                                            validationError = e.message ?: "Failed to submit proposal"
                                        } finally {
                                            isSubmitting = false
                                        }
                                    }
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier.weight(0.58f).height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PundarTheme.colors.brandPrimary)
                            ) {
                                Icon(Icons.Filled.Send, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (isSubmitting) "Sending..." else "Send Invitations",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (!isReviewStep) {
            // ── STEP 1: CONFIGURE CIRCLE PROPOSAL (Section 2) ──────────
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PundarGoldLight,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Groups, null, tint = PundarGoldDark, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Step 1 of 2 · Configure Circle Terms",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PundarTheme.colors.textPrimary
                            )
                            Text(
                                "Define fixed contributions, schedule, verified members, and deterministic rotating payout order.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PundarTheme.colors.textSecondary
                            )
                        }
                    }
                }

                item { HorizontalDivider(color = PundarDivider) }

                // Circle Name
                item {
                    OutlinedTextField(
                        value = circleName,
                        onValueChange = { circleName = it },
                        label = { Text("Circle Name / Title") },
                        placeholder = { Text("e.g. Batangas Community Savings Circle") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PundarTheme.colors.brandPrimary,
                            focusedLabelColor = PundarTheme.colors.brandPrimary,
                            focusedTextColor = PundarTheme.colors.textPrimary,
                            unfocusedTextColor = PundarTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Currency + Fixed Contribution Amount
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        var currencyExpanded by remember { mutableStateOf(false) }
                        val currencyOptions = listOf("XLM", "PHP")

                        ExposedDropdownMenuBox(
                            expanded = currencyExpanded,
                            onExpandedChange = { currencyExpanded = it },
                            modifier = Modifier.weight(0.38f)
                        ) {
                            OutlinedTextField(
                                value = selectedCurrency,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Asset") },
                                modifier = Modifier.menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PundarTheme.colors.brandPrimary,
                                    focusedLabelColor = PundarTheme.colors.brandPrimary,
                                    focusedTextColor = PundarTheme.colors.textPrimary,
                                    unfocusedTextColor = PundarTheme.colors.textPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = currencyExpanded,
                                onDismissRequest = { currencyExpanded = false },
                                modifier = Modifier.background(PundarTheme.colors.surfacePrimary)
                            ) {
                                currencyOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt, color = PundarTheme.colors.textPrimary) },
                                        onClick = {
                                            selectedCurrency = opt
                                            currencyExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = contributionAmount,
                            onValueChange = { contributionAmount = it },
                            label = { Text("Fixed Contribution ($selectedCurrency)") },
                            modifier = Modifier.weight(0.62f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PundarTheme.colors.brandPrimary,
                                focusedLabelColor = PundarTheme.colors.brandPrimary,
                                focusedTextColor = PundarTheme.colors.textPrimary,
                                unfocusedTextColor = PundarTheme.colors.textPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }

                // Contribution Schedule
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = payoutFrequencyValue,
                            onValueChange = { payoutFrequencyValue = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Contribute Every") },
                            modifier = Modifier.weight(0.4f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PundarTheme.colors.brandPrimary,
                                focusedLabelColor = PundarTheme.colors.brandPrimary,
                                focusedTextColor = PundarTheme.colors.textPrimary,
                                unfocusedTextColor = PundarTheme.colors.textPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        var frequencyExpanded by remember { mutableStateOf(false) }
                        val frequencyOptions = listOf("Days", "Weeks", "Months")

                        ExposedDropdownMenuBox(
                            expanded = frequencyExpanded,
                            onExpandedChange = { frequencyExpanded = it },
                            modifier = Modifier.weight(0.6f)
                        ) {
                            OutlinedTextField(
                                value = payoutFrequencyUnit,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Schedule Interval") },
                                modifier = Modifier.menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PundarTheme.colors.brandPrimary,
                                    focusedLabelColor = PundarTheme.colors.brandPrimary,
                                    focusedTextColor = PundarTheme.colors.textPrimary,
                                    unfocusedTextColor = PundarTheme.colors.textPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = frequencyExpanded,
                                onDismissRequest = { frequencyExpanded = false },
                                modifier = Modifier.background(PundarTheme.colors.surfacePrimary)
                            ) {
                                frequencyOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt, color = PundarTheme.colors.textPrimary) },
                                        onClick = {
                                            payoutFrequencyUnit = opt
                                            frequencyExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Derived Scheduled Payout Summary Box
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = PundarTheme.colors.brandPrimary.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, PundarTheme.colors.brandPrimary.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Total Participants",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PundarTheme.colors.textSecondary
                                )
                                Text(
                                    "$memberCount Members ($memberCount Rounds)",
                                    fontWeight = FontWeight.Bold,
                                    color = PundarTheme.colors.textPrimary
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Derived Scheduled Payout / Round",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PundarTheme.colors.textSecondary
                                )
                                Text(
                                    "${String.format("%,.2f", scheduledPayoutAmount)} $selectedCurrency",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = PundarTheme.colors.brandPrimary
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Formula: ${String.format("%,.2f", fixedContribution)} $selectedCurrency × $memberCount members = ${String.format("%,.2f", scheduledPayoutAmount)} $selectedCurrency per round",
                                style = MaterialTheme.typography.labelSmall,
                                color = PundarTheme.colors.textMuted
                            )
                        }
                    }
                }

                // Add Verified Participants Search
                item {
                    Text(
                        "Add Verified PUNDAR Participants",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PundarTheme.colors.textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Only registered & verified PUNDAR accounts can be invited to a Circle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PundarTheme.colors.textSecondary
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by phone number, name, or Stellar address", color = PundarTheme.colors.textSecondary) },
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = PundarTheme.colors.textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PundarTheme.colors.surfaceTertiary,
                            unfocusedContainerColor = PundarTheme.colors.surfaceTertiary,
                            focusedBorderColor = PundarTheme.colors.brandPrimary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = PundarTheme.colors.textPrimary,
                            unfocusedTextColor = PundarTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                }

                if (isSearching) {
                    item {
                        Text("Searching verified accounts...", style = MaterialTheme.typography.bodySmall, color = PundarTheme.colors.textSecondary)
                    }
                } else if (searchQuery.isNotBlank() && searchResults.isEmpty()) {
                    item {
                        Text(
                            "No unadded verified PUNDAR account found matching \"$searchQuery\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = PundarTheme.colors.accentRed
                        )
                    }
                } else if (searchResults.isNotEmpty()) {
                    items(searchResults) { contact ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedMembers.add(contact.copy(payoutRound = selectedMembers.size + 1))
                                reindexPayoutOrder()
                                searchQuery = ""
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, PundarTheme.colors.brandPrimary.copy(alpha = 0.35f)),
                            color = PundarTheme.colors.surfacePrimary
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(40.dp).clip(CircleShape)
                                        .background(PundarTheme.colors.brandPrimary.copy(alpha = 0.15f))
                                ) {
                                    Text(
                                        contact.initials,
                                        color = PundarTheme.colors.brandPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(contact.name, fontWeight = FontWeight.Bold, color = PundarTheme.colors.textPrimary)
                                        Spacer(Modifier.width(6.dp))
                                        Icon(Icons.Filled.Verified, null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                    }
                                    Text(
                                        contact.userId,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PundarTheme.colors.textSecondary
                                    )
                                }
                                Icon(Icons.Filled.PersonAdd, "Add Member", tint = PundarTheme.colors.brandPrimary)
                            }
                        }
                    }
                }

                // Deterministic Rotating Payout Order List
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Deterministic Rotating Payout Order (${selectedMembers.size} Rounds)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PundarTheme.colors.textPrimary
                    )
                    Text(
                        "Assign the exact payout round for every member using the ↑ / ↓ arrows. Each member is assigned to exactly one round.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PundarTheme.colors.textSecondary
                    )
                }

                itemsIndexed(selectedMembers.toList()) { idx, member ->
                    val roundNum = idx + 1
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, PundarTheme.colors.borderPrimary),
                        color = PundarTheme.colors.surfacePrimary
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PundarTheme.colors.brandPrimary.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    "Round $roundNum",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = PundarTheme.colors.brandPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (member.isYou) "${member.name} (Organizer)" else member.name,
                                        fontWeight = FontWeight.Bold,
                                        color = PundarTheme.colors.textPrimary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Filled.Verified, null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                }
                                Text(
                                    "Receives ${String.format("%,.2f", scheduledPayoutAmount)} $selectedCurrency in Round $roundNum",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PundarTheme.colors.textSecondary
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (idx > 0) {
                                        val prev = selectedMembers[idx - 1]
                                        selectedMembers[idx - 1] = member
                                        selectedMembers[idx] = prev
                                        reindexPayoutOrder()
                                    }
                                },
                                enabled = idx > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.KeyboardArrowUp, "Move Up")
                            }
                            IconButton(
                                onClick = {
                                    if (idx < selectedMembers.size - 1) {
                                        val next = selectedMembers[idx + 1]
                                        selectedMembers[idx + 1] = member
                                        selectedMembers[idx] = next
                                        reindexPayoutOrder()
                                    }
                                },
                                enabled = idx < selectedMembers.size - 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.KeyboardArrowDown, "Move Down")
                            }
                            if (!member.isYou) {
                                IconButton(
                                    onClick = {
                                        selectedMembers.removeAt(idx)
                                        reindexPayoutOrder()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Close, "Remove", tint = PundarTheme.colors.accentRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ── STEP 2: REVIEW CIRCLE TERMS — PROPOSAL SUMMARY (Section 3) ──
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Info, null, tint = Color(0xFFD97706))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Pre-Activation Proposal (Terms v1 · Inactive)",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                                Text(
                                    "At this stage, the Circle is still a proposal and is not yet active. Every invited participant must explicitly accept these exact terms before the Circle can be activated on Soroban.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PundarTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }

                item {
                    PundarCard {
                        Text(
                            "Proposed Circle Terms Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PundarTheme.colors.textPrimary
                        )
                        Spacer(Modifier.height(12.dp))
                        ProposalSummaryRow("Circle Title", circleName)
                        ProposalSummaryRow("Terms Version", "Version 1 (Initial Proposal)")
                        ProposalSummaryRow("Fixed Contribution", "${String.format("%,.2f", fixedContribution)} $selectedCurrency / member / round")
                        ProposalSummaryRow("Contribution Schedule", scheduleText)
                        ProposalSummaryRow("Total Participants", "$memberCount Verified Members")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = PundarDivider)
                        ProposalSummaryRow(
                            "Scheduled Payout / Round",
                            "${String.format("%,.2f", scheduledPayoutAmount)} $selectedCurrency (${String.format("%,.0f", fixedContribution)} × $memberCount)",
                            highlight = true
                        )
                    }
                }

                item {
                    Text(
                        "Complete Rotating Payout Order (Round 1 to Round $memberCount)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PundarTheme.colors.textPrimary
                    )
                    Text(
                        "Once all members accept and the Circle is activated on Soroban, this payout order becomes immutable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PundarTheme.colors.textSecondary
                    )
                }

                itemsIndexed(selectedMembers.toList()) { idx, member ->
                    val roundNum = idx + 1
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, PundarTheme.colors.borderPrimary),
                        color = PundarTheme.colors.surfacePrimary
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PundarTheme.colors.brandPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "R$roundNum",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        if (member.isYou) "${member.name} (Organizer)" else member.name,
                                        fontWeight = FontWeight.Bold,
                                        color = PundarTheme.colors.textPrimary
                                    )
                                    Text(
                                        "Account: ${member.userId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PundarTheme.colors.textSecondary
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${String.format("%,.2f", scheduledPayoutAmount)} $selectedCurrency",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PundarTheme.colors.brandPrimary
                                )
                                Text(
                                    if (member.isYou) "Accepted (v1)" else "Invite Pending",
                                    fontSize = 11.sp,
                                    color = if (member.isYou) Color(0xFF059669) else Color(0xFFD97706),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProposalSummaryRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = PundarTheme.colors.textSecondary)
        Text(
            value,
            style = if (highlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (highlight) PundarTheme.colors.brandPrimary else PundarTheme.colors.textPrimary
        )
    }
}
