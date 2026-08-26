package io.chronicle.usagestats.domain.usecase

import io.chronicle.usagestats.core.util.DataSizeUtils
import io.chronicle.usagestats.domain.model.AppCategory
import io.chronicle.usagestats.domain.model.AppHabitLoop
import io.chronicle.usagestats.domain.model.BurnoutRisk
import io.chronicle.usagestats.domain.model.CategoryDataShare
import io.chronicle.usagestats.domain.model.ContinuousDoomscrollSession
import io.chronicle.usagestats.domain.model.DailyDataPoint
import io.chronicle.usagestats.domain.model.DailyDataUsageSummary
import io.chronicle.usagestats.domain.model.DataDepletionForecast
import io.chronicle.usagestats.domain.model.DataFilter
import io.chronicle.usagestats.domain.model.DataSortOrder
import io.chronicle.usagestats.domain.model.DataUsageInfo
import io.chronicle.usagestats.domain.model.HourlyDataPoint
import io.chronicle.usagestats.domain.model.NetworkTypeFilter
import io.chronicle.usagestats.domain.model.ScreenTimeForecast
import io.chronicle.usagestats.domain.model.SleepDataLeakInfo
import io.chronicle.usagestats.domain.model.WeeklyExecutiveBriefing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkDataUsageTest {

    @Test
    fun testDataSizeFormatting() {
        assertEquals("0 B", DataSizeUtils.formatBytes(0))
        assertEquals("500 B", DataSizeUtils.formatBytes(500))
        assertEquals("1 KB", DataSizeUtils.formatBytes(1024))
        assertEquals("1.5 MB", DataSizeUtils.formatBytes((1.5 * 1024 * 1024).toLong()))
        assertEquals("2.50 GB", DataSizeUtils.formatBytes((2.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testDataUsageInfo_Aggregation() {
        val app = DataUsageInfo(
            packageName = "com.google.android.youtube",
            appLabel = "YouTube",
            wifiRxBytes = 100_000_000L,
            wifiTxBytes = 5_000_000L,
            mobileRxBytes = 50_000_000L,
            mobileTxBytes = 2_000_000L,
            isWifiPreferred = true,
            category = AppCategory.ENTERTAINMENT
        )

        assertEquals(105_000_000L, app.wifiTotalBytes)
        assertEquals(52_000_000L, app.mobileTotalBytes)
        assertEquals(157_000_000L, app.totalBytes)
        assertTrue(app.isWifiPreferred)
        assertFalse(app.isHotspot)
        assertFalse(app.isRemoved)
    }

    @Test
    fun testHotspotDataUsageInfo() {
        val hotspot = DataUsageInfo(
            packageName = "system.tethering.hotspot",
            appLabel = "Mobile Hotspot & Tethering",
            mobileRxBytes = 350_000_000L,
            mobileTxBytes = 50_000_000L,
            isHotspot = true,
            category = AppCategory.SYSTEM
        )

        assertTrue(hotspot.isHotspot)
        assertEquals(400_000_000L, hotspot.totalBytes)
    }

    @Test
    fun testDailyDataUsageSummary_Totals() {
        val app1 = DataUsageInfo(
            packageName = "com.brave.browser",
            appLabel = "Brave",
            wifiRxBytes = 50_000_000L,
            wifiTxBytes = 10_000_000L
        )

        val app2 = DataUsageInfo(
            packageName = "com.whatsapp",
            appLabel = "WhatsApp",
            mobileRxBytes = 30_000_000L,
            mobileTxBytes = 5_000_000L
        )

        val summary = DailyDataUsageSummary(
            dateEpochMillis = 1700000000000L,
            totalWifiRxBytes = 50_000_000L,
            totalWifiTxBytes = 10_000_000L,
            totalMobileRxBytes = 30_000_000L,
            totalMobileTxBytes = 5_000_000L,
            totalHotspotBytes = 15_000_000L,
            appUsageList = listOf(app1, app2),
            topWifiApp = app1,
            topMobileApp = app2
        )

        assertEquals(60_000_000L, summary.totalWifiBytes)
        assertEquals(35_000_000L, summary.totalMobileBytes)
        assertEquals(95_000_000L, summary.grandTotalBytes)
        assertEquals(15_000_000L, summary.totalHotspotBytes)
        assertEquals("Brave", summary.topWifiApp?.appLabel)
        assertEquals("WhatsApp", summary.topMobileApp?.appLabel)
    }

    @Test
    fun testHourlyDataPoints() {
        val point = HourlyDataPoint(
            hour = 14,
            wifiBytes = 150_000_000L,
            mobileBytes = 50_000_000L
        )
        assertEquals(14, point.hour)
        assertEquals(200_000_000L, point.totalBytes)
    }

    @Test
    fun testDailyDataPoints() {
        val daily = DailyDataPoint(
            dateEpochMillis = 1700000000000L,
            dayLabel = "Mon 24",
            wifiBytes = 500_000_000L,
            mobileBytes = 200_000_000L
        )
        assertEquals("Mon 24", daily.dayLabel)
        assertEquals(700_000_000L, daily.totalBytes)
    }

    @Test
    fun testCategoryDataShare() {
        val share = CategoryDataShare(
            category = AppCategory.SOCIAL,
            totalBytes = 250_000_000L,
            percentage = 45.5f
        )
        assertEquals(AppCategory.SOCIAL, share.category)
        assertEquals(250_000_000L, share.totalBytes)
        assertEquals(45.5f, share.percentage, 0.01f)
    }

    @Test
    fun testSleepDataLeakInfo() {
        val app = DataUsageInfo(
            packageName = "com.cloud.sync",
            appLabel = "Cloud Sync",
            wifiRxBytes = 10_000_000L
        )
        val leak = SleepDataLeakInfo(
            app = app,
            sleepBytes = 10_000_000L,
            percentageOfNight = 80.0f
        )
        assertEquals("Cloud Sync", leak.app.appLabel)
        assertEquals(10_000_000L, leak.sleepBytes)
        assertEquals(80.0f, leak.percentageOfNight, 0.01f)
    }

    @Test
    fun testDataDepletionForecast() {
        val forecast = DataDepletionForecast(
            burnRateBytesPerHour = 250_000_000L,
            projectedDepletionEpochMillis = 1700050000000L,
            isAtRiskOfDepletion = true,
            percentOfQuotaBurned = 75,
            statusMessage = "High cellular burn rate."
        )
        assertEquals(250_000_000L, forecast.burnRateBytesPerHour)
        assertTrue(forecast.isAtRiskOfDepletion)
        assertEquals(75, forecast.percentOfQuotaBurned)
    }

    @Test
    fun testScreenTimeForecastAndBurnout() {
        val forecast = ScreenTimeForecast(
            projectedMillis = 5 * 3600 * 1000L,
            currentVelocityMinutesPerHour = 25f,
            burnoutRisk = BurnoutRisk.MODERATE,
            burnoutScore = 55,
            reason = "Moderate digital stimulation."
        )
        assertEquals(5 * 3600 * 1000L, forecast.projectedMillis)
        assertEquals(BurnoutRisk.MODERATE, forecast.burnoutRisk)
        assertEquals(55, forecast.burnoutScore)
    }

    @Test
    fun testAppHabitLoop() {
        val loop = AppHabitLoop(
            triggerPackage = "com.whatsapp",
            triggerLabel = "WhatsApp",
            targetPackage = "com.instagram.android",
            targetLabel = "Instagram",
            transitionCount = 12,
            averageTargetTimeMillis = 28 * 60 * 1000L
        )
        assertEquals("WhatsApp", loop.triggerLabel)
        assertEquals("Instagram", loop.targetLabel)
        assertEquals(12, loop.transitionCount)
    }

    @Test
    fun testWeeklyExecutiveBriefing() {
        val briefing = WeeklyExecutiveBriefing(
            totalScreenTimeMillis = 24 * 3600 * 1000L,
            dailyAverageMillis = 3 * 3600 * 1000L + 25 * 60 * 1000L,
            consciousReclaimedMillis = 88 * 3600 * 1000L,
            topDistractionApp = "Instagram",
            longestFocusStreakMinutes = 90,
            burnoutRisk = BurnoutRisk.LOW,
            efficiencyScore = 78
        )
        assertEquals(78, briefing.efficiencyScore)
        assertEquals("Instagram", briefing.topDistractionApp)
        assertEquals(BurnoutRisk.LOW, briefing.burnoutRisk)
    }

    @Test
    fun testDataFilter_Defaults() {
        val filter = DataFilter()
        assertEquals("", filter.searchQuery)
        assertEquals(NetworkTypeFilter.ALL, filter.networkType)
        assertEquals(DataSortOrder.TOTAL_DESC, filter.sortOrder)
        assertEquals(null, filter.selectedHour)
    }
}
