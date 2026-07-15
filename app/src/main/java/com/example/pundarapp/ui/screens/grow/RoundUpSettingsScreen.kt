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
        containerColor = Navy900
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
                        .background(Navy800)
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Enable Round-Ups",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Auto-invest spare change from Pay",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = settings.isEnabled,
                        onCheckedChange = { AppState.toggleRoundUp(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ElectricBlue,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = SpaceMedium
                        )
                    )
                }
            }

            // Multiplier
            item {
                Text(
                    text = "Multiplier",
                    color = TextPrimary,
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
                                .background(if (isSelected) ElectricBlue else Navy800)
                                .border(
                                    1.dp,
                                    if (isSelected) ElectricBlue else NavyBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { AppState.setRoundUpMultiplier(multiplier) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${multiplier}x",
                                color = if (isSelected) Color.White else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "A ₱5.00 round-up at ${settings.roundUpMultiplier}x will invest ₱${5.0 * settings.roundUpMultiplier}.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Threshold
            item {
                Text(
                    text = "Auto-Invest Threshold",
                    color = TextPrimary,
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
                                .background(if (isSelected) ElectricBlue else Navy800)
                                .border(
                                    1.dp,
                                    if (isSelected) ElectricBlue else NavyBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { AppState.setRoundUpThreshold(threshold) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "₱${threshold.toInt()}",
                                color = if (isSelected) Color.White else TextPrimary,
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
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Your round-ups will be distributed across these stocks.",
                    color = TextSecondary,
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
                                .background(Navy800)
                                .border(1.dp, if (isSelected) ElectricBlue.copy(alpha = 0.5f) else NavyBorder, RoundedCornerShape(12.dp))
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
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = ticker,
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) ElectricBlue else Color.Transparent)
                                    .border(1.dp, if (isSelected) ElectricBlue else TextSecondary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
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
