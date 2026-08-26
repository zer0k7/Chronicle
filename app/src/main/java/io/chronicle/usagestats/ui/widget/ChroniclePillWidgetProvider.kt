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
import io.chronicle.usagestats.core.util.DataSizeUtils
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.data.local.preferences.UserPreferencesRepository
import io.chronicle.usagestats.domain.model.DataFilter
import io.chronicle.usagestats.domain.model.DataPeriod
import io.chronicle.usagestats.domain.repository.UsageRepository
import io.chronicle.usagestats.domain.usecase.GetDataUsageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChroniclePillWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun usageRepository(): UsageRepository
        fun getDataUsageUseCase(): GetDataUsageUseCase
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
                val getDataUsageUseCase = entryPoint.getDataUsageUseCase()
                val prefsRepo = entryPoint.userPreferencesRepository()

                val todaySummary = usageRepo.getTodaySummary()
                val screenTimeStr = DateTimeUtils.formatDuration(todaySummary.totalScreenTimeMillis)

                val settings = prefsRepo.userSettingsFlow.first()
                val dataSummary = getDataUsageUseCase(
                    period = DataPeriod.DAY,
                    referenceDate = System.currentTimeMillis(),
                    filter = DataFilter(),
                    billingCycleDay = settings.billingCycleStartDay
                ).first()
                val dataStr = DataSizeUtils.formatBytes(dataSummary.grandTotalBytes)

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
                    val views = RemoteViews(context.packageName, R.layout.widget_chronicle_pill).apply {
                        setTextViewText(R.id.tv_widget_pill_screen, screenTimeStr)
                        setTextViewText(R.id.tv_widget_pill_data, dataStr)
                        setOnClickPendingIntent(R.id.widget_pill_root, pendingIntent)
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
            val componentName = ComponentName(context, ChroniclePillWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, ChroniclePillWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
