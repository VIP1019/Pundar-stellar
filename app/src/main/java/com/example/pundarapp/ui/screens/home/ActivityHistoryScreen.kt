package com.example.pundarapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.HomeActivity
import com.example.pundarapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHistoryScreen(navController: NavController) {
    var selectedActivity by remember { mutableStateOf<HomeActivity?>(null) }

    Scaffold(
        topBar = {
            PundarDetailTopBar(title = "All Activity", onBack = { navController.navigateUp() })
        },
        containerColor = PundarTheme.colors.bgPrimary
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (AppState.homeActivities.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No recent activities.", color = PundarTheme.colors.textMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    itemsIndexed(AppState.homeActivities) { _, activity ->
                        ActivityRowClickable(
                            activity = activity,
                            onClick = { selectedActivity = activity }
                        )
                    }
                }
            }
        }

        // Receipt Dialog
        selectedActivity?.let { activity ->
            ReceiptDialog(
                activity = activity,
                onDismiss = { selectedActivity = null }
            )
        }
    }
}

@Composable
private fun ActivityRowClickable(activity: HomeActivity, onClick: () -> Unit) {
    val accent = when (activity.module) {
        "Pay"    -> Blue400
        "Circle" -> PundarTheme.colors.accentGold
        "Grow"   -> PundarTheme.colors.accentGreen
        "Wallet" -> PundarTheme.colors.brandLight
        else     -> PundarTheme.colors.textMuted
    }
    val icon = when (activity.module) {
        "Pay"    -> Icons.Filled.Receipt
        "Circle" -> Icons.Filled.Groups
        "Grow"   -> Icons.AutoMirrored.Filled.TrendingUp
        "Wallet" -> Icons.Filled.AccountBalanceWallet
        else     -> Icons.Filled.Info
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(0.1f))
                .border(1.dp, accent.copy(0.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.title,
                color = PundarTheme.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = activity.subtitle,
                color = PundarTheme.colors.textMuted,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        Text(
            text = activity.amount,
            color = if (activity.isPositive) PundarTheme.colors.accentGreen else PundarTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

