package io.chronicle.usagestats.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.chronicle.usagestats.BuildConfig
import io.chronicle.usagestats.data.local.dao.AppLimitDao
import io.chronicle.usagestats.data.local.entity.AppLimitEntity
import io.chronicle.usagestats.domain.model.AppLimitConfig
import io.chronicle.usagestats.domain.model.LimitBypassMode
import io.chronicle.usagestats.domain.repository.AppLimitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class AppLimitRepositoryImpl @Inject constructor(
    private val dao: AppLimitDao,
    @ApplicationContext private val context: Context
) : AppLimitRepository {

    companion object {
        private const val TAG = "AppLimitRepository"
        private val IST_ZONE = ZoneId.of("Asia/Kolkata")
    }

    override fun getAllLimits(): Flow<List<AppLimitConfig>> {
        return dao.getAllLimits().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getLimitForPackage(packageName: String): Flow<AppLimitConfig?> {
        return dao.getLimitForPackage(packageName).map { it?.toDomainModel() }
    }

    override suspend fun getLimitSync(packageName: String): AppLimitConfig? {
        return dao.getLimitSync(packageName)?.toDomainModel()
    }

    override suspend fun setLimit(
        packageName: String,
        appLabel: String,
        dailyMinutes: Int,
        bypassMode: LimitBypassMode
    ) {
        val entity = AppLimitEntity(
            packageName = packageName,
            appLabel = appLabel,
            dailyLimitMinutes = dailyMinutes,
            isEnabled = true,
            bypassMode = bypassMode.name,
            temporaryUnlockUntilMillis = null
        )
        dao.upsertLimit(entity)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Set limit for $packageName: $dailyMinutes minutes")
        }
    }

    override suspend fun removeLimit(packageName: String) {
        dao.deleteLimit(packageName)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Removed limit for $packageName")
        }
    }

    override suspend fun setTemporaryUnlock(packageName: String, unlockUntilMillis: Long) {
        dao.setTemporaryUnlock(packageName, unlockUntilMillis)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Set temporary unlock for $packageName until $unlockUntilMillis")
        }
    }

    override suspend fun clearExpiredUnlocks() {
        val now = System.currentTimeMillis()
        dao.clearExpiredUnlocks(now)
    }

    override suspend fun getUsedTodayMillis(packageName: String): Long {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usageStatsManager == null) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "UsageStatsManager is null")
            }
            return 0L
        }

        val now = System.currentTimeMillis()
        val todayStartMillis = LocalDate.now(IST_ZONE).atStartOfDay(IST_ZONE).toInstant().toEpochMilli()

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            todayStartMillis,
            now
        )

        return usageStatsList
            .filter { it.packageName == packageName }
            .sumOf { it.totalTimeInForeground }
    }

    override suspend fun isAppCurrentlyBlocked(packageName: String): Boolean {
        val limit = getLimitSync(packageName) ?: return false
        if (!limit.isEnabled) return false

        val now = System.currentTimeMillis()
        if (limit.temporaryUnlockUntilMillis != null && limit.temporaryUnlockUntilMillis > now) {
            return false
        }

        val usedMillis = getUsedTodayMillis(packageName)
        val limitMillis = limit.dailyLimitMinutes * 60 * 1000L

        return usedMillis >= limitMillis
    }

    private fun AppLimitEntity.toDomainModel(): AppLimitConfig {
        val bypassModeEnum = try {
            LimitBypassMode.valueOf(this.bypassMode)
        } catch (e: IllegalArgumentException) {
            LimitBypassMode.NO_BYPASS
        }

        return AppLimitConfig(
            packageName = this.packageName,
            appLabel = this.appLabel,
            dailyLimitMinutes = this.dailyLimitMinutes,
            isEnabled = this.isEnabled,
            bypassMode = bypassModeEnum,
            temporaryUnlockUntilMillis = this.temporaryUnlockUntilMillis
        )
    }
}
