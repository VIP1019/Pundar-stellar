package com.example.pundarapp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private data class TourStep(
    val badge: String,
    val title: String,
    val description: String,
    val highlights: List<String>,
    val icon: ImageVector,
    val accentColor: Color
)

private val pundarTourSteps = listOf(
    TourStep(
        badge = "WELCOME TO PUNDAR",
        title = "Build Together, Grow Together",
        description = "Your all-in-one Stellar-powered social wallet designed for instant payments, group bill splits, and community savings.",
        highlights = listOf(
            "Live PHP & Stellar (XLM) dual-balance wallet",
            "Instant peer-to-peer & QR code settlements",
            "Verified Pundar Trust Score on every transaction"
        ),
        icon = Icons.Filled.AccountBalanceWallet,
        accentColor = Color(0xFF2563EB)
    ),
    TourStep(
        badge = "PRIVACY & WALLET",
        title = "Hide Balance & Quick Actions",
        description = "Keep your finances private in public spaces and access Send, Receive, Cash In, and Convert in one tap.",
        highlights = listOf(
            "Tap the Eye icon next to your balance to hide/show amounts",
            "Filter dashboard activities by All, Received, Sent, Pay, or Circle",
            "Tap any activity row to view its verifiable transaction receipt"
        ),
        icon = Icons.Filled.VisibilityOff,
        accentColor = Color(0xFF10B981)
    ),
    TourStep(
        badge = "SMART QR & PUNDAR PAY",
        title = "Configure Before Generating QR",
        description = "Whether requesting money or inviting friends to a group split, you set the exact parameters before generating your QR.",
        highlights = listOf(
            "Set Bill Title, Category, Total Amount & Participant Count first",
            "Locked Share per person prevents payers from editing the amount",
            "Scan any PUNDAR or Stellar QR from the center scanner tab"
        ),
        icon = Icons.Filled.QrCodeScanner,
        accentColor = Color(0xFF6366F1)
    ),
    TourStep(
        badge = "SAVINGS CIRCLES & GROW",
        title = "Community Paluwagan & Round-Ups",
        description = "Save with trusted friends in Soroban smart-contract Circles and automatically invest spare change.",
        highlights = listOf(
            "Deterministic rotating payouts tracked on Stellar Testnet",
            "Automatic spare-change Round-Ups when settling bills",
            "Customize your profile photo & replay this tour in Settings"
        ),
        icon = Icons.Filled.Groups,
        accentColor = Color(0xFFF59E0B)
    )
)

/**
 * Interactive Product Tour / Walkthrough modal shown automatically upon onboarding
 * (or replayed anytime from the Home header or Settings).
 */
@Composable
fun ProductTourDialog(
    userName: String = "there",
    onFinish: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val step = pundarTourSteps[currentStep]
    val isLastStep = currentStep == pundarTourSteps.lastIndex

    Dialog(
        onDismissRequest = onFinish,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Deep Navy Hero Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0B1B3D), Color(0xFF1E3A8A))
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = Color.White.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "STEP ${currentStep + 1} OF ${pundarTourSteps.size}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "Skip Tour",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onFinish)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        AnimatedContent(
                            targetState = step,
                            transitionSpec = {
                                fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                            },
                            label = "tourHeroIcon"
                        ) { s ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(s.accentColor.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(s.accentColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = s.icon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = if (currentStep == 0 && userName.isNotBlank())
                                "Hello, ${userName.split(" ").firstOrNull() ?: userName}!"
                            else step.badge,
                            color = Color(0xFF60A5FA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = step.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Body Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = step.description,
                        color = Color(0xFF475569),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            step.highlights.forEach { bullet ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = step.accentColor,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .padding(top = 1.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = bullet,
                                        color = Color(0xFF0F172A),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Step Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        pundarTourSteps.indices.forEach { index ->
                            val isSelected = index == currentStep
                            Box(
                                modifier = Modifier
                                    .height(7.dp)
                                    .width(if (isSelected) 22.dp else 7.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isSelected) Color(0xFF0B1B3D) else Color(0xFFCBD5E1)
                                    )
                                    .clickable { currentStep = index }
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Navigation Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (currentStep > 0) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Back",
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isLastStep) {
                                    onFinish()
                                } else {
                                    currentStep++
                                }
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0B1B3D)
                            )
                        ) {
                            Text(
                                text = if (isLastStep) "Get Started" else "Next",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isLastStep) Icons.Filled.DoneAll else Icons.AutoMirrored.Filled.ArrowForward,
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

