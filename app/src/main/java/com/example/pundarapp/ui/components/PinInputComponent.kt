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
import androidx.compose.ui.graphics.Brush
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

// ── Reusable PIN input with numeric keypad ────────────────────────
@Composable
fun PinInputComponent(
    pinLength:        Int     = 4,
    title:            String  = "Enter MPIN",
    subtitle:         String  = "",
    errorMessage:     String? = null,
    onPinComplete:    (String) -> Unit,
    onBiometric:      (() -> Unit)? = null,
    modifier:         Modifier = Modifier
) {
    var pin        by remember { mutableStateOf("") }
    var shaking    by remember { mutableStateOf(false) }
    val shakeAnim  = remember { Animatable(0f) }
    val scope      = rememberCoroutineScope()
    val haptic     = LocalHapticFeedback.current

    // Trigger shake on errorMessage change
    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            shaking = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            // Shake: oscillate left/right 3 times
            repeat(3) {
                shakeAnim.animateTo(14f, tween(60, easing = FastOutSlowInEasing))
                shakeAnim.animateTo(-14f, tween(60, easing = FastOutSlowInEasing))
            }
            shakeAnim.animateTo(0f, tween(60))
            shaking = false
            pin = ""  // clear pin after error
        }
    }

    Column(
        modifier            = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(title, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = TextWhite)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted,
                textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(32.dp))

        // PIN dots with shake
        Row(
            modifier = Modifier.graphicsLayer(translationX = shakeAnim.value),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            repeat(pinLength) { i ->
                PinDot(filled = i < pin.length, hasError = !errorMessage.isNullOrBlank())
            }
        }

        // Error text
        Spacer(Modifier.height(14.dp))
        if (!errorMessage.isNullOrBlank()) {
            Text(
                text  = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Red400
            )
        } else {
            Spacer(Modifier.height(18.dp)) // keep layout stable
        }

        Spacer(Modifier.height(32.dp))

        // Numeric keypad
        NumericKeypad(
            onDigit = { digit ->
                if (pin.length < pinLength) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    pin += digit
                    if (pin.length == pinLength) {
                        onPinComplete(pin)
                    }
                }
            },
            onBackspace = {
                if (pin.isNotEmpty()) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    pin = pin.dropLast(1)
                }
            },
            onBiometric = onBiometric
        )
    }
}

// ── Single PIN dot ────────────────────────────────────────────────
@Composable
private fun PinDot(filled: Boolean, hasError: Boolean) {
    val size by animateDpAsState(
        if (filled) 16.dp else 14.dp,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dotSize"
    )
    val dotColor = when {
        hasError && filled -> Red500
        filled             -> Blue400
        else               -> NavyBorder
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (filled) dotColor else Color.Transparent)
            .border(2.dp, dotColor, CircleShape)
    )
}

// ── Numeric keypad grid ───────────────────────────────────────────
@Composable
private fun NumericKeypad(
    onDigit:     (String) -> Unit,
    onBackspace: () -> Unit,
    onBiometric: (() -> Unit)? = null
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("bio", "0", "del")
    )

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                row.forEach { key ->
                    KeypadButton(
                        key         = key,
                        onDigit     = onDigit,
                        onBackspace = onBackspace,
                        onBiometric = onBiometric
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    key:         String,
    onDigit:     (String) -> Unit,
    onBackspace: () -> Unit,
    onBiometric: (() -> Unit)?
) {
    val interactionSource = remember { MutableInteractionSource() }
    val buttonSize: Dp = 72.dp

    when (key) {
        "del" -> {
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .background(Navy700)
                    .border(1.dp, NavyBorder, CircleShape)
                    .clickable(interactionSource = interactionSource, indication = null) { onBackspace() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Delete",
                    tint     = TextSoft,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        "bio" -> {
            if (onBiometric != null) {
                Box(
                    modifier = Modifier
                        .size(buttonSize)
                        .clip(CircleShape)
                        .background(Navy700)
                        .border(1.dp, NavyBorder, CircleShape)
                        .clickable(interactionSource = interactionSource, indication = null) { onBiometric() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Fingerprint,
                        contentDescription = "Biometric",
                        tint     = Blue400,
                        modifier = Modifier.size(26.dp)
                    )
                }
            } else {
                // Empty placeholder to keep grid aligned
                Spacer(Modifier.size(buttonSize))
            }
        }
        else -> {
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Navy700, Navy800))
                    )
                    .border(1.dp, NavyBorder, CircleShape)
                    .clickable(interactionSource = interactionSource, indication = null) { onDigit(key) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = key,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextWhite
                )
            }
        }
    }
}
