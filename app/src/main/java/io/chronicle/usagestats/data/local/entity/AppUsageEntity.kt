package io.chronicle.usagestats.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_usage_records",
    indices = [
        Index(value = ["dateStartEpochMillis", "packageName"], unique = true),
        Index(value = ["dateStartEpochMillis"]),
        Index(value = ["packageName"]),
        Index(value = ["isRemoved"])
    ]
)
data class AppUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateStartEpochMillis: Long,
    val packageName: String,
    val appLabel: String,
    val foregroundTimeMillis: Long,
    val launchCount: Int = 0,
    val lastTimeUsedMillis: Long = 0L,
    val isRemoved: Boolean = false,
    val category: String = "OTHER"
)
