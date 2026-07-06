package com.example.pundarapp.ui.screens.grow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.SampleData
import com.example.pundarapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(ticker: String, navController: NavController) {
    val holding = SampleData.portfolio.holdings.find { it.ticker == ticker } ?: SampleData.portfolio.holdings.first()
    var selectedTimeRange by remember { mutableStateOf("1M") }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = holding.ticker,
                onBack = { navController.navigateUp() },
                showStar = true
            )
        },
        containerColor = PundarBackground,
        bottomBar = {
            Row(
                modifier = Modifier
                    .background(PundarSurface)
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PundarSecondaryButton(
                    text = "Sell",
                    onClick = { },
                    modifier = Modifier.weight(1f)
                )
                PundarPrimaryButton(
                    text = "Buy ${holding.ticker}",
                    onClick = { },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ───────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PundarSurface)
                    ) {
                        Text(holding.ticker.take(2), style = MaterialTheme.typography.titleMedium, color = PundarBlue)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = holding.companyName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = holding.sector,
                            style = MaterialTheme.typography.bodySmall,
                            color = PundarTextSecondary
                        )
                    }
                }
            }

            // ── Price ────────────────────────────────────────────
            item {
                Text(
                    text = "₱ ${String.format("%,.2f", holding.currentPrice)}",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = PundarTextPrimary
                )
                Spacer(Modifier.height(8.dp))
                StatusBadge(
                    text = "📈 +₱ ${String.format("%,.2f", holding.priceChange)} (+${holding.priceChangePercent}%) Today",
                    color = PundarBlue
                )
            }

            // ── Chart ────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PundarSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        listOf("1D", "1W", "1M", "1Y").forEach { range ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedTimeRange == range) PundarSurface else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTimeRange = range }
                            ) {
                                Text(
                                    text = range,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selectedTimeRange == range) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (selectedTimeRange == range) PundarBlue else PundarTextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                PundarLineChart(
                    dataPoints = holding.priceHistory,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── My Holding ───────────────────────────────────────
            item {
                PundarAccentCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(PundarBlue)
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "My Holding",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Value", style = MaterialTheme.typography.labelSmall, color = PundarTextSecondary)
                            Text(
                                text = "₱ ${String.format("%,.2f", holding.value)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Return", style = MaterialTheme.typography.labelSmall, color = PundarTextSecondary)
                            Text(
                                text = "📈 ₱ ${String.format("%,.2f", holding.value * holding.returnPercent / 100)} (+${holding.returnPercent}%)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PundarBlue
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Shares Owned", style = MaterialTheme.typography.labelSmall, color = PundarTextSecondary)
                            Text(
                                text = "${String.format("%,.2f", holding.shares.toDouble())}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PundarTextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Average Cost", style = MaterialTheme.typography.labelSmall, color = PundarTextSecondary)
                            Text(
                                text = "₱ ${String.format("%,.2f", holding.averageCost)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PundarTextPrimary
                            )
                        }
                    }
                }
            }

            // ── Key Stats ────────────────────────────────────────
            item {
                Text(
                    text = "Key Stats",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PundarTextPrimary
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(label = "Market Cap", value = holding.marketCap, modifier = Modifier.weight(1f))
                    StatCard(label = "P/E Ratio", value = holding.peRatio, modifier = Modifier.weight(1f))
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(label = "Div Yield", value = holding.divYield, modifier = Modifier.weight(1f))
                    StatCard(label = "52W High", value = "₱ ${String.format("%,.2f", holding.high52w)}", modifier = Modifier.weight(1f))
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(label = "52W Low", value = "₱ ${String.format("%,.2f", holding.low52w)}", modifier = Modifier.weight(1f))
                    StatCard(label = "Volume", value = holding.volume, modifier = Modifier.weight(1f))
                }
            }

            // ── About ────────────────────────────────────────────
            item {
                Text(
                    text = "About ${holding.companyName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PundarTextPrimary
                )
            }
            
            item {
                Text(
                    text = holding.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PundarTextSecondary,
                    lineHeight = 22.sp
                )
                
                Spacer(Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { }
                ) {
                    Text(
                        text = "Read more",
                        style = MaterialTheme.typography.labelLarge,
                        color = PundarBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = PundarBlue,
                        modifier = Modifier.size(12.dp)
                    )
                }
                
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = PundarSurfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = PundarTextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PundarTextPrimary
            )
        }
    }
}
