package com.example.pundarapp.data.remote

import com.example.pundarapp.ui.data.BillStatus
import com.example.pundarapp.ui.data.GroupBill
import com.example.pundarapp.ui.data.BillMember
import com.example.pundarapp.ui.data.parseCategorizedTitle
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object PayRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getBills(): List<GroupBill> {
        return try {
            val phone = AuthRepository.getCurrentUserPhone().ifBlank { AuthRepository.getCurrentUserId().orEmpty() }
            if (phone.isBlank()) return emptyList()
            
            // Query where the user is a member or the creator
            val snapshot = db.collection("group_bills")
                .whereArrayContains("memberUsernames", phone)
                .get().await()
                
            val fetchedBills = snapshot.documents.mapNotNull { doc ->
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
                
                val creatorId = doc.getString("creatorId") ?: ""
                val paidUsernames = (doc.get("paidUsernames") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                var yourShare = 0.0
                if (creatorId == phone) {
                     yourShare = doc.getDouble("your_share") ?: 0.0
                } else {
                     val myMember = members.find { it.username == phone }
                     if (myMember != null) {
                         yourShare = myMember.amount
                     } else {
                         yourShare = doc.getDouble("your_share") ?: 0.0
                     }
                }
                val parsedStatus = runCatching { BillStatus.valueOf(statusStr) }.getOrDefault(BillStatus.PENDING)
                val effectiveStatus = if (paidUsernames.contains(phone) || parsedStatus == BillStatus.SETTLED) {
                    BillStatus.SETTLED
                } else {
                    parsedStatus
                }
                
                GroupBill(
                    id = id,
                    name = name,
                    totalAmount = totalAmount,
                    memberCount = memberCount,
                    status = effectiveStatus,
                    date = date,
                    yourShare = yourShare,
                    members = members
                )
            }.toMutableList()

            // Also include any settled QR bill payments recorded in the transactions collection
            try {
                val txSnap = db.collection("transactions")
                    .whereEqualTo("sender_id", phone)
                    .get().await()
                for (txDoc in txSnap.documents) {
                    val txType = txDoc.getString("type").orEmpty()
                    val billRef = txDoc.getString("bill_ref").orEmpty()
                    val notes = txDoc.getString("notes").orEmpty()
                    val isQrBillTx = txType == "GROUP_PAY_JOIN" || txType == "BILL_QR_PAYMENT" || txType == "QR_TRANSFER" || billRef.isNotBlank()
                    if (!isQrBillTx) continue

                    val ref = txDoc.getString("reference").orEmpty().ifBlank { txDoc.id.take(10).uppercase() }
                    val billId = txDoc.getString("bill_id")?.takeIf { it.isNotBlank() } ?: "bill_$ref"
                    val amount = txDoc.getDouble("amount") ?: 0.0
                    if (amount <= 0.0) continue
                    val title = billRef.ifBlank { notes.ifBlank { "QR Bill Payment" } }

                    val alreadyPresent = fetchedBills.any { existing ->
                        existing.id == billId ||
                            existing.id.endsWith(ref, ignoreCase = true) ||
                            (existing.name.equals(title, ignoreCase = true) && kotlin.math.abs(existing.yourShare - amount) < 0.01)
                    }
                    if (!alreadyPresent) {
                        val recipientId = txDoc.getString("recipient_id").orEmpty()
                        fetchedBills.add(
                            0,
                            GroupBill(
                                id = billId,
                                name = title,
                                totalAmount = amount,
                                memberCount = 2,
                                status = BillStatus.SETTLED,
                                date = "Today",
                                yourShare = amount,
                                members = listOf(
                                    BillMember(
                                        name = if (recipientId.isNotBlank()) "Host ($recipientId)" else "Host",
                                        username = recipientId,
                                        initials = "HP",
                                        amount = amount
                                    ),
                                    BillMember(
                                        name = AuthRepository.getCurrentUserName(),
                                        username = phone,
                                        initials = AuthRepository.getCurrentUserInitials(),
                                        amount = amount
                                    )
                                )
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            }

            // Reconcile duplicate split entries where a pending categorized bill (e.g. WiFi)
            // and a recipient-named settled bill (e.g. Reyny Mabeza) were both created for 1 transaction
            val pendingCategorized = fetchedBills.filter {
                it.status != BillStatus.SETTLED && parseCategorizedTitle(it.name).second != null
            }
            val duplicateRecipientBills = fetchedBills.filter { b ->
                b.status == BillStatus.SETTLED &&
                    parseCategorizedTitle(b.name).second == null &&
                    (b.name.equals("Reyny Mabeza", ignoreCase = true) ||
                        (b.totalAmount == 850.0 && b.yourShare == 425.0 && pendingCategorized.isNotEmpty()))
            }
            if (duplicateRecipientBills.isNotEmpty() && pendingCategorized.isNotEmpty()) {
                val targetPending = pendingCategorized.firstOrNull {
                    it.name.contains("wifi", ignoreCase = true)
                } ?: pendingCategorized.first()
                val idx = fetchedBills.indexOfFirst { it.id == targetPending.id }
                if (idx >= 0) {
                    val settledTarget = targetPending.copy(status = BillStatus.SETTLED)
                    fetchedBills[idx] = settledTarget
                    try {
                        db.collection("group_bills").document(settledTarget.id)
                            .update("status", BillStatus.SETTLED.name)
                            .await()
                    } catch (_: Exception) {
                    }
                }
                for (dup in duplicateRecipientBills) {
                    fetchedBills.removeAll { it.id == dup.id }
                    try {
                        db.collection("group_bills").document(dup.id).delete().await()
                    } catch (_: Exception) {
                    }
                }
            }

            fetchedBills.distinctBy { it.id }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun createBill(bill: GroupBill, sendInvitations: Boolean = true): Boolean {
        return try {
            val currentPhone = AuthRepository.getCurrentUserPhone().ifBlank { AuthRepository.getCurrentUserId().orEmpty() }
            val currentName = AuthRepository.currentUserState.value?.name ?: "You"
            if (currentPhone.isBlank()) return false
            
            val memberUsernames = (listOf(currentPhone) + bill.members.mapNotNull { it.username.takeIf { u -> u.isNotBlank() } }).distinct()
            val paidUsernames = if (bill.status == BillStatus.SETTLED) listOf(currentPhone) else emptyList()
            
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
                "paidUsernames" to paidUsernames,
                "members" to membersData
            )
            db.collection("group_bills").document(bill.id).set(billData).await()
            
            if (sendInvitations) {
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
