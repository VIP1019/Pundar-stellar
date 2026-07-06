package com.example.pundarapp.ui.screens.grow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.pundarapp.ui.data.ActivityType
import com.example.pundarapp.ui.data.SampleData
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowScreen(navController: NavController) {
    val user = SampleData.currentUser
    val portfolio = SampleData.portfolio

    Scaffold(
        topBar = {
            PundarGrowTopBar(
                userInitials = user.initials,
                pundarScore = user.pundarScore
            )
        },
        containerColor = PundarBackground
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Portfolio",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = PundarBlueDark
                    )
                    
                    PundarSmallButton(
                        text = "Optimize",
                        onClick = { },
                        containerColor = PundarGold,
                        contentColor = PundarTextPrimary
                    )
                }
            }

            // ── Main Chart Card ──────────────────────────────────
            item {
                PundarAccentCard {
                    Text(
                        text = "Total Assets Value",
                        style = MaterialTheme.typography.labelMedium,
                        color = PundarTextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "₱ ${String.format("%,.2f", portfolio.totalValue)}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PundarTextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    StatusBadge(
                        text = "📈 +${portfolio.totalReturnPercent}% (₱ ${String.format("%,.0f", portfolio.totalReturnAmount)}) All time",
                        color = PundarGoldDark
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PundarBlueButton(
                            text = "Invest More",
                            onClick = { },
                            modifier = Modifier.weight(1f).height(40.dp)
                        )
                        PundarSecondaryButton(
                            text = "Withdraw",
                            onClick = { },
                            modifier = Modifier.weight(1f).height(40.dp)
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    
                    PundarLineChart(
                        dataPoints = SampleData.portfolioChartData,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Allocation ───────────────────────────────────────
            item {
                Text(
                    text = "Allocation",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PundarTextPrimary
                )
            }
            
            item {
                PundarCard {
                    PundarAllocationBar("PH Equities", portfolio.phEquitiesPercent, PundarBlue)
                    Spacer(Modifier.height(16.dp))
                    PundarAllocationBar("US Equities", portfolio.usEquitiesPercent, PundarGold)
                    Spacer(Modifier.height(16.dp))
                    PundarAllocationBar("Fixed Income", portfolio.fixedIncomePercent, PundarTextSecondary)
                }
            }

            // ── Activity ─────────────────────────────────────────
            item {
                Text(
                    text = "Activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PundarTextPrimary
                )
            }
            
            item {
                PundarCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        portfolio.activities.take(2).forEachIndexed { index, activity ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when(activity.type) {
                                                ActivityType.AUTO_SWEEP -> PundarBlueSubtle
                                                ActivityType.DIVIDEND -> PundarGoldLight
                                                else -> PundarSurfaceVariant
                                            }
                                        )
                                ) {
                                    Icon(
                                        imageVector = when(activity.type) {
                                            ActivityType.AUTO_SWEEP -> Icons.Filled.Autorenew
                                            ActivityType.DIVIDEND -> Icons.Filled.LocalAtm
                                            else -> Icons.Filled.Tune
                                        },
                                        contentDescription = null,
                                        tint = when(activity.type) {
                                            ActivityType.AUTO_SWEEP -> PundarBlue
                                            ActivityType.DIVIDEND -> PundarGoldDark
                                            else -> PundarTextSecondary
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                
                                Spacer(Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activity.description,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = activity.date,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PundarTextSecondary
                                    )
                                }
                                
                                Text(
                                    text = "${if (activity.isPositive) "+" else "-"}₱ ${String.format("%,.2f", activity.amount)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activity.type == ActivityType.DIVIDEND) PundarGoldDark else PundarBlue
                                )
                            }
                            
                            if (index == 0) {
                                HorizontalDivider(color = PundarDivider)
                            }
                        }
                    }
                }
            }

            // ── Holdings ─────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Holdings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PundarTextPrimary
                    )
                    TextButton(onClick = { }) {
                        Text("View All", color = PundarBlue)
                    }
                }
            }
            
            item {
                PundarCard(modifier = Modifier.padding(bottom = 0.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Asset", style = MaterialTheme.typography.labelSmall, color = PundarTextSecondary, modifier = Modifier.weight(1.5f))
                        Text("Shares", style = MaterialTheme.typography.labelSmall, color = PundarTextSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text("Value", style = MaterialTheme.typography.labelSmall, color = PundarTextSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text("Return", style = MaterialTheme.typography.labelSmall, color = PundarTextSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                    
                    HorizontalDivider(color = PundarDivider)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        portfolio.holdings.forEachIndexed { index, holding ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navController.navigate(Routes.stockDetail(holding.ticker)) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.5f)) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(PundarSurfaceVariant) // Optional: Add image loading here
                                    ) {
                                        Text(holding.ticker.take(2), style = MaterialTheme.typography.labelMedium, color = PundarBlue)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(holding.companyName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text(holding.ticker, style = MaterialTheme.typography.labelSmall, color = PundarTextSecondary)
                                    }
                                }
                                
                                Text(
                                    text = holding.shares.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                                
                                Text(
                                    text = "₱ ${String.format("%,.0f", holding.value)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                                
                                Text(
                                    text = "${if (holding.returnPercent >= 0) "+" else ""}${holding.returnPercent}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (holding.returnPercent >= 0) PundarSuccess else PundarError,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            }
                            
                            if (index < portfolio.holdings.size - 1) {
                                HorizontalDivider(color = PundarDivider)
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}
