package io.chronicle.usagestats.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    @PrimaryKey
    val dateStartEpochMillis: Long,
    val totalScreenTimeMillis: Long,
    val topPackageName: String?,
    val topAppLabel: String?,
    val activeAppsCount: Int,
    val lastUpdatedEpochMillis: Long = System.currentTimeMillis()
)
