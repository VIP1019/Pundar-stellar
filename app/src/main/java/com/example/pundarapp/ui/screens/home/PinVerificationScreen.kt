package com.example.pundarapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.ui.components.PinInputComponent
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.theme.PundarBackground
import kotlinx.coroutines.launch

@Composable
fun PinVerificationScreen(
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Security Verification",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            PinInputComponent(
                title = "Enter your PIN",
                subtitle = "Please enter your 4-digit PIN to confirm this action.",
                errorMessage = errorMessage,
                onBiometric = {
                    // Biometric placeholder
                    errorMessage = "Biometric authentication will be available in a future update."
                },
                onPinComplete = { enteredPin ->
                    if (isVerifying) return@PinInputComponent
                    isVerifying = true
                    errorMessage = null

                    scope.launch {
                        val result = AuthRepository.verifyMpin(enteredPin)
                        isVerifying = false
                        if (result.isSuccess) {
                            navController.previousBackStackEntry?.savedStateHandle?.set("pin_verified", true)
                            navController.previousBackStackEntry?.savedStateHandle?.set("pin", enteredPin)
                            navController.popBackStack()
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Incorrect PIN"
                        }
                    }
                }
            )
        }
    }
}
