package io.chronicle.usagestats.domain.usecase

import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.domain.model.DailyDataUsageSummary
import io.chronicle.usagestats.domain.model.DataFilter
import io.chronicle.usagestats.domain.model.DataPeriod
import io.chronicle.usagestats.domain.repository.NetworkUsageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDataUsageUseCase @Inject constructor(
    private val repository: NetworkUsageRepository
) {
    operator fun invoke(
        period: DataPeriod,
        referenceDate: Long,
        filter: DataFilter,
        billingCycleDay: Int = 1
    ): Flow<DailyDataUsageSummary> {
        return when (period) {
            DataPeriod.DAY -> {
                repository.getDailyDataUsage(referenceDate, filter)
            }
            DataPeriod.WEEK -> {
                val start = DateTimeUtils.getStartOfWeek(referenceDate)
                val end = DateTimeUtils.getEndOfWeek(referenceDate)
                repository.getDataUsageRange(start, end, filter)
            }
            DataPeriod.MONTH -> {
                val start = DateTimeUtils.getStartOfMonth(referenceDate)
                val end = DateTimeUtils.getEndOfMonth(referenceDate)
                repository.getDataUsageRange(start, end, filter)
            }
            DataPeriod.BILLING_CYCLE -> {
                val zdt = DateTimeUtils.toZonedDateTime(referenceDate)
                val dayOfMonth = zdt.dayOfMonth
                val cycleStart = if (dayOfMonth >= billingCycleDay) {
                    zdt.withDayOfMonth(billingCycleDay.coerceIn(1, zdt.toLocalDate().lengthOfMonth()))
                } else {
                    zdt.minusMonths(1).withDayOfMonth(billingCycleDay.coerceIn(1, zdt.minusMonths(1).toLocalDate().lengthOfMonth()))
                }
                val cycleEnd = cycleStart.plusMonths(1)
                val startMillis = cycleStart.toLocalDate().atStartOfDay(DateTimeUtils.IST_ZONE_ID).toInstant().toEpochMilli()
                val endMillis = cycleEnd.toLocalDate().atStartOfDay(DateTimeUtils.IST_ZONE_ID).toInstant().toEpochMilli()
                repository.getDataUsageRange(startMillis, endMillis, filter)
            }
        }
    }
}
