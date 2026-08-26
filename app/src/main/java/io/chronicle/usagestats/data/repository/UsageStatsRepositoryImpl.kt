package io.chronicle.usagestats.data.repository

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import io.chronicle.usagestats.core.util.AppIconHelper
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.data.local.dao.AppOverrideDao
import io.chronicle.usagestats.data.local.dao.UsageDao
import io.chronicle.usagestats.data.local.entity.AppOverrideEntity
import io.chronicle.usagestats.data.local.entity.AppUsageEntity
import io.chronicle.usagestats.data.local.entity.DailySummaryEntity
import io.chronicle.usagestats.domain.model.AppCategory
import io.chronicle.usagestats.domain.model.AppDetailInfo
import io.chronicle.usagestats.domain.model.AppUsageInfo
import io.chronicle.usagestats.domain.model.CustomAppOverride
import io.chronicle.usagestats.domain.model.DailyUsageSummary
import io.chronicle.usagestats.domain.model.DisciplineStreaks
import io.chronicle.usagestats.domain.model.DopamineDebt
import io.chronicle.usagestats.domain.model.GhostOpensInsight
import io.chronicle.usagestats.domain.model.HabitInsights
import io.chronicle.usagestats.domain.model.HourlyUsageSlot
import io.chronicle.usagestats.domain.model.LifeClockProjection
import io.chronicle.usagestats.domain.model.LongestSession
import io.chronicle.usagestats.domain.model.MorningDoomscroll
import io.chronicle.usagestats.domain.model.PhantomUnlocks
import io.chronicle.usagestats.domain.model.RangeUsageReport
import io.chronicle.usagestats.domain.model.TimelineData
import io.chronicle.usagestats.domain.model.TimelinePeriod
import io.chronicle.usagestats.domain.model.TrendComparison
import io.chronicle.usagestats.domain.model.WakingLifeImpact
import io.chronicle.usagestats.domain.repository.UsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

class UsageStatsRepositoryImpl(
    private val context: Context,
    private val usageDao: UsageDao,
    private val appOverrideDao: AppOverrideDao,
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
        val overrides = appOverrideDao.getAllOverridesDirect().associateBy { it.packageName }
        val entities = mutableListOf<AppUsageEntity>()

        for ((pkgName, metrics) in usageMap) {
            if (metrics.totalForegroundMillis <= 0 && metrics.launchCount == 0) continue

            val isInstalled = appIconHelper.isAppInstalled(pkgName)
            val label = appIconHelper.getAppLabel(pkgName, existingRecords[pkgName]?.appLabel)
            val override = overrides[pkgName]
            val category = when {
                !isInstalled -> AppCategory.REMOVED.name
                override?.customCategory != null -> override.customCategory
                else -> appIconHelper.getAppCategory(pkgName).name
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

        return combine(
            usageDao.getUsageForDate(startOfDay),
            appOverrideDao.getAllOverrides()
        ) { entities, overridesList ->
            val overridesMap = overridesList.associateBy { it.packageName }
            val totalTime = entities.sumOf { it.foregroundTimeMillis }
            val top = entities.maxByOrNull { it.foregroundTimeMillis }
            val domainApps = entities.map { it.toDomain(appIconHelper, overridesMap[it.packageName]) }

            val analyticsResult = aggregateDayData(startOfDay, queryEnd)

            val previousDayStart = DateTimeUtils.toZonedDateTime(startOfDay).minusDays(1).toInstant().toEpochMilli()
            val previousDaySummary = usageDao.getSummaryForDate(previousDayStart)
            val previousDuration = previousDaySummary?.totalScreenTimeMillis ?: 0L
            val trend = calculateTrend(totalTime, previousDuration)

            // Compute discipline streaks
            val summariesList = usageDao.getAllSummariesDirect()
            val streaks = calculateStreaksFromSummaries(summariesList, 150)
            val habitWithStreaks = analyticsResult.habitInsights.copy(disciplineStreaks = streaks)

            DailyUsageSummary(
                dateEpochMillis = startOfDay,
                totalScreenTimeMillis = totalTime,
                topAppPackage = top?.packageName,
                topAppLabel = top?.appLabel,
                appCount = domainApps.size,
                apps = domainApps,
                hourlySlots = analyticsResult.hourlySlots,
                habitInsights = habitWithStreaks,
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
        return combine(
            usageDao.getUsageForRange(start, end),
            appOverrideDao.getAllOverrides()
        ) { entities, overridesList ->
            val overridesMap = overridesList.associateBy { it.packageName }
            entities.groupBy { it.packageName }.map { (pkg, list) ->
                val totalTime = list.sumOf { it.foregroundTimeMillis }
                val totalLaunches = list.sumOf { it.launchCount }
                val lastUsed = list.maxOfOrNull { it.lastTimeUsedMillis } ?: 0L
                val isRemoved = list.any { it.isRemoved } || !appIconHelper.isAppInstalled(pkg)
                val label = list.firstOrNull()?.appLabel ?: appIconHelper.getAppLabel(pkg)
                val override = overridesMap[pkg]
                val category = when {
                    isRemoved -> AppCategory.REMOVED
                    override?.customCategory != null -> try {
                        AppCategory.valueOf(override.customCategory)
                    } catch (_: Exception) {
                        appIconHelper.getAppCategory(pkg)
                    }
                    else -> appIconHelper.getAppCategory(pkg)
                }

                AppUsageInfo(
                    packageName = pkg,
                    appLabel = label,
                    totalTimeForegroundMillis = totalTime,
                    launchCount = totalLaunches,
                    lastTimeUsedMillis = lastUsed,
                    avgSessionDurationMillis = if (totalLaunches > 0) totalTime / totalLaunches else 0L,
                    isRemoved = isRemoved,
                    category = category,
                    dailyLimitMinutes = override?.dailyLimitMinutes,
                    isDistraction = override?.isDistraction ?: false
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

        return combine(
            usageDao.getUsageForRange(start, end),
            appOverrideDao.getAllOverrides()
        ) { entities, overridesList ->
            val overridesMap = overridesList.associateBy { it.packageName }
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
                        apps = dayEntities.map { it.toDomain(appIconHelper, overridesMap[it.packageName]) }
                    )
                }.sortedBy { it.dateEpochMillis }

            val topApps = entities.groupBy { it.packageName }.map { (pkg, list) ->
                val time = list.sumOf { it.foregroundTimeMillis }
                val launches = list.sumOf { it.launchCount }
                val isRemoved = list.any { it.isRemoved } || !appIconHelper.isAppInstalled(pkg)
                val label = list.firstOrNull()?.appLabel ?: appIconHelper.getAppLabel(pkg)
                val override = overridesMap[pkg]
                val category = when {
                    isRemoved -> AppCategory.REMOVED
                    override?.customCategory != null -> try {
                        AppCategory.valueOf(override.customCategory)
                    } catch (_: Exception) {
                        appIconHelper.getAppCategory(pkg)
                    }
                    else -> appIconHelper.getAppCategory(pkg)
                }

                AppUsageInfo(
                    packageName = pkg,
                    appLabel = label,
                    totalTimeForegroundMillis = time,
                    launchCount = launches,
                    lastTimeUsedMillis = list.maxOfOrNull { it.lastTimeUsedMillis } ?: 0L,
                    avgSessionDurationMillis = if (launches > 0) time / launches else 0L,
                    isRemoved = isRemoved,
                    category = category,
                    dailyLimitMinutes = override?.dailyLimitMinutes,
                    isDistraction = override?.isDistraction ?: false
                )
            }.sortedByDescending { it.totalTimeForegroundMillis }

            val (prevStart, prevEnd) = getPreviousPeriodRange(period, referenceEpochMillis)
            val prevEntities = usageDao.getUsageForRangeDirect(prevStart, prevEnd)
            val prevDuration = prevEntities.sumOf { it.foregroundTimeMillis }
            val trend = calculateTrend(totalDuration, prevDuration)

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
        val overrides = appOverrideDao.getAllOverridesDirect().associateBy { it.packageName }
        val totalTime = records.sumOf { it.foregroundTimeMillis }
        val top = records.maxByOrNull { it.foregroundTimeMillis }
        val domainApps = records.map { it.toDomain(appIconHelper, overrides[it.packageName]) }

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

    override fun getRangeReport(
        startDateEpochMillis: Long,
        endDateEpochMillis: Long
    ): Flow<RangeUsageReport> {
        val start = DateTimeUtils.getStartOfDay(startDateEpochMillis)
        val end = DateTimeUtils.getEndOfDay(endDateEpochMillis)

        return combine(
            usageDao.getUsageForRange(start, end),
            appOverrideDao.getAllOverrides()
        ) { entities, overridesList ->
            val overridesMap = overridesList.associateBy { it.packageName }
            val totalTime = entities.sumOf { it.foregroundTimeMillis }
            val dailySummariesEntities = usageDao.getSummariesInRangeDirect(start, end)
            val daysCount = maxOf(1, dailySummariesEntities.size)
            val dailyAverage = totalTime / daysCount

            val appList = entities.groupBy { it.packageName }.map { (pkg, list) ->
                val appTotal = list.sumOf { it.foregroundTimeMillis }
                val launches = list.sumOf { it.launchCount }
                val isRemoved = list.any { it.isRemoved } || !appIconHelper.isAppInstalled(pkg)
                val override = overridesMap[pkg]
                val category = when {
                    isRemoved -> AppCategory.REMOVED
                    override?.customCategory != null -> try {
                        AppCategory.valueOf(override.customCategory)
                    } catch (_: Exception) {
                        appIconHelper.getAppCategory(pkg)
                    }
                    else -> appIconHelper.getAppCategory(pkg)
                }

                AppUsageInfo(
                    packageName = pkg,
                    appLabel = list.firstOrNull()?.appLabel ?: appIconHelper.getAppLabel(pkg),
                    totalTimeForegroundMillis = appTotal,
                    launchCount = launches,
                    lastTimeUsedMillis = list.maxOfOrNull { it.lastTimeUsedMillis } ?: 0L,
                    avgSessionDurationMillis = if (launches > 0) appTotal / launches else 0L,
                    isRemoved = isRemoved,
                    category = category,
                    dailyLimitMinutes = override?.dailyLimitMinutes,
                    isDistraction = override?.isDistraction ?: false
                )
            }.sortedByDescending { it.totalTimeForegroundMillis }

            val catMap = mutableMapOf<AppCategory, Long>()
            for (app in appList) {
                catMap[app.category] = (catMap[app.category] ?: 0L) + app.totalTimeForegroundMillis
            }

            val summaries = dailySummariesEntities.map { s ->
                DailyUsageSummary(
                    dateEpochMillis = s.dateStartEpochMillis,
                    totalScreenTimeMillis = s.totalScreenTimeMillis,
                    topAppPackage = s.topPackageName,
                    topAppLabel = s.topAppLabel,
                    appCount = s.activeAppsCount
                )
            }

            val yearsLost = (dailyAverage.toDouble() / (24.0 * 3600.0 * 1000.0)) * 50.0
            val consciousPct = ((dailyAverage.toDouble() / (16.0 * 3600.0 * 1000.0)) * 100.0).coerceIn(0.0, 100.0)
            val lifeClock = LifeClockProjection(dailyAverage, yearsLost, consciousPct)

            val baseline = daysCount * 2.5 * 3600 * 1000L
            val debt = (totalTime - baseline.toLong()).coerceAtLeast(0L)
            val fastMins = ((debt.toDouble() / 3_600_000.0) * 30.0).toInt().coerceIn(0, 480)
            val dopamineDebt = DopamineDebt(totalTime, baseline.toLong(), debt, fastMins)

            RangeUsageReport(
                startDateEpochMillis = start,
                endDateEpochMillis = end,
                totalScreenTimeMillis = totalTime,
                dailyAverageMillis = dailyAverage,
                daysCount = daysCount,
                totalUnlocks = 0,
                topApps = appList,
                categoryBreakdown = catMap,
                dailySummaries = summaries,
                lifeClock = lifeClock,
                dopamineDebt = dopamineDebt
            )
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getEarliestRecordedDate(): Long? = withContext(Dispatchers.IO) {
        usageDao.getEarliestRecordedDate()
    }

    override suspend fun getTotalDaysTracked(): Int = withContext(Dispatchers.IO) {
        usageDao.getTotalDaysTracked()
    }

    override fun getAppDetail(
        packageName: String,
        dateStartEpochMillis: Long
    ): Flow<AppDetailInfo> = flow {
        val startOfDay = DateTimeUtils.getStartOfDay(dateStartEpochMillis)
        val endOfDay = DateTimeUtils.getEndOfDay(dateStartEpochMillis)
        val now = System.currentTimeMillis()
        val queryEnd = if (endOfDay > now) now else endOfDay

        val isInstalled = appIconHelper.isAppInstalled(packageName)
        val label = appIconHelper.getAppLabel(packageName)
        val override = appOverrideDao.getOverrideDirect(packageName)

        val resolvedCategory = when {
            !isInstalled -> AppCategory.REMOVED
            override?.customCategory != null -> try {
                AppCategory.valueOf(override.customCategory)
            } catch (_: Exception) {
                appIconHelper.getAppCategory(packageName)
            }
            else -> appIconHelper.getAppCategory(packageName)
        }

        // Aggregate 24 hourly slots specifically for this app
        val singleAppHourlySlots = aggregateSingleAppHourlySlots(packageName, startOfDay, queryEnd)
        val todayRecord = usageDao.getUsageForDateDirect(startOfDay).find { it.packageName == packageName }
        val totalDailyScreenTime = usageDao.getSummaryForDate(startOfDay)?.totalScreenTimeMillis ?: 1L

        val todayTime = todayRecord?.foregroundTimeMillis ?: 0L
        val todayLaunches = todayRecord?.launchCount ?: 0
        val todayAvgSession = if (todayLaunches > 0) todayTime / todayLaunches else 0L
        val pctTotal = if (totalDailyScreenTime > 0) {
            (todayTime.toFloat() / totalDailyScreenTime.toFloat()).coerceIn(0f, 1f)
        } else 0f

        // Query past 7 days history for this specific app
        val sevenDaysAgo = DateTimeUtils.toZonedDateTime(startOfDay).minusDays(6).toInstant().toEpochMilli()
        val rangeRecords = usageDao.getUsageForRangeDirect(sevenDaysAgo, endOfDay)
            .filter { it.packageName == packageName }
            .associateBy { it.dateStartEpochMillis }

        val recentDaysUsage = mutableListOf<Pair<Long, Long>>()
        for (i in 6 downTo 0) {
            val d = DateTimeUtils.toZonedDateTime(startOfDay).minusDays(i.toLong()).toInstant().toEpochMilli()
            val dayStart = DateTimeUtils.getStartOfDay(d)
            val time = rangeRecords[dayStart]?.foregroundTimeMillis ?: 0L
            recentDaysUsage.add(Pair(dayStart, time))
        }

        // Count ghost reflex opens (<30s) for this app today
        val ghostOpens = countGhostOpensForApp(packageName, startOfDay, queryEnd)

        emit(
            AppDetailInfo(
                packageName = packageName,
                appLabel = label,
                category = resolvedCategory,
                isRemoved = !isInstalled,
                isDistraction = override?.isDistraction ?: false,
                dailyLimitMinutes = override?.dailyLimitMinutes,
                todayForegroundMillis = todayTime,
                todayLaunchCount = todayLaunches,
                todayAvgSessionMillis = todayAvgSession,
                percentageOfTotalDaily = pctTotal,
                hourlySlots = singleAppHourlySlots,
                recentDaysUsage = recentDaysUsage,
                ghostOpensCount = ghostOpens
            )
        )
    }.flowOn(Dispatchers.IO)

    override fun getDisciplineStreaks(goalMinutes: Int): Flow<DisciplineStreaks> = flow {
        val allSummaries = usageDao.getAllSummariesDirect()
        emit(calculateStreaksFromSummaries(allSummaries, goalMinutes))
    }.flowOn(Dispatchers.IO)

    override suspend fun saveAppOverride(override: CustomAppOverride) = withContext(Dispatchers.IO) {
        appOverrideDao.upsertOverride(
            AppOverrideEntity(
                packageName = override.packageName,
                customCategory = override.customCategory?.name,
                isDistraction = override.isDistraction,
                dailyLimitMinutes = override.dailyLimitMinutes,
                updatedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    override fun getAppOverride(packageName: String): Flow<CustomAppOverride?> {
        return appOverrideDao.getOverride(packageName).map { entity ->
            entity?.let {
                CustomAppOverride(
                    packageName = it.packageName,
                    customCategory = it.customCategory?.let { catStr ->
                        try { AppCategory.valueOf(catStr) } catch (_: Exception) { null }
                    },
                    isDistraction = it.isDistraction,
                    dailyLimitMinutes = it.dailyLimitMinutes
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getAllAppOverrides(): Flow<List<CustomAppOverride>> {
        return appOverrideDao.getAllOverrides().map { list ->
            list.map { entity ->
                CustomAppOverride(
                    packageName = entity.packageName,
                    customCategory = entity.customCategory?.let { catStr ->
                        try { AppCategory.valueOf(catStr) } catch (_: Exception) { null }
                    },
                    isDistraction = entity.isDistraction,
                    dailyLimitMinutes = entity.dailyLimitMinutes
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun exportUsageToCsv(
        startDateEpochMillis: Long,
        endDateEpochMillis: Long
    ): File = withContext(Dispatchers.IO) {
        val start = DateTimeUtils.getStartOfDay(startDateEpochMillis)
        val end = DateTimeUtils.getEndOfDay(endDateEpochMillis)

        val records = usageDao.getUsageForRangeDirect(start, end)
        val exportDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val exportFile = File(exportDir, "chronicle_usage_export_${System.currentTimeMillis()}.csv")

        FileWriter(exportFile).use { writer ->
            writer.append("Date,Package Name,App Name,Category,Foreground Time (Minutes),Launch Count,Last Used (IST)\n")
            for (record in records) {
                val dateStr = DateTimeUtils.formatDate(record.dateStartEpochMillis)
                val durationMinutes = record.foregroundTimeMillis / 60000.0
                val lastUsedStr = if (record.lastTimeUsedMillis > 0) {
                    DateTimeUtils.formatDateTime(record.lastTimeUsedMillis)
                } else "N/A"

                val escapedLabel = "\"" + record.appLabel.replace("\"", "\"\"") + "\""
                val escapedPkg = "\"" + record.packageName.replace("\"", "\"\"") + "\""

                writer.append("$dateStr,$escapedPkg,$escapedLabel,${record.category},${String.format(java.util.Locale.ENGLISH, "%.2f", durationMinutes)},${record.launchCount},$lastUsedStr\n")
            }
        }

        exportFile
    }

    private fun calculateStreaksFromSummaries(
        summaries: List<DailyUsageSummaryEntity>,
        goalMinutes: Int
    ): DisciplineStreaks {
        if (summaries.isEmpty()) return DisciplineStreaks()

        val sorted = summaries.sortedByDescending { it.dateStartEpochMillis }
        val goalLimitMillis = goalMinutes * 60 * 1000L

        var currentGoalStreak = 0
        var bestGoalStreak = 0
        var countingCurrent = true

        var tempStreak = 0
        for (summary in sorted) {
            if (summary.totalScreenTimeMillis <= goalLimitMillis) {
                tempStreak++
                if (countingCurrent) currentGoalStreak++
                if (tempStreak > bestGoalStreak) bestGoalStreak = tempStreak
            } else {
                countingCurrent = false
                tempStreak = 0
            }
        }

        return DisciplineStreaks(
            goalStreakDays = currentGoalStreak,
            morningShieldStreakDays = maxOf(1, currentGoalStreak),
            sleepSanctuaryStreakDays = maxOf(1, (currentGoalStreak * 0.8).toInt()),
            bestGoalStreakDays = maxOf(bestGoalStreak, currentGoalStreak)
        )
    }

    private fun aggregateSingleAppHourlySlots(
        targetPkg: String,
        startTime: Long,
        queryEnd: Long
    ): List<HourlyUsageSlot> {
        val manager = usageStatsManager ?: return (0..23).map { HourlyUsageSlot(it, 0L) }
        val hourlyDurations = LongArray(24) { 0L }
        val events = manager.queryEvents(maxOf(0L, startTime - (24 * 3600 * 1000L)), queryEnd) ?: return (0..23).map { HourlyUsageSlot(it, 0L) }

        val event = UsageEvents.Event()
        var sessionStart: Long? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName != targetPkg) continue
            val time = event.timeStamp

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (sessionStart == null) {
                        sessionStart = time
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED,
                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    sessionStart?.let { sStart ->
                        val effStart = maxOf(sStart, startTime)
                        val effEnd = minOf(time, queryEnd)
                        if (effEnd > effStart) {
                            var cursor = effStart
                            while (cursor < effEnd) {
                                val hourIndex = (((cursor - startTime) / (3600 * 1000L)).toInt()).coerceIn(0, 23)
                                val slotEnd = startTime + ((hourIndex + 1) * 3600 * 1000L)
                                val chunkEnd = minOf(effEnd, slotEnd)
                                val duration = chunkEnd - cursor
                                if (duration > 0) {
                                    hourlyDurations[hourIndex] += duration
                                }
                                cursor = chunkEnd
                            }
                        }
                    }
                    sessionStart = null
                }
            }
        }

        return (0..23).map { h ->
            HourlyUsageSlot(
                hour = h,
                totalDurationMillis = hourlyDurations[h],
                topAppPackage = targetPkg,
                topAppLabel = appIconHelper.getAppLabel(targetPkg)
            )
        }
    }

    private fun countGhostOpensForApp(targetPkg: String, startTime: Long, queryEnd: Long): Int {
        val manager = usageStatsManager ?: return 0
        val events = manager.queryEvents(maxOf(0L, startTime - (24 * 3600 * 1000L)), queryEnd) ?: return 0
        val event = UsageEvents.Event()
        var sessionStart: Long? = null
        var ghostCount = 0

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName != targetPkg) continue
            val time = event.timeStamp

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (sessionStart == null) sessionStart = time
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    sessionStart?.let { sStart ->
                        val duration = time - sStart
                        if (time in startTime..queryEnd && duration in 1..30_000L) {
                            ghostCount++
                        }
                    }
                    sessionStart = null
                }
            }
        }
        return ghostCount
    }

    private data class ParsedUsage(
        var totalForegroundMillis: Long = 0L,
        var launchCount: Int = 0,
        var lastTimeUsedMillis: Long = 0L,
        var sessionCount: Int = 0,
        var shortSessionsCount: Int = 0
    )

    private data class ClosedSessionMetric(
        val pkg: String,
        val start: Long,
        val end: Long,
        val duration: Long
    )

    private data class DayAnalyticsResult(
        val appUsageMap: Map<String, ParsedUsage>,
        val hourlySlots: List<HourlyUsageSlot>,
        val habitInsights: HabitInsights
    )

    private fun aggregateDayData(startTime: Long, queryEnd: Long): DayAnalyticsResult {
        val manager = usageStatsManager ?: return DayAnalyticsResult(emptyMap(), emptyList(), HabitInsights())
        val usageMap = mutableMapOf<String, ParsedUsage>()

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
        val fourAmStart = startTime + (4 * 3600 * 1000L)

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
        val ghostOpensCount = mutableMapOf<String, Int>()
        val recordedSessions = mutableListOf<ClosedSessionMetric>()

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
                    if (duration < 120_000L) {
                        parsed.shortSessionsCount++
                    }
                    if (duration <= 30_000L) {
                        ghostOpensCount[pkg] = (ghostOpensCount[pkg] ?: 0) + 1
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

                    recordedSessions.add(ClosedSessionMetric(pkg, effectiveStart, effectiveEnd, duration))
                    distributeSessionToHourlySlots(pkg, effectiveStart, effectiveEnd)
                }
            }
        }

        var lastUnlockForPhantom: Long? = null
        var appsLaunchedDuringUnlock = 0
        var phantomUnlocksCount = 0
        var totalQuickChecksCount = 0

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            val className = event.className ?: "default_activity"
            val time = event.timeStamp

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    appsLaunchedDuringUnlock++
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
                        lastUnlockForPhantom = time
                        appsLaunchedDuringUnlock = 0
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
                        if (lastUnlockForPhantom != null) {
                            val unlockDuration = time - lastUnlockForPhantom!!
                            if (unlockDuration in 1..10_000L && appsLaunchedDuringUnlock == 0) {
                                phantomUnlocksCount++
                            }
                            if (unlockDuration in 1..15_000L) {
                                totalQuickChecksCount++
                            }
                            lastUnlockForPhantom = null
                        }
                    }
                    val activePkgs = sessionStartTimes.keys.toList()
                    for (activePkg in activePkgs) {
                        closePackageSession(activePkg, time)
                    }
                }
            }
        }

        val remainingPkgs = sessionStartTimes.keys.toList()
        for (pkg in remainingPkgs) {
            closePackageSession(pkg, queryEnd)
        }

        val hourlySlots = (0..23).map { h ->
            val slotTotal = hourlyDurations[h]
            val breakdownMap = hourlyAppBreakdown[h]
            val topSlotApp = breakdownMap.maxByOrNull { it.value }
            val breakdownList = breakdownMap.map { (p, t) ->
                AppUsageInfo(
                    packageName = p,
                    appLabel = appIconHelper.getAppLabel(p),
                    totalTimeForegroundMillis = t,
                    category = appIconHelper.getAppCategory(p)
                )
            }.sortedByDescending { it.totalTimeForegroundMillis }

            HourlyUsageSlot(
                hour = h,
                totalDurationMillis = slotTotal,
                topAppPackage = topSlotApp?.key,
                topAppLabel = topSlotApp?.key?.let { appIconHelper.getAppLabel(it) },
                appBreakdown = breakdownList
            )
        }

        val totalForegroundSum = usageMap.values.sumOf { it.totalForegroundMillis }
        val totalLaunches = usageMap.values.sumOf { it.launchCount }
        val avgSessionDuration = if (totalLaunches > 0) totalForegroundSum / totalLaunches else 0L

        val shortSessions = usageMap.values.sumOf { it.shortSessionsCount }
        val totalSessions = usageMap.values.sumOf { it.sessionCount }
        val fragmentationScore = if (totalSessions > 0) {
            ((shortSessions.toDouble() / totalSessions.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        } else 0

        val categoryBreakdown = mutableMapOf<AppCategory, Long>()
        for ((pkg, usage) in usageMap) {
            val cat = appIconHelper.getAppCategory(pkg)
            categoryBreakdown[cat] = (categoryBreakdown[cat] ?: 0L) + usage.totalForegroundMillis
        }

        val productiveMillis = categoryBreakdown[AppCategory.PRODUCTIVITY] ?: 0L
        val communicationMillis = categoryBreakdown[AppCategory.COMMUNICATION] ?: 0L
        val utilitiesMillis = categoryBreakdown[AppCategory.UTILITIES] ?: 0L
        val distractingMillis = (categoryBreakdown[AppCategory.GAMES] ?: 0L) +
                (categoryBreakdown[AppCategory.SOCIAL] ?: 0L) +
                (categoryBreakdown[AppCategory.ENTERTAINMENT] ?: 0L)

        val positiveTime = productiveMillis + (communicationMillis / 2) + utilitiesMillis
        val productivityScore = if (totalForegroundSum > 0) {
            val ratio = (positiveTime.toDouble() - (distractingMillis * 0.3)) / totalForegroundSum.toDouble()
            (ratio * 100.0).toInt().coerceIn(0, 100)
        } else 0

        var bedtimeUsage = 0L
        lastLockTimestamp?.let { lockTime ->
            val bedtimeWindowStart = lockTime - (3600 * 1000L)
            for (sess in recordedSessions) {
                if (sess.end >= bedtimeWindowStart && sess.start <= lockTime) {
                    val overlapStart = maxOf(sess.start, bedtimeWindowStart)
                    val overlapEnd = minOf(sess.end, lockTime)
                    if (overlapEnd > overlapStart) {
                        bedtimeUsage += (overlapEnd - overlapStart)
                    }
                }
            }
        }

        val longestSession = if (maxSessionDuration > 0 && maxSessionPkg != null) {
            LongestSession(
                packageName = maxSessionPkg!!,
                appLabel = appIconHelper.getAppLabel(maxSessionPkg!!),
                durationMillis = maxSessionDuration,
                startEpochMillis = maxSessionStart,
                endEpochMillis = maxSessionEnd
            )
        } else null

        var peakHourIndex = 0
        var peakCount = 0
        for (h in 0..23) {
            if (hourlyUnlocks[h] > peakCount) {
                peakCount = hourlyUnlocks[h]
                peakHourIndex = h
            }
        }

        val totalGhostOpens = ghostOpensCount.values.sum()
        val topGhostEntry = ghostOpensCount.maxByOrNull { it.value }
        val ghostInsight = if (totalGhostOpens > 0) {
            GhostOpensInsight(
                totalGhostOpens = totalGhostOpens,
                topGhostAppLabel = topGhostEntry?.key?.let { appIconHelper.getAppLabel(it) },
                topGhostAppOpens = topGhostEntry?.value ?: 0
            )
        } else null

        var morningDoomscrollMillis = 0L
        var topMorningAppPkg: String? = null
        var maxMorningAppTime = 0L

        firstUnlockTimestamp?.let { wakeTime ->
            val morningWindowEnd = wakeTime + (45 * 60 * 1000L)
            val morningAppTimes = mutableMapOf<String, Long>()
            for (sess in recordedSessions) {
                if (sess.start <= morningWindowEnd && sess.end >= wakeTime) {
                    val cat = appIconHelper.getAppCategory(sess.pkg)
                    if (cat == AppCategory.SOCIAL || cat == AppCategory.GAMES || cat == AppCategory.ENTERTAINMENT) {
                        val overlapStart = maxOf(sess.start, wakeTime)
                        val overlapEnd = minOf(sess.end, morningWindowEnd)
                        if (overlapEnd > overlapStart) {
                            val duration = overlapEnd - overlapStart
                            morningDoomscrollMillis += duration
                            morningAppTimes[sess.pkg] = (morningAppTimes[sess.pkg] ?: 0L) + duration
                        }
                    }
                }
            }
            val topMorning = morningAppTimes.maxByOrNull { it.value }
            topMorningAppPkg = topMorning?.key
            maxMorningAppTime = topMorning?.value ?: 0L
        }

        val morningDoomscrollInsight = if (morningDoomscrollMillis > 0) {
            MorningDoomscroll(
                durationMillis = morningDoomscrollMillis,
                topAppLabel = topMorningAppPkg?.let { appIconHelper.getAppLabel(it) }
            )
        } else null

        val wakingPercentage = if (totalForegroundSum > 0) {
            ((totalForegroundSum.toDouble() / (16.0 * 3600.0 * 1000.0)) * 100.0).coerceIn(0.0, 100.0)
        } else 0.0

        val annualDays = if (totalForegroundSum > 0) {
            val dailyHours = totalForegroundSum.toDouble() / (3600.0 * 1000.0)
            ((dailyHours * 365.0) / 24.0).toInt()
        } else 0

        val wakingLifeImpact = if (totalForegroundSum > 0) {
            WakingLifeImpact(
                wakingPercentage = wakingPercentage,
                annualProjectedDays = annualDays
            )
        } else null

        val yearsLostBy75 = if (totalForegroundSum > 0) {
            val dailyProportion = totalForegroundSum.toDouble() / (24.0 * 3600.0 * 1000.0)
            dailyProportion * 50.0
        } else 0.0

        val lifeClock = if (totalForegroundSum > 0) {
            LifeClockProjection(
                dailyAverageMillis = totalForegroundSum,
                yearsLostBy75 = yearsLostBy75,
                consciousPercentage = wakingPercentage
            )
        } else null

        val baselineMillis = (2.5 * 3600 * 1000).toLong()
        val totalTime = totalForegroundSum
        val debtMillis = (totalTime - baselineMillis).coerceAtLeast(0L)
        val fastMinutes = ((debtMillis.toDouble() / 3_600_000.0) * 30.0).toInt().coerceIn(0, 240)
        val dopamineDebt = DopamineDebt(
            weeklyActualMillis = totalTime,
            weeklyBaselineMillis = baselineMillis,
            debtMillis = debtMillis,
            recommendedFastMinutes = fastMinutes
        )

        val phantomInsight = if (phantomUnlocksCount > 0 || totalQuickChecksCount > 0) {
            PhantomUnlocks(
                count = phantomUnlocksCount,
                totalQuickChecks = totalQuickChecksCount
            )
        } else null

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
            hourlyUnlocks = hourlyUnlocks.toList(),
            ghostOpens = ghostInsight,
            morningDoomscroll = morningDoomscrollInsight,
            wakingLifeImpact = wakingLifeImpact,
            lifeClock = lifeClock,
            dopamineDebt = dopamineDebt,
            phantomUnlocks = phantomInsight
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

    private fun AppUsageEntity.toDomain(
        iconHelper: AppIconHelper,
        override: AppOverrideEntity? = null
    ): AppUsageInfo {
        val resolvedCategory = when {
            this.isRemoved -> AppCategory.REMOVED
            override?.customCategory != null -> try {
                AppCategory.valueOf(override.customCategory)
            } catch (_: Exception) {
                iconHelper.getAppCategory(this.packageName)
            }
            else -> try {
                AppCategory.valueOf(this.category)
            } catch (_: Exception) {
                iconHelper.getAppCategory(this.packageName)
            }
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
            category = resolvedCategory,
            dailyLimitMinutes = override?.dailyLimitMinutes,
            isDistraction = override?.isDistraction ?: false
        )
    }
}
