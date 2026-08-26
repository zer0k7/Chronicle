package io.chronicle.usagestats.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.chronicle.usagestats.core.util.PermissionHelper
import io.chronicle.usagestats.domain.usecase.SyncUsageDataUseCase

@HiltWorker
class DailyUsageSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncUsageDataUseCase: SyncUsageDataUseCase,
    private val syncDataUsageUseCase: io.chronicle.usagestats.domain.usecase.SyncDataUsageUseCase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!PermissionHelper.hasUsageStatsPermission(context)) {
            return Result.success()
        }

        return try {
            syncUsageDataUseCase.syncToday()
            syncDataUsageUseCase.syncDate(System.currentTimeMillis())
            io.chronicle.usagestats.ui.widget.ChronicleWidgetUpdater.updateAll(context)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
