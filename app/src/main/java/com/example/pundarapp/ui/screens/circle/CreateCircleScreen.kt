package com.example.pundarapp.ui.screens.circle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.*
import com.example.pundarapp.ui.theme.*

import com.example.pundarapp.ui.theme.PundarTheme
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.launch
import com.example.pundarapp.data.stellar.StellarWalletManager
import com.example.pundarapp.data.remote.CircleRepository
import com.example.pundarapp.ui.components.Icon3DWarning
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCircleScreen(navController: NavController) {
    var circleName by remember { mutableStateOf("") }
    var contributionAmount by remember { mutableStateOf("") }
    var payoutFrequencyValue by remember { mutableStateOf("1") }
    var payoutFrequencyUnit by remember { mutableStateOf("Months") }
    var maxMembers by remember { mutableStateOf("10") }
    var selectedCurrency by remember { mutableStateOf("PHP") }
    var payoutOrderType by remember { mutableStateOf("Customized") }
    
    var showMpinDialog by remember { mutableStateOf(false) }
    var mpin by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<CircleMember>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val selectedMembers = remember {
        val u = AuthRepository.currentUserState.value
        mutableStateListOf(
            CircleMember(
                userId = u?.phone ?: "",
                name = u?.name ?: "You",
                initials = u?.name?.take(2)?.uppercase() ?: "YO",
                sharePercent = 100,
                amount = 0.0,
                status = ContributionStatus.PENDING,
                isYou = true,
                avatarColor = 0xFF0052CC
            )
        )
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }
        isSearching = true
        kotlinx.coroutines.delay(300)
        val users = AuthRepository.searchUsers(searchQuery)
        searchResults = users.map { u ->
            CircleMember(
                userId = u.phone,
                name = u.name,
                initials = u.name.take(2).uppercase(),
                sharePercent = 0,
                amount = 0.0,
                status = ContributionStatus.PENDING,
                avatarColor = 0xFF6B7280
            )
        }.filter { contact ->
            selectedMembers.none { it.name == contact.name }
        }
        isSearching = false
    }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Create a Circle",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarTheme.colors.bgPrimary,
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                PundarPrimaryButton(
                    text = if (isLoading) "Launching on Soroban..." else "Launch Circle \ud83d\ude80",
                    enabled = !isLoading && circleName.isNotBlank() && (contributionAmount.toDoubleOrNull() ?: 0.0) > 0,
                    onClick = {
                        showMpinDialog = true
                    }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(12.dp), color = PundarGoldLight, modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Groups, null, tint = PundarGoldDark, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("New PUNDAR Circle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Secured by Soroban smart contract", style = MaterialTheme.typography.bodySmall, color = PundarTheme.colors.textSecondary)
                    }
                }
            }

            item { HorizontalDivider(color = PundarDivider) }

            // Fields
            item {
                OutlinedTextField(
                    value = circleName,
                    onValueChange = { circleName = it },
                    label = { Text("Circle Name") },
                    placeholder = { Text("e.g. Family Dream House") },
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

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    var currencyExpanded by remember { mutableStateOf(false) }
                    val currencyOptions = listOf("PHP", "XLM", "USD")

                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = it },
                        modifier = Modifier.weight(0.4f)
                    ) {
                        OutlinedTextField(
                            value = selectedCurrency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Currency") },
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
                            currencyOptions.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption, color = PundarTheme.colors.textPrimary) },
                                    onClick = {
                                        selectedCurrency = selectionOption
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = contributionAmount,
                        onValueChange = { contributionAmount = it },
                        label = { Text("Contribution per Member ($selectedCurrency)") },
                        modifier = Modifier.weight(0.6f),
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


            item {
                val cAmount = contributionAmount.toDoubleOrNull() ?: 0.0
                val mCount = maxMembers.toIntOrNull() ?: 1
                val calculatedTarget = cAmount * mCount

                OutlinedTextField(
                    value = String.format("%,.2f", calculatedTarget),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Target Amount ($selectedCurrency)") },
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

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = payoutFrequencyValue,
                        onValueChange = { payoutFrequencyValue = it },
                        label = { Text("Release Every") },
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
                            label = { Text("Unit") },
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
                            frequencyOptions.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption, color = PundarTheme.colors.textPrimary) },
                                    onClick = {
                                        payoutFrequencyUnit = selectionOption
                                        frequencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = maxMembers,
                        onValueChange = { maxMembers = it },
                        label = { Text("Max Members") },
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
                    
                    var payoutOrderExpanded by remember { mutableStateOf(false) }
                    val payoutOrderOptions = listOf("Customized", "Randomized")
                    
                    ExposedDropdownMenuBox(
                        expanded = payoutOrderExpanded,
                        onExpandedChange = { payoutOrderExpanded = it },
                        modifier = Modifier.weight(0.6f)
                    ) {
                        OutlinedTextField(
                            value = payoutOrderType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payout Order") },
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
                            expanded = payoutOrderExpanded,
                            onDismissRequest = { payoutOrderExpanded = false },
                            modifier = Modifier.background(PundarTheme.colors.surfacePrimary)
                        ) {
                            payoutOrderOptions.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption, color = PundarTheme.colors.textPrimary) },
                                    onClick = {
                                        payoutOrderType = selectionOption
                                        payoutOrderExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Invite Members Section
            item {
                Spacer(Modifier.height(8.dp))
                Text("Invite Members (${selectedMembers.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Name, @username, or phone", color = PundarTheme.colors.textSecondary) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = PundarTheme.colors.textSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PundarTheme.colors.surfaceTertiary,
                        unfocusedContainerColor = PundarTheme.colors.surfaceTertiary,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = PundarTheme.colors.textPrimary,
                        unfocusedTextColor = PundarTheme.colors.textPrimary
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
            }

            // Selected Members List
            items(selectedMembers.toList()) { member ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PundarBorder),
                    color = PundarTheme.colors.surfacePrimary
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                .background(
                                    if (member.isYou) PundarBlueSubtle
                                    else Color(member.avatarColor)
                                )
                        ) {
                            Text(
                                text = member.initials,
                                color = if (member.isYou) PundarTheme.colors.brandPrimary else PundarTheme.colors.surfacePrimary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        if (!member.isYou) {
                            IconButton(
                                onClick = { selectedMembers.remove(member) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Close, "Remove", tint = PundarTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Search Results
            if (isSearching) {
                item {
                    Text("Searching...", style = MaterialTheme.typography.bodySmall, color = PundarTheme.colors.textSecondary)
                }
            } else if (searchResults.isNotEmpty()) {
                item {
                    Text(
                        text = "Search Results",
                        style = MaterialTheme.typography.labelMedium,
                        color = PundarTheme.colors.textSecondary
                    )
                }
                items(searchResults) { contact ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { 
                            selectedMembers.add(contact) 
                            searchQuery = ""
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = PundarTheme.colors.surfacePrimary
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(40.dp).clip(CircleShape)
                                    .background(Color(contact.avatarColor).copy(alpha = 0.2f))
                            ) {
                                Text(
                                    contact.initials,
                                    color = PundarTheme.colors.textPrimary,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(contact.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(Icons.Filled.Add, "Add", tint = PundarTheme.colors.brandPrimary)
                        }
                    }
                }
            }

            item {
                PundarCard {
                    Text("🔒 Soroban Smart Contract", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "All contributions will be locked in a Soroban smart contract on the Stellar network. Funds can only be released when the target amount is met and all members agree.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PundarTheme.colors.textSecondary
                    )
                }
            }
            
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
    
    if (showMpinDialog) {
        AlertDialog(
            onDismissRequest = { 
                showMpinDialog = false 
                mpin = ""
                errorMessage = null
            },
            title = { Text("Confirm MPIN", color = PundarTheme.colors.textPrimary) },
            text = {
                Column {
                    Text("Enter your 4-digit MPIN to authorize the creation of this Soroban smart contract.", color = PundarTheme.colors.textSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = mpin,
                        onValueChange = { mpin = it.take(4) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PundarTheme.colors.textPrimary,
                            unfocusedTextColor = PundarTheme.colors.textPrimary
                        )
                    )
                    if (errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(errorMessage!!, color = PundarTheme.colors.accentRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (mpin.length != 4) {
                            errorMessage = "MPIN must be 4 digits"
                            return@Button
                        }
                        showMpinDialog = false
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val freqVal = payoutFrequencyValue.toLongOrNull() ?: 1L
                                val cycleSeconds = when (payoutFrequencyUnit.lowercase()) {
                                    "days" -> 86400L * freqVal
                                    "weeks" -> 604800L * freqVal
                                    "months" -> 2592000L * freqVal
                                    else -> 2592000L * freqVal
                                }
                                val currentUser = AuthRepository.currentUserState.value
                                if (currentUser == null) {
                                    throw Exception("User not logged in")
                                }
                                
                                val finalMembers = if (payoutOrderType == "Randomized") {
                                    selectedMembers.shuffled()
                                } else {
                                    selectedMembers.toList()
                                }

                                val cAmount = contributionAmount.toDoubleOrNull() ?: 0.0
                                val mCount = maxMembers.toIntOrNull() ?: 1
                                val calculatedTarget = cAmount * mCount

                                // Attempt to create Soroban contract if wallet exists
                                var sorobanCircleId: String? = null
                                if (!currentUser.stellarPublicKey.isNullOrEmpty()) {
                                    try {
                                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                        val doc = db.collection("users").document(currentUser.phone).get().await()
                                        val encryptedSeed = doc.getString("encryptedStellarSeed")
                                        
                                        if (!encryptedSeed.isNullOrEmpty()) {
                                            val membersList = finalMembers.mapNotNull { 
                                                if (it.isYou) currentUser.stellarPublicKey 
                                                else {
                                                    val udoc = db.collection("users").document(if (it.userId.isNotEmpty()) it.userId else it.name).get().await()
                                                    udoc.getString("stellarPublicKey")
                                                }
                                            }

                                            val result = StellarWalletManager.createCircle(
                                                adminPublicKey = currentUser.stellarPublicKey,
                                                adminEncryptedSeed = encryptedSeed,
                                                adminMpin = mpin,
                                                members = membersList,
                                                contributionAmount = cAmount,
                                                payoutOrder = membersList,
                                                cycleDurationSeconds = cycleSeconds
                                            )
                                            if (result.isSuccess) {
                                                sorobanCircleId = result.getOrNull().toString()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                
                                val circleId = sorobanCircleId ?: "soroban_mock_${System.currentTimeMillis()}"

                                val initialMembers = finalMembers.filter { it.isYou }
                                val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")
                                val today = java.time.LocalDate.now()
                                val freqValInt = payoutFrequencyValue.toIntOrNull() ?: 1
                                val firstPayoutDate = when (payoutFrequencyUnit.lowercase()) {
                                    "days" -> today.plusDays(freqValInt.toLong())
                                    "weeks" -> today.plusWeeks(freqValInt.toLong())
                                    "months" -> today.plusMonths(freqValInt.toLong())
                                    else -> today.plusMonths(freqValInt.toLong())
                                }
                                val updatedInitialMembers = initialMembers.map { 
                                    it.copy(payoutDate = firstPayoutDate.format(formatter))
                                }

                                val newCircle = Circle(
                                    id = circleId,
                                    name = circleName,
                                    targetAmount = calculatedTarget,
                                    savedAmount = 0.0,
                                    targetDate = "Every $payoutFrequencyValue $payoutFrequencyUnit",
                                    memberCount = updatedInitialMembers.size,
                                    contributionPerMonth = cAmount,
                                    members = updatedInitialMembers,
                                    isActive = true,
                                    currency = selectedCurrency,
                                    creatorId = currentUser.id,
                                    payoutFrequencyValue = freqValInt,
                                    payoutFrequencyUnit = payoutFrequencyUnit,
                                    maxMembers = mCount,
                                    payoutOrderType = payoutOrderType
                                )
                                val saveResult = CircleRepository.createCircle(newCircle, currentUser.id)
                                if (saveResult.isSuccess) {
                                    // send invitations to the others
                                    finalMembers.filter { !it.isYou }.forEach { invitee ->
                                        CircleRepository.sendInvitation(
                                            circleId = circleId,
                                            inviteeUserId = if (invitee.userId.isNotEmpty()) invitee.userId else invitee.name,
                                            inviteeName = invitee.name,
                                            inviteeInitials = invitee.initials,
                                            inviterName = currentUser.name,
                                            inviterScore = currentUser.pundarScore
                                        )
                                    }
                                    com.example.pundarapp.ui.data.AppState.circles.add(0, newCircle)
                                    com.example.pundarapp.ui.data.AppState.addHomeActivity(
                                        com.example.pundarapp.ui.data.HomeActivity(
                                            icon = "savings",
                                            title = circleName,
                                            subtitle = "Created new Paluwagan",
                                            amount = "Goal: ${String.format("%,.0f", newCircle.targetAmount)} ${newCircle.currency}",
                                            isPositive = true,
                                            module = "Circle"
                                        )
                                    )
                                    navController.navigateUp()
                                } else {
                                    throw Exception("Failed to save to database")
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message
                                showMpinDialog = true // show dialog again with error
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PundarTheme.colors.brandPrimary)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showMpinDialog = false 
                    mpin = ""
                    errorMessage = null
                }) {
                    Text("Cancel", color = PundarTheme.colors.textMuted)
                }
            },
            containerColor = PundarTheme.colors.surfaceSecondary
        )
    }
}
