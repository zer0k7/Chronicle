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
import io.chronicle.usagestats.domain.repository.UsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChronicleTimelineWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun usageRepository(): UsageRepository
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

                val todaySummary = usageRepo.getTodaySummary()
                val totalDuration = todaySummary.totalScreenTimeMillis
                val durationStr = DateTimeUtils.formatDuration(totalDuration)
                val appsCount = todaySummary.appCount
                val topAppLabel = todaySummary.topAppLabel ?: "None"
                val topAppDuration = todaySummary.apps.firstOrNull()?.totalTimeForegroundMillis ?: 0L
                val topAppTimeStr = if (topAppDuration > 0) DateTimeUtils.formatDuration(topAppDuration) else "0m"

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
                    val views = RemoteViews(context.packageName, R.layout.widget_chronicle_timeline).apply {
                        setTextViewText(R.id.tv_widget_timeline_duration, durationStr)
                        setTextViewText(R.id.tv_widget_timeline_apps, "$appsCount active apps")
                        setTextViewText(R.id.tv_widget_timeline_top_app, topAppLabel)
                        setTextViewText(R.id.tv_widget_timeline_top_time, topAppTimeStr)
                        setOnClickPendingIntent(R.id.widget_timeline_root, pendingIntent)
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
            val componentName = ComponentName(context, ChronicleTimelineWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, ChronicleTimelineWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
