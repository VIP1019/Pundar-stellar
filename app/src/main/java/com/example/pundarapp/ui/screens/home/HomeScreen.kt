package com.example.pundarapp.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.remote.HomeRepository
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.HomeActivity
import com.example.pundarapp.ui.data.SampleData
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import kotlinx.coroutines.launch

private val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val scope = rememberCoroutineScope()

    var selectedActivity by remember { mutableStateOf<HomeActivity?>(null) }
    var isLoadingActivities by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()
    val refreshTrigger = AppState.homeRefreshTrigger.intValue

    suspend fun refreshData() {
        val uid = AuthRepository.getCurrentUserId()
        if (uid != null) {
            AppState.homeActivities.clear()
            HomeRepository.getRecentActivities(uid).getOrNull()
                ?.let { AppState.homeActivities.addAll(it) }
        }
        AppState.refreshWalletBalance()
        AppState.refreshCircles()
    }

    LaunchedEffect(Unit, refreshTrigger) {
        refreshData()
        isLoadingActivities = false
    }

    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) scope.launch { refreshData() }
        }
        lifecycle.lifecycle.addObserver(obs)
        onDispose { lifecycle.lifecycle.removeObserver(obs) }
    }

    val session by AuthRepository.currentUserState
    val fullName = session?.name ?: "Mark James"
    val initials = AuthRepository.getCurrentUserInitials().ifBlank { "MJ" }
    val walletBalance = AppState.walletBalance.value
    val exchangeRate = AppState.getPhpRate()
    val fiatBalanceStr = if (AppState.isBalanceHidden.value) "₱ •••••" else "₱ ${String.format("%,.2f", walletBalance * exchangeRate)}"
    val xlmBalanceStr = if (AppState.isBalanceHidden.value) "••••• XLM" else "≈ ${String.format("%,.2f", walletBalance)} XLM"

    Scaffold(
        containerColor = Color(0xFFF4F6F9)
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true; scope.launch { refreshData(); isRefreshing = false } },
            state = pullState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── DEEP NAVY HERO HEADER (Matching Screenshot) ─────────
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                                .background(Color(0xFF0B1B3D)) // Deep Dark Navy Blue
                                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 44.dp)
                        ) {
                            // User Profile + Top Icon Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E2E5C))
                                            .border(1.5.dp, Color(0xFF3B82F6), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            initials,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Kamusta,",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                        Text(
                                            fullName,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Notification Icon Button
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E2E5C))
                                            .clickable { navController.navigate(Routes.NOTIFICATIONS) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Notifications,
                                            contentDescription = "Notifications",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                                .align(Alignment.TopEnd)
                                        )
                                    }

                                    // Settings Icon Button
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E2E5C))
                                            .clickable { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Settings,
                                            contentDescription = "Settings",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            // Available Balance Section
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Pill Badge: • Available Balance • Stellar Live
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF1E2E5C))
                                        .padding(horizontal = 14.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Available Balance • Stellar Live",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                // Main Amount: ₱ 12,450.00
                                Text(
                                    fiatBalanceStr,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )

                                Spacer(Modifier.height(4.dp))

                                // Subtitle: ≈ 842.15 XLM • Verified Account
                                Text(
                                    "$xlmBalanceStr • Verified Account",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Spacer(Modifier.height(24.dp))

                            // Embedded PUNDAR SCORE Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navController.navigate(Routes.SCORE) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2450))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "PUNDAR SCORE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8),
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Filled.Star,
                                                contentDescription = "Star",
                                                tint = Color(0xFFF59E0B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "${AppState.pundarScore.intValue}",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFF59E0B)
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF064E3B))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            "Excellent",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF34D399)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── FLOATING QUICK ACTION TILES (Overlapping Card) ─────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .offset(y = (-20).dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Cash In
                                QuickActionTile(
                                    label = "Cash In",
                                    icon = Icons.Filled.Add,
                                    bgColor = Color(0xFF10B981),
                                    onClick = { navController.navigate(Routes.CASH_IN) }
                                )

                                // Send
                                QuickActionTile(
                                    label = "Send",
                                    icon = Icons.Filled.Send,
                                    bgColor = Color(0xFF2563EB),
                                    onClick = { navController.navigate(Routes.SEND_MONEY) }
                                )

                                // Receive
                                QuickActionTile(
                                    label = "Receive",
                                    icon = Icons.Filled.ArrowDownward,
                                    bgColor = Color(0xFFF59E0B),
                                    onClick = { navController.navigate(Routes.RECEIVE_MONEY) }
                                )
                            }
                        }
                    }
                }

                // ── UPCOMING OBLIGATIONS SECTION ───────────────────────
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Upcoming Obligations",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "See all",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB),
                                modifier = Modifier.clickable { navController.navigate(Routes.CIRCLE) }
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Card 1: Batangas Bakers Circle
                        ObligationCard(
                            title = "Batangas Bakers Circle",
                            subtitle = "Round 4 of 10 • ",
                            highlightedText = "Due in 3 days",
                            amountStr = "₱ 1,000.00",
                            statusStr = "Auto-Debit",
                            icon = Icons.Filled.Groups,
                            iconBgColor = Color(0xFFEFF6FF),
                            iconColor = Color(0xFF2563EB),
                            onClick = { navController.navigate(Routes.CIRCLE) }
                        )

                        Spacer(Modifier.height(10.dp))

                        // Card 2: Fiber Internet Split
                        ObligationCard(
                            title = "Fiber Internet Split",
                            subtitle = "Apartment 4B • Due on 15th",
                            highlightedText = "",
                            amountStr = "₱ 375.00",
                            statusStr = "Pending Pay",
                            icon = Icons.Filled.Wifi,
                            iconBgColor = Color(0xFFF3E8FF),
                            iconColor = Color(0xFF8B5CF6),
                            onClick = { navController.navigate(Routes.PAY) }
                        )
                    }
                }

                // ── RECENT ACTIVITY SECTION ───────────────────────────
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Recent Activity",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "View All",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB),
                                modifier = Modifier.clickable { navController.navigate(Routes.ALL_ACTIVITY) }
                            )
                        }
                    }
                }

                if (isLoadingActivities) {
                    item { ActivityShimmer() }
                } else if (AppState.homeActivities.isEmpty()) {
                    item { EmptyActivity() }
                } else {
                    itemsIndexed(AppState.homeActivities) { idx, activity ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ActivityRow(
                                activity = activity,
                                index = idx,
                                onClick = { selectedActivity = activity }
                            )
                        }
                    }
                }

                item {
                    Text(
                        "Spend · Save · Score · Together",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        }
    }

    selectedActivity?.let { activity ->
        ReceiptDialog(
            activity = activity,
            onDismiss = { selectedActivity = null }
        )
    }
}

// ── Quick Action Tile Helper ──────────────────────────────────────
@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    bgColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
    }
}

// ── Obligation Card Helper ────────────────────────────────────────
@Composable
private fun ObligationCard(
    title: String,
    subtitle: String,
    highlightedText: String,
    amountStr: String,
    statusStr: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
                    if (highlightedText.isNotEmpty()) {
                        Text(highlightedText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF97316))
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    amountStr,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    statusStr,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

// ── Activity Row ──────────────────────────────────────────────────
@Composable
private fun ActivityRow(activity: HomeActivity, index: Int, onClick: () -> Unit = {}) {
    val accent = when (activity.module) {
        "Pay" -> Color(0xFF2563EB)
        "Circle" -> Color(0xFFF59E0B)
        "Wallet" -> Color(0xFF10B981)
        else -> Color(0xFF64748B)
    }
    val icon = when (activity.module) {
        "Pay" -> Icons.Filled.Receipt
        "Circle" -> Icons.Filled.Groups
        "Wallet" -> Icons.Filled.AccountBalanceWallet
        else -> Icons.Filled.Info
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    activity.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(activity.subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
            }
            Text(
                activity.amount,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (activity.isPositive) Color(0xFF10B981) else Color(0xFF0F172A)
            )
        }
    }
}

// ── Activity Shimmer ──────────────────────────────────────────────
@Composable
private fun ActivityShimmer() {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(3) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE2E8F0))
            )
        }
    }
}

// ── Empty Activity State ──────────────────────────────────────────
@Composable
private fun EmptyActivity() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                "No recent activity yet.",
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}
