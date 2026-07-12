package com.example.pundarapp.data.remote

import android.util.Log
import com.example.pundarapp.data.qr.QrPayload
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

enum class BillQrStatus { ACTIVE, EXPIRED, PAID, CANCELLED }

data class BillQrRecord(
    val securityToken: String,
    val billId: String,
    val creatorId: String,
    val amount: Double,
    val billRef: String,
    val status: BillQrStatus,
    val createdAt: Long,
    val expiresAt: Long,
    val paidAt: Long? = null,
    val paidBy: String? = null
)

object BillQrRepository {
    private const val TAG = "BillQrRepository"
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("bill_qrs")

    suspend fun save(payload: QrPayload, billId: String, creatorId: String, ttlMs: Long = 5L * 60L * 1000L): Result<Unit> {
        return try {
            val expiresAt = payload.timestamp + ttlMs
            collection.document(payload.securityToken).set(
                hashMapOf(
                    "security_token" to payload.securityToken,
                    "bill_id" to billId,
                    "creator_id" to creatorId,
                    "amount" to (payload.amount ?: 0.0),
                    "bill_ref" to (payload.billRef ?: billId),
                    "transaction_id" to (payload.transactionId ?: billId),
                    "status" to BillQrStatus.ACTIVE.name,
                    "created_at" to payload.timestamp,
                    "expires_at" to expiresAt,
                    "payload_json" to QrPayload.encode(payload)
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "save failed", e)
            Result.failure(e)
        }
    }

    suspend fun getRecord(securityToken: String): BillQrRecord? {
        return try {
            val doc = collection.document(securityToken).get().await()
            if (!doc.exists()) return null
            val statusName = doc.getString("status") ?: BillQrStatus.ACTIVE.name
            BillQrRecord(
                securityToken = securityToken,
                billId = doc.getString("bill_id") ?: "",
                creatorId = doc.getString("creator_id") ?: "",
                amount = doc.getDouble("amount") ?: 0.0,
                billRef = doc.getString("bill_ref") ?: "",
                status = BillQrStatus.valueOf(statusName),
                createdAt = doc.getLong("created_at") ?: 0L,
                expiresAt = doc.getLong("expires_at") ?: 0L,
                paidAt = doc.getLong("paid_at"),
                paidBy = doc.getString("paid_by")
            )
        } catch (e: Exception) {
            Log.e(TAG, "getRecord failed", e)
            null
        }
    }

    suspend fun resolveStatus(securityToken: String): BillQrStatus {
        val record = getRecord(securityToken) ?: return BillQrStatus.ACTIVE
        if (record.status == BillQrStatus.PAID) return BillQrStatus.PAID
        if (System.currentTimeMillis() > record.expiresAt) return BillQrStatus.EXPIRED
        return record.status
    }

    suspend fun markPaid(securityToken: String, payerId: String): Result<Unit> {
        return try {
            collection.document(securityToken).update(
                mapOf(
                    "status" to BillQrStatus.PAID.name,
                    "paid_at" to System.currentTimeMillis(),
                    "paid_by" to payerId
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "markPaid failed", e)
            Result.failure(e)
        }
    }
}
