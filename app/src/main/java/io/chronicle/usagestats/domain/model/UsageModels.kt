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
    val dailyLimitMinutes: Int? = null
)

data class HourlyUsageSlot(
    val hour: Int, // 0..23
    val totalDurationMillis: Long,
    val topAppPackage: String? = null,
    val topAppLabel: String? = null,
    val appBreakdown: List<AppUsageInfo> = emptyList()
)

data class HabitInsights(
    val deviceUnlocks: Int = 0,
    val firstUnlockEpochMillis: Long? = null,
    val lastLockEpochMillis: Long? = null,
    val bedtimeUsageMillis: Long = 0L,
    val avgSessionDurationMillis: Long = 0L,
    val fragmentationScore: Int = 0, // 0..100
    val productivityScore: Int = 0, // 0..100
    val categoryBreakdown: Map<AppCategory, Long> = emptyMap()
)

data class TrendComparison(
    val previousPeriodDurationMillis: Long,
    val deltaDurationMillis: Long, // positive = more time, negative = less time
    val percentageChange: Double // e.g. -12.5%
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

enum class ExportFormat {
    IMAGE,
    PDF
}
