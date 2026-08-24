package io.chronicle.usagestats.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZonedDateTime

class DateTimeUtilsTest {

    @Test
    fun testIstZoneId() {
        val now = DateTimeUtils.nowInIst()
        assertEquals("Asia/Kolkata", now.zone.id)
    }

    @Test
    fun testFormatDuration() {
        assertEquals("0m", DateTimeUtils.formatDuration(0L))
        assertEquals("45s", DateTimeUtils.formatDuration(45_000L))
        assertEquals("15m", DateTimeUtils.formatDuration(15 * 60 * 1000L))
        assertEquals("2h 30m", DateTimeUtils.formatDuration((2 * 3600 + 30 * 60) * 1000L))
        assertEquals("4h", DateTimeUtils.formatDuration(4 * 3600 * 1000L))
    }

    @Test
    fun testStartAndEndOfDay() {
        val now = System.currentTimeMillis()
        val startOfDay = DateTimeUtils.getStartOfDay(now)
        val endOfDay = DateTimeUtils.getEndOfDay(now)

        assertTrue(endOfDay > startOfDay)
        assertEquals(24 * 60 * 60 * 1000L, endOfDay - startOfDay)

        val startZoned = DateTimeUtils.toZonedDateTime(startOfDay)
        assertEquals(0, startZoned.hour)
        assertEquals(0, startZoned.minute)
        assertEquals(0, startZoned.second)

        val endZoned = DateTimeUtils.toZonedDateTime(endOfDay)
        assertEquals(0, endZoned.hour)
        assertEquals(0, endZoned.minute)
        assertEquals(0, endZoned.second)
    }

    @Test
    fun testStartAndEndOfWeek() {
        val now = System.currentTimeMillis()
        val startOfWeek = DateTimeUtils.getStartOfWeek(now)
        val endOfWeek = DateTimeUtils.getEndOfWeek(now)

        assertTrue(endOfWeek > startOfWeek)

        val startZoned = DateTimeUtils.toZonedDateTime(startOfWeek)
        assertEquals(DayOfWeek.MONDAY, startZoned.dayOfWeek)
        assertEquals(0, startZoned.hour)
        assertEquals(0, startZoned.minute)

        val endZoned = DateTimeUtils.toZonedDateTime(endOfWeek)
        assertEquals(DayOfWeek.MONDAY, endZoned.dayOfWeek)
        assertEquals(0, endZoned.hour)
        assertEquals(0, endZoned.minute)
    }

    @Test
    fun testStartAndEndOfMonth() {
        val now = System.currentTimeMillis()
        val startOfMonth = DateTimeUtils.getStartOfMonth(now)
        val endOfMonth = DateTimeUtils.getEndOfMonth(now)

        assertTrue(endOfMonth > startOfMonth)

        val startZoned = DateTimeUtils.toZonedDateTime(startOfMonth)
        assertEquals(1, startZoned.dayOfMonth)

        val endZoned = DateTimeUtils.toZonedDateTime(endOfMonth)
        assertEquals(1, endZoned.dayOfMonth)
        assertEquals(0, endZoned.hour)
        assertEquals(0, endZoned.minute)
    }

    @Test
    fun testStartAndEndOfYear() {
        val now = System.currentTimeMillis()
        val startOfYear = DateTimeUtils.getStartOfYear(now)
        val endOfYear = DateTimeUtils.getEndOfYear(now)

        assertTrue(endOfYear > startOfYear)

        val startZoned = DateTimeUtils.toZonedDateTime(startOfYear)
        assertEquals(1, startZoned.dayOfMonth)
        assertEquals(1, startZoned.monthValue)

        val endZoned = DateTimeUtils.toZonedDateTime(endOfYear)
        assertEquals(1, endZoned.dayOfMonth)
        assertEquals(1, endZoned.monthValue)
        assertEquals(0, endZoned.hour)
        assertEquals(0, endZoned.minute)
    }

    @Test
    fun testFormatPeriodLabel() {
        // Test DAY format
        val now = System.currentTimeMillis()
        val startOfDay = DateTimeUtils.getStartOfDay(now)
        val endOfDay = DateTimeUtils.getEndOfDay(now)
        val dayLabel = DateTimeUtils.formatPeriodLabel(
            io.chronicle.usagestats.domain.model.TimelinePeriod.DAY,
            startOfDay,
            endOfDay
        )
        assertTrue(dayLabel.isNotEmpty())
        // Should NOT contain a dash indicating range
        org.junit.Assert.assertFalse(dayLabel.contains(" - "))

        // Test WEEK format
        val startOfWeek = DateTimeUtils.getStartOfWeek(now)
        val endOfWeek = DateTimeUtils.getEndOfWeek(now)
        val weekLabel = DateTimeUtils.formatPeriodLabel(
            io.chronicle.usagestats.domain.model.TimelinePeriod.WEEK,
            startOfWeek,
            endOfWeek
        )
        assertTrue(weekLabel.contains(" - "))
    }

    @Test
    fun testGetDaysInRange() {
        val now = System.currentTimeMillis()
        val startOfDay = DateTimeUtils.getStartOfDay(now)
        val endOfDay = DateTimeUtils.getEndOfDay(now)
        val singleDayList = DateTimeUtils.getDaysInRange(startOfDay, endOfDay)
        assertEquals(1, singleDayList.size)

        val startOfWeek = DateTimeUtils.getStartOfWeek(now)
        val endOfWeek = DateTimeUtils.getEndOfWeek(now)
        val weekDaysList = DateTimeUtils.getDaysInRange(startOfWeek, endOfWeek)
        assertEquals(7, weekDaysList.size)
    }
}

