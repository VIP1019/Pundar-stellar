package com.example.pundarapp.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.launch

private val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController) {
    var fullName           by remember { mutableStateOf("") }
    var phone              by remember { mutableStateOf("") }
    var mpin               by remember { mutableStateOf("") }
    var confirmMpin        by remember { mutableStateOf("") }
    var mpinVisible        by remember { mutableStateOf(false) }
    var confirmMpinVisible by remember { mutableStateOf(false) }
    var isLoading          by remember { mutableStateOf(false) }
    var errorMessage       by remember { mutableStateOf<String?>(null) }
    val scope              = rememberCoroutineScope()

    val formAlpha = remember { Animatable(0f) }
    val formSlide = remember { Animatable(24f) }
    LaunchedEffect(Unit) {
        formAlpha.animateTo(1f, tween(500, easing = EaseOutExpo))
        formSlide.animateTo(0f, tween(500, easing = EaseOutExpo))
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor      = Blue400,
        unfocusedBorderColor    = NavyBorder,
        focusedLabelColor       = Blue400,
        unfocusedLabelColor     = TextMuted,
        focusedTextColor        = TextWhite,
        unfocusedTextColor      = TextWhite,
        cursorColor             = Blue400,
        focusedContainerColor   = Navy700,
        unfocusedContainerColor = Navy700
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF08122A), Navy900, Navy950)))
    ) {
        // Ambient orb
        Box(
            Modifier.size(300.dp).align(Alignment.TopCenter)
                .offset(y = (-60).dp).blur(120.dp)
                .background(Blue500.copy(0.09f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            // Header
            Column(
                modifier            = Modifier.alpha(formAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(68.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Blue500, Blue600)))
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(60.dp).clip(CircleShape).background(Navy800)
                    ) {
                        Text("P", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Blue400)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 24.sp,
                    color = TextWhite, letterSpacing = (-0.3).sp)
                Spacer(Modifier.height(5.dp))
                Text("Join PUNDAR — build together, grow together.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(32.dp))

            // Form card
            Box(
                modifier = Modifier
                    .graphicsLayer(alpha = formAlpha.value, translationY = formSlide.value)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Navy800, Navy700)))
                    .border(1.dp, Brush.linearGradient(listOf(Glass15, Glass10)), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(22.dp)) {

                    // Full Name
                    Text("Full Name", style = MaterialTheme.typography.labelMedium,
                        color = TextSoft, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value           = fullName,
                        onValueChange   = { fullName = it },
                        placeholder     = { Text("Juan dela Cruz", color = TextDim) },
                        modifier        = Modifier.fillMaxWidth(),
                        colors          = fieldColors,
                        shape           = RoundedCornerShape(14.dp),
                        singleLine      = true
                    )

                    Spacer(Modifier.height(16.dp))

                    // Mobile
                    Text("Mobile Number", style = MaterialTheme.typography.labelMedium,
                        color = TextSoft, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value           = phone,
                        onValueChange   = { phone = it },
                        placeholder     = { Text("09171234567", color = TextDim) },
                        modifier        = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors          = fieldColors,
                        shape           = RoundedCornerShape(14.dp),
                        singleLine      = true
                    )

                    Spacer(Modifier.height(16.dp))

                    // MPIN
                    Text("Set 4-Digit MPIN", style = MaterialTheme.typography.labelMedium,
                        color = TextSoft, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value                = mpin,
                        onValueChange        = { mpin = it.take(4) },
                        placeholder          = { Text("••••", color = TextDim) },
                        modifier             = Modifier.fillMaxWidth(),
                        visualTransformation = if (mpinVisible) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        trailingIcon = {
                            IconButton(onClick = { mpinVisible = !mpinVisible }) {
                                Icon(if (mpinVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    "Toggle", tint = TextMuted)
                            }
                        },
                        colors     = fieldColors,
                        shape      = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(Modifier.height(16.dp))

                    // Confirm MPIN
                    Text("Confirm MPIN", style = MaterialTheme.typography.labelMedium,
                        color = TextSoft, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value                = confirmMpin,
                        onValueChange        = { confirmMpin = it.take(4) },
                        placeholder          = { Text("••••", color = TextDim) },
                        modifier             = Modifier.fillMaxWidth(),
                        visualTransformation = if (confirmMpinVisible) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        trailingIcon = {
                            IconButton(onClick = { confirmMpinVisible = !confirmMpinVisible }) {
                                Icon(if (confirmMpinVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    "Toggle", tint = TextMuted)
                            }
                        },
                        colors     = fieldColors,
                        shape      = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    // Error
                    if (errorMessage != null) {
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(RedBg)
                                .border(1.dp, Red500.copy(0.30f), RoundedCornerShape(10.dp))
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
                        text    = if (isLoading) "Creating account…" else "Register",
                        enabled = !isLoading && fullName.isNotBlank() && phone.isNotBlank()
                                  && mpin.length == 4 && confirmMpin.length == 4,
                        onClick = {
                            if (mpin != confirmMpin) {
                                errorMessage = "MPINs do not match."
                                return@PundarPrimaryButton
                            }
                            isLoading    = true
                            errorMessage = null
                            scope.launch {
                                val result = AuthRepository.registerWithPhone(phone, fullName, mpin)
                                isLoading  = false
                                if (result.isSuccess) {
                                    AuthRepository.loginWithPhone(phone, mpin)
                                    AppState.refreshWalletBalance()
                                    AppState.refreshNotifications()
                                    AppState.loadFavorites()
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.LOGIN)  { inclusive = true }
                                        popUpTo(Routes.REGISTER) { inclusive = true }
                                    }
                                } else {
                                    val msg = result.exceptionOrNull()?.message ?: "Unknown error"
                                    errorMessage = if (msg.contains("already registered"))
                                        "This mobile number is already registered."
                                    else "Error: $msg"
                                }
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Sign in link
            Row(
                modifier = Modifier.alpha(formAlpha.value).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Already have an account? ", color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium)
                Text("Sign In", style = MaterialTheme.typography.labelLarge,
                    color = Blue300, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { navController.navigate(Routes.LOGIN) })
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
