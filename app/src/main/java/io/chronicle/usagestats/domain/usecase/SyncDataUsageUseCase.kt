package io.chronicle.usagestats.domain.usecase

import io.chronicle.usagestats.domain.repository.NetworkUsageRepository
import javax.inject.Inject

class SyncDataUsageUseCase @Inject constructor(
    private val repository: NetworkUsageRepository
) {
    suspend fun syncDate(dateMillis: Long) {
        repository.syncDataUsage(dateMillis)
    }

    suspend fun syncRange(startMillis: Long, endMillis: Long) {
        repository.syncDateRange(startMillis, endMillis)
    }
}
