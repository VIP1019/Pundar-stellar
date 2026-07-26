package com.example.pundarapp.data.remote

import android.util.Log
import com.example.pundarapp.ui.data.Circle
import com.example.pundarapp.ui.data.CircleInvitation
import com.example.pundarapp.ui.data.CircleJoinRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object CircleRepository {
    private const val TAG = "CircleRepository"
    const val MAX_MEMBER_LIMIT_MESSAGE = "This Paluwagan has reached its maximum member limit."

    private val db = FirebaseFirestore.getInstance()
    private val circlesCollection = db.collection("circles")
    private val invitationsCollection = db.collection("circle_invitations")

    suspend fun getCirclesForUser(userId: String): List<Circle> {
        return try {
            val snapshot = kotlinx.coroutines.withTimeout(5000) { circlesCollection.get().await() }
            val allCircles = snapshot.documents.mapNotNull { doc ->
                try {
                    val membersList = doc.get("members") as? List<Map<String, Any>> ?: emptyList()
                    val members = membersList.map { m ->
                        com.example.pundarapp.ui.data.CircleMember(
                            userId = m["userId"] as? String ?: "",
                            name = m["name"] as? String ?: "",
                            initials = m["initials"] as? String ?: "",
                            sharePercent = (m["sharePercent"] as? Number)?.toInt() ?: 0,
                            amount = (m["amount"] as? Number)?.toDouble() ?: 0.0,
                            status = try { com.example.pundarapp.ui.data.ContributionStatus.valueOf(m["status"] as? String ?: "PENDING") } catch (e: Exception) { com.example.pundarapp.ui.data.ContributionStatus.PENDING },
                            isYou = (m["userId"] as? String == userId) || (m["name"] as? String == userId) || (m["name"] as? String == AuthRepository.currentUserState.value?.name),
                            payoutDate = m["payoutDate"] as? String ?: "",
                            avatarColor = (m["avatarColor"] as? Number)?.toLong() ?: 0xFFB0BEC5
                        )
                    }.distinctBy { if (it.userId.isNotEmpty()) it.userId else it.name }
                    Circle(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        targetAmount = doc.getDouble("targetAmount") ?: 0.0,
                        savedAmount = doc.getDouble("savedAmount") ?: 0.0,
                        targetDate = doc.getString("targetDate") ?: "",
                        memberCount = members.size,
                        contributionPerMonth = doc.getDouble("contributionPerMonth") ?: 0.0,
                        members = members,
                        isActive = doc.getBoolean("isActive") ?: true,
                        currency = doc.getString("currency") ?: "XLM",
                        creatorId = doc.getString("creatorId") ?: "",
                        payoutFrequencyValue = doc.getLong("payoutFrequencyValue")?.toInt() ?: 1,
                        payoutFrequencyUnit = doc.getString("payoutFrequencyUnit") ?: "Months",
                        maxMembers = doc.getLong("maxMembers")?.toInt() ?: 10
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse circle", e)
                    null
                }
            }
            allCircles.filter { circle -> circle.members.any { it.name == userId || it.userId == userId || circle.creatorId == userId } }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get circles for user", e)
            emptyList()
        }
    }

    suspend fun getPendingInvitations(userId: String): List<CircleInvitation> {
        return try {
            val snapshot = kotlinx.coroutines.withTimeout(5000) { 
                invitationsCollection.whereEqualTo("inviteeUserId", userId).whereEqualTo("status", "PENDING").get().await() 
            }
            snapshot.documents.mapNotNull { it.toObject(CircleInvitation::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get pending invitations", e)
            emptyList()
        }
    }

    suspend fun getPendingJoinRequests(circleId: String): List<CircleJoinRequest> {
        return try {
            val snapshot = kotlinx.coroutines.withTimeout(5000) {
                db.collection("circle_join_requests").whereEqualTo("circleId", circleId).get().await()
            }
            snapshot.documents.mapNotNull { it.toObject(CircleJoinRequest::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get join requests", e)
            emptyList()
        }
    }

    suspend fun getPendingJoinRequestsForCreator(creatorId: String): List<CircleJoinRequest> {
        return try {
            // First get all circles owned by creator
            val circlesSnapshot = circlesCollection.whereEqualTo("creatorId", creatorId).get().await()
            val circleIds = circlesSnapshot.documents.map { it.id }
            if (circleIds.isEmpty()) return emptyList()
            
            // Then get all join requests for these circles
            val requests = mutableListOf<CircleJoinRequest>()
            for (cid in circleIds) {
                val reqSnapshot = db.collection("circle_join_requests").whereEqualTo("circleId", cid).get().await()
                requests.addAll(reqSnapshot.documents.mapNotNull { it.toObject(CircleJoinRequest::class.java) })
            }
            requests
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get creator join requests", e)
            emptyList()
        }
    }

    suspend fun getDiscoverableCircles(userId: String): List<Circle> {
        return try {
            val snapshot = kotlinx.coroutines.withTimeout(5000) { circlesCollection.get().await() }
            val allCircles = snapshot.documents.mapNotNull { doc ->
                try {
                    val membersList = doc.get("members") as? List<Map<String, Any>> ?: emptyList()
                    val members = membersList.map { m ->
                        com.example.pundarapp.ui.data.CircleMember(
                            userId = m["userId"] as? String ?: "",
                            name = m["name"] as? String ?: "",
                            initials = m["initials"] as? String ?: "",
                            sharePercent = (m["sharePercent"] as? Number)?.toInt() ?: 0,
                            amount = (m["amount"] as? Number)?.toDouble() ?: 0.0,
                            status = try { com.example.pundarapp.ui.data.ContributionStatus.valueOf(m["status"] as? String ?: "PENDING") } catch (e: Exception) { com.example.pundarapp.ui.data.ContributionStatus.PENDING },
                            isYou = (m["userId"] as? String == userId) || (m["name"] as? String == userId) || (m["name"] as? String == AuthRepository.currentUserState.value?.name),
                            payoutDate = m["payoutDate"] as? String ?: "",
                            avatarColor = (m["avatarColor"] as? Number)?.toLong() ?: 0xFFB0BEC5
                        )
                    }.distinctBy { if (it.userId.isNotEmpty()) it.userId else it.name }
                    Circle(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        targetAmount = doc.getDouble("targetAmount") ?: 0.0,
                        savedAmount = doc.getDouble("savedAmount") ?: 0.0,
                        targetDate = doc.getString("targetDate") ?: "",
                        memberCount = members.size,
                        contributionPerMonth = doc.getDouble("contributionPerMonth") ?: 0.0,
                        members = members,
                        isActive = doc.getBoolean("isActive") ?: true,
                        currency = doc.getString("currency") ?: "XLM",
                        creatorId = doc.getString("creatorId") ?: "",
                        payoutFrequencyValue = doc.getLong("payoutFrequencyValue")?.toInt() ?: 1,
                        payoutFrequencyUnit = doc.getString("payoutFrequencyUnit") ?: "Months",
                        maxMembers = doc.getLong("maxMembers")?.toInt() ?: 10
                    )
                } catch (e: Exception) {
                    null
                }
            }
            // Return circles where the user is NOT a member and NOT the creator
            allCircles.filter { circle -> circle.members.none { it.name == userId || it.userId == userId } && circle.creatorId != userId }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get discoverable circles", e)
            emptyList()
        }
    }

    suspend fun requestToJoinCircle(circleId: String, userId: String, userName: String): Result<Unit> {
        return try {
            val request = CircleJoinRequest(
                id = "req_${System.currentTimeMillis()}",
                userId = userId,
                userName = userName,
                circleId = circleId
            )
            db.collection("circle_join_requests").document(request.id).set(request).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCircle(circle: Circle, creatorId: String): Result<Circle> {
        return try {
            // Save as a Map to avoid any potential serialization issues with complex data classes, enums, or getters
            val circleMap = hashMapOf<String, Any>(
                "id" to circle.id,
                "name" to circle.name,
                "targetAmount" to circle.targetAmount,
                "savedAmount" to circle.savedAmount,
                "targetDate" to circle.targetDate,
                "memberCount" to circle.memberCount,
                "contributionPerMonth" to circle.contributionPerMonth,
                "isActive" to circle.isActive,
                "currency" to circle.currency,
                "creatorId" to circle.creatorId,
                "maxMembers" to circle.maxMembers,
                "payoutFrequencyValue" to circle.payoutFrequencyValue,
                "payoutFrequencyUnit" to circle.payoutFrequencyUnit,
                "members" to circle.members.map {
                    hashMapOf(
                        "userId" to it.userId,
                        "name" to it.name,
                        "initials" to it.initials,
                        "sharePercent" to it.sharePercent,
                        "amount" to it.amount,
                        "status" to it.status.name, // Enum to String
                        "isYou" to it.isYou,
                        "payoutDate" to it.payoutDate,
                        "avatarColor" to it.avatarColor
                    )
                }
            )
            
            kotlinx.coroutines.withTimeout(5000) {
                circlesCollection.document(circle.id).set(circleMap).await()
            }
            Result.success(circle)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create circle in Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun updateCircle(circle: Circle): Result<Circle> {
        return try {
            val circleMap = hashMapOf(
                "id" to circle.id,
                "name" to circle.name,
                "targetAmount" to circle.targetAmount,
                "savedAmount" to circle.savedAmount,
                "targetDate" to circle.targetDate,
                "memberCount" to circle.memberCount,
                "contributionPerMonth" to circle.contributionPerMonth,
                "isActive" to circle.isActive,
                "currency" to circle.currency,
                "creatorId" to circle.creatorId,
                "maxMembers" to circle.maxMembers,
                "payoutFrequencyValue" to circle.payoutFrequencyValue,
                "payoutFrequencyUnit" to circle.payoutFrequencyUnit,
                "members" to circle.members.map {
                    hashMapOf(
                        "userId" to it.userId,
                        "name" to it.name,
                        "initials" to it.initials,
                        "sharePercent" to it.sharePercent,
                        "amount" to it.amount,
                        "status" to it.status.name, // Enum to String
                        "isYou" to it.isYou,
                        "payoutDate" to it.payoutDate,
                        "avatarColor" to it.avatarColor
                    )
                }
            )
            
            kotlinx.coroutines.withTimeout(5000) {
                circlesCollection.document(circle.id).set(circleMap).await()
            }
            Result.success(circle)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update circle in Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun sendInvitation(
        circleId: String,
        inviteeUserId: String,
        inviteeName: String,
        inviteeInitials: String,
        inviterName: String,
        inviterScore: Int
    ): Result<Unit> {
        return try {
            val invitation = CircleInvitation(
                id = "inv_${System.currentTimeMillis()}",
                circleId = circleId,
                circleName = "Paluwagan Circle",
                goal = "Saving together",
                inviterName = inviterName,
                inviterScore = inviterScore,
                inviterCirclesCompleted = 0,
                targetAmount = 0.0,
                fundedPercent = 0,
                monthlyContribution = 0.0,
                memberCount = 1,
                maxMembers = 10,
                // These are new fields we'll add or use if present, using a map to bypass strict constructor if we want, but better to update CircleInvitation
            )
            // Save as a map to easily include extra fields without modifying Data Class heavily everywhere
            val data = mapOf(
                "id" to invitation.id,
                "circleId" to circleId,
                "circleName" to "Paluwagan Circle",
                "inviterName" to inviterName,
                "inviteeUserId" to inviteeUserId,
                "inviteeName" to inviteeName,
                "status" to "PENDING"
            )
            kotlinx.coroutines.withTimeout(5000) {
                invitationsCollection.document(invitation.id).set(data).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send invitation", e)
            Result.failure(e)
        }
    }

    suspend fun updateInvitationStatus(invitationId: String, status: String): Result<Unit> {
        return try {
            kotlinx.coroutines.withTimeout(5000) {
                invitationsCollection.document(invitationId).update("status", status).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update invitation status", e)
            Result.failure(e)
        }
    }

    suspend fun joinCircle(
        circleId: String,
        userId: String,
        userName: String,
        userInitials: String,
        monthlyContribution: Double
    ): Result<Circle> {
        return try {
            val docRef = circlesCollection.document(circleId)
            val snapshot = kotlinx.coroutines.withTimeout(5000) { docRef.get().await() }
            val circle = snapshot.toObject(Circle::class.java) ?: return Result.failure(Exception("Circle not found"))
            
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")
            val today = java.time.LocalDate.now()
            val freqVal = circle.payoutFrequencyValue.toLong()
            val multiplier = (circle.members.size + 1).toLong()
            val totalOffset = freqVal * multiplier
            val payoutDate = when (circle.payoutFrequencyUnit.lowercase()) {
                "days" -> today.plusDays(totalOffset)
                "weeks" -> today.plusWeeks(totalOffset)
                "months" -> today.plusMonths(totalOffset)
                else -> today.plusMonths(totalOffset)
            }
            
            if (circle.members.any { it.userId == userId || it.name == userName }) {
                return Result.success(circle)
            }
            
            val newMember = com.example.pundarapp.ui.data.CircleMember(
                userId = userId,
                name = userName,
                initials = userInitials,
                sharePercent = 0,
                amount = 0.0,
                status = com.example.pundarapp.ui.data.ContributionStatus.PENDING,
                isYou = false,
                payoutDate = payoutDate.format(formatter),
                avatarColor = 0xFF6B7280
            )
            val updatedMembers = circle.members + newMember
            val updatedCircle = circle.copy(members = updatedMembers, memberCount = updatedMembers.size)
            
            // Use updateCircle to ensure serialization matches
            val updateResult = updateCircle(updatedCircle)
            if (updateResult.isSuccess) {
                Result.success(updatedCircle)
            } else {
                Result.failure(Exception("Failed to save circle"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join circle", e)
            Result.failure(e)
        }
    }


    suspend fun approveJoinRequest(requestId: String, approverId: String): Result<Circle> {
        return try {
            val requestDoc = db.collection("circle_join_requests").document(requestId).get().await()
            val userId = requestDoc.getString("userId") ?: throw java.lang.Exception("User not found")
            val userName = requestDoc.getString("userName") ?: throw java.lang.Exception("User not found")
            val circleId = requestDoc.getString("circleId") ?: throw java.lang.Exception("Circle not found")
            
            // Delete request
            db.collection("circle_join_requests").document(requestId).delete().await()
            
            // Add user to circle
            joinCircle(circleId, userId, userName, userName.take(2).uppercase(), 0.0)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to approve join request", e)
            Result.failure(e)
        }
    }

    suspend fun rejectJoinRequest(requestId: String, approverId: String): Result<Unit> {
        return try {
            db.collection("circle_join_requests").document(requestId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshInvitation(invitation: CircleInvitation): CircleInvitation {
        return invitation
    }
}
