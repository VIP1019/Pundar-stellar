package com.example.pundarapp.ui.screens.circle

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.Circle
import com.example.pundarapp.ui.data.SampleData
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.data.remote.AuthRepository

private val EaseOutBack   = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
private val EaseOutExpo   = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleScreen(navController: NavController) {
    val currentName = AuthRepository.getCurrentUserName()
    val initials    = AuthRepository.getCurrentUserInitials()
    val userSession by AuthRepository.currentUserState
    val invitation  = AppState.pendingInvitation.value

    LaunchedEffect(Unit) {
        AppState.refreshCircles()
    }

    Scaffold(
        topBar = {
            PundarMainTopBar(
                userName            = currentName,
                userInitials        = initials,
                profileImageUrl     = userSession?.profileImageUrl,
                pundarScore         = SampleData.currentUser.pundarScore,
                onNotificationClick = { navController.navigate(Routes.NOTIFICATIONS) },
                onSettingsClick     = { navController.navigate(Routes.SETTINGS) }
            )
        },
        floatingActionButton = {
            CircleFab { navController.navigate(Routes.CIRCLE_CREATE) }
        },
        containerColor = SpaceNavy
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { CircleHeroHeader() }

            if (invitation != null) {
                item {
                    InvitationBanner(circleName = invitation.circleName) {
                        navController.navigate(Routes.circleInvite(invitation.id))
                    }
                }
            }

            item {
                Text(
                    "Active Paluwagans",
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 15.sp,
                    color         = TextPrimary,
                    letterSpacing = 0.2.sp
                )
            }

            if (AppState.circles.isEmpty()) {
                item { CircleEmptyState() }
            } else {
                itemsIndexed(AppState.circles.toList()) { idx, circle ->
                    CircleCard(circle = circle, index = idx) {
                        navController.navigate(Routes.circleDetail(circle.id))
                    }
                }
            }
        }
    }
}


// ── Glowing green FAB ─────────────────────────────────────────────
@Composable
private fun CircleFab(onClick: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "cfab").animateFloat(
        initialValue  = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "cfabPulse"
    )
    Box(
        Modifier.shadow(
            elevation    = (14 * pulse).dp,
            shape        = RoundedCornerShape(18.dp),
            ambientColor = NeonGreen.copy(alpha = pulse * 0.45f),
            spotColor    = NeonGreen.copy(alpha = pulse * 0.45f)
        )
    ) {
        ExtendedFloatingActionButton(
            onClick        = onClick,
            containerColor = Color.Transparent,
            contentColor   = SpaceBlack,
            shape          = RoundedCornerShape(18.dp),
            modifier       = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(NeonGreenDim, NeonGreen)))
        ) {
            Icon(Icons.Filled.Add, contentDescription = "New Paluwagan")
            Spacer(Modifier.width(8.dp))
            Text("Create Paluwagan", fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ── Hero Header ───────────────────────────────────────────────────
@Composable
private fun CircleHeroHeader() {
    val alpha  = remember { Animatable(0f) }
    val slideY = remember { Animatable(-24f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f,  tween(500, easing = EaseOutExpo))
        slideY.animateTo(0f, tween(500, easing = EaseOutExpo))
    }
    Column(Modifier.graphicsLayer(alpha = alpha.value, translationY = slideY.value)) {
        Text(
            "PUNDAR Paluwagan",
            fontWeight    = FontWeight.Black,
            fontSize      = 32.sp,
            color         = TextOnDark,
            letterSpacing = (-0.5).sp
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Shield, null, tint = PremiumGoldWarm, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Probably Safe Paluwagan · Soroban Escrow",
                style         = MaterialTheme.typography.bodyMedium,
                color         = TextSecondary,
                letterSpacing = 0.3.sp
            )
        }
    }
}

// ── Invitation Banner (pulsing gold glow) ─────────────────────────
@Composable
private fun InvitationBanner(circleName: String, onClick: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "invPulse").animateFloat(
        initialValue  = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "invP"
    )
    Box(
        Modifier.fillMaxWidth()
            .shadow(
                elevation    = (12 * pulse).dp,
                shape        = RoundedCornerShape(18.dp),
                ambientColor = PremiumGoldWarm.copy(pulse * 0.45f),
                spotColor    = PremiumGoldWarm.copy(pulse * 0.45f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(CardGoldStart, CardGoldEnd)))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(PremiumGoldWarm.copy(0.7f), GlassWhite)),
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(CircleShape)
                    .background(PremiumGoldWarm.copy(0.18f))
                    .border(1.dp, PremiumGoldWarm.copy(0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon3DBell(size = 42.dp, pulse = true) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Pending Invitation", fontWeight = FontWeight.Bold,
                    color = PremiumGoldWarm, fontSize = 14.sp)
                Text(circleName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = PremiumGoldWarm, modifier = Modifier.size(20.dp))
        }
    }
}


// ── Holographic Circle Card with 3D entrance ──────────────────────
@Composable
private fun CircleCard(circle: Circle, index: Int, onClick: () -> Unit) {
    val progress     = (circle.savedAmount / circle.targetAmount).toFloat().coerceIn(0f, 1f)
    val accentColor  = if (circle.isActive) ElectricBlue else NeonGreen

    // 3D entrance: alternate rotationY direction per index
    val rotY   = remember { Animatable(if (index % 2 == 0) -22f else 22f) }
    val alpha  = remember { Animatable(0f) }
    val slideY = remember { Animatable(28f) }
    LaunchedEffect(Unit) {
        val delay = index * 100
        alpha.animateTo(1f,  tween(400, delayMillis = delay))
        slideY.animateTo(0f, tween(460, delayMillis = delay, easing = EaseOutExpo))
        rotY.animateTo(0f,   tween(520, delayMillis = delay, easing = EaseOutBack))
    }

    // Animated progress fill (delayed so card is visible first)
    val animProg by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(900, delayMillis = index * 100 + 350, easing = EaseOutExpo),
        label         = "circleProgress_$index"
    )

    // Continuous shimmer sweep
    val sweepX by rememberInfiniteTransition(label = "csh_$index").animateFloat(
        initialValue  = -700f, targetValue = 1400f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing)),
        label         = "csw_$index"
    )

    Box(
        Modifier.fillMaxWidth()
            .graphicsLayer(
                alpha          = alpha.value,
                translationY   = slideY.value,
                rotationY      = rotY.value,
                cameraDistance = 14f * 8
            )
            .shadow(
                elevation    = 18.dp,
                shape        = RoundedCornerShape(24.dp),
                ambientColor = accentColor.copy(0.22f),
                spotColor    = accentColor.copy(0.22f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF0A1E3D), Color(0xFF0D2252), Color(0xFF06121E)))
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(accentColor.copy(0.50f), GlassWhite)),
                RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // Shimmer sweep overlay
        Box(
            Modifier.matchParentSize().background(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(0.045f), Color.Transparent),
                    start = Offset(sweepX, 0f), end = Offset(sweepX + 260f, 320f)
                )
            )
        )
        // Top neon edge glow
        Box(
            Modifier.fillMaxWidth().height(1.dp)
                .background(
                    Brush.horizontalGradient(listOf(Color.Transparent, accentColor.copy(0.7f), Color.Transparent))
                )
        )

        Column(Modifier.padding(20.dp)) {
            // ── Header row: status + member count | target ────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(0.15f))
                            .border(1.dp, accentColor.copy(0.42f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            if (circle.isActive) "ACTIVE" else "COMPLETED",
                            fontSize      = 10.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color         = accentColor
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Filled.Group, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("${circle.memberCount} / ${circle.maxMembers}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    if (circle.isFull) {
                        Spacer(Modifier.width(6.dp))
                        Text("Full", fontSize = 9.sp, color = ErrorRed, fontWeight = FontWeight.Bold)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TARGET", fontSize = 9.sp, letterSpacing = 1.2.sp, color = TextTertiary)
                    Text(
                        "₱${String.format("%,.0f", circle.targetAmount)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 16.sp,
                        color      = accentColor
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Circle name ───────────────────────────────────────
            Text(circle.name, fontWeight = FontWeight.Black, fontSize = 20.sp, color = TextOnDark)

            Spacer(Modifier.height(16.dp))

            // ── Animated progress track ───────────────────────────
            Box(
                Modifier.fillMaxWidth().height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SpaceMedium)
            ) {
                Box(
                    Modifier.fillMaxHeight()
                        .fillMaxWidth(animProg)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(listOf(accentColor, accentColor.copy(0.55f)))
                        )
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${(animProg * 100).toInt()}% Completed",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "₱${String.format("%,.0f", circle.targetAmount - circle.savedAmount)} left",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Total saved pill ──────────────────────────────────
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(0.10f))
                    .border(1.dp, accentColor.copy(0.28f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Total Saved", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(
                        "₱${String.format("%,.0f", circle.savedAmount)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 16.sp,
                        color      = accentColor
                    )
                }
            }
        }
    }
}

// ── Empty state card ──────────────────────────────────────────────
@Composable
private fun CircleEmptyState() {
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(SpaceDeep, Color(0xFF0E1825))))
            .border(1.dp, SpaceBorder, RoundedCornerShape(20.dp))
            .padding(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🌀", fontSize = 36.sp)
            Spacer(Modifier.height(10.dp))
            Text("No Paluwagans yet.", fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap \"Create Paluwagan\" to start your savings group.",
                style     = MaterialTheme.typography.bodySmall,
                color     = TextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}
