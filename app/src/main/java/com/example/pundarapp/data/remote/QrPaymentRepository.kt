package com.example.pundarapp.data.remote

import android.util.Log
import com.example.pundarapp.data.qr.QrPayload
import com.example.pundarapp.data.qr.isExpired
import com.example.pundarapp.ui.data.BillStatus
import com.example.pundarapp.ui.data.HomeActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class QrRecipient(
    val userId: String,
    val displayName: String,
    val walletId: String?,
    val isActive: Boolean = true
)

/**
 * QR payment orchestration: validation, anti-replay, atomic wallet updates,
 * transaction logging, and notifications.
 */
object QrPaymentRepository {
    private const val TAG = "QrPaymentRepository"
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val transactionsCollection = db.collection("transactions")
    private val qrTokensCollection = db.collection("qr_tokens")

    /** Validate structure, expiry, and replay status. */
    suspend fun validatePayload(payload: QrPayload): Result<Unit> {
        if (payload.userId.isBlank()) {
            return Result.failure(Exception("Invalid QR: missing user ID."))
        }
        if (payload.isExpired()) {
            return Result.failure(Exception("This QR code has expired. Ask the sender to generate a new one."))
        }
        if (isTokenUsed(payload.securityToken)) {
            return Result.failure(Exception("This QR code has already been used."))
        }
        return Result.success(Unit)
    }

    suspend fun lookupRecipient(userId: String): Result<QrRecipient> {
        return try {
            val doc = usersCollection.document(userId).get().await()
            if (!doc.exists()) {
                return Result.failure(Exception("Recipient not found."))
            }
            val name = doc.getString("full_name") ?: "PUNDAR User"
            val walletId = doc.getString("stellarPublicKey")
            val active = doc.getBoolean("is_active") ?: true
            if (!active) {
                return Result.failure(Exception("Recipient account is inactive."))
            }
            Result.success(
                QrRecipient(
                    userId = userId,
                    displayName = name,
                    walletId = walletId,
                    isActive = active
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "lookupRecipient failed", e)
            Result.failure(e)
        }
    }

    private suspend fun isTokenUsed(token: String): Boolean {
        return try {
            val doc = qrTokensCollection.document(token).get().await()
            doc.exists() && (doc.getBoolean("used") == true)
        } catch (e: Exception) {
            Log.w(TAG, "Token check failed, allowing: ${e.message}")
            false
        }
    }

    /**
     * Transfer money from sender to recipient after PIN verification.
     * Uses a Firestore batch for wallet + transaction + token + notifications.
     */
    suspend fun transferMoney(
        senderId: String,
        senderName: String,
        recipient: QrRecipient,
        amount: Double,
        notes: String?,
        qrPayload: QrPayload?
    ): Result<String> {
        if (senderId == recipient.userId) {
            return Result.failure(Exception("You cannot send money to yourself."))
        }
        if (amount <= 0) {
            return Result.failure(Exception("Enter a valid amount."))
        }

        qrPayload?.let {
            validatePayload(it).onFailure { return Result.failure(it) }
            if (it.type == QrPayload.TYPE_BILL_PAYMENT) {
                validateBillPayment(it, amount).onFailure { return Result.failure(it) }
            }
        }

        return try {
            val senderDoc = usersCollection.document(senderId).get().await()
            val recipientDoc = usersCollection.document(recipient.userId).get().await()
            if (!senderDoc.exists() || !recipientDoc.exists()) {
                return Result.failure(Exception("Account not found."))
            }

            val senderBalance = senderDoc.getDouble("wallet_balance")
                ?: senderDoc.getDouble("walletBalance")
                ?: 0.0
            if (amount > senderBalance) {
                return Result.failure(Exception("Insufficient wallet balance."))
            }

            val recipientBalance = recipientDoc.getDouble("wallet_balance")
                ?: recipientDoc.getDouble("walletBalance")
                ?: 0.0

            val reference = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}"
            val now = System.currentTimeMillis()
            val batch = db.batch()

            batch.update(senderDoc.reference, "wallet_balance", senderBalance - amount)
            batch.update(recipientDoc.reference, "wallet_balance", recipientBalance + amount)

            val txnType = when (qrPayload?.type) {
                QrPayload.TYPE_BILL_PAYMENT -> "BILL_QR_PAYMENT"
                else -> "QR_TRANSFER"
            }

            val billQrRecord = if (qrPayload?.type == QrPayload.TYPE_BILL_PAYMENT && qrPayload.securityToken.isNotBlank()) {
                BillQrRepository.getRecord(qrPayload.securityToken)
            } else null

            val txnRef = transactionsCollection.document()
            batch.set(
                txnRef,
                hashMapOf(
                    "transaction_id" to txnRef.id,
                    "reference" to reference,
                    "type" to txnType,
                    "sender_id" to senderId,
                    "recipient_id" to recipient.userId,
                    "amount" to amount,
                    "status" to "SUCCESS",
                    "notes" to (notes ?: ""),
                    "qr_id" to (qrPayload?.securityToken ?: ""),
                    "bill_ref" to (qrPayload?.billRef ?: ""),
                    "bill_id" to (billQrRecord?.billId ?: ""),
                    "timestamp" to now,
                    "created_at" to now
                )
            )

            qrPayload?.let { payload ->
                batch.set(
                    qrTokensCollection.document(payload.securityToken),
                    hashMapOf(
                        "used" to true,
                        "used_at" to now,
                        "used_by" to senderId,
                        "type" to payload.type
                    )
                )
                if (payload.type == QrPayload.TYPE_BILL_PAYMENT && billQrRecord != null) {
                    batch.update(
                        db.collection("group_bills").document(billQrRecord.billId),
                        "status", BillStatus.SETTLED.name
                    )
                    batch.update(
                        db.collection("bill_qrs").document(payload.securityToken),
                        mapOf(
                            "status" to BillQrStatus.PAID.name,
                            "paid_at" to now,
                            "paid_by" to senderId
                        )
                    )
                }
            }

            val senderNotifRef = NotificationRepository.collection(senderId).document()
            batch.set(
                senderNotifRef,
                hashMapOf(
                    "title" to "Transfer Sent",
                    "message" to "You successfully sent ₱${String.format("%,.2f", amount)} to ${recipient.displayName}.",
                    "kind" to "SYSTEM",
                    "created_at" to now,
                    "is_read" to false
                )
            )

            val recipientNotifRef = NotificationRepository.collection(recipient.userId).document()
            batch.set(
                recipientNotifRef,
                hashMapOf(
                    "title" to "Money Received",
                    "message" to "You received ₱${String.format("%,.2f", amount)} from $senderName.",
                    "kind" to "SYSTEM",
                    "created_at" to now,
                    "is_read" to false
                )
            )

            batch.commit().await()
            Log.d(TAG, "transferMoney success ref=$reference amount=$amount")
            Result.success(reference)
        } catch (e: Exception) {
            Log.e(TAG, "transferMoney failed", e)
            Result.failure(e)
        }
    }

    private suspend fun validateBillPayment(payload: QrPayload, amount: Double): Result<Unit> {
        val expected = payload.amount
            ?: return Result.failure(Exception("Invalid bill QR: missing amount."))
        if (kotlin.math.abs(amount - expected) > 0.01) {
            return Result.failure(Exception("Amount must be ₱${String.format("%,.2f", expected)}."))
        }
        when (BillQrRepository.resolveStatus(payload.securityToken)) {
            BillQrStatus.PAID -> return Result.failure(Exception("This bill has already been paid."))
            BillQrStatus.EXPIRED -> return Result.failure(Exception("This bill QR has expired."))
            BillQrStatus.CANCELLED -> return Result.failure(Exception("This bill QR is no longer valid."))
            else -> return Result.success(Unit)
        }
    }

    fun homeActivityForSend(recipientName: String, amount: Double): HomeActivity =
        HomeActivity(
            icon = "send",
            title = "Sent to $recipientName",
            subtitle = "QR transfer",
            amount = "-₱ ${String.format("%,.2f", amount)}",
            isPositive = false,
            module = "Wallet"
        )

    fun homeActivityForBillPayment(billName: String, amount: Double): HomeActivity =
        HomeActivity(
            icon = "receipt",
            title = billName,
            subtitle = "Bill QR payment",
            amount = "-₱ ${String.format("%,.2f", amount)}",
            isPositive = false,
            module = "Pay"
        )

    fun homeActivityForReceive(senderName: String, amount: Double): HomeActivity =
        HomeActivity(
            icon = "receive",
            title = "Received from $senderName",
            subtitle = "QR transfer",
            amount = "+₱ ${String.format("%,.2f", amount)}",
            isPositive = true,
            module = "Wallet"
        )
}
