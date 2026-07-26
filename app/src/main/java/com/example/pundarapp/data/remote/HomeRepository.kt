package com.example.pundarapp.data.remote

import com.example.pundarapp.ui.data.HomeActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object HomeRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getRecentActivities(userId: String): Result<List<HomeActivity>> {
        return try {
            val transactions = mutableListOf<Pair<Long, HomeActivity>>()

            val userTxns = db.collection("transactions").whereEqualTo("user_id", userId).get().await()
            val sentTxns = db.collection("transactions").whereEqualTo("sender_id", userId).get().await()
            val recvTxns = db.collection("transactions").whereEqualTo("recipient_id", userId).get().await()

            val allDocs = userTxns.documents + sentTxns.documents + recvTxns.documents
            val uniqueDocs = allDocs.distinctBy { it.id }

            for (doc in uniqueDocs) {
                val amount = doc.getDouble("amount") ?: 0.0
                val ts = doc.getLong("timestamp") ?: 0L
                val type = doc.getString("type") ?: ""
                val senderId = doc.getString("sender_id")
                val recipientId = doc.getString("recipient_id")

                val title: String
                val subtitle: String
                val amountStr: String
                val isPositive: Boolean
                val module: String

                when {
                    type == "INSTANT_SETTLEMENT" -> {
                        val numBills = (doc.get("bill_ids") as? List<*>)?.size ?: 1
                        title = "Bills Settled"
                        subtitle = "Paid $numBills bill(s)"
                        amountStr = "-₱ ${String.format("%,.2f", amount)}"
                        isPositive = false
                        module = "Pay"
                    }
                    senderId == userId -> {
                        val isQr = type == "BILL_QR_PAYMENT" || type == "QR_TRANSFER"
                        title = if (isQr) "QR Payment" else "Transfer Sent"
                        subtitle = "To user"
                        amountStr = "-₱ ${String.format("%,.2f", amount)}"
                        isPositive = false
                        module = "Wallet"
                    }
                    recipientId == userId -> {
                        val isQr = type == "BILL_QR_PAYMENT" || type == "QR_TRANSFER"
                        title = if (isQr) "Received QR" else "Transfer Received"
                        subtitle = "From user"
                        amountStr = "+₱ ${String.format("%,.2f", amount)}"
                        isPositive = true
                        module = "Wallet"
                    }
                    else -> {
                        title = "Transaction"
                        subtitle = "Processed"
                        amountStr = "₱ ${String.format("%,.2f", amount)}"
                        isPositive = true
                        module = "Wallet"
                    }
                }

                transactions.add(
                    Pair(ts, HomeActivity("receipt", title, subtitle, amountStr, isPositive, module))
                )
            }

            val phone = AuthRepository.getCurrentUserPhone()
            if (phone.isNotBlank()) {
                val userDoc = db.collection("users").document(phone).get().await()
                val acts = userDoc.get("homeActivities") as? List<Map<String, Any>>
                if (acts != null) {
                    for (m in acts) {
                        val icon = m["icon"] as? String ?: continue
                        val title = m["title"] as? String ?: continue
                        val subtitle = m["subtitle"] as? String ?: continue
                        val amountStr = m["amount"] as? String ?: continue
                        val isPositive = m["isPositive"] as? Boolean ?: false
                        val module = m["module"] as? String ?: continue
                        val ts = (m["timestamp"] as? Number)?.toLong() ?: 0L

                        transactions.add(
                            Pair(ts, HomeActivity(icon, title, subtitle, amountStr, isPositive, module, timestamp = ts))
                        )
                    }
                }
            }

            val sortedActivities = transactions.sortedByDescending { it.first }.map { it.second }.take(20)
            Result.success(sortedActivities)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun createActivity(userId: String, activity: HomeActivity): Result<Boolean> {
        return try {
            val data = hashMapOf(
                "user_id" to userId,
                "icon" to activity.icon,
                "title" to activity.title,
                "subtitle" to activity.subtitle,
                "amount" to activity.amount,
                "is_positive" to activity.isPositive,
                "module" to activity.module,
                "created_at" to System.currentTimeMillis().toString()
            )
            db.collection("home_activities").add(data).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
