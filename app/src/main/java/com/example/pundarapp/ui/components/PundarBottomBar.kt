package com.example.pundarapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme

// Enum only holds route/label/accent. Icons resolved separately to avoid
// AutoMirrored extension property limitation in enum default values.
enum class BottomNavItem(
    val route:  String,
    val label:  String,
    val accent: Color
) {
    HOME("home",   "Home",   Blue400),
    PAY("pay",     "Pay",    Gold500),
    SCAN("scan",   "Scan",   Blue300),
    CIRCLE("circle", "Circle", Color(0xFF8B5CF6)),
    PROFILE("settings", "Profile", Color(0xFF10B981))
}

// Resolve icons here — extension properties are fine at function level
@Composable
private fun iconFor(item: BottomNavItem, selected: Boolean): ImageVector = when (item) {
    BottomNavItem.HOME   -> if (selected) Icons.Filled.Home          else Icons.Outlined.Home
    BottomNavItem.PAY    -> if (selected) Icons.Filled.Receipt        else Icons.Outlined.Receipt
    BottomNavItem.SCAN   -> Icons.Filled.QrCodeScanner
    BottomNavItem.CIRCLE -> if (selected) Icons.Filled.Groups         else Icons.Outlined.Groups
    BottomNavItem.PROFILE -> if (selected) Icons.Filled.Person        else Icons.Outlined.Person
}

@Composable
fun PundarBottomBar(
    currentRoute: String,
    onNavigate:   (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE5EAF1), RoundedCornerShape(28.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            BottomNavItem.entries.forEach { item ->
                val selected = currentRoute.startsWith(item.route)
                NavItem(item = item, selected = selected, onClick = { onNavigate(item.route) })
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    item:     BottomNavItem,
    selected: Boolean,
    onClick:  () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue   = if (selected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "scale_${item.name}"
    )
    val iconTint by animateColorAsState(
        targetValue   = if (selected) item.accent else PundarTheme.colors.textMuted,
        animationSpec = tween(220),
        label         = "tint_${item.name}"
    )
    val labelColor by animateColorAsState(
        targetValue   = if (selected) item.accent else PundarTheme.colors.textDim,
        animationSpec = tween(220),
        label         = "label_${item.name}"
    )

    val icon = iconFor(item, selected)

    if (item == BottomNavItem.SCAN) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .shadow(5.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF10284B))
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = item.label,
                    tint               = Color.White,
                    modifier           = Modifier.size(22.dp)
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClick() }
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Active pill indicator
            Box(
                modifier = Modifier
                    .width(if (selected) 20.dp else 0.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (selected) item.accent else Color.Transparent)
            )
            Spacer(Modifier.height(4.dp))

            // Icon container
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected) item.accent.copy(alpha = 0.12f)
                        else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = item.label,
                    tint               = iconTint,
                    modifier           = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.height(3.dp))
            Text(
                text       = item.label,
                fontSize   = 10.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color      = labelColor
            )
        }
    }
}
