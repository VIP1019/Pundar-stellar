package com.example.pundarapp.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.CircleRepository
import com.example.pundarapp.data.remote.PayRepository
import com.example.pundarapp.ui.components.PundarCard
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.components.PundarPrimaryButton
import com.example.pundarapp.ui.components.PundarSecondaryButton
import com.example.pundarapp.ui.data.*
import com.example.pundarapp.ui.theme.PundarTheme
import com.example.pundarapp.ui.theme.ElectricBlue
import com.example.pundarapp.data.remote.AuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(navController: NavController) {
    val requests = AppState.unifiedRequests
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        AppState.refreshUnifiedRequests()
    }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Requests",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarTheme.colors.bgPrimary
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch {
                    AppState.refreshUnifiedRequests()
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
                if (requests.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "No pending requests.",
                                color = PundarTheme.colors.textSecondary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    items(requests.toList()) { req ->
                        RequestCard(
                            request = req,
                            onAccept = {
                                scope.launch {
                                    handleAccept(req, context)
                                    AppState.refreshUnifiedRequests()
                                }
                            },
                            onDecline = {
                                scope.launch {
                                    handleDecline(req, context)
                                    AppState.refreshUnifiedRequests()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

suspend fun handleAccept(req: AppRequest, context: android.content.Context) {
    val phone = AuthRepository.getCurrentUserPhone()
    when (req.type) {
        RequestType.BILL_INVITE -> {
            val payload = req.payload as BillInvitation
            val result = PayRepository.acceptBillInvitation(payload.id, payload.billId, phone)
            if (result) {
                com.example.pundarapp.data.remote.NotificationRepository.sendNotification(
                    userId = phone,
                    title = "Bill Split Accepted",
                    message = "You have joined the bill split for ${payload.billName}.",
                    kind = "BILL"
                )
                Toast.makeText(context, "Bill Split Accepted", Toast.LENGTH_SHORT).show()
                AppState.refreshBills()
            }
        }
        RequestType.CIRCLE_INVITE -> {
            val payload = req.payload as CircleInvitation
            val result = AppState.acceptInvitation(payload)
            if (result.isSuccess) {
                com.example.pundarapp.data.remote.NotificationRepository.sendNotification(
                    userId = phone,
                    title = "Circle Joined",
                    message = "You have successfully joined ${payload.circleName}.",
                    kind = "CIRCLE"
                )
                Toast.makeText(context, "Circle Joined", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Error joining circle", Toast.LENGTH_SHORT).show()
            }
        }
        RequestType.CIRCLE_JOIN_REQUEST -> {
            val payload = req.payload as CircleJoinRequest
            val result = CircleRepository.approveJoinRequest(payload.id, phone)
            if (result.isSuccess) {
                com.example.pundarapp.data.remote.NotificationRepository.sendNotification(
                    userId = payload.userId,
                    title = "Join Request Approved",
                    message = "Your request to join the Paluwagan circle has been approved.",
                    kind = "CIRCLE"
                )
                Toast.makeText(context, "Join request approved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Error approving request", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

suspend fun handleDecline(req: AppRequest, context: android.content.Context) {
    val phone = AuthRepository.getCurrentUserPhone()
    when (req.type) {
        RequestType.BILL_INVITE -> {
            val payload = req.payload as BillInvitation
            PayRepository.rejectBillInvitation(payload.id)
            Toast.makeText(context, "Bill Split Declined", Toast.LENGTH_SHORT).show()
        }
        RequestType.CIRCLE_INVITE -> {
            val payload = req.payload as CircleInvitation
            AppState.rejectInvitation(payload)
            Toast.makeText(context, "Circle Invite Declined", Toast.LENGTH_SHORT).show()
        }
        RequestType.CIRCLE_JOIN_REQUEST -> {
            val payload = req.payload as CircleJoinRequest
            CircleRepository.rejectJoinRequest(payload.id, phone)
            Toast.makeText(context, "Join request declined", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun RequestCard(request: AppRequest, onAccept: () -> Unit, onDecline: () -> Unit) {
    PundarCard {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PundarTheme.colors.brandPrimary.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = PundarTheme.colors.brandPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = request.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PundarTheme.colors.textPrimary
                    )
                    Text(
                        text = request.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = PundarTheme.colors.textSecondary
                    )
                }
            }
            Text(
                text = request.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = PundarTheme.colors.textPrimary
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    PundarSecondaryButton(
                        text = "Decline",
                        onClick = onDecline
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    PundarPrimaryButton(
                        text = "Accept",
                        onClick = onAccept
                    )
                }
            }
        }
    }
}
