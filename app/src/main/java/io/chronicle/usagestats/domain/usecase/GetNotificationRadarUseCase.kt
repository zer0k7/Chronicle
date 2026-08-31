package io.chronicle.usagestats.domain.usecase

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.chronicle.usagestats.core.util.AppIconHelper
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.core.util.PermissionHelper
import io.chronicle.usagestats.data.local.dao.NotificationDao
import io.chronicle.usagestats.domain.model.AppCategory
import io.chronicle.usagestats.domain.model.BaitAndSwitchEvent
import io.chronicle.usagestats.domain.model.DisruptorApp
import io.chronicle.usagestats.domain.model.FragmentationRisk
import io.chronicle.usagestats.domain.model.HourlyPingSlot
import io.chronicle.usagestats.domain.model.NotificationRadarData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class GetNotificationRadarUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationDao: NotificationDao,
    private val appIconHelper: AppIconHelper
) {

    operator fun invoke(dateEpochMillis: Long = System.currentTimeMillis()): Flow<NotificationRadarData> = flow {
        val hasPermission = PermissionHelper.hasNotificationListenerPermission(context)
        if (!hasPermission) {
            emit(
                NotificationRadarData(
                    totalPings = 0,
                    disruptivePings = 0,
                    fragmentationScore = 0,
                    fragmentationRisk = FragmentationRisk.LOW,
                    hourlySlots = (0..23).map { HourlyPingSlot(it, 0) },
                    topDisruptors = emptyList(),
                    baitAndSwitchEvents = emptyList(),
                    isListenerPermissionGranted = false
                )
            )
            return@flow
        }

        val startOfDay = DateTimeUtils.getStartOfDay(dateEpochMillis)
        val endOfDay = DateTimeUtils.getEndOfDay(dateEpochMillis)

        notificationDao.getEventsBetween(startOfDay, endOfDay).collect { events ->
            val totalPings = events.size
            val disruptiveEvents = events.filter { !it.isOngoing }
            val disruptivePings = disruptiveEvents.size

            // 1. Group by 24 hourly buckets (0..23 in IST)
            val eventsByHour = disruptiveEvents.groupBy { event ->
                DateTimeUtils.toZonedDateTime(event.timestamp).hour
            }

            val hourlySlots = (0..23).map { hour ->
                val hourEvents = eventsByHour[hour] ?: emptyList()
                val topPkg = hourEvents.groupBy { it.packageName }
                    .maxByOrNull { it.value.size }
                    ?.key

                HourlyPingSlot(
                    hour = hour,
                    pingCount = hourEvents.size,
                    topAppPackage = topPkg,
                    topAppLabel = topPkg?.let { appIconHelper.getAppLabel(it) }
                )
            }

            // 2. Calculate peak hour
            val peakSlot = hourlySlots.maxByOrNull { it.pingCount }
            val peakHour = if ((peakSlot?.pingCount ?: 0) > 0) peakSlot?.hour else null
            val peakHourCount = peakSlot?.pingCount ?: 0

            // 3. Calculate Top Disruptors
            val disruptorCounts = disruptiveEvents.groupBy { it.packageName }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
                .take(6)

            val topDisruptors = disruptorCounts.map { (pkg, count) ->
                val percentage = if (disruptivePings > 0) {
                    (count.toFloat() / disruptivePings.toFloat()) * 100f
                } else 0f

                DisruptorApp(
                    packageName = pkg,
                    appLabel = appIconHelper.getAppLabel(pkg),
                    pingCount = count,
                    percentage = percentage,
                    category = appIconHelper.getAppCategory(pkg)
                )
            }

            // 4. Calculate Attention Fragmentation Index (0..100)
            val activeHoursWithPings = hourlySlots.count { it.hour in 7..23 && it.pingCount > 0 }
            val volumeComponent = (disruptivePings / 80.0) * 60.0
            val consistencyComponent = (activeHoursWithPings / 16.0) * 40.0
            val rawScore = (volumeComponent + consistencyComponent).toInt().coerceIn(0, 100)

            val risk = when {
                rawScore >= 75 -> FragmentationRisk.HIGH
                rawScore >= 50 -> FragmentationRisk.ELEVATED
                rawScore >= 25 -> FragmentationRisk.MODERATE
                else -> FragmentationRisk.LOW
            }

            // 5. Bait-and-Switch Detection (Pings that triggered opening a doomscroll app)
            val baitAndSwitchEvents = detectBaitAndSwitchEvents(
                context = context,
                events = disruptiveEvents,
                startOfDay = startOfDay,
                endOfDay = endOfDay
            )

            emit(
                NotificationRadarData(
                    totalPings = totalPings,
                    disruptivePings = disruptivePings,
                    fragmentationScore = rawScore,
                    fragmentationRisk = risk,
                    hourlySlots = hourlySlots,
                    topDisruptors = topDisruptors,
                    baitAndSwitchEvents = baitAndSwitchEvents,
                    peakHour = peakHour,
                    peakHourCount = peakHourCount,
                    isListenerPermissionGranted = true
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    private fun detectBaitAndSwitchEvents(
        context: Context,
        events: List<io.chronicle.usagestats.data.local.entity.NotificationEventEntity>,
        startOfDay: Long,
        endOfDay: Long
    ): List<BaitAndSwitchEvent> {
        if (events.isEmpty() || !PermissionHelper.hasUsageStatsPermission(context)) {
            return emptyList()
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()

        val usageEvents = try {
            usageStatsManager.queryEvents(startOfDay, endOfDay)
        } catch (_: Exception) {
            return emptyList()
        }

        data class ActivityLaunch(val packageName: String, val timestamp: Long)
        val launches = mutableListOf<ActivityLaunch>()

        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                launches.add(ActivityLaunch(event.packageName, event.timeStamp))
            }
        }

        if (launches.isEmpty()) return emptyList()

        // Pair notification arrivals with launches occurring within 45 seconds
        val cascadePairs = mutableMapOf<Pair<String, String>, Int>()
        val distractCategories = setOf(AppCategory.SOCIAL, AppCategory.ENTERTAINMENT, AppCategory.GAMES)

        for (notif in events) {
            val nextLaunch = launches.firstOrNull { launch ->
                launch.timestamp in notif.timestamp..(notif.timestamp + 45_000L) &&
                        launch.packageName != notif.packageName
            }

            if (nextLaunch != null) {
                val category = appIconHelper.getAppCategory(nextLaunch.packageName)
                if (category in distractCategories) {
                    val key = Pair(notif.packageName, nextLaunch.packageName)
                    cascadePairs[key] = (cascadePairs[key] ?: 0) + 1
                }
            }
        }

        return cascadePairs.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { (pair, count) ->
                BaitAndSwitchEvent(
                    triggerAppPackage = pair.first,
                    triggerAppLabel = appIconHelper.getAppLabel(pair.first),
                    destinationAppPackage = pair.second,
                    destinationAppLabel = appIconHelper.getAppLabel(pair.second),
                    occurrenceCount = count
                )
            }
    }
}
