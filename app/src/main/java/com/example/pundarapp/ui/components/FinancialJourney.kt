package com.example.pundarapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pundarapp.ui.theme.*

private val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

private data class JourneyStep(
    val label: String,
    val icon: ImageVector,
    val accent: Color,
    val completed: Boolean,
    val active: Boolean
)

@Composable
fun FinancialJourneySection(modifier: Modifier = Modifier) {
    val steps = listOf(
        JourneyStep("Spend", Icons.Filled.ShoppingBag, NeonGreen, completed = true, active = false),
        JourneyStep("Save", Icons.Filled.Savings, NeonGreen, completed = true, active = false),
        JourneyStep("Grow", Icons.Filled.TrendingUp, ElectricBlue, completed = true, active = true),
        JourneyStep("Score", Icons.Filled.EmojiEvents, PremiumGoldWarm, completed = false, active = false),
        JourneyStep("Access", Icons.Filled.VpnKey, NeonCyan, completed = false, active = false)
    )

    val progressAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progressAnim.animateTo(0.6f, tween(1400, easing = EaseOutExpo))
    }

    Column(modifier) {
        Text(
            "Your Financial Journey",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = TextPrimary,
            letterSpacing = 0.2.sp
        )
        Spacer(Modifier.height(12.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = ElectricBlue.copy(0.15f))
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(SpaceDeep, Color(0xFF0E1825))))
                .border(
                    1.dp,
                    Brush.linearGradient(listOf(ElectricBlue.copy(0.35f), GlassWhite, ElectricPurple.copy(0.2f))),
                    RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 12.dp, vertical = 20.dp)
        ) {
            Column {
                // Progress track
                Box(Modifier.fillMaxWidth().height(4.dp).padding(horizontal = 24.dp)) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(2.dp))
                            .background(SpaceMedium)
                    )
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressAnim.value)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(NeonGreen, ElectricBlue, NeonCyan)
                                )
                            )
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    steps.forEachIndexed { index, step ->
                        JourneyNode(
                            step = step,
                            index = index,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyNode(
    step: JourneyStep,
    index: Int,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 100L)
        alpha.animateTo(1f, tween(350))
        scale.animateTo(1f, tween(450, easing = EaseOutExpo))
    }

    val infinite = rememberInfiniteTransition(label = "pulse_${step.label}")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (step.active) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            tween(if (step.active) 900 else 1, easing = EaseOutExpo),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val ringAlpha by infinite.animateFloat(
        initialValue = 0.3f,
        targetValue = if (step.active) 0.9f else 0.3f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "ring"
    )

    Column(
        modifier = modifier.graphicsLayer(
            scaleX = scale.value * pulse,
            scaleY = scale.value * pulse,
            alpha = alpha.value
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (step.active || step.completed) {
                Box(
                    Modifier
                        .size(48.dp)
                        .graphicsLayer(alpha = ringAlpha)
                        .clip(CircleShape)
                        .background(step.accent.copy(0.2f))
                )
            }
            Box(
                Modifier
                    .size(40.dp)
                    .shadow(
                        if (step.completed || step.active) 8.dp else 2.dp,
                        CircleShape,
                        ambientColor = step.accent.copy(0.4f),
                        spotColor = step.accent.copy(0.4f)
                    )
                    .clip(CircleShape)
                    .background(
                        if (step.completed || step.active)
                            Brush.linearGradient(listOf(step.accent.copy(0.35f), step.accent.copy(0.12f)))
                        else
                            Brush.linearGradient(listOf(SpaceMedium, SpaceDeep))
                    )
                    .border(
                        1.5.dp,
                        if (step.completed || step.active) step.accent.copy(0.7f) else SpaceBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (step.completed && !step.active) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = step.accent,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        step.icon,
                        contentDescription = step.label,
                        tint = if (step.completed || step.active) step.accent else TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = step.label,
            fontSize = 9.sp,
            fontWeight = if (step.active) FontWeight.Black else if (step.completed) FontWeight.Bold else FontWeight.Medium,
            color = when {
                step.active -> step.accent
                step.completed -> TextPrimary
                else -> TextTertiary
            },
            textAlign = TextAlign.Center,
            letterSpacing = 0.3.sp
        )
    }
}
