package io.chronicle.usagestats

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import io.chronicle.usagestats.core.util.Constants
import io.chronicle.usagestats.worker.DailyUsageSyncWorker
import io.chronicle.usagestats.worker.NotificationHelper
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class ChronicleApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        schedulePeriodicUsageSync()
    }

    private fun schedulePeriodicUsageSync() {
        val syncRequest = PeriodicWorkRequestBuilder<DailyUsageSyncWorker>(
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            Constants.WORKER_SYNC_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
