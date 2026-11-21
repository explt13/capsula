package com.capsula.app.data.local

import androidx.room.TypeConverter
import com.capsula.app.data.model.CapsuleEventType
import com.capsula.app.data.model.Mood

// SQLite хранит только числа и строки. Конвертеры говорят Room, как сохранять enum-ы (по имени константы).
class Converters {

    @TypeConverter
    fun moodToString(mood: Mood?): String? = mood?.name

    @TypeConverter
    fun stringToMood(value: String?): Mood? = value?.let { name -> Mood.entries.firstOrNull { it.name == name } }

    @TypeConverter
    fun eventTypeToString(type: CapsuleEventType): String = type.name

    @TypeConverter
    fun stringToEventType(value: String): CapsuleEventType = CapsuleEventType.valueOf(value)
}
