package io.chronicle.usagestats.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.chronicle.usagestats.data.local.entity.AppDataUsageEntity
import io.chronicle.usagestats.data.local.entity.DailyDataUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySummary(summary: DailyDataUsageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsages(usages: List<AppDataUsageEntity>)

    @Transaction
    suspend fun saveFullDailyDataUsage(
        summary: DailyDataUsageEntity,
        usages: List<AppDataUsageEntity>
    ) {
        insertDailySummary(summary)
        insertAppUsages(usages)
    }

    @Query("SELECT * FROM daily_data_summaries WHERE dateStartEpochMillis = :dateMillis")
    fun getDailySummary(dateMillis: Long): Flow<DailyDataUsageEntity?>

    @Query("SELECT * FROM app_data_usage WHERE dateStartEpochMillis = :dateMillis")
    fun getAppUsagesForDate(dateMillis: Long): Flow<List<AppDataUsageEntity>>

    @Query("SELECT * FROM daily_data_summaries WHERE dateStartEpochMillis >= :startMillis AND dateStartEpochMillis < :endMillis ORDER BY dateStartEpochMillis ASC")
    fun getDailySummariesForRange(startMillis: Long, endMillis: Long): Flow<List<DailyDataUsageEntity>>

    @Query("SELECT * FROM app_data_usage WHERE dateStartEpochMillis >= :startMillis AND dateStartEpochMillis < :endMillis")
    suspend fun getAppUsagesForRangeDirect(startMillis: Long, endMillis: Long): List<AppDataUsageEntity>

    @Query("DELETE FROM daily_data_summaries WHERE dateStartEpochMillis < :cutoffMillis")
    suspend fun deleteSummariesOlderThan(cutoffMillis: Long)

    @Query("DELETE FROM app_data_usage WHERE dateStartEpochMillis < :cutoffMillis")
    suspend fun deleteAppUsagesOlderThan(cutoffMillis: Long)
}
