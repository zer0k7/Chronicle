package io.chronicle.usagestats.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.chronicle.usagestats.core.util.PermissionHelper
import io.chronicle.usagestats.domain.usecase.SyncUsageDataUseCase
import kotlinx.coroutines.flow.first

@HiltWorker
class DailyUsageSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncUsageDataUseCase: SyncUsageDataUseCase,
    private val syncDataUsageUseCase: io.chronicle.usagestats.domain.usecase.SyncDataUsageUseCase,
    private val userPreferencesRepository: io.chronicle.usagestats.data.local.preferences.UserPreferencesRepository,
    private val usageRepository: io.chronicle.usagestats.domain.repository.UsageRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!PermissionHelper.hasUsageStatsPermission(context)) {
            return Result.success()
        }

        return try {
            syncUsageDataUseCase.syncToday()
            syncDataUsageUseCase.syncDate(System.currentTimeMillis())
            io.chronicle.usagestats.ui.widget.ChronicleWidgetUpdater.updateAll(context)
            io.chronicle.usagestats.service.ChronicleTileUpdater.updateAll(context)

            // Evaluate budget threshold warnings
            val settings = userPreferencesRepository.userSettingsFlow.first()
            if (settings.budgetAlertEnabled && settings.dailyGoalMinutes > 0) {
                    val totalScreenTime = usageRepository.getTodayTotalScreenTimeMillis()
                    val goalMillis = settings.dailyGoalMinutes * 60 * 1000L
                    if (totalScreenTime >= goalMillis) {
                        NotificationHelper.showBudgetThresholdNotification(
                            context = context,
                            percentage = 100,
                            totalDurationMillis = totalScreenTime,
                            goalMinutes = settings.dailyGoalMinutes
                        )
                    } else if (totalScreenTime >= (goalMillis * 0.8).toLong()) {
                        NotificationHelper.showBudgetThresholdNotification(
                            context = context,
                            percentage = 80,
                            totalDurationMillis = totalScreenTime,
                            goalMinutes = settings.dailyGoalMinutes
                        )
                    }
                }

                // Ensure alarms are armed
                if (settings.dailyNotificationEnabled) {
                    NotificationHelper.scheduleDailyNotification(
                        context,
                        settings.dailyNotificationHour,
                        settings.dailyNotificationMinute
                    )
                }
                if (settings.middayNotificationEnabled) {
                    NotificationHelper.scheduleMiddayNotification(context)
                }
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
