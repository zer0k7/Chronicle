package io.chronicle.usagestats.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.chronicle.usagestats.MainActivity
import io.chronicle.usagestats.R
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.data.local.preferences.UserPreferencesRepository
import io.chronicle.usagestats.domain.repository.UsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChronicleCommandCenterWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun usageRepository(): UsageRepository
        fun userPreferencesRepository(): UserPreferencesRepository
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    WidgetEntryPoint::class.java
                )
                val usageRepo = entryPoint.usageRepository()
                val prefsRepo = entryPoint.userPreferencesRepository()

                val todaySummary = usageRepo.getTodaySummary()
                val settings = prefsRepo.userSettingsFlow.first()
                val goalMillis = settings.dailyGoalMinutes * 60 * 1000L

                val totalDuration = todaySummary.totalScreenTimeMillis
                val progress = if (goalMillis > 0) {
                    ((totalDuration.toFloat() / goalMillis.toFloat()) * 100).toInt().coerceIn(0, 100)
                } else 0

                val durationStr = DateTimeUtils.formatDuration(totalDuration)
                val goalFormatted = DateTimeUtils.formatDuration(goalMillis)
                val apps = todaySummary.apps.sortedByDescending { it.totalTimeForegroundMillis }

                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_chronicle_command_center).apply {
                        setTextViewText(R.id.tv_cmd_duration, durationStr)
                        setProgressBar(R.id.pb_cmd_goal, 100, progress, false)
                        setTextViewText(R.id.tv_cmd_goal_sub, "$progress% of $goalFormatted goal")

                        // App 1
                        val app1 = apps.getOrNull(0)
                        setTextViewText(R.id.tv_cmd_app1_name, app1?.appLabel ?: "None")
                        setTextViewText(R.id.tv_cmd_app1_time, app1?.let { DateTimeUtils.formatDuration(it.totalTimeForegroundMillis) } ?: "0m")

                        // App 2
                        val app2 = apps.getOrNull(1)
                        setTextViewText(R.id.tv_cmd_app2_name, app2?.appLabel ?: "None")
                        setTextViewText(R.id.tv_cmd_app2_time, app2?.let { DateTimeUtils.formatDuration(it.totalTimeForegroundMillis) } ?: "0m")

                        // App 3
                        val app3 = apps.getOrNull(2)
                        setTextViewText(R.id.tv_cmd_app3_name, app3?.appLabel ?: "None")
                        setTextViewText(R.id.tv_cmd_app3_time, app3?.let { DateTimeUtils.formatDuration(it.totalTimeForegroundMillis) } ?: "0m")

                        setOnClickPendingIntent(R.id.widget_command_root, pendingIntent)
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ChronicleCommandCenterWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, ChronicleCommandCenterWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
