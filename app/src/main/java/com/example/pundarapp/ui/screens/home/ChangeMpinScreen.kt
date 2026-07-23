package com.example.pundarapp.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.components.PundarPrimaryButton
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeMpinScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentMpin by remember { mutableStateOf("") }
    var newMpin by remember { mutableStateOf("") }
    var confirmNewMpin by remember { mutableStateOf("") }

    var currentMpinVisible by remember { mutableStateOf(false) }
    var newMpinVisible by remember { mutableStateOf(false) }
    var confirmMpinVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val confirmMismatch = newMpin.isNotEmpty() && confirmNewMpin.isNotEmpty() && newMpin != confirmNewMpin
    val sameAsCurrent = newMpin.length == 4 && currentMpin.length == 4 && newMpin == currentMpin

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Change MPIN",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarTheme.colors.bgPrimary
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Secure your account with a new 4-digit PIN.",
                style = MaterialTheme.typography.bodyMedium,
                color = PundarTheme.colors.textSecondary
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = PundarError,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedTextField(
                value = currentMpin,
                onValueChange = {
                    if (it.length <= 4) {
                        currentMpin = it
                        errorMessage = null
                    }
                },
                label = { Text("Current PIN") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = if (currentMpinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (currentMpinVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { currentMpinVisible = !currentMpinVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle visibility")
                    }
                },
                isError = errorMessage?.contains("current", ignoreCase = true) == true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PundarTheme.colors.brandPrimary,
                    focusedLabelColor = PundarTheme.colors.brandPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = newMpin,
                onValueChange = {
                    if (it.length <= 4) {
                        newMpin = it
                        errorMessage = null
                    }
                },
                label = { Text("New PIN") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = if (newMpinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (newMpinVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { newMpinVisible = !newMpinVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle visibility")
                    }
                },
                isError = sameAsCurrent,
                supportingText = {
                    if (sameAsCurrent) {
                        Text("New PIN must differ from current PIN.", color = PundarError)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PundarTheme.colors.brandPrimary,
                    focusedLabelColor = PundarTheme.colors.brandPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = confirmNewMpin,
                onValueChange = {
                    if (it.length <= 4) {
                        confirmNewMpin = it
                        errorMessage = null
                    }
                },
                label = { Text("Confirm New PIN") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = if (confirmMpinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (confirmMpinVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { confirmMpinVisible = !confirmMpinVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle visibility")
                    }
                },
                isError = confirmMismatch,
                supportingText = {
                    if (confirmMismatch) {
                        Text("PINs do not match.", color = PundarError)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PundarTheme.colors.brandPrimary,
                    focusedLabelColor = PundarTheme.colors.brandPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            PundarPrimaryButton(
                text = "Update PIN",
                enabled = !isLoading &&
                    currentMpin.length == 4 &&
                    newMpin.length == 4 &&
                    confirmNewMpin.length == 4 &&
                    !confirmMismatch &&
                    !sameAsCurrent,
                isLoading = isLoading,
                onClick = {
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = AuthRepository.changeMpin(currentMpin, newMpin, confirmNewMpin)
                        isLoading = false
                        if (result.isSuccess) {
                            AuthRepository.logout()
                            Toast.makeText(
                                context,
                                "Your PIN has been changed successfully. Please log in again.",
                                Toast.LENGTH_LONG
                            ).show()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Failed to update PIN"
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}
