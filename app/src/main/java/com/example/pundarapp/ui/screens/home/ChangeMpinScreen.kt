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
import com.example.pundarapp.ui.theme.*
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

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Change MPIN",
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
            Text(
                text = "Secure your account with a new 4-digit PIN.",
                style = MaterialTheme.typography.bodyMedium,
                color = PundarTextSecondary
            )

            // Current MPIN
            OutlinedTextField(
                value = currentMpin,
                onValueChange = { if (it.length <= 4) currentMpin = it },
                label = { Text("Current MPIN") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = if (currentMpinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (currentMpinVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { currentMpinVisible = !currentMpinVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle visibility")
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PundarBlue,
                    focusedLabelColor = PundarBlue
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // New MPIN
            OutlinedTextField(
                value = newMpin,
                onValueChange = { if (it.length <= 4) newMpin = it },
                label = { Text("New MPIN") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = if (newMpinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (newMpinVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { newMpinVisible = !newMpinVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle visibility")
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PundarBlue,
                    focusedLabelColor = PundarBlue
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Confirm New MPIN
            OutlinedTextField(
                value = confirmNewMpin,
                onValueChange = { if (it.length <= 4) confirmNewMpin = it },
                label = { Text("Confirm New MPIN") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = if (confirmMpinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (confirmMpinVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { confirmMpinVisible = !confirmMpinVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle visibility")
                    }
                },
                isError = newMpin.isNotEmpty() && confirmNewMpin.isNotEmpty() && newMpin != confirmNewMpin,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PundarBlue,
                    focusedLabelColor = PundarBlue
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            PundarPrimaryButton(
                text = if (isLoading) "Updating..." else "Update MPIN",
                enabled = !isLoading && currentMpin.length == 4 && newMpin.length == 4 && confirmNewMpin.length == 4 && newMpin == confirmNewMpin,
                onClick = {
                    isLoading = true
                    coroutineScope.launch {
                        val result = AuthRepository.changeMpin(currentMpin, newMpin)
                        isLoading = false
                        if (result.isSuccess) {
                            Toast.makeText(context, "MPIN updated successfully!", Toast.LENGTH_SHORT).show()
                            navController.navigateUp()
                        } else {
                            val errorMsg = result.exceptionOrNull()?.message ?: "Failed to update MPIN"
                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}
