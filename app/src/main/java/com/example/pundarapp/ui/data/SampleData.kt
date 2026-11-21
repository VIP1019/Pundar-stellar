package com.example.pundarapp.ui.data

// ── Data Classes ────────────────────────────────────────────────

data class PundarUser(
    val name: String,
    val initials: String,
    val pundarScore: Int,
    val avatarColor: Long = 0xFF0052CC
)

data class GroupBill(
    val id: String,
    val name: String,
    val totalAmount: Double,
    val memberCount: Int,
    val status: BillStatus,
    val date: String,
    val yourShare: Double,
    val members: List<BillMember> = emptyList()
)

enum class BillStatus { SETTLED, PENDING, PARTIAL }

data class BillMember(
    val name: String,
    val username: String,
    val initials: String,
    val amount: Double,
    val avatarColor: Long = 0xFF6B7280
)

data class CircleTransactionRecord(
    val id: String = "ctx_${System.currentTimeMillis()}",
    val type: String = "CONTRIBUTION", // "PROPOSAL_CREATED", "TERMS_ACCEPTED", "TERMS_DECLINED", "TERMS_REVISED", "CIRCLE_ACTIVATION", "CONTRIBUTION", "PAYOUT", "VALIDATION_CHECK"
    val title: String = "",
    val memberName: String = "",
    val memberPhone: String = "",
    val roundNumber: Int = 1,
    val amount: Double = 0.0,
    val currency: String = "XLM",
    val status: String = "CONFIRMED", // "CONFIRMED", "REJECTED", "FAILED"
    val txHash: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    @get:com.google.firebase.firestore.Exclude
    val round: Int
        get() = roundNumber

    @get:com.google.firebase.firestore.Exclude
    val formattedTime: String
        get() = java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
}

data class Circle(
    val id: String = "",
    val name: String = "",
    val targetAmount: Double = 0.0,
    val savedAmount: Double = 0.0,
    val targetDate: String = "",
    val memberCount: Int = 0,
    val contributionPerMonth: Double = 0.0,
    val members: List<CircleMember> = emptyList(),
    val isActive: Boolean = true,
    val currency: String = "XLM",
    val escrowAddress: String = "CBGZJWLQHJSRWFS2HMBD5SRWY5BAIB76PS7S5MGFJXNN34NF4QDSE3Y3",
    val escrowNetwork: String = "Stellar Testnet · Soroban",
    val payoutFrequencyValue: Int = 1,
    val payoutFrequencyUnit: String = "Months",
    val maxMembers: Int = 10,
    val creatorId: String = "",
    val creatorName: String = "",
    val cycleStatus: CycleStatus = CycleStatus.NOT_STARTED,
    val allowedInviteMethods: List<InviteMethod> = InviteMethod.values().toList(),
    val penaltyAmount: Double = 0.0,
    val penaltyEnabled: Boolean = false,
    val cycleStartDate: String = "",
    val cycleEndDate: String = "",
    val currentRecipientIndex: Int = 0,
    val payoutOrderType: String = "Deterministic Fixed Rotation",
    // ── SOW Deterministic Circle Lifecycle Fields ──
    val lifecycleStatus: String = "ACTIVE", // "AWAITING_ACCEPTANCE", "DECLINED_REVISION_NEEDED", "READY_TO_ACTIVATE", "ACTIVE", "COMPLETED"
    val termsVersion: Int = 1,
    val currentRound: Int = 1,
    val sorobanCircleId: Long = 0L,
    val activationTxHash: String = "",
    val paidRounds: List<Int> = emptyList(),
    val transactionHistory: List<CircleTransactionRecord> = emptyList()
) {
    @get:com.google.firebase.firestore.Exclude
    val isFull: Boolean get() = memberCount >= maxMembers
    @get:com.google.firebase.firestore.Exclude
    val remainingSlots: Int get() = (maxMembers - memberCount).coerceAtLeast(0)
    @get:com.google.firebase.firestore.Exclude
    val scheduledPayoutAmount: Double get() = contributionPerMonth * members.size.coerceAtLeast(1)
    @get:com.google.firebase.firestore.Exclude
    val allMembersAcceptedCurrentTerms: Boolean get() =
        members.isNotEmpty() && members.all { it.acceptanceStatus == "ACCEPTED" && it.acceptedTermsVersion == termsVersion }
    @get:com.google.firebase.firestore.Exclude
    val hasDeclinedMember: Boolean get() =
        members.any { it.acceptanceStatus == "DECLINED" }
    @get:com.google.firebase.firestore.Exclude
    val allCurrentRoundContributionsComplete: Boolean get() =
        lifecycleStatus == "ACTIVE" && members.isNotEmpty() && members.all { it.status == ContributionStatus.PAID }
    @get:com.google.firebase.firestore.Exclude
    val isCurrentRoundPayoutEligible: Boolean get() =
        allCurrentRoundContributionsComplete && !paidRounds.contains(currentRound) && currentRound in 1..members.size
    @get:com.google.firebase.firestore.Exclude
    val currentRoundRecipient: CircleMember? get() =
        members.find { it.payoutRound == currentRound } ?: members.getOrNull(currentRound - 1)

    fun applyMemberTermsResponse(userId: String, accept: Boolean, forTermsVersion: Int): Result<Circle> {
        if (lifecycleStatus == "ACTIVE" || lifecycleStatus == "COMPLETED") {
            return Result.failure(IllegalStateException("Circle is already locked and active."))
        }
        if (termsVersion != forTermsVersion) {
            return Result.failure(
                IllegalArgumentException("Circle terms were revised to v$termsVersion. Please review the latest terms.")
            )
        }
        val memberExists = members.any { it.userId == userId || it.isYou }
        if (!memberExists) {
            return Result.failure(IllegalArgumentException("User is not an invited participant of this Circle."))
        }

        val newStatus = if (accept) "ACCEPTED" else "DECLINED"
        var responderName = userId
        val updatedMembers = members.map { m ->
            if (m.userId == userId || (m.userId.isBlank() && m.isYou)) {
                responderName = m.name
                m.copy(
                    acceptanceStatus = newStatus,
                    acceptedTermsVersion = termsVersion
                )
            } else m
        }

        val allAccepted = updatedMembers.all {
            it.acceptanceStatus == "ACCEPTED" && it.acceptedTermsVersion == termsVersion
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
            title = if (accept) "Accepted Circle Terms (v$termsVersion)"
            else "Declined Circle Invitation (v$termsVersion)",
            memberName = responderName,
            memberPhone = userId,
            roundNumber = 0,
            amount = contributionPerMonth,
            currency = currency,
            status = if (accept) "CONFIRMED" else "DECLINED",
            txHash = "CONSENT-V$termsVersion-${userId.takeLast(4)}"
        )

        return Result.success(
            copy(
                members = updatedMembers,
                lifecycleStatus = nextLifecycle,
                isActive = false,
                transactionHistory = listOf(record) + transactionHistory
            )
        )
    }

    fun applyDeclinedMemberRemovalAndReviseTerms(declinedUserId: String, organizerId: String): Result<Circle> {
        if (lifecycleStatus == "ACTIVE" || lifecycleStatus == "COMPLETED") {
            return Result.failure(IllegalStateException("Cannot remove or substitute members after Circle activation."))
        }
        if (creatorId != organizerId) {
            return Result.failure(IllegalArgumentException("Only the Circle organizer can revise pre-activation terms."))
        }

        val remaining = members.filterNot {
            it.userId == declinedUserId || (it.acceptanceStatus == "DECLINED" && declinedUserId.isBlank())
        }
        if (remaining.size < 2) {
            return Result.failure(
                IllegalArgumentException("A Circle requires at least 2 participants. Please add another member or create a new Circle proposal.")
            )
        }

        val nextVersion = termsVersion + 1
        val reindexedMembers = remaining.mapIndexed { index, m ->
            val isOrg = m.userId == organizerId
            m.copy(
                payoutRound = index + 1,
                acceptanceStatus = if (isOrg) "ACCEPTED" else "PENDING",
                acceptedTermsVersion = if (isOrg) nextVersion else m.acceptedTermsVersion
            )
        }

        val newTarget = contributionPerMonth * reindexedMembers.size
        val allAccepted = reindexedMembers.all {
            it.acceptanceStatus == "ACCEPTED" && it.acceptedTermsVersion == nextVersion
        }

        val record = CircleTransactionRecord(
            id = "ctx_${System.currentTimeMillis()}",
            type = "TERMS_REVISED",
            title = "Revised Final Terms Issued (v$nextVersion) — ${reindexedMembers.size} Members",
            memberName = creatorName.ifBlank { "Organizer" },
            memberPhone = organizerId,
            roundNumber = 0,
            amount = newTarget,
            currency = currency,
            status = "CONFIRMED",
            txHash = "REVISED-TERMS-V$nextVersion"
        )

        return Result.success(
            copy(
                members = reindexedMembers,
                memberCount = reindexedMembers.size,
                maxMembers = reindexedMembers.size,
                targetAmount = newTarget,
                termsVersion = nextVersion,
                lifecycleStatus = if (allAccepted) "READY_TO_ACTIVATE" else "AWAITING_ACCEPTANCE",
                isActive = false,
                transactionHistory = listOf(record) + transactionHistory
            )
        )
    }

    fun applyActivation(organizerId: String, newSorobanCircleId: Long, txHash: String, contractId: String): Result<Circle> {
        if (lifecycleStatus == "ACTIVE" || lifecycleStatus == "COMPLETED") {
            return Result.failure(IllegalStateException("Circle is already locked and active."))
        }
        if (!allMembersAcceptedCurrentTerms) {
            return Result.failure(IllegalStateException("Cannot activate: Not all participants have accepted Terms v$termsVersion."))
        }

        val resetMembers = members.sortedBy { it.payoutRound }.mapIndexed { idx, m ->
            m.copy(
                payoutRound = idx + 1,
                status = ContributionStatus.PENDING,
                amount = 0.0,
                contributionTxHash = "",
                hasReceivedPayout = false,
                payoutTxHash = ""
            )
        }
        val derivedPayout = contributionPerMonth * resetMembers.size

        val record = CircleTransactionRecord(
            id = "ctx_${System.currentTimeMillis()}",
            type = "CIRCLE_ACTIVATION",
            title = "Circle Activated & Locked on Soroban (Terms v$termsVersion)",
            memberName = creatorName.ifBlank { "Organizer" },
            memberPhone = organizerId,
            roundNumber = 1,
            amount = derivedPayout,
            currency = currency,
            status = "CONFIRMED",
            txHash = txHash
        )

        return Result.success(
            copy(
                members = resetMembers,
                memberCount = resetMembers.size,
                maxMembers = resetMembers.size,
                targetAmount = derivedPayout,
                savedAmount = 0.0,
                isActive = true,
                lifecycleStatus = "ACTIVE",
                currentRound = 1,
                currentRecipientIndex = 0,
                sorobanCircleId = newSorobanCircleId,
                activationTxHash = txHash,
                escrowAddress = contractId,
                escrowNetwork = "Stellar Testnet · Soroban (#$newSorobanCircleId)",
                transactionHistory = listOf(record) + transactionHistory
            )
        )
    }

    fun applyMemberContribution(userId: String, amountContributed: Double = contributionPerMonth, txHash: String): Result<Circle> {
        if (lifecycleStatus != "ACTIVE") {
            return Result.failure(IllegalStateException("Circle is not active."))
        }
        val member = members.find { it.userId == userId || (it.userId.isBlank() && it.isYou) }
            ?: return Result.failure(IllegalArgumentException("You are not an enrolled member of this Circle."))
        if (kotlin.math.abs(amountContributed - contributionPerMonth) > 1e-6 || amountContributed <= 0.0) {
            return Result.failure(
                IllegalArgumentException("Contribution must equal exact fixed amount ($contributionPerMonth $currency).")
            )
        }
        if (member.status == ContributionStatus.PAID) {
            return Result.failure(IllegalStateException("You have already contributed for Round $currentRound."))
        }

        val updatedMembers = members.map { m ->
            if (m.userId == member.userId) {
                m.copy(
                    status = ContributionStatus.PAID,
                    amount = contributionPerMonth,
                    contributionTxHash = txHash
                )
            } else m
        }
        val contributedCount = updatedMembers.count { it.status == ContributionStatus.PAID }
        val newPooled = contributedCount * contributionPerMonth

        val record = CircleTransactionRecord(
            id = "ctx_${System.currentTimeMillis()}",
            type = "CONTRIBUTION",
            title = "Round $currentRound Contribution",
            memberName = member.name,
            memberPhone = member.userId,
            roundNumber = currentRound,
            amount = contributionPerMonth,
            currency = currency,
            status = "CONFIRMED",
            txHash = txHash
        )

        return Result.success(
            copy(
                members = updatedMembers,
                savedAmount = newPooled,
                transactionHistory = listOf(record) + transactionHistory
            )
        )
    }

    fun applyRoundPayout(callerUserId: String, callerName: String, txHash: String): Result<Circle> {
        if (lifecycleStatus != "ACTIVE") {
            return Result.failure(IllegalStateException("Circle is not active."))
        }
        val isEnrolledCaller = members.any { it.userId == callerUserId || (it.userId.isBlank() && it.isYou) }
        if (!isEnrolledCaller) {
            return Result.failure(IllegalArgumentException("Only enrolled Circle members can invoke the scheduled payout."))
        }
        if (!allCurrentRoundContributionsComplete) {
            return Result.failure(IllegalStateException("Payout rejected: Not all members have contributed for Round $currentRound."))
        }
        if (paidRounds.contains(currentRound)) {
            return Result.failure(IllegalStateException("Duplicate payout rejected: Round $currentRound has already been paid."))
        }

        val orderedMembers = members.sortedBy { it.payoutRound }
        val recipient = orderedMembers.getOrNull(currentRound - 1)
            ?: return Result.failure(IllegalStateException("Invalid round recipient index."))
        val exactPayoutAmount = scheduledPayoutAmount

        val paidRound = currentRound
        val newPaidRounds = (paidRounds + paidRound).distinct()
        val isAllCompleted = newPaidRounds.size >= orderedMembers.size

        val nextRound = if (isAllCompleted) paidRound else paidRound + 1
        val nextMembers = orderedMembers.map { m ->
            val justPaid = m.userId == recipient.userId
            m.copy(
                status = if (isAllCompleted) ContributionStatus.PAID else ContributionStatus.PENDING,
                amount = if (isAllCompleted) contributionPerMonth else 0.0,
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
            currency = currency,
            status = "CONFIRMED",
            txHash = txHash
        )

        return Result.success(
            copy(
                members = nextMembers,
                savedAmount = if (isAllCompleted) exactPayoutAmount else 0.0,
                currentRound = nextRound,
                currentRecipientIndex = (nextRound - 1).coerceAtLeast(0),
                paidRounds = newPaidRounds,
                lifecycleStatus = if (isAllCompleted) "COMPLETED" else "ACTIVE",
                isActive = !isAllCompleted,
                transactionHistory = listOf(payoutRecord) + transactionHistory
            )
        )
    }
}

data class CircleMember(
    val userId: String = "",
    val name: String = "",
    val initials: String = "",
    val sharePercent: Int = 0,
    val amount: Double = 0.0,
    val status: ContributionStatus = ContributionStatus.PENDING,
    val isYou: Boolean = false,
    val payoutDate: String = "",
    val avatarColor: Long = 0xFF6B7280,
    val stellarPublicKey: String = "",
    val payoutRound: Int = 1,
    val acceptanceStatus: String = "ACCEPTED", // "ACCEPTED", "PENDING", "DECLINED"
    val acceptedTermsVersion: Int = 1,
    val declineReason: String = "",
    val contributionTxHash: String = "",
    val hasReceivedPayout: Boolean = false,
    val payoutTxHash: String = "",
    val pundarScore: Int = 800,
    val isBanned: Boolean = false,
    val paidAtTimestamp: Long = 0L,
    val ledgerSequence: Long = 0L
) {
    @get:com.google.firebase.firestore.Exclude
    val formattedPaidTime: String
        get() = if (paidAtTimestamp > 0L) {
            java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault())
                .format(java.util.Date(paidAtTimestamp))
        } else ""
}

data class CircleContractClause(
    val code: String,
    val title: String,
    val description: String
)

object CircleContractRules {
    const val POINTS_ON_TIME_CONTRIBUTION = 15
    const val POINTS_CIRCLE_COMPLETION = 25
    const val POINTS_FAILED_PAYMENT_PENALTY = -50
    const val MIN_REHABILITATION_SCORE = 750
    const val NEW_USER_MIN_RESERVE_XLM = 2.0

    fun getClauses(
        contributionFormatted: String = "Fixed Amount",
        scheduleText: String = "Bi-weekly / Monthly",
        payoutOrderType: String = "Predetermined Fixed Rotation",
        payoutFormatted: String = "Contribution x Members"
    ): List<CircleContractClause> = listOf(
        CircleContractClause(
            code = "a",
            title = "Voluntary Participation",
            description = "Every invited participant joins voluntarily and must explicitly review and accept the complete proposed Circle terms and member roster before activation."
        ),
        CircleContractClause(
            code = "b",
            title = "Fixed Membership",
            description = "Once all members accept the final terms and the Circle is activated on the Soroban smart contract, the member list is permanently locked for the entire cycle."
        ),
        CircleContractClause(
            code = "c",
            title = "Fixed Contribution Amount",
            description = "Every participating member must submit the exact same fixed contribution ($contributionFormatted) each round. Partial or custom amounts are rejected."
        ),
        CircleContractClause(
            code = "d",
            title = "Contribution Schedule",
            description = "Contributions follow the agreed schedule ($scheduleText) and must be submitted on or before the round due date. Missed payments reduce PUNDAR points and trigger a Circle participation ban."
        ),
        CircleContractClause(
            code = "e",
            title = "Predetermined Payout Order",
            description = "The payout rotation ($payoutOrderType) is assigned by the organizer or randomized with participant agreement prior to activation, assigning each member to one specific round."
        ),
        CircleContractClause(
            code = "f",
            title = "Payout Amount",
            description = "The scheduled payout amount per round ($payoutFormatted) equals the fixed contribution amount multiplied by the number of participating members locked at activation."
        ),
        CircleContractClause(
            code = "g",
            title = "Payout Eligibility",
            description = "Only the member assigned to the active round in the locked rotation is eligible for the scheduled payout, and only after 100% of required contributions for that round are recorded."
        ),
        CircleContractClause(
            code = "h",
            title = "Smart Contract Enforcement",
            description = "The Soroban smart contract independently validates active round state, contribution completeness, scheduled recipient identity, exact payout amount, and payout-only-once rules."
        ),
        CircleContractClause(
            code = "i",
            title = "Cancellation and Changes",
            description = "If an invited member declines before activation, the Circle remains inactive; the organizer may replace the participant, revise terms for remaining members, or create a new proposal. After activation, no terms or recipient order can be revised by anyone until completion."
        ),
        CircleContractClause(
            code = "j",
            title = "Wallet Authorization and Security",
            description = "All contract activations, round contributions, and payout invocations require explicit non-custodial Stellar wallet authorization signed with the member's 4-digit MPIN."
        ),
        CircleContractClause(
            code = "k",
            title = "Transaction Transparency",
            description = "All recorded transactions, including members who paid and did not pay, with dates, times, amounts, ledger numbers, and statuses, are visible to all Circle participants."
        ),
        CircleContractClause(
            code = "l",
            title = "Testnet Limitation",
            description = "All PUNDAR Circle smart contract executions, escrow settlements, and ledger verifications operate on the Stellar Testnet."
        )
    )
}

enum class ContributionStatus { PAID, PENDING, OVERDUE }

enum class CycleStatus { NOT_STARTED, ACTIVE, PAUSED, COMPLETED }

enum class InviteMethod { QR_CODE, SHARE_LINK, USERNAME, PHONE_NUMBER }

data class CircleInvitation(
    val id: String = "",
    val circleId: String = "",
    val circleName: String = "",
    val goal: String = "",
    val inviterName: String = "",
    val inviterScore: Int = 0,
    val inviterCirclesCompleted: Int = 0,
    val targetAmount: Double = 0.0,
    val fundedPercent: Int = 0,
    val monthlyContribution: Double = 0.0,
    val memberCount: Int = 0,
    val maxMembers: Int = 0,
    val inviterInitials: String = "",
    val inviterAvatarColor: Long = 0xFF0052CC,
    val termsVersion: Int = 1,
    val scheduleText: String = "Every 1 Months",
    val currency: String = "XLM"
)

data class CircleJoinRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val circleId: String = ""
)

enum class RequestType { BILL_INVITE, CIRCLE_INVITE, CIRCLE_JOIN_REQUEST }

data class AppRequest(
    val id: String,
    val type: RequestType,
    val title: String,
    val subtitle: String,
    val date: String,
    val payload: Any // Can cast to BillInvitation, CircleInvitation, or CircleJoinRequest based on type
)

data class BillInvitation(
    val id: String = "",
    val billId: String = "",
    val billName: String = "",
    val inviterName: String = "",
    val inviterPhone: String = "",
    val inviteePhone: String = "",
    val yourShare: Double = 0.0,
    val totalAmount: Double = 0.0,
    val currency: String = "XLM"
)

data class Portfolio(
    val totalValue: Double,
    val totalReturnPercent: Double,
    val totalReturnAmount: Double,
    val phEquitiesPercent: Int,
    val fixedIncomePercent: Int,
    val holdings: List<StockHolding>,
    val activities: List<PortfolioActivity>
)

data class StockHolding(
    val ticker: String,
    val companyName: String,
    val sector: String,
    val exchange: String,
    val shares: Double,
    val value: Double,
    val returnPercent: Double,
    val currentPrice: Double,
    val priceChange: Double,
    val priceChangePercent: Double,
    val averageCost: Double,
    val marketCap: String,
    val peRatio: String,
    val divYield: String,
    val high52w: Double,
    val low52w: Double,
    val volume: String,
    val description: String,
    val priceHistory: List<Float> = emptyList()
)

data class PortfolioActivity(
    val type: ActivityType,
    val description: String,
    val amount: Double,
    val date: String,
    val isPositive: Boolean = true
)

enum class ActivityType { AUTO_SWEEP, DIVIDEND, PURCHASE, ROUND_UP, PAYOUT, DEPOSIT, WITHDRAWAL }

data class RoundUpInvestment(
    val reference: String,
    val sourceReference: String,
    val sourceAmount: Double,
    val roundUpAmount: Double,
    val convertedAmount: Double,
    val ticker: String,
    val companyName: String,
    val fractionalShares: Double,
    val pricePerShare: Double,
    val provider: String,
    val status: String,
    val createdAt: Long
)

data class RoundUpSettings(
    val isEnabled: Boolean = false,
    val threshold: Double = 100.0,
    val roundUpMultiplier: Int = 1,
    val targetStocks: List<String> = listOf("AC", "SMPH", "BDO", "JFC"),
    val totalAccumulated: Double = 0.0,
    val totalInvested: Double = 0.0,
    val roundUpCount: Int = 0
)

data class HomeActivity(
    val icon: String, // icon name
    val title: String,
    val subtitle: String,
    val amount: String,
    val isPositive: Boolean,
    val module: String, // "Pay", "Circle", "Grow"
    val timestamp: Long = System.currentTimeMillis(),
    val referenceId: String = "TXN-${System.currentTimeMillis()}"
)

// ── Sample Data ─────────────────────────────────────────────────

object SampleData {

    val currentUser = PundarUser(
        name = "Tom",
        initials = "T",
        pundarScore = 850
    )

    // ── Pay ─────────────────────────────────────────────
    val recentBills = emptyList<GroupBill>()
    val recentContacts = emptyList<BillMember>()

    // ── Circle ──────────────────────────────────────────
    val circles = emptyList<Circle>()
    val circleInvitation: CircleInvitation? = null

    // ── Grow ────────────────────────────────────────────
    
    val roundUpSettings = RoundUpSettings()

    val portfolio = Portfolio(
        totalValue = 0.0,
        totalReturnPercent = 0.0,
        totalReturnAmount = 0.0,
        phEquitiesPercent = 0,
        fixedIncomePercent = 0,
        holdings = listOf(
            StockHolding(
                ticker = "AC",
                companyName = "Ayala Corp",
                sector = "Real Estate & Conglomerates",
                exchange = "Philippine Stock Exchange",
                shares = 0.0,
                value = 0.0,
                returnPercent = 0.0,
                currentPrice = 850.50,
                priceChange = 12.50,
                priceChangePercent = 1.49,
                averageCost = 0.0,
                marketCap = "₱ 520.4B",
                peRatio = "14.2x",
                divYield = "2.8%",
                high52w = 910.00,
                low52w = 650.00,
                volume = "1.2M",
                description = "Ayala Corporation is the oldest and one of the largest conglomerates in real estate, banking, telecommunications, and power. The company aims to build sustainable businesses that contribute to national development, reflecting the spirit of ongoing progress and establishment.",
                priceHistory = listOf(
                    720f, 735f, 728f, 745f, 752f, 740f, 758f, 770f, 765f, 780f,
                    792f, 785f, 800f, 810f, 805f, 818f, 825f, 830f, 838f, 842f,
                    835f, 840f, 845f, 848f, 850f, 852f, 855f, 848f, 850f, 850.5f
                )
            ),
            StockHolding(
                ticker = "MER",
                companyName = "Meralco",
                sector = "Utilities",
                exchange = "Philippine Stock Exchange",
                shares = 0.0,
                value = 0.0,
                returnPercent = 0.0,
                currentPrice = 350.00,
                priceChange = 5.00,
                priceChangePercent = 1.44,
                averageCost = 0.0,
                marketCap = "₱ 394.0B",
                peRatio = "12.1x",
                divYield = "5.1%",
                high52w = 380.00,
                low52w = 290.00,
                volume = "850K",
                description = "Manila Electric Company (Meralco) is the largest private sector electric distribution utility company in the Philippines.",
                priceHistory = listOf(
                    320f, 325f, 330f, 335f, 340f, 345f, 350f, 348f, 352f, 350f
                )
            ),
            StockHolding(
                ticker = "SMPH",
                companyName = "SM Prime",
                sector = "Real Estate",
                exchange = "Philippine Stock Exchange",
                shares = 0.0,
                value = 0.0,
                returnPercent = 0.0,
                currentPrice = 33.00,
                priceChange = 0.50,
                priceChangePercent = 1.54,
                averageCost = 0.0,
                marketCap = "₱ 950.2B",
                peRatio = "25.1x",
                divYield = "1.2%",
                high52w = 38.00,
                low52w = 28.50,
                volume = "15.8M",
                description = "SM Prime Holdings, Inc. is the largest integrated property developer in Southeast Asia.",
                priceHistory = listOf(
                    30f, 30.5f, 31f, 30.8f, 31.5f, 32f, 31.8f, 32.2f, 32.5f, 32.8f,
                    32.5f, 32.8f, 33f, 32.8f, 33f, 33.2f, 33f, 32.8f, 33f, 33f
                )
            ),
            StockHolding(
                ticker = "BDO",
                companyName = "BDO Unibank",
                sector = "Banking",
                exchange = "Philippine Stock Exchange",
                shares = 0.0,
                value = 0.0,
                returnPercent = 0.0,
                currentPrice = 150.00,
                priceChange = -0.80,
                priceChangePercent = -0.53,
                averageCost = 0.0,
                marketCap = "₱ 660.1B",
                peRatio = "10.8x",
                divYield = "3.1%",
                high52w = 168.00,
                low52w = 132.00,
                volume = "2.3M",
                description = "BDO Unibank, Inc. is the largest bank in the Philippines in terms of total assets.",
                priceHistory = listOf(
                    155f, 156f, 154f, 153f, 152f, 153f, 151f, 150f, 152f, 151f,
                    150f, 149f, 150f, 151f, 150f, 149f, 150f, 150.5f, 150f, 150f
                )
            )
        ),
        activities = listOf(
            PortfolioActivity(ActivityType.AUTO_SWEEP, "Auto-Sweep to AC", 15.50, "Today, 9:00 AM"),
            PortfolioActivity(ActivityType.DIVIDEND, "Dividend: MER", 120.50, "Yesterday"),
            PortfolioActivity(ActivityType.ROUND_UP, "Round-up from Pay", 33.33, "Jul 14")
        )
    )

    // ── Home Activities ─────────────────────────────────
    val homeActivities = emptyList<HomeActivity>()

    // ── Chart Data ──────────────────────────────────────

    val portfolioChartData = listOf(
        1000f, 1050f, 1020f, 1100f, 1150f, 1120f, 1200f, 1250f, 1300f, 1280f,
        1350f, 1400f, 1450f, 1420f, 1500f, 1550f, 1600f, 1580f, 1650f, 1700f,
        1750f, 1800f, 1780f, 1850f, 1900f, 1950f
    )
}
