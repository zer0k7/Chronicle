package io.chronicle.usagestats.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_data_summaries")
data class DailyDataUsageEntity(
    @PrimaryKey
    val dateStartEpochMillis: Long,
    val totalWifiRxBytes: Long,
    val totalWifiTxBytes: Long,
    val totalWifiBytes: Long,
    val totalMobileRxBytes: Long,
    val totalMobileTxBytes: Long,
    val totalMobileBytes: Long,
    val totalHotspotBytes: Long,
    val grandTotalBytes: Long,
    val lastUpdatedEpochMillis: Long = System.currentTimeMillis()
)
