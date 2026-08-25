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
import io.chronicle.usagestats.domain.model.HabitInsights
import io.chronicle.usagestats.domain.model.HourlyUsageSlot
import io.chronicle.usagestats.domain.model.LongestSession
import io.chronicle.usagestats.domain.model.TimelineData
import io.chronicle.usagestats.domain.model.TimelinePeriod
import io.chronicle.usagestats.domain.model.TrendComparison
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

        val result = aggregateDayData(startOfDay, queryEnd)
        val usageMap = result.appUsageMap

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
        val endOfDay = DateTimeUtils.getEndOfDay(dateStartEpochMillis)
        val now = System.currentTimeMillis()
        val queryEnd = if (endOfDay > now) now else endOfDay

        return usageDao.getUsageForDate(startOfDay).map { entities ->
            val totalTime = entities.sumOf { it.foregroundTimeMillis }
            val top = entities.maxByOrNull { it.foregroundTimeMillis }
            val domainApps = entities.map { it.toDomain(appIconHelper) }

            // Compute realtime advanced insights
            val analyticsResult = aggregateDayData(startOfDay, queryEnd)

            // Compute trend comparison vs previous day
            val previousDayStart = DateTimeUtils.toZonedDateTime(startOfDay).minusDays(1).toInstant().toEpochMilli()
            val previousDaySummary = usageDao.getSummaryForDate(previousDayStart)
            val previousDuration = previousDaySummary?.totalScreenTimeMillis ?: 0L
            val trend = calculateTrend(totalTime, previousDuration)

            DailyUsageSummary(
                dateEpochMillis = startOfDay,
                totalScreenTimeMillis = totalTime,
                topAppPackage = top?.packageName,
                topAppLabel = top?.appLabel,
                appCount = domainApps.size,
                apps = domainApps,
                hourlySlots = analyticsResult.hourlySlots,
                habitInsights = analyticsResult.habitInsights,
                trendComparison = trend
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
                    avgSessionDurationMillis = if (totalLaunches > 0) totalTime / totalLaunches else 0L,
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
                    avgSessionDurationMillis = if (launches > 0) time / launches else 0L,
                    isRemoved = isRemoved,
                    category = category
                )
            }.sortedByDescending { it.totalTimeForegroundMillis }

            // Calculate trend vs previous period
            val (prevStart, prevEnd) = getPreviousPeriodRange(period, referenceEpochMillis)
            val prevEntities = usageDao.getUsageForRangeDirect(prevStart, prevEnd)
            val prevDuration = prevEntities.sumOf { it.foregroundTimeMillis }
            val trend = calculateTrend(totalDuration, prevDuration)

            // Calculate day-specific insights for DAY period
            val dayAnalytics = if (period == TimelinePeriod.DAY) {
                val now = System.currentTimeMillis()
                val queryEnd = if (end > now) now else end
                aggregateDayData(start, queryEnd)
            } else null

            TimelineData(
                period = period,
                startEpochMillis = start,
                endEpochMillis = end,
                totalDurationMillis = totalDuration,
                activeAppCount = topApps.size,
                dailySummaries = dailySummaries,
                topApps = topApps,
                hourlySlots = dayAnalytics?.hourlySlots ?: emptyList(),
                habitInsights = dayAnalytics?.habitInsights,
                trendComparison = trend
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
                    avgSessionDurationMillis = 0L,
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
        syncUsageForDate(todayStart)
        val summary = usageDao.getSummaryForDate(todayStart)
        summary?.totalScreenTimeMillis ?: 0L
    }

    override suspend fun getTodaySummary(): DailyUsageSummary = withContext(Dispatchers.IO) {
        val todayStart = DateTimeUtils.getStartOfDay()
        val endOfDay = DateTimeUtils.getEndOfDay(todayStart)
        val now = System.currentTimeMillis()
        val queryEnd = if (endOfDay > now) now else endOfDay

        syncUsageForDate(todayStart)
        val records = usageDao.getUsageForDateDirect(todayStart)
        val totalTime = records.sumOf { it.foregroundTimeMillis }
        val top = records.maxByOrNull { it.foregroundTimeMillis }
        val domainApps = records.map { it.toDomain(appIconHelper) }

        val analytics = aggregateDayData(todayStart, queryEnd)

        DailyUsageSummary(
            dateEpochMillis = todayStart,
            totalScreenTimeMillis = totalTime,
            topAppPackage = top?.packageName,
            topAppLabel = top?.appLabel,
            appCount = domainApps.size,
            apps = domainApps,
            hourlySlots = analytics.hourlySlots,
            habitInsights = analytics.habitInsights
        )
    }

    private data class ParsedUsage(
        var totalForegroundMillis: Long = 0L,
        var launchCount: Int = 0,
        var lastTimeUsedMillis: Long = 0L,
        var sessionCount: Int = 0,
        var shortSessionsCount: Int = 0 // sessions < 2 mins
    )

    private data class DayAnalyticsResult(
        val appUsageMap: Map<String, ParsedUsage>,
        val hourlySlots: List<HourlyUsageSlot>,
        val habitInsights: HabitInsights
    )

    private fun aggregateDayData(startTime: Long, queryEnd: Long): DayAnalyticsResult {
        val manager = usageStatsManager ?: return DayAnalyticsResult(emptyMap(), emptyList(), HabitInsights())
        val usageMap = mutableMapOf<String, ParsedUsage>()

        // 24 hourly buckets
        val hourlyDurations = LongArray(24) { 0L }
        val hourlyAppBreakdown = Array(24) { mutableMapOf<String, Long>() }

        val queryBufferStart = maxOf(0L, startTime - (24 * 3600 * 1000L))
        val events = manager.queryEvents(queryBufferStart, queryEnd) ?: return DayAnalyticsResult(emptyMap(), emptyList(), HabitInsights())

        val event = UsageEvents.Event()
        val activeActivities = mutableMapOf<String, MutableSet<String>>()
        val sessionStartTimes = mutableMapOf<String, Long>()

        var deviceUnlocks = 0
        var firstUnlockTimestamp: Long? = null
        var lastLockTimestamp: Long? = null
        val fourAmStart = startTime + (4 * 3600 * 1000L) // 4:00 AM IST

        // Helper to partition session into hourly buckets
        fun distributeSessionToHourlySlots(pkg: String, sessionStart: Long, sessionEnd: Long) {
            val effStart = maxOf(sessionStart, startTime)
            val effEnd = minOf(sessionEnd, queryEnd)
            if (effEnd <= effStart) return

            var cursor = effStart
            while (cursor < effEnd) {
                val hourIndex = (((cursor - startTime) / (3600 * 1000L)).toInt()).coerceIn(0, 23)
                val slotEnd = startTime + ((hourIndex + 1) * 3600 * 1000L)
                val chunkEnd = minOf(effEnd, slotEnd)
                val chunkDuration = chunkEnd - cursor

                if (chunkDuration > 0) {
                    hourlyDurations[hourIndex] += chunkDuration
                    val hourMap = hourlyAppBreakdown[hourIndex]
                    hourMap[pkg] = (hourMap[pkg] ?: 0L) + chunkDuration
                }
                cursor = chunkEnd
            }
        }

        var maxSessionDuration = 0L
        var maxSessionPkg: String? = null
        var maxSessionStart = 0L
        var maxSessionEnd = 0L

        val hourlyUnlocks = IntArray(24) { 0 }

        fun closePackageSession(pkg: String, sessionEnd: Long) {
            val sessionStart = sessionStartTimes.remove(pkg) ?: return
            val effectiveStart = maxOf(sessionStart, startTime)
            val effectiveEnd = minOf(sessionEnd, queryEnd)
            if (effectiveEnd > effectiveStart) {
                val duration = effectiveEnd - effectiveStart
                if (duration in 1..(12 * 3600 * 1000L)) {
                    val parsed = usageMap.getOrPut(pkg) { ParsedUsage() }
                    parsed.totalForegroundMillis += duration
                    parsed.sessionCount++
                    if (duration < 120_000L) { // < 2 mins micro-pickup
                        parsed.shortSessionsCount++
                    }
                    if (effectiveEnd > parsed.lastTimeUsedMillis) {
                        parsed.lastTimeUsedMillis = effectiveEnd
                    }

                    if (duration > maxSessionDuration) {
                        maxSessionDuration = duration
                        maxSessionPkg = pkg
                        maxSessionStart = effectiveStart
                        maxSessionEnd = effectiveEnd
                    }

                    distributeSessionToHourlySlots(pkg, effectiveStart, effectiveEnd)
                }
            }
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            val className = event.className ?: "default_activity"
            val time = event.timeStamp

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    val activities = activeActivities.getOrPut(pkg) { mutableSetOf() }
                    val wasEmpty = activities.isEmpty()
                    activities.add(className)

                    if (wasEmpty) {
                        sessionStartTimes[pkg] = time
                    }

                    if (time in startTime..queryEnd && wasEmpty) {
                        val parsed = usageMap.getOrPut(pkg) { ParsedUsage() }
                        parsed.launchCount++
                        if (time > parsed.lastTimeUsedMillis) {
                            parsed.lastTimeUsedMillis = time
                        }
                    }
                }

                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val activities = activeActivities[pkg]
                    if (activities != null) {
                        activities.remove(className)
                        if (activities.isEmpty()) {
                            activeActivities.remove(pkg)
                            closePackageSession(pkg, time)
                        }
                    }
                }

                UsageEvents.Event.SCREEN_INTERACTIVE,
                UsageEvents.Event.KEYGUARD_HIDDEN -> {
                    if (time in startTime..queryEnd) {
                        deviceUnlocks++
                        val hourIdx = (((time - startTime) / (3600 * 1000L)).toInt()).coerceIn(0, 23)
                        hourlyUnlocks[hourIdx]++
                        if (time >= fourAmStart && firstUnlockTimestamp == null) {
                            firstUnlockTimestamp = time
                        }
                    }
                }

                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.KEYGUARD_SHOWN,
                UsageEvents.Event.DEVICE_SHUTDOWN -> {
                    if (time in startTime..queryEnd) {
                        lastLockTimestamp = time
                    }
                    val activePkgs = sessionStartTimes.keys.toList()
                    for (activePkg in activePkgs) {
                        closePackageSession(activePkg, time)
                    }
                    activeActivities.clear()
                    sessionStartTimes.clear()
                }
            }
        }

        // Close ongoing sessions
        val now = System.currentTimeMillis()
        val finalEnd = minOf(now, queryEnd)
        for (activePkg in sessionStartTimes.keys.toList()) {
            closePackageSession(activePkg, finalEnd)
        }

        // Build 24 Hourly Slots
        val hourlySlots = (0 until 24).map { hour ->
            val slotDuration = hourlyDurations[hour]
            val breakdownMap = hourlyAppBreakdown[hour]
            val topEntry = breakdownMap.maxByOrNull { it.value }
            val topLabel = topEntry?.key?.let { appIconHelper.getAppLabel(it) }

            val appList = breakdownMap.map { (p, dur) ->
                AppUsageInfo(
                    packageName = p,
                    appLabel = appIconHelper.getAppLabel(p),
                    totalTimeForegroundMillis = dur,
                    category = appIconHelper.getAppCategory(p)
                )
            }.sortedByDescending { it.totalTimeForegroundMillis }

            HourlyUsageSlot(
                hour = hour,
                totalDurationMillis = slotDuration,
                topAppPackage = topEntry?.key,
                topAppLabel = topLabel,
                appBreakdown = appList
            )
        }

        // Calculate Category Breakdown & Productivity Score
        val categoryBreakdown = mutableMapOf<AppCategory, Long>()
        var totalTime = 0L
        for ((p, metrics) in usageMap) {
            val cat = appIconHelper.getAppCategory(p)
            categoryBreakdown[cat] = (categoryBreakdown[cat] ?: 0L) + metrics.totalForegroundMillis
            totalTime += metrics.totalForegroundMillis
        }

        val productiveTime = categoryBreakdown[AppCategory.PRODUCTIVITY] ?: 0L
        val utilitiesTime = categoryBreakdown[AppCategory.UTILITIES] ?: 0L
        val communicationTime = categoryBreakdown[AppCategory.COMMUNICATION] ?: 0L
        val productivityScore = if (totalTime > 0) {
            (((productiveTime + (utilitiesTime * 0.7) + (communicationTime * 0.5)) / totalTime) * 100).toInt().coerceIn(0, 100)
        } else 0

        // Calculate Fragmentation & Average Session
        val totalSessions = usageMap.values.sumOf { it.sessionCount }
        val shortSessions = usageMap.values.sumOf { it.shortSessionsCount }
        val fragmentationScore = if (totalSessions > 0) {
            ((shortSessions.toDouble() / totalSessions) * 100).toInt().coerceIn(0, 100)
        } else 0

        val avgSessionDuration = if (totalSessions > 0) totalTime / totalSessions else 0L

        // Bedtime usage: screen time in the 60 minutes prior to lastLock
        var bedtimeUsage = 0L
        if (lastLockTimestamp != null) {
            val bedtimeWindowStart = lastLockTimestamp - (60 * 60 * 1000L)
            for (hour in 0 until 24) {
                val hourStart = startTime + (hour * 3600 * 1000L)
                val hourEnd = hourStart + (3600 * 1000L)
                if (hourEnd > bedtimeWindowStart && hourStart < lastLockTimestamp) {
                    val overlap = minOf(hourEnd, lastLockTimestamp) - maxOf(hourStart, bedtimeWindowStart)
                    if (overlap > 0 && hourlyDurations[hour] > 0) {
                        bedtimeUsage += minOf(hourlyDurations[hour], overlap)
                    }
                }
            }
        }

        val longestSession: LongestSession? = maxSessionPkg?.let { pkg: String ->
            LongestSession(
                packageName = pkg,
                appLabel = appIconHelper.getAppLabel(pkg),
                durationMillis = maxSessionDuration,
                startEpochMillis = maxSessionStart,
                endEpochMillis = maxSessionEnd
            )
        }

        val peakHourIndex: Int? = (0 until 24).maxByOrNull { i: Int -> hourlyUnlocks[i] }
        val peakCount = peakHourIndex?.let { i: Int -> hourlyUnlocks[i] } ?: 0

        val habitInsights = HabitInsights(
            deviceUnlocks = maxOf(deviceUnlocks, usageMap.values.sumOf { it.launchCount }),
            firstUnlockEpochMillis = firstUnlockTimestamp,
            lastLockEpochMillis = lastLockTimestamp,
            bedtimeUsageMillis = bedtimeUsage,
            avgSessionDurationMillis = avgSessionDuration,
            fragmentationScore = fragmentationScore,
            productivityScore = productivityScore,
            categoryBreakdown = categoryBreakdown,
            longestSession = longestSession,
            peakUnlockHour = if (peakCount > 0) peakHourIndex else null,
            peakUnlockCount = peakCount,
            hourlyUnlocks = hourlyUnlocks.toList()
        )

        return DayAnalyticsResult(
            appUsageMap = usageMap,
            hourlySlots = hourlySlots,
            habitInsights = habitInsights
        )
    }

    private fun calculateTrend(currentDuration: Long, previousDuration: Long): TrendComparison {
        val delta = currentDuration - previousDuration
        val percentage = if (previousDuration > 0) {
            ((delta.toDouble() / previousDuration) * 100.0)
        } else {
            0.0
        }
        return TrendComparison(
            previousPeriodDurationMillis = previousDuration,
            deltaDurationMillis = delta,
            percentageChange = percentage
        )
    }

    private fun getPreviousPeriodRange(period: TimelinePeriod, referenceEpochMillis: Long): Pair<Long, Long> {
        val currentZdt = DateTimeUtils.toZonedDateTime(referenceEpochMillis)
        return when (period) {
            TimelinePeriod.DAY -> {
                val prev = currentZdt.minusDays(1).toInstant().toEpochMilli()
                Pair(DateTimeUtils.getStartOfDay(prev), DateTimeUtils.getEndOfDay(prev))
            }
            TimelinePeriod.WEEK -> {
                val prev = currentZdt.minusWeeks(1).toInstant().toEpochMilli()
                Pair(DateTimeUtils.getStartOfWeek(prev), DateTimeUtils.getEndOfWeek(prev))
            }
            TimelinePeriod.MONTH -> {
                val prev = currentZdt.minusMonths(1).toInstant().toEpochMilli()
                Pair(DateTimeUtils.getStartOfMonth(prev), DateTimeUtils.getEndOfMonth(prev))
            }
            TimelinePeriod.YEAR -> {
                val prev = currentZdt.minusYears(1).toInstant().toEpochMilli()
                Pair(DateTimeUtils.getStartOfYear(prev), DateTimeUtils.getEndOfYear(prev))
            }
        }
    }

    private fun AppUsageEntity.toDomain(iconHelper: AppIconHelper): AppUsageInfo {
        val resolvedCategory = try {
            AppCategory.valueOf(this.category)
        } catch (_: Exception) {
            if (this.isRemoved) AppCategory.REMOVED else iconHelper.getAppCategory(this.packageName)
        }

        val avgSession = if (this.launchCount > 0) this.foregroundTimeMillis / this.launchCount else 0L

        return AppUsageInfo(
            packageName = this.packageName,
            appLabel = this.appLabel,
            totalTimeForegroundMillis = this.foregroundTimeMillis,
            launchCount = this.launchCount,
            lastTimeUsedMillis = this.lastTimeUsedMillis,
            avgSessionDurationMillis = avgSession,
            isRemoved = this.isRemoved,
            category = resolvedCategory
        )
    }
}
