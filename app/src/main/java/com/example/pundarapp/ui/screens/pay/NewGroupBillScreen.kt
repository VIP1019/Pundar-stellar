package com.example.pundarapp.ui.screens.pay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.BillMember
import com.example.pundarapp.ui.data.SampleData
import com.example.pundarapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupBillScreen(navController: NavController) {
    var totalAmount by remember { mutableStateOf("2450.00") }
    var expenseName by remember { mutableStateOf("Samgyupsal at BGC") }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    
    // We mock the selected members based on the design mockup
    val selectedMembers = remember {
        mutableStateListOf(
            SampleData.recentBills[0].members[0], // Miguel
            SampleData.recentBills[0].members[1], // Ana
            SampleData.recentBills[0].members[2]  // You
        )
    }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "New Group Bill",
                onBack = { navController.navigateUp() },
                showMoreOptions = true
            )
        },
        containerColor = PundarBackground,
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(PundarSurface)
                    .padding(16.dp)
            ) {
                // PUNDAR Score note
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
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = null,
                            tint = PundarBlue,
                            modifier = Modifier.size(16.dp)
                        )
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
                    onClick = { navController.navigateUp() }
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
            // ── Input Card ───────────────────────────────────────
            item {
                PundarAccentCard {
                    Text(
                        text = "Total Amount (PHP)",
                        style = MaterialTheme.typography.labelMedium,
                        color = PundarTextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = totalAmount,
                        onValueChange = { totalAmount = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        leadingIcon = {
                            Text(
                                "₱",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = PundarBlue
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PundarBorder,
                            unfocusedBorderColor = PundarBorder,
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Expense Name",
                        style = MaterialTheme.typography.labelMedium,
                        color = PundarTextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = expenseName,
                        onValueChange = { expenseName = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PundarBorder,
                            unfocusedBorderColor = PundarBorder,
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // ── Tabs ─────────────────────────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PundarSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(4.dp)
                    ) {
                        TabButton(
                            text = "Equal Split",
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        TabButton(
                            text = "Itemized",
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Split With ───────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Split With (${selectedMembers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PundarTextPrimary
                    )
                    TextButton(onClick = { }) {
                        Text("Select All", color = PundarBlue)
                    }
                }
                
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Name, @username, or phone", color = PundarTextSecondary) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = PundarTextSecondary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PundarSurfaceVariant,
                        unfocusedContainerColor = PundarSurfaceVariant,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
            }

            // ── Selected Members List ────────────────────────────
            items(selectedMembers) { member ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PundarBorder),
                    color = PundarSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (member.name == "You") PundarBlueSubtle else Color(member.avatarColor))
                        ) {
                            Text(
                                text = member.initials,
                                color = if (member.name == "You") PundarTextPrimary else Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (member.username.isNotEmpty()) {
                                Text(
                                    text = member.username,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PundarTextSecondary
                                )
                            }
                        }

                        Text(
                            text = "₱${String.format("%,.2f", member.amount)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        if (member.name != "You") {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove",
                                tint = PundarTextSecondary,
                                modifier = Modifier.size(20.dp).clickable { }
                            )
                        }
                    }
                }
            }

            // ── Recent Contacts ──────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Recent",
                    style = MaterialTheme.typography.labelMedium,
                    color = PundarTextSecondary
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SampleData.recentContacts.forEach { contact ->
                        RecentContactItem(contact)
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) PundarSurface else Color.Transparent,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) PundarBlue else PundarTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        )
    }
}

@Composable
private fun RecentContactItem(contact: BillMember) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color(contact.avatarColor).copy(alpha = 0.2f))
        ) {
            Text(
                text = contact.initials,
                color = PundarTextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = contact.name,
            style = MaterialTheme.typography.bodySmall,
            color = PundarTextPrimary,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}
