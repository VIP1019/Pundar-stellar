package com.example.pundarapp.data.remote

import android.util.Log
import com.example.pundarapp.ui.data.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object CircleRepository {
    private const val TAG = "CircleRepository"
    private val db = FirebaseFirestore.getInstance()
    private val circlesCollection = db.collection("circles")
    private val joinRequestsCollection = db.collection("circle_join_requests")

    const val MAX_MEMBER_LIMIT_MESSAGE =
        "This Circle has already reached its maximum member limit."

    // ── Read ────────────────────────────────────────────────────────

    suspend fun getCirclesForUser(userId: String): List<Circle> {
        if (userId.isBlank()) return emptyList()
        return try {
            val snapshot = circlesCollection
                .whereArrayContains("member_ids", userId)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc -> doc.toCircle() }
        } catch (e: Exception) {
            Log.e(TAG, "getCirclesForUser failed", e)
            emptyList()
        }
    }

    suspend fun getCircle(circleId: String): Circle? {
        return try {
            val doc = circlesCollection.document(circleId).get().await()
            if (!doc.exists()) null else doc.toCircle()
        } catch (e: Exception) {
            Log.e(TAG, "getCircle failed", e)
            null
        }
    }

    suspend fun getPendingJoinRequests(circleId: String): List<CircleJoinRequest> {
        return try {
            val snapshot = joinRequestsCollection
                .whereEqualTo("circle_id", circleId)
                .whereEqualTo("status", JoinRequestStatus.PENDING.name)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toJoinRequest() }
        } catch (e: Exception) {
            Log.e(TAG, "getPendingJoinRequests failed", e)
            emptyList()
        }
    }

    suspend fun getPendingInvitation(userId: String): CircleInvitation? {
        return try {
            val snapshot = joinRequestsCollection
                .whereEqualTo("user_id", userId)
                .whereEqualTo("status", JoinRequestStatus.PENDING.name)
                .whereEqualTo("is_invitation", true)
                .limit(1)
                .get()
                .await()
            val doc = snapshot.documents.firstOrNull() ?: return null
            val circleId = doc.getString("circle_id") ?: return null
            val circle = getCircle(circleId) ?: return null
            doc.toInvitation(circle)
        } catch (e: Exception) {
            Log.e(TAG, "getPendingInvitation failed", e)
            null
        }
    }

    // ── Create ──────────────────────────────────────────────────────

    suspend fun createCircle(circle: Circle, creatorId: String): Result<Circle> {
        if (circle.members.size > circle.maxMembers) {
            return Result.failure(
                Exception("Cannot create circle: member count exceeds maximum of ${circle.maxMembers}.")
            )
        }
        return try {
            val memberIds = circle.members.map { it.userId.ifBlank { creatorId } }.distinct()
            val circleData = hashMapOf(
                "id" to circle.id,
                "name" to circle.name,
                "target_amount" to circle.targetAmount,
                "saved_amount" to circle.savedAmount,
                "target_date" to circle.targetDate,
                "member_count" to circle.members.size,
                "max_members" to circle.maxMembers,
                "contribution_per_month" to circle.contributionPerMonth,
                "creator_id" to creatorId,
                "is_active" to circle.isActive,
                "member_ids" to memberIds,
                "created_at" to System.currentTimeMillis()
            )
            db.runTransaction { tx ->
                val circleRef = circlesCollection.document(circle.id)
                tx.set(circleRef, circleData)
                circle.members.forEach { member ->
                    val uid = member.userId.ifBlank { creatorId }
                    val memberRef = circleRef.collection("members").document(uid)
                    tx.set(memberRef, member.toFirestoreMap(uid))
                }
            }.await()
            Result.success(circle)
        } catch (e: Exception) {
            Log.e(TAG, "createCircle failed", e)
            Result.failure(e)
        }
    }

    // ── Join (direct — invitation accept) ───────────────────────────

    suspend fun joinCircle(
        circleId: String,
        userId: String,
        userName: String,
        userInitials: String,
        monthlyContribution: Double
    ): Result<Circle> {
        return try {
            db.runTransaction { tx ->
                val circleRef = circlesCollection.document(circleId)
                val circleSnap = tx.get(circleRef)
                if (!circleSnap.exists()) {
                    throw Exception("Circle not found.")
                }
                val maxMembers = circleSnap.getLong("max_members")?.toInt()
                    ?: throw Exception("Invalid circle configuration.")
                val memberCount = circleSnap.getLong("member_count")?.toInt() ?: 0
                if (memberCount >= maxMembers) {
                    throw Exception(MAX_MEMBER_LIMIT_MESSAGE)
                }
                val memberRef = circleRef.collection("members").document(userId)
                if (tx.get(memberRef).exists()) {
                    throw Exception("You are already a member of this circle.")
                }
                val sharePercent = (100.0 / (memberCount + 1)).toInt()
                val memberData = hashMapOf(
                    "user_id" to userId,
                    "name" to userName,
                    "initials" to userInitials,
                    "share_percent" to sharePercent,
                    "amount" to monthlyContribution,
                    "status" to ContributionStatus.PENDING.name,
                    "is_you" to false,
                    "avatar_color" to 0xFF0052CC,
                    "joined_at" to System.currentTimeMillis()
                )
                tx.set(memberRef, memberData)
                val memberIds = (circleSnap.get("member_ids") as? List<*>)?.map { it.toString() }?.toMutableList()
                    ?: mutableListOf()
                if (!memberIds.contains(userId)) memberIds.add(userId)
                tx.update(
                    circleRef,
                    mapOf(
                        "member_count" to memberCount + 1,
                        "member_ids" to memberIds
                    )
                )
                // Mark invitation / join request as approved
                val inviteRef = joinRequestsCollection.document("invite_${circleId}_${userId}")
                val reqRef = joinRequestsCollection.document("req_${circleId}_${userId}")
                if (tx.get(inviteRef).exists()) {
                    tx.update(inviteRef, mapOf("status" to JoinRequestStatus.APPROVED.name))
                }
                if (tx.get(reqRef).exists()) {
                    tx.update(reqRef, mapOf("status" to JoinRequestStatus.APPROVED.name))
                }
                circleSnap
            }.await()

            getCircle(circleId)?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to load circle after join."))
        } catch (e: Exception) {
            Log.e(TAG, "joinCircle failed", e)
            Result.failure(e)
        }
    }

    // ── Join request (request → approve flow) ───────────────────────

    suspend fun submitJoinRequest(
        circleId: String,
        userId: String,
        userName: String,
        userInitials: String
    ): Result<Unit> {
        return try {
            val circle = getCircle(circleId)
                ?: return Result.failure(Exception("Circle not found."))
            if (circle.isFull) {
                return Result.failure(Exception(MAX_MEMBER_LIMIT_MESSAGE))
            }
            if (circle.members.any { it.userId == userId }) {
                return Result.failure(Exception("You are already a member of this circle."))
            }
            val requestId = "req_${circleId}_${userId}"
            val data = hashMapOf(
                "id" to requestId,
                "circle_id" to circleId,
                "circle_name" to circle.name,
                "user_id" to userId,
                "user_name" to userName,
                "user_initials" to userInitials,
                "status" to JoinRequestStatus.PENDING.name,
                "is_invitation" to false,
                "created_at" to System.currentTimeMillis()
            )
            joinRequestsCollection.document(requestId).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "submitJoinRequest failed", e)
            Result.failure(e)
        }
    }

    suspend fun approveJoinRequest(
        requestId: String,
        approverId: String
    ): Result<Circle> {
        return try {
            val requestDoc = joinRequestsCollection.document(requestId).get().await()
            if (!requestDoc.exists()) {
                return Result.failure(Exception("Join request not found."))
            }
            val status = requestDoc.getString("status")
            if (status != JoinRequestStatus.PENDING.name) {
                return Result.failure(Exception("This request has already been processed."))
            }
            val circleId = requestDoc.getString("circle_id")
                ?: return Result.failure(Exception("Invalid request."))
            val userId = requestDoc.getString("user_id")
                ?: return Result.failure(Exception("Invalid request."))
            val userName = requestDoc.getString("user_name") ?: "Member"
            val userInitials = requestDoc.getString("user_initials") ?: "M"

            val circleRef = circlesCollection.document(circleId)
            db.runTransaction { tx ->
                val circleSnap = tx.get(circleRef)
                if (!circleSnap.exists()) throw Exception("Circle not found.")
                val creatorId = circleSnap.getString("creator_id") ?: ""
                if (creatorId != approverId) {
                    throw Exception("Only the circle owner can approve join requests.")
                }
                val maxMembers = circleSnap.getLong("max_members")?.toInt()
                    ?: throw Exception("Invalid circle configuration.")
                val memberCount = circleSnap.getLong("member_count")?.toInt() ?: 0
                if (memberCount >= maxMembers) {
                    throw Exception(MAX_MEMBER_LIMIT_MESSAGE)
                }
                val memberRef = circleRef.collection("members").document(userId)
                if (tx.get(memberRef).exists()) {
                    throw Exception("User is already a member.")
                }
                val monthlyContribution = circleSnap.getDouble("contribution_per_month") ?: 0.0
                val sharePercent = (100.0 / (memberCount + 1)).toInt()
                tx.set(
                    memberRef,
                    hashMapOf(
                        "user_id" to userId,
                        "name" to userName,
                        "initials" to userInitials,
                        "share_percent" to sharePercent,
                        "amount" to monthlyContribution,
                        "status" to ContributionStatus.PENDING.name,
                        "is_you" to false,
                        "avatar_color" to 0xFF6B7280,
                        "joined_at" to System.currentTimeMillis()
                    )
                )
                val memberIds = (circleSnap.get("member_ids") as? List<*>)?.map { it.toString() }?.toMutableList()
                    ?: mutableListOf()
                if (!memberIds.contains(userId)) memberIds.add(userId)
                tx.update(
                    circleRef,
                    mapOf("member_count" to memberCount + 1, "member_ids" to memberIds)
                )
                val reqRef = joinRequestsCollection.document(requestId)
                tx.update(reqRef, mapOf("status" to JoinRequestStatus.APPROVED.name))
            }.await()

            getCircle(circleId)?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to load circle after approval."))
        } catch (e: Exception) {
            Log.e(TAG, "approveJoinRequest failed", e)
            Result.failure(e)
        }
    }

    suspend fun rejectJoinRequest(requestId: String, approverId: String): Result<Unit> {
        return try {
            val requestDoc = joinRequestsCollection.document(requestId).get().await()
            if (!requestDoc.exists()) return Result.failure(Exception("Join request not found."))
            val circleId = requestDoc.getString("circle_id") ?: return Result.failure(Exception("Invalid request."))
            val circle = getCircle(circleId) ?: return Result.failure(Exception("Circle not found."))
            if (circle.creatorId != approverId) {
                return Result.failure(Exception("Only the circle owner can reject join requests."))
            }
            joinRequestsCollection.document(requestId)
                .update("status", JoinRequestStatus.REJECTED.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "rejectJoinRequest failed", e)
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
            val circle = getCircle(circleId)
                ?: return Result.failure(Exception("Circle not found."))
            if (circle.isFull) {
                return Result.failure(Exception(MAX_MEMBER_LIMIT_MESSAGE))
            }
            val requestId = "invite_${circleId}_${inviteeUserId}"
            val data = hashMapOf(
                "id" to requestId,
                "circle_id" to circleId,
                "circle_name" to circle.name,
                "user_id" to inviteeUserId,
                "user_name" to inviteeName,
                "user_initials" to inviteeInitials,
                "inviter_name" to inviterName,
                "inviter_score" to inviterScore,
                "target_amount" to circle.targetAmount,
                "funded_percent" to ((circle.savedAmount / circle.targetAmount.coerceAtLeast(1.0)) * 100).toInt(),
                "monthly_contribution" to circle.contributionPerMonth,
                "member_count" to circle.memberCount,
                "max_members" to circle.maxMembers,
                "status" to JoinRequestStatus.PENDING.name,
                "is_invitation" to true,
                "created_at" to System.currentTimeMillis()
            )
            joinRequestsCollection.document(requestId).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "sendInvitation failed", e)
            Result.failure(e)
        }
    }

    suspend fun refreshInvitation(invitation: CircleInvitation): CircleInvitation {
        val circle = getCircle(invitation.circleId) ?: return invitation
        return invitation.copy(
            memberCount = circle.memberCount,
            maxMembers = circle.maxMembers
        )
    }

    // ── Mappers ─────────────────────────────────────────────────────

    private suspend fun com.google.firebase.firestore.DocumentSnapshot.toCircle(): Circle? {
        val id = getString("id") ?: return null
        val name = getString("name") ?: return null
        val membersSnap = reference.collection("members").get().await()
        val currentUserId = AuthRepository.getCurrentUserId()
        val members = membersSnap.documents.mapNotNull { it.toCircleMember(currentUserId) }
        return Circle(
            id = id,
            name = name,
            targetAmount = getDouble("target_amount") ?: 0.0,
            savedAmount = getDouble("saved_amount") ?: 0.0,
            targetDate = getString("target_date") ?: "TBD",
            memberCount = getLong("member_count")?.toInt() ?: members.size,
            maxMembers = getLong("max_members")?.toInt() ?: 10,
            contributionPerMonth = getDouble("contribution_per_month") ?: 0.0,
            members = members,
            creatorId = getString("creator_id") ?: "",
            isActive = getBoolean("is_active") ?: true
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toCircleMember(
        currentUserId: String?
    ): CircleMember? {
        val name = getString("name") ?: return null
        val userId = getString("user_id") ?: ""
        return CircleMember(
            userId = userId,
            name = name,
            initials = getString("initials") ?: name.take(2).uppercase(),
            sharePercent = getLong("share_percent")?.toInt() ?: 0,
            amount = getDouble("amount") ?: 0.0,
            status = try {
                ContributionStatus.valueOf(getString("status") ?: ContributionStatus.PENDING.name)
            } catch (_: Exception) {
                ContributionStatus.PENDING
            },
            isYou = userId == currentUserId,
            avatarColor = getLong("avatar_color") ?: 0xFF6B7280
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toJoinRequest(): CircleJoinRequest? {
        val id = getString("id") ?: id
        val circleId = getString("circle_id") ?: return null
        return CircleJoinRequest(
            id = id,
            circleId = circleId,
            circleName = getString("circle_name") ?: "",
            userId = getString("user_id") ?: "",
            userName = getString("user_name") ?: "",
            userInitials = getString("user_initials") ?: "",
            status = try {
                JoinRequestStatus.valueOf(getString("status") ?: JoinRequestStatus.PENDING.name)
            } catch (_: Exception) {
                JoinRequestStatus.PENDING
            },
            createdAt = getLong("created_at") ?: 0L
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toInvitation(circle: Circle): CircleInvitation {
        return CircleInvitation(
            id = getString("id") ?: id,
            circleId = getString("circle_id") ?: circle.id,
            circleName = getString("circle_name") ?: circle.name,
            goal = "Save together for a shared goal",
            inviterName = getString("inviter_name") ?: "Organizer",
            inviterScore = getLong("inviter_score")?.toInt() ?: 850,
            inviterCirclesCompleted = 3,
            targetAmount = getDouble("target_amount") ?: circle.targetAmount,
            fundedPercent = getLong("funded_percent")?.toInt()
                ?: ((circle.savedAmount / circle.targetAmount.coerceAtLeast(1.0)) * 100).toInt(),
            monthlyContribution = getDouble("monthly_contribution") ?: circle.contributionPerMonth,
            memberCount = getLong("member_count")?.toInt() ?: circle.memberCount,
            maxMembers = getLong("max_members")?.toInt() ?: circle.maxMembers
        )
    }

    private fun CircleMember.toFirestoreMap(userId: String) = hashMapOf(
        "user_id" to userId,
        "name" to name,
        "initials" to initials,
        "share_percent" to sharePercent,
        "amount" to amount,
        "status" to status.name,
        "is_you" to isYou,
        "avatar_color" to avatarColor,
        "joined_at" to System.currentTimeMillis()
    )
}
