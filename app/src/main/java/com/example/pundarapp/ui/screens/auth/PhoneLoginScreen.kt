package com.example.pundarapp.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.ui.components.PundarPrimaryButton
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.PundarBackground
import com.example.pundarapp.ui.theme.PundarError
import com.example.pundarapp.ui.theme.PundarTextPrimary
import com.example.pundarapp.ui.theme.PundarTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneLoginScreen(navController: NavController) {
    var phone by remember { mutableStateOf("") }
    var otpToken by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) } // 1 = Enter Phone, 2 = Enter OTP
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PundarBackground)
            )
        },
        containerColor = PundarBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = if (step == 1) "Enter your phone number" else "Verify your number",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = PundarTextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (step == 1) "We will send you a one-time password (OTP)" else "Enter the 6-digit code sent to $phone",
                style = MaterialTheme.typography.bodyLarge,
                color = PundarTextSecondary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (step == 1) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (e.g. +1234567890)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = otpToken,
                    onValueChange = { otpToken = it.take(6) }, // limit to 6 chars
                    label = { Text("6-Digit OTP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = PundarError,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            PundarPrimaryButton(
                text = if (isLoading) "Please wait..." else if (step == 1) "Send OTP" else "Verify & Sign In",
                enabled = !isLoading && if (step == 1) phone.length > 5 else otpToken.length == 6,
                onClick = {
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        if (step == 1) {
                            val result = AuthRepository.sendOtp(phone)
                            if (result.isSuccess) {
                                step = 2
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Failed to send OTP."
                            }
                        } else {
                            val result = AuthRepository.verifyOtp(phone, otpToken)
                            if (result.isSuccess) {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                }
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Invalid OTP."
                            }
                        }
                        isLoading = false
                    }
                }
            )
        }
    }
}
