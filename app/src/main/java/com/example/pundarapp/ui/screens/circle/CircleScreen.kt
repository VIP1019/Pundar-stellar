package com.example.pundarapp.ui.screens.circle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.SampleData
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.data.remote.AuthRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleScreen(navController: NavController) {
    val currentName = AuthRepository.getCurrentUserName()
    val initials = AuthRepository.getCurrentUserInitials()

    Scaffold(
        topBar = {
            PundarMainTopBar(
                userName = currentName,
                userInitials = initials,
                pundarScore = SampleData.currentUser.pundarScore,
                onNotificationClick = { navController.navigate(Routes.NOTIFICATIONS) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
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
                Text(
                    "Create Circle",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = PundarBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Circle",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = PundarTextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Safe",
                            tint = PundarGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Probably Safe Paluwagan",
                            style = MaterialTheme.typography.bodyLarge,
                            color = PundarTextSecondary
                        )
                    }
                }
            }

            // Invitations
            val invitation = AppState.pendingInvitation.value
            if (invitation != null) {
                item {
                    PundarAccentCard(
                        modifier = Modifier.clickable { 
                            navController.navigate(Routes.circleInvite(invitation.id))
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "1 Pending Invitation",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PundarTextPrimary
                                )
                                Text(
                                    text = invitation.circleName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PundarTextSecondary
                                )
                            }
                            PundarSmallButton(
                                text = "View",
                                onClick = { navController.navigate(Routes.circleInvite(invitation.id)) },
                                containerColor = PundarGoldLight,
                                contentColor = PundarTextPrimary
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Active Circles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PundarTextPrimary
                )
            }

            items(AppState.circles.toList()) { circle ->
                PundarAccentCard(
                    modifier = Modifier.clickable { 
                        navController.navigate(Routes.circleDetail(circle.id))
                    }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header section with status and members
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(
                                    text = if (circle.isActive) "Active" else "Completed",
                                    color = if (circle.isActive) PundarBlue else PundarSuccess
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "${circle.memberCount} Members",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PundarTextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Target",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PundarTextSecondary
                                )
                                Text(
                                    text = "₱${String.format("%,.0f", circle.targetAmount)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PundarBlue
                                )
                            }
                        }

                        // Circle name
                        Text(
                            text = circle.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Progress section
                        val progress = (circle.savedAmount / circle.targetAmount).toFloat()
                        val progressPercent = (progress * 100).toInt()

                        PundarProgressBar(
                            progress = progress,
                            color = PundarBlue,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$progressPercent% Completed",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PundarTextSecondary
                            )
                            Text(
                                text = "₱${String.format("%,.0f", circle.targetAmount - circle.savedAmount)} remaining",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PundarTextSecondary
                            )
                        }

                        // Total saved
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PundarGoldLight, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Saved",
                                style = MaterialTheme.typography.labelMedium,
                                color = PundarTextSecondary
                            )
                            Text(
                                text = "₱${String.format("%,.0f", circle.savedAmount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = PundarGoldDark
                            )
                        }
                    }
                }
            }
            
            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}
