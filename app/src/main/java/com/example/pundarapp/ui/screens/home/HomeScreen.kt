package com.example.pundarapp.ui.screens.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.R
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
        AppState.refreshWalletBalance()
        isLoadingActivities = false
    }

    val currentUserName = AuthRepository.getCurrentUserName().split(" ").first()
    val currentUserInitials = AuthRepository.getCurrentUserInitials()
    val totalSaved = AppState.circles.sumOf { it.savedAmount }

    Scaffold(
        topBar = {
            PundarMainTopBar(
                userName = currentUserName,
                userInitials = currentUserInitials,
                pundarScore = user.pundarScore,
                onNotificationClick = { navController.navigate(Routes.NOTIFICATIONS) },
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
                    shape = RoundedCornerShape(24.dp),
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
                    ) {
                        Column {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text(
                                    text = "Wallet Balance",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "${String.format("%,.2f", AppState.walletBalance.value)} XLM",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { AppState.refreshWalletBalance() }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh balance",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Tap to Refresh",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                thickness = 0.5.dp
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                                    .padding(vertical = 16.dp)
                            ) {
                                // Invested Section
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.15f))
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.invested),
                                            contentDescription = null
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Invested",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = "₱ 124,500",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                VerticalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = Color.White.copy(alpha = 0.15f),
                                    thickness = 0.5.dp
                                )

                                // Credit Score Section
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.15f))
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.score1),
                                            contentDescription = null
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Credit Score",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = "850",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
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
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        JourneyStep("Spend", R.drawable.spend, PundarSuccess, true)
                        JourneyArrow()
                        JourneyStep("Save", R.drawable.save, PundarSuccess, true)
                        JourneyArrow()
                        JourneyStep("Grow", R.drawable.grow1, PundarBlue, true)
                        JourneyArrow()
                        JourneyStep("Score", R.drawable.score, PundarGold, true)
                        JourneyArrow()
                        JourneyStep("Access", R.drawable.access, PundarTextSecondary, false)
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
private fun JourneyStep(
    label: String,
    @DrawableRes iconRes: Int,
    color: Color,
    isComplete: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )
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