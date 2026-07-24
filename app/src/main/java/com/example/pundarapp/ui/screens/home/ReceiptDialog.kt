package com.example.pundarapp.ui.screens.home

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
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
import com.example.pundarapp.ui.theme.PundarTheme
import java.text.SimpleDateFormat
import java.util.*

// ═══════════════════════════════════════════════════════════
//  Receipt Dialog
// ═══════════════════════════════════════════════════════════

@Composable
fun ReceiptDialog(
    activity: HomeActivity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("MMMM dd, yyyy  hh:mm a", Locale.getDefault())
    val dateStr = sdf.format(Date(activity.timestamp))
    val userName = AuthRepository.getCurrentUserName().ifBlank { "PUNDAR User" }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(PundarTheme.colors.surfaceSecondary)
                .border(1.dp, PundarTheme.colors.glassMedium, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Header ──────────────────────────────────────────
                Text(
                    text = "PUNDAR",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = PundarTheme.colors.brandPrimary,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "Transaction Receipt",
                    style = MaterialTheme.typography.labelMedium,
                    color = PundarTheme.colors.textMuted,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(20.dp))

                // ── Success checkmark ────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF16C784).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF16C784),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Amount ───────────────────────────────────────────
                Text(
                    text = activity.amount,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    color = if (activity.isPositive) Color(0xFF16C784) else PundarTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "SUCCESSFUL",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF16C784),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(24.dp))

                // ── Divider with scissors ────────────────────────────
                ReceiptDivider()

                Spacer(Modifier.height(20.dp))

                // ── Receipt Details ──────────────────────────────────
                ReceiptRow("Transaction", activity.title)
                ReceiptRow("Description", activity.subtitle)
                ReceiptRow("Module", activity.module)
                ReceiptRow("Date & Time", dateStr)
                ReceiptRow("Account", userName)
                ReceiptRow("Reference ID", activity.referenceId)
                ReceiptRow("Status", "COMPLETED")

                Spacer(Modifier.height(20.dp))

                ReceiptDivider()

                Spacer(Modifier.height(20.dp))

                // ── Footer ───────────────────────────────────────────
                Text(
                    text = "Thank you for using PUNDAR!",
                    style = MaterialTheme.typography.labelMedium,
                    color = PundarTheme.colors.textMuted,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "This is your official digital receipt.",
                    style = MaterialTheme.typography.labelSmall,
                    color = PundarTheme.colors.textDim,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                // ── Buttons ──────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PundarTheme.colors.textPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, PundarTheme.colors.glassMedium
                        )
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = {
                            printReceipt(
                                context = context,
                                activity = activity,
                                dateStr = dateStr,
                                userName = userName
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PundarTheme.colors.brandPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Print,
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

// ── Receipt Row ──────────────────────────────────────────────────
@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = PundarTheme.colors.textMuted,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = PundarTheme.colors.textPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f)
        )
    }
}

// ── Dashed divider ───────────────────────────────────────────────
@Composable
private fun ReceiptDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(30) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(PundarTheme.colors.glassMedium)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  Print Helper — generates HTML and sends to PrintManager
// ═══════════════════════════════════════════════════════════

fun printReceipt(
    context: Context,
    activity: HomeActivity,
    dateStr: String,
    userName: String
) {
    val html = buildReceiptHtml(activity, dateStr, userName)

    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
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
    val amountColor = if (activity.isPositive) "#16C784" else "#1B4A8A"
    return """
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
      font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
      background: #ffffff;
      color: #1a1a2e;
      padding: 0;
    }
    .receipt {
      max-width: 400px;
      margin: 40px auto;
      border: 1px solid #e0e0e0;
      border-radius: 16px;
      overflow: hidden;
      box-shadow: 0 4px 24px rgba(0,0,0,0.08);
    }
    .header {
      background: linear-gradient(135deg, #0F1D45 0%, #1B3A6B 100%);
      color: white;
      padding: 28px 24px 20px;
      text-align: center;
    }
    .header .brand {
      font-size: 26px;
      font-weight: 900;
      letter-spacing: 6px;
      color: #F0B429;
      margin-bottom: 4px;
    }
    .header .subtitle {
      font-size: 11px;
      letter-spacing: 3px;
      color: rgba(255,255,255,0.65);
      text-transform: uppercase;
    }
    .status-section {
      background: #f8fafc;
      padding: 24px;
      text-align: center;
      border-bottom: 1px dashed #e0e0e0;
    }
    .status-badge {
      display: inline-block;
      background: rgba(22, 199, 132, 0.1);
      color: #16C784;
      border: 1px solid rgba(22,199,132,0.3);
      border-radius: 20px;
      padding: 4px 14px;
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 2px;
      margin-bottom: 12px;
    }
    .amount {
      font-size: 36px;
      font-weight: 900;
      color: ${amountColor};
      margin-bottom: 4px;
    }
    .tx-title {
      font-size: 13px;
      color: #666;
      font-weight: 500;
    }
    .details {
      padding: 24px;
    }
    .details table {
      width: 100%;
      border-collapse: collapse;
    }
    .details tr {
      border-bottom: 1px solid #f0f0f0;
    }
    .details tr:last-child {
      border-bottom: none;
    }
    .details td {
      padding: 10px 0;
      font-size: 13px;
      vertical-align: top;
    }
    .details td.label {
      color: #888;
      width: 40%;
      font-weight: 500;
    }
    .details td.value {
      color: #1a1a2e;
      font-weight: 600;
      text-align: right;
      width: 60%;
    }
    .footer {
      background: #f8fafc;
      padding: 20px 24px;
      text-align: center;
      border-top: 1px dashed #e0e0e0;
    }
    .footer p {
      font-size: 12px;
      color: #888;
      margin-bottom: 4px;
    }
    .footer .ref {
      font-size: 10px;
      color: #bbb;
      font-family: 'Courier New', monospace;
      margin-top: 8px;
    }
    .watermark {
      text-align: center;
      padding: 16px;
      font-size: 10px;
      color: #ccc;
      letter-spacing: 1px;
    }
  </style>
</head>
<body>
  <div class="receipt">
    <div class="header">
      <div class="brand">PUNDAR</div>
      <div class="subtitle">Official Transaction Receipt</div>
    </div>
    <div class="status-section">
      <div class="status-badge">✓ COMPLETED</div>
      <div class="amount">${activity.amount}</div>
      <div class="tx-title">${activity.title}</div>
    </div>
    <div class="details">
      <table>
        <tr>
          <td class="label">Description</td>
          <td class="value">${activity.subtitle}</td>
        </tr>
        <tr>
          <td class="label">Module</td>
          <td class="value">${activity.module}</td>
        </tr>
        <tr>
          <td class="label">Date &amp; Time</td>
          <td class="value">$dateStr</td>
        </tr>
        <tr>
          <td class="label">Account</td>
          <td class="value">$userName</td>
        </tr>
        <tr>
          <td class="label">Status</td>
          <td class="value" style="color:#16C784;">Completed</td>
        </tr>
        <tr>
          <td class="label">Reference No.</td>
          <td class="value" style="font-family:monospace;font-size:11px;">${activity.referenceId}</td>
        </tr>
      </table>
    </div>
    <div class="footer">
      <p><strong>Thank you for using PUNDAR!</strong></p>
      <p>Keep this receipt for your records.</p>
      <p class="ref">${activity.referenceId}</p>
    </div>
  </div>
  <div class="watermark">PUNDAR • Spend · Save · Grow · Together</div>
</body>
</html>
    """.trimIndent()
}
