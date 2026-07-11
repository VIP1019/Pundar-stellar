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

data class Circle(
    val id: String,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val targetDate: String,
    val memberCount: Int,
    val contributionPerMonth: Double,
    val members: List<CircleMember>,
    val isActive: Boolean = true,
    val escrowAddress: String = "0x7f...9A2B",
    val escrowNetwork: String = "Stellar Network",
    val monthlyDueDay: Int = 5
)

data class CircleMember(
    val name: String,
    val initials: String,
    val sharePercent: Int,
    val amount: Double,
    val status: ContributionStatus,
    val isYou: Boolean = false,
    val avatarColor: Long = 0xFF6B7280
)

enum class ContributionStatus { PAID, PENDING, OVERDUE }

data class CircleInvitation(
    val id: String,
    val circleName: String,
    val goal: String,
    val inviterName: String,
    val inviterScore: Int,
    val inviterCirclesCompleted: Int,
    val targetAmount: Double,
    val fundedPercent: Int,
    val monthlyContribution: Double,
    val memberCount: Int,
    val maxMembers: Int,
    val inviterInitials: String = "",
    val inviterAvatarColor: Long = 0xFF0052CC
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
    val shares: Int,
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

enum class ActivityType { AUTO_SWEEP, DIVIDEND, PURCHASE, ROUND_UP, PAYOUT }

data class HomeActivity(
    val icon: String, // icon name
    val title: String,
    val subtitle: String,
    val amount: String,
    val isPositive: Boolean,
    val module: String // "Pay", "Circle", "Grow"
)

// ── Sample Data ─────────────────────────────────────────────────

object SampleData {

    val currentUser = PundarUser(
        name = "Juan Dela Cruz",
        initials = "JD",
        pundarScore = 850
    )

    // ── Pay ─────────────────────────────────────────────
    val recentBills = emptyList<GroupBill>()
    val recentContacts = emptyList<BillMember>()

    // ── Circle ──────────────────────────────────────────
    val circles = emptyList<Circle>()
    val circleInvitation: CircleInvitation? = null

    // ── Grow ────────────────────────────────────────────

    val portfolio = Portfolio(
        totalValue = 124500.00,
        totalReturnPercent = 12.4,
        totalReturnAmount = 15436.00,
        phEquitiesPercent = 80,
        fixedIncomePercent = 20,
        holdings = listOf(
            StockHolding(
                ticker = "AC",
                companyName = "Ayala Corp",
                sector = "Real Estate & Conglomerates",
                exchange = "Philippine Stock Exchange",
                shares = 150,
                value = 90000.00,
                returnPercent = 5.2,
                currentPrice = 850.50,
                priceChange = 12.50,
                priceChangePercent = 1.49,
                averageCost = 808.00,
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
                ticker = "SMPH",
                companyName = "SM Prime",
                sector = "Real Estate",
                exchange = "Philippine Stock Exchange",
                shares = 500,
                value = 16500.00,
                returnPercent = 1.8,
                currentPrice = 33.00,
                priceChange = 0.50,
                priceChangePercent = 1.54,
                averageCost = 32.40,
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
                shares = 120,
                value = 18000.00,
                returnPercent = -0.5,
                currentPrice = 150.00,
                priceChange = -0.80,
                priceChangePercent = -0.53,
                averageCost = 150.75,
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
            PortfolioActivity(ActivityType.AUTO_SWEEP, "Auto-Sweep", 500.00, "Today, 9:00 AM"),
            PortfolioActivity(ActivityType.DIVIDEND, "Dividend: MER", 120.50, "Yesterday"),
            PortfolioActivity(ActivityType.ROUND_UP, "Round-up from Pay", 33.33, "Jul 4"),
            PortfolioActivity(ActivityType.PURCHASE, "Buy: AC x5", 4252.50, "Jul 3", false)
        )
    )

    // ── Home Activities ─────────────────────────────────
    val homeActivities = emptyList<HomeActivity>()

    // ── Chart Data ──────────────────────────────────────

    val portfolioChartData = listOf(
        85000f, 87000f, 86500f, 89000f, 92000f, 91000f, 94000f, 96000f, 95500f,
        98000f, 100000f, 99000f, 102000f, 105000f, 103000f, 108000f, 110000f,
        112000f, 115000f, 118000f, 116000f, 119000f, 121000f, 122000f, 124000f, 124500f
    )
}
