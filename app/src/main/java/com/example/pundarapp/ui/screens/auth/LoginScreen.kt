package com.example.pundarapp.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.pundarapp.ui.components.Icon3DWarning
import com.example.pundarapp.ui.components.PundarPrimaryButton
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import com.example.pundarapp.ui.theme.PundarTheme
import kotlinx.coroutines.launch

private val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    var phone        by remember { mutableStateOf("") }
    var mpin         by remember { mutableStateOf("") }
    var mpinVisible  by remember { mutableStateOf(false) }
    var isLoading    by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope        = rememberCoroutineScope()

    val headerAlpha   = remember { Animatable(0f) }
    val formAlpha     = remember { Animatable(0f) }
    val formSlide     = remember { Animatable(28f) }
    LaunchedEffect(Unit) {
        headerAlpha.animateTo(1f, tween(520, easing = EaseOutExpo))
        formAlpha.animateTo(1f, tween(480, delayMillis = 160, easing = EaseOutExpo))
        formSlide.animateTo(0f, tween(480, delayMillis = 160, easing = EaseOutExpo))
    }

    val inf   = rememberInfiniteTransition(label = "loginBg")
    val orbY  by inf.animateFloat(-18f, 18f,
        infiniteRepeatable(tween(3600, easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)), RepeatMode.Reverse),
        label = "oy")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF08122A), PundarTheme.colors.bgPrimary, PundarTheme.colors.bgSecondary)))
    ) {
        // Ambient blue orb
        Box(
            Modifier.size(340.dp).align(Alignment.TopCenter)
                .offset(y = ((-70).dp + orbY.dp))
                .blur(130.dp)
                .background(PundarTheme.colors.brandPrimary.copy(0.10f), CircleShape)
        )

        Column(
            modifier            = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(0.6f))

            // Logo + header
            Column(
                modifier            = Modifier.alpha(headerAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(PundarTheme.colors.brandPrimary, PundarTheme.colors.brandSecondary)))
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(PundarTheme.colors.surfacePrimary)
                    ) {
                        Text("P", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Blue400)
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text("Welcome Back",
                    fontWeight = FontWeight.Bold, fontSize = 26.sp,
                    color = PundarTheme.colors.textPrimary, letterSpacing = (-0.3).sp)
                Spacer(Modifier.height(6.dp))
                Text("Sign in to your PUNDAR account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PundarTheme.colors.textMuted, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(36.dp))

            // Form card
            Box(
                modifier = Modifier
                    .graphicsLayer(alpha = formAlpha.value, translationY = formSlide.value)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(PundarTheme.colors.surfacePrimary, PundarTheme.colors.surfaceSecondary)))
                    .border(1.dp, Brush.linearGradient(listOf(PundarTheme.colors.glassMedium, PundarTheme.colors.glassSubtle)), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(22.dp)) {

                    // Mobile Number
                    Text("Mobile Number", style = MaterialTheme.typography.labelMedium,
                        color = PundarTheme.colors.textSecondary, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value           = phone,
                        onValueChange   = { phone = it },
                        placeholder     = { Text("09171234567", color = PundarTheme.colors.textDim) },
                        modifier        = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors          = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = Blue400,
                            unfocusedBorderColor    = PundarTheme.colors.borderPrimary,
                            focusedLabelColor       = Blue400,
                            focusedTextColor        = PundarTheme.colors.textPrimary,
                            unfocusedTextColor      = PundarTheme.colors.textPrimary,
                            cursorColor             = Blue400,
                            focusedContainerColor   = PundarTheme.colors.surfaceSecondary,
                            unfocusedContainerColor = PundarTheme.colors.surfaceSecondary
                        ),
                        shape      = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(Modifier.height(18.dp))

                    // 4-Digit MPIN
                    Text("4-Digit MPIN", style = MaterialTheme.typography.labelMedium,
                        color = PundarTheme.colors.textSecondary, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value                = mpin,
                        onValueChange        = { mpin = it.take(4) },
                        placeholder          = { Text("••••", color = PundarTheme.colors.textDim) },
                        modifier             = Modifier.fillMaxWidth(),
                        visualTransformation = if (mpinVisible) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        trailingIcon = {
                            IconButton(onClick = { mpinVisible = !mpinVisible }) {
                                Icon(
                                    if (mpinVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    "Toggle", tint = PundarTheme.colors.textMuted
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = Blue400,
                            unfocusedBorderColor    = PundarTheme.colors.borderPrimary,
                            focusedLabelColor       = Blue400,
                            focusedTextColor        = PundarTheme.colors.textPrimary,
                            unfocusedTextColor      = PundarTheme.colors.textPrimary,
                            cursorColor             = Blue400,
                            focusedContainerColor   = PundarTheme.colors.surfaceSecondary,
                            unfocusedContainerColor = PundarTheme.colors.surfaceSecondary
                        ),
                        shape      = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    // Error
                    if (errorMessage != null) {
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(RedBg)
                                .border(1.dp, PundarTheme.colors.accentRed.copy(0.30f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon3DWarning(size = 14.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(errorMessage!!, color = Red400,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(Modifier.height(26.dp))
                    PundarPrimaryButton(
                        text    = if (isLoading) "Signing in…" else "Sign In",
                        enabled = !isLoading && phone.isNotBlank() && mpin.length == 4,
                        onClick = {
                            isLoading    = true
                            errorMessage = null
                            scope.launch {
                                val result = AuthRepository.loginWithPhone(phone, mpin)
                                isLoading  = false
                                if (result.isSuccess) {
                                    AppState.refreshWalletBalance()
                                    AppState.refreshNotifications()
                                    AppState.loadFavorites()
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

            // Register link
            Row(
                modifier = Modifier.alpha(formAlpha.value).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Don't have an account? ",
                    color = PundarTheme.colors.textMuted, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Register Here",
                    style      = MaterialTheme.typography.labelLarge,
                    color      = PundarTheme.colors.brandLight,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.clickable { navController.navigate(Routes.REGISTER) }
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
