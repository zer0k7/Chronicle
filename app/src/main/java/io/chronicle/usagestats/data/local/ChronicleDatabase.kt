package io.chronicle.usagestats.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import io.chronicle.usagestats.core.util.Constants
import io.chronicle.usagestats.data.local.dao.AppLimitDao
import io.chronicle.usagestats.data.local.dao.AppOverrideDao
import io.chronicle.usagestats.data.local.dao.NetworkUsageDao
import io.chronicle.usagestats.data.local.dao.NotificationDao
import io.chronicle.usagestats.data.local.dao.UsageDao
import io.chronicle.usagestats.data.local.entity.AppDataUsageEntity
import io.chronicle.usagestats.data.local.entity.AppLimitEntity
import io.chronicle.usagestats.data.local.entity.AppOverrideEntity
import io.chronicle.usagestats.data.local.entity.AppUsageEntity
import io.chronicle.usagestats.data.local.entity.DailyDataUsageEntity
import io.chronicle.usagestats.data.local.entity.DailySummaryEntity
import io.chronicle.usagestats.data.local.entity.NotificationEventEntity

@Database(
    entities = [
        AppUsageEntity::class,
        DailySummaryEntity::class,
        AppOverrideEntity::class,
        DailyDataUsageEntity::class,
        AppDataUsageEntity::class,
        AppLimitEntity::class,
        NotificationEventEntity::class
    ],
    version = Constants.DATABASE_VERSION,
    exportSchema = false
)
abstract class ChronicleDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao
    abstract fun appOverrideDao(): AppOverrideDao
    abstract fun networkUsageDao(): NetworkUsageDao
    abstract fun appLimitDao(): AppLimitDao
    abstract fun notificationDao(): NotificationDao
}
