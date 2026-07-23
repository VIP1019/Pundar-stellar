package com.example.pundarapp.ui.components

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pundarapp.ui.screens.home.NotificationData
import com.example.pundarapp.ui.theme.*

private val TopBarBg = Color(0xF2090F1F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PundarMainTopBar(
    userName: String = "User",
    userInitials: String = "JD",
    profileImageUrl: String? = null,
    pundarScore: Int = 850,
    onNotificationClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(TopBarBg)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Glass15),
                    startY = 0f, endY = Float.POSITIVE_INFINITY
                ),
                shape = RectangleShape
            )
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Hey, ${userName.split(" ").first()} 👋",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextWhite
                    )
                    Text(
                        text = "Every transaction counts.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        letterSpacing = 0.2.sp
                    )
                }
            },
            navigationIcon = {
                PundarAvatar(
                    initials = userInitials,
                    imageUrl = profileImageUrl,
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                    size = 40.dp,
                    showRing = true
                )
            },
            actions = {
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Gold500.copy(0.12f))
                        .border(1.dp, Gold500.copy(0.30f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 10.dp)
                ) {
                    Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                        Icon3DStar(size = 12.dp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "$pundarScore",
                            color = Gold400,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(onClick = onNotificationClick) {
                    if (NotificationData.hasUnread()) {
                        BadgedBox(badge = { Badge(containerColor = Red500) }) {
                            Icon(Icons.Filled.Notifications, "Alerts", tint = TextSoft)
                        }
                    } else {
                        Icon(Icons.Filled.Notifications, "Alerts", tint = TextSoft)
                    }
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Filled.Settings, "Settings", tint = TextSoft)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = TextWhite
            )
        )
    }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(TopBarBg)
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color.Transparent, Glass15), startY = 0f, endY = Float.POSITIVE_INFINITY),
                RectangleShape
            )
    ) {
        TopAppBar(
            title = {
                Text(title, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = TextWhite)
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Navy700)
                            .border(1.dp, NavyBorder, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                            tint = TextWhite, modifier = Modifier.size(17.dp))
                    }
                }
            },
            actions = {
                actions()
                if (showHelp)          IconButton(onClick = {}) { Icon(Icons.Filled.Help,         "Help",  tint = TextSoft) }
                if (showMoreOptions)   IconButton(onClick = {}) { Icon(Icons.Filled.MoreVert,      "More",  tint = TextSoft) }
                if (showNotifications) IconButton(onClick = {}) { Icon(Icons.Filled.Notifications, "Notif", tint = TextSoft) }
                if (showStar)          IconButton(onClick = {}) { Icon(Icons.Filled.Star,           "Fav",   tint = TextSoft) }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent, titleContentColor = TextWhite)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PundarCircleTopBar(
    onBack: () -> Unit = {},
    showNotifications: Boolean = true,
    onNotificationClick: () -> Unit = {},
    userInitials: String = "JD",
    profileImageUrl: String? = null
) {
    Box(
        modifier = Modifier.fillMaxWidth().background(TopBarBg)
            .border(1.dp, Brush.verticalGradient(listOf(Color.Transparent, Glass15),
                startY = 0f, endY = Float.POSITIVE_INFINITY), RectangleShape)
    ) {
        TopAppBar(
            title = {
                Text("PUNDAR", fontWeight = FontWeight.Black, fontSize = 18.sp,
                    color = TextWhite, letterSpacing = 2.sp)
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextWhite)
                }
            },
            actions = {
                PundarAvatar(userInitials, imageUrl = profileImageUrl, size = 34.dp,
                    showRing = true, initialsFontSize = 12.sp)
                if (showNotifications) {
                    IconButton(onClick = onNotificationClick) {
                        if (NotificationData.hasUnread())
                            BadgedBox(badge = { Badge(containerColor = Red500) }) {
                                Icon(Icons.Filled.Notifications, "Notifications", tint = TextSoft)
                            }
                        else Icon(Icons.Filled.Notifications, "Notifications", tint = TextSoft)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = TextWhite)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PundarGrowTopBar(
    userInitials: String = "JD",
    profileImageUrl: String? = null,
    pundarScore: Int = 850,
    onNotificationClick: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxWidth().background(TopBarBg)
            .border(1.dp, Brush.verticalGradient(listOf(Color.Transparent, Glass15),
                startY = 0f, endY = Float.POSITIVE_INFINITY), RectangleShape)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PundarAvatar(userInitials, imageUrl = profileImageUrl, size = 34.dp,
                        showRing = true, initialsFontSize = 12.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("PUNDAR", fontWeight = FontWeight.Black, fontSize = 18.sp,
                        color = TextWhite, letterSpacing = 2.sp)
                }
            },
            actions = {
                Box(
                    modifier = Modifier.height(28.dp).clip(RoundedCornerShape(50.dp))
                        .background(Gold500.copy(0.12f))
                        .border(1.dp, Gold500.copy(0.30f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 10.dp)
                ) {
                    Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                        Icon3DStar(size = 12.dp)
                        Spacer(Modifier.width(4.dp))
                        Text("$pundarScore", color = Gold400,
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
                IconButton(onClick = onNotificationClick) {
                    if (NotificationData.hasUnread())
                        BadgedBox(badge = { Badge(containerColor = Red500) }) {
                            Icon(Icons.Filled.Notifications, "Notifications", tint = TextSoft)
                        }
                    else Icon(Icons.Filled.Notifications, "Notifications", tint = TextSoft)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = TextWhite)
        )
    }
}
