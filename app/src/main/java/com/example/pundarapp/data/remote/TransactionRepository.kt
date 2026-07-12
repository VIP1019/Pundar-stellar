package com.example.pundarapp.data.remote

import android.util.Log
import com.example.pundarapp.ui.data.BillStatus
import com.example.pundarapp.ui.data.HomeActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Handles money movement (settle bills, transfer, etc.) and writes a
 * transaction log entry to Firestore using batch writes where possible.
 */
object TransactionRepository {
    private const val TAG = "TransactionRepository"
    private val db = FirebaseFirestore.getInstance()
    private val transactionsCollection = db.collection("transactions")
    private val usersCollection = db.collection("users")

    suspend fun settleBills(
        userId: String,
        billIds: Set<String>,
        currentBalance: Double,
        billsById: Map<String, BillLite>
    ): Result<String> {
        return try {
            if (billIds.isEmpty()) {
                return Result.failure(Exception("No bills selected."))
            }

            val duplicates = mutableListOf<String>()
            for (id in billIds) {
                val b = billsById[id] ?: return Result.failure(Exception("Bill $id not found."))
                if (b.status == BillStatus.SETTLED) duplicates.add(id)
            }
            if (duplicates.isNotEmpty()) {
                return Result.failure(Exception("Already settled: ${duplicates.joinToString()}"))
            }

            val totalAmount = billIds.sumOf { billsById[it]?.yourShare ?: 0.0 }
            if (totalAmount > currentBalance) {
                return Result.failure(Exception("Insufficient wallet balance."))
            }

            val reference = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}"
            val now = System.currentTimeMillis()
            val batch = db.batch()

            for (id in billIds) {
                batch.update(
                    db.collection("group_bills").document(id),
                    "status", BillStatus.SETTLED.name
                )
            }

            val userRef = usersCollection.document(userId)
            batch.update(userRef, "wallet_balance", currentBalance - totalAmount)

            val txnRef = transactionsCollection.document()
            batch.set(
                txnRef,
                hashMapOf(
                    "transaction_id" to txnRef.id,
                    "reference" to reference,
                    "type" to "INSTANT_SETTLEMENT",
                    "user_id" to userId,
                    "amount" to totalAmount,
                    "bill_ids" to billIds.toList(),
                    "status" to "SUCCESS",
                    "timestamp" to now,
                    "created_at" to now
                )
            )

            val notifRef = NotificationRepository.collection(userId).document()
            batch.set(
                notifRef,
                hashMapOf(
                    "title" to "Bills Settled",
                    "message" to "You instantly settled ₱${String.format("%,.2f", totalAmount)} across ${billIds.size} bill(s).",
                    "kind" to "BILL",
                    "created_at" to now,
                    "is_read" to false
                )
            )

            batch.commit().await()
            Log.d(TAG, "settleBills: $reference settled=${billIds.size} amount=$totalAmount")
            Result.success(reference)
        } catch (e: Exception) {
            Log.e(TAG, "settleBills failed", e)
            Result.failure(e)
        }
    }

    data class BillLite(
        val id: String,
        val status: BillStatus,
        val yourShare: Double,
        val name: String
    )

    suspend fun logTransfer(
        senderId: String,
        recipientId: String,
        amount: Double,
        notes: String?
    ): Result<String> {
        return try {
            val reference = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}"
            val now = System.currentTimeMillis()
            val data = hashMapOf(
                "reference" to reference,
                "type" to "TRANSFER",
                "sender_id" to senderId,
                "recipient_id" to recipientId,
                "amount" to amount,
                "notes" to (notes ?: ""),
                "status" to "SUCCESS",
                "timestamp" to now,
                "created_at" to now
            )
            transactionsCollection.add(data).await()
            Result.success(reference)
        } catch (e: Exception) {
            Log.e(TAG, "logTransfer failed", e)
            Result.failure(e)
        }
    }

    fun homeActivityForSettlement(billName: String, amount: Double): HomeActivity =
        HomeActivity(
            icon = "receipt",
            title = billName,
            subtitle = "Instant settlement",
            amount = "-₱ ${String.format("%,.2f", amount)}",
            isPositive = false,
            module = "Pay"
        )
}
