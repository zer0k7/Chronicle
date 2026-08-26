package io.chronicle.usagestats.domain.model

enum class TimelinePeriod {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

enum class AppCategory {
    ALL,
    PRODUCTIVITY,
    SOCIAL,
    ENTERTAINMENT,
    GAMES,
    COMMUNICATION,
    UTILITIES,
    SYSTEM,
    REMOVED,
    OTHER
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
    val isDistraction: Boolean = false
)

data class HourlyUsageSlot(
    val hour: Int, // 0..23
    val totalDurationMillis: Long,
    val topAppPackage: String? = null,
    val topAppLabel: String? = null,
    val appBreakdown: List<AppUsageInfo> = emptyList()
)

data class LongestSession(
    val packageName: String,
    val appLabel: String,
    val durationMillis: Long,
    val startEpochMillis: Long,
    val endEpochMillis: Long
)

data class GhostOpensInsight(
    val totalGhostOpens: Int,
    val topGhostAppLabel: String? = null,
    val topGhostAppOpens: Int = 0
)

data class MorningDoomscroll(
    val durationMillis: Long,
    val topAppLabel: String? = null
)

data class WakingLifeImpact(
    val wakingPercentage: Double, // 0.0 .. 100.0%
    val annualProjectedDays: Int  // Full 24h days per year
)

data class LifeClockProjection(
    val dailyAverageMillis: Long,
    val yearsLostBy75: Double,
    val consciousPercentage: Double
)

data class DopamineDebt(
    val weeklyActualMillis: Long,
    val weeklyBaselineMillis: Long,
    val debtMillis: Long,
    val recommendedFastMinutes: Int
)

data class PhantomUnlocks(
    val count: Int,
    val totalQuickChecks: Int
)

data class DisciplineStreaks(
    val goalStreakDays: Int = 0,
    val morningShieldStreakDays: Int = 0,
    val sleepSanctuaryStreakDays: Int = 0,
    val bestGoalStreakDays: Int = 0
)

data class HabitInsights(
    val deviceUnlocks: Int = 0,
    val firstUnlockEpochMillis: Long? = null,
    val lastLockEpochMillis: Long? = null,
    val bedtimeUsageMillis: Long = 0L,
    val avgSessionDurationMillis: Long = 0L,
    val fragmentationScore: Int = 0, // 0..100
    val productivityScore: Int = 0, // 0..100
    val categoryBreakdown: Map<AppCategory, Long> = emptyMap(),
    val longestSession: LongestSession? = null,
    val peakUnlockHour: Int? = null,
    val peakUnlockCount: Int = 0,
    val hourlyUnlocks: List<Int> = emptyList(),
    val ghostOpens: GhostOpensInsight? = null,
    val morningDoomscroll: MorningDoomscroll? = null,
    val wakingLifeImpact: WakingLifeImpact? = null,
    val lifeClock: LifeClockProjection? = null,
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

data class DailyUsageSummary(
    val dateEpochMillis: Long,
    val totalScreenTimeMillis: Long,
    val topAppPackage: String?,
    val topAppLabel: String?,
    val appCount: Int,
    val apps: List<AppUsageInfo> = emptyList(),
    val hourlySlots: List<HourlyUsageSlot> = emptyList(),
    val habitInsights: HabitInsights? = null,
    val trendComparison: TrendComparison? = null
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
    val dopamineDebt: DopamineDebt? = null
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
    val trendComparison: TrendComparison? = null
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
    val dailyLimitMinutes: Int? = null
)

data class AppDetailInfo(
    val packageName: String,
    val appLabel: String,
    val category: AppCategory,
    val isRemoved: Boolean,
    val isDistraction: Boolean,
    val dailyLimitMinutes: Int?,
    val todayForegroundMillis: Long,
    val todayLaunchCount: Int,
    val todayAvgSessionMillis: Long,
    val percentageOfTotalDaily: Float,
    val hourlySlots: List<HourlyUsageSlot>,
    val recentDaysUsage: List<Pair<Long, Long>>, // (dateEpochMillis, durationMillis)
    val ghostOpensCount: Int
)
