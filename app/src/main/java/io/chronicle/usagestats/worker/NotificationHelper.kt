package io.chronicle.usagestats.worker

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import io.chronicle.usagestats.MainActivity
import io.chronicle.usagestats.R
import io.chronicle.usagestats.core.util.Constants
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.core.util.PermissionHelper

object NotificationHelper {

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Daily Summary Channel
            val dailyName = context.getString(R.string.notification_channel_daily)
            val dailyDesc = context.getString(R.string.notification_channel_daily_desc)
            val dailyChannel = NotificationChannel(Constants.NOTIFICATION_CHANNEL_DAILY_ID, dailyName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = dailyDesc
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(dailyChannel)

            // 2. Reality Check & Midday Channel
            val realityName = context.getString(R.string.notification_channel_reality)
            val realityDesc = context.getString(R.string.notification_channel_reality_desc)
            val realityChannel = NotificationChannel(Constants.NOTIFICATION_CHANNEL_REALITY_ID, realityName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = realityDesc
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(realityChannel)

            // 3. Distraction Alerts Channel
            val distractionName = context.getString(R.string.notification_channel_distraction)
            val distractionDesc = context.getString(R.string.notification_channel_distraction_desc)
            val distractionChannel = NotificationChannel(Constants.NOTIFICATION_CHANNEL_DISTRACTION_ID, distractionName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = distractionDesc
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(distractionChannel)
        }
    }

    fun showDailySummaryNotification(
        context: Context,
        summary: io.chronicle.usagestats.domain.model.DailyUsageSummary,
        badgeEnabled: Boolean = true,
        realityCheckEnabled: Boolean = true,
        weekendNotificationsMuted: Boolean = false
    ) {
        if (!PermissionHelper.hasNotificationPermission(context)) {
            return
        }

        // Check weekend quiet mode (Saturday / Sunday in IST)
        if (weekendNotificationsMuted) {
            val dayOfWeek = DateTimeUtils.nowInIst().dayOfWeek
            if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
                return
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val totalDurationMillis = summary.totalScreenTimeMillis
        val habitInsights = summary.habitInsights
        val categories = habitInsights?.categoryBreakdown ?: emptyMap()

        val gamingDuration = categories[io.chronicle.usagestats.domain.model.AppCategory.GAMES] ?: 0L
        val socialDuration = categories[io.chronicle.usagestats.domain.model.AppCategory.SOCIAL] ?: 0L
        val productivityScore = habitInsights?.productivityScore ?: 0
        val bedtimeUsage = habitInsights?.bedtimeUsageMillis ?: 0L
        val topApp = summary.topAppLabel ?: context.getString(R.string.category_other)
        val durationFormatted = DateTimeUtils.formatDuration(totalDurationMillis)

        val (title, body) = when {
            // Reality check alerts (only if enabled)
            realityCheckEnabled && gamingDuration >= 90 * 60 * 1000L -> {
                val gameDurationStr = DateTimeUtils.formatDuration(gamingDuration)
                Pair(
                    context.getString(R.string.notification_gaming_title, gameDurationStr),
                    context.getString(R.string.notification_gaming_body, gameDurationStr, topApp)
                )
            }
            realityCheckEnabled && socialDuration >= 90 * 60 * 1000L -> {
                val socialDurationStr = DateTimeUtils.formatDuration(socialDuration)
                Pair(
                    context.getString(R.string.notification_social_title, socialDurationStr),
                    context.getString(R.string.notification_social_body, socialDurationStr)
                )
            }
            realityCheckEnabled && totalDurationMillis >= 270 * 60 * 1000L && productivityScore < 30 -> {
                Pair(
                    context.getString(R.string.notification_productivity_title, productivityScore),
                    context.getString(R.string.notification_productivity_body, productivityScore, durationFormatted)
                )
            }
            realityCheckEnabled && productivityScore >= 65 && totalDurationMillis >= 60 * 60 * 1000L -> {
                Pair(
                    context.getString(R.string.notification_discipline_title, productivityScore),
                    context.getString(R.string.notification_discipline_body, durationFormatted)
                )
            }
            realityCheckEnabled && bedtimeUsage >= 45 * 60 * 1000L -> {
                Pair(
                    context.getString(R.string.notification_sleep_title),
                    context.getString(R.string.notification_sleep_body)
                )
            }
            // Standard Balanced Overview
            else -> {
                Pair(
                    context.getString(R.string.notification_daily_title, durationFormatted),
                    context.getString(R.string.notification_daily_body, topApp, summary.appCount)
                )
            }
        }

        val hoursCount = (totalDurationMillis / (1000 * 60 * 60)).toInt()

        val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_DAILY_ID)
            .setSmallIcon(R.drawable.ic_chronicle_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (badgeEnabled && hoursCount > 0) {
            builder.setNumber(hoursCount)
        }

        notificationManager.notify(Constants.NOTIFICATION_DAILY_ID, builder.build())
    }

    fun showMiddayCheckNotification(
        context: Context,
        totalScreenTimeMillis: Long,
        dailyGoalMinutes: Int,
        topAppLabel: String?
    ) {
        if (!PermissionHelper.hasNotificationPermission(context)) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            Constants.NOTIFICATION_MIDDAY_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val durationFormatted = DateTimeUtils.formatDuration(totalScreenTimeMillis)
        val goalMillis = dailyGoalMinutes * 60 * 1000L
        val percentage = if (goalMillis > 0) {
            ((totalScreenTimeMillis.toDouble() / goalMillis.toDouble()) * 100.0).toInt().coerceIn(0, 200)
        } else 0

        val topApp = topAppLabel ?: context.getString(R.string.category_other)
        val title = context.getString(R.string.notification_midday_title, durationFormatted)
        val body = context.getString(R.string.notification_midday_body, topApp, percentage)

        val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_REALITY_ID)
            .setSmallIcon(R.drawable.ic_chronicle_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(Constants.NOTIFICATION_MIDDAY_ID, builder.build())
    }

    fun showDistractionSurgeNotification(
        context: Context,
        pingsInLastHour: Int,
        topAppLabel: String
    ) {
        if (!PermissionHelper.hasNotificationPermission(context)) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            Constants.NOTIFICATION_SURGE_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.notification_surge_title)
        val body = context.getString(R.string.notification_surge_body, pingsInLastHour, topAppLabel)

        val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_DISTRACTION_ID)
            .setSmallIcon(R.drawable.ic_chronicle_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(Constants.NOTIFICATION_SURGE_ID, builder.build())
    }

    fun showBudgetThresholdNotification(
        context: Context,
        percentage: Int,
        totalDurationMillis: Long,
        goalMinutes: Int
    ) {
        if (!PermissionHelper.hasNotificationPermission(context)) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            Constants.NOTIFICATION_BUDGET_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val durationFormatted = DateTimeUtils.formatDuration(totalDurationMillis)
        val goalFormatted = DateTimeUtils.formatDuration(goalMinutes * 60 * 1000L)

        val (title, body) = if (percentage >= 100) {
            Pair(
                context.getString(R.string.notification_budget_100_title),
                context.getString(R.string.notification_budget_100_body, goalFormatted)
            )
        } else {
            Pair(
                context.getString(R.string.notification_budget_80_title),
                context.getString(R.string.notification_budget_80_body, durationFormatted, goalFormatted)
            )
        }

        val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_REALITY_ID)
            .setSmallIcon(R.drawable.ic_chronicle_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(Constants.NOTIFICATION_BUDGET_ID, builder.build())
    }

    fun scheduleDailyNotification(context: Context, hour: Int, minute: Int) {
        scheduleAlarm(
            context = context,
            action = Constants.ACTION_DAILY_NOTIFICATION,
            requestCode = Constants.NOTIFICATION_DAILY_ID,
            hour = hour,
            minute = minute
        )
    }

    fun scheduleMiddayNotification(context: Context) {
        scheduleAlarm(
            context = context,
            action = Constants.ACTION_MIDDAY_NOTIFICATION,
            requestCode = Constants.NOTIFICATION_MIDDAY_ID,
            hour = Constants.DEFAULT_MIDDAY_NOTIFICATION_HOUR,
            minute = Constants.DEFAULT_MIDDAY_NOTIFICATION_MINUTE
        )
    }

    private fun scheduleAlarm(
        context: Context,
        action: String,
        requestCode: Int,
        hour: Int,
        minute: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, DailyNotificationReceiver::class.java).apply {
            this.action = action
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        var scheduledTime = DateTimeUtils.nowInIst()
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)

        if (scheduledTime.isBefore(DateTimeUtils.nowInIst())) {
            scheduledTime = scheduledTime.plusDays(1)
        }

        val triggerAtMillis = scheduledTime.toInstant().toEpochMilli()

        try {
            if (PermissionHelper.canScheduleExactAlarms(context)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                // Graceful fallback when exact alarm permission is absent
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            }
        } catch (_: SecurityException) {
            // Absolute safety fallback for strict OEM restrictions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    fun cancelDailyNotification(context: Context) {
        cancelAlarm(context, Constants.ACTION_DAILY_NOTIFICATION, Constants.NOTIFICATION_DAILY_ID)
    }

    fun cancelMiddayNotification(context: Context) {
        cancelAlarm(context, Constants.ACTION_MIDDAY_NOTIFICATION, Constants.NOTIFICATION_MIDDAY_ID)
    }

    private fun cancelAlarm(context: Context, action: String, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyNotificationReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
