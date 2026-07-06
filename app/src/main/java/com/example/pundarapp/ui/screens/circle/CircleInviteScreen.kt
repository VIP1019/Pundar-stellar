package com.example.pundarapp.ui.screens.circle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.SampleData
import com.example.pundarapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleInviteScreen(inviteId: String, navController: NavController) {
    val invite = SampleData.circleInvitation

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "PUNDAR", // Shows Pundar logo conceptually
                onBack = { navController.navigateUp() },
                showHelp = true
            )
        },
        containerColor = PundarBackground,
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(PundarSurface)
                    .padding(16.dp)
            ) {
                PundarBlueButton(
                    text = "Join Circle →",
                    onClick = { navController.navigateUp() }
                )
                Spacer(Modifier.height(12.dp))
                PundarSecondaryButton(
                    text = "View Detailed Schedule",
                    onClick = { }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ───────────────────────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PundarBlueSubtle
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Payments, contentDescription = null, tint = PundarBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Invitation Received",
                            style = MaterialTheme.typography.labelMedium,
                            color = PundarBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = "You're Invited to Join:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PundarTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = invite.circleName,
                    style = MaterialTheme.typography.titleLarge,
                    color = PundarBlue,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Invited by ${invite.inviterName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PundarTextSecondary
                )
            }

            // ── Illustration/Goal Card ───────────────────────────
            item {
                PundarCard {
                    // Placeholder for illustration
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PundarSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pundar: Savings Circle Illustration",
                            color = PundarTextTertiary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(PundarGold)
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "GOAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = PundarTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = invite.goal,
                                style = MaterialTheme.typography.titleMedium,
                                color = PundarTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ── Highlights ───────────────────────────────────────
            item {
                Text(
                    text = "Circle Highlights",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PundarTextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                PundarAccentCard {
                    Text(
                        text = "Total Target Amount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PundarTextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "₱${String.format("%,.0f", invite.targetAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = PundarBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    PundarProgressBar(progress = invite.fundedPercent / 100f, color = PundarBlue, height = 6)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${invite.fundedPercent}% funded",
                        style = MaterialTheme.typography.bodySmall,
                        color = PundarTextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PundarCard(modifier = Modifier.weight(1f)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PundarBlue)
                        ) {
                            Icon(Icons.Filled.Payments, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Monthly Contrib.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PundarTextSecondary
                        )
                        Text(
                            text = "₱${String.format("%,.0f", invite.monthlyContribution)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    PundarCard(modifier = Modifier.weight(1f)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PundarBlueSubtle)
                        ) {
                            Icon(Icons.Filled.Group, contentDescription = null, tint = PundarBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Members",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PundarTextSecondary
                        )
                        Text(
                            text = "${invite.memberCount} / ${invite.maxMembers}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${invite.maxMembers - invite.memberCount} spots left",
                            style = MaterialTheme.typography.bodySmall,
                            color = PundarBlue
                        )
                    }
                }
            }

            // ── Trust & Security ─────────────────────────────────
            item {
                Text(
                    text = "Trust & Security",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PundarTextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                PundarCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(invite.inviterAvatarColor))
                        ) {
                            Text(
                                text = invite.inviterInitials,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = invite.inviterName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = PundarGold
                                ) {
                                    Text(
                                        text = "ORGANIZER",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = PundarBlue, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "PUNDAR Score: ${invite.inviterScore} (Excellent)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PundarBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Successfully completed ${invite.inviterCirclesCompleted} circles.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PundarTextSecondary
                            )
                        }
                    }
                }
            }
            
            item {
                PundarCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PundarSurfaceVariant
                        ) {
                            Icon(
                                Icons.Filled.Security,
                                contentDescription = null,
                                tint = PundarTextPrimary,
                                modifier = Modifier.padding(12.dp).size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Soroban Smart Contract",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Funds are securely locked and automatically distributed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PundarTextSecondary
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
