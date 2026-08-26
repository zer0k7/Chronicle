package io.chronicle.usagestats.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppLimitEntity(
    @PrimaryKey
    val packageName: String,
    val appLabel: String = "",
    val dailyLimitMinutes: Int,
    val isEnabled: Boolean = true,
    val bypassMode: String = "NO_BYPASS",
    val temporaryUnlockUntilMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
