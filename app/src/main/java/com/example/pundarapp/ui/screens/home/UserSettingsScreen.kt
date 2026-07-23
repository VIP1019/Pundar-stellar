package com.example.pundarapp.ui.screens.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.remote.CurrencyData
import com.example.pundarapp.data.remote.supportedCurrencies
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.pundarapp.ui.components.AnimatedBackground
import com.example.pundarapp.ui.components.BgAccent
import com.example.pundarapp.ui.components.PundarAvatar
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSettingsScreen(navController: NavController) {
    val context        = LocalContext.current
    val scope          = rememberCoroutineScope()
    val userSession    by AuthRepository.currentUserState
    val userName       = userSession?.name ?: "User"
    val userPhone      = userSession?.phone ?: ""
    val initials       = AuthRepository.getCurrentUserInitials()
    var profileUri     by remember { mutableStateOf<Uri?>(null) }
    var isLoggingOut by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showPhotoSheet by remember { mutableStateOf(false) }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            profileUri = uri
            Toast.makeText(context, "Photo updated!", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            Toast.makeText(context, "Photo captured!", Toast.LENGTH_SHORT).show()
        }
    }

    if (showPhotoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoSheet = false },
            containerColor   = Navy800,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Change Profile Photo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = TextWhite,
                    modifier = Modifier.padding(bottom = 16.dp))
                PhotoOption(Icons.Filled.PhotoLibrary, "Choose from Gallery", Blue400) {
                    showPhotoSheet = false; galleryLauncher.launch("image/*")
                }
                Spacer(Modifier.height(8.dp))
                PhotoOption(Icons.Filled.CameraAlt, "Take a Photo", Green400) {
                    showPhotoSheet = false; cameraLauncher.launch(null)
                }
                Spacer(Modifier.height(8.dp))
                PhotoOption(Icons.Filled.Delete, "Remove Photo", Red500) {
                    showPhotoSheet = false
                    profileUri = null
                    Toast.makeText(context, "Profile photo removed.", Toast.LENGTH_SHORT).show()
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }

    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            containerColor = Navy800,
            title = {
                Text("Select Currency", color = TextWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(supportedCurrencies) { currency ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (AppState.preferredCurrency.value == currency.code) Blue500.copy(0.2f) else Navy700)
                                .clickable {
                                    AppState.setCurrency(currency.code)
                                    showCurrencyDialog = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(currency.flag, fontSize = 24.sp)
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(currency.name, color = TextWhite, fontWeight = FontWeight.SemiBold)
                                Text("${currency.code} - ${currency.symbol}", color = TextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("Close", color = Blue400)
                }
            }
        )
    }

    Scaffold(
        topBar         = { PundarDetailTopBar(title = "Settings", onBack = { navController.navigateUp() }) },
        containerColor = Navy900
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Profile header ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF0E2260), Navy900)))
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        if (profileUri != null) {
                            AsyncImage(
                                model              = profileUri,
                                contentDescription = "Profile",
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.size(90.dp).clip(CircleShape)
                                    .border(3.dp, Brush.linearGradient(listOf(Blue500, Blue600)), CircleShape)
                            )
                        } else {
                            PundarAvatar(
                                initials         = initials,
                                imageUrl         = userSession?.profileImageUrl,
                                size             = 90.dp,
                                showRing         = true,
                                initialsFontSize = 28.sp
                            )
                        }
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape)
                                .background(Blue500).border(2.dp, Navy900, CircleShape)
                                .clickable { showPhotoSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CameraAlt, "Change photo",
                                tint = White, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(userName, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = TextWhite)
                    Spacer(Modifier.height(3.dp))
                    Text(userPhone, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Account ────────────────────────────────────────────
            SectionHeader("Account")
            SettingsGroup {
                SettingsRow(Icons.Filled.Person,   "Personal Information", Blue400) {}
                RowDivider()
                SettingsRow(Icons.Filled.Edit,     "Edit Name",            Blue400) {}
                RowDivider()
                SettingsRow(Icons.Filled.Phone,    "Phone Number",         Blue400, sub = userPhone) {}
                RowDivider()
                SettingsRow(Icons.Filled.Email,    "Email Address",        Blue400) {}
            }

            Spacer(Modifier.height(16.dp))

            // ── Security ───────────────────────────────────────────
            SectionHeader("Security")
            SettingsGroup {
                SettingsRow(Icons.Filled.Lock,        "Change MPIN",       Orange500) {
                    navController.navigate(Routes.CHANGE_MPIN)
                }
                RowDivider()
                SettingsRow(Icons.Filled.Password,    "Change Password",   Orange500) {}
                RowDivider()
                SettingsRow(Icons.Filled.Fingerprint, "Biometrics",        Orange500, sub = "Coming soon") {}
                RowDivider()
                SettingsRow(Icons.Filled.Devices,     "Device Management", Orange500) {}
            }

            Spacer(Modifier.height(16.dp))

            // ── Notifications ──────────────────────────────────────
            SectionHeader("Notifications")
            SettingsGroup {
                ToggleRow(Icons.Filled.Notifications, "Push Notifications",   Blue400, default = true)
                RowDivider()
                ToggleRow(Icons.Filled.Receipt,       "Transaction Alerts",   Blue400, default = true)
                RowDivider()
                ToggleRow(Icons.Filled.Campaign,      "Promotions & Offers",  Blue400, default = false)
            }

            Spacer(Modifier.height(16.dp))

            // ── Privacy ────────────────────────────────────────────
            SectionHeader("Privacy")
            SettingsGroup {
                // Hide Balance — wired directly to AppState
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBox(Icons.Filled.VisibilityOff, Blue400)
                    Spacer(Modifier.width(14.dp))
                    Text("Hide Balance", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium, color = TextWhite, modifier = Modifier.weight(1f))
                    Switch(
                        checked         = AppState.isBalanceHidden.value,
                        onCheckedChange = { AppState.toggleBalanceVisibility() },
                        colors          = SwitchDefaults.colors(
                            checkedTrackColor   = Blue500,
                            uncheckedTrackColor = Navy600,
                            checkedThumbColor   = White,
                            uncheckedThumbColor = TextMuted
                        )
                    )
                }
                RowDivider()
                SettingsRow(Icons.Filled.Policy, "Manage Data Preferences", Blue400) {}
            }

            Spacer(Modifier.height(16.dp))
            
            // ── Preferences ──────────────────────────────────────────
            SectionHeader("Preferences")
            SettingsGroup {
                SettingsRow(Icons.Filled.CurrencyExchange, "Preferred Currency", Blue400, sub = AppState.preferredCurrency.value) {
                    showCurrencyDialog = true
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Linked Cards ───────────────────────────────────────
            SectionHeader("Linked Cards")
            SettingsGroup {
                SettingsRow(Icons.Filled.CreditCard, "Manage Cards", Gold500) {
                    navController.navigate(Routes.LINK_CARD)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── About ──────────────────────────────────────────────
            SectionHeader("About")
            SettingsGroup {
                SettingsRow(Icons.Filled.Description, "Terms and Conditions", TextMuted) {}
                RowDivider()
                SettingsRow(Icons.Filled.PrivacyTip,  "Privacy Policy",       TextMuted) {}
                RowDivider()
                SettingsRow(Icons.Filled.Help,        "Help Center",          TextMuted) {}
                RowDivider()
                SettingsRow(Icons.Filled.Support,     "Contact Support",      TextMuted) {}
                RowDivider()
                SettingsRow(Icons.Filled.Info,        "App Version",          TextMuted,
                    sub = "1.0.0 (Beta)") {}
            }

            Spacer(Modifier.height(20.dp))

            // ── Logout ─────────────────────────────────────────────
            Box(Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(RedBg)
                        .border(1.dp, Red500.copy(0.28f), RoundedCornerShape(16.dp))
                        .clickable {
                            scope.launch {
                                AuthRepository.logout()
                                AppState.clearSession()
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null,
                        tint = Red400, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Log Out", color = Red400, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(36.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.labelMedium, color = TextMuted,
        fontWeight = FontWeight.SemiBold,
        modifier   = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Navy800, Navy700)))
            .border(1.dp, Glass10, RoundedCornerShape(16.dp)),
        content = content
    )
}

@Composable
private fun IconBox(icon: ImageVector, accent: Color) {
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(0.12f)),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp)) }
}

@Composable
private fun SettingsRow(
    icon:    ImageVector,
    label:   String,
    accent:  Color,
    sub:     String?    = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(icon, accent)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium, color = TextWhite)
            if (!sub.isNullOrBlank())
                Text(sub, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = TextDim, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, label: String, accent: Color, default: Boolean) {
    var checked by remember { mutableStateOf(default) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(icon, accent)
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium, color = TextWhite, modifier = Modifier.weight(1f))
        Switch(
            checked         = checked,
            onCheckedChange = { checked = it },
            colors          = SwitchDefaults.colors(
                checkedTrackColor   = Blue500, uncheckedTrackColor = Navy600,
                checkedThumbColor   = White,   uncheckedThumbColor = TextMuted
            )
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(horizontal = 16.dp),
        color     = NavyBorder.copy(0.5f),
        thickness = 0.5.dp
    )
}

@Composable
private fun PhotoOption(icon: ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(0.12f)),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium, color = TextWhite)
    }
}
