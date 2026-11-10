package com.example.pundarapp.ui.screens.home

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.ui.data.HomeActivity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReceiptDialog(
    activity: HomeActivity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sdfTime = SimpleDateFormat("h:mm a", Locale.getDefault())
    val sdfFull = SimpleDateFormat("MMMM dd, yyyy '|' h:mm a", Locale.getDefault())
    val timeStr = sdfTime.format(Date(activity.timestamp))
    val fullDateStr = sdfFull.format(Date(activity.timestamp))
    val userName = AuthRepository.getCurrentUserName().ifBlank { "PUNDAR User" }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Close Button
                Row(modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── SUCCESS CIRCULAR BADGE ─────────────────────────────
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2E8F0).copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1B4B)), // Dark Navy
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Success",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Pill Badge: • Payment Successful
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE2E8F0))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1B4B))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Payment Successful",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1B4B)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Heading
                Text(
                    "Payment Sent Successfully!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                // Subtitle
                Text(
                    "Today, $timeStr •",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(Modifier.height(14.dp))

                // Big Bold Amount
                Text(
                    activity.amount,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )

                Spacer(Modifier.height(24.dp))

                // ── STACKED CARD 1: Merchant / Recipient details ─────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Storefront,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                activity.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "Ref: ${activity.referenceId.ifBlank { "BLNGY-MKT-99210" }}",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "✓ QR Ph",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── STACKED CARD 2: Group Settlement details ────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Group,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Group Bill Settlement",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "2/3 Friends notified via PUNDAR Pay",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                            listOf("MP", "AK", "..").forEach { init ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2563EB))
                                        .border(1.5.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(init, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Bottom Ref Info
                Text(
                    "Ref No. ${activity.referenceId.ifBlank { "001318387" }}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    fullDateStr,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Close", color = Color(0xFF0F172A))
                    }

                    Button(
                        onClick = {
                            printReceipt(
                                context = context,
                                activity = activity,
                                dateStr = fullDateStr,
                                userName = userName
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Icon(
                            Icons.Filled.Print,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Print")
                    }
                }
            }
        }
    }
}

fun printReceipt(
    context: android.content.Context,
    activity: HomeActivity,
    dateStr: String,
    userName: String
) {
    val html = buildReceiptHtml(activity, dateStr, userName)
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as PrintManager
            val jobName = "PUNDAR Receipt - ${activity.referenceId}"
            val printAdapter = view.createPrintDocumentAdapter(jobName)
            val attrs = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
            printManager.print(jobName, printAdapter, attrs)
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}

private fun buildReceiptHtml(
    activity: HomeActivity,
    dateStr: String,
    userName: String
): String {
    val amountColor = if (activity.isPositive) "#16C784" else "#1F2937"
    return """
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8"/>
  <style>
    body { font-family: sans-serif; background: #f8fafc; color: #0f172a; padding: 20px; text-align: center; }
    .receipt { max-width: 400px; margin: auto; background: white; border-radius: 16px; padding: 24px; border: 1px solid #e2e8f0; }
    .amount { font-size: 32px; font-weight: bold; color: ${amountColor}; margin: 12px 0; }
  </style>
</head>
<body>
  <div class="receipt">
    <h2>PUNDAR Pay Receipt</h2>
    <div class="amount">${activity.amount}</div>
    <p><strong>${activity.title}</strong></p>
    <p>${activity.subtitle}</p>
    <p>Date: $dateStr</p>
    <p>Account: $userName</p>
    <p>Ref: ${activity.referenceId}</p>
  </div>
</body>
</html>
    """.trimIndent()
}
