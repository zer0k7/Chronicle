package io.chronicle.usagestats.domain.usecase

import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.domain.repository.UsageRepository

class SyncUsageDataUseCase(
    private val usageRepository: UsageRepository
) {
    suspend fun syncToday() {
        val today = DateTimeUtils.getStartOfDay()
        usageRepository.syncUsageForDate(today)
    }

    suspend fun syncDate(dateEpochMillis: Long) {
        val dayStart = DateTimeUtils.getStartOfDay(dateEpochMillis)
        usageRepository.syncUsageForDate(dayStart)
    }

    suspend fun syncRecentDays(dayCount: Int = 30) {
        val now = DateTimeUtils.nowInIst()
        for (i in 0 until dayCount) {
            val date = now.minusDays(i.toLong())
            val epoch = date.toLocalDate().atStartOfDay(DateTimeUtils.IST_ZONE_ID).toInstant().toEpochMilli()
            usageRepository.syncUsageForDate(epoch)
        }
    }
}
