package com.example.pundarapp.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.ui.components.PundarPrimaryButton
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.components.Icon3DWarning
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    var phone        by remember { mutableStateOf("") }
    var mpin         by remember { mutableStateOf("") }
    var mpinVisible  by remember { mutableStateOf(false) }
    var isLoading    by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // ── Entrance animations ──────────────────────────────────────
    val headerAlpha   = remember { Animatable(0f) }
    val formAlpha     = remember { Animatable(0f) }
    val formTranslate = remember { Animatable(40f) }

    LaunchedEffect(Unit) {
        headerAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        formAlpha.animateTo(1f, tween(500, delayMillis = 200, easing = FastOutSlowInEasing))
        formTranslate.animateTo(0f, tween(500, delayMillis = 200, easing = FastOutSlowInEasing))
    }

    // ── Ambient orb animation ────────────────────────────────────
    val infinite = rememberInfiniteTransition(label = "loginBg")
    val orb1Y by infinite.animateFloat(
        initialValue = -20f, targetValue = 20f,
        animationSpec = infiniteRepeatable(
            tween(3500, easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)),
            RepeatMode.Reverse
        ), label = "orb1"
    )
    val orb2Y by infinite.animateFloat(
        initialValue = 15f, targetValue = -15f,
        animationSpec = infiniteRepeatable(
            tween(2800, easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)),
            RepeatMode.Reverse
        ), label = "orb2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SpaceBlack, SpaceNavy, Color(0xFF0A1228))
                )
            )
    ) {

        // ── Ambient light orbs ───────────────────────────────────
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-80).dp, y = ((-100).dp + orb1Y.dp))
                .blur(100.dp)
                .background(ElectricBlue.copy(alpha = 0.10f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = (80.dp + orb2Y.dp))
                .blur(100.dp)
                .background(ElectricPurple.copy(alpha = 0.10f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Spacer(Modifier.weight(0.8f))

            // ── Logo & Header ────────────────────────────────────
            Box(
                modifier = Modifier
                    .alpha(headerAlpha.value)
                    .graphicsLayer(alpha = headerAlpha.value)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Logo mark
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(
                                elevation    = 20.dp,
                                shape        = CircleShape,
                                ambientColor = ElectricBlue.copy(0.4f),
                                spotColor    = ElectricBlue.copy(0.4f)
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(ElectricBlue, ElectricPurple)
                                )
                            )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(SpaceDeep)
                        ) {
                            Text(
                                text = "P",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                color = ElectricBlue
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "Welcome Back",
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        color = TextOnDark,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Sign in to your PUNDAR account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(44.dp))

            // ── Form card ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .graphicsLayer(
                        alpha       = formAlpha.value,
                        translationY = formTranslate.value
                    )
                    .fillMaxWidth()
                    .shadow(
                        elevation    = 24.dp,
                        shape        = RoundedCornerShape(28.dp),
                        ambientColor = ElectricBlue.copy(alpha = 0.12f),
                        spotColor    = ElectricBlue.copy(alpha = 0.12f)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(SpaceDeep, Color(0xFF0E1A2E))
                        )
                    )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    // ── Phone field ──────────────────────────────
                    Text(
                        text = "Mobile Number",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        placeholder = { Text("09171234567", color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor    = ElectricBlue,
                            unfocusedBorderColor  = SpaceBorder,
                            focusedLabelColor     = ElectricBlue,
                            focusedTextColor      = TextPrimary,
                            unfocusedTextColor    = TextPrimary,
                            cursorColor           = ElectricBlue,
                            focusedContainerColor = SpaceMedium,
                            unfocusedContainerColor = SpaceMedium
                        ),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── MPIN field ───────────────────────────────
                    Text(
                        text = "4-Digit MPIN",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = mpin,
                        onValueChange = { mpin = it.take(4) },
                        placeholder = { Text("••••", color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (mpinVisible) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        trailingIcon = {
                            IconButton(onClick = { mpinVisible = !mpinVisible }) {
                                Icon(
                                    imageVector = if (mpinVisible) Icons.Filled.Visibility
                                                  else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle MPIN",
                                    tint = TextSecondary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = ElectricBlue,
                            unfocusedBorderColor    = SpaceBorder,
                            focusedLabelColor       = ElectricBlue,
                            focusedTextColor        = TextPrimary,
                            unfocusedTextColor      = TextPrimary,
                            cursorColor             = ElectricBlue,
                            focusedContainerColor   = SpaceMedium,
                            unfocusedContainerColor = SpaceMedium
                        ),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    // Error message
                    if (errorMessage != null) {
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ErrorRed.copy(alpha = 0.12f))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon3DWarning(size = 14.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text  = errorMessage!!,
                                    color = ErrorRed,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // ── CTA ──────────────────────────────────────
                    PundarPrimaryButton(
                        text = if (isLoading) "Verifying..." else "Sign In",
                        enabled = !isLoading && phone.isNotBlank() && mpin.length == 4,
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                val result = AuthRepository.loginWithPhone(phone, mpin)
                                isLoading = false
                                if (result.isSuccess) {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.LOGIN) { inclusive = true }
                                    }
                                } else {
                                    val msg = result.exceptionOrNull()?.message ?: "Unknown error"
                                    errorMessage = if (msg.contains("Invalid login credentials"))
                                        "Invalid Mobile Number or MPIN."
                                    else "Error: $msg"
                                }
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Register link ────────────────────────────────────
            Row(
                modifier = Modifier
                    .alpha(formAlpha.value)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account? ",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Register Here",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { navController.navigate(Routes.REGISTER) }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}


