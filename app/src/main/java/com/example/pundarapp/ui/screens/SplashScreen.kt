package com.example.pundarapp.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pundarapp.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    // Logo scale animation
    val logoScale = remember { Animatable(0f) }
    // Text fade-in
    val textAlpha = remember { Animatable(0f) }
    // Tagline slide
    val taglineAlpha = remember { Animatable(0f) }
    // Overall fade out
    val screenAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Step 1: Logo bounces in
        logoScale.animateTo(
            targetValue = 1.1f,
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        )
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(200, easing = FastOutSlowInEasing)
        )

        // Step 2: Title fades in
        textAlpha.animateTo(1f, animationSpec = tween(400))

        // Step 3: Tagline slides in
        delay(200)
        taglineAlpha.animateTo(1f, animationSpec = tween(400))

        // Step 4: Hold for a beat
        delay(800)

        // Step 5: Fade everything out
        screenAlpha.animateTo(0f, animationSpec = tween(400))

        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha.value)
            .background(
                Brush.verticalGradient(
                    colors = listOf(PundarBlue, PundarBlueDark, Color(0xFF001F5C))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo circle with "P" letter
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(logoScale.value)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PundarGold, PundarGoldDark)
                        )
                    )
            ) {
                // Inner ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = "P",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PundarTextPrimary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // App Name
            Text(
                text = "PUNDAR",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 6.sp,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Spend Together · Save Together · Grow Together",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(taglineAlpha.value)
                    .padding(horizontal = 32.dp)
            )

            Spacer(Modifier.height(48.dp))

            // Powered by Stellar badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .alpha(taglineAlpha.value)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "⭐ Powered by Stellar",
                    style = MaterialTheme.typography.labelMedium,
                    color = PundarGold,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
