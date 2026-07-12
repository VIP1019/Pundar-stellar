package com.example.pundarapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pundarapp.ui.theme.*

enum class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val accent: Color
) {
    HOME("home", "Home", Icons.Filled.Home, ElectricBlue),
    PAY("pay", "Pay", Icons.Filled.Payment, PremiumGoldWarm),
    SCAN("scan", "Scan", Icons.Filled.QrCodeScanner, NeonCyan),
    CIRCLE("circle", "Circle", Icons.Filled.Groups, ElectricPurple),
    GROW("grow", "Grow", Icons.AutoMirrored.Filled.ShowChart, NeonGreen)
}

private val NavInactiveIcon = Color(0xFFB8C5D6)
private val NavInactiveLabel = Color(0xFFCBD5E1)

@Composable
fun PundarBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = 28.dp,
                    shape = RoundedCornerShape(36.dp),
                    ambientColor = ElectricBlue.copy(alpha = 0.35f),
                    spotColor = ElectricBlue.copy(alpha = 0.35f),
                    clip = false
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(36.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1A2844), Color(0xFF121C30))
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            ElectricBlue.copy(0.45f),
                            GlassWhite.copy(0.6f),
                            ElectricPurple.copy(0.3f)
                        )
                    ),
                    RoundedCornerShape(36.dp)
                )
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem.entries.forEach { item ->
                val selected = currentRoute.startsWith(item.route)
                NavBarItem(
                    item = item,
                    selected = selected,
                    onClick = { onNavigate(item.route) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavBarItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "navScale_${item.name}"
    )

    val infinite = rememberInfiniteTransition(label = "scanPulse")
    val scanPulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (item == BottomNavItem.SCAN) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            tween(if (item == BottomNavItem.SCAN) 1400 else 1),
            RepeatMode.Reverse
        ),
        label = "scanPulseScale"
    )

    val iconTint = if (selected) item.accent else NavInactiveIcon
    val labelColor = if (selected) item.accent else NavInactiveLabel

    Column(
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 8.dp)
            .graphicsLayer(scaleX = scale * scanPulse, scaleY = scale * scanPulse)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (item == BottomNavItem.SCAN) {
            FuturisticIcon(
                icon = item.icon,
                tint = NeonCyan,
                size = 50.dp,
                iconSize = 24.dp,
                shape = CircleShape,
                pulseGlow = true
            )
        } else {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (selected) item.accent else Color.Transparent)
            )
            Spacer(Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .size(if (selected) 42.dp else 38.dp)
                    .shadow(
                        elevation = if (selected) 10.dp else 4.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = iconTint.copy(if (selected) 0.45f else 0.15f),
                        spotColor = iconTint.copy(if (selected) 0.45f else 0.15f)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected)
                            Brush.linearGradient(
                                listOf(item.accent.copy(0.35f), item.accent.copy(0.12f))
                            )
                        else
                            Brush.linearGradient(
                                listOf(Color(0xFF243352), Color(0xFF1A2840))
                            )
                    )
                    .border(
                        1.dp,
                        if (selected) item.accent.copy(0.65f) else Color(0xFF3D4F6E),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = iconTint,
                    modifier = Modifier.size(if (selected) 22.dp else 20.dp)
                )
            }
        }

        if (item != BottomNavItem.SCAN) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = item.label,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = labelColor
            )
        }
    }
}
