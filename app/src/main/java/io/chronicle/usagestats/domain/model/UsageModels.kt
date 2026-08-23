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
    val isRemoved: Boolean = false,
    val category: AppCategory = AppCategory.OTHER
)

data class DailyUsageSummary(
    val dateEpochMillis: Long,
    val totalScreenTimeMillis: Long,
    val topAppPackage: String?,
    val topAppLabel: String?,
    val appCount: Int,
    val apps: List<AppUsageInfo> = emptyList()
)

data class TimelineData(
    val period: TimelinePeriod,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val totalDurationMillis: Long,
    val activeAppCount: Int,
    val dailySummaries: List<DailyUsageSummary>,
    val topApps: List<AppUsageInfo>
)

data class ReportFilter(
    val searchQuery: String = "",
    val selectedCategory: AppCategory = AppCategory.ALL
)

enum class ExportFormat {
    IMAGE,
    PDF
}
