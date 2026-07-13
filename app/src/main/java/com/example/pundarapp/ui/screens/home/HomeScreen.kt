package com.example.pundarapp.ui.screens.home

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.pundarapp.ui.components.AnimatedBackground
import com.example.pundarapp.ui.components.BgAccent
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.HomeActivity
import com.example.pundarapp.ui.data.SampleData
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import kotlinx.coroutines.launch

private val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val EaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val user    = SampleData.currentUser
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var isLoadingActivities by remember { mutableStateOf(true) }
    var isRefreshing        by remember { mutableStateOf(false) }
    val pullState           = rememberPullToRefreshState()
    val refreshTrigger      = AppState.homeRefreshTrigger.intValue

    suspend fun refreshData() {
        val uid = AuthRepository.getCurrentUserId()
        if (uid != null) {
            AppState.homeActivities.clear()
            HomeRepository.getRecentActivities(uid).getOrNull()
                ?.let { AppState.homeActivities.addAll(it) }
        }
        AppState.refreshWalletBalance()
    }

    LaunchedEffect(Unit, refreshTrigger) { refreshData(); isLoadingActivities = false }

    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) scope.launch { refreshData() }
        }
        lifecycle.lifecycle.addObserver(obs)
        onDispose { lifecycle.lifecycle.removeObserver(obs) }
    }

    val session         by AuthRepository.currentUserState
    val firstName       = session?.name?.split(" ")?.firstOrNull() ?: "User"
    val initials        = AuthRepository.getCurrentUserInitials()
    val totalSaved      = AppState.circles.sumOf { it.savedAmount }

    AnimatedBackground(accent = BgAccent.Blue) {
    Scaffold(
        topBar = {
            PundarMainTopBar(
                userName            = firstName,
                userInitials        = initials,
                profileImageUrl     = session?.profileImageUrl,
                pundarScore         = user.pundarScore,
                onNotificationClick = { navController.navigate(Routes.NOTIFICATIONS) },
                onSettingsClick     = { navController.navigate(Routes.SETTINGS) }
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true; scope.launch { refreshData(); isRefreshing = false } },
            state    = pullState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Wallet card
                item {
                    FlippableWalletCard(
                        onTransfer = { navController.navigate(Routes.SEND_MONEY) },
                        onCashIn   = { navController.navigate(Routes.CASH_IN) },
                        onReceive  = { navController.navigate(Routes.RECEIVE_MONEY) }
                    )
                }

                // Stats row
                item { StatsRow(totalSaved, user.pundarScore) }

                // Quick Actions
                item { QuickActionsSection(navController) }

                // Financial Journey
                item { FinancialJourneySection() }

                // Recent Activity header
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("Recent Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold, color = TextWhite)
                        TextButton(onClick = {
                            Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("View All", style = MaterialTheme.typography.labelLarge, color = Blue400)
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
                    Text("Spend · Save · Grow · Together",
                        style = MaterialTheme.typography.bodySmall, color = TextDim,
                        textAlign = TextAlign.Center, letterSpacing = 1.sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                }
            }
        }
    }
    } // AnimatedBackground
}

// ── Stats Row ─────────────────────────────────────────────────────
@Composable
private fun StatsRow(totalSaved: Double, score: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(Modifier.weight(1f), "Total Saved",
            "₱${String.format("%,.0f", totalSaved)}", Icons.Filled.Savings, Green400, 0)
        StatCard(Modifier.weight(1f), "Credit Score",
            score.toString(), Icons.Filled.Star, Gold500, 70)
    }
}

@Composable
private fun StatCard(
    modifier: Modifier, label: String, value: String,
    icon: ImageVector, accent: Color, delay: Int
) {
    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(380, delayMillis = delay, easing = EaseOutExpo))
        scale.animateTo(1f, tween(420, delayMillis = delay, easing = EaseOutBack))
    }
    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale.value, scaleY = scale.value, alpha = alpha.value)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Navy800, Navy700)))
            .border(1.dp, Brush.linearGradient(listOf(accent.copy(0.30f), Glass10)), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(0.12f))
                    .border(1.dp, accent.copy(0.22f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

// ── Quick Actions ─────────────────────────────────────────────────
@Composable
private fun QuickActionsSection(navController: NavController) {
    Text("Quick Actions", style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold, color = TextWhite)
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val items = listOf(
            Triple(Icons.Filled.Send,         "Send",    Blue400)    to { navController.navigate(Routes.SEND_MONEY) },
            Triple(Icons.Filled.QrCode2,      "Receive", Green400)   to { navController.navigate(Routes.RECEIVE_MONEY) },
            Triple(Icons.Filled.PhoneAndroid, "Load",    Gold500)    to { navController.navigate(Routes.BUY_LOAD) },
            Triple(Icons.Filled.Receipt,      "Split",   Orange500)  to { navController.navigate(Routes.PAY_NEW_BILL) },
        )
        items.forEachIndexed { idx, (info, action) ->
            val (icon, title, accent) = info
            QuickTile(icon, title, accent, Modifier.weight(1f), idx * 55, action)
        }
    }
}

@Composable
private fun QuickTile(
    icon: ImageVector, title: String, accent: Color,
    modifier: Modifier, delay: Int, onClick: () -> Unit
) {
    val scale = remember { Animatable(0.78f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(300, delayMillis = delay))
        scale.animateTo(1f, tween(360, delayMillis = delay, easing = EaseOutBack))
    }
    Column(
        modifier = modifier
            .graphicsLayer(scaleX = scale.value, scaleY = scale.value, alpha = alpha.value)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(0.10f))
                .border(1.dp, accent.copy(0.25f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(7.dp))
        Text(title, fontWeight = FontWeight.Medium, fontSize = 11.sp,
            color = TextSoft, textAlign = TextAlign.Center)
    }
}

// ── Activity Row ──────────────────────────────────────────────────
@Composable
private fun ActivityRow(activity: HomeActivity, index: Int) {
    val accent = when (activity.module) {
        "Pay"    -> Blue400
        "Circle" -> Gold500
        "Grow"   -> Green400
        "Wallet" -> Blue300
        else     -> TextMuted
    }
    val icon = when (activity.module) {
        "Pay"    -> Icons.Filled.Receipt
        "Circle" -> Icons.Filled.Groups
        "Grow"   -> Icons.AutoMirrored.Filled.TrendingUp
        "Wallet" -> Icons.Filled.AccountBalanceWallet
        else     -> Icons.Filled.Info
    }
    val slideX = remember { Animatable(30f) }
    val alpha  = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        slideX.animateTo(0f, tween(320, delayMillis = index * 45, easing = EaseOutExpo))
        alpha.animateTo(1f,  tween(260, delayMillis = index * 45))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(translationX = slideX.value, alpha = alpha.value)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Navy800, Navy700)))
            .border(1.dp, Glass10, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(0.10f))
                .border(1.dp, accent.copy(0.20f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(activity.title, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = TextWhite)
            Text(activity.subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Text(activity.amount, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (activity.isPositive) Green400 else TextWhite)
    }
}

// ── Shimmer ───────────────────────────────────────────────────────
@Composable
private fun ActivityShimmer() {
    val inf = rememberInfiniteTransition(label = "shim")
    val x   by inf.animateFloat(-700f, 700f,
        infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "sx")
    val brush = Brush.linearGradient(
        listOf(Navy800, Navy600, Navy800),
        start = Offset(x, 0f), end = Offset(x + 280f, 0f)
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) {
            Box(Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(16.dp)).background(brush))
        }
    }
}

// ── Empty ─────────────────────────────────────────────────────────
@Composable
private fun EmptyActivity() {
    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon3DWallet(size = 52.dp)
            Spacer(Modifier.height(12.dp))
            Text("No recent activity yet.",
                style = MaterialTheme.typography.bodyMedium, color = TextMuted,
                textAlign = TextAlign.Center)
        }
    }
}
