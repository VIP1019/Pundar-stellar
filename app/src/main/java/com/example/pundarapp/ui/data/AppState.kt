package com.example.pundarapp.ui.data

import androidx.compose.runtime.*
import com.example.pundarapp.data.qr.QrPayload
import com.example.pundarapp.data.remote.*
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
    val pendingJoinRequests = mutableStateListOf<CircleJoinRequest>()

    fun refreshCircles() {
        val userId = AuthRepository.getCurrentUserPhone()
        if (userId.isBlank()) return
        scope.launch {
            val remoteCircles = CircleRepository.getCirclesForUser(userId)
            circles.clear()
            circles.addAll(remoteCircles)
        }
    }

    fun refreshJoinRequests(circleId: String) {
        scope.launch {
            val requests = CircleRepository.getPendingJoinRequests(circleId)
            pendingJoinRequests.clear()
            pendingJoinRequests.addAll(requests)
        }
    }

    suspend fun approveJoinRequest(requestId: String): Result<Circle> {
        val userId = AuthRepository.getCurrentUserPhone()
        val result = CircleRepository.approveJoinRequest(requestId, userId)
        if (result.isSuccess) {
            val request = pendingJoinRequests.find { it.id == requestId }
            if (request != null) {
                refreshJoinRequests(request.circleId)
            }
            refreshCircles()
        }
        return result
    }

    suspend fun rejectJoinRequest(requestId: String): Result<Unit> {
        val userId = AuthRepository.getCurrentUserPhone()
        val result = CircleRepository.rejectJoinRequest(requestId, userId)
        if (result.isSuccess) {
            pendingJoinRequests.removeIf { it.id == requestId }
        }
        return result
    }

    suspend fun acceptInvitation(invitation: CircleInvitation): Result<Unit> {
        return try {
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
                        name = AuthRepository.getCurrentUserName(),
                        initials = AuthRepository.getCurrentUserInitials(),
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun contributeToCircle(circleId: String, amount: Double) {
        val index = circles.indexOfFirst { it.id == circleId }
        if (index >= 0) {
            val old = circles[index]
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
    val favoriteStocks = mutableStateOf(emptySet<String>())

    fun loadFavorites() {
        val userId = AuthRepository.getCurrentUserPhone()
        if (userId.isBlank()) return
        scope.launch {
            val favorites = GrowRepository.getFavorites(userId)
            favoriteStocks.value = favorites
        }
    }

    fun toggleFavorite(ticker: String) {
        val userId = AuthRepository.getCurrentUserPhone()
        if (userId.isBlank()) return
        scope.launch {
            GrowRepository.toggleFavorite(userId, ticker)
            loadFavorites()
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

    // ── NOTIFICATIONS ───────────────────────────────────────────────
    val recentNotifications = mutableStateListOf<AppNotification>()
    private val unreadCountState = mutableIntStateOf(0)
    fun unreadNotificationCount() = unreadCountState.intValue

    fun refreshNotifications() {
        val userId = AuthRepository.getCurrentUserPhone()
        if (userId.isBlank()) return
        scope.launch {
            val result = NotificationRepository.fetchAll(userId)
            if (result.isSuccess) {
                val list = result.getOrNull() ?: emptyList()
                recentNotifications.clear()
                recentNotifications.addAll(list)
                unreadCountState.intValue = list.count { !it.isRead }
            }
        }
    }

    fun markNotificationRead(id: String) {
        val userId = AuthRepository.getCurrentUserPhone()
        if (userId.isBlank()) return
        scope.launch {
            NotificationRepository.markRead(userId, id)
            refreshNotifications()
        }
    }

    fun markAllNotificationsRead() {
        val userId = AuthRepository.getCurrentUserPhone()
        if (userId.isBlank()) return
        scope.launch {
            NotificationRepository.markAllRead(userId)
            refreshNotifications()
        }
    }

    // ── HOME ACTIVITY FEED ──────────────────────────────────────────
    val homeActivities = mutableStateListOf<HomeActivity>()
    val walletBalance = mutableStateOf(5000.0)
    val homeRefreshTrigger = mutableIntStateOf(0)

    fun requestHomeRefresh() {
        homeRefreshTrigger.intValue += 1
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

    private fun addHomeActivity(activity: HomeActivity) {
        homeActivities.add(0, activity) // newest first
        if (homeActivities.size > 20) homeActivities.removeAt(homeActivities.lastIndex)
    }

    // ── QR PAYLOAD ──────────────────────────────────────────────────
    val pendingQrPayload = mutableStateOf<QrPayload?>(null)

    // ── BRIDGE ──────────────────────────────────────────────────────
    fun realBridgeFunding(amount: Double, onResult: (Result<String>) -> Unit) {
        scope.launch {
            kotlinx.coroutines.delay(1500)
            onResult(Result.success("Success: $amount PHP bridged to XLM"))
            refreshWalletBalance()
        }
    }

    // ── SETTINGS ───────────────────────────────────────────────────
    val isBalanceHidden = mutableStateOf(false)
    private var prefs: android.content.SharedPreferences? = null

    fun initPreferences(context: android.content.Context) {
        prefs = context.getSharedPreferences("pundar_prefs", android.content.Context.MODE_PRIVATE)
        isBalanceHidden.value = prefs?.getBoolean("hide_balance", false) ?: false
    }

    fun toggleBalanceVisibility() {
        val newState = !isBalanceHidden.value
        isBalanceHidden.value = newState
        prefs?.edit()?.putBoolean("hide_balance", newState)?.apply()
    }

    fun getDisplayBalance(): String {
        return if (isBalanceHidden.value) "₱ •••••" else "₱ ${String.format("%,.2f", walletBalance.value)}"
    }

    fun clearSession() {
        bills.clear()
        circles.clear()
        pendingInvitation.value = null
        pendingJoinRequests.clear()
        portfolio.value = SampleData.portfolio
        favoriteStocks.value = emptySet()
        recentNotifications.clear()
        unreadCountState.intValue = 0
        homeActivities.clear()
        walletBalance.value = 5000.0
        pendingQrPayload.value = null
        homeRefreshTrigger.intValue = 0
    }
}
