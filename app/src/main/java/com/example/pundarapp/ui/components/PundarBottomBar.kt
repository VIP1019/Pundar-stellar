package com.example.pundarapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pundarapp.ui.theme.*

enum class BottomNavItem(
    val route: String,
    val label: String,
    val iconName: String
) {
    HOME("home", "Home", "home"),
    PAY("pay", "Pay", "pay"),
    CIRCLE("circle", "Circle", "circle"),
    GROW("grow", "Grow", "grow")
}

@Composable
fun PundarBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = PundarSurface,
        contentColor = PundarTextSecondary,
        tonalElevation = 8.dp,
        modifier = Modifier.height(80.dp)
    ) {
        BottomNavItem.entries.forEach { item ->
            val selected = currentRoute.startsWith(item.route)

            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    if (item == BottomNavItem.CIRCLE) {
                        // Special Circle icon with yellow accent
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = if (selected) {
                                Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(PundarGold)
                            } else {
                                Modifier.size(48.dp)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Home, // placeholder, overridden below
                                contentDescription = item.label,
                                tint = if (selected) PundarTextPrimary else PundarTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            // We'll use text-based icons since we can't import custom drawables
                            Text(
                                text = when (item) {
                                    BottomNavItem.CIRCLE -> "👥"
                                    else -> ""
                                },
                                fontSize = 20.sp
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = when (item) {
                                    BottomNavItem.HOME -> if (selected) "🏠" else "🏠"
                                    BottomNavItem.PAY -> if (selected) "💳" else "💳"
                                    BottomNavItem.GROW -> if (selected) "📈" else "📈"
                                    else -> ""
                                },
                                fontSize = 22.sp
                            )
                        }
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) {
                            if (item == BottomNavItem.CIRCLE) PundarGold else PundarBlue
                        } else PundarTextSecondary
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
