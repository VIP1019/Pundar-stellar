package com.example.pundarapp.ui.screens.auth

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.R
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.remote.GoogleSignInHelper
import com.example.pundarapp.ui.components.PundarPrimaryButton
import com.example.pundarapp.ui.navigation.Routes
import com.example.pundarapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController) {
    var fullName by remember { mutableStateOf("") }
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        containerColor = PundarBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = PundarTextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Join the PUNDAR ecosystem today.",
                style = MaterialTheme.typography.bodyLarge,
                color = PundarTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Input Fields
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PundarBlue,
                    focusedLabelColor = PundarBlue,
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = emailOrPhone,
                onValueChange = { emailOrPhone = it },
                label = { Text("Email or Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PundarBlue,
                    focusedLabelColor = PundarBlue,
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle password visibility")
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PundarBlue,
                    focusedLabelColor = PundarBlue,
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PundarBlue,
                    focusedLabelColor = PundarBlue,
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = PundarError,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            PundarPrimaryButton(
                text = if (isLoading) "Registering..." else "Register",
                enabled = !isLoading && fullName.isNotBlank() && emailOrPhone.isNotBlank() && password.isNotBlank() && password == confirmPassword,
                onClick = { 
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = AuthRepository.register(emailOrPhone, password)
                        isLoading = false
                        if (result.isSuccess) {
                            val encodedEmail = java.net.URLEncoder.encode(emailOrPhone, "UTF-8")
                            navController.navigate(Routes.emailConfirm(encodedEmail)) {
                                popUpTo(Routes.LOGIN)
                            }
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Registration failed. Please try again."
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            // Or divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = PundarTextSecondary.copy(alpha = 0.3f))
                Text(
                    text = "Or sign up with",
                    style = MaterialTheme.typography.bodySmall,
                    color = PundarTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = PundarTextSecondary.copy(alpha = 0.3f))
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Google Button - uses CredentialManager directly
            OutlinedButton(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = GoogleSignInHelper.signInWithGoogle(context as Activity)
                        isLoading = false
                        if (result.isSuccess) {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        } else {
                            val msg = result.exceptionOrNull()?.message ?: ""
                            if (msg != "cancelled") {
                                errorMessage = msg.ifBlank { "Google sign-in failed." }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PundarTextPrimary)
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_myplaces),
                    contentDescription = "Google",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Google",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Phone Button
            OutlinedButton(
                onClick = { navController.navigate(Routes.PHONE_LOGIN) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PundarTextPrimary)
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_call),
                    contentDescription = "Phone",
                    modifier = Modifier.size(24.dp),
                    tint = PundarBlue
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Phone Number",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PundarTextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Sign in",
                    style = MaterialTheme.typography.labelLarge,
                    color = PundarBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { 
                        navController.navigateUp()
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
