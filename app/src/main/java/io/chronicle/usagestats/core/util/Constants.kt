package io.chronicle.usagestats.core.util

object Constants {
    const val NOTIFICATION_CHANNEL_DAILY_ID = "chronicle_daily_summary"
    const val NOTIFICATION_CHANNEL_REALITY_ID = "chronicle_reality_check"
    const val NOTIFICATION_CHANNEL_DISTRACTION_ID = "chronicle_distraction_alerts"

    const val NOTIFICATION_DAILY_ID = 1001
    const val NOTIFICATION_MIDDAY_ID = 1002
    const val NOTIFICATION_SURGE_ID = 1003
    const val NOTIFICATION_BUDGET_ID = 1004

    const val ACTION_DAILY_NOTIFICATION = "io.chronicle.usagestats.ACTION_DAILY_NOTIFICATION"
    const val ACTION_MIDDAY_NOTIFICATION = "io.chronicle.usagestats.ACTION_MIDDAY_NOTIFICATION"
    const val WORKER_SYNC_TAG = "chronicle_usage_sync_worker"

    const val DATASTORE_NAME = "chronicle_preferences"

    const val DEFAULT_NOTIFICATION_HOUR = 21 // 9:00 PM IST
    const val DEFAULT_NOTIFICATION_MINUTE = 0
    const val DEFAULT_MIDDAY_NOTIFICATION_HOUR = 14 // 2:00 PM IST
    const val DEFAULT_MIDDAY_NOTIFICATION_MINUTE = 0

    const val DATABASE_NAME = "chronicle_db"
    const val DATABASE_VERSION = 5
}
