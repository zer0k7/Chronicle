package io.chronicle.usagestats.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.chronicle.usagestats.core.util.AppIconHelper
import io.chronicle.usagestats.data.local.dao.NotificationDao
import io.chronicle.usagestats.data.local.entity.NotificationEventEntity
import io.chronicle.usagestats.data.local.preferences.UserPreferencesRepository
import io.chronicle.usagestats.worker.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationRadarService : NotificationListenerService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ServiceEntryPoint {
        fun notificationDao(): NotificationDao
        fun userPreferencesRepository(): UserPreferencesRepository
        fun appIconHelper(): AppIconHelper
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var lastSurgeAlertTimestamp = 0L

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return // Ignore Chronicle's own notifications

        val isOngoing = sbn.isOngoing || (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ServiceEntryPoint::class.java
        )
        val notificationDao = entryPoint.notificationDao()
        val preferencesRepo = entryPoint.userPreferencesRepository()
        val appIconHelper = entryPoint.appIconHelper()

        val postTime = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()
        val category = sbn.notification.category

        serviceScope.launch {
            try {
                val settings = preferencesRepo.userSettingsFlow.first()
                if (!settings.notificationRadarEnabled) return@launch

                val entity = NotificationEventEntity(
                    packageName = pkg,
                    timestamp = postTime,
                    category = category,
                    isOngoing = isOngoing
                )
                notificationDao.insert(entity)

                // Check for distraction surge if enabled
                if (!isOngoing && settings.distractionSurgeAlertEnabled) {
                    val now = System.currentTimeMillis()
                    // Throttle distraction alerts: at most once every 90 minutes
                    if (now - lastSurgeAlertTimestamp > 90 * 60 * 1000L) {
                        val oneHourAgo = now - 60 * 60 * 1000L
                        val recentCount = notificationDao.getDisruptiveCountBetween(oneHourAgo, now)

                        if (recentCount >= 30) {
                            lastSurgeAlertTimestamp = now
                            val topDisruptors = notificationDao.getTopDisruptorPackages(oneHourAgo, now, 1)
                            val topPkg = topDisruptors.firstOrNull()?.packageName
                            val topLabel = topPkg?.let { appIconHelper.getAppLabel(it) } ?: "Applications"

                            NotificationHelper.showDistractionSurgeNotification(
                                context = applicationContext,
                                pingsInLastHour = recentCount,
                                topAppLabel = topLabel
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Silently ignore background logging errors
            }
        }
    }
}
