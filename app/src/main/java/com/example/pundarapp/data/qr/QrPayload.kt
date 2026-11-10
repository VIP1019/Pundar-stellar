package com.example.pundarapp.data.qr

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * PUNDAR QR code payload.
 *
 * The QR contains only safe, non-sensitive fields. Passwords, PINs, and auth
 * tokens are intentionally NEVER encoded. The securityToken is a one-time
 * random nonce used to detect replays.
 */
@Serializable
data class QrPayload(
    val type: String,            // "RECEIVE_MONEY" | "BILL_PAYMENT" | "STELLAR_ADDRESS"
    val version: Int = 1,
    val userId: String,          // phone number or Stellar public key — public identifier
    val username: String? = null,  // optional display
    val displayName: String? = null,
    val walletId: String? = null,  // Stellar public key (safe to share)
    val amount: Double? = null,    // optional requested amount / bill-payment amount
    val billRef: String? = null,   // bill-payment / memo
    val transactionId: String? = null, // bill-payment only
    val timestamp: Long,         // epoch ms when QR was generated
    val securityToken: String    // random UUID — anti-replay
) {
    companion object {
        const val TYPE_RECEIVE_MONEY = "RECEIVE_MONEY"
        const val TYPE_BILL_PAYMENT  = "BILL_PAYMENT"
        const val TYPE_STELLAR_ADDRESS = "STELLAR_ADDRESS"

        private val STELLAR_KEY_REGEX = Regex("^G[A-Z2-7]{55}$")

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        /** Encode to the string that gets put into the QR. */
        fun encode(payload: QrPayload): String = json.encodeToString(serializer(), payload)

        /**
         * Decode a scanned string. Supports:
         * 1. PUNDAR JSON QR payloads
         * 2. SEP-0007 Stellar URIs (web+stellar:pay?destination=G...&amount=...&memo=...)
         * 3. Raw Stellar public keys (G...)
         * Returns null if it isn't a supported QR format.
         */
        fun decode(scanned: String): QrPayload? {
            val trimmed = scanned.trim()
            if (trimmed.isBlank()) return null

            // 1. Try PUNDAR JSON format
            try {
                return json.decodeFromString(serializer(), trimmed)
            } catch (_: Exception) {
                // Fall through to Stellar URI / raw key checks
            }

            // 2. Try SEP-0007 Stellar URI: web+stellar:pay?destination=G...
            if (trimmed.startsWith("web+stellar:", ignoreCase = true)) {
                return try {
                    val queryPart = trimmed.substringAfter('?', "")
                    if (queryPart.isBlank()) return null
                    val params = queryPart.split('&')
                        .mapNotNull { pair ->
                            val idx = pair.indexOf('=')
                            if (idx <= 0) null
                            else {
                                val k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name())
                                val v = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name())
                                k to v
                            }
                        }
                        .toMap()
                    val destination = params["destination"]?.trim().orEmpty()
                    if (!STELLAR_KEY_REGEX.matches(destination)) return null
                    val amount = params["amount"]?.toDoubleOrNull()
                    val memo = params["memo"]?.takeIf { it.isNotBlank() }
                    QrPayload(
                        type = TYPE_STELLAR_ADDRESS,
                        userId = destination,
                        displayName = "Stellar Wallet",
                        walletId = destination,
                        amount = amount,
                        billRef = memo,
                        timestamp = System.currentTimeMillis(),
                        securityToken = destination
                    )
                } catch (_: Exception) {
                    null
                }
            }

            // 3. Try raw Stellar public key (56-char G... address)
            if (STELLAR_KEY_REGEX.matches(trimmed)) {
                return QrPayload(
                    type = TYPE_STELLAR_ADDRESS,
                    userId = trimmed,
                    displayName = "Stellar Wallet",
                    walletId = trimmed,
                    timestamp = System.currentTimeMillis(),
                    securityToken = trimmed
                )
            }

            return null
        }
    }
}

/**
 * Returns true if the QR is older than [ttlMs] (default 5 minutes).
 * Caller should reject expired QRs.
 */
fun QrPayload.isExpired(ttlMs: Long = 5L * 60L * 1000L): Boolean {
    // Receive money and raw Stellar address QR codes are static and do not expire
    if (this.type == QrPayload.TYPE_RECEIVE_MONEY || this.type == QrPayload.TYPE_STELLAR_ADDRESS) {
        return false
    }

    val now = System.currentTimeMillis()
    return now - timestamp > ttlMs
}
