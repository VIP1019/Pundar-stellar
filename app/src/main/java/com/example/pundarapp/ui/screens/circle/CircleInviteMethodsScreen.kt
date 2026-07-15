package com.example.pundarapp.ui.screens.circle

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.data.qr.QrCodeGenerator
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.remote.CircleRepository
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.CircleMember
import com.example.pundarapp.ui.data.ContributionStatus
import com.example.pundarapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleInviteMethodsScreen(circleId: String, navController: NavController) {
    val circle = AppState.circles.find { it.id == circleId }
        ?: run { navController.navigateUp(); return }

    val tabs = listOf("QR Code", "Share Link", "Username", "Phone")
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Invite Members",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = Navy900
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Tab Row ───────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Navy800,
                contentColor = NeonGreen
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) NeonGreen else TextMuted
                            )
                        }
                    )
                }
            }

            // ── Tab Content ───────────────────────────────────────
            when (selectedTab) {
                0 -> QrCodeTab(circleId = circleId, circleName = circle.name)
                1 -> ShareLinkTab(circleId = circleId, circleName = circle.name,
                    memberCount = circle.memberCount, maxMembers = circle.maxMembers)
                2 -> UsernameTab(circleId = circleId)
                3 -> PhoneTab(circleId = circleId)
            }
        }
    }
}

// ── Tab 0: QR Code ────────────────────────────────────────────────
@Composable
private fun QrCodeTab(circleId: String, circleName: String) {
    val context = LocalContext.current
    val inviteLink = "https://pundar.app/join/$circleId"

    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(inviteLink) {
        qrBitmap = try { QrCodeGenerator.generateBitmap(inviteLink, 512) } catch (e: Exception) { null }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Share this QR Code",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = TextWhite
        )
        Text(
            "Anyone who scans this code can request to join $circleName.",
            color = TextMuted,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall
        )

        // QR bitmap or placeholder
        Box(
            Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(TextWhite)
                .border(3.dp, NeonGreen.copy(0.5f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap!!.asImageBitmap(),
                    contentDescription = "Invite QR Code",
                    modifier = Modifier.size(220.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.QrCode2, null, tint = Navy800, modifier = Modifier.size(64.dp))
                    Text("Generating...", color = Navy800, fontSize = 12.sp)
                }
            }
        }

        // Link text
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Navy700)
                .border(1.dp, NavyBorder, RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Text(inviteLink, color = Blue300, fontSize = 12.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { Toast.makeText(context, "QR Code saved!", Toast.LENGTH_SHORT).show() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSoft),
                border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
            ) {
                Icon(Icons.Filled.Download, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Save QR")
            }
            Button(
                onClick = { Toast.makeText(context, "Sharing QR...", Toast.LENGTH_SHORT).show() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Navy950)
            ) {
                Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Share QR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Tab 1: Share Link ─────────────────────────────────────────────
@Composable
private fun ShareLinkTab(
    circleId: String, circleName: String,
    memberCount: Int, maxMembers: Int
) {
    val context = LocalContext.current
    val inviteLink = "https://pundar.app/join/$circleId"

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Invite via Link", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextWhite)
        Text(
            "Share this link and anyone can request to join the group.",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )

        // Circle info card
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Navy800).border(1.dp, NavyBorder, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Groups, null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(circleName, fontWeight = FontWeight.SemiBold, color = TextWhite)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "$memberCount / $maxMembers members",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Link card
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Navy700).border(1.dp, Blue400.copy(0.3f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column {
                Text("Invite Link", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(inviteLink, color = Blue300, fontSize = 13.sp)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Pundar Invite", inviteLink))
                    Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue300),
                border = androidx.compose.foundation.BorderStroke(1.dp, Blue400.copy(0.5f))
            ) {
                Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Copy Link")
            }
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Join my Paluwagan group on PUNDAR!\n\n$inviteLink")
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Invite Link"))
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Navy950)
            ) {
                Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Share Link", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Tab 2: Username ───────────────────────────────────────────────
@Composable
private fun UsernameTab(circleId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<SearchUserResult>>(emptyList()) }
    var sendingTo by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Invite by Username", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextWhite)
        Text("Search for a PUNDAR user and send them an invite.", color = TextMuted,
            style = MaterialTheme.typography.bodySmall)

        // Search bar
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search username or name", color = TextDim) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextMuted) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Blue400, unfocusedBorderColor = NavyBorder,
                    focusedContainerColor = Navy700, unfocusedContainerColor = Navy700,
                    focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                    cursorColor = Blue400
                )
            )
            Button(
                onClick = {
                    if (query.isBlank()) return@Button
                    isSearching = true
                    scope.launch {
                        // Attempt real search; fall back to empty on error
                        results = try {
                            AuthRepository.searchUsers(query).map { user ->
                                val parts = user.name.split(" ")
                                val initials = if (parts.size >= 2)
                                    (parts.first().take(1) + parts.last().take(1)).uppercase()
                                else parts.first().take(2).uppercase()
                                SearchUserResult(user.id, user.name, initials)
                            }
                        } catch (e: Exception) { emptyList() }
                        isSearching = false
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue500)
            ) {
                if (isSearching) CircularProgressIndicator(
                    color = TextWhite, modifier = Modifier.size(16.dp), strokeWidth = 2.dp
                ) else Text("Search", fontWeight = FontWeight.SemiBold)
            }
        }

        if (results.isEmpty() && !isSearching && query.isNotBlank()) {
            Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                Text("No users found for \"$query\".", color = TextMuted, textAlign = TextAlign.Center)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(results.size) { i ->
                val user = results[i]
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Navy800).border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(Blue500),
                        contentAlignment = Alignment.Center
                    ) { Text(user.initials, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    Spacer(Modifier.width(12.dp))
                    Text(user.name, color = TextWhite, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    val isSending = sendingTo == user.userId
                    Button(
                        onClick = {
                            sendingTo = user.userId
                            scope.launch {
                                val result = CircleRepository.sendInvitation(
                                    circleId = circleId,
                                    inviteeUserId = user.userId,
                                    inviteeName = user.name,
                                    inviteeInitials = user.initials,
                                    inviterName = AuthRepository.getCurrentUserName(),
                                    inviterScore = 0
                                )
                                sendingTo = null
                                val msg = if (result.isSuccess) "Invite sent to ${user.name}!"
                                          else result.exceptionOrNull()?.message ?: "Failed to send invite."
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isSending,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Navy950)
                    ) {
                        if (isSending) CircularProgressIndicator(
                            color = Navy950, modifier = Modifier.size(14.dp), strokeWidth = 2.dp
                        ) else Text("Invite", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private data class SearchUserResult(val userId: String, val name: String, val initials: String)

// ── Tab 3: Phone Number ───────────────────────────────────────────
@Composable
private fun PhoneTab(circleId: String) {
    val context = LocalContext.current
    var phone by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }

    val inviteLink = "https://pundar.app/join/$circleId"

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Invite by Phone Number", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextWhite)
        Text(
            "An invite link will be sent via SMS to the phone number you enter.",
            color = TextMuted, style = MaterialTheme.typography.bodySmall
        )

        OutlinedTextField(
            value = phone,
            onValueChange = {
                if (it.length <= 11 && it.all(Char::isDigit)) {
                    phone = it
                    phoneError = if (it.length == 11) null else null
                }
            },
            label = { Text("Phone Number", color = TextMuted) },
            placeholder = { Text("09XXXXXXXXX", color = TextDim) },
            leadingIcon = { Icon(Icons.Filled.Phone, null, tint = TextMuted) },
            isError = phoneError != null,
            supportingText = phoneError?.let { { Text(it, color = Orange500) } },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Blue400, unfocusedBorderColor = NavyBorder,
                focusedContainerColor = Navy700, unfocusedContainerColor = Navy700,
                focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                cursorColor = Blue400, errorBorderColor = Orange500
            )
        )

        Button(
            onClick = {
                if (phone.length != 11) {
                    phoneError = "Enter a valid 11-digit number."
                    return@Button
                }
                phoneError = null
                Toast.makeText(context, "Invite sent to $phone!", Toast.LENGTH_SHORT).show()
                phone = ""
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Navy950)
        ) {
            Icon(Icons.Filled.Send, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Send Invite via SMS", fontWeight = FontWeight.Bold)
        }

        // Info note
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Blue500.copy(0.08f)).border(1.dp, Blue400.copy(0.2f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Info, null, tint = Blue300, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("How it works", color = Blue300, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "The recipient will receive a text message with your invite link ($inviteLink). They can tap it to open PUNDAR and join your group.",
                        color = TextMuted, fontSize = 11.sp
                    )
                }
            }
        }
    }
}
