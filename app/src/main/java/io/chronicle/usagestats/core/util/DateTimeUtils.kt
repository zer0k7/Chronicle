package io.chronicle.usagestats.core.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object DateTimeUtils {

    val IST_ZONE_ID: ZoneId = ZoneId.of("Asia/Kolkata")

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    private val SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)
    private val MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy", Locale.ENGLISH)
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
    private val TIME_24_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

    fun nowInIst(): ZonedDateTime {
        return ZonedDateTime.now(IST_ZONE_ID)
    }

    fun toZonedDateTime(epochMillis: Long): ZonedDateTime {
        return Instant.ofEpochMilli(epochMillis).atZone(IST_ZONE_ID)
    }

    fun getStartOfDay(epochMillis: Long = System.currentTimeMillis()): Long {
        val zonedDateTime = toZonedDateTime(epochMillis)
        return zonedDateTime.toLocalDate().atStartOfDay(IST_ZONE_ID).toInstant().toEpochMilli()
    }

    fun getEndOfDay(epochMillis: Long = System.currentTimeMillis()): Long {
        val zonedDateTime = toZonedDateTime(epochMillis)
        return zonedDateTime.toLocalDate().atTime(LocalTime.MAX).atZone(IST_ZONE_ID).toInstant().toEpochMilli()
    }

    fun getStartOfWeek(epochMillis: Long = System.currentTimeMillis()): Long {
        val zonedDateTime = toZonedDateTime(epochMillis)
        val monday = zonedDateTime.toLocalDate().`with`(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday.atStartOfDay(IST_ZONE_ID).toInstant().toEpochMilli()
    }

    fun getEndOfWeek(epochMillis: Long = System.currentTimeMillis()): Long {
        val zonedDateTime = toZonedDateTime(epochMillis)
        val sunday = zonedDateTime.toLocalDate().`with`(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        return sunday.atTime(LocalTime.MAX).atZone(IST_ZONE_ID).toInstant().toEpochMilli()
    }

    fun getStartOfMonth(epochMillis: Long = System.currentTimeMillis()): Long {
        val zonedDateTime = toZonedDateTime(epochMillis)
        val firstDay = zonedDateTime.toLocalDate().`with`(TemporalAdjusters.firstDayOfMonth())
        return firstDay.atStartOfDay(IST_ZONE_ID).toInstant().toEpochMilli()
    }

    fun getEndOfMonth(epochMillis: Long = System.currentTimeMillis()): Long {
        val zonedDateTime = toZonedDateTime(epochMillis)
        val lastDay = zonedDateTime.toLocalDate().`with`(TemporalAdjusters.lastDayOfMonth())
        return lastDay.atTime(LocalTime.MAX).atZone(IST_ZONE_ID).toInstant().toEpochMilli()
    }

    fun getStartOfYear(epochMillis: Long = System.currentTimeMillis()): Long {
        val zonedDateTime = toZonedDateTime(epochMillis)
        val firstDayOfYear = zonedDateTime.toLocalDate().`with`(TemporalAdjusters.firstDayOfYear())
        return firstDayOfYear.atStartOfDay(IST_ZONE_ID).toInstant().toEpochMilli()
    }

    fun getEndOfYear(epochMillis: Long = System.currentTimeMillis()): Long {
        val zonedDateTime = toZonedDateTime(epochMillis)
        val lastDayOfYear = zonedDateTime.toLocalDate().`with`(TemporalAdjusters.lastDayOfYear())
        return lastDayOfYear.atTime(LocalTime.MAX).atZone(IST_ZONE_ID).toInstant().toEpochMilli()
    }

    fun formatDuration(totalMillis: Long): String {
        if (totalMillis <= 0) return "0m"
        val totalSeconds = totalMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }

    fun formatDurationDetailed(totalMillis: Long): String {
        val totalSeconds = totalMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.ENGLISH, "%02dh %02dm %02ds", hours, minutes, seconds)
        } else {
            String.format(Locale.ENGLISH, "%02dm %02ds", minutes, seconds)
        }
    }

    fun formatDate(epochMillis: Long): String {
        return toZonedDateTime(epochMillis).format(DATE_FORMATTER)
    }

    fun formatShortDate(epochMillis: Long): String {
        return toZonedDateTime(epochMillis).format(SHORT_DATE_FORMATTER)
    }

    fun formatMonthYear(epochMillis: Long): String {
        return toZonedDateTime(epochMillis).format(MONTH_YEAR_FORMATTER)
    }

    fun formatYear(epochMillis: Long): String {
        return toZonedDateTime(epochMillis).format(YEAR_FORMATTER)
    }

    fun formatTime(hour: Int, minute: Int): String {
        val time = LocalTime.of(hour, minute)
        return time.format(TIME_FORMATTER)
    }

    fun formatTime24(hour: Int, minute: Int): String {
        val time = LocalTime.of(hour, minute)
        return time.format(TIME_24_FORMATTER)
    }

    fun formatDateRange(startMillis: Long, endMillis: Long): String {
        val start = toZonedDateTime(startMillis)
        val end = toZonedDateTime(endMillis)
        return if (start.toLocalDate() == end.toLocalDate()) {
            start.format(DATE_FORMATTER)
        } else {
            "${start.format(SHORT_DATE_FORMATTER)} - ${end.format(DATE_FORMATTER)}"
        }
    }

    fun getDaysInRange(startMillis: Long, endMillis: Long): List<LocalDate> {
        val startDate = toZonedDateTime(startMillis).toLocalDate()
        val endDate = toZonedDateTime(endMillis).toLocalDate()
        val days = mutableListOf<LocalDate>()
        var current = startDate
        while (!current.isAfter(endDate)) {
            days.add(current)
            current = current.plusDays(1)
        }
        return days
    }
}
