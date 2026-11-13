package com.example.pundarapp.ui.screens.circle

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.data.Circle
import com.example.pundarapp.ui.data.CircleInvitation
import com.example.pundarapp.ui.data.ContributionStatus
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme
import kotlinx.coroutines.launch

private val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleScreen(navController: NavController) {
    val initials    = AuthRepository.getCurrentUserInitials().ifBlank { "PJ" }
    val invitations = AppState.pendingInvitations
    val singleInvite = AppState.pendingInvitation.value
    val allInvites  = remember(invitations.size, singleInvite) {
        if (invitations.isNotEmpty()) invitations.toList()
        else if (singleInvite != null) listOf(singleInvite)
        else emptyList()
    }
    val totalSaved  = AppState.circles.sumOf { it.savedAmount }
    val activeCount = AppState.circles.count { it.lifecycleStatus == "ACTIVE" }
    val pendingCount = AppState.circles.count {
        it.lifecycleStatus in listOf("AWAITING_ACCEPTANCE", "DECLINED_REVISION_NEEDED", "READY_TO_ACTIVATE")
    }

    LaunchedEffect(Unit) {
        AppState.refreshCircles()
        AppState.refreshDiscoverableCircles()
    }

    Scaffold(
        floatingActionButton = {
            CircleFab { navController.navigate(Routes.CIRCLE_CREATE) }
        },
        containerColor = PundarTheme.colors.bgPrimary
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── DEEP NAVY HERO HEADER + FLOATING ACTION CARD ─────────
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                            .background(Color(0xFF0B1B3D))
                            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 44.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E2E5C))
                                        .border(1.5.dp, Color(0xFF3B82F6), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        initials,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "PUNDAR Circle",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        "Deterministic Community Savings · Soroban Testnet",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E2E5C))
                                        .clickable { navController.navigate(Routes.NOTIFICATIONS) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Notifications,
                                        contentDescription = "Notifications",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E2E5C))
                                        .clickable { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Settings,
                                        contentDescription = "Settings",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(22.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(Color(0xFF1E2E5C))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Total Round Pool Volume",
                                        fontSize = 11.sp,
                                        color = Color(0xFFCBD5E1),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = AppState.formatFiat(totalSaved),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "$activeCount Active · $pendingCount Pending Consent · Soroban Enforced",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF14254E))
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircleHeroStat(AppState.circles.size.toString(), "My Circles", Color(0xFF3B82F6))
                                Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.12f)))
                                CircleHeroStat(activeCount.toString(), "Active", Color(0xFF10B981))
                                Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.12f)))
                                CircleHeroStat(allInvites.size.toString(), "Invitations", Color(0xFFF59E0B))
                            }
                        }
                    }

                    // Floating White Action Card overlapping bottom of Navy Header
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .align(Alignment.BottomCenter)
                            .offset(y = 24.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = PundarTheme.colors.surfacePrimary,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, PundarTheme.colors.borderPrimary)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircleFloatingAction(
                                icon = Icons.Filled.AddCircle,
                                label = "Create Circle",
                                tint = Color(0xFF10B981),
                                onClick = { navController.navigate(Routes.CIRCLE_CREATE) }
                            )
                            CircleFloatingAction(
                                icon = Icons.Filled.MailOutline,
                                label = "Invites (${allInvites.size})",
                                tint = Color(0xFF2563EB),
                                onClick = {
                                    val first = allInvites.firstOrNull()
                                    if (first != null) {
                                        AppState.pendingInvitation.value = first
                                        navController.navigate(Routes.circleInvite(first.id))
                                    } else {
                                        navController.navigate(Routes.REQUESTS)
                                    }
                                }
                            )
                            CircleFloatingAction(
                                icon = Icons.Filled.Star,
                                label = "Score",
                                tint = Color(0xFFF59E0B),
                                onClick = { navController.navigate(Routes.SCORE) }
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // ── PENDING CIRCLE INVITATIONS ──────────────────────────
            if (allInvites.isNotEmpty()) {
                item {
                    Text(
                        "Pending Circle Invitations (${allInvites.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = PundarTheme.colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                itemsIndexed(allInvites) { _, inv ->
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        InvitationBanner(invitation = inv) {
                            AppState.pendingInvitation.value = inv
                            navController.navigate(Routes.circleInvite(inv.id))
                        }
                    }
                }
            }

            // ── MY CIRCLES DASHBOARD LIST ───────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "My Savings Circles",
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 17.sp,
                        color         = PundarTheme.colors.textPrimary
                    )
                    TextButton(onClick = { navController.navigate(Routes.CIRCLE_CREATE) }) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Create Circle", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (AppState.circles.isEmpty()) {
                item {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        CircleEmptyState(onCreateClick = { navController.navigate(Routes.CIRCLE_CREATE) })
                    }
                }
            } else {
                itemsIndexed(AppState.circles.toList()) { idx, circle ->
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        CircleCard(circle = circle, index = idx) {
                            navController.navigate(Routes.circleDetail(circle.id))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CircleHeroStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8))
    }
}

@Composable
private fun CircleFloatingAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = PundarTheme.colors.textPrimary
        )
    }
}

// ── Clean Navy FAB ────────────────────────────────────────────────
@Composable
private fun CircleFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick        = onClick,
        containerColor = Color(0xFF0B1B3D),
        contentColor   = Color.White,
        shape          = RoundedCornerShape(18.dp)
    ) {
        Icon(Icons.Filled.AddCircleOutline, contentDescription = "Create Circle", tint = Color.White)
        Spacer(Modifier.width(8.dp))
        Text(
            "Create Circle",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}

// ── Invitation Banner ─────────────────────────────────────────────
@Composable
private fun InvitationBanner(invitation: CircleInvitation, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = PundarTheme.colors.surfacePrimary,
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(0.55f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF59E0B).copy(0.14f)),
                contentAlignment = Alignment.Center
            ) { Icon3DBell(size = 32.dp, pulse = true) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Circle Invitation · Terms v${invitation.termsVersion}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706),
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    invitation.circleName,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = PundarTheme.colors.textPrimary
                )
                Text(
                    "Organizer: ${invitation.inviterName} · ${String.format("%,.0f", invitation.monthlyContribution)} ${invitation.currency}/round · ${invitation.scheduleText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PundarTheme.colors.textSecondary
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF59E0B).copy(alpha = 0.16f)
            ) {
                Text(
                    "Review Terms",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB45309),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ── Clean White Circle Card ───────────────────────────────────────
@Composable
private fun CircleCard(circle: Circle, index: Int, onClick: () -> Unit) {
    val totalMembers = circle.members.size.coerceAtLeast(1)
    val paidCount = circle.members.count { it.status == ContributionStatus.PAID }
    val acceptedCount = circle.members.count {
        it.acceptanceStatus == "ACCEPTED" && it.acceptedTermsVersion == circle.termsVersion
    }

    val (statusLabel, accentColor) = when (circle.lifecycleStatus) {
        "AWAITING_ACCEPTANCE" -> "AWAITING ACCEPTANCE · v${circle.termsVersion}" to Color(0xFFD97706)
        "DECLINED_REVISION_NEEDED" -> "DECLINED · REVISION NEEDED" to Color(0xFFDC2626)
        "READY_TO_ACTIVATE" -> "READY TO ACTIVATE · v${circle.termsVersion}" to Color(0xFF059669)
        "ACTIVE" -> "ACTIVE · ROUND ${circle.currentRound}/$totalMembers" to Color(0xFF2563EB)
        "COMPLETED" -> "COMPLETED · ALL ROUNDS PAID" to Color(0xFF10B981)
        else -> (if (circle.isActive) "ACTIVE" else "COMPLETED") to Color(0xFF2563EB)
    }

    val progress = when (circle.lifecycleStatus) {
        "AWAITING_ACCEPTANCE", "DECLINED_REVISION_NEEDED", "READY_TO_ACTIVATE" ->
            (acceptedCount.toFloat() / totalMembers.toFloat()).coerceIn(0f, 1f)
        "ACTIVE" ->
            (paidCount.toFloat() / totalMembers.toFloat()).coerceIn(0f, 1f)
        "COMPLETED" -> 1f
        else -> (circle.savedAmount / circle.targetAmount.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    }

    val animProg by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(900, delayMillis = index * 80 + 200, easing = EaseOutExpo),
        label         = "circleProgress_$index"
    )

    val currentRecipient = circle.currentRoundRecipient

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = PundarTheme.colors.surfacePrimary,
        border = BorderStroke(1.dp, PundarTheme.colors.borderPrimary)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            statusLabel,
                            fontSize      = 10.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp,
                            color         = accentColor
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.Group, null, tint = PundarTheme.colors.textMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "$totalMembers Members",
                        style = MaterialTheme.typography.labelSmall,
                        color = PundarTheme.colors.textMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${String.format("%,.0f", circle.scheduledPayoutAmount)} ${circle.currency}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 16.sp,
                        color      = accentColor
                    )
                    Text(
                        "Scheduled Payout / Round",
                        fontWeight = FontWeight.Medium,
                        fontSize   = 10.sp,
                        color      = PundarTheme.colors.textMuted
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(circle.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PundarTheme.colors.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Fixed Contribution: ${String.format("%,.0f", circle.contributionPerMonth)} ${circle.currency} · Schedule: ${circle.targetDate}",
                style = MaterialTheme.typography.bodySmall,
                color = PundarTheme.colors.textSecondary
            )

            Spacer(Modifier.height(14.dp))

            Box(
                Modifier.fillMaxWidth().height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PundarTheme.colors.surfaceTertiary)
            ) {
                Box(
                    Modifier.fillMaxHeight()
                        .fillMaxWidth(animProg)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(listOf(accentColor, accentColor.copy(0.65f)))
                        )
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val leftProgressText = when (circle.lifecycleStatus) {
                    "AWAITING_ACCEPTANCE", "DECLINED_REVISION_NEEDED", "READY_TO_ACTIVATE" ->
                        "$acceptedCount of $totalMembers Accepted Terms (v${circle.termsVersion})"
                    "ACTIVE" ->
                        "$paidCount of $totalMembers Contributed (Round ${circle.currentRound})"
                    "COMPLETED" ->
                        "All $totalMembers Rounds Paid"
                    else -> "${(animProg * 100).toInt()}% Completed"
                }
                val rightProgressText = when (circle.lifecycleStatus) {
                    "AWAITING_ACCEPTANCE" -> "Inactive Proposal"
                    "DECLINED_REVISION_NEEDED" -> "Action Required"
                    "READY_TO_ACTIVATE" -> "Ready to Activate"
                    "ACTIVE" -> if (circle.isCurrentRoundPayoutEligible) "Payout Eligible ✓" else "${totalMembers - paidCount} pending"
                    "COMPLETED" -> "Completed ✓"
                    else -> ""
                }
                Text(
                    leftProgressText,
                    style      = MaterialTheme.typography.labelSmall,
                    color      = PundarTheme.colors.textMuted,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    rightProgressText,
                    style      = MaterialTheme.typography.labelSmall,
                    color      = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(0.08f))
                    .border(1.dp, accentColor.copy(0.20f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        if (circle.lifecycleStatus == "COMPLETED") "Final Status"
                        else "Round ${circle.currentRound} Scheduled Recipient",
                        style = MaterialTheme.typography.labelMedium,
                        color = PundarTheme.colors.textSecondary
                    )
                    Text(
                        if (circle.lifecycleStatus == "COMPLETED") "All Payouts Settled"
                        else (currentRecipient?.name ?: "Round #${circle.currentRound}"),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 13.sp,
                        color      = accentColor
                    )
                }
            }
        }
    }
}

// ── Empty state card ──────────────────────────────────────────────
@Composable
private fun CircleEmptyState(onCreateClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = PundarTheme.colors.surfacePrimary,
        border = BorderStroke(1.dp, PundarTheme.colors.borderPrimary)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🌀", fontSize = 36.sp)
            Spacer(Modifier.height(10.dp))
            Text("No PUNDAR Circles yet.", fontWeight = FontWeight.SemiBold, color = PundarTheme.colors.textSecondary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Create a deterministic rotating savings Circle where members review and accept fixed contribution terms and payout order before Soroban activation.",
                style     = MaterialTheme.typography.bodySmall,
                color     = PundarTheme.colors.textMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = PundarTheme.colors.brandPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Create Circle", fontWeight = FontWeight.Bold)
            }
        }
    }
}
