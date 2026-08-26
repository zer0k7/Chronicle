package io.chronicle.usagestats.domain.usecase

import io.chronicle.usagestats.domain.model.CustomAppOverride
import io.chronicle.usagestats.domain.repository.UsageRepository
import javax.inject.Inject

class SaveAppOverrideUseCase @Inject constructor(
    private val usageRepository: UsageRepository
) {
    suspend operator fun invoke(override: CustomAppOverride) {
        usageRepository.saveAppOverride(override)
    }
}
