package io.chronicle.usagestats.domain.usecase

import io.chronicle.usagestats.domain.model.TimelineData
import io.chronicle.usagestats.domain.model.TimelinePeriod
import io.chronicle.usagestats.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow

class GetTimelineUsageUseCase(
    private val usageRepository: UsageRepository
) {
    operator fun invoke(period: TimelinePeriod, referenceEpochMillis: Long): Flow<TimelineData> {
        return usageRepository.getTimelineData(period, referenceEpochMillis)
    }
}
