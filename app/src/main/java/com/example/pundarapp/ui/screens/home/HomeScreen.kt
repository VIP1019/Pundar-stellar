package com.example.pundarapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.SampleData
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val user = SampleData.currentUser
    val homeActivities = AppState.homeActivities
    val portfolio by AppState.portfolio
    val totalSaved = AppState.circles.sumOf { it.savedAmount }

    Scaffold(
        topBar = {
            PundarMainTopBar(userInitials = user.initials, pundarScore = user.pundarScore)
        },
        containerColor = PundarBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Welcome Banner ─────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(Brush.horizontalGradient(colors = listOf(PundarBlue, PundarBlueDark)))
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = "Magandang gabi, ${user.name.split(" ").first()}! 👋",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White, fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "\"Every Peso Keeps Building.\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                HomeStatItem("Total Saved", "₱ ${String.format("%,.0f", totalSaved)}", Color.White)
                                HomeStatItem("Invested", "₱ ${String.format("%,.0f", portfolio.totalValue)}", Color.White)
                                HomeStatItem("Score", "${user.pundarScore}", PundarGold)
                            }
                        }
                    }
                }
            }

            // ── Quick Actions ──────────────────────────────────
            item {
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = PundarTextPrimary)
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionCard("Split a Bill", "PUNDAR Pay", Icons.Filled.Receipt, PundarBlue,
                        Modifier.weight(1f)) { navController.navigate(Routes.PAY_NEW_BILL) }
                    QuickActionCard("Join Circle", "PUNDAR Circle", Icons.Filled.Groups, PundarGoldDark,
                        Modifier.weight(1f)) { navController.navigate(Routes.CIRCLE) }
                    QuickActionCard("Invest", "PUNDAR Grow", Icons.AutoMirrored.Filled.TrendingUp, PundarSuccess,
                        Modifier.weight(1f)) { navController.navigate(Routes.GROW) }
                }
            }

            // ── Financial Journey ──────────────────────────────
            item {
                Text("Your Financial Journey", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = PundarTextPrimary)
            }
            item {
                PundarCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        JourneyStep("Spend", "✅", PundarSuccess, true)
                        JourneyArrow()
                        JourneyStep("Save", "✅", PundarSuccess, true)
                        JourneyArrow()
                        JourneyStep("Grow", "🔄", PundarBlue, true)
                        JourneyArrow()
                        JourneyStep("Score", "⭐", PundarGold, true)
                        JourneyArrow()
                        JourneyStep("Access", "🔒", PundarTextSecondary, false)
                    }
                }
            }

            // ── Recent Activity — live from AppState ───────────
            item {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent Activity", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = PundarTextPrimary)
                    TextButton(onClick = { }) { Text("View All", color = PundarBlue) }
                }
            }

            items(homeActivities.take(5).toList()) { activity ->
                PundarCard {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(
                                when (activity.module) {
                                    "Pay" -> PundarBlueSubtle
                                    "Circle" -> PundarGoldLight
                                    "Grow" -> PundarSuccessLight
                                    else -> PundarSurfaceVariant
                                }
                            )
                        ) {
                            Icon(
                                imageVector = when (activity.module) {
                                    "Pay" -> Icons.Filled.Receipt
                                    "Circle" -> Icons.Filled.Groups
                                    "Grow" -> Icons.AutoMirrored.Filled.TrendingUp
                                    else -> Icons.Filled.Info
                                },
                                contentDescription = null,
                                tint = when (activity.module) {
                                    "Pay" -> PundarBlue
                                    "Circle" -> PundarGoldDark
                                    "Grow" -> PundarSuccess
                                    else -> PundarTextSecondary
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(activity.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(activity.subtitle, style = MaterialTheme.typography.bodySmall, color = PundarTextSecondary)
                        }
                        Text(activity.amount, style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (activity.isPositive) PundarSuccess else PundarTextPrimary)
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("Spend Together → Save Together → Grow Together",
                    style = MaterialTheme.typography.bodySmall, color = PundarTextTertiary,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun HomeStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
private fun QuickActionCard(
    title: String, subtitle: String, icon: ImageVector,
    color: Color, modifier: Modifier = Modifier, onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.height(120.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PundarSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.1f))) {
                Icon(icon, title, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = PundarTextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun JourneyStep(label: String, emoji: String, color: Color, isComplete: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isComplete) FontWeight.Bold else FontWeight.Normal,
            color = if (isComplete) color else PundarTextTertiary)
    }
}

@Composable
private fun JourneyArrow() {
    Text("→", style = MaterialTheme.typography.bodySmall, color = PundarTextTertiary,
        modifier = Modifier.padding(top = 4.dp))
}
