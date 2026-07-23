package com.example.pundarapp.ui.screens.bridge

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.components.PundarPrimaryButton
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.theme.*

import com.example.pundarapp.ui.theme.PundarTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundingScreen(navController: NavController) {
    var phpAmount by remember { mutableStateOf("") }
    var isFunding by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Fund Wallet",
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.AccountBalanceWallet,
                contentDescription = null,
                tint = PundarTheme.colors.brandPrimary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Custodial Bridge Funding",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PundarTheme.colors.textPrimary
            )
            Text(
                "Temporary bridge for PHP to XLM/USDC conversion.",
                style = MaterialTheme.typography.bodyMedium,
                color = PundarTheme.colors.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(Modifier.height(32.dp))
            
            OutlinedTextField(
                value = phpAmount,
                onValueChange = { phpAmount = it },
                label = { Text("Amount (PHP)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PundarTheme.colors.brandPrimary,
                    focusedLabelColor = PundarTheme.colors.brandPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(Modifier.height(8.dp))
            
            val conversion = (phpAmount.toDoubleOrNull() ?: 0.0) * 0.5
            Text(
                "Estimated: ${String.format("%.2f", conversion)} XLM",
                style = MaterialTheme.typography.bodySmall,
                color = PundarTheme.colors.brandPrimary
            )
            
            Spacer(Modifier.weight(1f))
            
            PundarPrimaryButton(
                text = if (isFunding) "Processing..." else "Fund via Bridge",
                enabled = (phpAmount.toDoubleOrNull() ?: 0.0) > 0 && !isFunding,
                onClick = {
                    isFunding = true
                    AppState.realBridgeFunding(phpAmount.toDouble()) { result ->
                        if (result.isSuccess) {
                            Toast.makeText(context, result.getOrNull(), Toast.LENGTH_LONG).show()
                            navController.navigateUp()
                        } else {
                            Toast.makeText(context, "Funding failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            isFunding = false
                        }
                    }
                }
            )
        }
    }
}
