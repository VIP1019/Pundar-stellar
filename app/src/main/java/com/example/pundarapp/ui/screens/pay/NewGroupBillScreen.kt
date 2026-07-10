package com.example.pundarapp.ui.screens.pay

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.*
import com.example.pundarapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupBillScreen(navController: NavController) {
    var totalAmount by remember { mutableStateOf("") }
    var expenseName by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    // Start with "You" already in the list
    val selectedMembers = remember {
        mutableStateListOf(
            BillMember("You", "", "You", 0.0)
        )
    }

    // Itemized amounts
    val itemizedAmounts = remember { mutableStateMapOf<String, String>() }

    // Dynamically compute per-member share or total
    val total = if (selectedTabIndex == 0) {
        totalAmount.toDoubleOrNull() ?: 0.0
    } else {
        itemizedAmounts.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
    }
    
    val sharePerMember = if (selectedTabIndex == 0 && selectedMembers.isNotEmpty() && total > 0)
        total / selectedMembers.size else 0.0

    // Available contacts to add (search results)
    var searchResults by remember { mutableStateOf<List<BillMember>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Dynamic search effect
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }
        
        isSearching = true
        val users = AuthRepository.searchUsers(searchQuery)
        searchResults = users.map { u ->
            BillMember(
                name = u.name,
                username = u.phone, // using phone as username display for now
                initials = u.name.take(2).uppercase(),
                amount = 0.0,
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
                title = "New Group Bill",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarBackground,
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(PundarSurface)
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = PundarBlueSubtle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Verified, null, tint = PundarBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Settlement builds your PUNDAR Score.",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PundarBlueDark
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                PundarPrimaryButton(
                    text = "Create & Request ▷",
                    enabled = expenseName.isNotBlank() && total > 0 && selectedMembers.size >= 2,
                    onClick = {
                        val newBill = GroupBill(
                            id = "bill_${System.currentTimeMillis()}",
                            name = expenseName,
                            totalAmount = total,
                            memberCount = selectedMembers.size,
                            status = BillStatus.PENDING,
                            date = "Today",
                            yourShare = if (selectedTabIndex == 0) sharePerMember else (itemizedAmounts["You"]?.toDoubleOrNull() ?: 0.0),
                            members = selectedMembers.map { m ->
                                if (selectedTabIndex == 0) {
                                    m.copy(amount = sharePerMember)
                                } else {
                                    m.copy(amount = itemizedAmounts[m.name]?.toDoubleOrNull() ?: 0.0)
                                }
                            }
                        )
                        AppState.addBill(newBill)
                        navController.navigateUp()
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
            // ── Amount + Name ────────────────────────────────────
            item {
                PundarAccentCard {
                    Text("Total Amount (PHP)", style = MaterialTheme.typography.labelMedium, color = PundarTextSecondary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = if (selectedTabIndex == 0) totalAmount else total.toString(),
                        onValueChange = { if (selectedTabIndex == 0) totalAmount = it },
                        readOnly = selectedTabIndex == 1,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        leadingIcon = {
                            Text("₱", style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = PundarBlue)
                        },
                        placeholder = { Text("0.00", style = MaterialTheme.typography.headlineMedium, color = PundarTextTertiary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PundarBlue,
                            unfocusedBorderColor = PundarBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Expense Name", style = MaterialTheme.typography.labelMedium, color = PundarTextSecondary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = expenseName,
                        onValueChange = { expenseName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Dinner at BGC", color = PundarTextTertiary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PundarBlue,
                            unfocusedBorderColor = PundarBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // ── Split Mode Tabs ──────────────────────────────────
            item {
                Surface(shape = RoundedCornerShape(12.dp), color = PundarSurfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                        listOf("Equal Split", "Itemized").forEachIndexed { index, label ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedTabIndex == index) PundarSurface else Color.Transparent,
                                modifier = Modifier.weight(1f).clickable { selectedTabIndex = index }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (selectedTabIndex == index) PundarBlue else PundarTextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Members Header ───────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Split With (${selectedMembers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedTabIndex == 0 && sharePerMember > 0) {
                        Text(
                            text = "₱ ${String.format("%,.2f", sharePerMember)} each",
                            style = MaterialTheme.typography.labelLarge,
                            color = PundarBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else if (selectedTabIndex == 1 && total > 0) {
                        Text(
                            text = "Total: ₱ ${String.format("%,.2f", total)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = PundarBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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

            // ── Selected Members ─────────────────────────────────
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
                                    if (member.name == "You") PundarBlueSubtle
                                    else Color(member.avatarColor)
                                )
                        ) {
                            Text(
                                text = member.initials,
                                color = if (member.name == "You") PundarBlue else Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            if (member.username.isNotEmpty()) {
                                Text(member.username, style = MaterialTheme.typography.bodySmall, color = PundarTextSecondary)
                            }
                        }
                        if (selectedTabIndex == 0) {
                            if (sharePerMember > 0) {
                                Text(
                                    text = "₱ ${String.format("%,.2f", sharePerMember)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = itemizedAmounts[member.name] ?: "",
                                onValueChange = { itemizedAmounts[member.name] = it },
                                modifier = Modifier.width(100.dp).height(52.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                placeholder = { Text("0.00") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PundarBlue,
                                    unfocusedBorderColor = PundarBorder,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        }
                        if (member.name != "You") {
                            Spacer(Modifier.width(8.dp))
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

            // ── Available Contacts to Add ────────────────────────
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
                        modifier = Modifier.fillMaxWidth().clickable { 
                            selectedMembers.add(contact) 
                            searchQuery = "" // Clear search after adding
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
                                if (contact.username.isNotEmpty()) {
                                    Text(contact.username, style = MaterialTheme.typography.bodySmall, color = PundarTextSecondary)
                                }
                            }
                            Icon(Icons.Filled.Add, "Add", tint = PundarBlue)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
