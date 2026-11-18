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
import com.example.pundarapp.ui.components.*
import androidx.compose.ui.graphics.Brush
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
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()

    Scaffold(
        containerColor = Color(0xFFF5F7FB),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
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
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── PAY DASHBOARD HEADER ────────────────────────────────
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F7FB))
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF10284B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.ReceiptLong,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "PUNDAR Pay",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50.dp))
                                                    .background(Color(0xFFEAF2FF))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF2563EB))
                                                    )
                                                    Spacer(Modifier.width(3.dp))
                                                    Text(
                                                        "Stellar Rail",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF2563EB)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            "Smart Splits & Settlements",
                                            fontSize = 9.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF1F5F9))
                                            .clickable { navController.navigate(Routes.NOTIFICATIONS) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Notifications,
                                            contentDescription = "Notifications",
                                            tint = Color(0xFF334155),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF1F5F9))
                                            .clickable { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Settings,
                                            contentDescription = "Settings",
                                            tint = Color(0xFF334155),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        val isHidden = AppState.isBalanceHidden.value
                        val rate = AppState.getPhpRate()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF142E5D), Color(0xFF0B1426))
                                    )
                                )
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = Color.White.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF34D399))
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "MONTHLY GROUP SPEND",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (isHidden) "₱ •••••" else "₱ ${String.format("%,.2f", monthTotal)}",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.clickable { AppState.toggleBalanceVisibility() }
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (isHidden) "••••• XLM" else "≈ ${String.format("%,.2f", monthTotal / rate)} XLM",
                                fontSize = 10.sp,
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.clickable { AppState.toggleBalanceVisibility() }
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.07f))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PayHeroStat(settledCount, "Settled", Color(0xFF10B981))
                                Box(Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.10f)))
                                PayHeroStat(pendingCount, "Pending", Color(0xFFF59E0B))
                                Box(Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.10f)))
                                PayHeroStat(bills.size, "Total Bills", Color(0xFF3B82F6))
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clickable { navController.navigate(Routes.PAY_NEW_BILL) },
                                shape = RoundedCornerShape(16.dp),
                                shadowElevation = 4.dp,
                                color = Color(0xFF193A83),
                                contentColor = Color.White
                            ) {
                                Box(
                                    modifier = Modifier.background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF203F8A), Color(0xFF142F6A))
                                        )
                                    )
                                ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.PersonAdd, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text("New Bill", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Filled.ChevronRight, null, tint = Color(0xFFB7CBEC), modifier = Modifier.size(18.dp))
                                }
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clickable { navController.navigate(Routes.SCAN) },
                                shape = RoundedCornerShape(16.dp),
                                shadowElevation = 1.dp,
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFEAF2FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.QrCodeScanner, null, tint = Color(0xFF2563EB), modifier = Modifier.size(17.dp))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text("Scan QR", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Filled.ChevronRight, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                fontSize = 9.sp,
                color = Color(0xFFCBD5E1)
            )
        }
        Text(
            count.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
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
