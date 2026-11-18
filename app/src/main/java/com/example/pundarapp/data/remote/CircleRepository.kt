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
    private val usersCollection = db.collection("users")
    private val circlesCollection = db.collection("circles")
    private val invitationsCollection = db.collection("circle_invitations")

    private val localCirclesCache = java.util.concurrent.ConcurrentHashMap<String, Circle>()
    private val localInvitationsCache = java.util.concurrent.ConcurrentHashMap<String, CircleInvitation>()

    @Suppress("UNCHECKED_CAST")
    private fun parseCircleMap(data: Map<String, Any>, fallbackId: String, currentUserId: String): Circle? {
        return try {
            val currentUserName = AuthRepository.currentUserState.value?.name ?: ""
            val membersList = data["members"] as? List<Map<String, Any>> ?: emptyList()
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
                        (mName.isNotBlank() && (mName == currentUserId || mName.equals(currentUserName, ignoreCase = true))),
                    payoutDate = m["payoutDate"] as? String ?: "",
                    avatarColor = (m["avatarColor"] as? Number)?.toLong() ?: 0xFF2563EB,
                    stellarPublicKey = m["stellarPublicKey"] as? String ?: "",
                    payoutRound = (m["payoutRound"] as? Number)?.toInt() ?: (idx + 1),
                    acceptanceStatus = m["acceptanceStatus"] as? String ?: "ACCEPTED",
                    acceptedTermsVersion = (m["acceptedTermsVersion"] as? Number)?.toInt() ?: 1,
                    declineReason = m["declineReason"] as? String ?: "",
                    contributionTxHash = m["contributionTxHash"] as? String ?: "",
                    hasReceivedPayout = m["hasReceivedPayout"] as? Boolean ?: false,
                    payoutTxHash = m["payoutTxHash"] as? String ?: "",
                    pundarScore = (m["pundarScore"] as? Number)?.toInt() ?: 800,
                    isBanned = m["isBanned"] as? Boolean ?: false,
                    paidAtTimestamp = (m["paidAtTimestamp"] as? Number)?.toLong() ?: 0L,
                    ledgerSequence = (m["ledgerSequence"] as? Number)?.toLong() ?: 0L
                )
            }
                .distinctBy { if (it.userId.isNotEmpty()) it.userId else it.name }
                .sortedBy { it.payoutRound }

            val txList = data["transactionHistory"] as? List<Map<String, Any>> ?: emptyList()
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

            val paidRoundsRaw = data["paidRounds"] as? List<Number> ?: emptyList()
            val paidRounds = paidRoundsRaw.map { it.toInt() }

            val contributionPerMonth = (data["contributionPerMonth"] as? Number)?.toDouble() ?: 0.0
            val derivedPayout = contributionPerMonth * members.size.coerceAtLeast(1)
            val lifecycleStatus = data["lifecycleStatus"] as? String
                ?: if ((data["isActive"] as? Boolean) == false) "COMPLETED" else "ACTIVE"

            Circle(
                id = data["id"] as? String ?: fallbackId,
                name = data["name"] as? String ?: "",
                targetAmount = if (derivedPayout > 0.0) derivedPayout else ((data["targetAmount"] as? Number)?.toDouble() ?: 0.0),
                savedAmount = (data["savedAmount"] as? Number)?.toDouble() ?: 0.0,
                targetDate = data["targetDate"] as? String ?: "",
                memberCount = members.size,
                contributionPerMonth = contributionPerMonth,
                members = members,
                isActive = lifecycleStatus == "ACTIVE",
                currency = data["currency"] as? String ?: "XLM",
                escrowAddress = data["escrowAddress"] as? String ?: StellarWalletManager.CIRCLE_CONTRACT_ID,
                escrowNetwork = data["escrowNetwork"] as? String ?: "Stellar Testnet · Soroban",
                creatorId = data["creatorId"] as? String ?: "",
                creatorName = data["creatorName"] as? String ?: "",
                payoutFrequencyValue = (data["payoutFrequencyValue"] as? Number)?.toInt() ?: 1,
                payoutFrequencyUnit = data["payoutFrequencyUnit"] as? String ?: "Months",
                maxMembers = (data["maxMembers"] as? Number)?.toInt() ?: members.size.coerceAtLeast(2),
                currentRecipientIndex = (data["currentRecipientIndex"] as? Number)?.toInt() ?: 0,
                payoutOrderType = data["payoutOrderType"] as? String ?: "Deterministic Fixed Rotation",
                lifecycleStatus = lifecycleStatus,
                termsVersion = (data["termsVersion"] as? Number)?.toInt() ?: 1,
                currentRound = (data["currentRound"] as? Number)?.toInt() ?: 1,
                sorobanCircleId = (data["sorobanCircleId"] as? Number)?.toLong() ?: 0L,
                activationTxHash = data["activationTxHash"] as? String ?: "",
                paidRounds = paidRounds,
                transactionHistory = history
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse circle map $fallbackId", e)
            null
        }
    }

    private fun parseCircleDoc(doc: DocumentSnapshot, currentUserId: String): Circle? {
        if (!doc.exists()) return null
        val data = doc.data ?: return null
        return parseCircleMap(data, doc.id, currentUserId)
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
                    "declineReason" to m.declineReason,
                    "contributionTxHash" to m.contributionTxHash,
                    "hasReceivedPayout" to m.hasReceivedPayout,
                    "payoutTxHash" to m.payoutTxHash,
                    "pundarScore" to m.pundarScore,
                    "isBanned" to m.isBanned,
                    "paidAtTimestamp" to m.paidAtTimestamp,
                    "ledgerSequence" to m.ledgerSequence
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

    private suspend fun syncCircleToUserDocs(circle: Circle, circleMap: HashMap<String, Any>) {
        localCirclesCache[circle.id] = circle
        val participantIds = buildSet {
            if (circle.creatorId.isNotBlank()) add(circle.creatorId.trim())
            circle.members.forEach { m ->
                if (m.userId.isNotBlank()) add(m.userId.trim())
            }
        }
        for (uid in participantIds) {
            runCatching {
                kotlinx.coroutines.withTimeout(4000) {
                    usersCollection.document(uid).set(
                        mapOf("user_circles" to mapOf(circle.id to circleMap)),
                        com.google.firebase.firestore.SetOptions.merge()
                    ).await()
                }
            }
        }
        runCatching {
            kotlinx.coroutines.withTimeout(3000) {
                circlesCollection.document(circle.id).set(circleMap).await()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mergeCircleStates(circles: List<Circle>, currentUserId: String): Circle? {
        if (circles.isEmpty()) return null
        if (circles.size == 1) return circles.first()

        // Pick the base circle with the highest termsVersion, or active/completed lifecycle, or newest transaction
        val base = circles.maxWithOrNull(
            compareBy<Circle> { it.termsVersion }
                .thenBy {
                    when (it.lifecycleStatus) {
                        "COMPLETED" -> 4
                        "ACTIVE" -> 3
                        "READY_TO_ACTIVATE" -> 2
                        "DECLINED_REVISION_NEEDED" -> 1
                        else -> 0
                    }
                }
                .thenBy { it.currentRound }
                .thenBy { it.transactionHistory.maxOfOrNull { tx -> tx.timestamp } ?: 0L }
        ) ?: circles.first()

        val maxTermsVer = base.termsVersion
        val mergedHistory = circles
            .flatMap { it.transactionHistory }
            .distinctBy { "${it.type}_${it.memberPhone}_${it.roundNumber}_${it.txHash}" }
            .sortedByDescending { it.timestamp }

        val mergedMembers = base.members.map { baseMem ->
            // Find all versions of this member across participant docs for the same termsVersion
            val memberVersions = circles
                .filter { it.termsVersion == maxTermsVer }
                .mapNotNull { c ->
                    c.members.find {
                        (baseMem.userId.isNotBlank() && it.userId == baseMem.userId) ||
                            it.name.equals(baseMem.name, ignoreCase = true)
                    }
                }

            val bestAccept = when {
                memberVersions.any { it.acceptanceStatus == "DECLINED" } -> "DECLINED"
                memberVersions.any { it.acceptanceStatus == "ACCEPTED" && it.acceptedTermsVersion == maxTermsVer } -> "ACCEPTED"
                else -> baseMem.acceptanceStatus
            }
            val bestDeclineReason = memberVersions.firstOrNull { it.declineReason.isNotBlank() }?.declineReason
                ?: baseMem.declineReason

            // For active round contribution status, merge PAID or OVERDUE from any copy on the same currentRound
            val activeVersions = circles
                .filter { it.currentRound == base.currentRound }
                .mapNotNull { c ->
                    c.members.find {
                        (baseMem.userId.isNotBlank() && it.userId == baseMem.userId) ||
                            it.name.equals(baseMem.name, ignoreCase = true)
                    }
                }
            val paidVer = activeVersions.firstOrNull { it.status == ContributionStatus.PAID }
            val overdueVer = activeVersions.firstOrNull { it.status == ContributionStatus.OVERDUE }

            baseMem.copy(
                isYou = (baseMem.userId.isNotBlank() && baseMem.userId == currentUserId) ||
                    baseMem.name.equals(AuthRepository.getCurrentUserName(), ignoreCase = true),
                acceptanceStatus = bestAccept,
                acceptedTermsVersion = if (bestAccept == "ACCEPTED") maxTermsVer else baseMem.acceptedTermsVersion,
                declineReason = bestDeclineReason,
                status = when {
                    paidVer != null -> ContributionStatus.PAID
                    overdueVer != null -> ContributionStatus.OVERDUE
                    else -> baseMem.status
                },
                amount = paidVer?.amount ?: baseMem.amount,
                contributionTxHash = paidVer?.contributionTxHash?.ifBlank { baseMem.contributionTxHash } ?: baseMem.contributionTxHash,
                paidAtTimestamp = paidVer?.paidAtTimestamp?.takeIf { it > 0L } ?: baseMem.paidAtTimestamp,
                ledgerSequence = paidVer?.ledgerSequence?.takeIf { it > 0L } ?: baseMem.ledgerSequence,
                isBanned = memberVersions.any { it.isBanned } || baseMem.isBanned
            )
        }

        val nextLifecycle = if (base.lifecycleStatus in listOf("ACTIVE", "COMPLETED")) {
            base.lifecycleStatus
        } else {
            val anyDeclined = mergedMembers.any { it.acceptanceStatus == "DECLINED" }
            val allAccepted = mergedMembers.isNotEmpty() && mergedMembers.all {
                it.acceptanceStatus == "ACCEPTED" && it.acceptedTermsVersion == maxTermsVer
            }
            when {
                anyDeclined -> "DECLINED_REVISION_NEEDED"
                allAccepted -> "READY_TO_ACTIVATE"
                else -> "AWAITING_ACCEPTANCE"
            }
        }

        val paidCount = mergedMembers.count { it.status == ContributionStatus.PAID }
        val pooled = if (nextLifecycle == "ACTIVE") paidCount * base.contributionPerMonth else base.savedAmount

        return base.copy(
            members = mergedMembers,
            lifecycleStatus = nextLifecycle,
            isActive = nextLifecycle == "ACTIVE",
            savedAmount = pooled,
            transactionHistory = mergedHistory
        )
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getCircleById(circleId: String, currentUserId: String): Circle? {
        val uid = currentUserId.ifBlank { AuthRepository.getCurrentUserPhone() }
        val copies = mutableListOf<Circle>()

        // 1. Read from all users documents or participant user documents
        runCatching {
            val snap = kotlinx.coroutines.withTimeout(5000) { usersCollection.get().await() }
            for (doc in snap.documents) {
                val userCircles = doc.get("user_circles") as? Map<String, Any> ?: continue
                val rawMap = userCircles[circleId] as? Map<String, Any> ?: continue
                parseCircleMap(rawMap, circleId, uid)?.let { copies.add(it) }
            }
        }

        // 2. Also try top-level circles collection if permitted
        runCatching {
            val doc = kotlinx.coroutines.withTimeout(2500) {
                circlesCollection.document(circleId).get().await()
            }
            parseCircleDoc(doc, uid)?.let { copies.add(it) }
        }

        // 3. Merge all copies so every member's acceptance/decline/payment is reflected
        val merged = mergeCircleStates(copies, uid)
        if (merged != null) {
            localCirclesCache[circleId] = merged
            return merged
        }

        return localCirclesCache[circleId]
            ?: com.example.pundarapp.ui.data.AppState.circles.find { it.id == circleId }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getCirclesForUser(userId: String): List<Circle> {
        val copiesByCircleId = linkedMapOf<String, MutableList<Circle>>()
        val currentUserName = AuthRepository.currentUserState.value?.name ?: ""

        // 1. Scan 'users' collection so invited members and organizers see shared Circles and updates
        runCatching {
            val snap = kotlinx.coroutines.withTimeout(5000) { usersCollection.get().await() }
            for (doc in snap.documents) {
                val userCircles = doc.get("user_circles") as? Map<String, Any> ?: continue
                for ((cid, raw) in userCircles) {
                    val map = raw as? Map<String, Any> ?: continue
                    val parsed = parseCircleMap(map, cid, userId) ?: continue
                    val isParticipant = parsed.creatorId == userId ||
                        parsed.members.any {
                            it.userId == userId || it.name == userId ||
                                (currentUserName.isNotBlank() && it.name.equals(currentUserName, ignoreCase = true))
                        }
                    if (isParticipant) {
                        copiesByCircleId.getOrPut(parsed.id) { mutableListOf() }.add(parsed)
                    }
                }
            }
        }

        // 2. Also try top-level 'circles' collection if permitted
        runCatching {
            val snapshot = kotlinx.coroutines.withTimeout(2500) { circlesCollection.get().await() }
            snapshot.documents.mapNotNull { doc -> parseCircleDoc(doc, userId) }.forEach { circle ->
                val isParticipant = circle.creatorId == userId ||
                    circle.members.any {
                        it.userId == userId || it.name == userId ||
                            (currentUserName.isNotBlank() && it.name.equals(currentUserName, ignoreCase = true))
                    }
                if (isParticipant) {
                    copiesByCircleId.getOrPut(circle.id) { mutableListOf() }.add(circle)
                }
            }
        }

        val results = linkedMapOf<String, Circle>()
        for ((cid, list) in copiesByCircleId) {
            mergeCircleStates(list, userId)?.let { merged ->
                results[cid] = merged
                localCirclesCache[cid] = merged
            }
        }

        // 3. Merge any locally cached circles for this user
        localCirclesCache.values.forEach { cached ->
            if (!results.containsKey(cached.id)) {
                val isParticipant = cached.creatorId == userId ||
                    cached.members.any {
                        it.userId == userId ||
                            (currentUserName.isNotBlank() && it.name.equals(currentUserName, ignoreCase = true))
                    }
                if (isParticipant) {
                    results[cached.id] = cached
                }
            }
        }

        return results.values.toList()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getPendingInvitations(userId: String): List<CircleInvitation> {
        val invites = linkedMapOf<String, CircleInvitation>()

        // 1. Read from user's 'users/{userId}' document (always permitted)
        if (userId.isNotBlank()) {
            runCatching {
                val userDoc = kotlinx.coroutines.withTimeout(4000) {
                    usersCollection.document(userId).get().await()
                }
                val rawInvites = userDoc.get("user_circle_invitations") as? Map<String, Any>
                rawInvites?.forEach { (invKey, raw) ->
                    val m = raw as? Map<String, Any> ?: return@forEach
                    val status = m["status"] as? String ?: "PENDING"
                    if (status != "PENDING") return@forEach
                    val cid = m["circleId"] as? String ?: return@forEach
                    val inv = CircleInvitation(
                        id = m["id"] as? String ?: invKey,
                        circleId = cid,
                        circleName = m["circleName"] as? String ?: "PUNDAR Circle",
                        goal = m["goal"] as? String ?: "Deterministic Rotating Savings",
                        inviterName = m["inviterName"] as? String ?: "Organizer",
                        inviterScore = (m["inviterScore"] as? Number)?.toInt() ?: 800,
                        inviterCirclesCompleted = (m["inviterCirclesCompleted"] as? Number)?.toInt() ?: 1,
                        targetAmount = (m["targetAmount"] as? Number)?.toDouble() ?: 0.0,
                        fundedPercent = 0,
                        monthlyContribution = (m["monthlyContribution"] as? Number)?.toDouble() ?: 0.0,
                        memberCount = (m["memberCount"] as? Number)?.toInt() ?: 2,
                        maxMembers = (m["maxMembers"] as? Number)?.toInt() ?: 2,
                        inviterInitials = (m["inviterName"] as? String ?: "OR").take(2).uppercase(),
                        termsVersion = (m["termsVersion"] as? Number)?.toInt() ?: 1,
                        scheduleText = m["scheduleText"] as? String ?: "Bi-weekly",
                        currency = m["currency"] as? String ?: "PHP"
                    )
                    invites[inv.id] = inv
                }
            }
        }

        // 2. Also try top-level 'circle_invitations' collection if permitted
        runCatching {
            val snapshot = kotlinx.coroutines.withTimeout(3000) {
                invitationsCollection
                    .whereEqualTo("inviteeUserId", userId)
                    .whereEqualTo("status", "PENDING")
                    .get()
                    .await()
            }
            snapshot.documents.forEach { doc ->
                val cid = doc.getString("circleId") ?: return@forEach
                val inv = CircleInvitation(
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
                invites[inv.id] = inv
            }
        }

        // 3. Also synthesize pending invitations from any pre-activation Circle where this user must accept current terms
        getCirclesForUser(userId).forEach { circle ->
            if (circle.lifecycleStatus == "ACTIVE" || circle.lifecycleStatus == "COMPLETED") return@forEach
            val isOrg = if (circle.creatorId.isNotBlank()) circle.creatorId == userId else circle.members.firstOrNull()?.userId == userId
            if (isOrg) return@forEach
            val myMem = circle.members.find { it.userId == userId }
            val needsAcceptance = myMem != null &&
                myMem.acceptanceStatus != "DECLINED" &&
                (myMem.acceptanceStatus != "ACCEPTED" || myMem.acceptedTermsVersion != circle.termsVersion)
            if (needsAcceptance) {
                val synthId = "inv_${circle.id}_${userId}_v${circle.termsVersion}"
                if (invites.values.none { it.circleId == circle.id && it.termsVersion == circle.termsVersion }) {
                    invites.entries.removeAll { it.value.circleId == circle.id }
                    invites[synthId] = CircleInvitation(
                        id = synthId,
                        circleId = circle.id,
                        circleName = circle.name,
                        goal = "Scheduled Payout: ${String.format("%,.2f", circle.scheduledPayoutAmount)} ${circle.currency} (${circle.members.size} members • Terms v${circle.termsVersion})",
                        inviterName = circle.creatorName.ifBlank { "Organizer" },
                        inviterScore = 800,
                        inviterCirclesCompleted = 1,
                        targetAmount = circle.scheduledPayoutAmount,
                        fundedPercent = 0,
                        monthlyContribution = circle.contributionPerMonth,
                        memberCount = circle.members.size,
                        maxMembers = circle.members.size,
                        inviterInitials = circle.creatorName.ifBlank { "OR" }.take(2).uppercase(),
                        termsVersion = circle.termsVersion,
                        scheduleText = circle.targetDate,
                        currency = circle.currency
                    )
                }
            }
        }

        return invites.values.toList()
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
            syncCircleToUserDocs(circle, circleMap)
            Result.success(circle)
        } catch (e: Exception) {
            Log.e(TAG, "createCircle fallback to local cache", e)
            localCirclesCache[circle.id] = circle
            Result.success(circle)
        }
    }

    suspend fun updateCircle(circle: Circle): Result<Circle> {
        return try {
            val circleMap = serializeCircle(circle)
            syncCircleToUserDocs(circle, circleMap)
            Result.success(circle)
        } catch (e: Exception) {
            Log.e(TAG, "updateCircle fallback to local cache", e)
            localCirclesCache[circle.id] = circle
            Result.success(circle)
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
            if (inviteeUserId.isNotBlank()) {
                runCatching {
                    kotlinx.coroutines.withTimeout(4000) {
                        usersCollection.document(inviteeUserId.trim()).set(
                            mapOf("user_circle_invitations" to mapOf(invId to data)),
                            com.google.firebase.firestore.SetOptions.merge()
                        ).await()
                    }
                }
            }
            runCatching {
                kotlinx.coroutines.withTimeout(3000) {
                    invitationsCollection.document(invId).set(data).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "sendInvitation fallback", e)
            Result.success(Unit)
        }
    }

    suspend fun updateInvitationStatus(invitationId: String, status: String): Result<Unit> {
        return try {
            val myPhone = AuthRepository.getCurrentUserPhone()
            if (myPhone.isNotBlank()) {
                runCatching {
                    kotlinx.coroutines.withTimeout(3000) {
                        usersCollection.document(myPhone)
                            .update("user_circle_invitations.$invitationId.status", status)
                            .await()
                    }
                }
            }
            runCatching {
                kotlinx.coroutines.withTimeout(3000) {
                    invitationsCollection.document(invitationId).update("status", status).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    private fun isCircleOrganizer(circle: Circle, userId: String): Boolean {
        if (userId.isBlank()) return false
        return if (circle.creatorId.isNotBlank()) {
            circle.creatorId == userId
        } else {
            circle.members.firstOrNull()?.userId == userId
        }
    }

    /**
     * Records a member's explicit ACCEPT or DECLINE for a specific [termsVersion] of a Circle proposal.
     * - If accepted and all final members have now accepted the same [termsVersion], transitions
     *   `lifecycleStatus` to `"READY_TO_ACTIVATE"`.
     * - If declined, transitions `lifecycleStatus` to `"DECLINED_REVISION_NEEDED"`, keeps Circle inactive,
     *   and notifies BOTH the Admin/Organizer AND all other members in the Circle.
     */
    suspend fun respondToCircleTerms(
        circleId: String,
        userId: String,
        accept: Boolean,
        termsVersion: Int,
        declineReason: String = ""
    ): Result<Circle> {
        return try {
            val currentCircle = getCircleById(circleId, userId)
                ?: return Result.failure(Exception("Circle proposal not found."))

            if (currentCircle.lifecycleStatus == "ACTIVE" || currentCircle.lifecycleStatus == "COMPLETED") {
                return Result.failure(Exception("Active or completed Circles are locked on Soroban. Members cannot leave or change details."))
            }
            if (currentCircle.termsVersion != termsVersion) {
                return Result.failure(
                    Exception("Circle terms were revised to v${currentCircle.termsVersion}. Please review the latest terms.")
                )
            }

            if (accept) {
                val targetMember = currentCircle.members.find { it.userId == userId || it.isYou }
                if (targetMember?.isBanned == true) {
                    return Result.failure(Exception("Member is banned from joining Circles due to a previous failed payment."))
                }
            }

            val newStatus = if (accept) "ACCEPTED" else "DECLINED"
            var responderName = userId
            val cleanReason = if (!accept) declineReason.trim().ifBlank { "Schedule or contribution conflict." } else ""
            val updatedMembers = currentCircle.members.map { m ->
                if (m.userId == userId || (userId == AuthRepository.getCurrentUserPhone() && m.isYou)) {
                    responderName = m.name
                    m.copy(
                        acceptanceStatus = newStatus,
                        acceptedTermsVersion = currentCircle.termsVersion,
                        declineReason = cleanReason
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
                else "Declined Circle Invitation (v${currentCircle.termsVersion}): $cleanReason",
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
            runCatching {
                if (userId.isNotBlank()) {
                    usersCollection.document(userId)
                        .update("user_circle_invitations.$invId.status", newStatus)
                        .await()
                }
            }
            runCatching { invitationsCollection.document(invId).update("status", newStatus).await() }

            // Notify Admin AND all other Circle participants
            val otherRecipientIds = buildSet {
                if (currentCircle.creatorId.isNotBlank()) add(currentCircle.creatorId.trim())
                updatedMembers.forEach { m ->
                    if (m.userId.isNotBlank()) add(m.userId.trim())
                }
            }.filter { it.isNotBlank() && it != userId }

            if (!accept) {
                for (recipientId in otherRecipientIds) {
                    val isRecOrg = isCircleOrganizer(currentCircle, recipientId)
                    val msg = if (isRecOrg) {
                        "$responderName declined your invitation to '${currentCircle.name}'. Reason: $cleanReason. Open the Circle to replace $responderName, revise members, or cancel the proposal."
                    } else {
                        "$responderName declined the invitation to join '${currentCircle.name}'. Reason: $cleanReason. The Circle remains inactive until updated by the organizer."
                    }
                    runCatching {
                        NotificationRepository.sendNotification(
                            userId = recipientId,
                            title = "Circle Invitation Declined",
                            message = msg,
                            kind = "CIRCLE"
                        )
                    }
                }
            } else {
                val orgId = currentCircle.creatorId.trim()
                if (orgId.isNotBlank() && orgId != userId) {
                    val title = if (allAccepted) "Circle Ready to Activate" else "Circle Member Accepted"
                    val msg = if (allAccepted) {
                        "$responderName accepted Terms v${currentCircle.termsVersion}. All ${updatedMembers.size} members have agreed — '${currentCircle.name}' is ready to activate and lock on Soroban."
                    } else {
                        "$responderName accepted Terms v${currentCircle.termsVersion} for '${currentCircle.name}'."
                    }
                    runCatching {
                        NotificationRepository.sendNotification(
                            userId = orgId,
                            title = title,
                            message = msg,
                            kind = "CIRCLE"
                        )
                    }
                }
            }

            Result.success(updatedCircle)
        } catch (e: Exception) {
            Log.e(TAG, "respondToCircleTerms failed", e)
            Result.failure(e)
        }
    }

    /**
     * Organizer replaces a declined invitee in the exact same payout slot BEFORE activation,
     * increments `termsVersion + 1`, resets all non-organizer members to PENDING so everyone
     * must review and accept the replacement, and sends invitations + notifications.
     */
    suspend fun replaceDeclinedMemberAndReviseTerms(
        circleId: String,
        declinedUserId: String,
        replacementMember: CircleMember,
        organizerId: String
    ): Result<Circle> {
        return try {
            val circle = getCircleById(circleId, organizerId)
                ?: return Result.failure(Exception("Circle not found."))
            if (circle.lifecycleStatus == "ACTIVE" || circle.lifecycleStatus == "COMPLETED") {
                return Result.failure(Exception("Active or completed Circles are locked on Soroban. Members cannot be changed."))
            }
            if (!isCircleOrganizer(circle, organizerId)) {
                return Result.failure(Exception("Only the Circle admin/organizer can replace a declined participant."))
            }
            if (replacementMember.isBanned) {
                return Result.failure(Exception("Cannot invite ${replacementMember.name}: Banned from joining Circles due to a failed payment."))
            }
            if (circle.members.any {
                    (it.userId.isNotBlank() && it.userId == replacementMember.userId) ||
                        it.name.equals(replacementMember.name, ignoreCase = true)
                }
            ) {
                return Result.failure(Exception("${replacementMember.name} is already in this Circle."))
            }

            val replacedMember = circle.members.find { m ->
                (declinedUserId.isNotBlank() && m.userId == declinedUserId) ||
                    m.name.equals(declinedUserId, ignoreCase = true) ||
                    (m.acceptanceStatus == "DECLINED" && declinedUserId.isBlank())
            }

            val nextVersion = circle.termsVersion + 1
            val updatedMembers = circle.members.mapIndexed { index, m ->
                val isDeclinedTarget = (declinedUserId.isNotBlank() && m.userId == declinedUserId) ||
                    m.name.equals(declinedUserId, ignoreCase = true) ||
                    (m.acceptanceStatus == "DECLINED" && declinedUserId.isBlank())
                val isOrg = m.userId == organizerId || isCircleOrganizer(circle, m.userId)
                when {
                    isDeclinedTarget -> replacementMember.copy(
                        payoutRound = index + 1,
                        payoutDate = m.payoutDate,
                        acceptanceStatus = "PENDING",
                        acceptedTermsVersion = 0,
                        declineReason = ""
                    )
                    isOrg -> m.copy(
                        payoutRound = index + 1,
                        acceptanceStatus = "ACCEPTED",
                        acceptedTermsVersion = nextVersion,
                        declineReason = ""
                    )
                    else -> m.copy(
                        payoutRound = index + 1,
                        acceptanceStatus = "PENDING",
                        acceptedTermsVersion = 0,
                        declineReason = ""
                    )
                }
            }

            val newTarget = circle.contributionPerMonth * updatedMembers.size
            val record = CircleTransactionRecord(
                id = "ctx_${System.currentTimeMillis()}",
                type = "TERMS_REVISED",
                title = "Replaced ${replacedMember?.name ?: "Declined Member"} with ${replacementMember.name} (Terms v$nextVersion)",
                memberName = circle.creatorName.ifBlank { "Organizer" },
                memberPhone = organizerId,
                roundNumber = 0,
                amount = newTarget,
                currency = circle.currency,
                status = "CONFIRMED",
                txHash = "REPLACE-MEMBER-V$nextVersion"
            )

            val updatedCircle = circle.copy(
                members = updatedMembers,
                memberCount = updatedMembers.size,
                maxMembers = updatedMembers.size,
                targetAmount = newTarget,
                termsVersion = nextVersion,
                lifecycleStatus = "AWAITING_ACCEPTANCE",
                isActive = false,
                transactionHistory = listOf(record) + circle.transactionHistory
            )
            updateCircle(updatedCircle)

            // Clean up replaced/declined member's user_circles entry so their stale v1 copy doesn't linger
            replacedMember?.userId?.takeIf { it.isNotBlank() && it != organizerId }?.let { removedUid ->
                runCatching {
                    usersCollection.document(removedUid)
                        .update("user_circles.$circleId", com.google.firebase.firestore.FieldValue.delete())
                        .await()
                }
            }

            // Re-send invitations + notifications to ALL non-organizer members (existing + replacement)
            updatedMembers.filter { it.userId != organizerId && !isCircleOrganizer(circle, it.userId) }.forEach { invitee ->
                val inviteeUid = invitee.userId.ifBlank { invitee.name }
                sendInvitation(
                    circleId = circle.id,
                    inviteeUserId = inviteeUid,
                    inviteeName = invitee.name,
                    inviteeInitials = invitee.initials,
                    inviterName = circle.creatorName.ifBlank { "Organizer" },
                    inviterScore = 800,
                    circleName = circle.name,
                    contributionAmount = circle.contributionPerMonth,
                    memberCount = updatedMembers.size,
                    termsVersion = nextVersion,
                    scheduleText = circle.targetDate,
                    currency = circle.currency
                )
                if (invitee.userId.isNotBlank()) {
                    val isNew = invitee.userId == replacementMember.userId
                    runCatching {
                        NotificationRepository.sendNotification(
                            userId = invitee.userId,
                            title = if (isNew) "Circle Invitation Received" else "Circle Roster Updated — Re-Acceptance Required",
                            message = if (isNew) {
                                "${circle.creatorName.ifBlank { "Organizer" }} invited you to join '${circle.name}' (Terms v$nextVersion). Review the 12 contract clauses and member roster to accept or decline."
                            } else {
                                "${circle.creatorName.ifBlank { "Organizer" }} replaced ${replacedMember?.name ?: "a declined member"} with ${replacementMember.name} in '${circle.name}'. Please review and accept Terms v$nextVersion before activation."
                            },
                            kind = "CIRCLE"
                        )
                    }
                }
            }

            Result.success(updatedCircle)
        } catch (e: Exception) {
            Log.e(TAG, "replaceDeclinedMemberAndReviseTerms failed", e)
            Result.failure(e)
        }
    }

    /**
     * Organizer adds another verified registered member BEFORE activation (e.g. when lacking members).
     * Increments `termsVersion + 1`, recalculates the scheduled payout (`contribution * newSize`),
     * and resets ALL other members (existing AND newly added) to `PENDING` so every participant
     * must explicitly review and accept the newly added member and revised Terms before activation.
     */
    suspend fun addMemberBeforeActivationAndReviseTerms(
        circleId: String,
        newMember: CircleMember,
        organizerId: String
    ): Result<Circle> {
        return try {
            val circle = getCircleById(circleId, organizerId)
                ?: return Result.failure(Exception("Circle not found."))
            if (circle.lifecycleStatus == "ACTIVE" || circle.lifecycleStatus == "COMPLETED") {
                return Result.failure(Exception("Active or completed Circles are locked on Soroban. You cannot add or modify members."))
            }
            if (!isCircleOrganizer(circle, organizerId)) {
                return Result.failure(Exception("Only the Circle admin/organizer can add participants before activation."))
            }
            if (newMember.isBanned) {
                return Result.failure(Exception("Cannot invite ${newMember.name}: Banned from joining Circles due to a failed payment."))
            }
            if (circle.members.any {
                    (it.userId.isNotBlank() && it.userId == newMember.userId) ||
                        it.name.equals(newMember.name, ignoreCase = true)
                }
            ) {
                return Result.failure(Exception("${newMember.name} is already a participant in this Circle."))
            }

            val nextVersion = circle.termsVersion + 1
            val combined = circle.members + newMember.copy(
                acceptanceStatus = "PENDING",
                acceptedTermsVersion = 0,
                declineReason = ""
            )
            val updatedMembers = combined.mapIndexed { index, m ->
                val isOrg = m.userId == organizerId || isCircleOrganizer(circle, m.userId)
                val keepDeclined = m.acceptanceStatus == "DECLINED"
                when {
                    isOrg -> m.copy(
                        payoutRound = index + 1,
                        acceptanceStatus = "ACCEPTED",
                        acceptedTermsVersion = nextVersion,
                        declineReason = ""
                    )
                    keepDeclined -> m.copy(
                        payoutRound = index + 1,
                        acceptanceStatus = "DECLINED",
                        acceptedTermsVersion = 0
                    )
                    else -> m.copy(
                        payoutRound = index + 1,
                        acceptanceStatus = "PENDING",
                        acceptedTermsVersion = 0,
                        declineReason = ""
                    )
                }
            }

            val newTarget = circle.contributionPerMonth * updatedMembers.size
            val anyDeclined = updatedMembers.any { it.acceptanceStatus == "DECLINED" }
            val nextLifecycle = if (anyDeclined) "DECLINED_REVISION_NEEDED" else "AWAITING_ACCEPTANCE"

            val record = CircleTransactionRecord(
                id = "ctx_${System.currentTimeMillis()}",
                type = "TERMS_REVISED",
                title = "Added ${newMember.name} (${updatedMembers.size} Members) — All Members Must Accept Terms v$nextVersion",
                memberName = circle.creatorName.ifBlank { "Organizer" },
                memberPhone = organizerId,
                roundNumber = 0,
                amount = newTarget,
                currency = circle.currency,
                status = "CONFIRMED",
                txHash = "ADD-MEMBER-V$nextVersion"
            )

            val updatedCircle = circle.copy(
                members = updatedMembers,
                memberCount = updatedMembers.size,
                maxMembers = updatedMembers.size,
                targetAmount = newTarget,
                termsVersion = nextVersion,
                lifecycleStatus = nextLifecycle,
                isActive = false,
                transactionHistory = listOf(record) + circle.transactionHistory
            )
            updateCircle(updatedCircle)

            // Re-send invitations + notifications to ALL non-organizer non-declined members
            updatedMembers
                .filter { it.userId != organizerId && !isCircleOrganizer(circle, it.userId) && it.acceptanceStatus != "DECLINED" }
                .forEach { invitee ->
                    val inviteeUid = invitee.userId.ifBlank { invitee.name }
                    sendInvitation(
                        circleId = circle.id,
                        inviteeUserId = inviteeUid,
                        inviteeName = invitee.name,
                        inviteeInitials = invitee.initials,
                        inviterName = circle.creatorName.ifBlank { "Organizer" },
                        inviterScore = 800,
                        circleName = circle.name,
                        contributionAmount = circle.contributionPerMonth,
                        memberCount = updatedMembers.size,
                        termsVersion = nextVersion,
                        scheduleText = circle.targetDate,
                        currency = circle.currency
                    )
                    if (invitee.userId.isNotBlank()) {
                        val isNew = invitee.userId == newMember.userId
                        runCatching {
                            NotificationRepository.sendNotification(
                                userId = invitee.userId,
                                title = if (isNew) "Circle Invitation Received" else "New Member Added — Acceptance Required",
                                message = if (isNew) {
                                    "${circle.creatorName.ifBlank { "Organizer" }} added you to '${circle.name}' (Terms v$nextVersion). Review the 12 contract clauses and member list to accept or decline."
                                } else {
                                    "${circle.creatorName.ifBlank { "Organizer" }} added ${newMember.name} to '${circle.name}' (${updatedMembers.size} total members). All members must review and accept the added user (Terms v$nextVersion) before activation."
                                },
                                kind = "CIRCLE"
                            )
                        }
                    }
                }

            Result.success(updatedCircle)
        } catch (e: Exception) {
            Log.e(TAG, "addMemberBeforeActivationAndReviseTerms failed", e)
            Result.failure(e)
        }
    }

    suspend fun cancelCircleProposal(circleId: String, organizerId: String): Result<Unit> {
        return try {
            val circle = getCircleById(circleId, organizerId)
            if (circle != null && (circle.lifecycleStatus == "ACTIVE" || circle.lifecycleStatus == "COMPLETED")) {
                return Result.failure(Exception("Active or completed Circles are locked on Soroban and cannot be cancelled."))
            }
            if (circle != null && !isCircleOrganizer(circle, organizerId)) {
                return Result.failure(Exception("Only the Circle admin/organizer can cancel this proposal."))
            }
            localCirclesCache.remove(circleId)
            val participantIds = buildSet {
                if (organizerId.isNotBlank()) add(organizerId)
                circle?.members?.forEach { if (it.userId.isNotBlank()) add(it.userId) }
            }
            for (uid in participantIds) {
                runCatching {
                    usersCollection.document(uid)
                        .update("user_circles.$circleId", com.google.firebase.firestore.FieldValue.delete())
                        .await()
                }
                if (uid != organizerId && circle != null) {
                    runCatching {
                        NotificationRepository.sendNotification(
                            userId = uid,
                            title = "Circle Proposal Cancelled",
                            message = "${circle.creatorName.ifBlank { "Organizer" }} cancelled the pre-activation proposal for '${circle.name}'.",
                            kind = "CIRCLE"
                        )
                    }
                }
            }
            runCatching {
                kotlinx.coroutines.withTimeout(3000) {
                    circlesCollection.document(circleId).delete().await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "cancelCircleProposal fallback", e)
            Result.success(Unit)
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
                return Result.failure(Exception("Active or completed Circles are locked on Soroban. Members cannot be removed or modified."))
            }
            if (!isCircleOrganizer(circle, organizerId)) {
                return Result.failure(Exception("Only the Circle admin/organizer can revise pre-activation terms."))
            }

            val removedMember = circle.members.find { it.userId == declinedUserId || it.name.equals(declinedUserId, ignoreCase = true) || (it.acceptanceStatus == "DECLINED" && declinedUserId.isBlank()) }
            val remaining = circle.members.filterNot {
                it.userId == declinedUserId ||
                    it.name.equals(declinedUserId, ignoreCase = true) ||
                    (it.acceptanceStatus == "DECLINED" && declinedUserId.isBlank())
            }
            if (remaining.size < 2) {
                return Result.failure(Exception("A Circle requires at least 2 participants. Please add or replace a member, or cancel and create a new Circle proposal."))
            }

            val nextVersion = circle.termsVersion + 1
            val reindexedMembers = remaining.mapIndexed { index, m ->
                val isOrg = m.userId == organizerId || isCircleOrganizer(circle, m.userId)
                m.copy(
                    payoutRound = index + 1,
                    acceptanceStatus = if (isOrg) "ACCEPTED" else "PENDING",
                    acceptedTermsVersion = if (isOrg) nextVersion else 0,
                    declineReason = ""
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

            // Clean up removed member's user_circles entry
            removedMember?.userId?.takeIf { it.isNotBlank() && it != organizerId }?.let { removedUid ->
                runCatching {
                    usersCollection.document(removedUid)
                        .update("user_circles.$circleId", com.google.firebase.firestore.FieldValue.delete())
                        .await()
                }
            }

            // Re-send invitations + notifications for the new termsVersion to all non-organizer members
            reindexedMembers.filter { it.userId != organizerId && !isCircleOrganizer(circle, it.userId) }.forEach { invitee ->
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
                if (invitee.userId.isNotBlank()) {
                    runCatching {
                        NotificationRepository.sendNotification(
                            userId = invitee.userId,
                            title = "Circle Terms Revised — Re-Acceptance Required",
                            message = "${circle.creatorName.ifBlank { "Organizer" }} removed ${removedMember?.name ?: "a declined member"} and issued Revised Terms v$nextVersion for '${circle.name}' (${reindexedMembers.size} members). Please review and accept.",
                            kind = "CIRCLE"
                        )
                    }
                }
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
            if (circle.lifecycleStatus == "ACTIVE" || circle.lifecycleStatus == "COMPLETED") {
                return Result.failure(Exception("Circle is already active and locked on Soroban."))
            }
            if (!isCircleOrganizer(circle, organizerId)) {
                return Result.failure(Exception("Only the Circle admin/organizer can activate and lock the Circle."))
            }
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

            // Notify all non-organizer members that Circle is now active & permanently locked
            resetMembers.filter { it.userId.isNotBlank() && it.userId != organizerId }.forEach { member ->
                runCatching {
                    NotificationRepository.sendNotification(
                        userId = member.userId,
                        title = "Circle Activated & Locked",
                        message = "'${circle.name}' is now active and locked on Soroban. Round 1 contributions are open. Members cannot leave or change details until completion.",
                        kind = "CIRCLE"
                    )
                }
            }

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

            val nowTs = System.currentTimeMillis()
            val ledgerSeq = 4892100L + (nowTs % 890L)
            val newMemberScore = (member.pundarScore + com.example.pundarapp.ui.data.CircleContractRules.POINTS_ON_TIME_CONTRIBUTION).coerceAtMost(999)
            val updatedMembers = circle.members.map { m ->
                if (m.userId == member.userId) {
                    m.copy(
                        status = ContributionStatus.PAID,
                        amount = circle.contributionPerMonth,
                        contributionTxHash = txHash,
                        paidAtTimestamp = nowTs,
                        ledgerSequence = ledgerSeq,
                        pundarScore = newMemberScore
                    )
                } else m
            }
            val contributedCount = updatedMembers.count { it.status == ContributionStatus.PAID }
            val newPooled = contributedCount * circle.contributionPerMonth

            val record = CircleTransactionRecord(
                id = "ctx_$nowTs",
                type = "CONTRIBUTION",
                title = "Round ${circle.currentRound} Contribution (+${com.example.pundarapp.ui.data.CircleContractRules.POINTS_ON_TIME_CONTRIBUTION} PUNDAR Pts · Ledger #$ledgerSeq)",
                memberName = member.name,
                memberPhone = member.userId,
                roundNumber = circle.currentRound,
                amount = circle.contributionPerMonth,
                currency = circle.currency,
                status = "CONFIRMED",
                txHash = txHash,
                timestamp = nowTs
            )

            val updatedCircle = circle.copy(
                members = updatedMembers,
                savedAmount = newPooled,
                transactionHistory = listOf(record) + circle.transactionHistory
            )
            updateCircle(updatedCircle)
            AuthRepository.updateUserCircleStanding(
                phoneOrId = member.userId,
                scoreDelta = com.example.pundarapp.ui.data.CircleContractRules.POINTS_ON_TIME_CONTRIBUTION
            )
            Result.success(updatedCircle)
        } catch (e: Exception) {
            Log.e(TAG, "recordMemberContribution failed", e)
            Result.failure(e)
        }
    }

    /**
     * Marks a member as having failed their scheduled payment for the active round:
     * - Sets member status to `OVERDUE` and `isBanned = true`
     * - Decreases member's PUNDAR Points (`-50 pts`)
     * - Bans the member from joining or creating other Circles until rehabilitated
     * - Records the failed payment with date, time, amount, and `FAILED` status in `transactionHistory`
     */
    suspend fun markMemberFailedPayment(
        circleId: String,
        targetUserId: String,
        organizerId: String
    ): Result<Circle> {
        return try {
            val circle = getCircleById(circleId, organizerId)
                ?: return Result.failure(Exception("Circle not found."))
            val target = circle.members.find { it.userId == targetUserId || it.name.equals(targetUserId, ignoreCase = true) }
                ?: return Result.failure(Exception("Member not found in Circle."))
            if (target.status == ContributionStatus.PAID) {
                return Result.failure(Exception("${target.name} has already paid for Round ${circle.currentRound}."))
            }

            val nowTs = System.currentTimeMillis()
            val penalizedScore = (target.pundarScore + com.example.pundarapp.ui.data.CircleContractRules.POINTS_FAILED_PAYMENT_PENALTY).coerceAtLeast(300)
            val updatedMembers = circle.members.map { m ->
                if (m.userId == target.userId && m.name == target.name) {
                    m.copy(
                        status = ContributionStatus.OVERDUE,
                        isBanned = true,
                        pundarScore = penalizedScore
                    )
                } else m
            }

            val record = CircleTransactionRecord(
                id = "ctx_fail_$nowTs",
                type = "FAILED_PAYMENT",
                title = "Missed Round ${circle.currentRound} Due Date (${com.example.pundarapp.ui.data.CircleContractRules.POINTS_FAILED_PAYMENT_PENALTY} PUNDAR Pts · Banned from Other Circles)",
                memberName = target.name,
                memberPhone = target.userId,
                roundNumber = circle.currentRound,
                amount = circle.contributionPerMonth,
                currency = circle.currency,
                status = "FAILED",
                txHash = "MISSED-DUE-R${circle.currentRound}-${target.userId.takeLast(4)}",
                timestamp = nowTs
            )

            val updatedCircle = circle.copy(
                members = updatedMembers,
                transactionHistory = listOf(record) + circle.transactionHistory
            )
            updateCircle(updatedCircle)
            AuthRepository.updateUserCircleStanding(
                phoneOrId = target.userId,
                scoreDelta = com.example.pundarapp.ui.data.CircleContractRules.POINTS_FAILED_PAYMENT_PENALTY,
                setBanned = true,
                banReason = "Failed Round ${circle.currentRound} payment in ${circle.name}"
            )
            Result.success(updatedCircle)
        } catch (e: Exception) {
            Log.e(TAG, "markMemberFailedPayment failed", e)
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
                    paidAtTimestamp = if (isAllCompleted) m.paidAtTimestamp else 0L,
                    ledgerSequence = if (isAllCompleted) m.ledgerSequence else 0L,
                    hasReceivedPayout = m.hasReceivedPayout || justPaid,
                    payoutTxHash = if (justPaid) txHash else m.payoutTxHash
                )
            }

            if (isAllCompleted) {
                orderedMembers.filter { !it.isBanned && it.userId.isNotBlank() }.forEach { m ->
                    AuthRepository.updateUserCircleStanding(
                        phoneOrId = m.userId,
                        scoreDelta = com.example.pundarapp.ui.data.CircleContractRules.POINTS_CIRCLE_COMPLETION
                    )
                }
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
