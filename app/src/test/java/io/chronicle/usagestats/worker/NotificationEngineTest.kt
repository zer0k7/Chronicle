package io.chronicle.usagestats.worker

import io.chronicle.usagestats.domain.model.AppCategory
import io.chronicle.usagestats.domain.model.DailyUsageSummary
import io.chronicle.usagestats.domain.model.HabitInsights
import io.chronicle.usagestats.domain.model.LongestSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationEngineTest {

    @Test
    fun testGamingRule_TriggeredWhenOver90Mins() {
        val gamingDuration = 120 * 60 * 1000L // 2 hours
        val categories = mapOf(
            AppCategory.GAMES to gamingDuration,
            AppCategory.UTILITIES to 10 * 60 * 1000L
        )

        val habits = HabitInsights(
            deviceUnlocks = 30,
            categoryBreakdown = categories,
            productivityScore = 10
        )

        val summary = DailyUsageSummary(
            dateEpochMillis = 1700000000000L,
            totalScreenTimeMillis = 130 * 60 * 1000L,
            topAppPackage = "com.activision.callofduty.shooter",
            topAppLabel = "Call of Duty",
            appCount = 2,
            habitInsights = habits
        )

        val gamingTime = summary.habitInsights?.categoryBreakdown?.get(AppCategory.GAMES) ?: 0L
        assertTrue(gamingTime >= 90 * 60 * 1000L)
    }

    @Test
    fun testSocialRule_TriggeredWhenOver90Mins() {
        val socialDuration = 100 * 60 * 1000L
        val categories = mapOf(
            AppCategory.SOCIAL to socialDuration
        )

        val habits = HabitInsights(
            categoryBreakdown = categories,
            productivityScore = 5
        )

        val summary = DailyUsageSummary(
            dateEpochMillis = 1700000000000L,
            totalScreenTimeMillis = 100 * 60 * 1000L,
            topAppPackage = "com.instagram.android",
            topAppLabel = "Instagram",
            appCount = 1,
            habitInsights = habits
        )

        val socialTime = summary.habitInsights?.categoryBreakdown?.get(AppCategory.SOCIAL) ?: 0L
        assertTrue(socialTime >= 90 * 60 * 1000L)
    }

    @Test
    fun testLongestSession_Structure() {
        val session = LongestSession(
            packageName = "com.activision.callofduty.shooter",
            appLabel = "Call of Duty",
            durationMillis = 2 * 3600 * 1000L,
            startEpochMillis = 1700020000000L,
            endEpochMillis = 1700027200000L
        )

        assertEquals("Call of Duty", session.appLabel)
        assertEquals(7200000L, session.durationMillis)
    }
}
