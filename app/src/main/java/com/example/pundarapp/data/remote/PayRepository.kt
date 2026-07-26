package com.example.pundarapp.data.remote

import com.example.pundarapp.ui.data.BillStatus
import com.example.pundarapp.ui.data.GroupBill
import com.example.pundarapp.ui.data.BillMember
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object PayRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getBills(): List<GroupBill> {
        return try {
            val phone = AuthRepository.getCurrentUserPhone()
            if (phone.isBlank()) return emptyList()
            
            // Query where the user is a member or the creator
            val snapshot = db.collection("group_bills")
                .whereArrayContains("memberUsernames", phone)
                .get().await()
                
            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: return@mapNotNull null
                val name = doc.getString("name") ?: return@mapNotNull null
                val totalAmount = doc.getDouble("total_amount") ?: return@mapNotNull null
                val memberCount = doc.getLong("member_count")?.toInt() ?: return@mapNotNull null
                val statusStr = doc.getString("status") ?: return@mapNotNull null
                val date = doc.getString("date") ?: return@mapNotNull null
                
                val membersList = doc.get("members") as? List<Map<String, Any>> ?: emptyList()
                val members = membersList.map { m ->
                    BillMember(
                        name = m["name"] as? String ?: "",
                        username = m["username"] as? String ?: "",
                        initials = m["initials"] as? String ?: "",
                        amount = (m["amount"] as? Number)?.toDouble() ?: 0.0,
                        avatarColor = (m["avatarColor"] as? Number)?.toLong() ?: 0xFF6B7280
                    )
                }
                
                // Calculate your share based on if you are the creator or a member
                // If you're the creator, you might have set a specific "your_share" in createBill
                val creatorId = doc.getString("creatorId") ?: ""
                var yourShare = 0.0
                if (creatorId == phone) {
                     yourShare = doc.getDouble("your_share") ?: 0.0
                } else {
                     val myMember = members.find { it.username == phone }
                     if (myMember != null) {
                         yourShare = myMember.amount
                     }
                }
                
                GroupBill(
                    id = id,
                    name = name,
                    totalAmount = totalAmount,
                    memberCount = memberCount,
                    status = BillStatus.valueOf(statusStr),
                    date = date,
                    yourShare = yourShare,
                    members = members
                )
            }.sortedByDescending { it.date }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun createBill(bill: GroupBill): Boolean {
        return try {
            val currentPhone = AuthRepository.getCurrentUserPhone()
            val currentName = AuthRepository.currentUserState.value?.name ?: "You"
            
            // Initially, only the creator is an active member who can see the bill
            val memberUsernames = mutableListOf(currentPhone)
            
            val membersData = bill.members.map { m ->
                hashMapOf(
                    "name" to m.name,
                    "username" to m.username,
                    "initials" to m.initials,
                    "amount" to m.amount,
                    "avatarColor" to m.avatarColor
                )
            }
            
            val billData = hashMapOf(
                "id" to bill.id,
                "name" to bill.name,
                "total_amount" to bill.totalAmount,
                "member_count" to bill.memberCount,
                "status" to bill.status.name,
                "date" to bill.date,
                "your_share" to bill.yourShare,
                "creatorId" to currentPhone,
                "memberUsernames" to memberUsernames,
                "members" to membersData
            )
            db.collection("group_bills").document(bill.id).set(billData).await()
            
            // Send invitations to other users who have a phone number
            bill.members.forEach { m ->
                if (m.username.isNotBlank() && m.username != currentPhone) {
                    val inv = com.example.pundarapp.ui.data.BillInvitation(
                        id = "bill_inv_${System.currentTimeMillis()}_${m.username}",
                        billId = bill.id,
                        billName = bill.name,
                        inviterName = currentName,
                        inviterPhone = currentPhone,
                        inviteePhone = m.username,
                        yourShare = m.amount,
                        totalAmount = bill.totalAmount
                    )
                    db.collection("bill_invitations").document(inv.id).set(inv).await()
                }
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getBillInvitations(phone: String): List<com.example.pundarapp.ui.data.BillInvitation> {
        return try {
            val snapshot = db.collection("bill_invitations").whereEqualTo("inviteePhone", phone).get().await()
            snapshot.documents.mapNotNull { it.toObject(com.example.pundarapp.ui.data.BillInvitation::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun acceptBillInvitation(invitationId: String, billId: String, phone: String): Boolean {
        return try {
            db.collection("group_bills").document(billId)
                .update("memberUsernames", com.google.firebase.firestore.FieldValue.arrayUnion(phone))
                .await()
            db.collection("bill_invitations").document(invitationId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun rejectBillInvitation(invitationId: String): Boolean {
        return try {
            db.collection("bill_invitations").document(invitationId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
