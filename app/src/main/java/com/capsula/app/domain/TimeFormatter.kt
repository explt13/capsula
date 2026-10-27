package com.capsula.app.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFormatter {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)
    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

    /**
     * Live countdown text: "04:59", "02:15:09" or "12d 03h 10m 05s".
     * Seconds are rounded up so the timer never shows 00:00 while the capsule is still sealed.
     */
    fun countdown(remainingMillis: Long): String {
        if (remainingMillis <= 0L) return "00:00"
        val totalSeconds = (remainingMillis + 999) / 1_000
        val days = totalSeconds / 86_400
        val hours = totalSeconds % 86_400 / 3_600
        val minutes = totalSeconds % 3_600 / 60
        val seconds = totalSeconds % 60
        return when {
            days > 0 -> String.format(Locale.US, "%dd %02dh %02dm %02ds", days, hours, minutes, seconds)
            hours > 0 -> String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            else -> String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /** Rough, human friendly duration: "3 days", "1 month", "2 years". */
    fun humanDuration(millis: Long): String {
        val safe = millis.coerceAtLeast(0L)
        val minutes = safe / Durations.MINUTE
        val hours = safe / Durations.HOUR
        val days = safe / Durations.DAY
        return when {
            days >= 365 -> plural(days / 365, "year")
            days >= 30 -> plural(days / 30, "month")
            days >= 1 -> plural(days, "day")
            hours >= 1 -> plural(hours, "hour")
            minutes >= 1 -> plural(minutes, "minute")
            else -> "less than a minute"
        }
    }

    fun dateTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        dateTimeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

    fun date(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        dateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

    private fun plural(count: Long, unit: String): String =
        if (count == 1L) "1 $unit" else "$count ${unit}s"
}
