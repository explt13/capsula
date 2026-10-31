package com.capsula.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/// Класс для работы с временем открытия капсулы.
object UnlockTime {

    // Кастомное время открытия капсулы в 9 утра.
    const val CUSTOM_UNLOCK_HOUR = 9

    // Преобразует дату из Material 3 DatePicker в время открытия капсулы.
    fun fromPickerDate(utcMidnightMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        pickerMillisToLocalDate(utcMidnightMillis)
            .atTime(CUSTOM_UNLOCK_HOUR, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    // Проверяет, является ли выбранная дата в DatePicker допустимой для открытия капсулы (т.е. после сегодняшнего дня).
    fun isSelectablePickerDate(utcMillis: Long, today: LocalDate = LocalDate.now()): Boolean =
        pickerMillisToLocalDate(utcMillis).isAfter(today)

    private fun pickerMillisToLocalDate(utcMillis: Long): LocalDate =
        Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
}
