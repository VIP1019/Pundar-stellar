package com.example.pundarapp.data.qr

import com.example.pundarapp.data.remote.QrPaymentRepository

/**
 * Shared QR scan result after decode + server validation.
 */
sealed class QrScanResult {
    data class Success(val payload: QrPayload) : QrScanResult()
    data class Error(val message: String) : QrScanResult()
}

object QrScanHandler {

    const val NO_QR_FOUND = "No valid QR code detected in the selected image."
    const val INVALID_QR = "This QR code is invalid or unsupported."
    const val EXPIRED_QR = "This QR code has expired."

    /** Decode a raw scanned string and run server-side validation. */
    suspend fun processRawQr(raw: String): QrScanResult {
        val payload = QrPayload.decode(raw)
        if (payload == null) {
            return QrScanResult.Error(INVALID_QR)
        }
        if (payload.isExpired()) {
            return QrScanResult.Error(EXPIRED_QR)
        }
        val validation = QrPaymentRepository.validatePayload(payload)
        return validation.fold(
            onSuccess = { QrScanResult.Success(payload) },
            onFailure = { e ->
                val msg = e.message.orEmpty()
                QrScanResult.Error(
                    when {
                        msg.contains("expired", ignoreCase = true) -> EXPIRED_QR
                        msg.contains("already been used", ignoreCase = true) -> msg
                        else -> if (msg.isNotBlank()) msg else INVALID_QR
                    }
                )
            }
        )
    }
}
