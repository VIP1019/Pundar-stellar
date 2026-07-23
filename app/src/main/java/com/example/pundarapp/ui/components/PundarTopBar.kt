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
import com.example.pundarapp.ui.theme.PundarTheme

private val TopBarBgDark = Color(0xF2090F1F)   // original dark header
private val TopBarBgLight = Color(0xFFFAFBFF)  // crisp white-blue for light mode

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
    val topBarBg = if (PundarTheme.colors.isLight) TopBarBgLight else TopBarBgDark
    val topBarBorderColor = if (PundarTheme.colors.isLight)
        Color(0xFFE2E8F0) // Subtle gray border in light mode
    else
        PundarTheme.colors.glassMedium
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(topBarBg)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, topBarBorderColor),
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
                        color = PundarTheme.colors.textPrimary
                    )
                    Text(
                        text = "Every transaction counts.",
                        style = MaterialTheme.typography.labelSmall,
                        color = PundarTheme.colors.textMuted,
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
                        .background(PundarTheme.colors.accentGold.copy(0.12f))
                        .border(1.dp, PundarTheme.colors.accentGold.copy(0.30f), RoundedCornerShape(50.dp))
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
                        BadgedBox(badge = { Badge(containerColor = PundarTheme.colors.accentRed) }) {
                            Icon(Icons.Filled.Notifications, "Alerts", tint = PundarTheme.colors.textSecondary)
                        }
                    } else {
                        Icon(Icons.Filled.Notifications, "Alerts", tint = PundarTheme.colors.textSecondary)
                    }
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Filled.Settings, "Settings", tint = PundarTheme.colors.textSecondary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = PundarTheme.colors.textPrimary
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
    val topBarBg = if (PundarTheme.colors.isLight) TopBarBgLight else TopBarBgDark
    val topBarBorderColor = if (PundarTheme.colors.isLight) Color(0xFFE2E8F0) else PundarTheme.colors.glassMedium
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(topBarBg)
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color.Transparent, topBarBorderColor), startY = 0f, endY = Float.POSITIVE_INFINITY),
                RectangleShape
            )
    ) {
        TopAppBar(
            title = {
                Text(title, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = PundarTheme.colors.textPrimary)
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(PundarTheme.colors.surfaceSecondary)
                            .border(1.dp, PundarTheme.colors.borderPrimary, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                            tint = PundarTheme.colors.textPrimary, modifier = Modifier.size(17.dp))
                    }
                }
            },
            actions = {
                actions()
                if (showHelp)          IconButton(onClick = {}) { Icon(Icons.Filled.Help,         "Help",  tint = PundarTheme.colors.textSecondary) }
                if (showMoreOptions)   IconButton(onClick = {}) { Icon(Icons.Filled.MoreVert,      "More",  tint = PundarTheme.colors.textSecondary) }
                if (showNotifications) IconButton(onClick = {}) { Icon(Icons.Filled.Notifications, "Notif", tint = PundarTheme.colors.textSecondary) }
                if (showStar)          IconButton(onClick = {}) { Icon(Icons.Filled.Star,           "Fav",   tint = PundarTheme.colors.textSecondary) }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent, titleContentColor = PundarTheme.colors.textPrimary)
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
    val topBarBg = if (PundarTheme.colors.isLight) TopBarBgLight else TopBarBgDark
    val topBarBorderColor = if (PundarTheme.colors.isLight) Color(0xFFE2E8F0) else PundarTheme.colors.glassMedium
    Box(
        modifier = Modifier.fillMaxWidth().background(topBarBg)
            .border(1.dp, Brush.verticalGradient(listOf(Color.Transparent, topBarBorderColor),
                startY = 0f, endY = Float.POSITIVE_INFINITY), RectangleShape)
    ) {
        TopAppBar(
            title = {
                Text("PUNDAR", fontWeight = FontWeight.Black, fontSize = 18.sp,
                    color = PundarTheme.colors.textPrimary, letterSpacing = 2.sp)
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PundarTheme.colors.textPrimary)
                }
            },
            actions = {
                PundarAvatar(userInitials, imageUrl = profileImageUrl, size = 34.dp,
                    showRing = true, initialsFontSize = 12.sp)
                if (showNotifications) {
                    IconButton(onClick = onNotificationClick) {
                        if (NotificationData.hasUnread())
                            BadgedBox(badge = { Badge(containerColor = PundarTheme.colors.accentRed) }) {
                                Icon(Icons.Filled.Notifications, "Notifications", tint = PundarTheme.colors.textSecondary)
                            }
                        else Icon(Icons.Filled.Notifications, "Notifications", tint = PundarTheme.colors.textSecondary)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = PundarTheme.colors.textPrimary)
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
    val topBarBg = if (PundarTheme.colors.isLight) TopBarBgLight else TopBarBgDark
    val topBarBorderColor = if (PundarTheme.colors.isLight) Color(0xFFE2E8F0) else PundarTheme.colors.glassMedium
    Box(
        modifier = Modifier.fillMaxWidth().background(topBarBg)
            .border(1.dp, Brush.verticalGradient(listOf(Color.Transparent, topBarBorderColor),
                startY = 0f, endY = Float.POSITIVE_INFINITY), RectangleShape)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PundarAvatar(userInitials, imageUrl = profileImageUrl, size = 34.dp,
                        showRing = true, initialsFontSize = 12.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("PUNDAR", fontWeight = FontWeight.Black, fontSize = 18.sp,
                        color = PundarTheme.colors.textPrimary, letterSpacing = 2.sp)
                }
            },
            actions = {
                Box(
                    modifier = Modifier.height(28.dp).clip(RoundedCornerShape(50.dp))
                        .background(PundarTheme.colors.accentGold.copy(0.12f))
                        .border(1.dp, PundarTheme.colors.accentGold.copy(0.30f), RoundedCornerShape(50.dp))
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
                        BadgedBox(badge = { Badge(containerColor = PundarTheme.colors.accentRed) }) {
                            Icon(Icons.Filled.Notifications, "Notifications", tint = PundarTheme.colors.textSecondary)
                        }
                    else Icon(Icons.Filled.Notifications, "Notifications", tint = PundarTheme.colors.textSecondary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = PundarTheme.colors.textPrimary)
        )
    }
}
