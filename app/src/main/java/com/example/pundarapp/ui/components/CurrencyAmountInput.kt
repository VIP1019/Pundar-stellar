package com.example.pundarapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.getCurrencySymbol
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyAmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    isFiatMode: Boolean,
    onToggleMode: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Amount",
    errorMessage: String = "",
    readOnly: Boolean = false
) {
    val rate = AppState.currentExchangeRate.doubleValue
    val prefCurrency = AppState.preferredCurrency.value
    
    val inputVal = value.toDoubleOrNull() ?: 0.0
    val equivalentText = if (isFiatMode) {
        val xlm = if (rate > 0) inputVal / rate else 0.0
        "≈ ${String.format("%,.2f", xlm)} XLM"
    } else {
        val fiat = inputVal * rate
        "≈ ${getCurrencySymbol(prefCurrency)} ${String.format("%,.2f", fiat)}"
    }

    val currentLabel = if (isFiatMode) "$label ($prefCurrency)" else "$label (XLM)"
    val placeholderText = if (isFiatMode) "e.g. 500" else "e.g. 50"

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(currentLabel, color = PundarTheme.colors.textMuted) },
            placeholder = { Text(placeholderText, color = PundarTheme.colors.textDim) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            readOnly = readOnly,
            isError = errorMessage.isNotEmpty(),
            trailingIcon = {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PundarTheme.colors.surfaceTertiary)
                        .clickable(onClick = onToggleMode)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapVert,
                        contentDescription = "Toggle Currency",
                        tint = Blue400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PundarTheme.colors.brandPrimary,
                unfocusedBorderColor = PundarTheme.colors.borderPrimary,
                focusedTextColor = PundarTheme.colors.textPrimary,
                unfocusedTextColor = PundarTheme.colors.textPrimary,
                cursorColor = PundarTheme.colors.brandPrimary,
                errorBorderColor = PundarTheme.colors.accentRed
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Red400,
                    style = MaterialTheme.typography.labelSmall
                )
            } else {
                Spacer(Modifier.width(1.dp)) // keeps layout stable
            }
            
            if (value.isNotEmpty()) {
                Text(
                    text = equivalentText,
                    color = PundarTheme.colors.textMuted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
