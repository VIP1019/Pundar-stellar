package com.example.pundarapp.ui.screens.grow

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.*
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*

// ── Easing curves ────────────────────────────────────────────────
private val EaseOutBack  = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
private val EaseOutExpo  = CubicBezierEasing(0.16f, 1f,    0.3f,  1f)


// ═══════════════════════════════════════════════════════════════
//  GrowScreen
// ═══════════════════════════════════════════════════════════════

@Composable
fun GrowScreen(navController: NavController) {
    val portfolio by AppState.portfolio

    // ── Dialog visibility ────────────────────────────────────────
    var showInvestDialog   by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showOptimizeDialog by remember { mutableStateOf(false) }
    var investAmount       by remember { mutableStateOf("") }
    var withdrawAmount     by remember { mutableStateOf("") }

    // ── Scaffold ─────────────────────────────────────────────────
    Scaffold(
        containerColor = SpaceNavy,
        topBar = {
            PundarGrowTopBar(
                userInitials       = SampleData.currentUser.initials,
                pundarScore        = SampleData.currentUser.pundarScore,
                onNotificationClick = { navController.navigate(Routes.NOTIFICATIONS) }
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceNavy),
            contentPadding = PaddingValues(
                top    = paddingValues.calculateTopPadding() + 12.dp,
                bottom = 100.dp,
                start  = 16.dp,
                end    = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1 ── Portfolio Hero Card
            item {
                PortfolioHeroCard(
                    portfolio  = portfolio,
                    onInvest   = { showInvestDialog   = true },
                    onWithdraw = { showWithdrawDialog = true }
                )
            }

            // 2 ── Allocation
            item { AllocationSection(portfolio = portfolio) }

            // 3 ── Activity
            item { ActivitySection(activities = portfolio.activities) }

            // 4 ── Holdings Table
            item {
                HoldingsSection(
                    holdings        = portfolio.holdings,
                    onOptimize      = { showOptimizeDialog = true },
                    onHoldingClick  = { ticker ->
                        navController.navigate(Routes.stockDetail(ticker))
                    }
                )
            }
        }
    }

    // ── Invest Dialog ─────────────────────────────────────────────
    if (showInvestDialog) {
        GrowAlertDialog(
            title        = "Invest More",
            confirmLabel = "Invest",
            onDismiss    = { showInvestDialog = false; investAmount = "" },
            onConfirm    = {
                investAmount.toDoubleOrNull()?.let { AppState.invest(it) }
                showInvestDialog = false
                investAmount = ""
            }
        ) {
            GrowTextField(
                value         = investAmount,
                onValueChange = { investAmount = it },
                label         = "Amount (₱)",
                placeholder   = "e.g. 5000"
            )
        }
    }

    // ── Withdraw Dialog ───────────────────────────────────────────
    if (showWithdrawDialog) {
        GrowAlertDialog(
            title        = "Withdraw",
            confirmLabel = "Withdraw",
            onDismiss    = { showWithdrawDialog = false; withdrawAmount = "" },
            onConfirm    = {
                withdrawAmount.toDoubleOrNull()?.let { AppState.withdraw(it) }
                showWithdrawDialog = false
                withdrawAmount = ""
            }
        ) {
            GrowTextField(
                value         = withdrawAmount,
                onValueChange = { withdrawAmount = it },
                label         = "Amount (₱)",
                placeholder   = "e.g. 1000"
            )
        }
    }

    // ── Optimize Dialog ───────────────────────────────────────────
    if (showOptimizeDialog) {
        GrowAlertDialog(
            title        = "Optimize Portfolio",
            confirmLabel = "Apply",
            onDismiss    = { showOptimizeDialog = false },
            onConfirm    = { showOptimizeDialog = false }
        ) {
            Text(
                text  = "Our AI-powered engine will rebalance your holdings to maximize risk-adjusted returns based on your current allocation targets.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier            = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpaceMedium)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("PH Equities", color = TextSecondary, fontSize = 12.sp)
                    Text("80%  →  75%", color = ElectricBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Fixed Income", color = TextSecondary, fontSize = 12.sp)
                    Text("20%  →  25%", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════
//  Shared Dialog Helpers
// ═══════════════════════════════════════════════════════════════

@Composable
private fun GrowAlertDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SpaceDeep,
        tonalElevation   = 0.dp,
        shape            = RoundedCornerShape(20.dp),
        title = {
            Text(
                text       = title,
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp,
                color      = TextPrimary
            )
        },
        text = {
            Column { content() }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(listOf(ElectricBlueDeep, ElectricBlue))
                    )
                    .clickable { onConfirm() }
                    .padding(horizontal = 22.dp, vertical = 10.dp)
            ) {
                Text(
                    text       = confirmLabel,
                    color      = TextOnDark,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary, fontSize = 14.sp)
            }
        }
    )
}

@Composable
private fun GrowTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    OutlinedTextField(
        value            = value,
        onValueChange    = onValueChange,
        label            = { Text(label, color = TextSecondary) },
        placeholder      = { Text(placeholder, color = TextTertiary) },
        singleLine       = true,
        keyboardOptions  = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier         = Modifier.fillMaxWidth(),
        colors           = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = ElectricBlue,
            unfocusedBorderColor    = SpaceBorder,
            focusedLabelColor       = ElectricBlue,
            unfocusedLabelColor     = TextSecondary,
            focusedContainerColor   = SpaceMedium,
            unfocusedContainerColor = SpaceMedium,
            cursorColor             = ElectricBlue,
            focusedTextColor        = TextPrimary,
            unfocusedTextColor      = TextPrimary
        ),
        shape = RoundedCornerShape(12.dp)
    )
}


// ═══════════════════════════════════════════════════════════════
//  Portfolio Hero Card
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PortfolioHeroCard(
    portfolio: Portfolio,
    onInvest:   () -> Unit,
    onWithdraw: () -> Unit
) {
    // ── 3-D entrance animation ──────────────────────────────────
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val rotX by animateFloatAsState(
        targetValue      = if (visible) 0f else -14f,
        animationSpec    = tween(700, easing = EaseOutExpo),
        label            = "heroRotX"
    )
    val scale by animateFloatAsState(
        targetValue      = if (visible) 1f else 0.88f,
        animationSpec    = tween(700, easing = EaseOutBack),
        label            = "heroScale"
    )
    val alpha by animateFloatAsState(
        targetValue      = if (visible) 1f else 0f,
        animationSpec    = tween(500),
        label            = "heroAlpha"
    )

    // ── Shimmer sweep ───────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "heroShimmer")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue     = -600f,
        targetValue      = 1400f,
        animationSpec    = infiniteRepeatable(
            animation    = tween(2200, easing = LinearEasing),
            repeatMode   = RepeatMode.Restart
        ),
        label            = "shimmerX"
    )

    // ── Return badge pulse ──────────────────────────────────────
    val badgeScale by infiniteTransition.animateFloat(
        initialValue     = 1f,
        targetValue      = 1.06f,
        animationSpec    = infiniteRepeatable(
            animation    = tween(900, easing = EaseOutBack),
            repeatMode   = RepeatMode.Reverse
        ),
        label            = "badgePulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(
                rotationX    = rotX,
                scaleX       = scale,
                scaleY       = scale,
                alpha        = alpha,
                cameraDistance = 12f * androidx.compose.ui.platform.LocalDensity.current.density
            )
            .shadow(
                elevation    = 24.dp,
                shape        = RoundedCornerShape(24.dp),
                ambientColor = ElectricBlue.copy(0.25f),
                spotColor    = ElectricBlue.copy(0.25f),
                clip         = false
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0A1628),
                        Color(0xFF0C1A30),
                        Color(0xFF050C18)
                    )
                )
            )
    ) {
        // Shimmer overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        start = Offset(shimmerX, 0f),
                        end   = Offset(shimmerX + 300f, 300f)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Top neon edge line ──────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                ElectricBlue.copy(0.8f),
                                ElectricBlue,
                                ElectricBlue.copy(0.8f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {

                // ── TOTAL ASSETS label ──────────────────────────
                Text(
                    text          = "TOTAL ASSETS",
                    color         = TextSecondary,
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(6.dp))

                // ── Balance value ───────────────────────────────
                Text(
                    text       = "₱ ${String.format("%,.2f", portfolio.totalValue)}",
                    color      = TextPrimary,
                    fontSize   = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )

                Spacer(Modifier.height(10.dp))

                // ── Return badge ────────────────────────────────
                Box(
                    modifier = Modifier
                        .graphicsLayer(scaleX = badgeScale, scaleY = badgeScale)
                        .clip(RoundedCornerShape(50.dp))
                        .background(NeonGreen.copy(alpha = 0.15f))
                        .border(1.dp, NeonGreen.copy(0.4f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector        = Icons.Filled.TrendingUp,
                            contentDescription = null,
                            tint               = NeonGreen,
                            modifier           = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text       = "+${portfolio.totalReturnPercent}%  " +
                                "₱ ${String.format("%,.2f", portfolio.totalReturnAmount)}",
                            color      = NeonGreen,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Action buttons ──────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Invest More — ElectricBlue gradient
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .shadow(
                                elevation    = 10.dp,
                                shape        = RoundedCornerShape(14.dp),
                                ambientColor = ElectricBlue.copy(0.35f),
                                spotColor    = ElectricBlue.copy(0.35f)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(ElectricBlueDeep, ElectricBlue)
                                )
                            )
                            .clickable { onInvest() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector        = Icons.Filled.Add,
                                contentDescription = null,
                                tint               = TextOnDark,
                                modifier           = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text       = "Invest More",
                                color      = TextOnDark,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 14.sp
                            )
                        }
                    }

                    // Withdraw — glass outline
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassWhite)
                            .border(
                                1.5.dp,
                                Brush.horizontalGradient(
                                    listOf(GlassBorder, GlassWhiteMid)
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onWithdraw() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector        = Icons.Filled.ArrowDownward,
                                contentDescription = null,
                                tint               = TextPrimary,
                                modifier           = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text       = "Withdraw",
                                color      = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 14.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Portfolio line chart ────────────────────────
                PundarLineChart(
                    dataPoints = SampleData.portfolioChartData,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════
//  Allocation Section
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AllocationSection(portfolio: Portfolio) {
    Column(modifier = Modifier.fillMaxWidth()) {

        Text(
            text       = "Allocation",
            fontWeight = FontWeight.Bold,
            fontSize   = 15.sp,
            color      = TextPrimary
        )

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(listOf(SpaceDeep, Color(0xFF0E1825)))
                )
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PundarAllocationBar(
                    label      = "PH Equities",
                    percentage = portfolio.phEquitiesPercent,
                    color      = ElectricBlue
                )
                PundarAllocationBar(
                    label      = "Fixed Income",
                    percentage = portfolio.fixedIncomePercent,
                    color      = NeonGreen
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Activity Section
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ActivitySection(activities: List<PortfolioActivity>) {
    Column(modifier = Modifier.fillMaxWidth()) {

        Text(
            text       = "Recent Activity",
            fontWeight = FontWeight.Bold,
            fontSize   = 15.sp,
            color      = TextPrimary
        )

        Spacer(Modifier.height(12.dp))

        activities.forEachIndexed { index, activity ->
            ActivityRow(activity = activity, index = index)
            if (index < activities.lastIndex) {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ActivityRow(activity: PortfolioActivity, index: Int) {
    // Staggered slide-in
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 80L)
        visible = true
    }

    val offsetY by animateFloatAsState(
        targetValue   = if (visible) 0f else 40f,
        animationSpec = tween(400, easing = EaseOutExpo),
        label         = "actSlide$index"
    )
    val alphaVal by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(350),
        label         = "actAlpha$index"
    )

    val iconColor = when (activity.type) {
        ActivityType.AUTO_SWEEP -> ElectricBlue
        ActivityType.DIVIDEND   -> PremiumGoldWarm
        else                    -> TextSecondary
    }
    val iconVector = when (activity.type) {
        ActivityType.AUTO_SWEEP -> Icons.Filled.Autorenew
        ActivityType.DIVIDEND   -> Icons.Filled.Stars
        ActivityType.PURCHASE   -> Icons.Filled.ShoppingCart
        ActivityType.ROUND_UP   -> Icons.Filled.ArrowUpward
        ActivityType.PAYOUT     -> Icons.Filled.AccountBalanceWallet
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(translationY = offsetY, alpha = alphaVal)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(listOf(SpaceDeep, Color(0xFF0E1825)))
            )
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon box
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.12f))
                .border(1.dp, iconColor.copy(0.25f), RoundedCornerShape(12.dp))
        ) {
            Icon(
                imageVector        = iconVector,
                contentDescription = null,
                tint               = iconColor,
                modifier           = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = activity.description,
                color      = TextPrimary,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Text(
                text     = activity.date,
                color    = TextTertiary,
                fontSize = 12.sp
            )
        }

        Text(
            text       = "${if (activity.isPositive) "+" else "-"}₱ ${
                String.format("%,.2f", activity.amount)
            }",
            color      = if (activity.isPositive) NeonGreen else ErrorRed,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


// ═══════════════════════════════════════════════════════════════
//  Holdings Section
// ═══════════════════════════════════════════════════════════════

@Composable
private fun HoldingsSection(
    holdings:       List<StockHolding>,
    onOptimize:     () -> Unit,
    onHoldingClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        // ── Section header with Optimize pill ──────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text       = "Holdings",
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp,
                color      = TextPrimary
            )
            // Optimize pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(ElectricBlue.copy(alpha = 0.12f))
                    .border(1.dp, ElectricBlue.copy(0.35f), RoundedCornerShape(50.dp))
                    .clickable { onOptimize() }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = Icons.Filled.AutoFixHigh,
                        contentDescription = null,
                        tint               = ElectricBlue,
                        modifier           = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text       = "Optimize",
                        color      = ElectricBlue,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Glass card wrapper ──────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(listOf(SpaceDeep, Color(0xFF0E1825)))
                )
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
        ) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpaceMedium.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text      = "Stock",
                    color     = TextSecondary,
                    fontSize  = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier  = Modifier.weight(2.5f)
                )
                Text(
                    text      = "Shares",
                    color     = TextSecondary,
                    fontSize  = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier  = Modifier.weight(1f)
                )
                Text(
                    text      = "Value",
                    color     = TextSecondary,
                    fontSize  = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier  = Modifier.weight(1.3f)
                )
                Text(
                    text      = "Return",
                    color     = TextSecondary,
                    fontSize  = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier  = Modifier.weight(1f)
                )
            }

            // Holding rows
            holdings.forEachIndexed { index, holding ->
                HoldingRow(
                    holding        = holding,
                    index          = index,
                    onHoldingClick = onHoldingClick
                )
                if (index < holdings.lastIndex) {
                    Divider(
                        color     = SpaceBorder,
                        thickness = 0.5.dp,
                        modifier  = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

// ── Avatar gradient colors cycling per ticker ───────────────────
private val avatarGradients = listOf(
    listOf(Color(0xFF0040C8), Color(0xFF00B4FF)),
    listOf(Color(0xFF7C3AED), Color(0xFFA855F7)),
    listOf(Color(0xFF065F46), Color(0xFF00FF87)),
    listOf(Color(0xFF92400E), Color(0xFFFFB830)),
    listOf(Color(0xFF1D4ED8), Color(0xFF00D4FF))
)

@Composable
private fun HoldingRow(
    holding:        StockHolding,
    index:          Int,
    onHoldingClick: (String) -> Unit
) {
    // Staggered entrance
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 100L)
        visible = true
    }

    val offsetX by animateFloatAsState(
        targetValue   = if (visible) 0f else -30f,
        animationSpec = tween(450, easing = EaseOutBack),
        label         = "holdSlide$index"
    )
    val alphaVal by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(350),
        label         = "holdAlpha$index"
    )

    val gradient = avatarGradients[index % avatarGradients.size]
    val returnColor = if (holding.returnPercent >= 0) NeonGreen else ErrorRed
    val returnPrefix = if (holding.returnPercent >= 0) "+" else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(translationX = offsetX, alpha = alphaVal)
            .clickable { onHoldingClick(holding.ticker) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ticker avatar circle + company name — weight 2.5
        Row(
            modifier          = Modifier.weight(2.5f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradient))
            ) {
                Text(
                    text       = holding.ticker.take(2),
                    color      = Color.White,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text       = holding.companyName,
                    color      = TextPrimary,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    text     = holding.ticker,
                    color    = TextTertiary,
                    fontSize = 11.sp
                )
            }
        }

        // Shares — weight 1
        Text(
            text     = "${holding.shares}",
            color    = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )

        // Value — weight 1.3
        Text(
            text       = "₱${String.format("%,.0f", holding.value)}",
            color      = TextPrimary,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.weight(1.3f)
        )

        // Return % — weight 1
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(returnColor.copy(alpha = 0.12f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text       = "$returnPrefix${holding.returnPercent}%",
                color      = returnColor,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
