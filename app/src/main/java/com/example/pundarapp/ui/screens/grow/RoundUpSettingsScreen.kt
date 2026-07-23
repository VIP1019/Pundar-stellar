package com.example.pundarapp.ui.screens.grow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme

@Composable
fun RoundUpSettingsScreen(navController: NavController) {
    val settings = AppState.roundUpSettings.value
    
    val allStocks = listOf("AC", "SMPH", "BDO", "JFC", "MER")

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Round-Up Settings",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarTheme.colors.bgPrimary
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Enable Toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PundarTheme.colors.surfacePrimary)
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Enable Round-Ups",
                            color = PundarTheme.colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Auto-invest spare change from Pay",
                            color = PundarTheme.colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = settings.isEnabled,
                        onCheckedChange = { AppState.toggleRoundUp(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PundarTheme.colors.surfacePrimary,
                            checkedTrackColor = ElectricBlue,
                            uncheckedThumbColor = PundarTheme.colors.surfacePrimary,
                            uncheckedTrackColor = PundarTheme.colors.surfaceTertiary
                        )
                    )
                }
            }

            // Multiplier
            item {
                Text(
                    text = "Multiplier",
                    color = PundarTheme.colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 3, 5, 10).forEach { multiplier ->
                        val isSelected = settings.roundUpMultiplier == multiplier
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) ElectricBlue else PundarTheme.colors.surfacePrimary)
                                .border(
                                    1.dp,
                                    if (isSelected) ElectricBlue else PundarTheme.colors.borderPrimary,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { AppState.setRoundUpMultiplier(multiplier) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${multiplier}x",
                                color = if (isSelected) PundarTheme.colors.surfacePrimary else PundarTheme.colors.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "A ₱5.00 round-up at ${settings.roundUpMultiplier}x will invest ₱${5.0 * settings.roundUpMultiplier}.",
                    color = PundarTheme.colors.textSecondary,
                    fontSize = 12.sp
                )
            }

            // Threshold
            item {
                Text(
                    text = "Auto-Invest Threshold",
                    color = PundarTheme.colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(50.0, 100.0, 200.0, 500.0).forEach { threshold ->
                        val isSelected = settings.threshold == threshold
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) ElectricBlue else PundarTheme.colors.surfacePrimary)
                                .border(
                                    1.dp,
                                    if (isSelected) ElectricBlue else PundarTheme.colors.borderPrimary,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { AppState.setRoundUpThreshold(threshold) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "₱${threshold.toInt()}",
                                color = if (isSelected) PundarTheme.colors.surfacePrimary else PundarTheme.colors.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Target Stocks
            item {
                Text(
                    text = "Target Stocks",
                    color = PundarTheme.colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Your round-ups will be distributed across these stocks.",
                    color = PundarTheme.colors.textSecondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    allStocks.forEach { ticker ->
                        val isSelected = ticker in settings.targetStocks
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(PundarTheme.colors.surfacePrimary)
                                .border(1.dp, if (isSelected) ElectricBlue.copy(alpha = 0.5f) else PundarTheme.colors.borderPrimary, RoundedCornerShape(12.dp))
                                .clickable {
                                    val newTargets = if (isSelected) {
                                        if (settings.targetStocks.size > 1) {
                                            settings.targetStocks - ticker
                                        } else {
                                            settings.targetStocks
                                        }
                                    } else {
                                        settings.targetStocks + ticker
                                    }
                                    AppState.updateTargetStocks(newTargets)
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(ElectricBlue, NeonGreen))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ticker.take(2),
                                        color = PundarTheme.colors.surfacePrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = ticker,
                                    color = PundarTheme.colors.textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) ElectricBlue else Color.Transparent)
                                    .border(1.dp, if (isSelected) ElectricBlue else PundarTheme.colors.textSecondary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = PundarTheme.colors.surfacePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
