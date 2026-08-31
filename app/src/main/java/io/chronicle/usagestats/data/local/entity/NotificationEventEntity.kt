package io.chronicle.usagestats.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_events",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["package_name"]),
        Index(value = ["timestamp", "package_name"])
    ]
)
data class NotificationEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "category")
    val category: String? = null,
    @ColumnInfo(name = "is_ongoing")
    val isOngoing: Boolean = false
)
