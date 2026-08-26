package io.chronicle.usagestats.domain.repository

import io.chronicle.usagestats.domain.model.AppLimitConfig
import io.chronicle.usagestats.domain.model.AppLimitStatus
import io.chronicle.usagestats.domain.model.LimitBypassMode
import kotlinx.coroutines.flow.Flow

interface AppLimitRepository {
    fun getAllLimits(): Flow<List<AppLimitConfig>>
    fun getLimitForPackage(packageName: String): Flow<AppLimitConfig?>
    suspend fun getLimitSync(packageName: String): AppLimitConfig?
    suspend fun setLimit(packageName: String, appLabel: String, dailyMinutes: Int, bypassMode: LimitBypassMode)
    suspend fun removeLimit(packageName: String)
    suspend fun setTemporaryUnlock(packageName: String, unlockUntilMillis: Long)
    suspend fun clearExpiredUnlocks()
    suspend fun getUsedTodayMillis(packageName: String): Long
    suspend fun isAppCurrentlyBlocked(packageName: String): Boolean
}
