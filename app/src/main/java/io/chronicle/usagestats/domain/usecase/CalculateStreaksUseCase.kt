package io.chronicle.usagestats.domain.usecase

import io.chronicle.usagestats.domain.model.DisciplineStreaks
import io.chronicle.usagestats.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CalculateStreaksUseCase @Inject constructor(
    private val usageRepository: UsageRepository
) {
    operator fun invoke(goalMinutes: Int): Flow<DisciplineStreaks> {
        return usageRepository.getDisciplineStreaks(goalMinutes)
    }
}
