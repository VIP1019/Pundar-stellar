package com.example.pundarapp.data.remote

import com.example.pundarapp.ui.data.Circle
import com.example.pundarapp.ui.data.CircleInvitation
import com.example.pundarapp.ui.data.CircleJoinRequest
import com.example.pundarapp.ui.data.SampleData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object CircleRepository {
    const val MAX_MEMBER_LIMIT_MESSAGE = "This Paluwagan has reached its maximum member limit."

    suspend fun getCirclesForUser(userId: String): List<Circle> {
        return SampleData.circles
    }

    suspend fun getPendingInvitation(userId: String): CircleInvitation? {
        return SampleData.circleInvitation
    }

    suspend fun getPendingJoinRequests(circleId: String): List<CircleJoinRequest> {
        return emptyList()
    }

    suspend fun createCircle(circle: Circle, creatorId: String): Result<Circle> {
        return Result.success(circle)
    }

    suspend fun sendInvitation(
        circleId: String,
        inviteeUserId: String,
        inviteeName: String,
        inviteeInitials: String,
        inviterName: String,
        inviterScore: Int
    ): Result<Unit> {
        return Result.success(Unit)
    }

    suspend fun joinCircle(
        circleId: String,
        userId: String,
        userName: String,
        userInitials: String,
        monthlyContribution: Double
    ): Result<Circle> {
        val circle = SampleData.circles.find { it.id == circleId }
            ?: return Result.failure(Exception("Circle not found"))
        return Result.success(circle)
    }

    suspend fun approveJoinRequest(requestId: String, approverId: String): Result<Circle> {
        // Return a dummy circle for now to satisfy the compiler
        return Result.success(SampleData.circles.firstOrNull() ?: Circle(id = "1", name = "Test", targetAmount = 1000.0, savedAmount = 0.0, targetDate = "2024", memberCount = 1, contributionPerMonth = 100.0, members = emptyList(), maxMembers = 10, creatorId = approverId))
    }

    suspend fun rejectJoinRequest(requestId: String, approverId: String): Result<Unit> {
        return Result.success(Unit)
    }

    suspend fun refreshInvitation(invitation: CircleInvitation): CircleInvitation {
        return invitation
    }
}
