package com.example.pundarapp.data.remote

import com.example.pundarapp.ui.data.HomeActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object HomeRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getRecentActivities(userId: String): Result<List<HomeActivity>> {
        return try {
            val richUserActivities = mutableListOf<HomeActivity>()
            val phone = AuthRepository.getCurrentUserPhone().ifBlank { userId }

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
                        val refId = m["referenceId"] as? String ?: "TXN-$ts"

                        richUserActivities.add(
                            HomeActivity(
                                icon = icon,
                                title = title,
                                subtitle = subtitle,
                                amount = amountStr,
                                isPositive = isPositive,
                                module = module,
                                timestamp = ts,
                                referenceId = refId
                            )
                        )
                    }
                }
            }

            // Deduplicate rich user activities (by referenceId or identical title+amount within 10s)
            val deduplicatedList = mutableListOf<HomeActivity>()
            for (act in richUserActivities.sortedByDescending { it.timestamp }) {
                val isDuplicate = deduplicatedList.any { existing ->
                    (act.referenceId.isNotBlank() && existing.referenceId == act.referenceId) ||
                        (existing.title == act.title &&
                            existing.amount == act.amount &&
                            existing.isPositive == act.isPositive &&
                            kotlin.math.abs(existing.timestamp - act.timestamp) < 10_000L)
                }
                if (!isDuplicate) {
                    deduplicatedList.add(act)
                }
            }

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
                val refId = doc.getString("reference")?.takeIf { it.isNotBlank() } ?: doc.id

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
                        val isQr = type == "BILL_QR_PAYMENT" || type == "QR_TRANSFER" || type == "GROUP_PAY_JOIN"
                        val billRef = doc.getString("bill_ref")?.takeIf { it.isNotBlank() }
                        title = billRef ?: if (isQr) "QR Payment" else "Transfer Sent"
                        subtitle = if (type == "GROUP_PAY_JOIN") "Joined & Paid Split" else "To user"
                        amountStr = "-₱ ${String.format("%,.2f", amount)}"
                        isPositive = false
                        module = if (type == "BILL_QR_PAYMENT" || type == "GROUP_PAY_JOIN") "Pay" else "Wallet"
                    }
                    recipientId == userId -> {
                        val isQr = type == "BILL_QR_PAYMENT" || type == "QR_TRANSFER" || type == "GROUP_PAY_JOIN"
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

                // Skip if this transaction is already represented in the user's rich activity list
                val normalizedDocAmt = amountStr.replace(" ", "")
                val alreadyInRichList = deduplicatedList.any { existing ->
                    existing.referenceId == refId ||
                        (existing.amount.replace(" ", "") == normalizedDocAmt &&
                            existing.isPositive == isPositive &&
                            kotlin.math.abs(existing.timestamp - ts) < 20_000L)
                }

                if (!alreadyInRichList) {
                    deduplicatedList.add(
                        HomeActivity(
                            icon = "receipt",
                            title = title,
                            subtitle = subtitle,
                            amount = amountStr,
                            isPositive = isPositive,
                            module = module,
                            timestamp = ts,
                            referenceId = refId
                        )
                    )
                }
            }

            val sortedActivities = deduplicatedList
                .sortedByDescending { it.timestamp }
                .take(20)
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
