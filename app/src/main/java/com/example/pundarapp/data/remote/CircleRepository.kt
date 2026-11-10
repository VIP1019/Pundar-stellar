package com.example.pundarapp.data.remote

import android.util.Log
import com.example.pundarapp.data.stellar.StellarWalletManager
import com.example.pundarapp.ui.data.Circle
import com.example.pundarapp.ui.data.CircleInvitation
import com.example.pundarapp.ui.data.CircleJoinRequest
import com.example.pundarapp.ui.data.CircleMember
import com.example.pundarapp.ui.data.CircleTransactionRecord
import com.example.pundarapp.ui.data.ContributionStatus
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object CircleRepository {
    private const val TAG = "CircleRepository"
    const val MAX_MEMBER_LIMIT_MESSAGE = "This Paluwagan has reached its maximum member limit."

    private val db = FirebaseFirestore.getInstance()
    private val circlesCollection = db.collection("circles")
    private val invitationsCollection = db.collection("circle_invitations")

    @Suppress("UNCHECKED_CAST")
    private fun parseCircleDoc(doc: DocumentSnapshot, currentUserId: String): Circle? {
        return try {
            if (!doc.exists()) return null
            val currentUserName = AuthRepository.currentUserState.value?.name ?: ""
            val membersList = doc.get("members") as? List<Map<String, Any>> ?: emptyList()
            val members = membersList.mapIndexed { idx, m ->
                val mUserId = m["userId"] as? String ?: ""
                val mName = m["name"] as? String ?: ""
                CircleMember(
                    userId = mUserId,
                    name = mName,
                    initials = m["initials"] as? String ?: mName.take(2).uppercase(),
                    sharePercent = (m["sharePercent"] as? Number)?.toInt() ?: 0,
                    amount = (m["amount"] as? Number)?.toDouble() ?: 0.0,
                    status = try {
                        ContributionStatus.valueOf(m["status"] as? String ?: "PENDING")
                    } catch (_: Exception) {
                        ContributionStatus.PENDING
                    },
                    isYou = (mUserId.isNotBlank() && mUserId == currentUserId) ||
                        (mName.isNotBlank() && (mName == currentUserId || mName == currentUserName)),
                    payoutDate = m["payoutDate"] as? String ?: "",
                    avatarColor = (m["avatarColor"] as? Number)?.toLong() ?: 0xFF2563EB,
                    stellarPublicKey = m["stellarPublicKey"] as? String ?: "",
                    payoutRound = (m["payoutRound"] as? Number)?.toInt() ?: (idx + 1),
                    acceptanceStatus = m["acceptanceStatus"] as? String ?: "ACCEPTED",
                    acceptedTermsVersion = (m["acceptedTermsVersion"] as? Number)?.toInt() ?: 1,
                    contributionTxHash = m["contributionTxHash"] as? String ?: "",
                    hasReceivedPayout = m["hasReceivedPayout"] as? Boolean ?: false,
                    payoutTxHash = m["payoutTxHash"] as? String ?: ""
                )
            }
                .distinctBy { if (it.userId.isNotEmpty()) it.userId else it.name }
                .sortedBy { it.payoutRound }

            val txList = doc.get("transactionHistory") as? List<Map<String, Any>> ?: emptyList()
            val history = txList.map { t ->
                CircleTransactionRecord(
                    id = t["id"] as? String ?: "ctx_${System.currentTimeMillis()}",
                    type = t["type"] as? String ?: "CONTRIBUTION",
                    title = t["title"] as? String ?: "",
                    memberName = t["memberName"] as? String ?: "",
                    memberPhone = t["memberPhone"] as? String ?: "",
                    roundNumber = (t["roundNumber"] as? Number)?.toInt() ?: 1,
                    amount = (t["amount"] as? Number)?.toDouble() ?: 0.0,
                    currency = t["currency"] as? String ?: "XLM",
                    status = t["status"] as? String ?: "CONFIRMED",
                    txHash = t["txHash"] as? String ?: "",
                    timestamp = (t["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }.sortedByDescending { it.timestamp }

            val paidRoundsRaw = doc.get("paidRounds") as? List<Number> ?: emptyList()
            val paidRounds = paidRoundsRaw.map { it.toInt() }

            val contributionPerMonth = doc.getDouble("contributionPerMonth") ?: 0.0
            val derivedPayout = contributionPerMonth * members.size.coerceAtLeast(1)
            val lifecycleStatus = doc.getString("lifecycleStatus")
                ?: if (doc.getBoolean("isActive") == false) "COMPLETED" else "ACTIVE"

            Circle(
                id = doc.getString("id") ?: doc.id,
                name = doc.getString("name") ?: "",
                targetAmount = if (derivedPayout > 0.0) derivedPayout else (doc.getDouble("targetAmount") ?: 0.0),
                savedAmount = doc.getDouble("savedAmount") ?: 0.0,
                targetDate = doc.getString("targetDate") ?: "",
                memberCount = members.size,
                contributionPerMonth = contributionPerMonth,
                members = members,
                isActive = lifecycleStatus == "ACTIVE",
                currency = doc.getString("currency") ?: "XLM",
                escrowAddress = doc.getString("escrowAddress") ?: StellarWalletManager.CIRCLE_CONTRACT_ID,
                escrowNetwork = doc.getString("escrowNetwork") ?: "Stellar Testnet · Soroban",
                creatorId = doc.getString("creatorId") ?: "",
                creatorName = doc.getString("creatorName") ?: "",
                payoutFrequencyValue = doc.getLong("payoutFrequencyValue")?.toInt() ?: 1,
                payoutFrequencyUnit = doc.getString("payoutFrequencyUnit") ?: "Months",
                maxMembers = doc.getLong("maxMembers")?.toInt() ?: members.size.coerceAtLeast(2),
                currentRecipientIndex = doc.getLong("currentRecipientIndex")?.toInt() ?: 0,
                payoutOrderType = doc.getString("payoutOrderType") ?: "Deterministic Fixed Rotation",
                lifecycleStatus = lifecycleStatus,
                termsVersion = doc.getLong("termsVersion")?.toInt() ?: 1,
                currentRound = doc.getLong("currentRound")?.toInt() ?: 1,
                sorobanCircleId = doc.getLong("sorobanCircleId") ?: 0L,
                activationTxHash = doc.getString("activationTxHash") ?: "",
                paidRounds = paidRounds,
                transactionHistory = history
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse circle doc ${doc.id}", e)
            null
        }
    }

    private fun serializeCircle(circle: Circle): HashMap<String, Any> {
        val derivedTarget = circle.contributionPerMonth * circle.members.size.coerceAtLeast(1)
        return hashMapOf(
            "id" to circle.id,
            "name" to circle.name,
            "targetAmount" to derivedTarget,
            "savedAmount" to circle.savedAmount,
            "targetDate" to circle.targetDate,
            "memberCount" to circle.members.size,
            "contributionPerMonth" to circle.contributionPerMonth,
            "isActive" to (circle.lifecycleStatus == "ACTIVE"),
            "currency" to circle.currency,
            "escrowAddress" to circle.escrowAddress,
            "escrowNetwork" to circle.escrowNetwork,
            "creatorId" to circle.creatorId,
            "creatorName" to circle.creatorName,
            "maxMembers" to circle.members.size,
            "payoutFrequencyValue" to circle.payoutFrequencyValue,
            "payoutFrequencyUnit" to circle.payoutFrequencyUnit,
            "currentRecipientIndex" to circle.currentRecipientIndex,
            "payoutOrderType" to circle.payoutOrderType,
            "lifecycleStatus" to circle.lifecycleStatus,
            "termsVersion" to circle.termsVersion,
            "currentRound" to circle.currentRound,
            "sorobanCircleId" to circle.sorobanCircleId,
            "activationTxHash" to circle.activationTxHash,
            "paidRounds" to circle.paidRounds,
            "members" to circle.members.sortedBy { it.payoutRound }.mapIndexed { idx, m ->
                hashMapOf(
                    "userId" to m.userId,
                    "name" to m.name,
                    "initials" to m.initials,
                    "sharePercent" to m.sharePercent,
                    "amount" to m.amount,
                    "status" to m.status.name,
                    "isYou" to m.isYou,
                    "payoutDate" to m.payoutDate,
                    "avatarColor" to m.avatarColor,
                    "stellarPublicKey" to m.stellarPublicKey,
                    "payoutRound" to (if (m.payoutRound > 0) m.payoutRound else idx + 1),
                    "acceptanceStatus" to m.acceptanceStatus,
                    "acceptedTermsVersion" to m.acceptedTermsVersion,
                    "contributionTxHash" to m.contributionTxHash,
                    "hasReceivedPayout" to m.hasReceivedPayout,
                    "payoutTxHash" to m.payoutTxHash
                )
            },
            "transactionHistory" to circle.transactionHistory.map { t ->
                hashMapOf(
                    "id" to t.id,
                    "type" to t.type,
                    "title" to t.title,
                    "memberName" to t.memberName,
                    "memberPhone" to t.memberPhone,
                    "roundNumber" to t.roundNumber,
                    "amount" to t.amount,
                    "currency" to t.currency,
                    "status" to t.status,
                    "txHash" to t.txHash,
                    "timestamp" to t.timestamp
                )
            }
        )
    }

    suspend fun getCircleById(circleId: String, currentUserId: String): Circle? {
        return try {
            val doc = kotlinx.coroutines.withTimeout(6000) {
                circlesCollection.document(circleId).get().await()
            }
            parseCircleDoc(doc, currentUserId)
        } catch (e: Exception) {
            Log.e(TAG, "getCircleById failed for $circleId", e)
            null
        }
    }

    suspend fun getCirclesForUser(userId: String): List<Circle> {
        return try {
            val snapshot = kotlinx.coroutines.withTimeout(6000) { circlesCollection.get().await() }
            val allCircles = snapshot.documents.mapNotNull { doc -> parseCircleDoc(doc, userId) }
            val currentUserName = AuthRepository.currentUserState.value?.name ?: ""
            allCircles.filter { circle ->
                circle.creatorId == userId ||
                    circle.members.any {
                        it.userId == userId || it.name == userId ||
                            (currentUserName.isNotBlank() && it.name.equals(currentUserName, ignoreCase = true))
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get circles for user", e)
            emptyList()
        }
    }

    suspend fun getPendingInvitations(userId: String): List<CircleInvitation> {
        return try {
            val snapshot = kotlinx.coroutines.withTimeout(5000) {
                invitationsCollection
                    .whereEqualTo("inviteeUserId", userId)
                    .whereEqualTo("status", "PENDING")
                    .get()
                    .await()
            }
            snapshot.documents.mapNotNull { doc ->
                val cid = doc.getString("circleId") ?: return@mapNotNull null
                CircleInvitation(
                    id = doc.getString("id") ?: doc.id,
                    circleId = cid,
                    circleName = doc.getString("circleName") ?: "PUNDAR Circle",
                    goal = doc.getString("goal") ?: "Deterministic Rotating Savings",
                    inviterName = doc.getString("inviterName") ?: "Organizer",
                    inviterScore = (doc.getLong("inviterScore") ?: 800L).toInt(),
                    inviterCirclesCompleted = (doc.getLong("inviterCirclesCompleted") ?: 1L).toInt(),
                    targetAmount = doc.getDouble("targetAmount") ?: 0.0,
                    fundedPercent = 0,
                    monthlyContribution = doc.getDouble("monthlyContribution") ?: 0.0,
                    memberCount = (doc.getLong("memberCount") ?: 1L).toInt(),
                    maxMembers = (doc.getLong("maxMembers") ?: 1L).toInt(),
                    inviterInitials = (doc.getString("inviterName") ?: "OR").take(2).uppercase(),
                    termsVersion = (doc.getLong("termsVersion") ?: 1L).toInt(),
                    scheduleText = doc.getString("scheduleText") ?: "Every 1 Months",
                    currency = doc.getString("currency") ?: "XLM"
                )
            }
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
            val circlesSnapshot = circlesCollection.whereEqualTo("creatorId", creatorId).get().await()
            val circleIds = circlesSnapshot.documents.map { it.id }
            if (circleIds.isEmpty()) return emptyList()

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
            val allCircles = snapshot.documents.mapNotNull { doc -> parseCircleDoc(doc, userId) }
            allCircles.filter { circle ->
                circle.members.none { it.name == userId || it.userId == userId } && circle.creatorId != userId
            }
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
            val circleMap = serializeCircle(circle)
            kotlinx.coroutines.withTimeout(6000) {
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
            val circleMap = serializeCircle(circle)
            kotlinx.coroutines.withTimeout(6000) {
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
        inviterScore: Int,
        circleName: String = "PUNDAR Circle",
        contributionAmount: Double = 0.0,
        memberCount: Int = 2,
        termsVersion: Int = 1,
        scheduleText: String = "Every 1 Months",
        currency: String = "XLM"
    ): Result<Unit> {
        return try {
            val invId = "inv_${circleId}_${inviteeUserId}_v$termsVersion"
            val scheduledPayout = contributionAmount * memberCount
            val data = mapOf(
                "id" to invId,
                "circleId" to circleId,
                "circleName" to circleName,
                "goal" to "Scheduled Payout: ${String.format("%,.2f", scheduledPayout)} $currency ($memberCount members)",
                "inviterName" to inviterName,
                "inviterScore" to inviterScore,
                "inviteeUserId" to inviteeUserId,
                "inviteeName" to inviteeName,
                "inviteeInitials" to inviteeInitials,
                "monthlyContribution" to contributionAmount,
                "targetAmount" to scheduledPayout,
                "memberCount" to memberCount,
                "maxMembers" to memberCount,
                "termsVersion" to termsVersion,
                "scheduleText" to scheduleText,
                "currency" to currency,
                "status" to "PENDING"
            )
            kotlinx.coroutines.withTimeout(5000) {
                invitationsCollection.document(invId).set(data).await()
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

    /**
     * Records a member's explicit ACCEPT or DECLINE for a specific [termsVersion] of a Circle proposal.
     * - If accepted and all final members have now accepted the same [termsVersion], transitions
     *   `lifecycleStatus` to `"READY_TO_ACTIVATE"`.
     * - If declined, transitions `lifecycleStatus` to `"DECLINED_REVISION_NEEDED"` and keeps Circle inactive.
     */
    suspend fun respondToCircleTerms(
        circleId: String,
        userId: String,
        accept: Boolean,
        termsVersion: Int
    ): Result<Circle> {
        return try {
            val currentCircle = getCircleById(circleId, userId)
                ?: return Result.failure(Exception("Circle proposal not found."))

            if (currentCircle.lifecycleStatus == "ACTIVE" || currentCircle.lifecycleStatus == "COMPLETED") {
                return Result.failure(Exception("Circle is already locked and active."))
            }
            if (currentCircle.termsVersion != termsVersion) {
                return Result.failure(
                    Exception("Circle terms were revised to v${currentCircle.termsVersion}. Please review the latest terms.")
                )
            }

            val newStatus = if (accept) "ACCEPTED" else "DECLINED"
            var responderName = userId
            val updatedMembers = currentCircle.members.map { m ->
                if (m.userId == userId || m.isYou) {
                    responderName = m.name
                    m.copy(
                        acceptanceStatus = newStatus,
                        acceptedTermsVersion = currentCircle.termsVersion
                    )
                } else m
            }

            val allAccepted = updatedMembers.all {
                it.acceptanceStatus == "ACCEPTED" && it.acceptedTermsVersion == currentCircle.termsVersion
            }
            val anyDeclined = updatedMembers.any { it.acceptanceStatus == "DECLINED" }

            val nextLifecycle = when {
                anyDeclined -> "DECLINED_REVISION_NEEDED"
                allAccepted -> "READY_TO_ACTIVATE"
                else -> "AWAITING_ACCEPTANCE"
            }

            val record = CircleTransactionRecord(
                id = "ctx_${System.currentTimeMillis()}",
                type = if (accept) "TERMS_ACCEPTED" else "TERMS_DECLINED",
                title = if (accept) "Accepted Circle Terms (v${currentCircle.termsVersion})"
                else "Declined Circle Invitation (v${currentCircle.termsVersion})",
                memberName = responderName,
                memberPhone = userId,
                roundNumber = 0,
                amount = currentCircle.contributionPerMonth,
                currency = currentCircle.currency,
                status = if (accept) "CONFIRMED" else "DECLINED",
                txHash = "CONSENT-V${currentCircle.termsVersion}-${userId.takeLast(4)}"
            )

            val updatedCircle = currentCircle.copy(
                members = updatedMembers,
                lifecycleStatus = nextLifecycle,
                isActive = false,
                transactionHistory = listOf(record) + currentCircle.transactionHistory
            )
            updateCircle(updatedCircle)

            // Also mark any matching pending invitation doc in Firestore
            val invId = "inv_${circleId}_${userId}_v${currentCircle.termsVersion}"
            runCatching { invitationsCollection.document(invId).update("status", newStatus).await() }

            Result.success(updatedCircle)
        } catch (e: Exception) {
            Log.e(TAG, "respondToCircleTerms failed", e)
            Result.failure(e)
        }
    }

    /**
     * Organizer removes a declined invitee BEFORE activation, issues revised final terms (`termsVersion + 1`),
     * recalculates the deterministic scheduled payout (`fixedContribution * remainingMembers.size`) and
     * rotating payout order (`1..remainingMembers.size`), and resets all other participants' acceptance
     * to `PENDING` so every remaining participant must explicitly accept the revised final terms.
     */
    suspend fun removeDeclinedMemberAndReviseTerms(
        circleId: String,
        declinedUserId: String,
        organizerId: String
    ): Result<Circle> {
        return try {
            val circle = getCircleById(circleId, organizerId)
                ?: return Result.failure(Exception("Circle not found."))
            if (circle.lifecycleStatus == "ACTIVE" || circle.lifecycleStatus == "COMPLETED") {
                return Result.failure(Exception("Cannot remove or substitute members after Circle activation."))
            }
            if (circle.creatorId != organizerId) {
                return Result.failure(Exception("Only the Circle organizer can revise pre-activation terms."))
            }

            val remaining = circle.members.filterNot { it.userId == declinedUserId || (it.acceptanceStatus == "DECLINED" && declinedUserId.isBlank()) }
            if (remaining.size < 2) {
                return Result.failure(Exception("A Circle requires at least 2 participants. Please add another member or create a new Circle proposal."))
            }

            val nextVersion = circle.termsVersion + 1
            val reindexedMembers = remaining.mapIndexed { index, m ->
                val isOrg = m.userId == organizerId
                m.copy(
                    payoutRound = index + 1,
                    acceptanceStatus = if (isOrg) "ACCEPTED" else "PENDING",
                    acceptedTermsVersion = if (isOrg) nextVersion else m.acceptedTermsVersion
                )
            }

            val newTarget = circle.contributionPerMonth * reindexedMembers.size
            val allAccepted = reindexedMembers.all { it.acceptanceStatus == "ACCEPTED" && it.acceptedTermsVersion == nextVersion }

            val record = CircleTransactionRecord(
                id = "ctx_${System.currentTimeMillis()}",
                type = "TERMS_REVISED",
                title = "Revised Final Terms Issued (v$nextVersion) — ${reindexedMembers.size} Members",
                memberName = circle.creatorName.ifBlank { "Organizer" },
                memberPhone = organizerId,
                roundNumber = 0,
                amount = newTarget,
                currency = circle.currency,
                status = "CONFIRMED",
                txHash = "REVISED-TERMS-V$nextVersion"
            )

            val updatedCircle = circle.copy(
                members = reindexedMembers,
                memberCount = reindexedMembers.size,
                maxMembers = reindexedMembers.size,
                targetAmount = newTarget,
                termsVersion = nextVersion,
                lifecycleStatus = if (allAccepted) "READY_TO_ACTIVATE" else "AWAITING_ACCEPTANCE",
                isActive = false,
                transactionHistory = listOf(record) + circle.transactionHistory
            )
            updateCircle(updatedCircle)

            // Re-send invitations for the new termsVersion to all non-organizer members
            reindexedMembers.filter { it.userId != organizerId }.forEach { invitee ->
                sendInvitation(
                    circleId = circle.id,
                    inviteeUserId = invitee.userId,
                    inviteeName = invitee.name,
                    inviteeInitials = invitee.initials,
                    inviterName = circle.creatorName.ifBlank { "Organizer" },
                    inviterScore = 800,
                    circleName = circle.name,
                    contributionAmount = circle.contributionPerMonth,
                    memberCount = reindexedMembers.size,
                    termsVersion = nextVersion,
                    scheduleText = circle.targetDate,
                    currency = circle.currency
                )
            }

            Result.success(updatedCircle)
        } catch (e: Exception) {
            Log.e(TAG, "removeDeclinedMemberAndReviseTerms failed", e)
            Result.failure(e)
        }
    }

    /**
     * Locks and activates the Circle on Soroban after verifying that all final participants
     * have accepted the exact same [Circle.termsVersion].
     */
    suspend fun activateCircle(
        circleId: String,
        organizerId: String,
        sorobanCircleId: Long,
        activationTxHash: String
    ): Result<Circle> {
        return try {
            val circle = getCircleById(circleId, organizerId)
                ?: return Result.failure(Exception("Circle not found."))
            if (!circle.allMembersAcceptedCurrentTerms) {
                return Result.failure(Exception("Cannot activate: Not all participants have accepted Terms v${circle.termsVersion}."))
            }

            val resetMembers = circle.members.sortedBy { it.payoutRound }.mapIndexed { idx, m ->
                m.copy(
                    payoutRound = idx + 1,
                    status = ContributionStatus.PENDING,
                    amount = 0.0,
                    contributionTxHash = "",
                    hasReceivedPayout = false,
                    payoutTxHash = ""
                )
            }
            val derivedPayout = circle.contributionPerMonth * resetMembers.size

            val record = CircleTransactionRecord(
                id = "ctx_${System.currentTimeMillis()}",
                type = "CIRCLE_ACTIVATION",
                title = "Circle Activated & Locked on Soroban (Terms v${circle.termsVersion})",
                memberName = circle.creatorName.ifBlank { "Organizer" },
                memberPhone = organizerId,
                roundNumber = 1,
                amount = derivedPayout,
                currency = circle.currency,
                status = "CONFIRMED",
                txHash = activationTxHash
            )

            val activated = circle.copy(
                members = resetMembers,
                memberCount = resetMembers.size,
                maxMembers = resetMembers.size,
                targetAmount = derivedPayout,
                savedAmount = 0.0,
                isActive = true,
                lifecycleStatus = "ACTIVE",
                currentRound = 1,
                currentRecipientIndex = 0,
                sorobanCircleId = sorobanCircleId,
                activationTxHash = activationTxHash,
                escrowAddress = StellarWalletManager.CIRCLE_CONTRACT_ID,
                escrowNetwork = "Stellar Testnet · Soroban (#$sorobanCircleId)",
                transactionHistory = listOf(record) + circle.transactionHistory
            )
            updateCircle(activated)
            Result.success(activated)
        } catch (e: Exception) {
            Log.e(TAG, "activateCircle failed", e)
            Result.failure(e)
        }
    }

    /**
     * Records a member's verified on-chain contribution for the active round.
     */
    suspend fun recordMemberContribution(
        circleId: String,
        userId: String,
        txHash: String
    ): Result<Circle> {
        return try {
            val circle = getCircleById(circleId, userId)
                ?: return Result.failure(Exception("Circle not found."))
            if (circle.lifecycleStatus != "ACTIVE") {
                return Result.failure(Exception("Circle is not active."))
            }
            val member = circle.members.find { it.userId == userId || it.isYou }
                ?: return Result.failure(Exception("You are not an enrolled member of this Circle."))
            if (member.status == ContributionStatus.PAID) {
                return Result.failure(Exception("You have already contributed for Round ${circle.currentRound}."))
            }

            val updatedMembers = circle.members.map { m ->
                if (m.userId == member.userId) {
                    m.copy(
                        status = ContributionStatus.PAID,
                        amount = circle.contributionPerMonth,
                        contributionTxHash = txHash
                    )
                } else m
            }
            val contributedCount = updatedMembers.count { it.status == ContributionStatus.PAID }
            val newPooled = contributedCount * circle.contributionPerMonth

            val record = CircleTransactionRecord(
                id = "ctx_${System.currentTimeMillis()}",
                type = "CONTRIBUTION",
                title = "Round ${circle.currentRound} Contribution",
                memberName = member.name,
                memberPhone = member.userId,
                roundNumber = circle.currentRound,
                amount = circle.contributionPerMonth,
                currency = circle.currency,
                status = "CONFIRMED",
                txHash = txHash
            )

            val updatedCircle = circle.copy(
                members = updatedMembers,
                savedAmount = newPooled,
                transactionHistory = listOf(record) + circle.transactionHistory
            )
            updateCircle(updatedCircle)
            Result.success(updatedCircle)
        } catch (e: Exception) {
            Log.e(TAG, "recordMemberContribution failed", e)
            Result.failure(e)
        }
    }

    /**
     * Executes and records the deterministic scheduled payout for the active round, advances
     * to the next round (or marks the Circle `"COMPLETED"` when all rounds are paid).
     */
    suspend fun recordRoundPayout(
        circleId: String,
        callerUserId: String,
        callerName: String,
        txHash: String
    ): Result<Circle> {
        return try {
            val circle = getCircleById(circleId, callerUserId)
                ?: return Result.failure(Exception("Circle not found."))
            if (circle.lifecycleStatus != "ACTIVE") {
                return Result.failure(Exception("Circle is not active."))
            }
            if (!circle.allCurrentRoundContributionsComplete) {
                return Result.failure(Exception("Payout rejected: Not all members have contributed for Round ${circle.currentRound}."))
            }
            if (circle.paidRounds.contains(circle.currentRound)) {
                return Result.failure(Exception("Duplicate payout rejected: Round ${circle.currentRound} has already been paid."))
            }

            val orderedMembers = circle.members.sortedBy { it.payoutRound }
            val recipient = orderedMembers.getOrNull(circle.currentRound - 1)
                ?: return Result.failure(Exception("Invalid round recipient index."))
            val exactPayoutAmount = circle.scheduledPayoutAmount

            val paidRound = circle.currentRound
            val newPaidRounds = (circle.paidRounds + paidRound).distinct()
            val isAllCompleted = newPaidRounds.size >= orderedMembers.size

            val nextRound = if (isAllCompleted) paidRound else paidRound + 1
            val nextMembers = orderedMembers.map { m ->
                val justPaid = m.userId == recipient.userId
                m.copy(
                    status = if (isAllCompleted) ContributionStatus.PAID else ContributionStatus.PENDING,
                    amount = if (isAllCompleted) circle.contributionPerMonth else 0.0,
                    contributionTxHash = if (isAllCompleted) m.contributionTxHash else "",
                    hasReceivedPayout = m.hasReceivedPayout || justPaid,
                    payoutTxHash = if (justPaid) txHash else m.payoutTxHash
                )
            }

            val payoutRecord = CircleTransactionRecord(
                id = "ctx_${System.currentTimeMillis()}",
                type = "PAYOUT",
                title = "Round $paidRound Scheduled Payout → ${recipient.name} (invoked by $callerName)",
                memberName = recipient.name,
                memberPhone = recipient.userId,
                roundNumber = paidRound,
                amount = exactPayoutAmount,
                currency = circle.currency,
                status = "CONFIRMED",
                txHash = txHash
            )

            val updatedCircle = circle.copy(
                members = nextMembers,
                savedAmount = if (isAllCompleted) exactPayoutAmount else 0.0,
                currentRound = nextRound,
                currentRecipientIndex = (nextRound - 1).coerceAtLeast(0),
                paidRounds = newPaidRounds,
                lifecycleStatus = if (isAllCompleted) "COMPLETED" else "ACTIVE",
                isActive = !isAllCompleted,
                transactionHistory = listOf(payoutRecord) + circle.transactionHistory
            )
            updateCircle(updatedCircle)
            Result.success(updatedCircle)
        } catch (e: Exception) {
            Log.e(TAG, "recordRoundPayout failed", e)
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
        val current = getCircleById(circleId, userId)
            ?: return Result.failure(Exception("Circle not found"))
        val res = respondToCircleTerms(circleId, userId, accept = true, termsVersion = current.termsVersion)
        return res
    }

    suspend fun approveJoinRequest(requestId: String, approverId: String): Result<Circle> {
        return try {
            val requestDoc = db.collection("circle_join_requests").document(requestId).get().await()
            val circleId = requestDoc.getString("circleId") ?: throw Exception("Circle not found")
            db.collection("circle_join_requests").document(requestId).delete().await()
            val circle = getCircleById(circleId, approverId) ?: throw Exception("Circle not found")
            Result.success(circle)
        } catch (e: Exception) {
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
        val circle = getCircleById(invitation.circleId, AuthRepository.getCurrentUserPhone())
        return if (circle != null) {
            invitation.copy(
                circleName = circle.name,
                targetAmount = circle.scheduledPayoutAmount,
                monthlyContribution = circle.contributionPerMonth,
                memberCount = circle.members.size,
                maxMembers = circle.members.size,
                termsVersion = circle.termsVersion,
                scheduleText = circle.targetDate,
                currency = circle.currency
            )
        } else invitation
    }
}
