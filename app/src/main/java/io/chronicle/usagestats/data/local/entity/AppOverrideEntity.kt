package io.chronicle.usagestats.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_custom_overrides")
data class AppOverrideEntity(
    @PrimaryKey
    val packageName: String,
    val customCategory: String? = null,
    val isDistraction: Boolean = false,
    val dailyLimitMinutes: Int? = null,
    val isWifiPreferred: Boolean = false,
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
)
