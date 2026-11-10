package com.example.pundarapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

private val HeroNavyBg = Color(0xFF0B1B3D)      // Signature Deep Dark Navy header
private val HeroNavyButton = Color(0xFF1E2E5C)  // Circular dark-navy icon container
private val HeroNavySub = Color(0xFF94A3B8)     // Muted slate subtitle on navy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PundarMainTopBar(
    userName: String = "User",
    userInitials: String = "JD",
    profileImageUrl: String? = null,
    pundarScore: Int = 850,
    onScoreClick: () -> Unit = {},
    onRequestClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(HeroNavyBg)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Kamusta,",
                        fontSize = 11.sp,
                        color = HeroNavySub
                    )
                    Text(
                        text = userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                }
            },
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 6.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(HeroNavyButton)
                        .border(1.5.dp, Color(0xFF3B82F6), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        userInitials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            },
            actions = {
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(HeroNavyButton)
                        .border(1.dp, Color(0xFFF59E0B).copy(0.45f), RoundedCornerShape(50.dp))
                        .clickable { onScoreClick() }
                        .padding(horizontal = 10.dp)
                ) {
                    Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                        Icon3DStar(size = 12.dp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "$pundarScore",
                            color = Gold400,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = onRequestClick,
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(HeroNavyButton)
                ) {
                    if (com.example.pundarapp.ui.data.AppState.unifiedRequests.isNotEmpty()) {
                        BadgedBox(badge = { Badge(containerColor = Color(0xFF10B981)) }) {
                            Icon(Icons.Filled.MailOutline, "Requests", tint = Color.White, modifier = Modifier.size(19.dp))
                        }
                    } else {
                        Icon(Icons.Filled.MailOutline, "Requests", tint = Color.White, modifier = Modifier.size(19.dp))
                    }
                }
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(HeroNavyButton)
                ) {
                    if (NotificationData.hasUnread()) {
                        BadgedBox(badge = { Badge(containerColor = Color(0xFF10B981)) }) {
                            Icon(Icons.Filled.Notifications, "Alerts", tint = Color.White, modifier = Modifier.size(19.dp))
                        }
                    } else {
                        Icon(Icons.Filled.Notifications, "Alerts", tint = Color.White, modifier = Modifier.size(19.dp))
                    }
                }
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.padding(end = 8.dp).size(38.dp).clip(CircleShape).background(HeroNavyButton)
                ) {
                    Icon(Icons.Filled.Settings, "Settings", tint = Color.White, modifier = Modifier.size(19.dp))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White
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
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(HeroNavyBg)
    ) {
        TopAppBar(
            title = {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HeroNavyButton)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            actions = {
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    actions()
                    if (showHelp) IconButton(onClick = {}) { Icon(Icons.Filled.Help, "Help", tint = Color.White) }
                    if (showMoreOptions) IconButton(onClick = {}) { Icon(Icons.Filled.MoreVert, "More", tint = Color.White) }
                    if (showNotifications) IconButton(onClick = {}) { Icon(Icons.Filled.Notifications, "Notif", tint = Color.White) }
                    if (showStar) IconButton(onClick = {}) { Icon(Icons.Filled.Star, "Fav", tint = Color.White) }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            )
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(HeroNavyBg)
    ) {
        TopAppBar(
            title = {
                Text(
                    "PUNDAR",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(HeroNavyButton)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            },
            actions = {
                PundarAvatar(
                    userInitials,
                    imageUrl = profileImageUrl,
                    size = 34.dp,
                    showRing = true,
                    initialsFontSize = 12.sp
                )
                if (showNotifications) {
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier.padding(end = 8.dp).size(38.dp).clip(CircleShape).background(HeroNavyButton)
                    ) {
                        if (NotificationData.hasUnread())
                            BadgedBox(badge = { Badge(containerColor = Color(0xFF10B981)) }) {
                                Icon(Icons.Filled.Notifications, "Notifications", tint = Color.White, modifier = Modifier.size(19.dp))
                            }
                        else Icon(Icons.Filled.Notifications, "Notifications", tint = Color.White, modifier = Modifier.size(19.dp))
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White)
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
    onBack: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(HeroNavyBg)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PundarAvatar(
                        userInitials,
                        imageUrl = profileImageUrl,
                        size = 34.dp,
                        showRing = true,
                        initialsFontSize = 12.sp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "PUNDAR Grow",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onMenuClick) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(HeroNavyButton)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            actions = {
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(HeroNavyButton)
                        .border(1.dp, Color(0xFFF59E0B).copy(0.45f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 10.dp)
                ) {
                    Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                        Icon3DStar(size = 12.dp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "$pundarScore",
                            color = Gold400,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier.padding(end = 8.dp).size(38.dp).clip(CircleShape).background(HeroNavyButton)
                ) {
                    if (NotificationData.hasUnread())
                        BadgedBox(badge = { Badge(containerColor = Color(0xFF10B981)) }) {
                            Icon(Icons.Filled.Notifications, "Notifications", tint = Color.White, modifier = Modifier.size(19.dp))
                        }
                    else Icon(Icons.Filled.Notifications, "Notifications", tint = Color.White, modifier = Modifier.size(19.dp))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White)
        )
    }
}
