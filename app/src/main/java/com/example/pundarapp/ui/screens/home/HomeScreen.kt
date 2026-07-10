package com.example.pundarapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.SampleData
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.remote.HomeRepository
import com.example.pundarapp.ui.data.HomeActivity
import com.example.pundarapp.ui.data.AppState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val user = SampleData.currentUser
    val context = LocalContext.current
    
    var userName by remember { mutableStateOf("User") }
    var isLoadingActivities by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        userName = AuthRepository.getCurrentUserName().split(" ").first()
        val userId = AuthRepository.getCurrentUserId()
        if (userId != null && AppState.homeActivities.isEmpty()) {
            val result = HomeRepository.getRecentActivities(userId)
            if (result.isSuccess) {
                AppState.homeActivities.addAll(result.getOrDefault(emptyList()))
            }
        }
        isLoadingActivities = false
    }
    
    val totalSaved = AppState.circles.sumOf { it.savedAmount }

    Scaffold(
        topBar = {
            PundarMainTopBar(
                userName = userName,
                userInitials = user.initials,
                pundarScore = user.pundarScore,
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        },
        containerColor = PundarBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Welcome Banner ─────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(PundarBlue, PundarBlueDark)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = "Welcome, $userName! 👋",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "\"Every Peso Keeps Building.\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                HomeStatItem("Wallet Balance", "₱ ${String.format("%,.0f", AppState.walletBalance.value)}", Color.White)
                                HomeStatItem("Total Saved", "₱ ${String.format("%,.0f", totalSaved)}", Color.White)
                                HomeStatItem("Score", "${user.pundarScore}", PundarGold)
                            }
                        }
                    }
                }
            }

            // ── Quick Actions ──────────────────────────────────
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PundarTextPrimary
                )
                Spacer(Modifier.height(12.dp))
                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Filled.Send,
                        title = "Send Money",
                        subtitle = "PUNDAR Wallet",
                        color = PundarBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Routes.SEND_MONEY) }
                    )
                    QuickActionCard(
                        icon = Icons.Filled.PhoneAndroid,
                        title = "Buy Load",
                        subtitle = "PUNDAR Wallet",
                        color = PundarGoldDark,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Routes.BUY_LOAD) }
                    )
                }
                Spacer(Modifier.height(12.dp))
                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Filled.Receipt,
                        title = "Split a Bill",
                        subtitle = "PUNDAR Pay",
                        color = PundarWarning,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Routes.PAY_NEW_BILL) }
                    )
                    QuickActionCard(
                        icon = Icons.Filled.Groups,
                        title = "Join Circle",
                        subtitle = "PUNDAR Circle",
                        color = PundarSuccess,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Routes.CIRCLE) }
                    )
                }
            }

            // ── Financial Progress ─────────────────────────────
            item {
                Text(
                    text = "Your Financial Journey",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PundarTextPrimary
                )
            }

            item {
                PundarCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        JourneyStep("Spend", "✅", PundarSuccess, true)
                        JourneyArrow()
                        JourneyStep("Save", "✅", PundarSuccess, true)
                        JourneyArrow()
                        JourneyStep("Grow", "🔄", PundarBlue, true)
                        JourneyArrow()
                        JourneyStep("Score", "⭐", PundarGold, true)
                        JourneyArrow()
                        JourneyStep("Access", "🔒", PundarTextSecondary, false)
                    }
                }
            }

            // ── Recent Activity ────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PundarTextPrimary
                    )
                    TextButton(onClick = { Toast.makeText(context, "Activity history coming soon!", Toast.LENGTH_SHORT).show() }) {
                        Text(
                            text = "View All",
                            color = PundarBlue,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            if (isLoadingActivities) {
                item {
                    Text(
                        text = "Loading activities...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PundarTextSecondary,
                        modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (AppState.homeActivities.isEmpty()) {
                item {
                    Text(
                        text = "No recent activity yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PundarTextSecondary,
                        modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(AppState.homeActivities) { activity ->
                    PundarCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (activity.module) {
                                            "Pay" -> PundarBlueSubtle
                                            "Circle" -> PundarGoldLight
                                            "Grow" -> PundarSuccessLight
                                            "Wallet" -> PundarInfoLight
                                            else -> PundarSurfaceVariant
                                        }
                                    )
                            ) {
                                Icon(
                                    imageVector = when (activity.module) {
                                        "Pay" -> Icons.Filled.Receipt
                                        "Circle" -> Icons.Filled.Groups
                                        "Grow" -> Icons.AutoMirrored.Filled.TrendingUp
                                        "Wallet" -> Icons.Filled.AccountBalanceWallet
                                        else -> Icons.Filled.Info
                                    },
                                    contentDescription = null,
                                    tint = when (activity.module) {
                                        "Pay" -> PundarBlue
                                        "Circle" -> PundarGoldDark
                                        "Grow" -> PundarSuccess
                                        "Wallet" -> PundarInfo
                                        else -> PundarTextSecondary
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activity.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = activity.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PundarTextSecondary
                                )
                            }

                            Text(
                                text = activity.amount,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (activity.isPositive) PundarSuccess else PundarTextPrimary
                            )
                        }
                    }
                }
            }

            // ── Bottom Tagline ─────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Spend Together → Save Together → Grow Together",
                    style = MaterialTheme.typography.bodySmall,
                    color = PundarTextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun HomeStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PundarSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f))
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = PundarTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun JourneyStep(label: String, emoji: String, color: Color, isComplete: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isComplete) FontWeight.Bold else FontWeight.Normal,
            color = if (isComplete) color else PundarTextTertiary
        )
    }
}

@Composable
private fun JourneyArrow() {
    Text(
        text = "→",
        style = MaterialTheme.typography.bodySmall,
        color = PundarTextTertiary,
        modifier = Modifier.padding(top = 4.dp)
    )
}
