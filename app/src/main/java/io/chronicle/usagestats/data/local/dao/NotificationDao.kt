package io.chronicle.usagestats.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.chronicle.usagestats.data.local.entity.NotificationEventEntity
import kotlinx.coroutines.flow.Flow

data class PackageNotificationCount(
    val packageName: String,
    val count: Int
)

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: NotificationEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<NotificationEventEntity>)

    @Query("SELECT * FROM notification_events WHERE timestamp >= :startMillis AND timestamp < :endMillis ORDER BY timestamp ASC")
    fun getEventsBetween(startMillis: Long, endMillis: Long): Flow<List<NotificationEventEntity>>

    @Query("SELECT * FROM notification_events WHERE timestamp >= :startMillis AND timestamp < :endMillis ORDER BY timestamp ASC")
    suspend fun getEventsBetweenDirect(startMillis: Long, endMillis: Long): List<NotificationEventEntity>

    @Query("SELECT COUNT(*) FROM notification_events WHERE timestamp >= :startMillis AND timestamp < :endMillis AND is_ongoing = 0")
    suspend fun getDisruptiveCountBetween(startMillis: Long, endMillis: Long): Int

    @Query("SELECT package_name AS packageName, COUNT(*) AS count FROM notification_events WHERE timestamp >= :startMillis AND timestamp < :endMillis AND is_ongoing = 0 GROUP BY package_name ORDER BY count DESC LIMIT :limit")
    suspend fun getTopDisruptorPackages(startMillis: Long, endMillis: Long, limit: Int = 10): List<PackageNotificationCount>

    @Query("DELETE FROM notification_events WHERE timestamp < :cutoffMillis")
    suspend fun deleteEventsOlderThan(cutoffMillis: Long)
}
