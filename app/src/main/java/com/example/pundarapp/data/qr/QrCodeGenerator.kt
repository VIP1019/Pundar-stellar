package com.example.pundarapp.data.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.UUID

/**
 * Builds PUNDAR QR payloads and renders them to bitmaps via ZXing.
 */
object QrCodeGenerator {

    /** Create a new "RECEIVE_MONEY" payload for the given user. */
    fun createReceiveMoneyPayload(
        userId: String,
        displayName: String?,
        username: String?,
        walletId: String?
    ): QrPayload = QrPayload(
        type = QrPayload.TYPE_RECEIVE_MONEY,
        version = 1,
        userId = userId,
        username = username,
        displayName = displayName,
        walletId = walletId,
        timestamp = System.currentTimeMillis(),
        securityToken = UUID.randomUUID().toString()
    )

    /** Create a new "BILL_PAYMENT" payload for a pending bill. */
    fun createBillPaymentPayload(
        userId: String,
        amount: Double,
        billRef: String,
        transactionId: String
    ): QrPayload = QrPayload(
        type = QrPayload.TYPE_BILL_PAYMENT,
        version = 1,
        userId = userId,
        amount = amount,
        billRef = billRef,
        transactionId = transactionId,
        timestamp = System.currentTimeMillis(),
        securityToken = UUID.randomUUID().toString()
    )

    /** Render a payload string to a square QR bitmap. */
    fun generateBitmap(content: String, sizePx: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    fun generateBitmap(payload: QrPayload, sizePx: Int = 512): Bitmap =
        generateBitmap(QrPayload.encode(payload), sizePx)
}
