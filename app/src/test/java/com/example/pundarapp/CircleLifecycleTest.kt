package com.example.pundarapp

import com.example.pundarapp.data.stellar.StellarWalletManager
import com.example.pundarapp.ui.data.Circle
import com.example.pundarapp.ui.data.CircleMember
import com.example.pundarapp.ui.data.ContributionStatus
import org.junit.Assert.*
import org.junit.Test

/**
 * Functional test suite for Deliverable 2 — PUNDAR Circle, covering:
 * 1. Membership restrictions
 * 2. Fixed contribution validation
 * 3. Revised-term re-acceptance before activation
 * 4. Payout-amount derivation
 * 5. Activated configuration enforcement
 * 6. Recipient eligibility
 * 7. Unauthorized recipient/amount change prevention
 * 8. Duplicate payout prevention (and duplicate contribution prevention)
 * 9. Incomplete contributions
 */
class CircleLifecycleTest {

    private fun sampleProposal(): Circle {
        val members = listOf(
            CircleMember(
                userId = "09170000001",
                name = "Alice (Organizer)",
                initials = "AL",
                payoutRound = 1,
                acceptanceStatus = "ACCEPTED",
                acceptedTermsVersion = 1,
                stellarPublicKey = "GDS74PTXOXWFXSG4LSAK24LL4TSMWOMQZTVIAHF4J4FXRWMTQFMQTRBZ"
            ),
            CircleMember(
                userId = "09170000002",
                name = "Bob",
                initials = "BO",
                payoutRound = 2,
                acceptanceStatus = "PENDING",
                acceptedTermsVersion = 0,
                stellarPublicKey = "GA4E7Z4JS2IBL4WSXQ6Z3HOWRQBM3AHM566WUSXK2H6XJYR3AXCEWIDK"
            ),
            CircleMember(
                userId = "09170000003",
                name = "Charlie",
                initials = "CH",
                payoutRound = 3,
                acceptanceStatus = "PENDING",
                acceptedTermsVersion = 0,
                stellarPublicKey = "GBRHI4TF5VMIDVR6LY6M7L6SZSBD6T6MU277ZHXIHUGEDPZC7AFSECMN"
            )
        )
        return Circle(
            id = "circle_test_1",
            name = "Bicol Community Circle",
            contributionPerMonth = 50.0,
            memberCount = members.size,
            maxMembers = members.size,
            targetAmount = 150.0,
            members = members,
            isActive = false,
            currency = "XLM",
            creatorId = "09170000001",
            creatorName = "Alice (Organizer)",
            lifecycleStatus = "AWAITING_ACCEPTANCE",
            termsVersion = 1,
            currentRound = 1
        )
    }

    @Test
    fun `1 and 3 - pre-activation decline blocks activation and revised terms require re-acceptance`() {
        var circle = sampleProposal()

        // Premature activation before all members accept v1 must fail
        val prematureActivate = circle.applyActivation(
            organizerId = "09170000001",
            newSorobanCircleId = 1L,
            txHash = "TX_PREMATURE",
            contractId = StellarWalletManager.CIRCLE_CONTRACT_ID
        )
        assertTrue(prematureActivate.isFailure)

        // Bob accepts v1 -> still awaiting Charlie
        circle = circle.applyMemberTermsResponse("09170000002", accept = true, forTermsVersion = 1).getOrThrow()
        assertEquals("AWAITING_ACCEPTANCE", circle.lifecycleStatus)
        assertFalse(circle.allMembersAcceptedCurrentTerms)

        // Charlie declines v1 -> status becomes DECLINED_REVISION_NEEDED and activation is blocked
        circle = circle.applyMemberTermsResponse("09170000003", accept = false, forTermsVersion = 1).getOrThrow()
        assertEquals("DECLINED_REVISION_NEEDED", circle.lifecycleStatus)
        assertTrue(circle.hasDeclinedMember)
        assertFalse(circle.allMembersAcceptedCurrentTerms)

        // Non-organizer cannot revise terms
        assertTrue(circle.applyDeclinedMemberRemovalAndReviseTerms("09170000003", organizerId = "09170000002").isFailure)

        // Organizer removes declined Charlie and issues Revised Terms v2
        circle = circle.applyDeclinedMemberRemovalAndReviseTerms("09170000003", organizerId = "09170000001").getOrThrow()
        assertEquals(2, circle.termsVersion)
        assertEquals(2, circle.members.size)
        assertEquals(100.0, circle.scheduledPayoutAmount, 0.001) // 50 * 2 remaining members
        assertEquals("AWAITING_ACCEPTANCE", circle.lifecycleStatus)

        // Bob's prior v1 acceptance is no longer sufficient; Bob is reset to PENDING for v2
        val bob = circle.members.first { it.userId == "09170000002" }
        assertEquals("PENDING", bob.acceptanceStatus)
        assertFalse(circle.allMembersAcceptedCurrentTerms)

        // Activation before Bob re-accepts v2 must fail
        assertTrue(
            circle.applyActivation("09170000001", 1L, "TX_ACT", StellarWalletManager.CIRCLE_CONTRACT_ID).isFailure
        )

        // Stale acceptance for v1 must be rejected
        assertTrue(circle.applyMemberTermsResponse("09170000002", accept = true, forTermsVersion = 1).isFailure)

        // Bob accepts revised v2 -> Circle transitions to READY_TO_ACTIVATE
        circle = circle.applyMemberTermsResponse("09170000002", accept = true, forTermsVersion = 2).getOrThrow()
        assertEquals("READY_TO_ACTIVATE", circle.lifecycleStatus)
        assertTrue(circle.allMembersAcceptedCurrentTerms)
    }

    @Test
    fun `5 - activated configuration enforcement locks rules and blocks post-activation edits`() {
        var circle = sampleProposal()
        circle = circle.applyMemberTermsResponse("09170000002", accept = true, forTermsVersion = 1).getOrThrow()
        circle = circle.applyMemberTermsResponse("09170000003", accept = true, forTermsVersion = 1).getOrThrow()
        assertEquals("READY_TO_ACTIVATE", circle.lifecycleStatus)

        // Activate on Soroban
        circle = circle.applyActivation(
            organizerId = "09170000001",
            newSorobanCircleId = 42L,
            txHash = "TX_SOROBAN_ACTIVATE_42",
            contractId = StellarWalletManager.CIRCLE_CONTRACT_ID
        ).getOrThrow()

        assertEquals("ACTIVE", circle.lifecycleStatus)
        assertTrue(circle.isActive)
        assertEquals(42L, circle.sorobanCircleId)
        assertEquals(StellarWalletManager.CIRCLE_CONTRACT_ID, circle.escrowAddress)

        // Post-activation attempts to change terms, decline, or remove members must fail
        assertTrue(circle.applyMemberTermsResponse("09170000002", accept = false, forTermsVersion = 1).isFailure)
        assertTrue(circle.applyDeclinedMemberRemovalAndReviseTerms("09170000002", "09170000001").isFailure)
        assertTrue(circle.applyActivation("09170000001", 43L, "TX_DUP_ACT", StellarWalletManager.CIRCLE_CONTRACT_ID).isFailure)
    }

    @Test
    fun `1, 2, 8 - membership restrictions, fixed contribution validation, and duplicate contribution prevention`() {
        var circle = sampleProposal()
            .applyMemberTermsResponse("09170000002", true, 1).getOrThrow()
            .applyMemberTermsResponse("09170000003", true, 1).getOrThrow()
            .applyActivation("09170000001", 10L, "TX_ACT", StellarWalletManager.CIRCLE_CONTRACT_ID).getOrThrow()

        // Non-member cannot contribute
        val outsiderRes = circle.applyMemberContribution("09999999999", amountContributed = 50.0, txHash = "TX_OUTSIDER")
        assertTrue(outsiderRes.isFailure)

        // Partial or custom contribution amount must be rejected
        assertTrue(circle.applyMemberContribution("09170000001", amountContributed = 25.0, txHash = "TX_PARTIAL").isFailure)
        assertTrue(circle.applyMemberContribution("09170000001", amountContributed = 75.0, txHash = "TX_OVER").isFailure)
        assertTrue(circle.applyMemberContribution("09170000001", amountContributed = 0.0, txHash = "TX_ZERO").isFailure)

        // Exact fixed contribution succeeds
        circle = circle.applyMemberContribution("09170000001", amountContributed = 50.0, txHash = "TX_R1_M1").getOrThrow()
        assertEquals(50.0, circle.savedAmount, 0.001)

        // Duplicate contribution in the same round by the same member must be rejected
        val dupContrib = circle.applyMemberContribution("09170000001", amountContributed = 50.0, txHash = "TX_R1_M1_DUP")
        assertTrue(dupContrib.isFailure)
    }

    @Test
    fun `4, 6, 7, 8, 9 - payout derivation, incomplete contribution block, recipient rotation, and duplicate payout prevention`() {
        var circle = sampleProposal()
            .applyMemberTermsResponse("09170000002", true, 1).getOrThrow()
            .applyMemberTermsResponse("09170000003", true, 1).getOrThrow()
            .applyActivation("09170000001", 10L, "TX_ACT", StellarWalletManager.CIRCLE_CONTRACT_ID).getOrThrow()

        // Requirement 4: Payout amount is deterministically derived as 50.0 * 3 = 150.0 XLM
        assertEquals(150.0, circle.scheduledPayoutAmount, 0.001)

        // Requirement 9: Incomplete contributions (only 2 of 3 paid) must block payout
        circle = circle.applyMemberContribution("09170000001", 50.0, "TX_R1_M1").getOrThrow()
        circle = circle.applyMemberContribution("09170000002", 50.0, "TX_R1_M2").getOrThrow()
        assertFalse(circle.allCurrentRoundContributionsComplete)
        assertFalse(circle.isCurrentRoundPayoutEligible)
        assertTrue(circle.applyRoundPayout("09170000001", "Alice (Organizer)", "TX_PREMATURE_PAYOUT").isFailure)

        // 3rd member contributes -> Round 1 becomes payout-eligible
        circle = circle.applyMemberContribution("09170000003", 50.0, "TX_R1_M3").getOrThrow()
        assertTrue(circle.allCurrentRoundContributionsComplete)
        assertTrue(circle.isCurrentRoundPayoutEligible)
        assertEquals("09170000001", circle.currentRoundRecipient?.userId)

        // Non-member cannot invoke payout
        assertTrue(circle.applyRoundPayout("09999999999", "Outsider", "TX_OUTSIDER_PAYOUT").isFailure)

        // Round 1 Payout -> pays Alice (Round 1 recipient) exact derived 150.0 XLM, advances to Round 2
        circle = circle.applyRoundPayout("09170000002", "Bob", "TX_PAYOUT_R1").getOrThrow()
        assertEquals(2, circle.currentRound)
        assertEquals(listOf(1), circle.paidRounds)
        assertEquals("09170000002", circle.currentRoundRecipient?.userId) // Round 2 recipient is Bob
        assertTrue(circle.members.first { it.userId == "09170000001" }.hasReceivedPayout)

        // Round 2 contributions & payout -> pays Bob (Round 2 recipient)
        circle = circle.applyMemberContribution("09170000001", 50.0, "TX_R2_M1").getOrThrow()
        circle = circle.applyMemberContribution("09170000002", 50.0, "TX_R2_M2").getOrThrow()
        circle = circle.applyMemberContribution("09170000003", 50.0, "TX_R2_M3").getOrThrow()
        circle = circle.applyRoundPayout("09170000001", "Alice (Organizer)", "TX_PAYOUT_R2").getOrThrow()
        assertEquals(3, circle.currentRound)
        assertEquals(listOf(1, 2), circle.paidRounds)
        assertEquals("09170000003", circle.currentRoundRecipient?.userId) // Round 3 recipient is Charlie

        // Round 3 contributions & payout -> pays Charlie (Round 3 recipient) and completes Circle
        circle = circle.applyMemberContribution("09170000001", 50.0, "TX_R3_M1").getOrThrow()
        circle = circle.applyMemberContribution("09170000002", 50.0, "TX_R3_M2").getOrThrow()
        circle = circle.applyMemberContribution("09170000003", 50.0, "TX_R3_M3").getOrThrow()
        circle = circle.applyRoundPayout("09170000003", "Charlie", "TX_PAYOUT_R3").getOrThrow()

        assertEquals("COMPLETED", circle.lifecycleStatus)
        assertFalse(circle.isActive)
        assertEquals(listOf(1, 2, 3), circle.paidRounds)

        // Requirement 8: Duplicate payout attempt after completion must be rejected
        assertTrue(circle.applyRoundPayout("09170000001", "Alice (Organizer)", "TX_DUP_PAYOUT").isFailure)
    }
}
