package com.example.pundarapp.data.qr

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * PUNDAR QR code payload.
 *
 * The QR contains only safe, non-sensitive fields. Passwords, PINs, and auth
 * tokens are intentionally NEVER encoded. The securityToken is a one-time
 * random nonce used to detect replays.
 */
@Serializable
data class QrPayload(
    val type: String,            // "RECEIVE_MONEY" | "BILL_PAYMENT" | "STELLAR_ADDRESS" | "GROUP_BILL_JOIN"
    val version: Int = 1,
    val userId: String,          // phone number or Stellar public key — public identifier
    val username: String? = null,  // optional display
    val displayName: String? = null,
    val walletId: String? = null,  // Stellar public key (safe to share)
    val amount: Double? = null,    // fixed per-person requested amount / bill-payment amount
    val totalBillAmount: Double? = null, // total bill amount before splitting
    val participantCount: Int? = null,   // quantity of people joining the bill
    val billRef: String? = null,   // bill-payment / memo / group bill name
    val transactionId: String? = null, // bill-payment only
    val timestamp: Long = System.currentTimeMillis(), // epoch ms when QR was generated
    val securityToken: String = UUID.randomUUID().toString() // random UUID — anti-replay
) {
    companion object {
        const val TYPE_RECEIVE_MONEY = "RECEIVE_MONEY"
        const val TYPE_BILL_PAYMENT  = "BILL_PAYMENT"
        const val TYPE_STELLAR_ADDRESS = "STELLAR_ADDRESS"
        const val TYPE_GROUP_BILL_JOIN = "GROUP_BILL_JOIN"

        private val STELLAR_KEY_REGEX = Regex("^G[A-Z2-7]{55}$")
        private val PHONE_REGEX = Regex("^(?:\\+?63|0)?9\\d{9}$")
        private val FEDERATED_REGEX = Regex("^[^@*\\s]+(?:\\*|@)[^@*\\s]+\\.[a-zA-Z]{2,}$")

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        /** Encode to the string that gets put into the QR. */
        fun encode(payload: QrPayload): String = json.encodeToString(serializer(), payload)

        /**
         * Decode any scanned QR string. Works in all ways:
         * 1. PUNDAR JSON QR payloads
         * 2. SEP-0007 Stellar URIs (web+stellar:pay?destination=G...&amount=...&memo=...)
         * 3. Raw Stellar public keys (G...)
         * 4. Deep links / URLs (pundar://..., https://...)
         * 5. Group bill join refs (bill_..., group_..., BILL-...)
         * 6. Phone numbers (09..., +639...)
         * 7. Federated addresses (user*domain)
         * 8. Universal non-blank fallback
         */
        fun decode(scanned: String): QrPayload? {
            val trimmed = scanned.trim()
            if (trimmed.isBlank()) return null

            // 1. Try PUNDAR JSON format
            try {
                return json.decodeFromString(serializer(), trimmed)
            } catch (_: Exception) {
                // Fall through to next parsers
            }

            // 2. Try SEP-0007 / Stellar URI: web+stellar:pay?destination=G... or stellar:pay?...
            if (trimmed.startsWith("web+stellar:", ignoreCase = true) || trimmed.startsWith("stellar:", ignoreCase = true)) {
                try {
                    val queryPart = trimmed.substringAfter('?', "")
                    val params = parseQueryParams(queryPart)
                    val destination = params["destination"] ?: params["to"] ?: ""
                    val amount = params["amount"]?.toDoubleOrNull()
                    val totalBillAmount = params["totalAmount"]?.toDoubleOrNull()
                    val participantCount = params["participants"]?.toIntOrNull()
                    val memo = params["memo"] ?: params["msg"] ?: params["billRef"]
                    val targetId = if (STELLAR_KEY_REGEX.matches(destination)) destination else destination.ifBlank { trimmed }
                    return QrPayload(
                        type = if (STELLAR_KEY_REGEX.matches(targetId)) TYPE_STELLAR_ADDRESS else TYPE_RECEIVE_MONEY,
                        userId = targetId,
                        displayName = if (STELLAR_KEY_REGEX.matches(targetId)) "Stellar Wallet" else "Payment Target",
                        walletId = if (STELLAR_KEY_REGEX.matches(targetId)) targetId else null,
                        amount = amount,
                        totalBillAmount = totalBillAmount,
                        participantCount = participantCount,
                        billRef = memo,
                        timestamp = System.currentTimeMillis(),
                        securityToken = targetId
                    )
                } catch (_: Exception) {
                    // Fall through
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

            // 4. Try PUNDAR / Web Deep Link (e.g. pundar://pay, pundar://join, https://pundar.app/...)
            if (trimmed.startsWith("pundar://", ignoreCase = true) ||
                trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)) {
                try {
                    val queryPart = trimmed.substringAfter('?', "")
                    val params = parseQueryParams(queryPart)
                    val to = params["to"] ?: params["user"] ?: params["recipient"] ?: params["phone"] ?: ""
                    val billId = params["billId"] ?: params["bill"] ?: params["id"]
                    val billRef = params["billRef"] ?: params["title"] ?: params["name"] ?: billId
                    val amount = params["amount"]?.toDoubleOrNull()
                    val typeParam = params["type"]?.uppercase()

                    val isGroupJoin = typeParam == "JOIN" ||
                            typeParam == TYPE_GROUP_BILL_JOIN ||
                            trimmed.contains("/join", ignoreCase = true) ||
                            trimmed.contains("/bill", ignoreCase = true) ||
                            !billId.isNullOrBlank()

                    val effectiveType = when {
                        isGroupJoin -> TYPE_GROUP_BILL_JOIN
                        typeParam == TYPE_BILL_PAYMENT -> TYPE_BILL_PAYMENT
                        STELLAR_KEY_REGEX.matches(to) -> TYPE_STELLAR_ADDRESS
                        else -> TYPE_RECEIVE_MONEY
                    }

                    val targetUser = to.ifBlank { billId ?: trimmed }
                    return QrPayload(
                        type = effectiveType,
                        userId = targetUser,
                        displayName = params["displayName"] ?: params["name"] ?: if (isGroupJoin) "Group Bill" else "PUNDAR User",
                        walletId = if (STELLAR_KEY_REGEX.matches(targetUser)) targetUser else null,
                        amount = amount,
                        billRef = billRef,
                        transactionId = billId,
                        timestamp = System.currentTimeMillis(),
                        securityToken = billId ?: targetUser
                    )
                } catch (_: Exception) {
                    // Fall through
                }
            }

            // 5. Try Group bill join ref (e.g. "BILL-1234", "bill_1234", "group_1234")
            if (trimmed.startsWith("BILL-", ignoreCase = true) ||
                trimmed.startsWith("bill_", ignoreCase = true) ||
                trimmed.startsWith("group_", ignoreCase = true) ||
                trimmed.startsWith("join:", ignoreCase = true)) {
                val cleanRef = trimmed.substringAfter(':').trim()
                return QrPayload(
                    type = TYPE_GROUP_BILL_JOIN,
                    userId = cleanRef,
                    displayName = "Group Bill ($cleanRef)",
                    billRef = cleanRef,
                    transactionId = cleanRef,
                    timestamp = System.currentTimeMillis(),
                    securityToken = cleanRef
                )
            }

            // 6. Try Phone number (e.g. 09171234567, +639171234567)
            if (PHONE_REGEX.matches(trimmed)) {
                val normalizedPhone = when {
                    trimmed.startsWith("+63") -> "0" + trimmed.removePrefix("+63")
                    trimmed.startsWith("63") -> "0" + trimmed.removePrefix("63")
                    else -> trimmed
                }
                return QrPayload(
                    type = TYPE_RECEIVE_MONEY,
                    userId = normalizedPhone,
                    displayName = "PUNDAR User ($normalizedPhone)",
                    timestamp = System.currentTimeMillis(),
                    securityToken = normalizedPhone
                )
            }

            // 7. Try Federated address (user*domain.com)
            if (FEDERATED_REGEX.matches(trimmed)) {
                return QrPayload(
                    type = TYPE_STELLAR_ADDRESS,
                    userId = trimmed,
                    displayName = trimmed,
                    timestamp = System.currentTimeMillis(),
                    securityToken = trimmed
                )
            }

            // 8. Universal fallback: never reject any non-blank scanned text
            return QrPayload(
                type = TYPE_RECEIVE_MONEY,
                userId = trimmed,
                displayName = "Recipient (${trimmed.take(16)})",
                timestamp = System.currentTimeMillis(),
                securityToken = trimmed
            )
        }

        private fun parseQueryParams(queryPart: String): Map<String, String> {
            if (queryPart.isBlank()) return emptyMap()
            return queryPart.split('&')
                .mapNotNull { pair ->
                    val idx = pair.indexOf('=')
                    if (idx <= 0) null
                    else {
                        try {
                            val k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name())
                            val v = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name())
                            k to v
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
                .toMap()
        }
    }
}

/**
 * Returns true if the QR is older than [ttlMs] (default 24 hours).
 * Receive money, Stellar addresses, and group bill join QR codes are static and do not expire.
 */
fun QrPayload.isExpired(ttlMs: Long = 24L * 60L * 60L * 1000L): Boolean {
    if (this.type == QrPayload.TYPE_RECEIVE_MONEY ||
        this.type == QrPayload.TYPE_STELLAR_ADDRESS ||
        this.type == QrPayload.TYPE_GROUP_BILL_JOIN) {
        return false
    }

    val now = System.currentTimeMillis()
    return now - timestamp > ttlMs
}
