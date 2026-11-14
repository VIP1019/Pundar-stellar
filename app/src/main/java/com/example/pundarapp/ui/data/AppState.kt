package com.example.pundarapp.ui.data

import androidx.compose.runtime.*
import com.example.pundarapp.data.qr.QrPayload
import com.example.pundarapp.data.remote.*
import com.example.pundarapp.data.stellar.PayStellarClient
import com.example.pundarapp.data.stellar.StellarWalletManager
import com.example.pundarapp.data.stellar.Stroops
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log
import android.telephony.TelephonyManager
import java.util.Locale

/** Maps ISO 3166-1 alpha-2 country codes to their primary ISO 4217 currency codes */
private val COUNTRY_CURRENCY_MAP = mapOf(
    "PH" to "PHP", // Philippines
    "US" to "USD", // United States
    "GB" to "GBP", // United Kingdom
    "EU" to "EUR", // European Union (fallback)
    "DE" to "EUR", "FR" to "EUR", "IT" to "EUR", "ES" to "EUR", "NL" to "EUR",
    "JP" to "JPY", // Japan
    "KR" to "KRW", // South Korea
    "SG" to "SGD", // Singapore
    "AU" to "AUD", // Australia
    "NZ" to "NZD", // New Zealand
    "CA" to "CAD", // Canada
    "CH" to "CHF", // Switzerland
    "CN" to "CNY", // China
    "HK" to "HKD", // Hong Kong
    "IN" to "INR", // India
    "MY" to "MYR", // Malaysia
    "ID" to "IDR", // Indonesia
    "VN" to "VND", // Vietnam
    "TH" to "THB", // Thailand
    "BD" to "BDT", // Bangladesh
    "PK" to "PKR", // Pakistan
    "SA" to "SAR", // Saudi Arabia
    "AE" to "AED", // UAE
    "BR" to "BRL", // Brazil
    "MX" to "MXN", // Mexico
    "ZA" to "ZAR", // South Africa
    "NG" to "NGN", // Nigeria
    "RU" to "RUB", // Russia
    "SE" to "SEK", // Sweden
    "NO" to "NOK", // Norway
    "DK" to "DKK", // Denmark
    "TW" to "TWD", // Taiwan
)

/** Detects the country code from network carrier → SIM → device locale (best to worst accuracy) */
fun detectCountryFromNetwork(context: android.content.Context): String {
    return try {
        val tm = context.getSystemService(android.content.Context.TELEPHONY_SERVICE) as? TelephonyManager
        // 1. Network country (changes when phone registers on a foreign carrier tower)
        val networkCountry = tm?.networkCountryIso?.uppercase()?.takeIf { it.length == 2 }
        if (networkCountry != null) return networkCountry
        // 2. SIM country (doesn't change when roaming, but better than locale)
        val simCountry = tm?.simCountryIso?.uppercase()?.takeIf { it.length == 2 }
        if (simCountry != null) return simCountry
        // 3. Fallback: Default to PH for PUNDAR wallet if no SIM/network carrier is detected
        "PH"
    } catch (e: Exception) {
        Log.w("AppState", "Country detection failed: ${e.message}")
        "PH"
    }
}

/** Maps a country code to its primary currency code */
fun countryToCurrency(country: String): String = COUNTRY_CURRENCY_MAP[country.uppercase()] ?: "PHP"

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
                bills.clear()
                bills.addAll(remoteBills)
                
                refreshNotifications()
                loadFavorites()
                refreshCircles()
                loadHomeActivities(AuthRepository.getCurrentUserPhone())
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
                amount = "-₱ ${String.format("%,.2f", bill.yourShare)}",
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
            // Deduct user's share from wallet (convert PHP share to XLM)
            val shareXlm = phpToXlm(old.yourShare)
            updateWalletBalance((walletBalance.value - shareXlm).coerceAtLeast(0.0))
            bills[index] = old.copy(status = BillStatus.SETTLED)
            // Record as home activity
            addHomeActivity(
                HomeActivity(
                    icon = "payment",
                    title = old.name,
                    subtitle = "Bill settled",
                    amount = "-₱ ${String.format("%,.2f", old.yourShare)}",
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

    fun markBillSettled(billId: String) {
        val index = bills.indexOfFirst { it.id == billId }
        if (index >= 0) {
            val old = bills[index]
            if (old.status == BillStatus.SETTLED) return
            bills[index] = old.copy(status = BillStatus.SETTLED)
            scope.launch {
                if (AuthRepository.isUserLoggedIn()) {
                    PayRepository.createBill(bills[index])
                }
            }
        }
    }

    fun refreshBills() {
        scope.launch {
            if (AuthRepository.isUserLoggedIn()) {
                val remoteBills = PayRepository.getBills()
                bills.clear()
                bills.addAll(remoteBills)
            }
        }
    }

    // ── CIRCLE ──────────────────────────────────────────────────────
    val circles = mutableStateListOf<Circle>()
    val discoverableCircles = mutableStateListOf<Circle>()
    val pendingInvitation = mutableStateOf<CircleInvitation?>(null)
    val pendingInvitations = mutableStateListOf<CircleInvitation>()
    val pendingJoinRequests = mutableStateListOf<CircleJoinRequest>()
    val unifiedRequests = mutableStateListOf<com.example.pundarapp.ui.data.AppRequest>()

    fun refreshUnifiedRequests() {
        val phone = AuthRepository.getCurrentUserPhone()
        if (phone.isBlank()) return
        scope.launch {
            val requests = mutableListOf<com.example.pundarapp.ui.data.AppRequest>()
            
            // 1. Bill Invitations
            val billInvites = com.example.pundarapp.data.remote.PayRepository.getBillInvitations(phone)
            billInvites.forEach { bi ->
                requests.add(
                    com.example.pundarapp.ui.data.AppRequest(
                        id = bi.id,
                        type = com.example.pundarapp.ui.data.RequestType.BILL_INVITE,
                        title = "Split Bill Request",
                        subtitle = "${bi.inviterName} invited you to split '${bi.billName}'. Your share: ${String.format("%,.2f", bi.yourShare)} XLM.",
                        date = "Just now",
                        payload = bi
                    )
                )
            }
            
            // 2. Circle Invitations
            val circleInvites = CircleRepository.getPendingInvitations(phone)
            circleInvites.forEach { ci ->
                requests.add(
                    com.example.pundarapp.ui.data.AppRequest(
                        id = ci.id,
                        type = com.example.pundarapp.ui.data.RequestType.CIRCLE_INVITE,
                        title = "Circle Invitation",
                        subtitle = "${ci.inviterName} invited you to join '${ci.circleName}'. Goal: ${ci.goal}",
                        date = "Just now",
                        payload = ci
                    )
                )
            }
            
            // 3. Circle Join Requests
            val joinRequests = CircleRepository.getPendingJoinRequestsForCreator(phone)
            joinRequests.forEach { cjr ->
                requests.add(
                    com.example.pundarapp.ui.data.AppRequest(
                        id = cjr.id,
                        type = com.example.pundarapp.ui.data.RequestType.CIRCLE_JOIN_REQUEST,
                        title = "Join Request",
                        subtitle = "${cjr.userName} requested to join your circle.",
                        date = "Just now",
                        payload = cjr
                    )
                )
            }
            
            unifiedRequests.clear()
            unifiedRequests.addAll(requests)
        }
    }

    fun refreshCircles() {
        val userId = AuthRepository.getCurrentUserPhone()
        if (userId.isBlank()) return
        scope.launch {
            val remoteCircles = CircleRepository.getCirclesForUser(userId)
            circles.clear()
            circles.addAll(remoteCircles)
            // Still updating legacy variables for backward compatibility if any
            val pendingInvites = CircleRepository.getPendingInvitations(userId)
            pendingInvitations.clear()
            pendingInvitations.addAll(pendingInvites)
            pendingInvitation.value = pendingInvites.firstOrNull()
        }
        refreshUnifiedRequests()
    }

    fun refreshDiscoverableCircles() {
        val userId = AuthRepository.getCurrentUserPhone()
        if (userId.isBlank()) return
        scope.launch {
            val remoteDiscoverable = CircleRepository.getDiscoverableCircles(userId)
            discoverableCircles.clear()
            discoverableCircles.addAll(remoteDiscoverable)
        }
    }

    suspend fun requestToJoinCircle(circleId: String): Result<Unit> {
        val userId = AuthRepository.getCurrentUserPhone()
        val userName = AuthRepository.getCurrentUserName()
        return CircleRepository.requestToJoinCircle(circleId, userId, userName)
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
            val userId = AuthRepository.getCurrentUserPhone()
            val refreshed = CircleRepository.refreshInvitation(invitation)
            val respondResult = CircleRepository.respondToCircleTerms(
                circleId = refreshed.circleId,
                userId = userId,
                accept = true,
                termsVersion = refreshed.termsVersion
            )

            if (respondResult.isSuccess) {
                CircleRepository.updateInvitationStatus(invitation.id, "ACCEPTED")
                pendingInvitations.removeAll { it.id == invitation.id || it.circleId == invitation.circleId }
                pendingInvitation.value = pendingInvitations.firstOrNull()
                refreshCircles()

                addHomeActivity(
                    HomeActivity(
                        icon = "savings",
                        title = refreshed.circleName,
                        subtitle = "Accepted Circle Terms (v${refreshed.termsVersion})",
                        amount = "${String.format("%,.2f", refreshed.monthlyContribution)} ${refreshed.currency}",
                        isPositive = true,
                        module = "Circle"
                    )
                )
                Result.success(Unit)
            } else {
                Result.failure(respondResult.exceptionOrNull() ?: Exception("Failed to accept terms"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectInvitation(invitation: CircleInvitation): Result<Unit> {
        return try {
            val userId = AuthRepository.getCurrentUserPhone()
            val refreshed = CircleRepository.refreshInvitation(invitation)
            CircleRepository.respondToCircleTerms(
                circleId = refreshed.circleId,
                userId = userId,
                accept = false,
                termsVersion = refreshed.termsVersion
            )
            val result = CircleRepository.updateInvitationStatus(invitation.id, "DECLINED")
            pendingInvitations.removeAll { it.id == invitation.id || it.circleId == invitation.circleId }
            pendingInvitation.value = pendingInvitations.firstOrNull()
            refreshCircles()
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun contributeToCircle(circleId: String, circleCurrencyAmount: Double, xlmAmount: Double): Boolean {
        // Guard: insufficient balance
        if (walletBalance.value < xlmAmount) return false
        val index = circles.indexOfFirst { it.id == circleId }
        if (index >= 0) {
            val old = circles[index]
            val updatedMembers = old.members.map { member ->
                if (member.isYou) {
                    member.copy(
                        status = ContributionStatus.PAID,
                        amount = member.amount + circleCurrencyAmount
                    )
                } else {
                    member
                }
            }
            var finalCircle = old.copy(
                savedAmount = (old.savedAmount + circleCurrencyAmount).coerceAtMost(old.targetAmount),
                members = updatedMembers
            )
            
            var releasedFunds = 0.0
            var recipientName = ""
            
            if (finalCircle.savedAmount >= finalCircle.targetAmount) {
                // Auto-Release logic
                val recipientIndex = finalCircle.currentRecipientIndex
                if (recipientIndex < finalCircle.members.size) {
                    val recipient = finalCircle.members[recipientIndex]
                    recipientName = recipient.name
                    releasedFunds = finalCircle.targetAmount
                    
                    val amountXlm = if (old.currency == "XLM") finalCircle.targetAmount else (finalCircle.targetAmount / AppState.currentExchangeRate.doubleValue)
                    if (recipient.isYou) {
                        // Directly add to wallet if you are the recipient
                        updateWalletBalance(walletBalance.value + amountXlm)
                        
                        addHomeActivity(
                            HomeActivity(
                                icon = "savings",
                                title = finalCircle.name,
                                subtitle = "Funds auto-released to you",
                                amount = if (old.currency == "XLM") "+${String.format("%,.2f", releasedFunds)} XLM" else "+${old.currency} ${String.format("%,.2f", releasedFunds)}",
                                isPositive = true,
                                module = "Circle"
                            )
                        )
                    } else {
                        // Update the other user's balance in Firestore
                        scope.launch {
                            try {
                                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                val docId = if (recipient.userId.isNotEmpty()) recipient.userId else recipient.name
                                var doc = db.collection("users").document(docId).get().await()
                                
                                if (!doc.exists()) {
                                    // Fallback: try finding the user by their full_name
                                    val usersByName = db.collection("users").whereEqualTo("full_name", recipient.name).get().await()
                                    if (usersByName.documents.isNotEmpty()) {
                                        doc = usersByName.documents.first()
                                    }
                                }

                                if (doc.exists()) {
                                    val currentBal = doc.getDouble("wallet_balance") ?: doc.getDouble("walletBalance") ?: 0.0
                                    val newBal = currentBal + amountXlm
                                    db.collection("users").document(doc.id).update(mapOf(
                                        "wallet_balance" to newBal,
                                        "walletBalance" to newBal
                                    )).await()
                                    
                                    // Add activity for the recipient in Firestore
                                    val activityId = java.util.UUID.randomUUID().toString()
                                    val remoteActivity = hashMapOf(
                                        "id" to activityId,
                                        "userId" to doc.id,
                                        "icon" to "savings",
                                        "title" to finalCircle.name,
                                        "subtitle" to "Funds auto-released to you",
                                        "amount" to if (old.currency == "XLM") "+${String.format("%,.2f", releasedFunds)} XLM" else "+${old.currency} ${String.format("%,.2f", releasedFunds)}",
                                        "timestamp" to System.currentTimeMillis(),
                                        "isPositive" to true,
                                        "module" to "Circle"
                                    )
                                    db.collection("recent_activities").document(activityId).set(remoteActivity).await()
                                    
                                } else {
                                    android.util.Log.e("AppState", "Could not find recipient user document in Firestore to credit balance.")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("AppState", "Failed to update recipient balance", e)
                            }
                        }
                    }
                    
                    // Reset savedAmount, advance recipient, and reset member statuses for the next cycle
                    val nextCycleMembers = finalCircle.members.map { m ->
                        m.copy(status = com.example.pundarapp.ui.data.ContributionStatus.PENDING, amount = 0.0)
                    }
                    
                    val newRecipientIndex = recipientIndex + 1
                    finalCircle = finalCircle.copy(
                        savedAmount = 0.0,
                        currentRecipientIndex = newRecipientIndex,
                        members = nextCycleMembers,
                        isActive = newRecipientIndex < finalCircle.members.size
                    )
                }
            }
            
            circles[index] = finalCircle
            
            scope.launch {
                com.example.pundarapp.data.remote.CircleRepository.updateCircle(finalCircle)
            }
            
            // Deduct from wallet
            updateWalletBalance(walletBalance.value - xlmAmount)
            addHomeActivity(
                HomeActivity(
                    icon = "savings",
                    title = old.name,
                    subtitle = "Contribution sent",
                    amount = if (old.currency == "XLM") "-${String.format("%,.2f", xlmAmount)} XLM" else "-${old.currency} ${String.format("%,.2f", circleCurrencyAmount)}",
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
        
        val targetMultiplier = 10.0
        val mod = amount % targetMultiplier
        val baseRoundUp = if (mod == 0.0) 0.0 else (targetMultiplier - mod)
        
        return baseRoundUp * settings.roundUpMultiplier
    }

    fun sweepBalanceRemainderToGrow(): Double {
        val current = walletBalance.value
        if (current <= 0.0) return 0.0
        val mod = current % 10.0
        val remainder = if (mod == 0.0) 0.0 else mod
        if (remainder <= 0.0 || current < remainder) return 0.0

        updateWalletBalance(current - remainder)
        val settings = roundUpSettings.value
        val accumulated = settings.totalAccumulated + remainder
        val updatedSettings = settings.copy(
            totalAccumulated = accumulated,
            roundUpCount = settings.roundUpCount + 1
        )
        roundUpSettings.value = updatedSettings

        addHomeActivity(
            HomeActivity(
                icon = "trending_up",
                title = "Spare Change Swept",
                subtitle = "Placed in Pundar Grow",
                amount = "-${String.format("%,.2f", remainder)} XLM",
                isPositive = false,
                module = "Grow"
            )
        )

        if (accumulated >= settings.threshold) {
            scope.launch {
                triggerManualRoundUpInvest("")
            }
        }
        return remainder
    }

    suspend fun processPayRoundUp(
        sourceReference: String,
        sourceAmount: Double,
        sourceLabel: String,
        mpin: String
    ): RoundUpInvestment? {
        val settings = roundUpSettings.value
        if (!settings.isEnabled) return null

        val roundUp = calculateRoundUpAmount(sourceAmount)
        if (roundUp <= 0.0) return null

        val roundUpXlm = phpToXlm(roundUp)
        if (walletBalance.value < roundUpXlm) {
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

        updateWalletBalance((walletBalance.value - roundUpXlm).coerceAtLeast(0.0))
        val accumulated = settings.totalAccumulated + roundUp
        
        val updatedSettings = settings.copy(
            totalAccumulated = accumulated,
            roundUpCount = settings.roundUpCount + 1
        )
        roundUpSettings.value = updatedSettings

        return if (accumulated >= settings.threshold) {
            val pk = AuthRepository.getCurrentUserStellarPublicKey() ?: ""
            val seed = AuthRepository.getCurrentUserEncryptedSeed() ?: ""
            
            // Purchase tokenized RWA dynamically
            val result = StellarWalletManager.buyMockRwa(pk, seed, mpin, accumulated.toString())
            if (result.isSuccess) {
                // Update local RWA state
                fetchRwaBalance()
                
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
                addHomeActivity(
                    HomeActivity(
                        icon = "trending_up",
                        title = "Round-up failed",
                        subtitle = result.exceptionOrNull()?.message ?: "Transaction error",
                        amount = "₱ ${String.format("%,.2f", roundUp)}",
                        isPositive = false,
                        module = "Grow"
                    )
                )
                null
            }
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

    suspend fun triggerManualRoundUpInvest(mpin: String = "") {
        val settings = roundUpSettings.value
        val accumulated = settings.totalAccumulated
        if (accumulated <= 0.0) return

        val pk = AuthRepository.getCurrentUserStellarPublicKey() ?: ""
        val seed = AuthRepository.getCurrentUserEncryptedSeed() ?: ""
        val result = StellarWalletManager.buyMockRwa(pk, seed, mpin, accumulated.toString())
        
        if (result.isSuccess) {
            fetchRwaBalance()
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
        persistPortfolioValue()
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
        val phpAmount = amount * currentExchangeRate.doubleValue
        portfolio.value = old.copy(
            totalValue = old.totalValue + phpAmount,
            totalReturnAmount = old.totalReturnAmount + phpAmount * 0.01,
            activities = listOf(
                PortfolioActivity(
                    type = ActivityType.DEPOSIT,
                    amount = phpAmount,
                    date = "Just now",
                    description = "Invested via Grow",
                    isPositive = true
                )
            ) + old.activities
        )
        // Deduct from wallet
        updateWalletBalance(walletBalance.value - amount)
        persistPortfolioValue()
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
        val phpAmount = amount * currentExchangeRate.doubleValue
        if (old.totalValue < phpAmount) return false
        portfolio.value = old.copy(
            totalValue = (old.totalValue - phpAmount).coerceAtLeast(0.0),
            activities = listOf(
                PortfolioActivity(
                    type = ActivityType.WITHDRAWAL,
                    amount = phpAmount,
                    date = "Just now",
                    description = "Withdrawn from Grow",
                    isPositive = false
                )
            ) + old.activities
        )
        // Add back to wallet
        updateWalletBalance(walletBalance.value + amount)
        persistPortfolioValue()
        addHomeActivity(
            HomeActivity(
                icon = "trending_down",
                title = "Withdrawal",
                subtitle = "Withdrawn from Grow",
                amount = "+${String.format("%,.2f", amount)} XLM",
                isPositive = true,
                module = "Grow"
            )
        )
        return true
    }

    private fun persistPortfolioValue() {
        val phone = AuthRepository.getCurrentUserPhone()
        if (phone.isNotBlank()) {
            prefs?.edit()?.putFloat("portfolio_value_$phone", portfolio.value.totalValue.toFloat())?.apply()
        }
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
    val pundarScore = mutableIntStateOf(800)

    fun requestHomeRefresh() {
        homeRefreshTrigger.intValue += 1
    }

    fun loadUserBalance(phone: String, remoteBalance: Double) {
        // On login/load, the remote backend should always be the source of truth,
        // so we don't accidentally load an outdated local cache if another user sent us funds.
        val finalBalance = remoteBalance
        walletBalance.value = finalBalance

        val key = "wallet_balance_$phone"
        prefs?.edit()?.putFloat(key, finalBalance.toFloat())?.apply()
        if (phone.isNotBlank()) {
            scope.launch {
                try {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(phone)
                        .update(mapOf("wallet_balance" to finalBalance, "walletBalance" to finalBalance))
                } catch (e: Exception) {
                    Log.e("AppState", "Failed to sync balance to Firestore", e)
                }
            }
        }
    }

    fun updateWalletBalance(newBalance: Double) {
        val clamped = newBalance.coerceAtLeast(0.0)
        walletBalance.value = clamped
        val phone = AuthRepository.getCurrentUserPhone()
        if (phone.isNotBlank()) {
            prefs?.edit()?.putFloat("wallet_balance_$phone", clamped.toFloat())?.apply()
            scope.launch {
                try {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(phone)
                        .update(mapOf("wallet_balance" to clamped, "walletBalance" to clamped))
                } catch (e: Exception) {
                    Log.e("AppState", "Failed to update Firestore wallet balance", e)
                }
            }
        }
    }

    fun refreshWalletBalance() {
        fetchExchangeRate()
        val phone = AuthRepository.getCurrentUserPhone()
        if (phone.isNotBlank()) {
            scope.launch {
                try {
                    val publicKey = AuthRepository.getCurrentUserStellarPublicKey()
                    if (!publicKey.isNullOrBlank()) {
                        val initialResult = PayStellarClient.getXlmBalance(publicKey)
                        val resolvedResult = PayStellarClient.recoverMissingAccountBalance(publicKey, initialResult)
                        if (resolvedResult.isSuccess) {
                            val stroops = resolvedResult.getOrThrow()
                            val onChainXlm = stroops.toDouble() / Stroops.PER_XLM
                            walletBalance.value = onChainXlm
                            prefs?.edit()?.putFloat("wallet_balance_$phone", onChainXlm.toFloat())?.apply()
                            try {
                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("users").document(phone)
                                    .update(mapOf("wallet_balance" to onChainXlm, "walletBalance" to onChainXlm))
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {}
                        } else {
                            // Preserve the last known balance on refresh failure
                            Log.w(
                                "AppState",
                                "Horizon balance refresh failed for $publicKey; preserving last known balance (${walletBalance.value})",
                                resolvedResult.exceptionOrNull()
                            )
                        }
                        return@launch
                    }

                    // Fallback to cached Firestore balance only when no Stellar publicKey is configured
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val doc = db.collection("users").document(phone).get().await()
                    if (doc.exists()) {
                        val remoteBalance = doc.getDouble("wallet_balance") ?: doc.getDouble("walletBalance")
                        if (remoteBalance != null) {
                            walletBalance.value = remoteBalance
                            prefs?.edit()?.putFloat("wallet_balance_$phone", remoteBalance.toFloat())?.apply()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.e("AppState", "Failed to refresh wallet balance; preserving last known balance", e)
                }
            }
        }
    }

    fun addHomeActivity(activity: HomeActivity) {
        val old = homeActivities.toMutableList()
        old.add(0, activity)
        homeActivities.clear()
        homeActivities.addAll(old.take(50)) // Keep last 50 activities
        saveHomeActivities()
    }

    private fun saveHomeActivities() {
        val phone = AuthRepository.getCurrentUserPhone()
        if (phone.isBlank()) return
        scope.launch {
            try {
                val list = homeActivities.map { a ->
                    mapOf(
                        "icon" to a.icon,
                        "title" to a.title,
                        "subtitle" to a.subtitle,
                        "amount" to a.amount,
                        "isPositive" to a.isPositive,
                        "module" to a.module,
                        "timestamp" to a.timestamp,
                        "referenceId" to a.referenceId
                    )
                }
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(phone)
                    .set(mapOf("homeActivities" to list), com.google.firebase.firestore.SetOptions.merge())
            } catch (e: Exception) {
                Log.e("AppState", "Failed to save activities", e)
            }
        }
    }

    fun loadHomeActivities(phone: String) {
        if (phone.isBlank()) return
        scope.launch {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(phone).get()
                .addOnSuccessListener { doc ->
                    val acts = doc.get("homeActivities") as? List<Map<String, Any>>
                    if (acts != null) {
                        val parsed = acts.map { m ->
                            HomeActivity(
                                icon = m["icon"] as? String ?: "",
                                title = m["title"] as? String ?: "",
                                subtitle = m["subtitle"] as? String ?: "",
                                amount = m["amount"] as? String ?: "",
                                isPositive = m["isPositive"] as? Boolean ?: false,
                                module = m["module"] as? String ?: "",
                                timestamp = (m["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                referenceId = m["referenceId"] as? String ?: ""
                            )
                        }
                        scope.launch(Dispatchers.Main) {
                            homeActivities.clear()
                            homeActivities.addAll(parsed)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("AppState", "Failed to load activities", e)
                }
        }
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

    // ── SETTINGS & ONBOARDING TOUR ───────────────────────────────────────────
    val isBalanceHidden = mutableStateOf(false)
    val showProductTour = mutableStateOf(false)
    val preferredCurrency = mutableStateOf("PHP")
    val currentExchangeRate = mutableDoubleStateOf(CurrencyRepository.DEFAULT_PHP_PER_XLM)
    val phpExchangeRate = mutableDoubleStateOf(CurrencyRepository.DEFAULT_PHP_PER_XLM)

    /** Returns live PHP per 1 XLM rate (e.g., ~6.50 PHP/XLM) */
    fun getPhpRate(): Double =
        phpExchangeRate.doubleValue.takeIf { it > 0.0 } ?: CurrencyRepository.getCachedPhpPerXlm()

    /** Converts PHP amount to XLM using the live PHP/XLM exchange rate */
    fun phpToXlm(phpAmount: Double): Double = phpAmount / getPhpRate()

    /** Converts XLM amount to PHP using the live PHP/XLM exchange rate */
    fun xlmToPhp(xlmAmount: Double): Double = xlmAmount * getPhpRate()
    
    val rwaBalance = mutableDoubleStateOf(0.0)

    suspend fun fetchRwaBalance() {
        val pk = AuthRepository.getCurrentUserStellarPublicKey()
        if (pk != null) {
            val balanceStr = StellarWalletManager.getRwaBalance(pk)
            rwaBalance.doubleValue = balanceStr.toDoubleOrNull() ?: 0.0
        }
    }
    val themeMode = mutableStateOf("LIGHT")
    
    private var prefs: android.content.SharedPreferences? = null

    fun setThemeMode(mode: String) {
        themeMode.value = mode
        prefs?.edit()?.putString("theme_mode", mode)?.apply()
    }

    fun initPreferences(context: android.content.Context) {
        prefs = context.getSharedPreferences("pundar_prefs", android.content.Context.MODE_PRIVATE)
        isBalanceHidden.value = prefs?.getBoolean("hide_balance", false) ?: false
        // Theme Mode - default to LIGHT
        themeMode.value = prefs?.getString("theme_mode", "LIGHT") ?: "LIGHT"

        // Load saved currency or detect from network carrier country
        val savedCurrency = prefs?.getString("preferred_currency", null)
        val detectedCurrency = savedCurrency ?: countryToCurrency(detectCountryFromNetwork(context))
        preferredCurrency.value = detectedCurrency
        prefs?.edit()?.putString("preferred_currency", detectedCurrency)?.apply()
        fetchExchangeRate()

        val phone = AuthRepository.getCurrentUserPhone()
        if (phone.isNotBlank()) {
            val key = "wallet_balance_$phone"
            if (prefs?.contains(key) == true) {
                walletBalance.value = prefs?.getFloat(key, 0f)?.toDouble() ?: 0.0
            }
            
            // Refresh in background to get latest remote balance
            refreshWalletBalance()
            
            pundarScore.intValue = AuthRepository.currentUserState.value?.pundarScore ?: prefs?.getInt("pundar_score_$phone", 800) ?: 800
            // Restore portfolio totalValue
            val savedPortfolioValue = prefs?.getFloat("portfolio_value_$phone", -1f) ?: -1f
            if (savedPortfolioValue >= 0f) {
                portfolio.value = portfolio.value.copy(totalValue = savedPortfolioValue.toDouble())
            }
        }
    }

    fun incrementPundarScore(points: Int = 5) {
        pundarScore.value += points
        val phone = AuthRepository.getCurrentUserPhone()
        if (phone.isNotBlank()) {
            prefs?.edit()?.putInt("pundar_score_$phone", pundarScore.value)?.apply()
        }
    }
    
    fun setCurrency(currencyCode: String) {
        preferredCurrency.value = currencyCode.uppercase()
        prefs?.edit()?.putString("preferred_currency", preferredCurrency.value)?.apply()
        fetchExchangeRate()
    }

    /** Called from MainActivity.onResume — silently updates currency if network country changed */
    fun updateCurrencyFromNetwork(context: android.content.Context) {
        val newCurrency = countryToCurrency(detectCountryFromNetwork(context))
        if (newCurrency != preferredCurrency.value) {
            Log.d("AppState", "Country changed — currency updated: ${preferredCurrency.value} → $newCurrency")
            preferredCurrency.value = newCurrency
            prefs?.edit()?.putString("preferred_currency", newCurrency)?.apply()
            fetchExchangeRate()
        }
    }

    /** Called from MainActivity.onResume — uses GPS to determine exact country and update currency */
    fun updateCurrencyFromGps(context: android.content.Context) {
        scope.launch {
            val countryCode = com.example.pundarapp.data.location.LocationCurrencyManager.getCountryCode(context)
            if (countryCode != null) {
                val newCurrency = countryToCurrency(countryCode)
                if (newCurrency != preferredCurrency.value) {
                    Log.d("AppState", "GPS: country=$countryCode → currency $newCurrency")
                    preferredCurrency.value = newCurrency
                    prefs?.edit()?.putString("preferred_currency", newCurrency)?.apply()
                    fetchExchangeRate()
                }
            } else {
                // GPS unavailable — fall back to network/SIM detection
                updateCurrencyFromNetwork(context)
            }
        }
    }
    
    fun fetchExchangeRate(forceRefresh: Boolean = false) {
        scope.launch {
            val rates = CurrencyRepository.getXlmRates(forceRefresh = forceRefresh)
            val phpRate = rates["PHP"]?.takeIf { it > 0.0 } ?: CurrencyRepository.DEFAULT_PHP_PER_XLM
            phpExchangeRate.doubleValue = phpRate
            val activeCurrency = preferredCurrency.value.uppercase()
            currentExchangeRate.doubleValue = rates[activeCurrency]?.takeIf { it > 0.0 }
                ?: CurrencyRepository.getDefaultRate(activeCurrency)
        }
    }

    fun toggleBalanceVisibility() {
        val newState = !isBalanceHidden.value
        isBalanceHidden.value = newState
        prefs?.edit()?.putBoolean("hide_balance", newState)?.apply()
    }

    fun checkAndTriggerProductTour(userId: String, forceNewOnboarding: Boolean = false) {
        val cleanId = userId.trim().ifBlank { "default" }
        val key = "has_seen_product_tour_$cleanId"
        val hasSeen = prefs?.getBoolean(key, false) ?: false
        if (forceNewOnboarding || !hasSeen) {
            showProductTour.value = true
        }
    }

    fun markProductTourCompleted() {
        showProductTour.value = false
        val cleanId = AuthRepository.getCurrentUserPhone().trim().ifBlank { "default" }
        prefs?.edit()?.putBoolean("has_seen_product_tour_$cleanId", true)?.apply()
    }

    fun replayProductTour() {
        showProductTour.value = true
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
        val phone = AuthRepository.getCurrentUserPhone()
        if (phone.isNotBlank()) {
            prefs?.edit()?.remove("portfolio_value_$phone")?.apply()
        }
        bills.clear()
        bills.addAll(SampleData.recentBills)
        circles.clear()
        circles.addAll(SampleData.circles)
        pendingInvitation.value = null
        pendingJoinRequests.clear()
        portfolio.value = SampleData.portfolio
        favoriteStocks.value = emptySet()
        roundUpSettings.value = SampleData.roundUpSettings
        recentNotifications.clear()
        unreadCountState.intValue = 0
        homeActivities.clear()
        homeActivities.addAll(SampleData.homeActivities)
        walletBalance.value = 0.0
        pendingQrPayload.value = null
        homeRefreshTrigger.intValue = 0
    }
}
