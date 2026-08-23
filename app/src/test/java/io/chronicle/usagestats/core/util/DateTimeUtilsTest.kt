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

        val startZoned = DateTimeUtils.toZonedDateTime(startOfDay)
        assertEquals(0, startZoned.hour)
        assertEquals(0, startZoned.minute)
        assertEquals(0, startZoned.second)

        val endZoned = DateTimeUtils.toZonedDateTime(endOfDay)
        assertEquals(23, endZoned.hour)
        assertEquals(59, endZoned.minute)
        assertEquals(59, endZoned.second)
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
        assertEquals(DayOfWeek.SUNDAY, endZoned.dayOfWeek)
        assertEquals(23, endZoned.hour)
        assertEquals(59, endZoned.minute)
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
        assertTrue(endZoned.dayOfMonth in 28..31)
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
        assertEquals(31, endZoned.dayOfMonth)
        assertEquals(12, endZoned.monthValue)
    }

    @Test
    fun testFormatTime() {
        val formattedAm = DateTimeUtils.formatTime(9, 15)
        assertTrue(formattedAm.contains("09:15") || formattedAm.contains("9:15"))
        assertTrue(formattedAm.contains("AM"))

        val formattedPm = DateTimeUtils.formatTime(21, 30)
        assertTrue(formattedPm.contains("09:30") || formattedPm.contains("9:30"))
        assertTrue(formattedPm.contains("PM"))
    }
}
