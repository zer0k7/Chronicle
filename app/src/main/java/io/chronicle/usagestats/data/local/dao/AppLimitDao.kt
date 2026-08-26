package io.chronicle.usagestats.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.chronicle.usagestats.data.local.entity.AppLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {

    @Query("SELECT * FROM app_limits WHERE isEnabled = 1")
    fun getAllEnabledLimits(): Flow<List<AppLimitEntity>>

    @Query("SELECT * FROM app_limits")
    fun getAllLimits(): Flow<List<AppLimitEntity>>

    @Query("SELECT * FROM app_limits WHERE packageName = :pkg")
    fun getLimitForPackage(pkg: String): Flow<AppLimitEntity?>

    @Query("SELECT * FROM app_limits WHERE packageName = :pkg LIMIT 1")
    suspend fun getLimitSync(pkg: String): AppLimitEntity?

    @Query("SELECT * FROM app_limits WHERE isEnabled = 1")
    suspend fun getAllEnabledLimitsDirect(): List<AppLimitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLimit(entity: AppLimitEntity)

    @Query("DELETE FROM app_limits WHERE packageName = :pkg")
    suspend fun deleteLimit(pkg: String)

    @Query("UPDATE app_limits SET temporaryUnlockUntilMillis = :unlockUntil WHERE packageName = :pkg")
    suspend fun setTemporaryUnlock(pkg: String, unlockUntil: Long)

    @Query("UPDATE app_limits SET temporaryUnlockUntilMillis = NULL WHERE temporaryUnlockUntilMillis IS NOT NULL AND temporaryUnlockUntilMillis < :nowMillis")
    suspend fun clearExpiredUnlocks(nowMillis: Long)

    @Query("SELECT COUNT(*) FROM app_limits WHERE isEnabled = 1")
    suspend fun getEnabledLimitCount(): Int

    @Query("SELECT COUNT(*) FROM app_limits WHERE isEnabled = 1")
    fun getEnabledLimitCountFlow(): Flow<Int>
}
