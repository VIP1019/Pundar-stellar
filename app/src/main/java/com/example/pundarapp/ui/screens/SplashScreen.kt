package com.example.pundarapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.components.Icon3DStar
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ── Particle data ────────────────────────────────────────────────
private data class Particle(
    val x: Float, val y: Float,
    val radius: Float,
    val alpha: Float,
    val color: Color
)

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {

    // ── Animation states ─────────────────────────────────────────
    val logoScale    = remember { Animatable(0f) }
    val logoAlpha    = remember { Animatable(0f) }
    val ringScale    = remember { Animatable(0f) }
    val textAlpha    = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val badgeAlpha   = remember { Animatable(0f) }
    val screenAlpha  = remember { Animatable(1f) }

    // ── Infinite pulse ring ──────────────────────────────────────
    val infinite = rememberInfiniteTransition(label = "splashPulse")
    val pulseRing by infinite.animateFloat(
        initialValue = 1f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = EaseOutCubic), RepeatMode.Restart
        ), label = "pulseRing"
    )
    val pulseRingAlpha by infinite.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = LinearEasing), RepeatMode.Restart
        ), label = "pulseAlpha"
    )
    val rotateOrbit by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(6000, easing = LinearEasing), RepeatMode.Restart
        ), label = "orbit"
    )

    // ── Static random particles ──────────────────────────────────
    val particles = remember {
        List(22) {
            Particle(
                x      = Random.nextFloat(),
                y      = Random.nextFloat(),
                radius = Random.nextFloat() * 2.5f + 0.8f,
                alpha  = Random.nextFloat() * 0.5f + 0.1f,
                color  = if (Random.nextBoolean()) ElectricBlue else NeonCyan
            )
        }
    }

    // ── Entrance choreography ────────────────────────────────────
    LaunchedEffect(Unit) {
        // Ring expand
        ringScale.animateTo(1f, tween(600, easing = EaseOutBack))
        // Logo scale-in with bounce
        logoScale.animateTo(1.15f, tween(350, easing = FastOutSlowInEasing))
        logoAlpha.animateTo(1f,   tween(300))
        logoScale.animateTo(1f,   tween(200, easing = FastOutSlowInEasing))
        // Text fade
        textAlpha.animateTo(1f, tween(400))
        delay(150)
        taglineAlpha.animateTo(1f, tween(400))
        delay(100)
        badgeAlpha.animateTo(1f, tween(400))
        // Hold
        delay(900)
        // Fade out
        screenAlpha.animateTo(0f, tween(450))
        onSplashFinished()
    }

    // ── Full-screen container ────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha.value)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0A1832),
                        SpaceNavy,
                        SpaceBlack
                    ),
                    radius = 1600f
                )
            )
    ) {

        // ── Particle field ───────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                drawCircle(
                    color  = p.color.copy(alpha = p.alpha),
                    radius = p.radius.dp.toPx(),
                    center = Offset(p.x * size.width, p.y * size.height)
                )
            }
        }

        // ── Background glow orbs ─────────────────────────────────
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = (-60).dp, y = (-80).dp)
                .blur(100.dp)
                .background(ElectricBlue.copy(alpha = 0.12f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 60.dp)
                .blur(80.dp)
                .background(NeonPurple.copy(alpha = 0.10f), CircleShape)
        )

        // ── Center content ───────────────────────────────────────
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── Logo assembly ────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Pulse ring (outer)
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseRing)
                        .alpha(pulseRingAlpha)
                        .clip(CircleShape)
                        .background(ElectricBlue.copy(alpha = 0.2f))
                )

                // Static ring
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(ringScale.value)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1A3A60).copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Orbiting dot
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .alpha(ringScale.value)
                        .graphicsLayer(rotationZ = rotateOrbit)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = 8.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(NeonCyan, NeonCyan.copy(alpha = 0f))
                                )
                            )
                    )
                }

                // Logo circle — gradient border ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    ElectricBlue,
                                    ElectricPurple,
                                    PremiumGoldWarm
                                )
                            )
                        )
                ) {
                    // Inner dark circle (creates ring effect)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(SpaceDeep)
                    ) {
                        // Glass inner fill
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(94.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF0D2040),
                                            SpaceDeep
                                        )
                                    )
                                )
                        ) {
                            Text(
                                text = "P",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                color = ElectricBlue
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── App name ─────────────────────────────────────────
            Text(
                text = "PUNDAR",
                fontWeight = FontWeight.Black,
                fontSize = 36.sp,
                letterSpacing = 8.sp,
                color = TextOnDark,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(Modifier.height(6.dp))

            // Neon underline bar
            Box(
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .width(120.dp)
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, ElectricBlue, Color.Transparent)
                        )
                    )
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Spend · Save · Grow · Together",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                letterSpacing = 1.5.sp,
                modifier = Modifier
                    .alpha(taglineAlpha.value)
                    .padding(horizontal = 32.dp)
            )

            Spacer(Modifier.height(40.dp))

            // ── Stellar badge ────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .alpha(badgeAlpha.value)
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                ElectricBlue.copy(alpha = 0.15f),
                                NeonPurple.copy(alpha = 0.10f)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                // Glass border effect via outline
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon3DStar(size = 14.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Powered by Stellar Blockchain",
                        style = MaterialTheme.typography.labelMedium,
                        color = PremiumGoldWarm,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

private val EaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
