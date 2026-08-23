package io.chronicle.usagestats.domain.usecase

import io.chronicle.usagestats.domain.model.AppCategory
import io.chronicle.usagestats.domain.model.AppUsageInfo
import io.chronicle.usagestats.domain.model.DailyUsageSummary
import io.chronicle.usagestats.domain.model.ReportFilter
import io.chronicle.usagestats.domain.repository.UsageRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetReportUseCaseTest {

    private val usageRepository: UsageRepository = mockk()
    private lateinit var getReportUseCase: GetReportUseCase

    private val sampleApps = listOf(
        AppUsageInfo(
            packageName = "com.google.android.youtube",
            appLabel = "YouTube",
            totalTimeForegroundMillis = 3600000L,
            launchCount = 10,
            isRemoved = false,
            category = AppCategory.ENTERTAINMENT
        ),
        AppUsageInfo(
            packageName = "com.whatsapp",
            appLabel = "WhatsApp",
            totalTimeForegroundMillis = 1800000L,
            launchCount = 25,
            isRemoved = false,
            category = AppCategory.SOCIAL
        ),
        AppUsageInfo(
            packageName = "com.example.deletedgame",
            appLabel = "Old Game",
            totalTimeForegroundMillis = 900000L,
            launchCount = 2,
            isRemoved = true,
            category = AppCategory.REMOVED
        )
    )

    private val sampleSummary = DailyUsageSummary(
        dateEpochMillis = 1700000000000L,
        totalScreenTimeMillis = 6300000L,
        topAppPackage = "com.google.android.youtube",
        topAppLabel = "YouTube",
        appCount = 3,
        apps = sampleApps
    )

    @Before
    fun setUp() {
        getReportUseCase = GetReportUseCase(usageRepository)
        every { usageRepository.getDailyUsage(any()) } returns flowOf(sampleSummary)
    }

    @Test
    fun testGetDailyReport_NoFilter_ReturnsAllApps() = runTest {
        val result = getReportUseCase.getDailyReport(1700000000000L, ReportFilter()).first()

        assertEquals(3, result.apps.size)
        assertEquals(3, result.appCount)
    }

    @Test
    fun testGetDailyReport_FilterByCategory_ReturnsCategoryOnly() = runTest {
        val filter = ReportFilter(selectedCategory = AppCategory.SOCIAL)
        val result = getReportUseCase.getDailyReport(1700000000000L, filter).first()

        assertEquals(1, result.apps.size)
        assertEquals("WhatsApp", result.apps.first().appLabel)
    }

    @Test
    fun testGetDailyReport_FilterByRemovedApps_ReturnsRemovedOnly() = runTest {
        val filter = ReportFilter(selectedCategory = AppCategory.REMOVED)
        val result = getReportUseCase.getDailyReport(1700000000000L, filter).first()

        assertEquals(1, result.apps.size)
        assertTrue(result.apps.first().isRemoved)
        assertEquals("Old Game", result.apps.first().appLabel)
    }

    @Test
    fun testGetDailyReport_FilterBySearchQuery_ReturnsMatchingApps() = runTest {
        val filter = ReportFilter(searchQuery = "you")
        val result = getReportUseCase.getDailyReport(1700000000000L, filter).first()

        assertEquals(1, result.apps.size)
        assertEquals("YouTube", result.apps.first().appLabel)
    }
}
