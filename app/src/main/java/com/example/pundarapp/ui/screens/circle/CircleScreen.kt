package com.example.pundarapp.ui.screens.circle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleScreen(navController: NavController) {
    val circles = AppState.circles
    val pendingInvitation by AppState.pendingInvitation

    var showNotifDialog by remember { mutableStateOf(false) }

    if (showNotifDialog) {
        AlertDialog(
            onDismissRequest = { showNotifDialog = false },
            title = { Text("Notifications", fontWeight = FontWeight.Bold) },
            text = {
                if (pendingInvitation != null) {
                    Text("📩 You have a pending circle invitation from ${pendingInvitation!!.inviterName}.")
                } else {
                    Text("✅ You're all caught up! No new notifications.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotifDialog = false }) { Text("OK", color = PundarBlue) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("PUNDAR Circle", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold, color = PundarTextPrimary)
                },
                actions = {
                    IconButton(onClick = { showNotifDialog = true }) {
                        BadgedBox(badge = {
                            if (pendingInvitation != null) Badge()
                        }) {
                            Icon(Icons.Filled.NotificationsNone, "Notifications", tint = PundarTextSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PundarSurface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Routes.CIRCLE_CREATE) },
                containerColor = PundarGold,
                contentColor = PundarTextPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Circle")
                Spacer(Modifier.width(8.dp))
                Text("Create Circle", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = PundarBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("PUNDAR Circle", style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold, color = PundarTextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Smart-contract-secured paluwagan savings, with no organizer risk",
                    style = MaterialTheme.typography.bodyMedium, color = PundarTextSecondary)
            }

            // Pending invitation — disappears after accept
            if (pendingInvitation != null) {
                item {
                    PundarAccentCard(
                        modifier = Modifier.clickable {
                            navController.navigate(Routes.circleInvite(pendingInvitation!!.id))
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("1 Pending Invitation", style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold, color = PundarTextPrimary)
                                Text(pendingInvitation!!.circleName,
                                    style = MaterialTheme.typography.bodySmall, color = PundarTextSecondary)
                            }
                            PundarSmallButton(
                                text = "View",
                                onClick = { navController.navigate(Routes.circleInvite(pendingInvitation!!.id)) },
                                containerColor = PundarGoldLight,
                                contentColor = PundarTextPrimary
                            )
                        }
                    }
                }
            }

            item {
                Text("Active Circles", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = PundarTextPrimary)
            }

            if (circles.isEmpty()) {
                item {
                    PundarCard {
                        Text("No circles yet. Create one or accept an invitation!",
                            style = MaterialTheme.typography.bodyMedium, color = PundarTextSecondary)
                    }
                }
            }

            items(circles.toList()) { circle ->
                PundarCard(modifier = Modifier.clickable {
                    navController.navigate(Routes.circleDetail(circle.id))
                }) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(
                                    text = if (circle.isActive) "Active" else "Completed",
                                    color = if (circle.isActive) PundarBlue else PundarSuccess
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("${circle.memberCount} Members",
                                    style = MaterialTheme.typography.bodySmall, color = PundarTextSecondary)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(circle.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Saved", style = MaterialTheme.typography.bodySmall, color = PundarTextSecondary)
                            Text("₱ ${String.format("%,.0f", circle.savedAmount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = PundarBlue)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    val progress = (circle.savedAmount / circle.targetAmount).toFloat().coerceIn(0f, 1f)
                    PundarProgressBar(progress = progress, color = PundarBlue)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${(progress * 100).toInt()}% Completed",
                            style = MaterialTheme.typography.labelMedium, color = PundarTextSecondary)
                        Text("₱ ${String.format("%,.0f", circle.targetAmount - circle.savedAmount)} remaining",
                            style = MaterialTheme.typography.labelMedium, color = PundarTextSecondary)
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
