package com.example.pundarapp.ui.screens.pay

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.BillStatus
import com.example.pundarapp.ui.data.SampleData
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.data.remote.AuthRepository

private val EaseOutBack   = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
private val EaseOutExpo   = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayScreen(navController: NavController) {
    val bills         = AppState.bills
    val settledCount  = bills.count { it.status == BillStatus.SETTLED }
    val pendingCount  = bills.count { it.status == BillStatus.PENDING || it.status == BillStatus.PARTIAL }
    val monthTotal    = bills.sumOf { it.yourShare }
    val userSession by AuthRepository.currentUserState
    val currentName   = userSession?.name ?: "User"
    val initials      = AuthRepository.getCurrentUserInitials()

    Scaffold(
        topBar = {
            PundarMainTopBar(
                userName            = currentName,
                userInitials        = initials,
                profileImageUrl     = userSession?.profileImageUrl,
                pundarScore         = SampleData.currentUser.pundarScore,
                onNotificationClick = { navController.navigate(Routes.NOTIFICATIONS) },
                onSettingsClick     = { navController.navigate(Routes.SETTINGS) }
            )
        },
        floatingActionButton = { PayFab { navController.navigate(Routes.PAY_NEW_BILL) } },
        containerColor = SpaceNavy
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { PayHeroHeader() }
            item { PayStatsCard(settledCount, pendingCount, monthTotal) }
            item { PayActionRow(navController) }
            item { PaySectionLabel("Recent Splits") }
            if (bills.isEmpty()) {
                item { PayEmptyState() }
            } else {
                itemsIndexed(bills.toList()) { idx, bill ->
                    BillCard(bill = bill, index = idx,
                        onClick = { navController.navigate(Routes.billDetail(bill.id)) })
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Verified, null, tint = ElectricBlue, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Settlement builds your PUNDAR Score.", style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary)
                }
            }
        }
    }
}

// ── Glowing FAB ───────────────────────────────────────────────────
@Composable
private fun PayFab(onClick: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "fab").animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "fabPulse"
    )
    Box(
        Modifier.shadow(
            (16 * pulse).dp, RoundedCornerShape(18.dp),
            ambientColor = PremiumGoldWarm.copy(pulse * 0.55f),
            spotColor    = PremiumGoldWarm.copy(pulse * 0.55f)
        )
    ) {
        ExtendedFloatingActionButton(
            onClick          = onClick,
            containerColor   = Color.Transparent,
            contentColor     = SpaceBlack,
            shape            = RoundedCornerShape(18.dp),
            modifier         = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(PremiumGoldDim, PremiumGoldWarm)))
        ) {
            Icon(Icons.Filled.Add, "New Bill")
            Spacer(Modifier.width(8.dp))
            Text("New Group Bill", fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ── Hero header ───────────────────────────────────────────────────
@Composable
private fun PayHeroHeader() {
    val alpha = remember { Animatable(0f) }
    val slideY = remember { Animatable(-20f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(500, easing = EaseOutExpo))
        slideY.animateTo(0f, tween(500, easing = EaseOutExpo))
    }
    Column(Modifier.graphicsLayer(alpha = alpha.value, translationY = slideY.value)) {
        Text("PUNDAR Pay", fontWeight = FontWeight.Black, fontSize = 32.sp,
            color = TextOnDark, letterSpacing = (-0.5).sp)
        Spacer(Modifier.height(4.dp))
        Text("Intelligent Settlement · Stellar Rails",
            style = MaterialTheme.typography.bodyMedium, color = TextSecondary, letterSpacing = 0.3.sp)
    }
}

// ── Stats card ────────────────────────────────────────────────────
@Composable
private fun PayStatsCard(settled: Int, pending: Int, total: Double) {
    val infinite = rememberInfiniteTransition(label = "payCard")
    val sweepX by infinite.animateFloat(
        initialValue = -700f, targetValue = 1400f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "ps"
    )
    val scale = remember { Animatable(0.9f) }
    val rotX  = remember { Animatable(-12f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(550, easing = EaseOutBack))
        rotX.animateTo(0f,  tween(600, easing = EaseOutExpo))
    }

    Box(
        Modifier.fillMaxWidth()
            .graphicsLayer(scaleX = scale.value, scaleY = scale.value,
                rotationX = rotX.value, cameraDistance = 14f * 8)
            .shadow(20.dp, RoundedCornerShape(24.dp),
                ambientColor = ElectricBlue.copy(0.22f), spotColor = ElectricBlue.copy(0.22f))
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0A1E3D), Color(0xFF0D2A52), Color(0xFF06121E))))
    ) {
        // shimmer
        Box(Modifier.matchParentSize().background(
            Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(0.04f), Color.Transparent),
                start = Offset(sweepX, 0f), end = Offset(sweepX + 280f, 300f))))
        // top glow line
        Box(Modifier.fillMaxWidth().height(1.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, ElectricBlue.copy(0.7f), Color.Transparent))))

        Column(Modifier.padding(22.dp)) {
            Text("MONTHLY SPEND", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp, color = ElectricBlue.copy(0.85f))
            Spacer(Modifier.height(8.dp))
            Text("₱${String.format("%,.2f", total)}", fontWeight = FontWeight.Black,
                fontSize = 34.sp, color = TextOnDark, letterSpacing = (-1).sp)
            Spacer(Modifier.height(20.dp))
            // Divider
            Box(Modifier.fillMaxWidth().height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, GlassBorder, Color.Transparent))))
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                PayStatPill(settled, "Settled", Icons.Filled.CheckCircle, NeonGreen)
                Box(Modifier.width(1.dp).height(36.dp).background(SpaceBorder))
                PayStatPill(pending, "Pending", Icons.Filled.Schedule, WarningAmber)
            }
        }
    }
}

@Composable
private fun PayStatPill(
    count: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FuturisticIconCircle(
                icon = icon,
                tint = color,
                size = 30.dp,
                iconSize = 15.dp
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = count.toString(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = color
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}


// ── Action Row ────────────────────────────────────────────────────
@Composable
private fun PayActionRow(navController: NavController) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PayActionBtn(
            label = "Create Group\nExpense",
            icon  = Icons.Filled.PersonAdd,
            brush = Brush.horizontalGradient(listOf(ElectricBlueDeep, ElectricBlue)),
            tint  = TextOnDark,
            modifier = Modifier.weight(1f)
        ) { navController.navigate(Routes.PAY_NEW_BILL) }

        PayActionBtn(
            label = "Instant\nSettle",
            icon  = Icons.Filled.FlashOn,
            brush = Brush.horizontalGradient(listOf(PremiumGoldDim, PremiumGoldWarm)),
            tint  = SpaceBlack,
            modifier = Modifier.weight(1f)
        ) { navController.navigate(Routes.INSTANT_SETTLE) }
    }
}

@Composable
private fun PayActionBtn(
    label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    brush: Brush, tint: Color, modifier: Modifier, onClick: () -> Unit
) {
    val scale = remember { Animatable(0.88f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, tween(400, easing = EaseOutBack)) }
    Box(
        modifier.graphicsLayer(scaleX = scale.value, scaleY = scale.value)
            .height(56.dp)
            .shadow(10.dp, RoundedCornerShape(16.dp),
                ambientColor = PremiumGoldWarm.copy(0.2f), spotColor = ElectricBlue.copy(0.2f))
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = tint, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}

// ── Bill Card (3D entrance, staggered) ────────────────────────────
@Composable
private fun BillCard(bill: com.example.pundarapp.ui.data.GroupBill, index: Int, onClick: () -> Unit) {
    val accent = when (bill.status) {
        BillStatus.SETTLED -> NeonGreen
        BillStatus.PENDING -> WarningAmber
        BillStatus.PARTIAL -> ElectricBlue
    }
    val statusIcon = when (bill.status) {
        BillStatus.SETTLED -> Icons.Filled.CheckCircle
        BillStatus.PENDING -> Icons.Filled.Schedule
        BillStatus.PARTIAL -> Icons.Filled.PieChart
    }
    val statusLabel = when (bill.status) {
        BillStatus.SETTLED -> "Settled"
        BillStatus.PENDING -> "Pending"
        BillStatus.PARTIAL -> "Partial"
    }
    val slideX = remember { Animatable(50f) }
    val alpha  = remember { Animatable(0f) }
    val rotY   = remember { Animatable(15f) }
    LaunchedEffect(Unit) {
        slideX.animateTo(0f, tween(380, delayMillis = index * 70, easing = EaseOutExpo))
        alpha.animateTo(1f,  tween(320, delayMillis = index * 70))
        rotY.animateTo(0f,   tween(450, delayMillis = index * 70, easing = EaseOutExpo))
    }

    Box(
        Modifier.fillMaxWidth()
            .graphicsLayer(translationX = slideX.value, alpha = alpha.value,
                rotationY = rotY.value, cameraDistance = 14f * 8)
            .shadow(12.dp, RoundedCornerShape(20.dp),
                ambientColor = accent.copy(0.18f), spotColor = accent.copy(0.18f))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(SpaceDeep, Color(0xFF0E1825))))
            .border(1.dp, Brush.linearGradient(listOf(accent.copy(0.45f), GlassWhite)), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                .background(accent.copy(0.14f))
                .border(1.dp, accent.copy(0.35f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center) {
                Icon(statusIcon, null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(bill.name, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.clip(RoundedCornerShape(5.dp))
                        .background(accent.copy(0.13f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(statusLabel, style = MaterialTheme.typography.labelSmall,
                            color = accent, fontWeight = FontWeight.Bold)
                    }
                    Text(" · ${bill.memberCount} members · ${bill.date}",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("₱${String.format("%,.2f", bill.totalAmount)}",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Your: ₱${String.format("%,.2f", bill.yourShare)}",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun PayEmptyState() {
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(SpaceDeep, Color(0xFF0E1825))))
            .border(1.dp, SpaceBorder, RoundedCornerShape(20.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon3DReceipt(size = 56.dp)
            Spacer(Modifier.height(10.dp))
            Text("No bills yet.", fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text("Tap \"New Group Bill\" to get started.", style = MaterialTheme.typography.bodySmall,
                color = TextTertiary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PaySectionLabel(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary, letterSpacing = 0.2.sp)
}
