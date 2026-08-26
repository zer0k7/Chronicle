package io.chronicle.usagestats.domain.repository

import io.chronicle.usagestats.domain.model.DailyDataUsageSummary
import io.chronicle.usagestats.domain.model.DataFilter
import kotlinx.coroutines.flow.Flow

interface NetworkUsageRepository {
    fun getDailyDataUsage(dateMillis: Long, filter: DataFilter): Flow<DailyDataUsageSummary>
    fun getDataUsageRange(startMillis: Long, endMillis: Long, filter: DataFilter): Flow<DailyDataUsageSummary>
    suspend fun syncDataUsage(dateMillis: Long)
    suspend fun syncDateRange(startMillis: Long, endMillis: Long)
}
