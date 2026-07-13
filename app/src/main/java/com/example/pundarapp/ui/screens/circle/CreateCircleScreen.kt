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
import kotlinx.coroutines.launch
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.*
import com.example.pundarapp.ui.theme.*
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCircleScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var circleName by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var monthlyContribution by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var maxMembers by remember { mutableStateOf("10") }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<CircleMember>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    var isCreating by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }

    val selectedMembers = remember {
        mutableStateListOf(
            CircleMember(
                userId = AuthRepository.getCurrentUserId() ?: "",
                name = AuthRepository.getCurrentUserName().ifBlank { SampleData.currentUser.name },
                initials = AuthRepository.getCurrentUserInitials().ifBlank { SampleData.currentUser.initials },
                sharePercent = 100,
                amount = 0.0,
                status = ContributionStatus.PENDING,
                isYou = true,
                avatarColor = 0xFF0052CC
            )
        )
    }

    val maxMembersInt = (maxMembers.toIntOrNull() ?: 10).coerceIn(2, 50)
    val remainingSlots = (maxMembersInt - selectedMembers.size).coerceAtLeast(0)
    val isAtCapacity = selectedMembers.size >= maxMembersInt

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }

        isSearching = true
        val users = AuthRepository.searchUsers(searchQuery)
        searchResults = users.map { u ->
            CircleMember(
                userId = u.id,
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
                title = "Create a Paluwagan",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarBackground,
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                createError?.let { err ->
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                }
                PundarPrimaryButton(
                    text = if (isCreating) "Launching..." else "Launch Paluwagan",
                    enabled = !isCreating && circleName.isNotBlank()
                        && (targetAmount.toDoubleOrNull() ?: 0.0) > 0
                        && selectedMembers.size <= maxMembersInt,
                    onClick = {
                        isCreating = true
                        createError = null
                        val creatorId = AuthRepository.getCurrentUserId() ?: ""
                        val newCircle = Circle(
                            id = "circle_${System.currentTimeMillis()}",
                            name = circleName,
                            targetAmount = targetAmount.toDoubleOrNull() ?: 0.0,
                            savedAmount = 0.0,
                            targetDate = targetDate.ifBlank { "TBD" },
                            memberCount = selectedMembers.size,
                            maxMembers = maxMembersInt,
                            contributionPerMonth = monthlyContribution.toDoubleOrNull() ?: 0.0,
                            members = selectedMembers.toList(),
                            creatorId = creatorId,
                            isActive = true
                        )
                        scope.launch {
                            val invitees = selectedMembers.filter { !it.isYou }
                            val result = AppState.createCircle(newCircle, invitees)
                            isCreating = false
                            result.onSuccess { navController.navigateUp() }
                                .onFailure { createError = it.message }
                        }
                    }
                )
            }
        }
    ) { padding ->
        // Date Picker Dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        // Convert evaluating UTC timestamp to LocalDate (DatePicker uses UTC internally)
                        val selectableDate = Instant.ofEpochMilli(utcTimeMillis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()

                        // Get tomorrow's local date
                        val tomorrow = LocalDate.now().plusDays(1)

                        // Selectable if date is tomorrow or later
                        return !selectableDate.isBefore(tomorrow)
                    }

                    override fun isSelectableYear(year: Int): Boolean {
                        // Prevent selecting past years
                        return year >= LocalDate.now().year
                    }
                }
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Date(millis)
                            val format = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                            targetDate = format.format(date)
                        }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

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
                        Text("New PUNDAR Paluwagan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Secured by Soroban smart contract", style = MaterialTheme.typography.bodySmall, color = PundarTextSecondary)
                    }
                }
            }

            item { HorizontalDivider(color = PundarDivider) }

            // Fields
            item {
                OutlinedTextField(
                    value = circleName,
                    onValueChange = { circleName = it },
                    label = { Text("Paluwagan Name") },
                    placeholder = { Text("e.g. Family Dream House") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PundarBlue, focusedLabelColor = PundarBlue),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { targetAmount = it },
                    label = { Text("Target Amount (₱)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PundarBlue, focusedLabelColor = PundarBlue),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = monthlyContribution,
                    onValueChange = { monthlyContribution = it },
                    label = { Text("Monthly Contribution per Member (₱)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PundarBlue, focusedLabelColor = PundarBlue),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = targetDate,
                            onValueChange = { },
                            label = { Text("Target Date") },
                            placeholder = { Text("Pick a date") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PundarBlue,
                                focusedLabelColor = PundarBlue,
                                disabledBorderColor = PundarBorder,
                                disabledLabelColor = PundarTextSecondary,
                                disabledTextColor = PundarTextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            enabled = false,
                            readOnly = true
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showDatePicker = true }
                        )
                    }
                    OutlinedTextField(
                        value = maxMembers,
                        onValueChange = { maxMembers = it },
                        label = { Text("Max Members") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PundarBlue, focusedLabelColor = PundarBlue),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // Invite Members Section
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Invite Members (${selectedMembers.size} / $maxMembersInt)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isAtCapacity) "Paluwagan Full" else "$remainingSlots slots left",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isAtCapacity) MaterialTheme.colorScheme.error else PundarBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Name, @username, or phone", color = PundarTextSecondary) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = PundarTextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PundarSurfaceVariant,
                        unfocusedContainerColor = PundarSurfaceVariant,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
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
                    color = PundarSurface
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
                                color = if (member.isYou) PundarBlue else Color.White,
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
                                Icon(Icons.Filled.Close, "Remove", tint = PundarTextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Search Results
            if (isSearching) {
                item {
                    Text("Searching...", style = MaterialTheme.typography.bodySmall, color = PundarTextSecondary)
                }
            } else if (searchResults.isNotEmpty()) {
                item {
                    Text(
                        text = "Search Results",
                        style = MaterialTheme.typography.labelMedium,
                        color = PundarTextSecondary
                    )
                }
                items(searchResults) { contact ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !isAtCapacity) {
                            if (!isAtCapacity) {
                                selectedMembers.add(contact)
                                searchQuery = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = PundarSurface
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
                                    color = PundarTextPrimary,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(contact.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(Icons.Filled.Add, "Add", tint = PundarBlue)
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
                        color = PundarTextSecondary
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}