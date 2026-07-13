package com.example.pundarapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pundarapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Reusable PIN input with dot indicators and numeric keypad.
 *
 * @param pinLength Number of digits (default 4)
 * @param title Header text (e.g. "Enter your PIN")
 * @param subtitle Secondary text below title
 * @param errorMessage If non-null, shows error and triggers shake animation
 * @param onPinComplete Called when all digits are entered
 * @param showBiometric Whether to show biometric button (future-ready)
 * @param onBiometricClick Callback for biometric button
 * @param accentColor Color for filled dots and keypad highlights
 */
@Composable
fun PinInputComponent(
    pinLength: Int = 4,
    title: String = "Enter PIN",
    subtitle: String = "",
    errorMessage: String? = null,
    onPinComplete: (String) -> Unit,
    showBiometric: Boolean = false,
    onBiometricClick: () -> Unit = {},
    accentColor: Color = ElectricBlue,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Shake animation for errors
    val shakeOffset = remember { Animatable(0f) }
    var isShaking by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null && !isShaking) {
            isShaking = true
            pin = ""
            // Shake animation
            repeat(4) {
                shakeOffset.animateTo(12f, tween(40))
                shakeOffset.animateTo(-12f, tween(40))
            }
            shakeOffset.animateTo(0f, tween(40))
            isShaking = false
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(32.dp))

        // PIN dot indicators with shake
        Row(
            modifier = Modifier.graphicsLayer(translationX = shakeOffset.value),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pinLength) { index ->
                val filled = index < pin.length
                PinDot(filled = filled, accentColor = accentColor, hasError = errorMessage != null)
            }
        }

        // Error message
        if (errorMessage != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = ErrorRed,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(40.dp))

        // Numeric keypad
        NumericKeypad(
            onDigitPress = { digit ->
                if (pin.length < pinLength) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    pin += digit
                    if (pin.length == pinLength) {
                        scope.launch {
                            delay(150) // Small delay for visual feedback
                            onPinComplete(pin)
                        }
                    }
                }
            },
            onBackspacePress = {
                if (pin.isNotEmpty()) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    pin = pin.dropLast(1)
                }
            },
            showBiometric = showBiometric,
            onBiometricClick = onBiometricClick,
            accentColor = accentColor
        )
    }
}

@Composable
private fun PinDot(
    filled: Boolean,
    accentColor: Color,
    hasError: Boolean,
    size: Dp = 16.dp
) {
    val scale by animateFloatAsState(
        targetValue = if (filled) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dotScale"
    )

    val dotColor = when {
        hasError && !filled -> ErrorRed.copy(alpha = 0.3f)
        filled -> accentColor
        else -> SpaceMedium
    }

    val borderColor = when {
        hasError -> ErrorRed.copy(alpha = 0.5f)
        filled -> accentColor.copy(alpha = 0.6f)
        else -> SpaceBorder
    }

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(CircleShape)
            .background(dotColor)
            .border(1.5.dp, borderColor, CircleShape)
    )
}

@Composable
private fun NumericKeypad(
    onDigitPress: (String) -> Unit,
    onBackspacePress: () -> Unit,
    showBiometric: Boolean,
    onBiometricClick: () -> Unit,
    accentColor: Color
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(if (showBiometric) "bio" else "", "0", "back")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    when (key) {
                        "back" -> KeypadButton(
                            onClick = onBackspacePress,
                            accentColor = accentColor
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Delete",
                                tint = TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        "bio" -> KeypadButton(
                            onClick = onBiometricClick,
                            accentColor = accentColor
                        ) {
                            Icon(
                                Icons.Filled.Fingerprint,
                                contentDescription = "Biometric",
                                tint = accentColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        "" -> Spacer(Modifier.size(72.dp))
                        else -> KeypadButton(
                            onClick = { onDigitPress(key) },
                            accentColor = accentColor
                        ) {
                            Text(
                                text = key,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    onClick: () -> Unit,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(SpaceMedium.copy(alpha = 0.5f))
            .border(1.dp, SpaceBorder, CircleShape)
            .clickable(
                indication = ripple(bounded = true, color = accentColor),
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
    ) {
        content()
    }
}
