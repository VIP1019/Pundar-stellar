package com.example.pundarapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pundarapp.R
import com.example.pundarapp.ui.theme.*

enum class BottomNavItem(
    val route: String,
    val label: String,
    val iconName: String
) {
    HOME("home",     "Home",   "home"),
    PAY("pay",       "Pay",    "pay"),
    SCAN("scan",     "Scan",   "scan"),
    CIRCLE("circle", "Circle", "circle"),
    GROW("grow",     "Grow",   "grow")
}

@Composable
fun PundarBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    // Floating pill outer shell
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Glow underneath
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation    = 24.dp,
                    shape        = RoundedCornerShape(36.dp),
                    ambientColor = ElectricBlue.copy(alpha = 0.20f),
                    spotColor    = ElectricBlue.copy(alpha = 0.20f),
                    clip         = false
                )
        )

        // Pill background
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(36.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0D1830), Color(0xFF0A1220))
                    )
                )
                // Glass border
                .then(
                    Modifier.padding(1.dp)
                )
                .clip(RoundedCornerShape(35.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            GlassBorder.copy(alpha = 0.8f),
                            Color.Transparent,
                            GlassWhite
                        )
                    )
                ),
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
            stiffness    = Spring.StiffnessMedium
        ),
        label = "navScale_${item.name}"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.55f,
        animationSpec = tween(200),
        label = "labelAlpha_${item.name}"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 10.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (item == BottomNavItem.SCAN) {
            // Special center Scan button — elevated pill
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .shadow(
                        elevation    = 12.dp,
                        shape        = CircleShape,
                        ambientColor = ElectricBlue.copy(0.5f),
                        spotColor    = ElectricBlue.copy(0.5f)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ElectricBlueDeep, ElectricBlue)
                        )
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = "Scan",
                    tint = TextOnDark,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            // Indicator dot above icon
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) ElectricBlue else Color.Transparent
                    )
            )
            Spacer(Modifier.height(4.dp))

            val iconRes = when (item) {
                BottomNavItem.HOME   -> R.drawable.home
                BottomNavItem.PAY    -> R.drawable.pay
                BottomNavItem.CIRCLE -> R.drawable.circle
                BottomNavItem.GROW   -> R.drawable.grow
                else -> R.drawable.home
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(if (selected) 40.dp else 36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected)
                            Brush.radialGradient(
                                colors = listOf(
                                    ElectricBlue.copy(0.22f),
                                    Color.Transparent
                                )
                            )
                        else
                            Brush.radialGradient(colors = listOf(Color.Transparent, Color.Transparent))
                    )
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = item.label,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (item != BottomNavItem.SCAN) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.label,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) ElectricBlue else TextTertiary,
                modifier = Modifier.graphicsLayer(alpha = labelAlpha)
            )
        }
    }
}
