package com.example.pundarapp.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.components.PundarPrimaryButton
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LinkCardScreen(navController: NavController) {
    var cardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var cardholderName by remember { mutableStateOf("") }

    var isLinking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            PundarDetailTopBar(title = "Link Bank Card", onBack = { navController.navigateUp() })
        },
        containerColor = PundarTheme.colors.bgPrimary,
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                PundarPrimaryButton(
                    text = if (isLinking) "Linking..." else "Link Card",
                    enabled = cardNumber.isNotBlank() && expiryDate.isNotBlank() && cvv.isNotBlank() && cardholderName.isNotBlank() && !isLinking,
                    onClick = {
                        val sanitizedCard = cardNumber.replace(" ", "")
                        if (!isValidLuhn(sanitizedCard)) {
                            errorMessage = "Invalid card number."
                            return@PundarPrimaryButton
                        }
                        if (sanitizedCard.length < 13) {
                            errorMessage = "Card number is too short."
                            return@PundarPrimaryButton
                        }
                        
                        errorMessage = null
                        isLinking = true
                        scope.launch {
                            delay(1500) // Simulate network call
                            isLinking = false
                            Toast.makeText(context, "Card successfully linked!", Toast.LENGTH_SHORT).show()
                            navController.navigateUp()
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = ElectricBlue.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CreditCard, contentDescription = null, tint = ElectricBlue)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Your card details are securely encrypted and protected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PundarTheme.colors.textSecondary
                    )
                }
            }

            OutlinedTextField(
                value = cardholderName,
                onValueChange = { cardholderName = it },
                label = { Text("Name on Card") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PundarTheme.colors.brandPrimary,
                    focusedLabelColor = PundarTheme.colors.brandPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = formatCardNumber(cardNumber),
                onValueChange = {
                    val raw = it.replace(" ", "")
                    if (raw.length <= 19 && raw.all { char -> char.isDigit() }) {
                        cardNumber = raw
                    }
                },
                label = { Text("Card Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PundarTheme.colors.brandPrimary,
                    focusedLabelColor = PundarTheme.colors.brandPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = formatExpiryDate(expiryDate),
                    onValueChange = {
                        val raw = it.replace("/", "")
                        if (raw.length <= 4 && raw.all { char -> char.isDigit() }) {
                            expiryDate = raw
                        }
                    },
                    label = { Text("MM/YY") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PundarTheme.colors.brandPrimary,
                        focusedLabelColor = PundarTheme.colors.brandPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = cvv,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            cvv = it
                        }
                    },
                    label = { Text("CVV") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PundarTheme.colors.brandPrimary,
                        focusedLabelColor = PundarTheme.colors.brandPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

private fun isValidLuhn(cardNumber: String): Boolean {
    var sum = 0
    var alternate = false
    for (i in cardNumber.length - 1 downTo 0) {
        var n = cardNumber[i] - '0'
        if (alternate) {
            n *= 2
            if (n > 9) {
                n = (n % 10) + 1
            }
        }
        sum += n
        alternate = !alternate
    }
    return sum % 10 == 0
}

private fun formatCardNumber(input: String): String {
    val chunked = input.chunked(4)
    return chunked.joinToString(" ")
}

private fun formatExpiryDate(input: String): String {
    if (input.length <= 2) return input
    return "${input.substring(0, 2)}/${input.substring(2)}"
}
