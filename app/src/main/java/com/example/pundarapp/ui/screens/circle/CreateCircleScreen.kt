package com.example.pundarapp.ui.screens.circle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.*
import com.example.pundarapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCircleScreen(navController: NavController) {
    var circleName by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var monthlyContribution by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf("") }
    var maxMembers by remember { mutableStateOf("10") }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Create a Circle",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarBackground,
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                PundarPrimaryButton(
                    text = "Launch Circle 🚀",
                    enabled = circleName.isNotBlank() && (targetAmount.toDoubleOrNull() ?: 0.0) > 0,
                    onClick = {
                        val newCircle = Circle(
                            id = "circle_${System.currentTimeMillis()}",
                            name = circleName,
                            targetAmount = targetAmount.toDoubleOrNull() ?: 0.0,
                            savedAmount = 0.0,
                            targetDate = targetDate.ifBlank { "TBD" },
                            memberCount = 1,
                            contributionPerMonth = monthlyContribution.toDoubleOrNull() ?: 0.0,
                            members = listOf(
                                CircleMember(
                                    name = SampleData.currentUser.name,
                                    initials = SampleData.currentUser.initials,
                                    sharePercent = 100,
                                    amount = 0.0,
                                    status = ContributionStatus.PENDING,
                                    isYou = true,
                                    avatarColor = 0xFF0052CC
                                )
                            ),
                            isActive = true
                        )
                        AppState.circles.add(0, newCircle)
                        navController.navigateUp()
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = PundarGoldLight, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Groups, null, tint = PundarGoldDark, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("New PUNDAR Circle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Secured by Soroban smart contract", style = MaterialTheme.typography.bodySmall, color = PundarTextSecondary)
                }
            }

            HorizontalDivider(color = PundarDivider)

            // Fields
            OutlinedTextField(
                value = circleName,
                onValueChange = { circleName = it },
                label = { Text("Circle Name") },
                placeholder = { Text("e.g. Family Dream House") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PundarBlue, focusedLabelColor = PundarBlue),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

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

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("Target Date") },
                    placeholder = { Text("e.g. Dec 2025") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PundarBlue, focusedLabelColor = PundarBlue),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
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
    }
}
