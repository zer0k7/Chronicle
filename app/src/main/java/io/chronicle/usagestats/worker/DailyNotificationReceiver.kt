package io.chronicle.usagestats.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.chronicle.usagestats.core.util.Constants
import io.chronicle.usagestats.data.local.preferences.UserPreferencesRepository
import io.chronicle.usagestats.domain.repository.UsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DailyNotificationReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun usageRepository(): UsageRepository
        fun userPreferencesRepository(): UserPreferencesRepository
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Constants.ACTION_DAILY_NOTIFICATION && action != Constants.ACTION_MIDDAY_NOTIFICATION) return

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReceiverEntryPoint::class.java
        )

        val usageRepo = entryPoint.usageRepository()
        val preferencesRepo = entryPoint.userPreferencesRepository()

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = preferencesRepo.userSettingsFlow.first()

                if (action == Constants.ACTION_DAILY_NOTIFICATION && settings.dailyNotificationEnabled) {
                    val summary = usageRepo.getTodaySummary()
                    NotificationHelper.showDailySummaryNotification(
                        context = context,
                        summary = summary,
                        badgeEnabled = settings.badgeEnabled,
                        realityCheckEnabled = settings.realityCheckEnabled,
                        weekendNotificationsMuted = settings.weekendNotificationsMuted
                    )

                    // Reschedule daily summary for next day
                    NotificationHelper.scheduleDailyNotification(
                        context = context,
                        hour = settings.dailyNotificationHour,
                        minute = settings.dailyNotificationMinute
                    )
                } else if (action == Constants.ACTION_MIDDAY_NOTIFICATION && settings.middayNotificationEnabled) {
                    val summary = usageRepo.getTodaySummary()
                    NotificationHelper.showMiddayCheckNotification(
                        context = context,
                        totalScreenTimeMillis = summary.totalScreenTimeMillis,
                        dailyGoalMinutes = settings.dailyGoalMinutes,
                        topAppLabel = summary.topAppLabel
                    )

                    // Reschedule midday notification for next day
                    NotificationHelper.scheduleMiddayNotification(context = context)
                }
            } catch (_: Exception) {
                // Silently handle background dispatch failure
            } finally {
                pendingResult.finish()
            }
        }
    }
}
