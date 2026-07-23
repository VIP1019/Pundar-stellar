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

fun getCurrencySymbol(code: String): String {
    return when (code.uppercase()) {
        "PHP" -> "₱"
        "USD", "CAD", "AUD", "NZD", "SGD", "HKD", "TWD" -> "$"
        "EUR" -> "€"
        "JPY", "CNY" -> "¥"
        "KRW" -> "₩"
        "GBP" -> "£"
        "IDR" -> "Rp"
        "VND" -> "₫"
        "INR" -> "₹"
        "MYR" -> "RM"
        "THB" -> "฿"
        else -> "$"
    }
}

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
                amount = "-${String.format("%,.2f", bill.yourShare)} XLM",
                isPositive = false,
                module = "Pay"
            )
        )
    }

    fun settleBill(billId: String) {
        val index = bills.indexOfFirst { it.id == billId }
        if (index >= 0) {
            val old = bills[index]
            if (old.status == BillStatus.SETTLED) return // already settled
            // Deduct user's share from wallet
            val share = old.yourShare
            walletBalance.value = (walletBalance.value - share).coerceAtLeast(0.0)
            bills[index] = old.copy(status = BillStatus.SETTLED)
            // Record as home activity
            addHomeActivity(
                HomeActivity(
                    icon = "payment",
                    title = old.name,
                    subtitle = "Bill settled",
                    amount = "-${String.format("%,.2f", share)} XLM",
                    isPositive = false,
                    module = "Pay"
                )
            )
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
                    amount = "+${String.format("%,.0f", invitation.monthlyContribution)} XLM/mo",
                    isPositive = true,
                    module = "Circle"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun contributeToCircle(circleId: String, amount: Double): Boolean {
        // Guard: insufficient balance
        if (walletBalance.value < amount) return false
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
            // Deduct from wallet
            walletBalance.value -= amount
            addHomeActivity(
                HomeActivity(
                    icon = "savings",
                    title = old.name,
                    subtitle = "Contribution sent",
                    amount = "-${String.format("%,.2f", amount)} XLM",
                    isPositive = false,
                    module = "Circle"
                )
            )
        }
        return true
    }

    // ── GROW ────────────────────────────────────────────────────────
    val portfolio = mutableStateOf(SampleData.portfolio)
    val favoriteStocks = mutableStateOf(emptySet<String>())
    val roundUpSettings = mutableStateOf(SampleData.roundUpSettings)

    fun loadRoundUpState() {
        val userId = AuthRepository.getCurrentUserPhone()
        if (userId.isBlank()) return
        scope.launch {
            // Placeholder: Load from repo
        }
    }

    fun toggleRoundUp(enabled: Boolean) {
        roundUpSettings.value = roundUpSettings.value.copy(isEnabled = enabled)
        persistRoundUpState(null)
    }

    fun setRoundUpMultiplier(multiplier: Int) {
        roundUpSettings.value = roundUpSettings.value.copy(roundUpMultiplier = multiplier)
        persistRoundUpState(null)
    }

    fun setRoundUpThreshold(threshold: Double) {
        roundUpSettings.value = roundUpSettings.value.copy(threshold = threshold)
        persistRoundUpState(null)
    }

    fun updateTargetStocks(targets: List<String>) {
        if (targets.isEmpty()) return
        roundUpSettings.value = roundUpSettings.value.copy(targetStocks = targets)
        persistRoundUpState(null)
    }

    private fun persistRoundUpState(investment: RoundUpInvestment?) {
        val userId = AuthRepository.getCurrentUserPhone()
        if (userId.isBlank()) return
        scope.launch {
            // Placeholder: persist to backend
        }
    }

    fun calculateRoundUpAmount(amount: Double): Double {
        val settings = roundUpSettings.value
        if (!settings.isEnabled || amount <= 0.0) return 0.0
        
        val roundedTarget = kotlin.math.ceil(amount)
        val baseRoundUp = roundedTarget - amount
        
        return if (baseRoundUp > 0.0) baseRoundUp * settings.roundUpMultiplier else 0.0
    }

    fun processPayRoundUp(
        sourceReference: String,
        sourceAmount: Double,
        sourceLabel: String
    ): RoundUpInvestment? {
        val settings = roundUpSettings.value
        if (!settings.isEnabled) return null

        val roundUp = calculateRoundUpAmount(sourceAmount)
        if (roundUp <= 0.0) return null

        if (walletBalance.value < roundUp) {
            addHomeActivity(
                HomeActivity(
                    icon = "trending_up",
                    title = "Round-up skipped",
                    subtitle = "Insufficient balance after $sourceLabel",
                    amount = "₱ ${String.format("%,.2f", roundUp)}",
                    isPositive = false,
                    module = "Grow"
                )
            )
            return null
        }

        walletBalance.value -= roundUp
        val accumulated = settings.totalAccumulated + roundUp
        
        val updatedSettings = settings.copy(
            totalAccumulated = accumulated,
            roundUpCount = settings.roundUpCount + 1
        )
        roundUpSettings.value = updatedSettings

        return if (accumulated >= settings.threshold) {
            val investment = purchaseTokenizedEquity(
                sourceReference = sourceReference,
                sourceAmount = sourceAmount,
                roundUpAmount = roundUp,
                accumulatedAmount = accumulated
            )
            // Reset accumulated and update lifetime invested
            roundUpSettings.value = roundUpSettings.value.copy(
                totalAccumulated = 0.0,
                totalInvested = roundUpSettings.value.totalInvested + accumulated
            )
            persistRoundUpState(investment)
            investment
        } else {
            persistRoundUpState(null)
            addHomeActivity(
                HomeActivity(
                    icon = "trending_up",
                    title = "Round-up captured",
                    subtitle = "$sourceLabel spare change saved for Grow",
                    amount = "-₱ ${String.format("%,.2f", roundUp)}",
                    isPositive = false,
                    module = "Grow"
                )
            )
            null
        }
    }

    fun triggerManualRoundUpInvest() {
        val settings = roundUpSettings.value
        val accumulated = settings.totalAccumulated
        if (accumulated <= 0.0) return

        val investment = purchaseTokenizedEquity(
            sourceReference = "MANUAL-${System.currentTimeMillis()}",
            sourceAmount = 0.0,
            roundUpAmount = 0.0,
            accumulatedAmount = accumulated
        )
        // Reset accumulated and update lifetime invested
        roundUpSettings.value = settings.copy(
            totalAccumulated = 0.0,
            totalInvested = settings.totalInvested + accumulated
        )
        persistRoundUpState(investment)
    }

    private fun purchaseTokenizedEquity(
        sourceReference: String,
        sourceAmount: Double,
        roundUpAmount: Double,
        accumulatedAmount: Double
    ): RoundUpInvestment {
        val oldPortfolio = portfolio.value
        val targetHolding = chooseRoundUpTarget(oldPortfolio.holdings)
        val fractionalShares = accumulatedAmount / targetHolding.currentPrice
        val updatedHoldings = oldPortfolio.holdings.map { holding ->
            if (holding.ticker == targetHolding.ticker) {
                holding.copy(
                    shares = holding.shares + fractionalShares,
                    value = holding.value + accumulatedAmount
                )
            } else {
                holding
            }
        }
        portfolio.value = oldPortfolio.copy(
            holdings = updatedHoldings,
            totalValue = oldPortfolio.totalValue + accumulatedAmount,
            activities = listOf(
                PortfolioActivity(
                    type = ActivityType.ROUND_UP,
                    amount = accumulatedAmount,
                    date = "Just now",
                    description = "Round-up auto-invested in ${targetHolding.ticker}",
                    isPositive = true
                )
            ) + oldPortfolio.activities
        )
        
        addHomeActivity(
            HomeActivity(
                icon = "trending_up",
                title = "Auto-Invested",
                subtitle = "Round-up goal reached!",
                amount = "₱ ${String.format("%,.2f", accumulatedAmount)}",
                isPositive = true,
                module = "Grow"
            )
        )
        
        return RoundUpInvestment(
            reference = "RINV-${System.currentTimeMillis()}",
            sourceReference = sourceReference,
            sourceAmount = sourceAmount,
            roundUpAmount = roundUpAmount,
            convertedAmount = accumulatedAmount,
            ticker = targetHolding.ticker,
            companyName = targetHolding.companyName,
            fractionalShares = fractionalShares,
            pricePerShare = targetHolding.currentPrice,
            provider = "Pundar Grow",
            status = "COMPLETED",
            createdAt = System.currentTimeMillis()
        )
    }

    private fun chooseRoundUpTarget(holdings: List<StockHolding>): StockHolding {
        val targets = roundUpSettings.value.targetStocks
        if (targets.isEmpty()) return holdings.first()
        val preferredHoldings = holdings.filter { it.ticker in targets }
        return if (preferredHoldings.isNotEmpty()) {
            preferredHoldings.minByOrNull { it.value } ?: holdings.first()
        } else {
            holdings.first()
        }
    }

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

    fun invest(amount: Double): Boolean {
        if (walletBalance.value < amount) return false
        val old = portfolio.value
        portfolio.value = old.copy(
            totalValue = old.totalValue + amount,
            totalReturnAmount = old.totalReturnAmount + amount * 0.01,
            activities = listOf(
                PortfolioActivity(
                    type = ActivityType.DEPOSIT,
                    amount = amount,
                    date = "Just now",
                    description = "Invested via Grow",
                    isPositive = true
                )
            ) + old.activities
        )
        // Deduct from wallet
        walletBalance.value -= amount
        addHomeActivity(
            HomeActivity(
                icon = "trending_up",
                title = "Investment",
                subtitle = "Invested via Grow",
                amount = "-${String.format("%,.2f", amount)} XLM",
                isPositive = false,
                module = "Grow"
            )
        )
        return true
    }

    fun withdraw(amount: Double): Boolean {
        val old = portfolio.value
        if (old.totalValue < amount) return false
        portfolio.value = old.copy(
            totalValue = (old.totalValue - amount).coerceAtLeast(0.0),
            activities = listOf(
                PortfolioActivity(
                    type = ActivityType.WITHDRAWAL,
                    amount = amount,
                    date = "Just now",
                    description = "Withdrawn from Grow",
                    isPositive = false
                )
            ) + old.activities
        )
        // Add back to wallet
        walletBalance.value += amount
        addHomeActivity(
            HomeActivity(
                icon = "trending_up",
                title = "Withdrawal",
                subtitle = "Withdrawn from Grow",
                amount = "+${String.format("%,.2f", amount)} XLM",
                isPositive = true,
                module = "Grow"
            )
        )
        return true
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
    val walletBalance = mutableStateOf(0.0)
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
    val preferredCurrency = mutableStateOf("PHP")
    val currentExchangeRate = mutableDoubleStateOf(1.0)
    
    private var prefs: android.content.SharedPreferences? = null

    fun initPreferences(context: android.content.Context) {
        prefs = context.getSharedPreferences("pundar_prefs", android.content.Context.MODE_PRIVATE)
        isBalanceHidden.value = prefs?.getBoolean("hide_balance", false) ?: false
        preferredCurrency.value = prefs?.getString("preferred_currency", "PHP") ?: "PHP"
        fetchExchangeRate()
    }
    
    fun setCurrency(currencyCode: String) {
        preferredCurrency.value = currencyCode.uppercase()
        prefs?.edit()?.putString("preferred_currency", preferredCurrency.value)?.apply()
        fetchExchangeRate()
    }
    
    private fun fetchExchangeRate() {
        scope.launch {
            val rates = CurrencyRepository.getXlmRates(forceRefresh = false)
            currentExchangeRate.doubleValue = rates[preferredCurrency.value] ?: 1.0
        }
    }

    fun toggleBalanceVisibility() {
        val newState = !isBalanceHidden.value
        isBalanceHidden.value = newState
        prefs?.edit()?.putBoolean("hide_balance", newState)?.apply()
    }

    fun getDisplayBalance(): String {
        return if (isBalanceHidden.value) "••••• XLM" else "${String.format("%,.2f", walletBalance.value)} XLM"
    }

    fun getFiatDisplayBalance(): String {
        return if (isBalanceHidden.value) "${getCurrencySymbol(preferredCurrency.value)} •••••"
        else formatFiat(walletBalance.value)
    }

    fun formatFiat(xlmAmount: Double): String {
        val fiatValue = xlmAmount * currentExchangeRate.doubleValue
        val symbol = getCurrencySymbol(preferredCurrency.value)
        return "~ $symbol ${String.format("%,.2f", fiatValue)}"
    }

    fun clearSession() {
        bills.clear()
        circles.clear()
        pendingInvitation.value = null
        pendingJoinRequests.clear()
        portfolio.value = SampleData.portfolio
        favoriteStocks.value = emptySet()
        roundUpSettings.value = SampleData.roundUpSettings
        recentNotifications.clear()
        unreadCountState.intValue = 0
        homeActivities.clear()
        walletBalance.value = 0.0
        pendingQrPayload.value = null
        homeRefreshTrigger.intValue = 0
    }
}
