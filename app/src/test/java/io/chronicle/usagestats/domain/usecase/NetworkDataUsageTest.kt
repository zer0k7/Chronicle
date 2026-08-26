package io.chronicle.usagestats.domain.usecase

import io.chronicle.usagestats.core.util.DataSizeUtils
import io.chronicle.usagestats.domain.model.AppCategory
import io.chronicle.usagestats.domain.model.DailyDataUsageSummary
import io.chronicle.usagestats.domain.model.DataFilter
import io.chronicle.usagestats.domain.model.DataPeriod
import io.chronicle.usagestats.domain.model.DataSortOrder
import io.chronicle.usagestats.domain.model.DataUsageInfo
import io.chronicle.usagestats.domain.model.NetworkTypeFilter
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
            category = AppCategory.ENTERTAINMENT
        )

        assertEquals(105_000_000L, app.wifiTotalBytes)
        assertEquals(52_000_000L, app.mobileTotalBytes)
        assertEquals(157_000_000L, app.totalBytes)
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
        assertEquals(110_000_000L, summary.grandTotalBytes)
        assertEquals("Brave", summary.topWifiApp?.appLabel)
        assertEquals("WhatsApp", summary.topMobileApp?.appLabel)
    }

    @Test
    fun testDataFilter_Defaults() {
        val filter = DataFilter()
        assertEquals("", filter.searchQuery)
        assertEquals(NetworkTypeFilter.ALL, filter.networkType)
        assertEquals(DataSortOrder.TOTAL_DESC, filter.sortOrder)
    }
}
