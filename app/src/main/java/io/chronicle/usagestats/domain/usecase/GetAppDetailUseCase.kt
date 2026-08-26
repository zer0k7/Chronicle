package io.chronicle.usagestats.domain.usecase

import io.chronicle.usagestats.domain.model.AppDetailInfo
import io.chronicle.usagestats.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppDetailUseCase @Inject constructor(
    private val usageRepository: UsageRepository
) {
    operator fun invoke(packageName: String, dateStartEpochMillis: Long): Flow<AppDetailInfo> {
        return usageRepository.getAppDetail(packageName, dateStartEpochMillis)
    }
}
