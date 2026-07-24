package com.example.pundarapp.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.ui.components.Icon3DStar
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.theme.Gold400
import com.example.pundarapp.ui.theme.Green400
import com.example.pundarapp.ui.theme.PundarTheme
import com.example.pundarapp.ui.data.AppState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreScreen(navController: NavController) {
    val user = com.example.pundarapp.ui.data.SampleData.currentUser
    val score = AppState.pundarScore.intValue

    val (levelText, levelColor) = when {
        score >= 800 -> "Excellent" to Green400
        score >= 700 -> "Good" to Gold400
        score >= 600 -> "Fair" to Color(0xFFF59E0B)
        else -> "Needs Work" to PundarTheme.colors.accentRed
    }

    val factors = listOf(
        ScoreFactor("On-Time Settlements", "Pays group bills promptly.", Icons.Filled.CheckCircle, Green400),
        ScoreFactor("Circle Contributions", "Consistent Paluwagan saving.", Icons.Filled.Groups, PundarTheme.colors.accentGold),
        ScoreFactor("App Engagement", "Active usage of Wallet features.", Icons.Filled.TrendingUp, Blue400)
    )

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "PUNDAR Score",
                onBack = { navController.popBackStack() }
            )
        },
        containerColor = PundarTheme.colors.bgPrimary
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp)
        ) {
            item {
                ScoreGauge(score, levelText, levelColor)
            }

            item {
                Text(
                    "Score Factors",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PundarTheme.colors.textPrimary
                )
            }

            items(factors) { factor ->
                FactorRow(factor)
            }

            item {
                Spacer(Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PundarTheme.colors.surfaceSecondary),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.TipsAndUpdates, "Tip", tint = Gold400)
                            Spacer(Modifier.width(12.dp))
                            Text("How to improve your score?", fontWeight = FontWeight.SemiBold, color = PundarTheme.colors.textPrimary)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Keep paying your group bills on time and contribute regularly to your Circles. A higher score unlocks lower transaction fees and exclusive app features.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PundarTheme.colors.textSecondary,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}

val Blue400 = Color(0xFF60A5FA)

@Composable
fun ScoreGauge(score: Int, levelText: String, levelColor: Color) {
    val animatedScore = remember { Animatable(0f) }
    LaunchedEffect(score) {
        animatedScore.animateTo(
            targetValue = score.toFloat(),
            animationSpec = tween(1500, easing = FastOutSlowInEasing)
        )
    }

    val maxScore = 1000f
    val progress = animatedScore.value / maxScore
    val trackColor = PundarTheme.colors.glassMedium

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Draw the gauge arc
        Box(
            modifier = Modifier
                .size(240.dp)
                .drawBehind {
                    val startAngle = 135f
                    val sweepAngle = 270f
                    val strokeWidth = 16.dp.toPx()
                    
                    // Track
                    drawArc(
                        color = trackColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round),
                        size = Size(size.width, size.height)
                    )
                    
                    // Progress
                    drawArc(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF60A5FA), levelColor)
                        ),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle * progress,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round),
                        size = Size(size.width, size.height)
                    )
                }
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon3DStar(size = 48.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                "${animatedScore.value.toInt()}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = PundarTheme.colors.textPrimary
            )
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(levelColor.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    levelText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = levelColor
                )
            }
        }
    }
}

data class ScoreFactor(val title: String, val desc: String, val icon: ImageVector, val color: Color)

@Composable
fun FactorRow(factor: ScoreFactor) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PundarTheme.colors.surfacePrimary)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(factor.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(factor.icon, contentDescription = null, tint = factor.color)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(factor.title, fontWeight = FontWeight.SemiBold, color = PundarTheme.colors.textPrimary)
            Text(factor.desc, style = MaterialTheme.typography.bodySmall, color = PundarTheme.colors.textSecondary)
        }
    }
}
