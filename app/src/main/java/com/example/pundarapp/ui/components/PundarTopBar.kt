package com.example.pundarapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pundarapp.ui.screens.home.NotificationData
import com.example.pundarapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PundarMainTopBar(
    userName: String = "User",
    userInitials: String = "JD",
    pundarScore: Int = 850,
    onNotificationClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Hey, ${userName.split(" ").first()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Every transaction counts.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    letterSpacing = 0.3.sp
                )
            }
        },
        navigationIcon = {
            // Avatar with electric ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(start = 12.dp, end = 6.dp)
                    .size(42.dp)
                    .shadow(8.dp, CircleShape, ambientColor = ElectricBlue.copy(0.4f), spotColor = ElectricBlue.copy(0.4f))
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ElectricBlueDeep, ElectricPurple)
                        )
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SpaceDeep)
                ) {
                    Text(
                        text = userInitials,
                        color = ElectricBlue,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }
        },
        actions = {
            // Score pill
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(GoldGlow, PremiumGoldWarm.copy(0.10f))
                        )
                    )
                    .border(1.dp, PremiumGoldWarm.copy(0.4f), RoundedCornerShape(50.dp))
                    .padding(horizontal = 10.dp)
            ) {
                Row(
                    Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon3DStar(size = 14.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$pundarScore",
                        color = PremiumGoldWarm,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Notification
            IconButton(onClick = onNotificationClick) {
                if (NotificationData.hasUnread()) {
                    BadgedBox(badge = {
                        Badge(containerColor = ErrorRed)
                    }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Alerts", tint = TextSecondary)
                    }
                } else {
                    Icon(Icons.Filled.Notifications, contentDescription = "Alerts", tint = TextSecondary)
                }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = TextSecondary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SpaceDeep,
            titleContentColor = TextPrimary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PundarDetailTopBar(
    title: String = "",
    onBack: () -> Unit = {},
    showHelp: Boolean = false,
    showMoreOptions: Boolean = false,
    showNotifications: Boolean = false,
    showStar: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SpaceMedium)
                        .border(1.dp, SpaceBorder, CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        actions = {
            actions()
            if (showHelp) {
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.HelpOutline, contentDescription = "Help", tint = TextSecondary)
                }
            }
            if (showMoreOptions) {
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = TextSecondary)
                }
            }
            if (showNotifications) {
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = TextSecondary)
                }
            }
            if (showStar) {
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Star, contentDescription = "Favorite", tint = TextSecondary)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SpaceDeep,
            titleContentColor = TextPrimary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PundarCircleTopBar(
    onBack: () -> Unit = {},
    showNotifications: Boolean = true,
    onNotificationClick: () -> Unit = {},
    userInitials: String = "JD"
) {
    TopAppBar(
        title = {
            Text(
                text = "PUNDAR",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = TextPrimary,
                letterSpacing = 2.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
        },
        actions = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(ElectricBlueDeep, ElectricPurple))
                    )
            ) {
                Text(
                    text = userInitials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            if (showNotifications) {
                IconButton(onClick = onNotificationClick) {
                    if (NotificationData.hasUnread()) {
                        BadgedBox(badge = { Badge(containerColor = ErrorRed) }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = TextSecondary)
                        }
                    } else {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = TextSecondary)
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SpaceDeep,
            titleContentColor = TextPrimary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PundarGrowTopBar(
    userInitials: String = "JD",
    pundarScore: Int = 850,
    onNotificationClick: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(ElectricBlueDeep, ElectricPurple)))
                ) {
                    Text(text = userInitials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "PUNDAR",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = TextPrimary,
                    letterSpacing = 2.sp
                )
            }
        },
        actions = {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(GoldGlow)
                    .border(1.dp, PremiumGoldWarm.copy(0.4f), RoundedCornerShape(50.dp))
                    .padding(horizontal = 10.dp)
            ) {
                Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    Icon3DStar(size = 14.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$pundarScore",
                        color = PremiumGoldWarm,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(onClick = onNotificationClick) {
                if (NotificationData.hasUnread()) {
                    BadgedBox(badge = { Badge(containerColor = ErrorRed) }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = TextSecondary)
                    }
                } else {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = TextSecondary)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SpaceDeep,
            titleContentColor = TextPrimary
        )
    )
}
