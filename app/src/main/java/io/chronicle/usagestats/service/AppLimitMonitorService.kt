package io.chronicle.usagestats.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import io.chronicle.usagestats.R
import io.chronicle.usagestats.data.local.dao.AppLimitDao
import io.chronicle.usagestats.ui.limits.AppLimitBlockerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class AppLimitMonitorService : Service() {

    @Inject
    lateinit var appLimitDao: AppLimitDao

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var lastBlockedPackage: String? = null

    private lateinit var usageStatsManager: UsageStatsManager

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        createNotificationChannel()
        startForegroundService()
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.limit_monitor_notification_title))
            .setContentText(getString(R.string.limit_monitor_notification_text))
            .setSmallIcon(R.drawable.ic_chronicle_logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_limits_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.limit_monitor_notification_text)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startMonitoring() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val now = System.currentTimeMillis()
                    appLimitDao.clearExpiredUnlocks(now)

                    val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - 5000, now)
                    val foregroundApp = stats?.maxByOrNull { it.lastTimeUsed }?.packageName

                    if (foregroundApp != null && foregroundApp != packageName && !isLauncherApp(foregroundApp)) {
                        val enabledLimits = appLimitDao.getAllEnabledLimitsDirect()
                        val matchedLimit = enabledLimits.find { it.packageName == foregroundApp }
                        
                        if (matchedLimit != null) {
                            val todayStartIST = LocalDate.now(ZoneId.of("Asia/Kolkata"))
                                .atStartOfDay(ZoneId.of("Asia/Kolkata"))
                                .toInstant()
                                .toEpochMilli()

                            val dailyStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, todayStartIST, now)
                            var totalTimeInForeground = 0L
                            
                            if (dailyStats != null) {
                                for (stat in dailyStats) {
                                    if (stat.packageName == foregroundApp) {
                                        totalTimeInForeground += stat.totalTimeInForeground
                                    }
                                }
                            }
                            
                            val usedMinutes = (totalTimeInForeground / 60000).toInt()
                            val isUnlocked = matchedLimit.temporaryUnlockUntilMillis != null && matchedLimit.temporaryUnlockUntilMillis > now
                            
                            if (usedMinutes >= matchedLimit.dailyLimitMinutes && !isUnlocked) {
                                if (lastBlockedPackage != foregroundApp) {
                                    launchBlockerActivity(
                                        packageName = foregroundApp,
                                        appLabel = matchedLimit.appLabel,
                                        limitMinutes = matchedLimit.dailyLimitMinutes,
                                        usedMinutes = usedMinutes,
                                        bypassMode = matchedLimit.bypassMode
                                    )
                                    lastBlockedPackage = foregroundApp
                                }
                            } else {
                                if (lastBlockedPackage == foregroundApp) {
                                    lastBlockedPackage = null
                                }
                            }
                        } else {
                            if (lastBlockedPackage == foregroundApp) {
                                lastBlockedPackage = null
                            }
                        }
                    } else if (foregroundApp != null && isLauncherApp(foregroundApp)) {
                        lastBlockedPackage = null
                    }
                } catch (e: Exception) {
                    // Ignore
                }
                delay(LIMIT_POLL_INTERVAL_MS)
            }
        }
    }

    private fun isLauncherApp(packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    private fun launchBlockerActivity(
        packageName: String,
        appLabel: String,
        limitMinutes: Int,
        usedMinutes: Int,
        bypassMode: String
    ) {
        val intent = Intent(this, AppLimitBlockerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_BLOCKED_PACKAGE, packageName)
            putExtra(EXTRA_BLOCKED_LABEL, appLabel)
            putExtra(EXTRA_LIMIT_MINUTES, limitMinutes)
            putExtra(EXTRA_USED_MINUTES, usedMinutes)
            putExtra(EXTRA_BYPASS_MODE, bypassMode)
        }
        startActivity(intent)
    }

    companion object {
        const val CHANNEL_ID = "chronicle_limit_monitor"
        const val NOTIFICATION_ID = 3001
        const val ACTION_STOP = "action_stop_limit_service"
        const val LIMIT_POLL_INTERVAL_MS = 2000L

        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
        const val EXTRA_BLOCKED_LABEL = "extra_blocked_label"
        const val EXTRA_LIMIT_MINUTES = "extra_limit_minutes"
        const val EXTRA_USED_MINUTES = "extra_used_minutes"
        const val EXTRA_BYPASS_MODE = "extra_bypass_mode"

        fun start(context: Context) {
            val intent = Intent(context, AppLimitMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AppLimitMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
