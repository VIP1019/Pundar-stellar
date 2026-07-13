package com.example.pundarapp.ui.screens.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.ui.components.PundarAvatar
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userSession by AuthRepository.currentUserState
    val userName = userSession?.name ?: "User"
    val userPhone = userSession?.phone ?: ""
    val initials = AuthRepository.getCurrentUserInitials()

    var profileUrl by remember(userSession?.profileImageUrl) {
        mutableStateOf(userSession?.profileImageUrl ?: "")
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            Toast.makeText(context, "Image selected (Upload not yet implemented)", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Settings",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        PundarAvatar(
                            initials = initials,
                            imageUrl = profileUrl.ifBlank { null },
                            size = 100.dp,
                            showRing = true,
                            initialsFontSize = 32.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = "Change Photo", tint = SpaceBlack, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = userPhone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Account Section
            SettingsSection("Account") {
                SettingsItem(
                    icon = Icons.Filled.Person,
                    title = "Personal Information",
                    subtitle = "Update name, email, and address",
                    onClick = { Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show() }
                )
                SettingsItem(
                    icon = Icons.Filled.CreditCard,
                    title = "Linked Accounts & Cards",
                    subtitle = "Manage payment methods",
                    onClick = { navController.navigate(Routes.LINK_CARD) }
                )
                SettingsItem(
                    icon = Icons.Filled.VerifiedUser,
                    title = "Account Verification",
                    subtitle = "Level 2 Verified",
                    value = "Verified",
                    onClick = { Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show() }
                )
            }

            // Security Section
            SettingsSection("Security") {
                SettingsItem(
                    icon = Icons.Filled.Security,
                    title = "Change MPIN",
                    subtitle = "Update your 4-digit security code",
                    onClick = { navController.navigate(Routes.CHANGE_MPIN) }
                )
                SettingsItem(
                    icon = Icons.Filled.Fingerprint,
                    title = "Biometric Login",
                    subtitle = "Use fingerprint or Face ID",
                    hasToggle = true,
                    isToggled = false,
                    onToggle = { Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show() }
                )
            }

            // App Settings
            SettingsSection("App Settings") {
                SettingsItem(
                    icon = Icons.Filled.Notifications,
                    title = "Notifications",
                    subtitle = "Manage alerts and updates",
                    onClick = { Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show() }
                )
                SettingsItem(
                    icon = Icons.Filled.DarkMode,
                    title = "Appearance",
                    subtitle = "Dark Mode (System Default)",
                    onClick = { Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show() }
                )
            }

            // Help & Support
            SettingsSection("Help & Support") {
                SettingsItem(
                    icon = Icons.Filled.Help,
                    title = "Help Center",
                    subtitle = "FAQs and Contact Support",
                    onClick = { Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show() }
                )
                SettingsItem(
                    icon = Icons.Filled.Info,
                    title = "About PUNDAR",
                    subtitle = "Version 1.0.0",
                    onClick = { Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show() }
                )
            }

            Spacer(Modifier.height(24.dp))

            // Logout Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            AuthRepository.logout()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpaceMedium,
                        contentColor = ErrorRed
                    )
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Log Out",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextTertiary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            letterSpacing = 1.sp
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SpaceDeep,
            border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String? = null,
    hasToggle: Boolean = false,
    isToggled: Boolean = false,
    onToggle: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SpaceMedium),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = ElectricBlue, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = NeonGreen,
                modifier = Modifier.padding(start = 8.dp)
            )
        } else if (hasToggle) {
            Switch(
                checked = isToggled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SpaceBlack,
                    checkedTrackColor = ElectricBlue,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = SpaceMedium
                )
            )
        } else if (onClick != null) {
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextTertiary)
        }
    }
}
