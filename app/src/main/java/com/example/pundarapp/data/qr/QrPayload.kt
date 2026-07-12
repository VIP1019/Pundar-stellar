package com.example.pundarapp.data.qr

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * PUNDAR QR code payload.
 *
 * The QR contains only safe, non-sensitive fields. Passwords, PINs, and auth
 * tokens are intentionally NEVER encoded. The securityToken is a one-time
 * random nonce used to detect replays; server-side enforcement of the token
 * is a follow-up (see // TODO(security)).
 */
@Serializable
data class QrPayload(
    val type: String,            // "RECEIVE_MONEY" | "BILL_PAYMENT"
    val version: Int = 1,
    val userId: String,          // phone number — public identifier
    val username: String? = null,  // optional display
    val displayName: String? = null,
    val walletId: String? = null,  // Stellar public key (safe to share)
    val amount: Double? = null,    // bill-payment only
    val billRef: String? = null,   // bill-payment only
    val transactionId: String? = null, // bill-payment only
    val timestamp: Long,         // epoch ms when QR was generated
    val securityToken: String    // random UUID — anti-replay
) {
    companion object {
        const val TYPE_RECEIVE_MONEY = "RECEIVE_MONEY"
        const val TYPE_BILL_PAYMENT  = "BILL_PAYMENT"

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        /** Encode to the string that gets put into the QR. */
        fun encode(payload: QrPayload): String = json.encodeToString(serializer(), payload)

        /** Decode a scanned string. Returns null if it isn't a PUNDAR QR. */
        fun decode(scanned: String): QrPayload? = try {
            json.decodeFromString(serializer(), scanned)
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Returns true if the QR is older than [ttlMs] (default 5 minutes).
 * Caller should reject expired QRs.
 */
fun QrPayload.isExpired(ttlMs: Long = 5L * 60L * 1000L): Boolean {
    val now = System.currentTimeMillis()
    return now - timestamp > ttlMs
}
