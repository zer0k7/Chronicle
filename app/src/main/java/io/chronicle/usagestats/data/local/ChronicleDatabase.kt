package io.chronicle.usagestats.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import io.chronicle.usagestats.core.util.Constants
import io.chronicle.usagestats.data.local.dao.UsageDao
import io.chronicle.usagestats.data.local.entity.AppUsageEntity
import io.chronicle.usagestats.data.local.entity.DailySummaryEntity

@Database(
    entities = [
        AppUsageEntity::class,
        DailySummaryEntity::class
    ],
    version = Constants.DATABASE_VERSION,
    exportSchema = false
)
abstract class ChronicleDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao
}
