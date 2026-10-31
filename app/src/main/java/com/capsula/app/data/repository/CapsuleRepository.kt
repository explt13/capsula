package com.capsula.app.data.repository

import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.Mood
import com.capsula.app.data.model.NewCapsule
import kotlinx.coroutines.flow.Flow

// Интерфейс для репозитория капсул. Определяет методы для наблюдения, добавления, открытия, сохранения размышлений и удаления капсул.
interface CapsuleRepository {
    val capsules: Flow<List<Capsule>>

    fun observeCapsule(id: Long): Flow<Capsule?>

    suspend fun addCapsule(newCapsule: NewCapsule): Long

    suspend fun markOpened(id: Long, openedAt: Long)

    suspend fun saveReflection(id: Long, mood: Mood, reflection: String)

    suspend fun deleteCapsule(id: Long)
}
