package com.example.pundarapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.pundarapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PundarMainTopBar(
    userName: String = "User",
    userInitials: String = "JD",
    pundarScore: Int = 850,
    onNotificationClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Magandang gabi, ${userName.split(" ").first()}! 👋",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PundarTextPrimary
                )
                Text(
                    text = "\"Every Transaction Counts.\"",
                    style = MaterialTheme.typography.labelSmall,
                    color = PundarTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        navigationIcon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(start = 12.dp, end = 8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PundarBlue)
            ) {
                Text(
                    text = userInitials,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        actions = {
            // PUNDAR Score chip
            Surface(
                shape = CircleShape,
                color = PundarTextPrimary,
                modifier = Modifier.height(32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Score",
                        tint = PundarGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Score: $pundarScore",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = PundarSurface,
            titleContentColor = PundarTextPrimary
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
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PundarTextPrimary
                )
            }
        },
        actions = {
            actions()
            if (showHelp) {
                IconButton(onClick = { }) {
                    Icon(Icons.Filled.HelpOutline, contentDescription = "Help", tint = PundarTextSecondary)
                }
            }
            if (showMoreOptions) {
                IconButton(onClick = { }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = PundarTextSecondary)
                }
            }
            if (showNotifications) {
                IconButton(onClick = { }) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = PundarTextSecondary)
                }
            }
            if (showStar) {
                IconButton(onClick = { }) {
                    Icon(Icons.Filled.Star, contentDescription = "Favorite", tint = PundarTextSecondary)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = PundarSurface,
            titleContentColor = PundarTextPrimary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PundarCircleTopBar(
    onBack: () -> Unit = {},
    showNotifications: Boolean = true,
    userInitials: String = "JD"
) {
    TopAppBar(
        title = {
            Text(
                text = "PUNDAR",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = PundarTextPrimary
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PundarTextPrimary
                )
            }
        },
        actions = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PundarBlue)
            ) {
                Text(
                    text = userInitials,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(8.dp))
            if (showNotifications) {
                IconButton(onClick = { }) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = PundarTextSecondary)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = PundarSurface,
            titleContentColor = PundarTextPrimary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PundarGrowTopBar(
    userInitials: String = "JD",
    pundarScore: Int = 850,
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
                        .background(PundarBlue)
                ) {
                    Text(
                        text = userInitials,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "PUNDAR",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = PundarTextPrimary
                )
            }
        },
        actions = {
            Surface(
                shape = CircleShape,
                color = PundarTextPrimary,
                modifier = Modifier.height(32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Score",
                        tint = PundarGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Score: $pundarScore",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = PundarSurface,
            titleContentColor = PundarTextPrimary
        )
    )
}
