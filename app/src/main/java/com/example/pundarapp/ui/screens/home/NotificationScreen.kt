package com.example.pundarapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AppNotification
import com.example.pundarapp.ui.components.PundarCard
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private fun formatTimestamp(ts: Long): String {
    if (ts <= 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - ts
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} min ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} hrs ago"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)} days ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(ts))
    }
}

private fun iconForKind(kind: String) = when (kind.uppercase()) {
    "SAVINGS" -> Icons.Filled.Savings
    "GOAL" -> Icons.Filled.Flag
    "INVESTMENT" -> Icons.Filled.TrendingUp
    "BILL" -> Icons.Filled.Receipt
    "SECURITY" -> Icons.Filled.Security
    else -> Icons.Filled.Notifications
}

@Composable
private fun colorForKind(kind: String) = when (kind.uppercase()) {
    "SAVINGS" -> NeonGreen
    "GOAL" -> PremiumGoldWarm
    "INVESTMENT" -> ElectricBlue
    "BILL" -> WarningAmber
    "SECURITY" -> ErrorRed
    else -> PundarTheme.colors.brandPrimary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(navController: NavController) {
    val notifications = AppState.recentNotifications
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()
    val unreadCount = AppState.unreadNotificationCount()

    LaunchedEffect(Unit) {
        AppState.refreshNotifications()
    }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Notifications",
                onBack = { navController.navigateUp() },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(onClick = { AppState.markAllNotificationsRead() }) {
                            Text("Mark all read", color = ElectricBlue)
                        }
                    }
                }
            )
        },
        containerColor = PundarTheme.colors.bgPrimary
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch {
                    AppState.refreshNotifications()
                    isRefreshing = false
                }
            },
            state = pullState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (notifications.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.NotificationsNone,
                                contentDescription = null,
                                tint = PundarTextTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "No notifications yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PundarTheme.colors.textSecondary
                            )
                        }
                    }
                } else {
                    items(notifications, key = { it.id }) { notification ->
                        NotificationRow(
                            notification = notification,
                            onClick = { AppState.markNotificationRead(notification.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: AppNotification,
    onClick: () -> Unit
) {
    val accent = colorForKind(notification.kind)
    val icon = iconForKind(notification.kind)

    PundarCard(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (!notification.isRead) PundarBlueSubtle else Color.Transparent)
                .padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f))
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (!notification.isRead) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = PundarTheme.colors.textPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = PundarTheme.colors.textSecondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(notification.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = PundarTextTertiary
                )
            }
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PundarTheme.colors.brandPrimary)
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}

/** Backward-compatible helper for top bar badge checks. */
object NotificationData {
    fun hasUnread(): Boolean = AppState.unreadNotificationCount() > 0
}
