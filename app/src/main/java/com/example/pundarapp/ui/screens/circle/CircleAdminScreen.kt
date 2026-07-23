package com.example.pundarapp.ui.screens.circle

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.data.*
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleAdminScreen(circleId: String, navController: NavController) {
    val circle = AppState.circles.find { it.id == circleId }
        ?: run { navController.navigateUp(); return }

    val currentUserId = AuthRepository.getCurrentUserId()
    if (circle.creatorId != currentUserId) {
        navController.navigateUp()
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Rules state
    var maxMembersInput by remember { mutableStateOf(circle.maxMembers.toString()) }
    var dueDayInput by remember { mutableStateOf(circle.monthlyDueDay.toString()) }
    var penaltyInput by remember { mutableStateOf(circle.penaltyAmount.toString()) }
    var penaltyEnabled by remember { mutableStateOf(circle.penaltyEnabled) }

    // Dialog state
    var showStartDialog by remember { mutableStateOf(false) }
    var showEndDialog by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<CircleMember?>(null) }

    // ── Start Cycle Dialog ────────────────────────────────────────
    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = { Text("Start Savings Cycle?", fontWeight = FontWeight.Bold, color = PundarTheme.colors.textPrimary) },
            text = {
                Text(
                    "Starting the cycle will lock the group rules and begin tracking contributions. Members will be notified.",
                    color = PundarTheme.colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idx = AppState.circles.indexOfFirst { it.id == circleId }
                        if (idx >= 0) AppState.circles[idx] = AppState.circles[idx].copy(cycleStatus = CycleStatus.ACTIVE)
                        showStartDialog = false
                        Toast.makeText(context, "Cycle started!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = PundarTheme.colors.bgSecondary),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Start Cycle", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) { Text("Cancel", color = PundarTheme.colors.textMuted) }
            },
            containerColor = PundarTheme.colors.surfacePrimary
        )
    }

    // ── End Cycle Dialog ──────────────────────────────────────────
    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("End Savings Cycle?", fontWeight = FontWeight.Bold, color = PundarTheme.colors.textPrimary) },
            text = {
                Text(
                    "Ending the cycle will mark this Paluwagan as completed. This cannot be undone.",
                    color = PundarTheme.colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idx = AppState.circles.indexOfFirst { it.id == circleId }
                        if (idx >= 0) AppState.circles[idx] = AppState.circles[idx].copy(cycleStatus = CycleStatus.COMPLETED)
                        showEndDialog = false
                        Toast.makeText(context, "Cycle ended.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("End Cycle", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) { Text("Cancel", color = PundarTheme.colors.textMuted) }
            },
            containerColor = PundarTheme.colors.surfacePrimary
        )
    }

    // ── Remove Member Dialog ──────────────────────────────────────
    memberToRemove?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text("Remove Member?", fontWeight = FontWeight.Bold, color = PundarTheme.colors.textPrimary) },
            text = { Text("Remove ${member.name} from this Paluwagan group?", color = PundarTheme.colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        val idx = AppState.circles.indexOfFirst { it.id == circleId }
                        if (idx >= 0) {
                            val c = AppState.circles[idx]
                            val newMembers = c.members.filter { it.userId != member.userId }
                            AppState.circles[idx] = c.copy(
                                members = newMembers,
                                memberCount = newMembers.size
                            )
                        }
                        memberToRemove = null
                        Toast.makeText(context, "${member.name} removed.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Remove", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemove = null }) { Text("Cancel", color = PundarTheme.colors.textMuted) }
            },
            containerColor = PundarTheme.colors.surfacePrimary
        )
    }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Paluwagan Admin",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarTheme.colors.bgPrimary
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Section 1: Group Status ───────────────────────────
            item {
                AdminCard {
                    Text(
                        "Group Status",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = PundarTheme.colors.textPrimary
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(circle.name, fontWeight = FontWeight.SemiBold, color = PundarTheme.colors.textPrimary)
                            Spacer(Modifier.height(6.dp))
                            CycleStatusBadge(circle.cycleStatus)
                        }
                        Icon(
                            Icons.Filled.AdminPanelSettings,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showStartDialog = true },
                            enabled = circle.cycleStatus == CycleStatus.NOT_STARTED || circle.cycleStatus == CycleStatus.PAUSED,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(0.5f))
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Start Cycle", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = { showEndDialog = true },
                            enabled = circle.cycleStatus == CycleStatus.ACTIVE,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(0.5f))
                        ) {
                            Icon(Icons.Filled.Stop, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("End Cycle", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Section 2: Group Rules ────────────────────────────
            item {
                AdminCard {
                    Text("Group Rules", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PundarTheme.colors.textPrimary)
                    Spacer(Modifier.height(14.dp))

                    AdminTextField(
                        value = maxMembersInput,
                        onValueChange = { if (it.length <= 2) maxMembersInput = it },
                        label = "Max Members",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(Modifier.height(10.dp))
                    AdminTextField(
                        value = dueDayInput,
                        onValueChange = { if (it.length <= 2) dueDayInput = it },
                        label = "Monthly Due Day (1–28)",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enable Penalty", color = PundarTheme.colors.textSecondary, modifier = Modifier.weight(1f))
                        Switch(
                            checked = penaltyEnabled,
                            onCheckedChange = { penaltyEnabled = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = NeonGreen)
                        )
                    }
                    if (penaltyEnabled) {
                        Spacer(Modifier.height(8.dp))
                        AdminTextField(
                            value = penaltyInput,
                            onValueChange = { penaltyInput = it },
                            label = "Penalty Amount (₱)",
                            keyboardType = KeyboardType.Decimal
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val newMax = maxMembersInput.toIntOrNull()?.coerceIn(2, 50) ?: circle.maxMembers
                            val newDay = dueDayInput.toIntOrNull()?.coerceIn(1, 28) ?: circle.monthlyDueDay
                            val newPenalty = penaltyInput.toDoubleOrNull() ?: circle.penaltyAmount
                            val idx = AppState.circles.indexOfFirst { it.id == circleId }
                            if (idx >= 0) {
                                AppState.circles[idx] = AppState.circles[idx].copy(
                                    maxMembers = newMax,
                                    monthlyDueDay = newDay,
                                    penaltyAmount = newPenalty,
                                    penaltyEnabled = penaltyEnabled
                                )
                            }
                            Toast.makeText(context, "Rules saved!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PundarTheme.colors.brandPrimary)
                    ) {
                        Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save Rules", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Section 3: Members ────────────────────────────────
            item {
                AdminCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Members (${circle.memberCount}/${circle.maxMembers})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PundarTheme.colors.textPrimary
                        )
                        OutlinedButton(
                            onClick = { navController.navigate("circle/$circleId/invite-methods") },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(0.5f))
                        ) {
                            Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Invite", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    circle.members.forEach { member ->
                        AdminMemberRow(
                            member = member,
                            onRemove = { memberToRemove = member }
                        )
                        HorizontalDivider(
                            color = PundarTheme.colors.borderPrimary,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }

            // ── Section 4: Pending Join Requests ─────────────────
            if (AppState.pendingJoinRequests.isNotEmpty()) {
                item {
                    AdminCard {
                        Text(
                            "Pending Join Requests",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PundarTheme.colors.textPrimary
                        )
                        Spacer(Modifier.height(12.dp))
                        AppState.pendingJoinRequests.toList().forEach { request ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(request.userName, fontWeight = FontWeight.SemiBold, color = PundarTheme.colors.textPrimary)
                                    Text("Wants to join", fontSize = 12.sp, color = PundarTheme.colors.textMuted)
                                }
                                TextButton(onClick = {
                                    scope.launch { AppState.approveJoinRequest(request.id) }
                                }) { Text("Approve", color = NeonGreen, fontWeight = FontWeight.SemiBold) }
                                TextButton(onClick = {
                                    scope.launch { AppState.rejectJoinRequest(request.id) }
                                }) { Text("Reject", color = ErrorRed) }
                            }
                            HorizontalDivider(color = PundarTheme.colors.borderPrimary, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }

            // ── Section 5: Contributions Overview ────────────────
            item {
                AdminCard {
                    Text(
                        "Contributions This Cycle",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = PundarTheme.colors.textPrimary
                    )
                    Spacer(Modifier.height(12.dp))
                    circle.members.forEach { member ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(32.dp).clip(CircleShape)
                                    .background(Color(member.avatarColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(member.initials, color = PundarTheme.colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(member.name, color = PundarTheme.colors.textSecondary, modifier = Modifier.weight(1f))
                            Text(
                                "₱${String.format("%,.0f", member.amount)}",
                                color = PundarTheme.colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(8.dp))
                            ContributionChip(member.status)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Reusable card wrapper ─────────────────────────────────────────
@Composable
private fun AdminCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(PundarTheme.colors.surfacePrimary, PundarTheme.colors.surfaceSecondary)))
            .border(1.dp, PundarTheme.colors.borderPrimary, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}

// ── Text field styled for admin ───────────────────────────────────
@Composable
private fun AdminTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = PundarTheme.colors.textMuted) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Blue400,
            unfocusedBorderColor = PundarTheme.colors.borderPrimary,
            focusedContainerColor = PundarTheme.colors.surfaceSecondary,
            unfocusedContainerColor = PundarTheme.colors.surfaceSecondary,
            focusedTextColor = PundarTheme.colors.textPrimary,
            unfocusedTextColor = PundarTheme.colors.textPrimary,
            cursorColor = Blue400
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

// ── Cycle status badge ────────────────────────────────────────────
@Composable
private fun CycleStatusBadge(status: CycleStatus) {
    val (bg, textColor, label) = when (status) {
        CycleStatus.NOT_STARTED -> Triple(PundarTheme.colors.textMuted.copy(0.15f), PundarTheme.colors.textMuted, "Not Started")
        CycleStatus.ACTIVE      -> Triple(NeonGreen.copy(0.15f), NeonGreen, "Active")
        CycleStatus.PAUSED      -> Triple(PundarTheme.colors.accentOrange.copy(0.15f), PundarTheme.colors.accentOrange, "Paused")
        CycleStatus.COMPLETED   -> Triple(Blue400.copy(0.15f), Blue400, "Completed")
    }
    Box(
        Modifier.clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, textColor.copy(0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Member row in admin panel ─────────────────────────────────────
@Composable
private fun AdminMemberRow(member: CircleMember, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(Color(member.avatarColor)),
            contentAlignment = Alignment.Center
        ) {
            Text(member.initials, color = PundarTheme.colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(member.name, color = PundarTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (member.isYou) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(4.dp))
                            .background(PundarTheme.colors.brandPrimary.copy(0.2f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) { Text("YOU", color = Blue400, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold) }
                }
            }
            ContributionChip(member.status)
        }
        if (!member.isYou) {
            TextButton(
                onClick = onRemove,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Filled.PersonRemove, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
                Text("Remove", color = ErrorRed, fontSize = 12.sp)
            }
        }
    }
}

// ── Contribution status chip ──────────────────────────────────────
@Composable
private fun ContributionChip(status: ContributionStatus) {
    val (bg, textColor, label) = when (status) {
        ContributionStatus.PAID    -> Triple(NeonGreen.copy(0.15f), NeonGreen, "Paid")
        ContributionStatus.PENDING -> Triple(PundarTheme.colors.accentOrange.copy(0.15f), PundarTheme.colors.accentOrange, "Pending")
        ContributionStatus.OVERDUE -> Triple(ErrorRed.copy(0.15f), ErrorRed, "Overdue")
    }
    Box(
        Modifier.clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(label, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
