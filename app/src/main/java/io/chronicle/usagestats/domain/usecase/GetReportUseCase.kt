package io.chronicle.usagestats.domain.usecase

import io.chronicle.usagestats.domain.model.AppCategory
import io.chronicle.usagestats.domain.model.AppUsageInfo
import io.chronicle.usagestats.domain.model.DailyUsageSummary
import io.chronicle.usagestats.domain.model.ReportFilter
import io.chronicle.usagestats.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetReportUseCase(
    private val usageRepository: UsageRepository
) {
    fun getDailyReport(
        dateEpochMillis: Long,
        filter: ReportFilter = ReportFilter()
    ): Flow<DailyUsageSummary> {
        return usageRepository.getDailyUsage(dateEpochMillis).map { summary ->
            val filteredApps = applyFilter(summary.apps, filter)
            summary.copy(
                apps = filteredApps,
                appCount = filteredApps.size
            )
        }
    }

    fun getRangeReport(
        startDateEpochMillis: Long,
        endDateEpochMillis: Long,
        filter: ReportFilter = ReportFilter()
    ): Flow<DailyUsageSummary> {
        return usageRepository.getRangeUsage(startDateEpochMillis, endDateEpochMillis).map { apps ->
            val filteredApps = applyFilter(apps, filter)
            val totalTime = filteredApps.sumOf { it.totalTimeForegroundMillis }
            val top = filteredApps.maxByOrNull { it.totalTimeForegroundMillis }
            DailyUsageSummary(
                dateEpochMillis = startDateEpochMillis,
                totalScreenTimeMillis = totalTime,
                topAppPackage = top?.packageName,
                topAppLabel = top?.appLabel,
                appCount = filteredApps.size,
                apps = filteredApps
            )
        }
    }

    private fun applyFilter(apps: List<AppUsageInfo>, filter: ReportFilter): List<AppUsageInfo> {
        return apps.filter { app ->
            val matchesQuery = filter.searchQuery.isBlank() ||
                    app.appLabel.contains(filter.searchQuery, ignoreCase = true) ||
                    app.packageName.contains(filter.searchQuery, ignoreCase = true)

            val matchesCategory = when (filter.selectedCategory) {
                AppCategory.ALL -> true
                AppCategory.REMOVED -> app.isRemoved
                else -> app.category == filter.selectedCategory && !app.isRemoved
            }

            matchesQuery && matchesCategory
        }
    }
}
