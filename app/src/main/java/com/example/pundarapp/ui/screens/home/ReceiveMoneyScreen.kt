package com.example.pundarapp.ui.screens.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.data.qr.QrCodeGenerator
import com.example.pundarapp.data.qr.QrPayload
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.ui.components.PundarAvatar
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.components.PundarOutlinedButton
import com.example.pundarapp.ui.components.PundarPrimaryButton
import com.example.pundarapp.ui.components.PundarTextButton
import com.example.pundarapp.ui.theme.PundarTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveMoneyScreen(navController: NavController) {
    val context = LocalContext.current
    val user by AuthRepository.currentUserState
    var amountInput by remember { mutableStateOf("") }
    var qrMode by remember { mutableIntStateOf(0) } // 0 = PUNDAR QR, 1 = Stellar SEP-0007 QR
    var refreshNonce by remember { mutableIntStateOf(0) }

    val requestedAmount = amountInput.toDoubleOrNull()?.takeIf { it > 0.0 }

    val qrContent: String? = remember(user?.id, user?.stellarPublicKey, requestedAmount, qrMode, refreshNonce) {
        val currentUser = user ?: return@remember null
        if (qrMode == 1 && !currentUser.stellarPublicKey.isNullOrBlank()) {
            val base = "web+stellar:pay?destination=${currentUser.stellarPublicKey}"
            if (requestedAmount != null) "$base&amount=$requestedAmount" else base
        } else {
            val payload = QrCodeGenerator.createReceiveMoneyPayload(
                userId = currentUser.id,
                displayName = currentUser.name,
                username = currentUser.phone,
                walletId = currentUser.stellarPublicKey,
                amount = requestedAmount
            )
            QrPayload.encode(payload)
        }
    }

    val qrBitmap: Bitmap? = remember(qrContent) {
        qrContent?.let {
            try {
                QrCodeGenerator.generateBitmap(it, 512)
            } catch (_: Exception) {
                null
            }
        }
    }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Receive Money",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarTheme.colors.bgPrimary
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val displayName = user?.name ?: "User"
            val initials = AuthRepository.getCurrentUserInitials()

            PundarAvatar(
                initials = initials,
                imageUrl = user?.profileImageUrl,
                size = 72.dp,
                showRing = true,
                initialsFontSize = 24.sp
            )

            Text(
                displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PundarTheme.colors.textPrimary
            )
            Text(
                user?.phone ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = PundarTheme.colors.textSecondary
            )

            if (!user?.stellarPublicKey.isNullOrBlank()) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = qrMode == 0,
                        onClick = { qrMode = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("PUNDAR QR") }
                    SegmentedButton(
                        selected = qrMode == 1,
                        onClick = { qrMode = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Stellar QR") }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PundarTheme.colors.surfacePrimary),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Receive money QR code",
                            modifier = Modifier.size(220.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PundarTheme.colors.brandPrimary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (requestedAmount != null) {
                            "Scan to send ₱${String.format("%,.2f", requestedAmount)}"
                        } else {
                            "Scan to send money"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = PundarTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Account: ${user?.id ?: ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = PundarTheme.colors.textPrimary
                    )
                    if (!user?.stellarPublicKey.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        val pk = user!!.stellarPublicKey!!
                        Text(
                            "Stellar: ${pk.take(8)}...${pk.takeLast(6)} (tap to copy)",
                            style = MaterialTheme.typography.labelSmall,
                            color = PundarTheme.colors.brandPrimary,
                            modifier = Modifier.clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Stellar Public Key", pk))
                                Toast.makeText(context, "Stellar address copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = amountInput,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountInput = input
                    }
                },
                label = { Text("Request specific amount (optional)") },
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PundarTheme.colors.brandPrimary,
                    focusedLabelColor = PundarTheme.colors.brandPrimary,
                    focusedTextColor = PundarTheme.colors.textPrimary,
                    unfocusedTextColor = PundarTheme.colors.textPrimary
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PundarOutlinedButton(
                    text = "Copy",
                    onClick = {
                        val copyVal = if (qrMode == 1 && !user?.stellarPublicKey.isNullOrBlank()) {
                            user!!.stellarPublicKey!!
                        } else {
                            user?.id ?: return@PundarOutlinedButton
                        }
                        val label = if (qrMode == 1) "Stellar Address" else "User ID"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(label, copyVal))
                        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.ContentCopy
                )
                PundarOutlinedButton(
                    text = "Save",
                    onClick = {
                        val bmp = qrBitmap ?: return@PundarOutlinedButton
                        val fileName = "PUNDAR_QR_${user?.id ?: "wallet"}_${System.currentTimeMillis()}.png"
                        QrCodeGenerator.saveBitmapToGallery(context, bmp, fileName)
                            .onSuccess {
                                Toast.makeText(context, "QR saved to Pictures/PUNDAR", Toast.LENGTH_SHORT).show()
                            }
                            .onFailure { e ->
                                Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Download
                )
                PundarOutlinedButton(
                    text = "Share",
                    onClick = {
                        val bmp = qrBitmap ?: return@PundarOutlinedButton
                        val amountText = if (requestedAmount != null) " (₱${String.format("%,.2f", requestedAmount)})" else ""
                        QrCodeGenerator.shareQrBitmap(
                            context = context,
                            bitmap = bmp,
                            shareText = "Send me money on PUNDAR$amountText. Account: ${user?.id ?: ""}",
                            chooserTitle = "Share Receive QR"
                        )
                    },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Share
                )
            }

            PundarTextButton(
                text = "Refresh QR",
                onClick = {
                    refreshNonce++
                    Toast.makeText(context, "QR refreshed", Toast.LENGTH_SHORT).show()
                },
                icon = Icons.Filled.Refresh
            )

            Spacer(Modifier.height(8.dp))

            PundarPrimaryButton(
                text = "Done",
                onClick = { navController.navigateUp() }
            )
        }
    }
}
