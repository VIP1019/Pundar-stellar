package com.example.pundarapp.ui.screens.pay

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pundarapp.data.qr.QrCodeGenerator
import com.example.pundarapp.data.qr.QrPayload
import com.example.pundarapp.data.qr.isExpired
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.remote.BillQrRepository
import com.example.pundarapp.data.remote.BillQrStatus
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.components.PundarOutlinedButton
import com.example.pundarapp.ui.components.PundarPrimaryButton
import com.example.pundarapp.ui.components.PundarTextButton
import com.example.pundarapp.ui.components.StatusBadge
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val QR_TTL_MS = 5L * 60L * 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillQrScreen(billId: String, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = AuthRepository.getCurrentUserId()

    val bill = AppState.bills.find { it.id == billId }
        ?: run { navController.navigateUp(); return }

    var payload by remember(billId) { mutableStateOf<QrPayload?>(null) }
    var qrStatus by remember { mutableStateOf(BillQrStatus.ACTIVE) }
    var remainingMs by remember { mutableLongStateOf(QR_TTL_MS) }
    var isGenerating by remember { mutableStateOf(true) }

    fun generatePayload(): QrPayload {
        val txnId = "BILL-${UUID.randomUUID().toString().take(8).uppercase()}"
        return QrCodeGenerator.createBillPaymentPayload(
            userId = userId ?: bill.id,
            amount = bill.yourShare,
            billRef = bill.name,
            transactionId = txnId
        )
    }

    LaunchedEffect(billId) {
        isGenerating = true
        val newPayload = generatePayload()
        payload = newPayload
        if (userId != null) {
            BillQrRepository.save(newPayload, billId, userId, QR_TTL_MS)
        }
        qrStatus = BillQrStatus.ACTIVE
        remainingMs = QR_TTL_MS
        isGenerating = false
    }

    // Countdown + poll payment status
    LaunchedEffect(payload?.securityToken) {
        val token = payload?.securityToken ?: return@LaunchedEffect
        while (true) {
            val p = payload ?: break
            if (p.isExpired(QR_TTL_MS)) {
                qrStatus = BillQrStatus.EXPIRED
                remainingMs = 0L
            } else {
                remainingMs = QR_TTL_MS - (System.currentTimeMillis() - p.timestamp)
                val remote = BillQrRepository.resolveStatus(token)
                if (remote == BillQrStatus.PAID) {
                    qrStatus = BillQrStatus.PAID
                    AppState.settleBill(billId)
                    break
                }
                if (remote == BillQrStatus.EXPIRED) qrStatus = BillQrStatus.EXPIRED
            }
            delay(1000)
        }
    }

    val qrBitmap: Bitmap? = remember(payload) {
        payload?.let { QrCodeGenerator.generateBitmap(it) }
    }

    val statusColor = when (qrStatus) {
        BillQrStatus.ACTIVE -> PundarSuccess
        BillQrStatus.PAID -> ElectricBlue
        BillQrStatus.EXPIRED -> PundarWarning
        BillQrStatus.CANCELLED -> PundarError
    }

    val statusLabel = when (qrStatus) {
        BillQrStatus.ACTIVE -> "Awaiting Payment"
        BillQrStatus.PAID -> "Paid"
        BillQrStatus.EXPIRED -> "Expired"
        BillQrStatus.CANCELLED -> "Cancelled"
    }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title = "Bill Payment QR",
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
            StatusBadge(text = statusLabel, color = statusColor)

            Text(
                bill.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PundarTextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                "₱${String.format("%,.2f", bill.yourShare)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = PundarBlue
            )

            if (qrStatus == BillQrStatus.ACTIVE) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Timer, null, tint = PundarTextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Expires in ${formatCountdown(remainingMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (remainingMs < 60_000) PundarWarning else PundarTextSecondary
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PundarSurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        isGenerating -> CircularProgressIndicator(color = PundarBlue)
                        qrStatus == BillQrStatus.PAID -> {
                            Icon(Icons.Filled.CheckCircle, null, tint = PundarSuccess, modifier = Modifier.size(80.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Payment received!", color = PundarSuccess, fontWeight = FontWeight.Bold)
                        }
                        qrStatus == BillQrStatus.EXPIRED -> {
                            Text(
                                "This QR has expired.\nGenerate a new one to continue.",
                                color = PundarTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                        qrBitmap != null -> {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Bill payment QR",
                                modifier = Modifier.size(220.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Ref: ${payload?.transactionId ?: bill.id}",
                        style = MaterialTheme.typography.labelMedium,
                        color = PundarTextSecondary
                    )
                }
            }

            if (qrStatus == BillQrStatus.ACTIVE && payload != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PundarOutlinedButton(
                        text = "Share",
                        onClick = {
                            val encoded = QrPayload.encode(payload!!)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Pay ₱${String.format("%,.2f", bill.yourShare)} for '${bill.name}' on PUNDAR:\n$encoded")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Bill QR"))
                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Share
                    )
                }

                PundarTextButton(
                    text = "Refresh QR",
                    onClick = {
                        scope.launch {
                            isGenerating = true
                            val newPayload = generatePayload()
                            payload = newPayload
                            if (userId != null) {
                                BillQrRepository.save(newPayload, billId, userId, QR_TTL_MS)
                            }
                            qrStatus = BillQrStatus.ACTIVE
                            remainingMs = QR_TTL_MS
                            isGenerating = false
                            Toast.makeText(context, "New QR generated", Toast.LENGTH_SHORT).show()
                        }
                    },
                    icon = Icons.Filled.Refresh
                )
            }

            if (qrStatus == BillQrStatus.EXPIRED) {
                PundarPrimaryButton(
                    text = "Generate New QR",
                    onClick = {
                        scope.launch {
                            isGenerating = true
                            val newPayload = generatePayload()
                            payload = newPayload
                            if (userId != null) {
                                BillQrRepository.save(newPayload, billId, userId, QR_TTL_MS)
                            }
                            qrStatus = BillQrStatus.ACTIVE
                            remainingMs = QR_TTL_MS
                            isGenerating = false
                        }
                    }
                )
            }

            if (qrStatus == BillQrStatus.PAID) {
                PundarPrimaryButton(
                    text = "Done",
                    onClick = { navController.navigateUp() }
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                "Customer scans this QR to pay instantly.",
                style = MaterialTheme.typography.bodySmall,
                color = PundarTextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatCountdown(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(safe)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(safe) % 60
    return "%d:%02d".format(minutes, seconds)
}
