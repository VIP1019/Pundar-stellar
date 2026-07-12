package com.example.pundarapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.theme.*

private val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val EaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

@Composable
fun FlippableWalletCard(
    modifier: Modifier = Modifier,
    onTransfer: () -> Unit
) {
    val density = LocalDensity.current
    var isFlipped by remember { mutableStateOf(false) }
    val rotation = remember { Animatable(0f) }
    val enterScale = remember { Animatable(0.88f) }
    val enterAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        enterAlpha.animateTo(1f, tween(500, easing = EaseOutExpo))
        enterScale.animateTo(1f, tween(550, easing = EaseOutBack))
    }

    LaunchedEffect(isFlipped) {
        rotation.animateTo(
            targetValue = if (isFlipped) 180f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    val infinite = rememberInfiniteTransition(label = "walletFloat")
    val floatY by infinite.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(2800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "floatY"
    )
    val shimmerX by infinite.animateFloat(
        initialValue = -700f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "shimmer"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .graphicsLayer {
                alpha = enterAlpha.value
                scaleX = enterScale.value
                scaleY = enterScale.value
                translationY = floatY
                rotationY = rotation.value
                cameraDistance = 14f * density.density
            }
            .clickable { isFlipped = !isFlipped }
    ) {
        if (rotation.value <= 90f) {
            WalletCardFront(shimmerX = shimmerX, onTransfer = onTransfer)
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
            ) {
                WalletCardBack(shimmerX = shimmerX)
            }
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Refresh,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = if (isFlipped) "Tap to view balance" else "Tap card to flip",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun WalletCardFront(shimmerX: Float, onTransfer: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "orb")
    val orbY by infinite.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(2600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "orbY"
    )

    Box(
        Modifier
            .fillMaxSize()
            .shadow(28.dp, RoundedCornerShape(28.dp),
                ambientColor = ElectricBlue.copy(0.28f), spotColor = ElectricBlue.copy(0.28f))
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0A1E3D), Color(0xFF0D2A52), Color(0xFF06121E))))
    ) {
        Box(Modifier.size(180.dp).align(Alignment.TopEnd)
            .offset(x = 30.dp, y = (orbY - 30).dp)
            .background(Brush.radialGradient(listOf(ElectricBlue.copy(0.18f), Color.Transparent))))
        Box(Modifier.size(140.dp).align(Alignment.BottomStart)
            .offset(x = (-20).dp, y = (20 - orbY).dp)
            .background(Brush.radialGradient(listOf(ElectricPurple.copy(0.13f), Color.Transparent))))

        Box(Modifier.matchParentSize().background(
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(0.06f), Color.Transparent),
                start = Offset(shimmerX, 0f),
                end = Offset(shimmerX + 260f, 400f)
            )
        ))
        Box(Modifier.fillMaxWidth().height(2.dp)
            .background(Brush.horizontalGradient(
                listOf(Color.Transparent, ElectricBlue.copy(0.8f), NeonCyan.copy(0.6f), Color.Transparent))))

        Column(Modifier.padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("PUNDAR WALLET", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.5.sp, color = ElectricBlue.copy(0.9f))
                Box(Modifier.clip(RoundedCornerShape(6.dp))
                    .background(ElectricBlue.copy(0.15f))
                    .border(1.dp, ElectricBlue.copy(0.45f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text("PHP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Available Balance", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "₱${String.format("%,.2f", AppState.walletBalance.value)}",
                fontWeight = FontWeight.Black,
                fontSize = 36.sp,
                color = TextOnDark,
                letterSpacing = (-1).sp
            )
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .shadow(12.dp, RoundedCornerShape(14.dp),
                        ambientColor = PremiumGoldWarm.copy(0.45f), spotColor = PremiumGoldWarm.copy(0.45f))
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(PremiumGoldDim, PremiumGoldWarm)))
                    .clickable(onClick = onTransfer),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = SpaceBlack, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Transfer Money", color = SpaceBlack, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun WalletCardBack(shimmerX: Float) {
    val userName = AuthRepository.getCurrentUserName()
    val initials = AuthRepository.getCurrentUserInitials()
    val userId = AuthRepository.getCurrentUserPhone().ifBlank { "•••• ••••" }

    Box(
        Modifier
            .fillMaxSize()
            .shadow(28.dp, RoundedCornerShape(28.dp),
                ambientColor = ElectricPurple.copy(0.25f), spotColor = ElectricPurple.copy(0.25f))
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1A0A3D), Color(0xFF2D1560), Color(0xFF0A1628))
                )
            )
    ) {
        Box(Modifier.matchParentSize().background(
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(0.05f), Color.Transparent),
                start = Offset(shimmerX, 0f),
                end = Offset(shimmerX + 200f, 300f)
            )
        ))

        Column(Modifier.padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("VIRTUAL CARD", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp, color = NeonCyan.copy(0.9f))
                Icon(Icons.Filled.CreditCard, null, tint = PremiumGoldWarm, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(20.dp))
            // Chip
            Box(
                Modifier
                    .size(width = 44.dp, height = 32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.linearGradient(listOf(PremiumGoldWarm, PremiumGoldDim)))
                    .border(1.dp, Color.White.copy(0.3f), RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "•••• •••• •••• ${userId.takeLast(4).padStart(4, '•')}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextOnDark,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("CARDHOLDER", fontSize = 8.sp, color = TextTertiary, letterSpacing = 1.sp)
                    Text(userName.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(ElectricBlueDeep, ElectricPurple))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials, fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}
