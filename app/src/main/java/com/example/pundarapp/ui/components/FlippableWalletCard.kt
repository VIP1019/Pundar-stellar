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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme

// ── Custom easing curves ──────────────────────────────────────────
private val ExpoOut  = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val BackOut  = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
private val SineIO   = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

// ── Card gradient palettes ────────────────────────────────────────
private val FrontGrad = listOf(
    Color(0xFF0A1F5C),   // deep navy
    Color(0xFF0D2880),   // royal blue
    Color(0xFF071545)    // midnight
)
private val FrontGradLight = listOf(
    Color(0xFFE0E7FF),
    Color(0xFFEEF2FF),
    Color(0xFFF5F8FF)
)

private val BackGrad = listOf(
    Color(0xFF0C0824),
    Color(0xFF180F48),
    Color(0xFF0A0618)
)
private val BackGradLight = listOf(
    Color(0xFFF3F4F6),
    Color(0xFFFFFFFF),
    Color(0xFFE5E7EB)
)

@Composable
fun FlippableWalletCard(
    modifier:   Modifier  = Modifier,
    onTransfer: () -> Unit,
    onCashIn:   () -> Unit = {},
    onReceive:  () -> Unit = {}
) {
    val density    = LocalDensity.current
    var isFlipped  by remember { mutableStateOf(false) }
    val rotation   = remember { Animatable(0f) }
    val enterScale = remember { Animatable(0.88f) }
    val enterAlpha = remember { Animatable(0f) }
    val enterY     = remember { Animatable(24f) }

    LaunchedEffect(Unit) {
        enterAlpha.animateTo(1f, tween(500, easing = ExpoOut))
        enterScale.animateTo(1f, tween(560, easing = BackOut))
        enterY.animateTo(0f,     tween(500, easing = ExpoOut))
    }
    LaunchedEffect(isFlipped) {
        rotation.animateTo(
            if (isFlipped) 180f else 0f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    val inf      = rememberInfiniteTransition(label = "wlt")
    val floatY   by inf.animateFloat(-2.5f, 2.5f,
        infiniteRepeatable(tween(3200, easing = SineIO), RepeatMode.Reverse), label = "fy")
    val shimmerX by inf.animateFloat(-800f, 1600f,
        infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "sx")
    val glowPulse by inf.animateFloat(0.55f, 1f,
        infiniteRepeatable(tween(2000, easing = SineIO), RepeatMode.Reverse), label = "gp")

    Column(modifier = modifier.fillMaxWidth()) {

        // ── Card shell with glow shadow ───────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .graphicsLayer {
                    alpha          = enterAlpha.value
                    scaleX         = enterScale.value
                    scaleY         = enterScale.value
                    translationY   = floatY + enterY.value
                    rotationY      = rotation.value
                    cameraDistance = 16f * density.density
                }
        ) {
            // The actual card (clickable to flip)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(
                        elevation    = (20 + glowPulse * 16).dp,
                        shape        = RoundedCornerShape(28.dp),
                        ambientColor = PundarTheme.colors.brandPrimary.copy(glowPulse * 0.5f),
                        spotColor    = PundarTheme.colors.brandSecondary.copy(glowPulse * 0.5f)
                    )
                    .clickable { isFlipped = !isFlipped }
            ) {
                if (rotation.value <= 90f) {
                    WalletFront(shimmerX = shimmerX, glowPulse = glowPulse)
                } else {
                    Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                        WalletBack(shimmerX = shimmerX)
                    }
                }
            }
        }

        // ── Flip hint ─────────────────────────────────────────────
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            val dotAlpha by rememberInfiniteTransition(label = "dot")
                .animateFloat(0.3f, 0.9f,
                    infiniteRepeatable(tween(1200, easing = SineIO), RepeatMode.Reverse), label = "da")
            Box(
                Modifier.size(5.dp).clip(CircleShape)
                    .background(Blue400.copy(dotAlpha))
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (isFlipped) "Tap to view balance" else "Tap card to flip",
                style    = MaterialTheme.typography.labelSmall,
                color    = PundarTheme.colors.textDim,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.size(5.dp).clip(CircleShape)
                    .background(Blue400.copy(dotAlpha))
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Action buttons ────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WalletAction(
                icon    = Icons.Filled.AddCircleOutline,
                label   = "Cash In",
                accent  = PundarTheme.colors.accentGreen,
                modifier = Modifier.weight(1f),
                onClick = onCashIn
            )
            WalletAction(
                icon    = Icons.AutoMirrored.Filled.Send,
                label   = "Send",
                accent  = Blue400,
                modifier = Modifier.weight(1f),
                onClick = onTransfer
            )
            WalletAction(
                icon    = Icons.Filled.QrCodeScanner,
                label   = "Receive",
                accent  = PundarTheme.colors.accentGold,
                modifier = Modifier.weight(1f),
                onClick = onReceive
            )
        }
    }
}

// ── Action button ─────────────────────────────────────────────────
@Composable
private fun WalletAction(
    icon: ImageVector, label: String,
    accent: Color, modifier: Modifier,
    onClick: () -> Unit
) {
    val inf = rememberInfiniteTransition(label = "wa_$label")
    val pulse by inf.animateFloat(0.7f, 1f,
        infiniteRepeatable(tween(1800, easing = SineIO), RepeatMode.Reverse), label = "wp")

    Column(
        modifier = modifier
            .shadow(
                elevation    = (6 * pulse).dp,
                shape        = RoundedCornerShape(18.dp),
                ambientColor = accent.copy(pulse * 0.25f),
                spotColor    = accent.copy(pulse * 0.25f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(0.13f), accent.copy(0.07f))
                )
            )
            .border(1.dp, accent.copy(0.28f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape)
                .background(accent.copy(0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = accent)
    }
}

// ── FRONT face ────────────────────────────────────────────────────
@Composable
private fun WalletFront(shimmerX: Float, glowPulse: Float) {
    val inf  = rememberInfiniteTransition(label = "wf")
    val orbY by inf.animateFloat(-16f, 16f,
        infiniteRepeatable(tween(3000, easing = SineIO), RepeatMode.Reverse), label = "oy")
    val orbX by inf.animateFloat(-8f, 8f,
        infiniteRepeatable(tween(4000, easing = SineIO), RepeatMode.Reverse), label = "ox")

    val isLight = PundarTheme.colors.isLight
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(if (isLight) FrontGradLight else FrontGrad, start = Offset(0f, 0f), end = Offset(800f, 600f)))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(PundarTheme.colors.brandLight.copy(if(isLight) 0.8f else 0.55f), Color.Transparent, PundarTheme.colors.brandPrimary.copy(0.15f)),
                    start = Offset(0f, 0f), end = Offset(800f, 600f)
                ),
                RoundedCornerShape(28.dp)
            )
    ) {
        // ── Background orbs ───────────────────────────────────────
        Box(
            Modifier.size(260.dp)
                .align(Alignment.TopEnd)
                .offset(x = (50 + orbX).dp, y = (-60 + orbY).dp)
                .background(
                    Brush.radialGradient(
                        listOf(Blue400.copy(glowPulse * 0.22f), Color.Transparent)
                    )
                )
        )
        Box(
            Modifier.size(180.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = 40.dp)
                .background(
                    Brush.radialGradient(
                        listOf(PundarTheme.colors.accentGold.copy(0.14f), Color.Transparent)
                    )
                )
        )
        Box(
            Modifier.size(100.dp)
                .align(Alignment.Center)
                .offset(x = orbX.dp, y = orbY.dp)
                .background(
                    Brush.radialGradient(
                        listOf(PundarTheme.colors.brandLight.copy(glowPulse * 0.08f), Color.Transparent)
                    )
                )
        )

        // ── Shimmer sweep ─────────────────────────────────────────
        Box(
            Modifier.matchParentSize().background(
                Brush.linearGradient(
                    listOf(Color.Transparent, PundarTheme.colors.surfacePrimary.copy(0.055f), Color.Transparent),
                    start = Offset(shimmerX, 0f), end = Offset(shimmerX + 300f, 400f)
                )
            )
        )

        // ── Top edge highlight ────────────────────────────────────
        Box(
            Modifier.fillMaxWidth().height(1.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Blue200.copy(0.8f), Color.Transparent)
                    )
                )
        )

        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // ── Row 1: Brand + chip ───────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Brand wordmark
                Column {
                    Text(
                        "PUNDAR",
                        fontSize      = 18.sp,
                        fontWeight    = FontWeight.Black,
                        color         = PundarTheme.colors.textPrimary,
                        letterSpacing = 3.sp
                    )
                    Text(
                        "e-wallet",
                        fontSize      = 9.sp,
                        fontWeight    = FontWeight.Medium,
                        color         = Blue200.copy(0.65f),
                        letterSpacing = 2.sp
                    )
                }
                // NFC + currency chip
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Contactless icon
                    Icon(
                        Icons.Filled.Wifi,
                        contentDescription = null,
                        tint     = Blue200.copy(0.55f),
                        modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = 90f }
                    )
                    Spacer(Modifier.width(8.dp))
                    // Currency badge
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(PundarTheme.colors.brandPrimary.copy(0.35f), PundarTheme.colors.brandSecondary.copy(0.2f))
                                )
                            )
                            .border(1.dp, PundarTheme.colors.brandLight.copy(0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(AppState.preferredCurrency.value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Blue200)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Row 2: Balance ────────────────────────────────────
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Available Balance",
                        fontSize      = 10.sp,
                        color         = Blue200.copy(0.65f),
                        letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .clickable { AppState.toggleBalanceVisibility() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (AppState.isBalanceHidden.value)
                                Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Toggle",
                            tint     = Blue200.copy(0.55f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    AppState.getDisplayBalance(),
                    fontSize      = 34.sp,
                    fontWeight    = FontWeight.Black,
                    color         = PundarTheme.colors.textPrimary,
                    letterSpacing = (-0.8).sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    AppState.getFiatDisplayBalance(),
                    fontSize      = 14.sp,
                    fontWeight    = FontWeight.Medium,
                    color         = PundarTheme.colors.accentGreen,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Row 3: Footer — verified badge + flip hint ────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(6.dp).clip(CircleShape)
                            .background(PundarTheme.colors.accentGreen)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "Verified Account",
                        fontSize  = 9.sp,
                        color     = PundarTheme.colors.accentGreen.copy(0.85f),
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                // Holographic pattern dots
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(4) { i ->
                        Box(
                            Modifier.size(4.dp).clip(CircleShape)
                                .background(PundarTheme.colors.brandLight.copy(0.2f + i * 0.12f))
                        )
                    }
                }
            }
        }
    }
}

// ── BACK face ─────────────────────────────────────────────────────
@Composable
private fun WalletBack(shimmerX: Float) {
    val userName = AuthRepository.getCurrentUserName()
    val initials = AuthRepository.getCurrentUserInitials()
    val userId   = AuthRepository.getCurrentUserPhone().ifBlank { "0000" }
    val last4    = userId.takeLast(4).padStart(4, '0')

    val inf  = rememberInfiniteTransition(label = "wb")
    val orbY by inf.animateFloat(-10f, 10f,
        infiniteRepeatable(tween(3400, easing = SineIO), RepeatMode.Reverse), label = "by")

    val isLight = PundarTheme.colors.isLight
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(if (isLight) BackGradLight else BackGrad, start = Offset(0f, 0f), end = Offset(600f, 500f))
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(Color(0xFF8B5CF6).copy(0.45f), Color.Transparent, Color(0xFF8B5CF6).copy(0.12f))
                ),
                RoundedCornerShape(28.dp)
            )
    ) {
        // Purple orb
        Box(
            Modifier.size(220.dp).align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-30 + orbY).dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF7C3AED).copy(0.18f), Color.Transparent)
                    )
                )
        )
        Box(
            Modifier.size(140.dp).align(Alignment.BottomStart)
                .offset(x = (-20).dp, y = 30.dp)
                .background(
                    Brush.radialGradient(
                        listOf(PundarTheme.colors.accentGold.copy(0.12f), Color.Transparent)
                    )
                )
        )

        Box(
            Modifier.matchParentSize().background(
                Brush.linearGradient(
                    listOf(Color.Transparent, PundarTheme.colors.surfacePrimary.copy(0.045f), Color.Transparent),
                    start = Offset(shimmerX, 0f), end = Offset(shimmerX + 250f, 350f)
                )
            )
        )

        // Top edge
        Box(
            Modifier.fillMaxWidth().height(1.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFFA78BFA).copy(0.65f), Color.Transparent)
                    )
                )
        )

        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "VIRTUAL CARD",
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 2.5.sp,
                    color         = Color(0xFFA78BFA).copy(0.85f)
                )
                Icon(
                    Icons.Filled.CreditCard,
                    contentDescription = null,
                    tint     = Gold400,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // SIM chip
            Box(
                Modifier
                    .size(width = 42.dp, height = 30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.linearGradient(listOf(PundarTheme.colors.accentGold, Gold400, GoldWarm))
                    )
                    .border(1.dp, PundarTheme.colors.surfacePrimary.copy(0.20f), RoundedCornerShape(6.dp))
            ) {
                // Chip lines
                Column(
                    Modifier.fillMaxSize().padding(4.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(3) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(PundarTheme.colors.surfacePrimary.copy(0.25f)))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "•••• •••• •••• $last4",
                fontSize      = 16.sp,
                fontWeight    = FontWeight.Bold,
                color         = PundarTheme.colors.textPrimary,
                letterSpacing = 2.5.sp
            )

            Spacer(Modifier.weight(1f))

            // Footer: cardholder + avatar
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "CARDHOLDER",
                        fontSize      = 8.sp,
                        color         = PundarTheme.colors.textDim,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        userName.uppercase(),
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.SemiBold,
                        color         = PundarTheme.colors.textSecondary,
                        letterSpacing = 0.5.sp
                    )
                }

                // Two overlapping circles (Mastercard-style)
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(32.dp).offset(x = (-8).dp)
                            .clip(CircleShape)
                            .background(PundarTheme.colors.brandPrimary.copy(0.75f))
                            .border(1.dp, PundarTheme.colors.brandLight.copy(0.4f), CircleShape)
                    )
                    Box(
                        Modifier.size(32.dp).offset(x = 8.dp)
                            .clip(CircleShape)
                            .background(PundarTheme.colors.accentGold.copy(0.65f))
                            .border(1.dp, Gold300.copy(0.4f), CircleShape)
                    )
                    // Initials overlay at center intersection
                    Text(
                        initials,
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = PundarTheme.colors.surfacePrimary,
                        textAlign  = TextAlign.Center
                    )
                }
            }
        }
    }
}
