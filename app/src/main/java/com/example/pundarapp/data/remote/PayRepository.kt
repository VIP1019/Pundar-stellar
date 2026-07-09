package com.example.pundarapp.data.remote

import com.example.pundarapp.ui.data.BillStatus
import com.example.pundarapp.ui.data.GroupBill
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object PayRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getBills(): List<GroupBill> {
        return try {
            val snapshot = db.collection("group_bills").get().await()
            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: return@mapNotNull null
                val name = doc.getString("name") ?: return@mapNotNull null
                val totalAmount = doc.getDouble("total_amount") ?: return@mapNotNull null
                val memberCount = doc.getLong("member_count")?.toInt() ?: return@mapNotNull null
                val statusStr = doc.getString("status") ?: return@mapNotNull null
                val date = doc.getString("date") ?: return@mapNotNull null
                val yourShare = doc.getDouble("your_share") ?: return@mapNotNull null
                
                GroupBill(
                    id = id,
                    name = name,
                    totalAmount = totalAmount,
                    memberCount = memberCount,
                    status = BillStatus.valueOf(statusStr),
                    date = date,
                    yourShare = yourShare,
                    members = emptyList() // Fetching members separately could be added later
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun createBill(bill: GroupBill): Boolean {
        return try {
            val billData = hashMapOf(
                "id" to bill.id,
                "name" to bill.name,
                "total_amount" to bill.totalAmount,
                "member_count" to bill.memberCount,
                "status" to bill.status.name,
                "date" to bill.date,
                "your_share" to bill.yourShare
            )
            db.collection("group_bills").document(bill.id).set(billData).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
