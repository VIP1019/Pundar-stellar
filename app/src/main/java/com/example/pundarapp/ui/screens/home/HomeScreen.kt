package com.example.pundarapp.ui.screens.home

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.SampleData
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.remote.HomeRepository
import com.example.pundarapp.ui.data.HomeActivity
import com.example.pundarapp.ui.data.AppState

// ── Easing constants ─────────────────────────────────────────────
private val EaseOutBack   = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
private val EaseOutExpo   = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val user    = SampleData.currentUser
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var isLoadingActivities by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()
    val refreshTrigger = AppState.homeRefreshTrigger.intValue

    // Function to refresh data
    suspend fun refreshData() {
        val userId = AuthRepository.getCurrentUserId()
        if (userId != null) {
            AppState.homeActivities.clear()
            val result = HomeRepository.getRecentActivities(userId)
            if (result.isSuccess) {
                AppState.homeActivities.addAll(result.getOrDefault(emptyList()))
            }
        }
        AppState.refreshWalletBalance()
    }

    // Initial data load + refresh after transactions
    LaunchedEffect(Unit, refreshTrigger) {
        refreshData()
        isLoadingActivities = false
    }

    // Auto-refresh on resume
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                scope.launch { refreshData() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val userSession by AuthRepository.currentUserState
    val currentUserName     = userSession?.name?.split(" ")?.firstOrNull() ?: "User"
    val currentUserInitials = AuthRepository.getCurrentUserInitials()
    val totalSaved          = AppState.circles.sumOf { it.savedAmount }

    Scaffold(
        topBar = {
            PundarMainTopBar(
                userName             = currentUserName,
                userInitials         = currentUserInitials,
                profileImageUrl      = userSession?.profileImageUrl,
                pundarScore          = user.pundarScore,
                onNotificationClick  = { navController.navigate(Routes.NOTIFICATIONS) },
                onSettingsClick      = { navController.navigate(Routes.SETTINGS) }
            )
        },
        containerColor = SpaceNavy
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch {
                    refreshData()
                    isRefreshing = false
                }
            },
            state = pullRefreshState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier        = Modifier.fillMaxSize(),
                contentPadding  = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FlippableWalletCard(
                        onTransfer = { navController.navigate(Routes.SEND_MONEY) }
                    )
                }
                item { StatsRow(totalSaved, user.pundarScore) }
                item { QuickActionsSection(navController) }
                item { FinancialJourneySection() }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        HomeSectionLabel("Recent Activity")
                        TextButton(onClick = { Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show() }) {
                            Text("View All", color = ElectricBlue, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                if (isLoadingActivities) {
                    item { ActivityShimmer() }
                } else if (AppState.homeActivities.isEmpty()) {
                    item { EmptyActivity() }
                } else {
                    itemsIndexed(AppState.homeActivities) { idx, activity ->
                        ActivityRow(activity = activity, index = idx)
                    }
                }
                item {
                    Text(
                        text  = "Spend · Save · Grow · Together",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        textAlign    = TextAlign.Center,
                        letterSpacing = 1.sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
        }
    }
}


// ── Stats Row ─────────────────────────────────────────────────────
@Composable
private fun StatsRow(totalSaved: Double, score: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(Modifier.weight(1f), "Total Saved",
            "₱${String.format("%,.0f", totalSaved)}", Icons.Filled.Savings, NeonGreen, delay = 0)
        StatCard(Modifier.weight(1f), "Credit Score",
            score.toString(), Icons.Filled.Star, PremiumGoldWarm, delay = 80)
    }
}

@Composable
private fun StatCard(
    modifier: Modifier, label: String, value: String,
    icon: ImageVector, accent: Color, delay: Int
) {
    val scale = remember { Animatable(0.82f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(400, delayMillis = delay, easing = EaseOutExpo))
        scale.animateTo(1f, tween(450, delayMillis = delay, easing = EaseOutBack))
    }
    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale.value, scaleY = scale.value, alpha = alpha.value)
            .shadow(10.dp, RoundedCornerShape(18.dp),
                ambientColor = accent.copy(0.18f), spotColor = accent.copy(0.18f))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(SpaceDeep, Color(0xFF0E1825))))
            .border(1.dp, Brush.linearGradient(listOf(accent.copy(0.45f), GlassWhite)), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            FuturisticIcon(
                icon = icon,
                tint = accent,
                size = 36.dp,
                iconSize = 18.dp,
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold, color = accent)
        }
    }
}


// ── Quick Actions ─────────────────────────────────────────────────
@Composable
private fun QuickActionsSection(navController: NavController) {
    HomeSectionLabel("Quick Actions")
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val items = listOf(
            Triple(Icons.Filled.AddCard,     "Cash In", NeonGreen)       to { navController.navigate(Routes.CASH_IN) },
            Triple(Icons.Filled.Send,        "Send",   ElectricBlue)     to { navController.navigate(Routes.SEND_MONEY) },
            Triple(Icons.Filled.QrCode2,     "Receive", NeonCyan)       to { navController.navigate(Routes.RECEIVE_MONEY) },
            Triple(Icons.Filled.Receipt,     "Pay",    PremiumGoldWarm)  to { navController.navigate(Routes.PAY) },
        )
        items.forEachIndexed { idx, (info, action) ->
            val (icon, title, accent) = info
            QuickTile(icon, title, accent, Modifier.weight(1f), delay = idx * 60, onClick = action)
        }
    }
}

@Composable
private fun QuickTile(
    icon: ImageVector, title: String, accent: Color,
    modifier: Modifier, delay: Int, onClick: () -> Unit
) {
    val scale = remember { Animatable(0.75f) }
    val alpha = remember { Animatable(0f) }
    val rotY  = remember { Animatable(25f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(350, delayMillis = delay))
        scale.animateTo(1f, tween(400, delayMillis = delay, easing = EaseOutBack))
        rotY.animateTo(0f,  tween(500, delayMillis = delay, easing = EaseOutExpo))
    }
    Column(
        modifier = modifier
            .graphicsLayer(
                scaleX = scale.value, scaleY = scale.value, alpha = alpha.value,
                rotationY = rotY.value, cameraDistance = 12f * 8
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FuturisticIcon(
            icon = icon,
            tint = accent,
            size = 58.dp,
            iconSize = 26.dp,
            shape = RoundedCornerShape(18.dp),
            pulseGlow = true
        )
        Spacer(Modifier.height(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp,
            color = TextPrimary, textAlign = TextAlign.Center)
    }
}

// ── Activity composables ──────────────────────────────────────────
@Composable
private fun ActivityRow(activity: HomeActivity, index: Int) {
    val accent = when (activity.module) {
        "Pay"    -> ElectricBlue
        "Circle" -> PremiumGoldWarm
        "Grow"   -> NeonGreen
        "Wallet" -> NeonCyan
        else     -> TextSecondary
    }
    val icon = when (activity.module) {
        "Pay"    -> Icons.Filled.Receipt
        "Circle" -> Icons.Filled.Groups
        "Grow"   -> Icons.AutoMirrored.Filled.TrendingUp
        "Wallet" -> Icons.Filled.AccountBalanceWallet
        else     -> Icons.Filled.Info
    }
    val slideX = remember { Animatable(40f) }
    val alpha  = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        slideX.animateTo(0f, tween(360, delayMillis = index * 55, easing = EaseOutExpo))
        alpha.animateTo(1f,  tween(300, delayMillis = index * 55))
    }
    Box(
        Modifier.fillMaxWidth()
            .graphicsLayer(translationX = slideX.value, alpha = alpha.value)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(SpaceDeep, Color(0xFF0E1825))))
            .border(1.dp, Brush.horizontalGradient(listOf(accent.copy(0.22f), GlassWhite)), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FuturisticIcon(
                icon = icon,
                tint = accent,
                size = 42.dp,
                iconSize = 20.dp,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(activity.title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(activity.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Text(activity.amount, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (activity.isPositive) NeonGreen else TextPrimary)
        }
    }
}

@Composable
private fun ActivityShimmer() {
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val x by infinite.animateFloat(
        initialValue = -800f, targetValue = 800f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "sx"
    )
    val brush = Brush.linearGradient(
        listOf(SpaceMedium, Color(0xFF2A3A55), SpaceMedium),
        start = Offset(x, 0f), end = Offset(x + 300f, 0f)
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) {
            Box(Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(16.dp)).background(brush))
        }
    }
}

@Composable
private fun EmptyActivity() {
    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon3DWallet(size = 56.dp)
            Spacer(Modifier.height(12.dp))
            Text("No recent activity yet.", style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun HomeSectionLabel(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp,
        color = TextPrimary, letterSpacing = 0.2.sp)
}
