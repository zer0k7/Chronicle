package io.chronicle.usagestats.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_data_usage",
    indices = [
        Index(value = ["dateStartEpochMillis", "packageName"], unique = true),
        Index(value = ["dateStartEpochMillis"])
    ]
)
data class AppDataUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateStartEpochMillis: Long,
    val packageName: String,
    val appLabel: String,
    val wifiRxBytes: Long = 0L,
    val wifiTxBytes: Long = 0L,
    val mobileRxBytes: Long = 0L,
    val mobileTxBytes: Long = 0L,
    val isRemoved: Boolean = false,
    val isHotspot: Boolean = false
)
