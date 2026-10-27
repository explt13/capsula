package com.capsula.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Converts dates coming from the Material 3 DatePicker into unlock timestamps. */
object UnlockTime {

    /** A capsule with a custom date opens at 09:00 local time on that day. */
    const val CUSTOM_UNLOCK_HOUR = 9

    /** DatePicker returns UTC midnight of the chosen day. */
    fun fromPickerDate(utcMidnightMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        pickerMillisToLocalDate(utcMidnightMillis)
            .atTime(CUSTOM_UNLOCK_HOUR, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    /** Only days after today can be chosen. */
    fun isSelectablePickerDate(utcMillis: Long, today: LocalDate = LocalDate.now()): Boolean =
        pickerMillisToLocalDate(utcMillis).isAfter(today)

    private fun pickerMillisToLocalDate(utcMillis: Long): LocalDate =
        Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
}
