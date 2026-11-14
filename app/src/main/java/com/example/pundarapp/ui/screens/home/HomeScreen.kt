package com.example.pundarapp.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import com.example.pundarapp.ui.data.BillStatus
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
    val isBalanceHidden = AppState.isBalanceHidden.value
    val fiatBalanceStr = if (isBalanceHidden) "₱ •••••" else "₱ ${String.format("%,.2f", walletBalance * exchangeRate)}"
    val xlmBalanceStr = if (isBalanceHidden) "••••• XLM" else "≈ ${String.format("%,.2f", walletBalance)} XLM"

    // Dashboard Filter State
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchField by remember { mutableStateOf(false) }
    val dashboardFilters = remember { listOf("All", "Received", "Sent", "Pay", "Circle", "Wallet") }

    val filteredActivities = remember(AppState.homeActivities.toList(), selectedFilter, searchQuery) {
        AppState.homeActivities.filter { act ->
            val matchesCategory = when (selectedFilter) {
                "All" -> true
                "Received" -> act.isPositive || act.amount.trim().startsWith("+")
                "Sent" -> !act.isPositive || act.amount.trim().startsWith("-")
                "Pay" -> act.module.equals("Pay", ignoreCase = true)
                "Circle" -> act.module.equals("Circle", ignoreCase = true)
                "Wallet" -> act.module.equals("Wallet", ignoreCase = true)
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                act.title.contains(searchQuery, ignoreCase = true) ||
                act.subtitle.contains(searchQuery, ignoreCase = true) ||
                act.module.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    // Automatically trigger onboarding Product Tour on first sign-in / registration
    LaunchedEffect(session?.id) {
        val uid = session?.id ?: session?.phone.orEmpty()
        if (uid.isNotBlank()) {
            AppState.checkAndTriggerProductTour(uid)
        }
    }

    if (AppState.showProductTour.value) {
        ProductTourDialog(
            userName = fullName,
            onFinish = { AppState.markProductTourCompleted() }
        )
    }

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
                // ── DEEP NAVY HERO HEADER ─────────────────────────────────
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                                    }
                                ) {
                                    PundarAvatar(
                                        initials = initials,
                                        imageUrl = session?.profileImageUrl,
                                        size = 46.dp,
                                        showRing = true,
                                        initialsFontSize = 16.sp,
                                        innerBackground = Color(0xFF1E2E5C)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Hello,",
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

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Product Tour / Walkthrough Icon Button
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E2E5C))
                                            .clickable { AppState.replayProductTour() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.HelpOutline,
                                            contentDescription = "Product Tour",
                                            tint = Color(0xFF60A5FA),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

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

                            // Available Balance Section with Hide/Show Amount Toggle
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Pill Badge + Hide Balance Toggle
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
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

                                    // Hide / Show Amount Pill Button
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFF1E2E5C))
                                            .clickable { AppState.toggleBalanceVisibility() }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isBalanceHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                                contentDescription = if (isBalanceHidden) "Show Amount" else "Hide Amount",
                                                tint = Color(0xFF60A5FA),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = if (isBalanceHidden) "Show" else "Hide",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF60A5FA)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                // Animated Main Amount with Eye Toggle
                                AnimatedContent(
                                    targetState = fiatBalanceStr,
                                    transitionSpec = {
                                        fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                                    },
                                    label = "balanceFade"
                                ) { targetBalance ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { AppState.toggleBalanceVisibility() }
                                    ) {
                                        Text(
                                            targetBalance,
                                            fontSize = 34.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Icon(
                                            imageVector = if (isBalanceHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = "Toggle Balance Visibility",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

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

                // ── UPCOMING OBLIGATIONS SECTION (Dynamic Only, No Hardcoded Splits) ──
                item {
                    val pendingBills = AppState.bills.filter { it.status != BillStatus.SETTLED }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateContentSize()
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
                                modifier = Modifier.clickable { navController.navigate(Routes.PAY) }
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        if (pendingBills.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEFF6FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                "All Caught Up",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                "No pending bill splits or obligations.",
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                    TextButton(onClick = { navController.navigate(Routes.PAY_NEW_BILL) }) {
                                        Text(
                                            "+ New Split",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2563EB),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            pendingBills.take(3).forEachIndexed { idx, bill ->
                                if (idx > 0) Spacer(Modifier.height(10.dp))
                                ObligationCard(
                                    title = bill.name,
                                    subtitle = "${bill.memberCount} members • ",
                                    highlightedText = bill.date,
                                    amountStr = if (isBalanceHidden) "₱ •••••" else "₱ ${String.format("%,.2f", bill.yourShare)}",
                                    statusStr = "Pending Pay",
                                    icon = Icons.Filled.Receipt,
                                    iconBgColor = Color(0xFFF3E8FF),
                                    iconColor = Color(0xFF8B5CF6),
                                    onClick = { navController.navigate(Routes.billDetail(bill.id)) }
                                )
                            }
                        }
                    }
                }

                // ── RECENT ACTIVITY SECTION WITH DASHBOARD FILTER ─────────
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateContentSize()
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showSearchField = !showSearchField }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FilterList,
                                        contentDescription = "Search & Filter",
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Filter",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                                Text(
                                    "View All",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier.clickable { navController.navigate(Routes.ALL_ACTIVITY) }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Filter Chips Row (All, Received, Sent, Pay, Circle, Wallet)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            dashboardFilters.forEach { filterName ->
                                val isSelected = selectedFilter == filterName
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) Color(0xFF0B1B3D) else Color.White,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF0B1B3D) else Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { selectedFilter = filterName }
                                ) {
                                    Text(
                                        text = filterName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        // Expandable Search / Keyword Filter Box
                        AnimatedVisibility(visible = showSearchField) {
                            Column {
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search activity by name or type...", fontSize = 13.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF64748B))
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color(0xFF64748B))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = Color(0xFF2563EB),
                                        unfocusedBorderColor = Color(0xFFE2E8F0),
                                        focusedTextColor = Color(0xFF0F172A),
                                        unfocusedTextColor = Color(0xFF0F172A)
                                    )
                                )
                            }
                        }
                    }
                }

                if (isLoadingActivities) {
                    item { ActivityShimmer() }
                } else if (filteredActivities.isEmpty()) {
                    item {
                        if (selectedFilter != "All" || searchQuery.isNotBlank()) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Filled.FilterAltOff,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "No activities match filter \"$selectedFilter\"${if (searchQuery.isNotBlank()) " • \"$searchQuery\"" else ""}.",
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    TextButton(onClick = {
                                        selectedFilter = "All"
                                        searchQuery = ""
                                    }) {
                                        Text("Reset Filter", fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                    }
                                }
                            }
                        } else {
                            EmptyActivity()
                        }
                    }
                } else {
                    itemsIndexed(
                        items = filteredActivities,
                        key = { idx, item -> "${item.referenceId}_${item.timestamp}_$idx" }
                    ) { idx, activity ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .animateItem()
                        ) {
                            ActivityRow(
                                activity = activity,
                                index = idx,
                                isHidden = isBalanceHidden,
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
private fun ActivityRow(
    activity: HomeActivity,
    index: Int,
    isHidden: Boolean = false,
    onClick: () -> Unit = {}
) {
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
                if (isHidden) "₱ •••••" else activity.amount,
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
