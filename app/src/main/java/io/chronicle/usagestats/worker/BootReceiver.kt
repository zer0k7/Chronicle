package io.chronicle.usagestats.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.chronicle.usagestats.data.local.dao.AppLimitDao
import io.chronicle.usagestats.data.local.preferences.UserPreferencesRepository
import io.chronicle.usagestats.service.AppLimitMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootEntryPoint {
        fun userPreferencesRepository(): UserPreferencesRepository
        fun appLimitDao(): AppLimitDao
    }

    override fun onReceive(context: Context, intent: Intent) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BootEntryPoint::class.java
        )
        val preferencesRepo = entryPoint.userPreferencesRepository()
        val appLimitDao = entryPoint.appLimitDao()

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = preferencesRepo.userSettingsFlow.first()
                if (settings.dailyNotificationEnabled) {
                    NotificationHelper.scheduleDailyNotification(
                        context = context,
                        hour = settings.dailyNotificationHour,
                        minute = settings.dailyNotificationMinute
                    )
                }

                val enabledLimitsCount = appLimitDao.getEnabledLimitCount()
                if (enabledLimitsCount > 0) {
                    AppLimitMonitorService.start(context)
                }
            } catch (_: Exception) {
                // Ignore reschedule failure
            } finally {
                pendingResult.finish()
            }
        }
    }
}
