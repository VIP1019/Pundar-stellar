package com.example.pundarapp.ui.screens.home

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.R
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.SampleData
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val user = SampleData.currentUser

    Scaffold(
        topBar = {
            PundarMainTopBar(
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
            // ── Welcome Banner ─────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(PundarBlue, PundarBlueDark)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = "Magandang gabi, ${user.name.split(" ").first()}! 👋",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "\"Every Transaction Counts.\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                HomeStatItem("Total Saved", "₱ 357,000", Color.White)
                                HomeStatItem("Invested", "₱ 124,500", Color.White)
                                HomeStatItem("Score", "${user.pundarScore}", PundarGold)
                            }
                        }
                    }
                }
            }

            // ── Quick Actions ──────────────────────────────────
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PundarTextPrimary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Filled.Receipt,
                        title = "Split a Bill",
                        subtitle = "PUNDAR Pay",
                        color = PundarBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Routes.PAY_NEW_BILL) }
                    )
                    QuickActionCard(
                        icon = Icons.Filled.Groups,
                        title = "Join Circle",
                        subtitle = "PUNDAR Circle",
                        color = PundarGoldDark,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Routes.CIRCLE) }
                    )
                    QuickActionCard(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        title = "Invest",
                        subtitle = "PUNDAR Grow",
                        color = PundarSuccess,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Routes.GROW) }
                    )
                }
            }

            // ── Financial Progress ─────────────────────────────
            item {
                Text(
                    text = "Your Financial Journey",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PundarTextPrimary
                )
            }

            item {
                PundarCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        JourneyStep("Spend", R.drawable.spend, PundarSuccess, true)
                        JourneyArrow()
                        JourneyStep("Save", R.drawable.save, PundarSuccess, true)
                        JourneyArrow()
                        JourneyStep("Grow", R.drawable.grow1, PundarBlue, true)
                        JourneyArrow()
                        JourneyStep("Score", R.drawable.score, PundarGold, true)
                        JourneyArrow()
                        JourneyStep("Access", R.drawable.access, PundarTextSecondary, false)
                    }
                }
            }

            // ── Recent Activity ────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PundarTextPrimary
                    )
                    TextButton(onClick = { }) {
                        Text(
                            text = "View All",
                            color = PundarBlue,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            items(SampleData.homeActivities) { activity ->
                PundarCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
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
                            Text(
                                text = activity.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = activity.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = PundarTextSecondary
                            )
                        }

                        Text(
                            text = activity.amount,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (activity.isPositive) PundarSuccess else PundarTextPrimary
                        )
                    }
                }
            }

            // ── Bottom Tagline ─────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Spend Together → Save Together → Grow Together",
                    style = MaterialTheme.typography.bodySmall,
                    color = PundarTextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun HomeStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PundarSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f))
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = PundarTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun JourneyStep(label: String, iconRes: Int, color: Color, isComplete: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isComplete) FontWeight.Bold else FontWeight.Normal,
            color = if (isComplete) color else PundarTextTertiary
        )
    }
}

@Composable
private fun JourneyArrow() {
    Text(
        text = "→",
        style = MaterialTheme.typography.bodySmall,
        color = PundarTextTertiary,
        modifier = Modifier.padding(top = 4.dp)
    )
}
