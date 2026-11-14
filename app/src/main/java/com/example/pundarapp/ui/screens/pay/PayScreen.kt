package com.example.pundarapp.ui.screens.pay

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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.pundarapp.ui.data.BillStatus
import com.example.pundarapp.ui.data.parseCategorizedTitle
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.PundarTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        AppState.reconcileSettledBillsFromActivities(AppState.homeActivities.toList())
        AppState.refreshBills()
    }
    val bills         = AppState.bills.distinctBy { it.id }
    val settledCount  = bills.count { it.status == BillStatus.SETTLED }
    val pendingCount  = bills.count { it.status == BillStatus.PENDING || it.status == BillStatus.PARTIAL }
    val monthTotal    = bills.sumOf { it.yourShare }
    val userSession by AuthRepository.currentUserState
    val initials      = AuthRepository.getCurrentUserInitials().ifBlank { "MJ" }

    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()

    Scaffold(
        floatingActionButton = { PayFab { navController.navigate(Routes.PAY_NEW_BILL) } },
        containerColor = PundarTheme.colors.bgPrimary
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch {
                    AppState.refreshBills()
                    isRefreshing = false
                }
            },
            state = pullState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
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
                            // Top Bar Row inside Navy Hero
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                                    }
                                ) {
                                    PundarAvatar(
                                        initials = initials,
                                        imageUrl = userSession?.profileImageUrl,
                                        size = 46.dp,
                                        showRing = true,
                                        initialsFontSize = 16.sp,
                                        innerBackground = Color(0xFF1E2E5C)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "PUNDAR Pay",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            "Intelligent Settlement · Stellar Rails",
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

                            // Monthly Spend Center Display
                            val isHidden = AppState.isBalanceHidden.value
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                                "Monthly Group Spend",
                                                fontSize = 11.sp,
                                                color = Color(0xFFCBD5E1),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50.dp))
                                            .background(Color(0xFF1E2E5C))
                                            .clickable { AppState.toggleBalanceVisibility() }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                                contentDescription = "Toggle Amount Visibility",
                                                tint = Color(0xFF60A5FA),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = if (isHidden) "Show" else "Hide",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF60A5FA)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = if (isHidden) "₱ •••••" else "₱ ${String.format("%,.2f", monthTotal)}",
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.clickable { AppState.toggleBalanceVisibility() }
                                )
                                val rate = AppState.getPhpRate()
                                Text(
                                    text = if (isHidden) "••••• XLM · Stellar Rail" else "≈ ${String.format("%,.2f", monthTotal / rate)} XLM · Stellar Rail",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Spacer(Modifier.height(20.dp))

                            // Inner Stats Card inside Navy Hero (matches Score card on HomeScreen)
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
                                    PayHeroStat(settledCount, "Settled", Color(0xFF10B981))
                                    Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.12f)))
                                    PayHeroStat(pendingCount, "Pending", Color(0xFFF59E0B))
                                    Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.12f)))
                                    PayHeroStat(bills.size, "Total Bills", Color(0xFF3B82F6))
                                }
                            }
                        }

                        // Floating White Action Pill overlapping bottom of Navy Header
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
                                PayFloatingActionItem(
                                    icon = Icons.Filled.PersonAdd,
                                    label = "New Bill",
                                    tint = Color(0xFF2563EB),
                                    onClick = { navController.navigate(Routes.PAY_NEW_BILL) }
                                )
                                PayFloatingActionItem(
                                    icon = Icons.Filled.QrCodeScanner,
                                    label = "Scan QR",
                                    tint = Color(0xFF10B981),
                                    onClick = { navController.navigate(Routes.SCAN) }
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recent Splits",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = PundarTheme.colors.textPrimary
                        )
                        Text(
                            "${bills.size} bills",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PundarTheme.colors.textMuted
                        )
                    }
                }

                if (bills.isEmpty()) {
                    item {
                        Box(Modifier.padding(horizontal = 20.dp)) {
                            PayEmptyState()
                        }
                    }
                } else {
                    itemsIndexed(bills.toList()) { idx, bill ->
                        Box(Modifier.padding(horizontal = 20.dp)) {
                            BillCard(
                                bill = bill,
                                onClick = { navController.navigate(Routes.billDetail(bill.id)) }
                            )
                        }
                    }
                }

                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 6.dp, start = 20.dp, end = 20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Verified, null, tint = Color(0xFF2563EB), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Settlement builds your PUNDAR Score.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PundarTheme.colors.textMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PayHeroStat(count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            count.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            label,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun PayFloatingActionItem(
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
private fun PayFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick          = onClick,
        containerColor   = Color(0xFF0B1B3D),
        contentColor     = Color.White,
        shape            = RoundedCornerShape(18.dp)
    ) {
        Icon(Icons.Filled.AddCircleOutline, "New Bill", tint = Color.White)
        Spacer(Modifier.width(8.dp))
        Text(
            "New Group Bill",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}

// ── Clean White Bill Card ─────────────────────────────────────────
@Composable
private fun BillCard(bill: com.example.pundarapp.ui.data.GroupBill, onClick: () -> Unit) {
    val accent = when (bill.status) {
        BillStatus.SETTLED -> Color(0xFF10B981)
        BillStatus.PENDING -> Color(0xFFF59E0B)
        BillStatus.PARTIAL -> Color(0xFF2563EB)
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
    val (cleanTitle, category) = remember(bill.name) {
        parseCategorizedTitle(bill.name)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = PundarTheme.colors.surfacePrimary,
        border = BorderStroke(1.dp, PundarTheme.colors.borderPrimary)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(statusIcon, null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    cleanTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PundarTheme.colors.textPrimary
                )
                if (!category.isNullOrBlank()) {
                    Spacer(Modifier.height(1.dp))
                    Text(
                        category,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = PundarTheme.colors.textSecondary
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(accent.copy(0.13f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        " · ${bill.memberCount} members · ${bill.date}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PundarTheme.colors.textMuted
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            val isHidden = AppState.isBalanceHidden.value
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (isHidden) "₱ •••••" else "₱${String.format("%,.2f", bill.totalAmount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PundarTheme.colors.textPrimary
                )
                Text(
                    if (isHidden) "Your: ₱ •••••" else "Your: ₱${String.format("%,.2f", bill.yourShare)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PundarTheme.colors.textMuted
                )
            }
        }
    }
}

@Composable
private fun PayEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = PundarTheme.colors.surfacePrimary,
        border = BorderStroke(1.dp, PundarTheme.colors.borderPrimary)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon3DReceipt(size = 56.dp)
            Spacer(Modifier.height(10.dp))
            Text("No bills yet.", fontWeight = FontWeight.SemiBold, color = PundarTheme.colors.textSecondary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap \"New Group Bill\" to get started.",
                style = MaterialTheme.typography.bodySmall,
                color = PundarTheme.colors.textMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
