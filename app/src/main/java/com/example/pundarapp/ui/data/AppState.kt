package com.example.pundarapp.ui.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.example.pundarapp.data.qr.QrPayload
import com.example.pundarapp.data.remote.AuthRepository
import com.example.pundarapp.data.remote.AppNotification
import com.example.pundarapp.data.remote.NotificationRepository
import com.example.pundarapp.data.remote.PayRepository
import com.example.pundarapp.data.stellar.StellarWalletManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

/**
 * Central in-memory app state shared across all screens.
 * Uses Compose snapshot state so changes automatically recompose subscribers.
 */
object AppState {

    // ── PAY ─────────────────────────────────────────────────────────
    val bills = mutableStateListOf<GroupBill>()
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e("AppState", "Background operation failed", exception)
    }
    private val scope = CoroutineScope(Dispatchers.IO + exceptionHandler)

    init {
        // Try to sync with Firebase if logged in
        scope.launch {
            if (AuthRepository.isUserLoggedIn()) {
                val remoteBills = PayRepository.getBills()
                if (remoteBills.isNotEmpty()) {
                    bills.clear()
                    bills.addAll(remoteBills)
                }
                refreshNotifications()
                loadFavorites()
            }
        }
    }

    fun addBill(bill: GroupBill) {
        bills.add(0, bill) // newest first

        scope.launch {
            if (AuthRepository.isUserLoggedIn()) {
                PayRepository.createBill(bill)
            }
        }

        addHomeActivity(
            HomeActivity(
                icon = "payment",
                title = bill.name,
                subtitle = "Pending • ${bill.memberCount} members",
                amount = "₱ ${String.format("%,.2f", bill.yourShare)}",
                isPositive = false,
                module = "Pay"
            )
        )
    }

    fun settleBill(billId: String) {
        val index = bills.indexOfFirst { it.id == billId }
        if (index >= 0) {
            val old = bills[index]
            bills[index] = old.copy(status = BillStatus.SETTLED)
            scope.launch {
                if (AuthRepository.isUserLoggedIn()) {
                    PayRepository.createBill(bills[index])
                }
            }
        }
    }

    // ── CIRCLE ──────────────────────────────────────────────────────
    val circles = mutableStateListOf<Circle>()
    val pendingInvitation = mutableStateOf<CircleInvitation?>(null)

    fun acceptInvitation(invitation: CircleInvitation) {
        val newCircle = Circle(
            id = "circle_${invitation.id}",
            name = invitation.circleName,
            targetAmount = invitation.targetAmount,
            savedAmount = invitation.targetAmount * invitation.fundedPercent / 100.0,
            targetDate = "Dec 2025",
            memberCount = invitation.memberCount + 1,
            contributionPerMonth = invitation.monthlyContribution,
            members = listOf(
                CircleMember(
                    name = SampleData.currentUser.name,
                    initials = SampleData.currentUser.initials,
                    sharePercent = (100.0 / (invitation.memberCount + 1)).toInt(),
                    amount = invitation.monthlyContribution,
                    status = ContributionStatus.PAID,
                    isYou = true,
                    avatarColor = 0xFF0052CC
                )
            ),
            isActive = true
        )
        circles.add(0, newCircle)
        pendingInvitation.value = null

        addHomeActivity(
            HomeActivity(
                icon = "savings",
                title = invitation.circleName,
                subtitle = "Joined circle",
                amount = "+₱ ${String.format("%,.0f", invitation.monthlyContribution)}",
                isPositive = true,
                module = "Circle"
            )
        )
    }

    fun contributeToCircle(circleId: String, amount: Double) {
        val index = circles.indexOfFirst { it.id == circleId }
        if (index >= 0) {
            val old = circles[index]
            // Update the member's contribution status and amount
            val updatedMembers = old.members.map { member ->
                if (member.isYou) {
                    member.copy(
                        status = ContributionStatus.PAID,
                        amount = member.amount + amount
                    )
                } else {
                    member
                }
            }
            circles[index] = old.copy(
                savedAmount = (old.savedAmount + amount).coerceAtMost(old.targetAmount),
                members = updatedMembers
            )
            addHomeActivity(
                HomeActivity(
                    icon = "savings",
                    title = old.name,
                    subtitle = "Contribution sent",
                    amount = "+₱ ${String.format("%,.0f", amount)}",
                    isPositive = true,
                    module = "Circle"
                )
            )
        }
    }

    // ── GROW ────────────────────────────────────────────────────────
    val portfolio = mutableStateOf(SampleData.portfolio)
    val favoriteStocks = mutableStateOf(setOf<String>())

    fun loadFavorites() {
        scope.launch {
            val userId = AuthRepository.getCurrentUserId() ?: return@launch
            val favorites = com.example.pundarapp.data.remote.GrowRepository.getFavorites(userId)
            favoriteStocks.value = favorites
        }
    }

    fun toggleFavorite(ticker: String) {
        // Optimistic UI: flip state immediately, then persist
        val wasFavorite = ticker in favoriteStocks.value
        favoriteStocks.value = if (wasFavorite) {
            favoriteStocks.value - ticker
        } else {
            favoriteStocks.value + ticker
        }
        scope.launch {
            val userId = AuthRepository.getCurrentUserId() ?: return@launch
            val result = com.example.pundarapp.data.remote.GrowRepository.toggleFavorite(userId, ticker)
            if (result.isFailure) {
                // Rollback on failure
                favoriteStocks.value = if (wasFavorite) {
                    favoriteStocks.value + ticker
                } else {
                    favoriteStocks.value - ticker
                }
            }
        }
    }

    fun invest(amount: Double) {
        val old = portfolio.value
        portfolio.value = old.copy(
            totalValue = old.totalValue + amount,
            totalReturnAmount = old.totalReturnAmount + amount * 0.01
        )
        addHomeActivity(
            HomeActivity(
                icon = "trending_up",
                title = "Investment",
                subtitle = "Invested via Grow",
                amount = "+₱ ${String.format("%,.2f", amount)}",
                isPositive = true,
                module = "Grow"
            )
        )
    }

    fun withdraw(amount: Double) {
        val old = portfolio.value
        portfolio.value = old.copy(
            totalValue = (old.totalValue - amount).coerceAtLeast(0.0)
        )
    }

    // ── HOME ACTIVITY FEED ──────────────────────────────────────────
    val homeActivities = mutableStateListOf<HomeActivity>()
    val walletBalance = mutableStateOf(5000.0)
    val homeRefreshTrigger = mutableIntStateOf(0)
    val pendingQrPayload = mutableStateOf<QrPayload?>(null)

    fun requestHomeRefresh() {
        homeRefreshTrigger.intValue++
    }

    fun clearSession() {
        bills.clear()
        circles.clear()
        homeActivities.clear()
        recentNotifications.clear()
        favoriteStocks.value = emptySet()
        pendingInvitation.value = null
        pendingQrPayload.value = null
        walletBalance.value = 5000.0
        portfolio.value = SampleData.portfolio
    }

    fun refreshWalletBalance() {
        val publicKey = AuthRepository.getCurrentUserStellarPublicKey()
        if (publicKey == null) {
            Log.w("AppState", "No Stellar public key found for current user")
            return
        }
        scope.launch {
            val balance = StellarWalletManager.getXlmBalance(publicKey)
            walletBalance.value = balance
        }
    }

    // ── NOTIFICATIONS ───────────────────────────────────────────────
    val recentNotifications = mutableStateListOf<AppNotification>()

    fun refreshNotifications() {
        val userId = AuthRepository.getCurrentUserId() ?: return
        scope.launch {
            NotificationRepository.seedDefaultsIfEmpty(userId)
            val result = NotificationRepository.fetchAll(userId)
            result.onSuccess { list ->
                recentNotifications.clear()
                recentNotifications.addAll(list)
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        val idx = recentNotifications.indexOfFirst { it.id == notificationId }
        if (idx >= 0) {
            val updated = recentNotifications[idx].copy(isRead = true)
            recentNotifications[idx] = updated
            val userId = AuthRepository.getCurrentUserId() ?: return
            scope.launch {
                NotificationRepository.markRead(userId, notificationId)
            }
        }
    }

    fun markAllNotificationsRead() {
        val userId = AuthRepository.getCurrentUserId() ?: return
        for (i in recentNotifications.indices) {
            val n = recentNotifications[i]
            if (!n.isRead) {
                recentNotifications[i] = n.copy(isRead = true)
            }
        }
        scope.launch {
            NotificationRepository.markAllRead(userId)
        }
    }

    fun unreadNotificationCount(): Int = recentNotifications.count { !it.isRead }

    private fun addHomeActivity(activity: HomeActivity) {
        homeActivities.add(0, activity) // newest first
        if (homeActivities.size > 20) homeActivities.removeAt(homeActivities.lastIndex)
    }
}
