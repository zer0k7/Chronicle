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
            val name = context.getString(R.string.notification_channel_daily)
            val descriptionText = context.getString(R.string.notification_channel_daily_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(Constants.NOTIFICATION_CHANNEL_DAILY_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
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

    fun scheduleDailyNotification(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, DailyNotificationReceiver::class.java).apply {
            action = Constants.ACTION_DAILY_NOTIFICATION
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.NOTIFICATION_DAILY_ID,
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
    }

    fun cancelDailyNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyNotificationReceiver::class.java).apply {
            action = Constants.ACTION_DAILY_NOTIFICATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.NOTIFICATION_DAILY_ID,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
