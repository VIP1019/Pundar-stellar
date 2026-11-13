package com.example.pundarapp.data.qr

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Builds PUNDAR QR payloads, renders them to bitmaps via ZXing,
 * and handles saving to gallery and sharing via FileProvider.
 */
object QrCodeGenerator {

    /** Create a new "RECEIVE_MONEY" or "GROUP_BILL_JOIN" payload for the given user. */
    fun createReceiveMoneyPayload(
        userId: String,
        displayName: String?,
        username: String?,
        walletId: String?,
        amount: Double? = null,
        totalBillAmount: Double? = null,
        participantCount: Int? = null,
        billRef: String? = null
    ): QrPayload = QrPayload(
        type = if ((participantCount ?: 1) > 1) QrPayload.TYPE_GROUP_BILL_JOIN else QrPayload.TYPE_RECEIVE_MONEY,
        version = 1,
        userId = userId,
        username = username,
        displayName = displayName,
        walletId = walletId,
        amount = amount,
        totalBillAmount = totalBillAmount,
        participantCount = participantCount,
        billRef = billRef,
        timestamp = System.currentTimeMillis(),
        securityToken = userId // Static token for personal receive money QR
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
            EncodeHintType.MARGIN to 2
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

    /** Save a QR bitmap as a PNG in the device's Pictures/PUNDAR gallery folder. */
    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "PUNDAR_QR_${System.currentTimeMillis()}.png"
    ): Result<Uri> {
        return try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PUNDAR")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return Result.failure(Exception("Failed to create MediaStore entry."))

            resolver.openOutputStream(uri)?.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    return Result.failure(Exception("Failed to compress QR image."))
                }
            } ?: return Result.failure(Exception("Unable to open output stream."))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Share a QR bitmap image + caption using FileProvider. */
    fun shareQrBitmap(
        context: Context,
        bitmap: Bitmap,
        shareText: String,
        chooserTitle: String = "Share QR Code"
    ) {
        try {
            val shareDir = File(context.cacheDir, "qr_shares").apply { mkdirs() }
            val qrFile = File(shareDir, "pundar_qr_${System.currentTimeMillis()}.png")
            FileOutputStream(qrFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                qrFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(fallback, chooserTitle))
        }
    }
}
