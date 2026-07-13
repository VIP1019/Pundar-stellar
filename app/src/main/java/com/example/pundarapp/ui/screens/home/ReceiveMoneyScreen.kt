package com.example.pundarapp.ui.screens.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.example.pundarapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveMoneyScreen(navController: NavController) {
    val context = LocalContext.current
    val user by AuthRepository.currentUserState

    var payload by remember(user?.id) {
        mutableStateOf(
            user?.let {
                QrCodeGenerator.createReceiveMoneyPayload(
                    userId = it.id,
                    displayName = it.name,
                    username = it.phone,
                    walletId = it.stellarPublicKey
                )
            }
        )
    }

    val qrBitmap: Bitmap? = remember(payload) {
        payload?.let { QrCodeGenerator.generateBitmap(it) }
    }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Receive Money",
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = PundarBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            Text(displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PundarTextPrimary)
            Text(user?.phone ?: "", style = MaterialTheme.typography.bodyMedium, color = PundarTextSecondary)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PundarSurface),
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
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Scan to send money",
                        style = MaterialTheme.typography.bodySmall,
                        color = PundarTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Account: ${user?.id ?: ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = PundarTextPrimary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PundarOutlinedButton(
                    text = "Copy",
                    onClick = {
                        val id = user?.id ?: return@PundarOutlinedButton
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("User ID", id))
                        Toast.makeText(context, "User ID copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.ContentCopy
                )
                PundarOutlinedButton(
                    text = "Save",
                    onClick = { Toast.makeText(context, "QR saved to gallery", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Download
                )
                PundarOutlinedButton(
                    text = "Share",
                    onClick = {
                        val encoded = payload?.let { QrPayload.encode(it) } ?: return@PundarOutlinedButton
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Send me money on PUNDAR. Scan this QR:\n$encoded")
                        }
                        context.startActivity(Intent.createChooser(intent, "Share QR"))
                    },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Share
                )
            }

            PundarTextButton(
                text = "Refresh QR",
                onClick = {
                    user?.let {
                        payload = QrCodeGenerator.createReceiveMoneyPayload(
                            userId = it.id,
                            displayName = it.name,
                            username = it.phone,
                            walletId = it.stellarPublicKey
                        )
                    }
                },
                icon = Icons.Filled.Refresh
            )

            Spacer(Modifier.weight(1f))

            PundarPrimaryButton(
                text = "Done",
                onClick = { navController.navigateUp() }
            )
        }
    }
}
