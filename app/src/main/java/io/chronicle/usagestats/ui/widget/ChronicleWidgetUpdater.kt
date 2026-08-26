package io.chronicle.usagestats.ui.widget

import android.content.Context

object ChronicleWidgetUpdater {
    fun updateAll(context: Context) {
        ChronicleGoalWidgetProvider.updateAllWidgets(context)
        ChronicleTimelineWidgetProvider.updateAllWidgets(context)
        ChronicleCommandCenterWidgetProvider.updateAllWidgets(context)
        ChronicleDataWidgetProvider.updateAllWidgets(context)
        ChronicleZenWidgetProvider.updateAllWidgets(context)
        ChronicleHabitsWidgetProvider.updateAllWidgets(context)
        ChroniclePillWidgetProvider.updateAllWidgets(context)
    }
}
