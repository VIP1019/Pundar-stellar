package com.example.pundarapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.pundarapp.ui.components.Icon3DStar
import com.example.pundarapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random

private data class Particle(val x: Float, val y: Float, val r: Float, val a: Float)

private val EaseOutBack_  = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
private val EaseOutExpo_  = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val logoScale    = remember { Animatable(0f) }
    val logoAlpha    = remember { Animatable(0f) }
    val ringScale    = remember { Animatable(0f) }
    val textAlpha    = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val badgeAlpha   = remember { Animatable(0f) }
    val screenAlpha  = remember { Animatable(1f) }

    val inf = rememberInfiniteTransition(label = "splash")
    val pulseRing by inf.animateFloat(
        1f, 1.55f,
        infiniteRepeatable(tween(1600, easing = EaseOutExpo_), RepeatMode.Restart),
        label = "pr"
    )
    val pulseAlpha by inf.animateFloat(
        0.45f, 0f,
        infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "pa"
    )
    val orbit by inf.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "orb"
    )
    val orb1Y by inf.animateFloat(
        -18f, 18f,
        infiniteRepeatable(tween(3200, easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)), RepeatMode.Reverse),
        label = "o1"
    )

    val particles = remember {
        List(18) {
            Particle(Random.nextFloat(), Random.nextFloat(),
                Random.nextFloat() * 2f + 0.6f, Random.nextFloat() * 0.3f + 0.06f)
        }
    }

    LaunchedEffect(Unit) {
        ringScale.animateTo(1f, tween(600, easing = EaseOutBack_))
        logoScale.animateTo(1.1f, tween(320, easing = FastOutSlowInEasing))
        logoAlpha.animateTo(1f, tween(280))
        logoScale.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
        textAlpha.animateTo(1f, tween(380, easing = EaseOutExpo_))
        delay(130)
        taglineAlpha.animateTo(1f, tween(350, easing = EaseOutExpo_))
        delay(100)
        badgeAlpha.animateTo(1f, tween(320, easing = EaseOutExpo_))
        delay(950)
        screenAlpha.animateTo(0f, tween(400))
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha.value)
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF0C1A3D), Navy900, Navy950),
                    radius = 1800f
                )
            )
    ) {
        // Background ambient orbs
        Box(
            Modifier.size(320.dp).align(Alignment.TopCenter)
                .offset(y = ((-50).dp + orb1Y.dp))
                .blur(120.dp)
                .background(Blue500.copy(0.12f), CircleShape)
        )
        Box(
            Modifier.size(200.dp).align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 50.dp)
                .blur(80.dp)
                .background(Gold500.copy(0.08f), CircleShape)
        )

        // Particle field
        Canvas(Modifier.fillMaxSize()) {
            particles.forEach { p ->
                drawCircle(
                    color  = Blue300.copy(p.a),
                    radius = p.r.dp.toPx(),
                    center = Offset(p.x * size.width, p.y * size.height)
                )
            }
        }

        Column(
            modifier            = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo assembly
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(164.dp)) {
                // Pulse ring
                Box(
                    Modifier.size(130.dp).scale(pulseRing).alpha(pulseAlpha)
                        .clip(CircleShape).background(Blue500.copy(0.15f))
                )
                // Static halo
                Box(
                    Modifier.size(130.dp).scale(ringScale.value)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Blue600.copy(0.25f), Color.Transparent)))
                )
                // Orbiting dot
                Box(
                    Modifier.size(150.dp).alpha(ringScale.value)
                        .graphicsLayer(rotationZ = orbit)
                ) {
                    Box(
                        Modifier.size(7.dp).align(Alignment.TopCenter).offset(y = 10.dp)
                            .clip(CircleShape).background(Blue300)
                    )
                }
                // Logo circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .scale(logoScale.value).alpha(logoAlpha.value)
                        .size(108.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Blue500, Blue600)))
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp).clip(CircleShape).background(Navy800)
                    ) {
                        Text("P", fontSize = 46.sp, fontWeight = FontWeight.Black, color = Blue400)
                    }
                }
            }

            Spacer(Modifier.height(30.dp))

            // App name
            Text(
                "PUNDAR",
                fontWeight    = FontWeight.Black,
                fontSize      = 36.sp,
                letterSpacing = 7.sp,
                color         = TextWhite,
                modifier      = Modifier.alpha(textAlpha.value)
            )
            Spacer(Modifier.height(6.dp))
            // Underline accent
            Box(
                Modifier.alpha(textAlpha.value).width(110.dp).height(2.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, Blue400, Color.Transparent)))
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "Build Together. Grow Together.",
                style         = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color         = TextSoft,
                fontWeight    = FontWeight.Normal,
                textAlign     = TextAlign.Center,
                letterSpacing = 0.8.sp,
                modifier      = Modifier.alpha(taglineAlpha.value).padding(horizontal = 36.dp)
            )
            Spacer(Modifier.height(38.dp))

            // Stellar badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .alpha(badgeAlpha.value)
                    .clip(RoundedCornerShape(50.dp))
                    .background(Blue500.copy(0.10f))
                    .border(1.dp, Blue400.copy(0.25f), RoundedCornerShape(50.dp))
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon3DStar(size = 13.dp)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "Powered by Stellar Blockchain",
                        style      = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color      = Gold400,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }
    }
}
