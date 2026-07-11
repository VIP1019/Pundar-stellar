package com.example.pundarapp.ui.screens.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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

// ── Easing constants ─────────────────────────────────────────────
private val EaseOutBack   = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
private val EaseOutExpo   = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val user    = SampleData.currentUser
    val context = LocalContext.current

    var isLoadingActivities by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val userId = AuthRepository.getCurrentUserId()
        if (userId != null && AppState.homeActivities.isEmpty()) {
            val result = HomeRepository.getRecentActivities(userId)
            if (result.isSuccess) AppState.homeActivities.addAll(result.getOrDefault(emptyList()))
        }
        AppState.refreshWalletBalance()
        isLoadingActivities = false
    }

    val currentUserName     = AuthRepository.getCurrentUserName().split(" ").first()
    val currentUserInitials = AuthRepository.getCurrentUserInitials()
    val totalSaved          = AppState.circles.sumOf { it.savedAmount }

    Scaffold(
        topBar = {
            PundarMainTopBar(
                userName             = currentUserName,
                userInitials         = currentUserInitials,
                pundarScore          = user.pundarScore,
                onNotificationClick  = { navController.navigate(Routes.NOTIFICATIONS) },
                onSettingsClick      = { navController.navigate(Routes.SETTINGS) }
            )
        },
        containerColor = SpaceNavy
    ) { padding ->
        LazyColumn(
            modifier        = Modifier.fillMaxSize().padding(padding),
            contentPadding  = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { WalletCard(navController) }
            item { StatsRow(totalSaved, user.pundarScore) }
            item { QuickActionsSection(navController) }
            item { JourneySection() }
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


// ── 3D Animated Wallet Card ───────────────────────────────────────
@Composable
private fun WalletCard(navController: NavController) {
    val infinite = rememberInfiniteTransition(label = "wallet")

    // Continuous shimmer sweep
    val sweepX by infinite.animateFloat(
        initialValue = -700f, targetValue = 1400f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "sweep"
    )
    // Orb float
    val orbY by infinite.animateFloat(
        initialValue = -14f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(2600, easing = EaseInOutSine), RepeatMode.Reverse), label = "orb"
    )

    // Entrance: scale + alpha + 3D rotateX (perspective flip from top)
    val enterScale = remember { Animatable(0.88f) }
    val enterAlpha = remember { Animatable(0f) }
    val rotateX    = remember { Animatable(-18f) }   // 3D tilt on entrance
    LaunchedEffect(Unit) {
        enterAlpha.animateTo(1f, tween(500, easing = EaseOutExpo))
        enterScale.animateTo(1f, tween(550, easing = EaseOutBack))
        rotateX.animateTo(0f, tween(700, easing = EaseOutExpo))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(
                alpha        = enterAlpha.value,
                scaleX       = enterScale.value,
                scaleY       = enterScale.value,
                rotationX    = rotateX.value,
                cameraDistance = 14f * 8   // perspective
            )
            .shadow(28.dp, RoundedCornerShape(28.dp),
                ambientColor = ElectricBlue.copy(0.28f), spotColor = ElectricBlue.copy(0.28f))
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0A1E3D), Color(0xFF0D2A52), Color(0xFF06121E))))
    ) {
        // Glow orbs inside card
        Box(Modifier.size(180.dp).align(Alignment.TopEnd)
            .offset(x = 30.dp, y = (orbY - 30).dp)
            .background(Brush.radialGradient(listOf(ElectricBlue.copy(0.18f), Color.Transparent))))
        Box(Modifier.size(140.dp).align(Alignment.BottomStart)
            .offset(x = (-20).dp, y = (20 - orbY).dp)
            .background(Brush.radialGradient(listOf(ElectricPurple.copy(0.13f), Color.Transparent))))

        // Shimmer stripe
        Box(Modifier.matchParentSize().background(
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(0.05f), Color.Transparent),
                start = Offset(sweepX, 0f), end = Offset(sweepX + 260f, 400f)
            )
        ))
        // Top edge glow line
        Box(Modifier.fillMaxWidth().height(1.dp)
            .background(Brush.horizontalGradient(
                listOf(Color.Transparent, ElectricBlue.copy(0.7f), Color.Transparent))))

        Column(Modifier.padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("PUNDAR WALLET", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.5.sp, color = ElectricBlue.copy(0.9f))
                Box(Modifier.clip(RoundedCornerShape(6.dp))
                    .background(ElectricBlue.copy(0.15f))
                    .border(1.dp, ElectricBlue.copy(0.45f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text("PHP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Available Balance", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "₱${String.format("%,.2f", AppState.walletBalance.value)}",
                fontWeight = FontWeight.Black, fontSize = 38.sp,
                color = TextOnDark, letterSpacing = (-1).sp
            )
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Transfer CTA
                Box(
                    Modifier.weight(1f).height(46.dp)
                        .shadow(12.dp, RoundedCornerShape(14.dp),
                            ambientColor = PremiumGoldWarm.copy(0.45f), spotColor = PremiumGoldWarm.copy(0.45f))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(listOf(PremiumGoldDim, PremiumGoldWarm)))
                        .clickable { navController.navigate(Routes.SEND_MONEY) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SendToMobile, null, tint = SpaceBlack, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Transfer", color = SpaceBlack, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                // Refresh
                Box(
                    Modifier.weight(1f).height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GlassWhiteMid)
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .clickable { AppState.refreshWalletBalance() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Refresh, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Refresh", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
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
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                .background(accent.copy(0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            }
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
            Triple(Icons.Filled.Send,        "Send",   ElectricBlue)     to { navController.navigate(Routes.SEND_MONEY) },
            Triple(Icons.Filled.PhoneAndroid,"Load",   PremiumGoldWarm)  to { navController.navigate(Routes.BUY_LOAD) },
            Triple(Icons.Filled.Receipt,     "Split",  WarningAmber)     to { navController.navigate(Routes.PAY_NEW_BILL) },
            Triple(Icons.Filled.Groups,      "Circle", NeonGreen)        to { navController.navigate(Routes.CIRCLE) }
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
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(58.dp)
                .shadow(14.dp, RoundedCornerShape(18.dp),
                    ambientColor = accent.copy(0.35f), spotColor = accent.copy(0.35f))
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(accent.copy(0.28f), accent.copy(0.10f))))
                .border(1.dp, accent.copy(0.45f), RoundedCornerShape(18.dp))
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp,
            color = TextPrimary, textAlign = TextAlign.Center)
    }
}

// ── Journey Section ───────────────────────────────────────────────
@Composable
private fun JourneySection() {
    HomeSectionLabel("Your Financial Journey")
    Spacer(Modifier.height(12.dp))
    Box(
        Modifier.fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = ElectricBlue.copy(0.12f))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(SpaceDeep, Color(0xFF0E1825))))
            .border(1.dp, Brush.horizontalGradient(listOf(ElectricBlue.copy(0.3f), GlassWhite)), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            JourneyDot("Spend", R.drawable.spend,  NeonGreen,      true)
            JourneyLine(true)
            JourneyDot("Save",  R.drawable.save,   NeonGreen,      true)
            JourneyLine(true)
            JourneyDot("Grow",  R.drawable.grow1,  ElectricBlue,   true)
            JourneyLine(false)
            JourneyDot("Score", R.drawable.score,  PremiumGoldWarm,false)
            JourneyLine(false)
            JourneyDot("Access",R.drawable.access, TextTertiary,   false)
        }
    }
}

@Composable
private fun JourneyDot(label: String, @DrawableRes icon: Int, color: Color, done: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(36.dp).clip(CircleShape)
            .background(if (done) color.copy(0.18f) else SpaceMedium)
            .border(1.dp, if (done) color.copy(0.55f) else SpaceBorder, CircleShape),
            contentAlignment = Alignment.Center) {
            Image(painterResource(icon), null, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 9.sp, fontWeight = if (done) FontWeight.Bold else FontWeight.Normal,
            color = if (done) color else TextTertiary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun JourneyLine(filled: Boolean) {
    Box(Modifier.width(14.dp).height(2.dp).clip(RoundedCornerShape(1.dp))
        .background(if (filled)
            Brush.horizontalGradient(listOf(NeonGreen.copy(0.8f), ElectricBlue.copy(0.8f)))
        else
            Brush.horizontalGradient(listOf(SpaceBorder, SpaceBorder))))
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
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                .background(accent.copy(0.13f))
                .border(1.dp, accent.copy(0.32f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            }
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
