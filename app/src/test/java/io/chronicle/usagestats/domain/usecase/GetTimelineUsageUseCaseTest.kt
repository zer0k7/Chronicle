package io.chronicle.usagestats.domain.usecase

import io.chronicle.usagestats.domain.model.TimelineData
import io.chronicle.usagestats.domain.model.TimelinePeriod
import io.chronicle.usagestats.domain.repository.UsageRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetTimelineUsageUseCaseTest {

    private val usageRepository: UsageRepository = mockk()
    private lateinit var getTimelineUsageUseCase: GetTimelineUsageUseCase

    private val sampleTimelineData = TimelineData(
        period = TimelinePeriod.DAY,
        startEpochMillis = 1700000000000L,
        endEpochMillis = 1700086399000L,
        totalDurationMillis = 5000000L,
        activeAppCount = 5,
        dailySummaries = emptyList(),
        topApps = emptyList()
    )

    @Before
    fun setUp() {
        getTimelineUsageUseCase = GetTimelineUsageUseCase(usageRepository)
        every { usageRepository.getTimelineData(any(), any()) } returns flowOf(sampleTimelineData)
    }

    @Test
    fun testInvoke_ReturnsTimelineData() = runTest {
        val result = getTimelineUsageUseCase(TimelinePeriod.DAY, 1700000000000L).first()

        assertEquals(TimelinePeriod.DAY, result.period)
        assertEquals(5000000L, result.totalDurationMillis)
        assertEquals(5, result.activeAppCount)
    }
}
