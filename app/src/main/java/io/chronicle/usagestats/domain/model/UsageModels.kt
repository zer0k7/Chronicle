package io.chronicle.usagestats.domain.model

enum class AppCategory {
    ALL,
    SOCIAL,
    ENTERTAINMENT,
    PRODUCTIVITY,
    GAMING,
    COMMUNICATION,
    EDUCATION,
    NEWS,
    SYSTEM,
    REMOVED,
    OTHER
}

enum class TimelinePeriod {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

enum class BurnoutRisk {
    LOW,
    MODERATE,
    ELEVATED,
    HIGH
}

data class AppUsageInfo(
    val packageName: String,
    val appLabel: String,
    val totalTimeForegroundMillis: Long,
    val category: AppCategory = AppCategory.OTHER,
    val isRemoved: Boolean = false,
    val isDistraction: Boolean = false,
    val isWifiPreferred: Boolean = false,
    val dailyLimitMinutes: Int? = null,
    val launchCount: Int = 0
)

data class HourlyUsageSlot(
    val hour: Int, // 0..23
    val totalDurationMillis: Long,
    val activeAppCount: Int = 0,
    val topAppPackage: String? = null,
    val topAppLabel: String? = null
)

data class WakingLifeMetrics(
    val screenPercentageOfWakingHours: Double,
    val projectedYearsLostInLifetime: Double,
    val daysSpentPerYear: Double,
    val wakingHoursAssumed: Int = 16
)

data class LifeClockProjection(
    val dailyAverageMillis: Long,
    val lifetimeYearsLost: Double, // based on 50 remaining years
    val consciousLifePercentage: Double
)

data class DopamineDebt(
    val totalScreenTimeMillis: Long,
    val baselineScreenTimeMillis: Long, // e.g. 2.5 hours
    val debtMillis: Long,
    val recommendedDigitalFastMinutes: Int
)

data class PhantomUnlocks(
    val totalUnlocks: Int,
    val quickRelapseUnlocks: Int, // unlocked and locked within 30 seconds
    val averageMinutesBetweenUnlocks: Int
)

data class DisciplineStreaks(
    val currentGoalStreakDays: Int,
    val bestGoalStreakDays: Int,
    val morningShieldStreakDays: Int,
    val sleepSanctuaryStreakDays: Int
)

data class HabitInsights(
    val totalPickups: Int,
    val pickupFrequencyMinutes: Int,
    val morningDoomscrollMillis: Long,
    val bedtimeRevengeMillis: Long,
    val ghostOpensCount: Int, // opens <30s
    val wakingLife: WakingLifeMetrics? = null,
    val dopamineDebt: DopamineDebt? = null,
    val phantomUnlocks: PhantomUnlocks? = null,
    val disciplineStreaks: DisciplineStreaks? = null
)

data class TrendComparison(
    val previousPeriodDurationMillis: Long,
    val deltaDurationMillis: Long, // positive = more time, negative = less time
    val percentageChange: Double // e.g. -12.5%
)

data class AppComparison(
    val appA: AppUsageInfo,
    val appB: AppUsageInfo
)

data class ScreenTimeForecast(
    val projectedMillis: Long,
    val currentVelocityMinutesPerHour: Float,
    val burnoutRisk: BurnoutRisk,
    val burnoutScore: Int, // 0..100
    val reason: String
)

data class ContinuousDoomscrollSession(
    val packageName: String,
    val appLabel: String,
    val continuousMillis: Long,
    val category: AppCategory
)

data class AppHabitLoop(
    val triggerPackage: String,
    val triggerLabel: String,
    val targetPackage: String,
    val targetLabel: String,
    val transitionCount: Int,
    val averageTargetTimeMillis: Long
)

data class WeeklyExecutiveBriefing(
    val totalScreenTimeMillis: Long,
    val dailyAverageMillis: Long,
    val consciousReclaimedMillis: Long,
    val topDistractionApp: String?,
    val longestFocusStreakMinutes: Int,
    val burnoutRisk: BurnoutRisk,
    val efficiencyScore: Int // 0..100
)

data class DailyUsageSummary(
    val dateEpochMillis: Long,
    val totalScreenTimeMillis: Long,
    val topAppPackage: String?,
    val topAppLabel: String?,
    val appCount: Int,
    val apps: List<AppUsageInfo> = emptyList(),
    val hourlySlots: List<HourlyUsageSlot> = emptyList(),
    val habitInsights: HabitInsights? = null,
    val trendComparison: TrendComparison? = null,
    val forecast: ScreenTimeForecast? = null,
    val doomscrollSessions: List<ContinuousDoomscrollSession> = emptyList(),
    val habitLoops: List<AppHabitLoop> = emptyList()
)

data class RangeUsageReport(
    val startDateEpochMillis: Long,
    val endDateEpochMillis: Long,
    val totalScreenTimeMillis: Long,
    val dailyAverageMillis: Long,
    val daysCount: Int,
    val totalUnlocks: Int,
    val topApps: List<AppUsageInfo>,
    val categoryBreakdown: Map<AppCategory, Long>,
    val dailySummaries: List<DailyUsageSummary>,
    val lifeClock: LifeClockProjection? = null,
    val dopamineDebt: DopamineDebt? = null,
    val executiveBriefing: WeeklyExecutiveBriefing? = null
)

data class TimelineData(
    val period: TimelinePeriod,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val totalDurationMillis: Long,
    val activeAppCount: Int,
    val dailySummaries: List<DailyUsageSummary>,
    val topApps: List<AppUsageInfo>,
    val hourlySlots: List<HourlyUsageSlot> = emptyList(),
    val habitInsights: HabitInsights? = null,
    val trendComparison: TrendComparison? = null,
    val forecast: ScreenTimeForecast? = null
)

data class ReportFilter(
    val searchQuery: String = "",
    val selectedCategory: AppCategory = AppCategory.ALL
)

enum class ExportDateRange {
    TODAY,
    WEEK_7D,
    MONTH_30D,
    CUSTOM,
    ALL_TIME
}

enum class ExportFormat {
    IMAGE,
    PDF,
    CSV
}

data class CustomAppOverride(
    val packageName: String,
    val customCategory: AppCategory? = null,
    val isDistraction: Boolean = false,
    val isWifiPreferred: Boolean = false,
    val dailyLimitMinutes: Int? = null
)

data class AppDetailInfo(
    val packageName: String,
    val appLabel: String,
    val category: AppCategory,
    val isRemoved: Boolean,
    val isDistraction: Boolean,
    val isWifiPreferred: Boolean = false,
    val dailyLimitMinutes: Int?,
    val todayForegroundMillis: Long,
    val todayLaunchCount: Int,
    val todayAvgSessionMillis: Long,
    val percentageOfTotalDaily: Float,
    val hourlySlots: List<HourlyUsageSlot>,
    val recentDaysUsage: List<Pair<Long, Long>>, // (dateEpochMillis, durationMillis)
    val ghostOpensCount: Int
)
