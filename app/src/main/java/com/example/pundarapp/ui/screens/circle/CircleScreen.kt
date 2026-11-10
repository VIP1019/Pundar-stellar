package com.example.pundarapp.ui.screens.circle

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.Circle
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme
import kotlinx.coroutines.launch

private val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleScreen(navController: NavController, openDrawer: () -> Unit = {}) {
    val initials    = AuthRepository.getCurrentUserInitials().ifBlank { "MJ" }
    val invitation  = AppState.pendingInvitation.value
    val totalSaved  = AppState.circles.sumOf { it.savedAmount }
    val activeCount = AppState.circles.count { it.isActive }

    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val requestedCircles = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        AppState.refreshCircles()
        AppState.refreshDiscoverableCircles()
    }

    Scaffold(
        floatingActionButton = {
            CircleFab { navController.navigate(Routes.CIRCLE_CREATE) }
        },
        containerColor = PundarTheme.colors.bgPrimary
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── DEEP NAVY HERO HEADER + FLOATING ACTION CARD ─────────
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                            .background(Color(0xFF0B1B3D))
                            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 44.dp)
                    ) {
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
                                        "PUNDAR Circle",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        "Paluwagan · Soroban Escrow",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                }
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

                        Spacer(Modifier.height(22.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(Color(0xFF1E2E5C))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
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
                                        "Total Pooled Savings",
                                        fontSize = 11.sp,
                                        color = Color(0xFFCBD5E1),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = AppState.formatFiat(totalSaved),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "$activeCount Active Groups · Soroban Verified",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF14254E))
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircleHeroStat(AppState.circles.size.toString(), "My Groups", Color(0xFF3B82F6))
                                Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.12f)))
                                CircleHeroStat(activeCount.toString(), "Active", Color(0xFF10B981))
                                Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.12f)))
                                CircleHeroStat(AppState.discoverableCircles.size.toString(), "Discover", Color(0xFFF59E0B))
                            }
                        }
                    }

                    // Floating White Action Card overlapping bottom of Navy Header
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .align(Alignment.BottomCenter)
                            .offset(y = 24.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = PundarTheme.colors.surfacePrimary,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, PundarTheme.colors.borderPrimary)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircleFloatingAction(
                                icon = Icons.Filled.AddCircle,
                                label = "Create Group",
                                tint = Color(0xFF10B981),
                                onClick = { navController.navigate(Routes.CIRCLE_CREATE) }
                            )
                            CircleFloatingAction(
                                icon = Icons.Filled.MailOutline,
                                label = "Requests",
                                tint = Color(0xFF2563EB),
                                onClick = { navController.navigate(Routes.REQUESTS) }
                            )
                            CircleFloatingAction(
                                icon = Icons.Filled.Star,
                                label = "Score",
                                tint = Color(0xFFF59E0B),
                                onClick = { navController.navigate(Routes.SCORE) }
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            if (invitation != null) {
                item {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        InvitationBanner(circleName = invitation.circleName) {
                            navController.navigate(Routes.CIRCLE_INVITE)
                        }
                    }
                }
            }

            item {
                Text(
                    "My Paluwagan Groups",
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 17.sp,
                    color         = PundarTheme.colors.textPrimary,
                    modifier      = Modifier.padding(horizontal = 20.dp)
                )
            }

            if (AppState.circles.isEmpty()) {
                item {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        CircleEmptyState()
                    }
                }
            } else {
                itemsIndexed(AppState.circles.toList()) { idx, circle ->
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        CircleCard(circle = circle, index = idx) {
                            navController.navigate(Routes.circleDetail(circle.id))
                        }
                    }
                }
            }

            if (AppState.discoverableCircles.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(6.dp)) }
                item {
                    Text(
                        "Discover Paluwagan Groups",
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 17.sp,
                        color         = PundarTheme.colors.textPrimary,
                        modifier      = Modifier.padding(horizontal = 20.dp)
                    )
                }
                itemsIndexed(AppState.discoverableCircles.toList()) { _, circle ->
                    val hasRequested = requestedCircles.contains(circle.id)
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        DiscoverCircleCard(
                            circle = circle,
                            hasRequested = hasRequested,
                            onRequestJoin = {
                                if (hasRequested) return@DiscoverCircleCard
                                coroutineScope.launch {
                                    val res = AppState.requestToJoinCircle(circle.id)
                                    if (res.isSuccess) {
                                        requestedCircles.add(circle.id)
                                        android.widget.Toast.makeText(context, "Request sent to join ${circle.name}", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Failed to send request", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CircleHeroStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8))
    }
}

@Composable
private fun CircleFloatingAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = PundarTheme.colors.textPrimary
        )
    }
}

// ── Clean Navy FAB ────────────────────────────────────────────────
@Composable
private fun CircleFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick        = onClick,
        containerColor = Color(0xFF0B1B3D),
        contentColor   = Color.White,
        shape          = RoundedCornerShape(18.dp)
    ) {
        Icon(Icons.Filled.AddCircleOutline, contentDescription = "New Circle", tint = Color.White)
        Spacer(Modifier.width(8.dp))
        Text(
            "Create Paluwagan",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}

// ── Invitation Banner ─────────────────────────────────────────────
@Composable
private fun InvitationBanner(circleName: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = PundarTheme.colors.surfacePrimary,
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(0.45f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF59E0B).copy(0.14f)),
                contentAlignment = Alignment.Center
            ) { Icon3DBell(size = 32.dp, pulse = true) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Pending Invitation",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD97706),
                    fontSize = 14.sp
                )
                Text(circleName, style = MaterialTheme.typography.bodySmall, color = PundarTheme.colors.textSecondary)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
        }
    }
}

// ── Clean White Circle Card ───────────────────────────────────────
@Composable
private fun CircleCard(circle: Circle, index: Int, onClick: () -> Unit) {
    val progress     = (circle.savedAmount / circle.targetAmount).toFloat().coerceIn(0f, 1f)
    val accentColor  = if (circle.isActive) Color(0xFF2563EB) else Color(0xFF10B981)

    val animProg by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(900, delayMillis = index * 80 + 200, easing = EaseOutExpo),
        label         = "circleProgress_$index"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = PundarTheme.colors.surfacePrimary,
        border = BorderStroke(1.dp, PundarTheme.colors.borderPrimary)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            if (circle.isActive) "ACTIVE" else "COMPLETED",
                            fontSize      = 10.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color         = accentColor
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Filled.Group, null, tint = PundarTheme.colors.textMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "${circle.memberCount} / ${circle.maxMembers}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PundarTheme.colors.textMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (circle.isFull) {
                        Spacer(Modifier.width(6.dp))
                        Text("Full", fontSize = 9.sp, color = ErrorRed, fontWeight = FontWeight.Bold)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (circle.currency == "XLM") "${String.format("%,.0f", circle.targetAmount)} XLM" else "${String.format("%,.0f", circle.targetAmount)} ${circle.currency}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 16.sp,
                        color      = accentColor
                    )
                    Text(
                        if (circle.currency == "XLM") AppState.formatFiat(circle.targetAmount) else "~ ${String.format("%,.2f", circle.targetAmount / AppState.currentExchangeRate.doubleValue.coerceAtLeast(0.0001))} XLM",
                        fontWeight = FontWeight.Medium,
                        fontSize   = 10.sp,
                        color      = PundarTheme.colors.textMuted
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(circle.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PundarTheme.colors.textPrimary)

            Spacer(Modifier.height(14.dp))

            Box(
                Modifier.fillMaxWidth().height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PundarTheme.colors.surfaceTertiary)
            ) {
                Box(
                    Modifier.fillMaxHeight()
                        .fillMaxWidth(animProg)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(listOf(accentColor, accentColor.copy(0.65f)))
                        )
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${(animProg * 100).toInt()}% Completed",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = PundarTheme.colors.textMuted,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (circle.currency == "XLM") "${String.format("%,.2f", circle.targetAmount - circle.savedAmount)} XLM left" else "${circle.currency} ${String.format("%,.0f", circle.targetAmount - circle.savedAmount)} left",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = PundarTheme.colors.textMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(14.dp))

            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(0.08f))
                    .border(1.dp, accentColor.copy(0.20f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Total Saved", style = MaterialTheme.typography.labelMedium, color = PundarTheme.colors.textSecondary)
                    Text(
                        if (circle.currency == "XLM") "${String.format("%,.2f", circle.savedAmount)} XLM" else "${circle.currency} ${String.format("%,.0f", circle.savedAmount)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 15.sp,
                        color      = accentColor
                    )
                }
            }
        }
    }
}

// ── Empty state card ──────────────────────────────────────────────
@Composable
private fun CircleEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = PundarTheme.colors.surfacePrimary,
        border = BorderStroke(1.dp, PundarTheme.colors.borderPrimary)
    ) {
        Column(
            modifier = Modifier.padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🌀", fontSize = 36.sp)
            Spacer(Modifier.height(10.dp))
            Text("No Paluwagan groups yet.", fontWeight = FontWeight.SemiBold, color = PundarTheme.colors.textSecondary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap \"Create Paluwagan\" to start your savings circle.",
                style     = MaterialTheme.typography.bodySmall,
                color     = PundarTheme.colors.textMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Discover Circle Card ───────────────────────────────────────────
@Composable
fun DiscoverCircleCard(circle: Circle, hasRequested: Boolean = false, onRequestJoin: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = PundarTheme.colors.surfacePrimary,
        border = BorderStroke(1.dp, PundarTheme.colors.borderPrimary)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = circle.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = PundarTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onRequestJoin,
                    enabled = !hasRequested,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasRequested) PundarTheme.colors.surfaceTertiary else Color(0xFF10B981).copy(0.14f),
                        contentColor = if (hasRequested) PundarTheme.colors.textSecondary else Color(0xFF059669),
                        disabledContainerColor = PundarTheme.colors.surfaceTertiary,
                        disabledContentColor = PundarTheme.colors.textSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(if (hasRequested) "Requested" else "Request to Join", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PeopleAlt, null, tint = PundarTheme.colors.textMuted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "${circle.memberCount} / ${circle.maxMembers} members",
                    style = MaterialTheme.typography.labelMedium,
                    color = PundarTheme.colors.textMuted
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AccountBalanceWallet, null, tint = PundarTheme.colors.textMuted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Goal: ₱${String.format("%,.0f", circle.targetAmount)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = PundarTheme.colors.textMuted
                )
            }
        }
    }
}
