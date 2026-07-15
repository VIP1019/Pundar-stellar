package com.example.pundarapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.pundarapp.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────
//  AnimatedBackground
//
//  A zero-interaction Canvas layer that renders:
//    1. A deep radial base gradient (navy center → black edge)
//    2. Two large drifting radial orbs (primary accent color)
//    3. A third smaller accent orb (secondary color)
//    4. A slow-rotating mesh of faint diagonal lines
//    5. A particle field of 12 tiny drifting dots
//    6. A shimmer sweep that crosses the screen every ~4 s
//
//  Usage:
//    AnimatedBackground(accent = Blue500) {
//        Scaffold(...) { ... }
//    }
// ─────────────────────────────────────────────────────────────────

enum class BgAccent {
    Blue,    // Home
    Teal,    // Pay
    Gold,    // Grow
    Purple,  // Circle
}

private val SineIO = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

@Composable
fun AnimatedBackground(
    accent:  BgAccent = BgAccent.Blue,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // ── Accent color palette ──────────────────────────────────────
    val (orb1Color, orb2Color, accentLineColor) = when (accent) {
        BgAccent.Blue   -> Triple(Blue500,         Blue300,        Blue400)
        BgAccent.Teal   -> Triple(Electric400,     Blue300,        Electric400)
        BgAccent.Gold   -> Triple(Gold500,          Gold300,        Gold400)
        BgAccent.Purple -> Triple(ElectricPurple,  Color(0xFFA78BFA), ElectricPurple)
    }

    val inf = rememberInfiniteTransition(label = "bg_$accent")

    // Orb 1 — drifts diagonally
    val orb1X by inf.animateFloat(0.15f, 0.55f,
        infiniteRepeatable(tween(7000, easing = SineIO), RepeatMode.Reverse), label = "o1x")
    val orb1Y by inf.animateFloat(0.05f, 0.35f,
        infiniteRepeatable(tween(9000, easing = SineIO), RepeatMode.Reverse), label = "o1y")

    // Orb 2 — drifts opposite corner
    val orb2X by inf.animateFloat(0.6f, 0.95f,
        infiniteRepeatable(tween(8500, easing = SineIO), RepeatMode.Reverse), label = "o2x")
    val orb2Y by inf.animateFloat(0.5f, 0.85f,
        infiniteRepeatable(tween(6500, easing = SineIO), RepeatMode.Reverse), label = "o2y")

    // Orb 3 — small accent, centre-bottom
    val orb3X by inf.animateFloat(0.3f, 0.7f,
        infiniteRepeatable(tween(11000, easing = SineIO), RepeatMode.Reverse), label = "o3x")
    val orb3Y by inf.animateFloat(0.6f, 0.95f,
        infiniteRepeatable(tween(8000, easing = SineIO), RepeatMode.Reverse), label = "o3y")

    // Mesh rotation — one full cycle every 30 s
    val meshAngle by inf.animateFloat(0f, 360f,
        infiniteRepeatable(tween(30000, easing = LinearEasing)), label = "mesh")

    // Shimmer sweep — diagonal, 0→130% width
    val shimmer by inf.animateFloat(-0.3f, 1.3f,
        infiniteRepeatable(tween(4500, easing = LinearEasing)), label = "shim")

    // Particle angles — each dot orbits slightly
    val particleAngle by inf.animateFloat(0f, 360f,
        infiniteRepeatable(tween(20000, easing = LinearEasing)), label = "pa")

    // Orb pulse
    val orbPulse by inf.animateFloat(0.7f, 1f,
        infiniteRepeatable(tween(3000, easing = SineIO), RepeatMode.Reverse), label = "op")

    Box(modifier = modifier.fillMaxSize()) {
        // ── Background canvas ─────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Base radial gradient — navy center
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0D1B3E), Color(0xFF060B18)),
                    center = Offset(w * 0.5f, h * 0.3f),
                    radius = h * 0.9f
                )
            )

            // 2. Orb 1 — large primary orb (top area)
            drawRadialOrb(
                center = Offset(w * orb1X, h * orb1Y),
                radius = w * 0.65f,
                color  = orb1Color,
                alpha  = 0.07f * orbPulse
            )

            // 3. Orb 2 — large secondary orb (bottom area)
            drawRadialOrb(
                center = Offset(w * orb2X, h * orb2Y),
                radius = w * 0.55f,
                color  = orb2Color,
                alpha  = 0.055f * orbPulse
            )

            // 4. Orb 3 — small accent orb
            drawRadialOrb(
                center = Offset(w * orb3X, h * orb3Y),
                radius = w * 0.35f,
                color  = orb2Color,
                alpha  = 0.045f
            )

            // 5. Rotating mesh lines
            drawMesh(
                w           = w,
                h           = h,
                angle       = meshAngle,
                lineColor   = accentLineColor.copy(alpha = 0.028f)
            )

            // 6. Particle field
            drawParticles(
                w            = w,
                h            = h,
                baseAngle    = particleAngle,
                dotColor     = orb1Color.copy(alpha = 0.35f)
            )

            // 7. Shimmer diagonal sweep
            val sx = w * shimmer
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.018f),
                        Color.Transparent
                    ),
                    start = Offset(sx, 0f),
                    end   = Offset(sx + w * 0.25f, h)
                )
            )
        }

        // ── Screen content on top ─────────────────────────────────
        content()
    }
}

// ── Draw a soft radial glow orb ───────────────────────────────────
private fun DrawScope.drawRadialOrb(
    center: Offset,
    radius: Float,
    color:  Color,
    alpha:  Float
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

// ── Draw a slow-rotating diagonal line mesh ───────────────────────
private fun DrawScope.drawMesh(
    w:         Float,
    h:         Float,
    angle:     Float,
    lineColor: Color
) {
    val diagonal = kotlin.math.sqrt(w * w + h * h)
    val spacing  = 48f          // px between lines
    val count    = (diagonal / spacing).toInt() + 4
    val cx       = w / 2f
    val cy       = h / 2f

    rotate(degrees = angle, pivot = Offset(cx, cy)) {
        val rad = Math.toRadians(45.0)
        val dx  = cos(rad).toFloat()
        val dy  = sin(rad).toFloat()
        val nx  = -dy          // perpendicular
        val ny  =  dx

        val origin = Offset(cx - nx * spacing * count / 2, cy - ny * spacing * count / 2)

        repeat(count) { i ->
            val px = origin.x + nx * spacing * i
            val py = origin.y + ny * spacing * i
            val len = diagonal
            drawLine(
                color       = lineColor,
                start       = Offset(px - dx * len, py - dy * len),
                end         = Offset(px + dx * len, py + dy * len),
                strokeWidth = 0.8f
            )
        }
    }
}

// ── Draw a subtle particle field ──────────────────────────────────
private val PARTICLE_SEEDS = listOf(
    Pair(0.12f, 0.08f), Pair(0.82f, 0.12f), Pair(0.45f, 0.22f),
    Pair(0.25f, 0.45f), Pair(0.70f, 0.38f), Pair(0.05f, 0.65f),
    Pair(0.90f, 0.55f), Pair(0.55f, 0.72f), Pair(0.35f, 0.85f),
    Pair(0.78f, 0.80f), Pair(0.18f, 0.92f), Pair(0.60f, 0.50f)
)
private val PARTICLE_RADII   = listOf(1.8f, 1.2f, 2.2f, 1.0f, 1.6f, 2.0f, 1.4f, 1.8f, 1.2f, 2.4f, 1.0f, 1.6f)
private val PARTICLE_ORBITS  = listOf(12f, 18f, 8f, 20f, 14f, 10f, 22f, 16f, 9f, 11f, 19f, 15f)
private val PARTICLE_PHASES  = listOf(0f, 60f, 120f, 180f, 240f, 300f, 30f, 90f, 150f, 210f, 270f, 330f)

private fun DrawScope.drawParticles(
    w:         Float,
    h:         Float,
    baseAngle: Float,
    dotColor:  Color
) {
    PARTICLE_SEEDS.forEachIndexed { i, (fx, fy) ->
        val orbitR = PARTICLE_ORBITS[i]
        val phase  = PARTICLE_PHASES[i]
        val rad    = Math.toRadians((baseAngle + phase).toDouble())
        val cx     = w * fx + orbitR * cos(rad).toFloat()
        val cy     = h * fy + orbitR * sin(rad).toFloat()
        drawCircle(
            color  = dotColor,
            radius = PARTICLE_RADII[i],
            center = Offset(cx, cy)
        )
    }
}
