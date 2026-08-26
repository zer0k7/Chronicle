package io.chronicle.usagestats.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.chronicle.usagestats.data.local.entity.AppOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppOverrideDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverride(override: AppOverrideEntity)

    @Query("SELECT * FROM app_custom_overrides WHERE packageName = :packageName LIMIT 1")
    fun getOverride(packageName: String): Flow<AppOverrideEntity?>

    @Query("SELECT * FROM app_custom_overrides WHERE packageName = :packageName LIMIT 1")
    suspend fun getOverrideDirect(packageName: String): AppOverrideEntity?

    @Query("SELECT * FROM app_custom_overrides")
    fun getAllOverrides(): Flow<List<AppOverrideEntity>>

    @Query("SELECT * FROM app_custom_overrides")
    suspend fun getAllOverridesDirect(): List<AppOverrideEntity>

    @Query("DELETE FROM app_custom_overrides WHERE packageName = :packageName")
    suspend fun deleteOverride(packageName: String)
}
