package io.chronicle.usagestats.domain.repository

import io.chronicle.usagestats.domain.model.AppDetailInfo
import io.chronicle.usagestats.domain.model.AppUsageInfo
import io.chronicle.usagestats.domain.model.CustomAppOverride
import io.chronicle.usagestats.domain.model.DailyUsageSummary
import io.chronicle.usagestats.domain.model.DisciplineStreaks
import io.chronicle.usagestats.domain.model.RangeUsageReport
import io.chronicle.usagestats.domain.model.TimelineData
import io.chronicle.usagestats.domain.model.TimelinePeriod
import kotlinx.coroutines.flow.Flow
import java.io.File

interface UsageRepository {

    suspend fun syncUsageForDate(dateStartEpochMillis: Long)

    suspend fun syncUsageForRange(startDateEpochMillis: Long, endDateEpochMillis: Long)

    fun getDailyUsage(dateStartEpochMillis: Long): Flow<DailyUsageSummary>

    fun getRangeUsage(startDateEpochMillis: Long, endDateEpochMillis: Long): Flow<List<AppUsageInfo>>

    fun getTimelineData(period: TimelinePeriod, referenceEpochMillis: Long): Flow<TimelineData>

    fun getRemovedAppsUsage(): Flow<List<AppUsageInfo>>

    suspend fun detectAndMarkRemovedApps()

    suspend fun getTodayTotalScreenTimeMillis(): Long

    suspend fun getTodaySummary(): DailyUsageSummary

    fun getRangeReport(startDateEpochMillis: Long, endDateEpochMillis: Long): Flow<RangeUsageReport>

    suspend fun getEarliestRecordedDate(): Long?

    suspend fun getTotalDaysTracked(): Int

    fun getAppDetail(packageName: String, dateStartEpochMillis: Long): Flow<AppDetailInfo>

    fun getDisciplineStreaks(goalMinutes: Int): Flow<DisciplineStreaks>

    suspend fun saveAppOverride(override: CustomAppOverride)

    fun getAppOverride(packageName: String): Flow<CustomAppOverride?>

    fun getAllAppOverrides(): Flow<List<CustomAppOverride>>

    suspend fun exportUsageToCsv(startDateEpochMillis: Long, endDateEpochMillis: Long): File
}
