package io.chronicle.usagestats.domain.usecase

import io.chronicle.usagestats.domain.model.AppCategory
import io.chronicle.usagestats.domain.model.DopamineDebt
import io.chronicle.usagestats.domain.model.GhostOpensInsight
import io.chronicle.usagestats.domain.model.HabitInsights
import io.chronicle.usagestats.domain.model.HourlyUsageSlot
import io.chronicle.usagestats.domain.model.LifeClockProjection
import io.chronicle.usagestats.domain.model.MorningDoomscroll
import io.chronicle.usagestats.domain.model.PhantomUnlocks
import io.chronicle.usagestats.domain.model.RangeUsageReport
import io.chronicle.usagestats.domain.model.TimelineData
import io.chronicle.usagestats.domain.model.TimelinePeriod
import io.chronicle.usagestats.domain.model.TrendComparison
import io.chronicle.usagestats.domain.model.WakingLifeImpact
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

    @Test
    fun testWakingLifeImpact_Calculation() {
        val totalScreenTimeMillis = (4.5 * 3600 * 1000).toLong() // 4.5 hours
        val wakingDayMillis = 16.0 * 3600.0 * 1000.0 // 16 waking hours
        val percentage = (totalScreenTimeMillis / wakingDayMillis) * 100.0
        val annualDays = ((totalScreenTimeMillis.toDouble() / (1000 * 3600)) * 365.0 / 24.0).toInt()

        val impact = WakingLifeImpact(
            wakingPercentage = percentage,
            annualProjectedDays = annualDays
        )

        assertEquals(28.125, impact.wakingPercentage, 0.01)
        assertEquals(68, impact.annualProjectedDays)
    }

    @Test
    fun testGhostOpensInsight_Structure() {
        val ghost = GhostOpensInsight(
            totalGhostOpens = 24,
            topGhostAppLabel = "Instagram",
            topGhostAppOpens = 18
        )

        assertEquals(24, ghost.totalGhostOpens)
        assertEquals("Instagram", ghost.topGhostAppLabel)
        assertEquals(18, ghost.topGhostAppOpens)
    }

    @Test
    fun testMorningDoomscroll_Structure() {
        val doom = MorningDoomscroll(
            durationMillis = 35 * 60 * 1000L,
            topAppLabel = "YouTube"
        )

        assertEquals(2100000L, doom.durationMillis)
        assertEquals("YouTube", doom.topAppLabel)
    }

    @Test
    fun testLifeClockProjection_Calculation() {
        val dailyAverageMillis = (4.8 * 3600 * 1000).toLong() // 4.8h/day
        val yearsLostBy75 = (dailyAverageMillis.toDouble() / (24.0 * 3600.0 * 1000.0)) * 50.0 // 50-yr baseline
        val consciousPct = (dailyAverageMillis.toDouble() / (16.0 * 3600.0 * 1000.0)) * 100.0

        val lifeClock = LifeClockProjection(
            dailyAverageMillis = dailyAverageMillis,
            yearsLostBy75 = yearsLostBy75,
            consciousPercentage = consciousPct
        )

        assertEquals(10.0, lifeClock.yearsLostBy75, 0.01)
        assertEquals(30.0, lifeClock.consciousPercentage, 0.01)
    }

    @Test
    fun testDopamineDebt_Calculation() {
        val actualMillis = (4.5 * 3600 * 1000).toLong() // 4.5h
        val baselineMillis = (2.5 * 3600 * 1000).toLong() // 2.5h baseline
        val debtMillis = (actualMillis - baselineMillis).coerceAtLeast(0L) // 2.0h excess
        val fastMinutes = ((debtMillis.toDouble() / 3_600_000.0) * 30.0).toInt() // 60 mins fast

        val dopamineDebt = DopamineDebt(
            weeklyActualMillis = actualMillis,
            weeklyBaselineMillis = baselineMillis,
            debtMillis = debtMillis,
            recommendedFastMinutes = fastMinutes
        )

        assertEquals(7200000L, dopamineDebt.debtMillis)
        assertEquals(60, dopamineDebt.recommendedFastMinutes)
    }

    @Test
    fun testPhantomUnlocks_Structure() {
        val phantom = PhantomUnlocks(
            count = 14,
            totalQuickChecks = 28
        )

        assertEquals(14, phantom.count)
        assertEquals(28, phantom.totalQuickChecks)
    }

    @Test
    fun testDisciplineStreaks_Structure() {
        val streaks = io.chronicle.usagestats.domain.model.DisciplineStreaks(
            goalStreakDays = 5,
            morningShieldStreakDays = 4,
            sleepSanctuaryStreakDays = 3,
            bestGoalStreakDays = 12
        )

        assertEquals(5, streaks.goalStreakDays)
        assertEquals(4, streaks.morningShieldStreakDays)
        assertEquals(3, streaks.sleepSanctuaryStreakDays)
        assertEquals(12, streaks.bestGoalStreakDays)
    }

    @Test
    fun testCustomAppOverride_Defaults() {
        val override = io.chronicle.usagestats.domain.model.CustomAppOverride(
            packageName = "com.google.android.youtube",
            customCategory = AppCategory.PRODUCTIVITY,
            isDistraction = true,
            dailyLimitMinutes = 45
        )

        assertEquals("com.google.android.youtube", override.packageName)
        assertEquals(AppCategory.PRODUCTIVITY, override.customCategory)
        assertTrue(override.isDistraction)
        assertEquals(45, override.dailyLimitMinutes)
    }
}
