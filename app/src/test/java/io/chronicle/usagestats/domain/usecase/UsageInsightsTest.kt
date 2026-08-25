package io.chronicle.usagestats.domain.usecase

import io.chronicle.usagestats.domain.model.AppCategory
import io.chronicle.usagestats.domain.model.HabitInsights
import io.chronicle.usagestats.domain.model.HourlyUsageSlot
import io.chronicle.usagestats.domain.model.TimelineData
import io.chronicle.usagestats.domain.model.TimelinePeriod
import io.chronicle.usagestats.domain.model.TrendComparison
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageInsightsTest {

    @Test
    fun testHourlySlot_Structure() {
        val slot = HourlyUsageSlot(
            hour = 14,
            totalDurationMillis = 45 * 60 * 1000L,
            topAppPackage = "com.brave.browser",
            topAppLabel = "Brave",
            appBreakdown = emptyList()
        )

        assertEquals(14, slot.hour)
        assertEquals(2700000L, slot.totalDurationMillis)
        assertEquals("Brave", slot.topAppLabel)
    }

    @Test
    fun testHabitInsights_Scoring() {
        val categories = mapOf(
            AppCategory.PRODUCTIVITY to 3600000L,
            AppCategory.UTILITIES to 1800000L,
            AppCategory.SOCIAL to 1800000L
        )
        val total = categories.values.sum()

        val habits = HabitInsights(
            deviceUnlocks = 42,
            firstUnlockEpochMillis = 1700010000000L,
            lastLockEpochMillis = 1700080000000L,
            bedtimeUsageMillis = 1200000L,
            avgSessionDurationMillis = 300000L,
            fragmentationScore = 25,
            productivityScore = 75,
            categoryBreakdown = categories
        )

        assertEquals(42, habits.deviceUnlocks)
        assertEquals(25, habits.fragmentationScore)
        assertEquals(75, habits.productivityScore)
        assertEquals(3, habits.categoryBreakdown.size)
    }

    @Test
    fun testTrendComparison_Delta() {
        val current = 7200000L // 2h
        val previous = 10800000L // 3h
        val delta = current - previous
        val percentage = (delta.toDouble() / previous) * 100.0

        val trend = TrendComparison(
            previousPeriodDurationMillis = previous,
            deltaDurationMillis = delta,
            percentageChange = percentage
        )

        assertEquals(-3600000L, trend.deltaDurationMillis)
        assertTrue(trend.percentageChange < 0)
        assertEquals(-33.33, trend.percentageChange, 0.1)
    }
}
