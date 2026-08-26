package io.chronicle.usagestats.domain.model

enum class AppCategory {
    ALL,
    SOCIAL,
    ENTERTAINMENT,
    PRODUCTIVITY,
    GAMING,
    GAMES,
    COMMUNICATION,
    UTILITIES,
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
    val launchCount: Int = 0,
    val lastTimeUsedMillis: Long = 0L,
    val avgSessionDurationMillis: Long = 0L,
    val isRemoved: Boolean = false,
    val category: AppCategory = AppCategory.OTHER,
    val dailyLimitMinutes: Int? = null,
    val isDistraction: Boolean = false,
    val isWifiPreferred: Boolean = false
)

data class HourlyUsageSlot(
    val hour: Int, // 0..23
    val totalDurationMillis: Long,
    val activeAppCount: Int = 0,
    val topAppPackage: String? = null,
    val topAppLabel: String? = null,
    val appBreakdown: List<AppUsageInfo> = emptyList()
)

data class WakingLifeMetrics(
    val screenPercentageOfWakingHours: Double = 0.0,
    val projectedYearsLostInLifetime: Double = 0.0,
    val daysSpentPerYear: Double = 0.0,
    val wakingHoursAssumed: Int = 16
)

data class LifeClockProjection(
    val dailyAverageMillis: Long = 0L,
    val yearsLostBy75: Double = 0.0,
    val lifetimeYearsLost: Double = yearsLostBy75,
    val consciousPercentage: Double = 0.0,
    val consciousLifePercentage: Double = consciousPercentage
)

data class DopamineDebt(
    val totalScreenTimeMillis: Long = 0L,
    val weeklyActualMillis: Long = totalScreenTimeMillis,
    val baselineScreenTimeMillis: Long = 0L,
    val weeklyBaselineMillis: Long = baselineScreenTimeMillis,
    val debtMillis: Long = 0L,
    val recommendedDigitalFastMinutes: Int = 0,
    val recommendedFastMinutes: Int = recommendedDigitalFastMinutes
)

data class PhantomUnlocks(
    val count: Int = 0,
    val totalUnlocks: Int = count,
    val totalQuickChecks: Int = 0,
    val quickRelapseUnlocks: Int = totalQuickChecks,
    val averageMinutesBetweenUnlocks: Int = 0
)

data class DisciplineStreaks(
    val goalStreakDays: Int = 0,
    val currentGoalStreakDays: Int = goalStreakDays,
    val bestGoalStreakDays: Int = 0,
    val morningShieldStreakDays: Int = 0,
    val sleepSanctuaryStreakDays: Int = 0
)

data class LongestSession(
    val packageName: String,
    val appLabel: String,
    val durationMillis: Long,
    val startEpochMillis: Long = 0L,
    val endEpochMillis: Long = 0L
)

data class GhostOpensInsight(
    val totalGhostOpens: Int = 0,
    val topGhostAppLabel: String? = null,
    val topGhostAppOpens: Int = 0
)

data class MorningDoomscroll(
    val durationMillis: Long = 0L,
    val topAppLabel: String? = null
)

data class WakingLifeImpact(
    val wakingPercentage: Double = 0.0,
    val annualProjectedDays: Int = 0
)

data class ClosedSessionMetric(
    val pkg: String,
    val start: Long,
    val end: Long,
    val duration: Long
)

data class HabitInsights(
    val totalPickups: Int = 0,
    val pickupFrequencyMinutes: Int = 0,
    val morningDoomscrollMillis: Long = 0L,
    val bedtimeRevengeMillis: Long = 0L,
    val ghostOpensCount: Int = 0,
    val firstUnlockEpochMillis: Long? = null,
    val lastLockEpochMillis: Long? = null,
    val bedtimeUsageMillis: Long = 0L,
    val avgSessionDurationMillis: Long = 0L,
    val fragmentationScore: Int = 0,
    val productivityScore: Int = 0,
    val categoryBreakdown: Map<AppCategory, Long> = emptyMap(),
    val longestSession: LongestSession? = null,
    val peakUnlockHour: Int = 0,
    val peakUnlockCount: Int = 0,
    val hourlyUnlocks: List<Int> = emptyList(),
    val ghostOpens: GhostOpensInsight? = null,
    val morningDoomscroll: MorningDoomscroll? = null,
    val wakingLifeImpact: WakingLifeImpact? = null,
    val lifeClock: LifeClockProjection? = null,
    val wakingLife: WakingLifeMetrics? = null,
    val dopamineDebt: DopamineDebt? = null,
    val phantomUnlocks: PhantomUnlocks? = null,
    val disciplineStreaks: DisciplineStreaks? = null,
    val deviceUnlocks: Int = totalPickups
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
