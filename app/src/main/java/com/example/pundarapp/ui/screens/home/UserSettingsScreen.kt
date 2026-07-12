package com.example.pundarapp.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.ui.components.PundarAvatar
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.components.PundarPrimaryButton
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import androidx.compose.ui.unit.sp
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

    var profileUrlInput by remember(userSession?.profileImageUrl) {
        mutableStateOf(userSession?.profileImageUrl ?: "")
    }
    var isSavingPhoto by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "User Settings",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PundarSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PundarAvatar(
                        initials = initials,
                        imageUrl = profileUrlInput.ifBlank { userSession?.profileImageUrl },
                        size = 80.dp,
                        showRing = true,
                        initialsFontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PundarTextPrimary
                    )
                    Text(
                        text = userPhone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PundarTextSecondary
                    )
                }
            }

            OutlinedTextField(
                value = profileUrlInput,
                onValueChange = { profileUrlInput = it },
                label = { Text("Profile Photo URL") },
                placeholder = { Text("https://example.com/photo.jpg") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PundarBlue,
                    focusedLabelColor = PundarBlue
                )
            )

            PundarPrimaryButton(
                text = if (isSavingPhoto) "Saving..." else "Save Profile Photo",
                enabled = !isSavingPhoto,
                isLoading = isSavingPhoto,
                onClick = {
                    isSavingPhoto = true
                    coroutineScope.launch {
                        val result = AuthRepository.updateProfileImageUrl(profileUrlInput)
                        isSavingPhoto = false
                        if (result.isSuccess) {
                            Toast.makeText(context, "Profile photo updated", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                context,
                                result.exceptionOrNull()?.message ?: "Failed to save photo",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )

            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PundarTextPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            SettingsItem(
                icon = Icons.Filled.Security,
                title = "Change MPIN",
                subtitle = "Update your 4-digit security code",
                onClick = { navController.navigate(Routes.CHANGE_MPIN) }
            )

            Spacer(modifier = Modifier.weight(1f))

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
                    containerColor = PundarError,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(
                    text = "Log Out",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PundarSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = PundarTextSecondary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PundarTextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = PundarTextSecondary
                )
            }
        }
    }
}
