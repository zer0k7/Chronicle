package io.chronicle.usagestats.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.chronicle.usagestats.data.local.entity.AppUsageEntity
import io.chronicle.usagestats.data.local.entity.DailySummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUsageList(records: List<AppUsageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSummary(summary: DailySummaryEntity)

    @Query("SELECT * FROM app_usage_records WHERE dateStartEpochMillis = :dateStartEpoch ORDER BY foregroundTimeMillis DESC")
    fun getUsageForDate(dateStartEpoch: Long): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage_records WHERE dateStartEpochMillis = :dateStartEpoch ORDER BY foregroundTimeMillis DESC")
    suspend fun getUsageForDateDirect(dateStartEpoch: Long): List<AppUsageEntity>

    @Query("SELECT * FROM app_usage_records WHERE dateStartEpochMillis >= :startEpoch AND dateStartEpochMillis < :endEpoch ORDER BY foregroundTimeMillis DESC")
    fun getUsageForRange(startEpoch: Long, endEpoch: Long): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage_records WHERE dateStartEpochMillis >= :startEpoch AND dateStartEpochMillis < :endEpoch ORDER BY foregroundTimeMillis DESC")
    suspend fun getUsageForRangeDirect(startEpoch: Long, endEpoch: Long): List<AppUsageEntity>

    @Query("SELECT * FROM daily_summaries WHERE dateStartEpochMillis >= :startEpoch AND dateStartEpochMillis < :endEpoch ORDER BY dateStartEpochMillis ASC")
    fun getSummariesInRange(startEpoch: Long, endEpoch: Long): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summaries WHERE dateStartEpochMillis = :dateStartEpoch LIMIT 1")
    suspend fun getSummaryForDate(dateStartEpoch: Long): DailySummaryEntity?

    @Query("SELECT * FROM app_usage_records WHERE isRemoved = 1 ORDER BY dateStartEpochMillis DESC")
    fun getRemovedAppsUsage(): Flow<List<AppUsageEntity>>

    @Query("UPDATE app_usage_records SET isRemoved = 1 WHERE packageName = :packageName")
    suspend fun markAppAsRemoved(packageName: String)

    @Query("SELECT DISTINCT packageName FROM app_usage_records")
    suspend fun getAllRecordedPackageNames(): List<String>

    @Query("SELECT MIN(dateStartEpochMillis) FROM daily_summaries")
    suspend fun getEarliestRecordedDate(): Long?

    @Query("SELECT COUNT(*) FROM daily_summaries")
    suspend fun getTotalDaysTracked(): Int

    @Query("SELECT * FROM daily_summaries ORDER BY dateStartEpochMillis ASC")
    suspend fun getAllSummariesDirect(): List<DailySummaryEntity>

    @Query("SELECT * FROM daily_summaries WHERE dateStartEpochMillis >= :startEpoch AND dateStartEpochMillis < :endEpoch ORDER BY dateStartEpochMillis ASC")
    suspend fun getSummariesInRangeDirect(startEpoch: Long, endEpoch: Long): List<DailySummaryEntity>

    @Transaction
    suspend fun upsertDayUsageAndSummary(
        usageRecords: List<AppUsageEntity>,
        summary: DailySummaryEntity
    ) {
        insertOrUpdateUsageList(usageRecords)
        insertOrUpdateSummary(summary)
    }
}
