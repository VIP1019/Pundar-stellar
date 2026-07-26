package com.example.pundarapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.PundarTheme

@Composable
fun PundarSidebar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()
    val userState = AuthRepository.currentUserState.value
    
    val totalSaved = AppState.portfolio.value.totalValue
    val pundarScore = AppState.pundarScore.intValue

    ModalDrawerSheet(
        drawerContainerColor = PundarTheme.colors.bgPrimary,
        drawerContentColor = PundarTheme.colors.textPrimary,
        modifier = Modifier.width(320.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Profile Header with Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                PundarTheme.colors.brandPrimary.copy(alpha = 0.8f),
                                PundarTheme.colors.bgPrimary
                            )
                        )
                    )
                    .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(PundarTheme.colors.surfacePrimary)
                                .border(2.dp, PundarTheme.colors.brandPrimary, CircleShape)
                        ) {
                            Text(
                                text = userState?.name?.take(1)?.uppercase() ?: "U",
                                color = PundarTheme.colors.brandPrimary,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = PundarTheme.colors.textPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = userState?.name ?: "Pundar User",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PundarTheme.colors.textPrimary
                    )
                    Text(
                        text = userState?.phone ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PundarTheme.colors.textSecondary
                    )
                }
            }

            // User Stats (Total Saved & Credit Score)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Saved
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = PundarTheme.colors.surfaceSecondary,
                    border = borderStroke(PundarTheme.colors.glassMedium)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = PundarTheme.colors.brandPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Total Saved", style = MaterialTheme.typography.labelSmall, color = PundarTheme.colors.textMuted)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "₱${String.format("%,.2f", totalSaved)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PundarTheme.colors.textPrimary
                        )
                    }
                }
                
                // Credit / Pundar Score
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = PundarTheme.colors.surfaceSecondary,
                    border = borderStroke(PundarTheme.colors.glassMedium)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, null, tint = Color(0xFFFFB800), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Credit Score", style = MaterialTheme.typography.labelSmall, color = PundarTheme.colors.textMuted)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$pundarScore",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB800)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Menu
            SidebarSectionTitle("Main Menu")
            SidebarItem(icon = Icons.Filled.Home, label = "Home", isSelected = currentRoute == Routes.HOME, onClick = { onNavigate(Routes.HOME) })
            SidebarItem(icon = Icons.Filled.AccountBalanceWallet, label = "Wallet & Pay", isSelected = currentRoute == Routes.PAY, onClick = { onNavigate(Routes.PAY) })
            SidebarItem(icon = Icons.Filled.Groups, label = "Savings Circles", isSelected = currentRoute == Routes.CIRCLE, onClick = { onNavigate(Routes.CIRCLE) })
            SidebarItem(icon = Icons.AutoMirrored.Filled.TrendingUp, label = "Grow Wealth", isSelected = currentRoute == Routes.GROW, onClick = { onNavigate(Routes.GROW) })
            SidebarItem(icon = Icons.Filled.History, label = "Activity History", isSelected = currentRoute == Routes.ALL_ACTIVITY, onClick = { onNavigate(Routes.ALL_ACTIVITY) })
            SidebarItem(icon = Icons.Filled.ShowChart, label = "Pundar Score", isSelected = currentRoute == Routes.SCORE, onClick = { onNavigate(Routes.SCORE) })

            HorizontalDivider(color = PundarTheme.colors.glassMedium, modifier = Modifier.padding(vertical = 12.dp))

            // Quick Actions
            SidebarSectionTitle("Quick Actions")
            SidebarItem(icon = Icons.Filled.AddCard, label = "Cash In", isSelected = currentRoute == Routes.CASH_IN, onClick = { onNavigate(Routes.CASH_IN) })
            SidebarItem(icon = Icons.AutoMirrored.Filled.Send, label = "Send Money", isSelected = currentRoute == Routes.SEND_MONEY, onClick = { onNavigate(Routes.SEND_MONEY) })
            SidebarItem(icon = Icons.Filled.QrCode, label = "Receive Money", isSelected = currentRoute == Routes.RECEIVE_MONEY, onClick = { onNavigate(Routes.RECEIVE_MONEY) })
            SidebarItem(icon = Icons.Filled.QrCodeScanner, label = "Scan QR", isSelected = currentRoute == Routes.SCAN, onClick = { onNavigate(Routes.SCAN) })
            SidebarItem(icon = Icons.Filled.PhoneAndroid, label = "Buy Load", isSelected = currentRoute == Routes.BUY_LOAD, onClick = { onNavigate(Routes.BUY_LOAD) })

            HorizontalDivider(color = PundarTheme.colors.glassMedium, modifier = Modifier.padding(vertical = 12.dp))

            // System
            SidebarSectionTitle("System")
            SidebarItem(icon = Icons.Filled.Settings, label = "Settings", isSelected = currentRoute == Routes.SETTINGS, onClick = { onNavigate(Routes.SETTINGS) })
            SidebarItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                label = "Log Out",
                isSelected = false,
                onClick = {
                    AuthRepository.logout()
                    onNavigate(Routes.LOGIN)
                },
                isDestructive = true
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "PUNDAR v1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = PundarTheme.colors.textMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun borderStroke(color: Color) = androidx.compose.foundation.BorderStroke(1.dp, color)

@Composable
private fun SidebarSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = PundarTheme.colors.textMuted,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun SidebarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val bgColor = if (isSelected) PundarTheme.colors.brandPrimary.copy(alpha = 0.1f) else Color.Transparent
    val contentColor = when {
        isDestructive -> PundarTheme.colors.accentRed
        isSelected -> PundarTheme.colors.brandPrimary
        else -> PundarTheme.colors.textPrimary
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
    }
}
