package com.example.pundarapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.PundarCard
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.theme.*

data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val time: String,
    var isRead: Boolean = false
)

object NotificationData {
    val notifications = mutableStateListOf(
        Notification("1", "Goal Reached!", "You've successfully reached your saving goal for 'Boracay Trip'.", "2 hrs ago", false),
        Notification("2", "Circle Update", "Maria deposited ₱5,000 to your Emergency Fund circle.", "5 hrs ago", false),
        Notification("3", "Bill Settled", "John settled his share of ₱1,200 for the Dinner bill.", "1 day ago", true)
    )

    fun hasUnread(): Boolean {
        return notifications.any { !it.isRead }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(navController: NavController) {
    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Notifications",
                onBack = { navController.navigateUp() }
            )
        },
        floatingActionButton = {
            if (NotificationData.hasUnread()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        NotificationData.notifications.forEach { it.isRead = true }
                        // Trigger recomposition by re-assigning the list
                        val current = NotificationData.notifications.toList()
                        NotificationData.notifications.clear()
                        NotificationData.notifications.addAll(current)
                    },
                    containerColor = PundarGold,
                    contentColor = PundarTextPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.DoneAll, contentDescription = "Mark all as read")
                    Spacer(Modifier.width(8.dp))
                    Text("Mark all read")
                }
            }
        },
        containerColor = PundarBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (NotificationData.notifications.isEmpty()) {
                item {
                    Text(
                        text = "You have no notifications.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PundarTextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            items(NotificationData.notifications) { notification ->
                PundarCard(
                    modifier = Modifier.clickable {
                        notification.isRead = true
                        val index = NotificationData.notifications.indexOf(notification)
                        NotificationData.notifications[index] = notification.copy(isRead = true)
                    }
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
                                .background(PundarBlueLight)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null,
                                tint = PundarBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = notification.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (!notification.isRead) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = PundarTextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = notification.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = PundarTextSecondary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = notification.time,
                                style = MaterialTheme.typography.labelSmall,
                                color = PundarTextTertiary
                            )
                        }
                        if (!notification.isRead) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PundarBlue)
                                    .align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            }
        }
    }
}
