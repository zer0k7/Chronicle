package io.chronicle.usagestats.data.repository

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import io.chronicle.usagestats.core.util.AppIconHelper
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.data.local.dao.UsageDao
import io.chronicle.usagestats.data.local.entity.AppUsageEntity
import io.chronicle.usagestats.data.local.entity.DailySummaryEntity
import io.chronicle.usagestats.domain.model.AppCategory
import io.chronicle.usagestats.domain.model.AppUsageInfo
import io.chronicle.usagestats.domain.model.DailyUsageSummary
import io.chronicle.usagestats.domain.model.TimelineData
import io.chronicle.usagestats.domain.model.TimelinePeriod
import io.chronicle.usagestats.domain.repository.UsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class UsageStatsRepositoryImpl(
    private val context: Context,
    private val usageDao: UsageDao,
    private val appIconHelper: AppIconHelper
) : UsageRepository {

    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    override suspend fun syncUsageForDate(dateStartEpochMillis: Long) = withContext(Dispatchers.IO) {
        val startOfDay = DateTimeUtils.getStartOfDay(dateStartEpochMillis)
        val endOfDay = DateTimeUtils.getEndOfDay(dateStartEpochMillis)
        val now = System.currentTimeMillis()
        val queryEnd = if (endOfDay > now) now else endOfDay

        if (startOfDay >= queryEnd) {
            return@withContext
        }

        val usageMap = aggregateUsageEvents(startOfDay, queryEnd)

        // Query existing records to preserve any removed apps data
        val existingRecords = usageDao.getUsageForDateDirect(startOfDay).associateBy { it.packageName }
        val entities = mutableListOf<AppUsageEntity>()

        for ((pkgName, metrics) in usageMap) {
            if (metrics.totalForegroundMillis <= 0 && metrics.launchCount == 0) continue

            val isInstalled = appIconHelper.isAppInstalled(pkgName)
            val label = appIconHelper.getAppLabel(pkgName, existingRecords[pkgName]?.appLabel)
            val category = if (!isInstalled) {
                AppCategory.REMOVED.name
            } else {
                appIconHelper.getAppCategory(pkgName).name
            }

            entities.add(
                AppUsageEntity(
                    id = existingRecords[pkgName]?.id ?: 0,
                    dateStartEpochMillis = startOfDay,
                    packageName = pkgName,
                    appLabel = label,
                    foregroundTimeMillis = metrics.totalForegroundMillis,
                    launchCount = metrics.launchCount,
                    lastTimeUsedMillis = metrics.lastTimeUsedMillis,
                    isRemoved = !isInstalled,
                    category = category
                )
            )
        }

        // Preserve uninstalled apps that were recorded previously for this day
        for ((pkgName, existing) in existingRecords) {
            if (!usageMap.containsKey(pkgName)) {
                val isInstalled = appIconHelper.isAppInstalled(pkgName)
                if (!isInstalled) {
                    entities.add(
                        existing.copy(
                            isRemoved = true,
                            category = AppCategory.REMOVED.name
                        )
                    )
                }
            }
        }

        val totalScreenTime = entities.sumOf { it.foregroundTimeMillis }
        val topApp = entities.maxByOrNull { it.foregroundTimeMillis }

        val summary = DailySummaryEntity(
            dateStartEpochMillis = startOfDay,
            totalScreenTimeMillis = totalScreenTime,
            topPackageName = topApp?.packageName,
            topAppLabel = topApp?.appLabel,
            activeAppsCount = entities.size,
            lastUpdatedEpochMillis = System.currentTimeMillis()
        )

        usageDao.upsertDayUsageAndSummary(entities, summary)
        detectAndMarkRemovedApps()
    }

    override suspend fun syncUsageForRange(
        startDateEpochMillis: Long,
        endDateEpochMillis: Long
    ) = withContext(Dispatchers.IO) {
        val days = DateTimeUtils.getDaysInRange(startDateEpochMillis, endDateEpochMillis)
        for (day in days) {
            val dayStart = day.atStartOfDay(DateTimeUtils.IST_ZONE_ID).toInstant().toEpochMilli()
            syncUsageForDate(dayStart)
        }
    }

    override fun getDailyUsage(dateStartEpochMillis: Long): Flow<DailyUsageSummary> {
        val startOfDay = DateTimeUtils.getStartOfDay(dateStartEpochMillis)
        return usageDao.getUsageForDate(startOfDay).map { entities ->
            val totalTime = entities.sumOf { it.foregroundTimeMillis }
            val top = entities.maxByOrNull { it.foregroundTimeMillis }
            val domainApps = entities.map { it.toDomain(appIconHelper) }
            DailyUsageSummary(
                dateEpochMillis = startOfDay,
                totalScreenTimeMillis = totalTime,
                topAppPackage = top?.packageName,
                topAppLabel = top?.appLabel,
                appCount = domainApps.size,
                apps = domainApps
            )
        }.flowOn(Dispatchers.IO)
    }

    override fun getRangeUsage(
        startDateEpochMillis: Long,
        endDateEpochMillis: Long
    ): Flow<List<AppUsageInfo>> {
        val start = DateTimeUtils.getStartOfDay(startDateEpochMillis)
        val end = DateTimeUtils.getEndOfDay(endDateEpochMillis)
        return usageDao.getUsageForRange(start, end).map { entities ->
            // Group by package name across multiple days
            entities.groupBy { it.packageName }.map { (pkg, list) ->
                val totalTime = list.sumOf { it.foregroundTimeMillis }
                val totalLaunches = list.sumOf { it.launchCount }
                val lastUsed = list.maxOfOrNull { it.lastTimeUsedMillis } ?: 0L
                val isRemoved = list.any { it.isRemoved } || !appIconHelper.isAppInstalled(pkg)
                val label = list.firstOrNull()?.appLabel ?: appIconHelper.getAppLabel(pkg)
                val category = if (isRemoved) {
                    AppCategory.REMOVED
                } else {
                    appIconHelper.getAppCategory(pkg)
                }

                AppUsageInfo(
                    packageName = pkg,
                    appLabel = label,
                    totalTimeForegroundMillis = totalTime,
                    launchCount = totalLaunches,
                    lastTimeUsedMillis = lastUsed,
                    isRemoved = isRemoved,
                    category = category
                )
            }.sortedByDescending { it.totalTimeForegroundMillis }
        }.flowOn(Dispatchers.IO)
    }

    override fun getTimelineData(
        period: TimelinePeriod,
        referenceEpochMillis: Long
    ): Flow<TimelineData> {
        val (start, end) = when (period) {
            TimelinePeriod.DAY -> Pair(
                DateTimeUtils.getStartOfDay(referenceEpochMillis),
                DateTimeUtils.getEndOfDay(referenceEpochMillis)
            )
            TimelinePeriod.WEEK -> Pair(
                DateTimeUtils.getStartOfWeek(referenceEpochMillis),
                DateTimeUtils.getEndOfWeek(referenceEpochMillis)
            )
            TimelinePeriod.MONTH -> Pair(
                DateTimeUtils.getStartOfMonth(referenceEpochMillis),
                DateTimeUtils.getEndOfMonth(referenceEpochMillis)
            )
            TimelinePeriod.YEAR -> Pair(
                DateTimeUtils.getStartOfYear(referenceEpochMillis),
                DateTimeUtils.getEndOfYear(referenceEpochMillis)
            )
        }

        return usageDao.getUsageForRange(start, end).map { entities ->
            val totalDuration = entities.sumOf { it.foregroundTimeMillis }

            // Group by day for daily summaries
            val dailySummaries = entities.groupBy { it.dateStartEpochMillis }
                .map { (dayStart, dayEntities) ->
                    val dayTotal = dayEntities.sumOf { it.foregroundTimeMillis }
                    val dayTop = dayEntities.maxByOrNull { it.foregroundTimeMillis }
                    DailyUsageSummary(
                        dateEpochMillis = dayStart,
                        totalScreenTimeMillis = dayTotal,
                        topAppPackage = dayTop?.packageName,
                        topAppLabel = dayTop?.appLabel,
                        appCount = dayEntities.size,
                        apps = dayEntities.map { it.toDomain(appIconHelper) }
                    )
                }.sortedBy { it.dateEpochMillis }

            // Group by package for top apps
            val topApps = entities.groupBy { it.packageName }.map { (pkg, list) ->
                val time = list.sumOf { it.foregroundTimeMillis }
                val launches = list.sumOf { it.launchCount }
                val isRemoved = list.any { it.isRemoved } || !appIconHelper.isAppInstalled(pkg)
                val label = list.firstOrNull()?.appLabel ?: appIconHelper.getAppLabel(pkg)
                val category = if (isRemoved) AppCategory.REMOVED else appIconHelper.getAppCategory(pkg)

                AppUsageInfo(
                    packageName = pkg,
                    appLabel = label,
                    totalTimeForegroundMillis = time,
                    launchCount = launches,
                    lastTimeUsedMillis = list.maxOfOrNull { it.lastTimeUsedMillis } ?: 0L,
                    isRemoved = isRemoved,
                    category = category
                )
            }.sortedByDescending { it.totalTimeForegroundMillis }

            TimelineData(
                period = period,
                startEpochMillis = start,
                endEpochMillis = end,
                totalDurationMillis = totalDuration,
                activeAppCount = topApps.size,
                dailySummaries = dailySummaries,
                topApps = topApps
            )
        }.flowOn(Dispatchers.IO)
    }

    override fun getRemovedAppsUsage(): Flow<List<AppUsageInfo>> {
        return usageDao.getRemovedAppsUsage().map { entities ->
            entities.groupBy { it.packageName }.map { (pkg, list) ->
                AppUsageInfo(
                    packageName = pkg,
                    appLabel = list.firstOrNull()?.appLabel ?: appIconHelper.getAppLabel(pkg),
                    totalTimeForegroundMillis = list.sumOf { it.foregroundTimeMillis },
                    launchCount = list.sumOf { it.launchCount },
                    lastTimeUsedMillis = list.maxOfOrNull { it.lastTimeUsedMillis } ?: 0L,
                    isRemoved = true,
                    category = AppCategory.REMOVED
                )
            }.sortedByDescending { it.totalTimeForegroundMillis }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun detectAndMarkRemovedApps() = withContext(Dispatchers.IO) {
        val recordedPackages = usageDao.getAllRecordedPackageNames()
        for (pkg in recordedPackages) {
            if (!appIconHelper.isAppInstalled(pkg)) {
                usageDao.markAppAsRemoved(pkg)
            }
        }
    }

    override suspend fun getTodayTotalScreenTimeMillis(): Long = withContext(Dispatchers.IO) {
        val todayStart = DateTimeUtils.getStartOfDay()
        val summary = usageDao.getSummaryForDate(todayStart)
        summary?.totalScreenTimeMillis ?: 0L
    }

    override suspend fun getTodaySummary(): DailyUsageSummary = withContext(Dispatchers.IO) {
        val todayStart = DateTimeUtils.getStartOfDay()
        val records = usageDao.getUsageForDateDirect(todayStart)
        val totalTime = records.sumOf { it.foregroundTimeMillis }
        val top = records.maxByOrNull { it.foregroundTimeMillis }
        val domainApps = records.map { it.toDomain(appIconHelper) }
        DailyUsageSummary(
            dateEpochMillis = todayStart,
            totalScreenTimeMillis = totalTime,
            topAppPackage = top?.packageName,
            topAppLabel = top?.appLabel,
            appCount = domainApps.size,
            apps = domainApps
        )
    }

    private data class ParsedUsage(
        var totalForegroundMillis: Long = 0L,
        var launchCount: Int = 0,
        var lastTimeUsedMillis: Long = 0L
    )

    private fun aggregateUsageEvents(startTime: Long, endTime: Long): Map<String, ParsedUsage> {
        val manager = usageStatsManager ?: return emptyMap()
        val events = manager.queryEvents(startTime, endTime) ?: return emptyMap()

        val event = UsageEvents.Event()
        val usageMap = mutableMapOf<String, ParsedUsage>()
        val startTimes = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            val parsed = usageMap.getOrPut(pkg) { ParsedUsage() }

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    startTimes[pkg] = event.timeStamp
                    parsed.launchCount++
                    if (event.timeStamp > parsed.lastTimeUsedMillis) {
                        parsed.lastTimeUsedMillis = event.timeStamp
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val resumedAt = startTimes.remove(pkg)
                    if (resumedAt != null && event.timeStamp > resumedAt) {
                        val duration = event.timeStamp - resumedAt
                        parsed.totalForegroundMillis += duration
                    }
                    if (event.timeStamp > parsed.lastTimeUsedMillis) {
                        parsed.lastTimeUsedMillis = event.timeStamp
                    }
                }
            }
        }

        // Account for any app currently in foreground at the query boundary
        val now = System.currentTimeMillis()
        val effectiveEnd = if (endTime > now) now else endTime
        for ((pkg, startStamp) in startTimes) {
            if (effectiveEnd > startStamp) {
                val parsed = usageMap.getOrPut(pkg) { ParsedUsage() }
                parsed.totalForegroundMillis += (effectiveEnd - startStamp)
            }
        }

        // Fallback or blend with queryUsageStats if event stream was sparse
        if (usageMap.isEmpty()) {
            val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, endTime)
            stats?.forEach { stat ->
                if (stat.totalTimeInForeground > 0) {
                    val parsed = usageMap.getOrPut(stat.packageName) { ParsedUsage() }
                    parsed.totalForegroundMillis = maxOf(parsed.totalForegroundMillis, stat.totalTimeInForeground)
                    parsed.lastTimeUsedMillis = maxOf(parsed.lastTimeUsedMillis, stat.lastTimeUsed)
                }
            }
        }

        return usageMap
    }

    private fun AppUsageEntity.toDomain(iconHelper: AppIconHelper): AppUsageInfo {
        val resolvedCategory = try {
            AppCategory.valueOf(this.category)
        } catch (_: Exception) {
            if (this.isRemoved) AppCategory.REMOVED else iconHelper.getAppCategory(this.packageName)
        }

        return AppUsageInfo(
            packageName = this.packageName,
            appLabel = this.appLabel,
            totalTimeForegroundMillis = this.foregroundTimeMillis,
            launchCount = this.launchCount,
            lastTimeUsedMillis = this.lastTimeUsedMillis,
            isRemoved = this.isRemoved,
            category = resolvedCategory
        )
    }
}
