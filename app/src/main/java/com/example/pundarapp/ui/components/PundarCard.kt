package com.example.pundarapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.pundarapp.ui.theme.*

// ── Standard surface card ─────────────────────────────────────────
@Composable
fun PundarCard(
    modifier:     Modifier = Modifier,
    accentColor:  Color?   = null,
    cornerRadius: Dp       = 20.dp,
    content:      @Composable ColumnScope.() -> Unit
) {
    val borderBrush = if (accentColor != null)
        Brush.linearGradient(listOf(accentColor.copy(0.40f), Glass10))
    else
        Brush.linearGradient(listOf(Glass15, Glass10))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(listOf(Navy800, Navy700)))
            .border(1.dp, borderBrush, RoundedCornerShape(cornerRadius))
    ) {
        // Accent top-edge line
        if (accentColor != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, accentColor.copy(0.6f), Color.Transparent)
                        )
                    )
            )
        }
        Column(
            modifier = Modifier.padding(20.dp),
            content  = content
        )
    }
}

// ── Accent card (gold / highlighted) ─────────────────────────────
@Composable
fun PundarAccentCard(
    modifier:    Modifier = Modifier,
    accentColor: Color    = Gold500,
    content:     @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Navy800, Navy700)))
            .border(
                1.dp,
                Brush.linearGradient(listOf(accentColor.copy(0.5f), Glass10)),
                RoundedCornerShape(20.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Brush.horizontalGradient(
                    listOf(Color.Transparent, accentColor.copy(0.7f), Color.Transparent)
                ))
        )
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

// ── Glass card ────────────────────────────────────────────────────
@Composable
fun GlassCard(
    modifier:     Modifier = Modifier,
    cornerRadius: Dp       = 20.dp,
    content:      @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(listOf(Glass15, Glass10)))
            .border(1.dp, Glass20, RoundedCornerShape(cornerRadius))
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

// ════════════════════════════════════════════════════════════════
//  BUTTONS
// ════════════════════════════════════════════════════════════════

// Primary — filled royal blue
@Composable
fun PundarPrimaryButton(
    text:      String,
    onClick:   () -> Unit,
    modifier:  Modifier = Modifier,
    enabled:   Boolean  = true,
    isLoading: Boolean  = false
) {
    val src      = remember { MutableInteractionSource() }
    val pressed  by src.collectIsPressedAsState()
    val canClick = enabled && !isLoading
    val scale    by animateFloatAsState(
        if (pressed && canClick) 0.97f else 1f,
        spring(stiffness = Spring.StiffnessHigh), label = "ps"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (canClick) Brush.linearGradient(listOf(Blue500, Blue600))
                else Brush.linearGradient(listOf(Navy600, Navy600))
            )
    ) {
        Button(
            onClick           = onClick,
            modifier          = Modifier.fillMaxSize(),
            enabled           = canClick,
            shape             = RoundedCornerShape(16.dp),
            colors            = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            elevation         = ButtonDefaults.buttonElevation(0.dp),
            interactionSource = src
        ) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp), color = White, strokeWidth = 2.dp)
            } else {
                Text(text, fontWeight = FontWeight.SemiBold, color = if (canClick) White else TextDim)
            }
        }
    }
}

// Secondary — outlined blue
@Composable
fun PundarSecondaryButton(
    text:     String,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    val src     = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale   by animateFloatAsState(
        if (pressed) 0.97f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "ss"
    )
    OutlinedButton(
        onClick           = onClick,
        modifier          = modifier.fillMaxWidth().height(54.dp).graphicsLayer(scaleX = scale, scaleY = scale),
        shape             = RoundedCornerShape(16.dp),
        border            = BorderStroke(1.dp, Brush.linearGradient(listOf(Blue400.copy(0.7f), Blue300.copy(0.4f)))),
        colors            = ButtonDefaults.outlinedButtonColors(contentColor = Blue300),
        interactionSource = src
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

// Gold primary — for key financial CTAs
@Composable
fun PundarGoldButton(
    text:      String,
    onClick:   () -> Unit,
    modifier:  Modifier = Modifier,
    enabled:   Boolean  = true,
    isLoading: Boolean  = false
) {
    val src      = remember { MutableInteractionSource() }
    val pressed  by src.collectIsPressedAsState()
    val canClick = enabled && !isLoading
    val scale    by animateFloatAsState(
        if (pressed && canClick) 0.97f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "gs"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (canClick) Brush.linearGradient(listOf(Gold500, GoldWarm))
                else Brush.linearGradient(listOf(Navy600, Navy600))
            )
    ) {
        Button(
            onClick           = onClick,
            modifier          = Modifier.fillMaxSize(),
            enabled           = canClick,
            shape             = RoundedCornerShape(16.dp),
            colors            = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            elevation         = ButtonDefaults.buttonElevation(0.dp),
            interactionSource = src
        ) {
            if (isLoading)
                CircularProgressIndicator(Modifier.size(20.dp), color = Navy950, strokeWidth = 2.dp)
            else
                Text(text, fontWeight = FontWeight.SemiBold, color = if (canClick) Navy950 else TextDim)
        }
    }
}

// Outlined — thin border, transparent bg
@Composable
fun PundarOutlinedButton(
    text:     String,
    onClick:  () -> Unit,
    modifier: Modifier  = Modifier,
    enabled:  Boolean   = true,
    icon:     ImageVector? = null
) {
    val src     = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale   by animateFloatAsState(
        if (pressed && enabled) 0.97f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "os"
    )
    OutlinedButton(
        onClick           = onClick,
        modifier          = modifier.height(54.dp).graphicsLayer(scaleX = scale, scaleY = scale),
        enabled           = enabled,
        shape             = RoundedCornerShape(16.dp),
        border            = BorderStroke(1.dp, Blue400.copy(if (enabled) 0.5f else 0.2f)),
        colors            = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
        interactionSource = src
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(17.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

// Text button
@Composable
fun PundarTextButton(
    text:    String,
    onClick: () -> Unit,
    modifier: Modifier     = Modifier,
    enabled: Boolean       = true,
    icon:    ImageVector?  = null
) {
    TextButton(onClick = onClick, modifier = modifier.height(44.dp), enabled = enabled, shape = RoundedCornerShape(12.dp)) {
        if (icon != null) { Icon(icon, null, Modifier.size(17.dp), tint = Blue400); Spacer(Modifier.width(6.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge, color = Blue400, fontWeight = FontWeight.SemiBold)
    }
}

// Blue button — alias kept for backward compat
@Composable
fun PundarBlueButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    PundarPrimaryButton(text = text, onClick = onClick, modifier = modifier)

// Small utility button
@Composable
fun PundarSmallButton(
    text:           String,
    onClick:        () -> Unit,
    modifier:       Modifier = Modifier,
    containerColor: Color    = Navy600,
    contentColor:   Color    = Blue300
) {
    val src     = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale   by animateFloatAsState(if (pressed) 0.93f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "sbs")
    Button(
        onClick           = onClick,
        modifier          = modifier.height(34.dp).graphicsLayer(scaleX = scale, scaleY = scale),
        shape             = RoundedCornerShape(10.dp),
        colors            = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        elevation         = ButtonDefaults.buttonElevation(0.dp),
        contentPadding    = PaddingValues(horizontal = 14.dp),
        interactionSource = src
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}
