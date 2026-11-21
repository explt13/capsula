package com.capsula.app.data.repository

import com.capsula.app.data.model.ActivityItem
import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.CapsuleContent
import com.capsula.app.data.model.CapsuleEvent
import com.capsula.app.data.model.Mood
import kotlinx.coroutines.flow.Flow

// Интерфейс репозитория капсул: единственная точка доступа к данным для ViewModel.
// ViewModel не знает, что внутри Room, — только этот контракт.
interface CapsuleRepository {

    // Капсулы вне корзины. Flow обновляется сам при любом изменении в базе.
    val capsules: Flow<List<Capsule>>

    val trashedCapsules: Flow<List<Capsule>>

    fun observeCapsule(id: Long): Flow<Capsule?>

    fun observeHistory(capsuleId: Long): Flow<List<CapsuleEvent>>

    fun observeRecentActivity(limit: Int): Flow<List<ActivityItem>>

    suspend fun getCapsule(id: Long): Capsule?

    // Создаёт новый черновик (id == null) или обновляет существующий. Возвращает id черновика.
    suspend fun saveDraft(id: Long?, content: CapsuleContent): Long

    // Запечатывает новую капсулу (id == null) или существующий черновик. Возвращает id капсулы.
    suspend fun seal(id: Long?, content: CapsuleContent, unlockAt: Long): Long

    suspend fun rename(id: Long, newTitle: String)

    suspend fun postpone(id: Long, newUnlockAt: Long, label: String)

    suspend fun markOpened(id: Long, openedAt: Long)

    suspend fun saveReflection(id: Long, mood: Mood, reflection: String)

    // Мягкое удаление: капсула уходит в корзину вместе с историей и может быть восстановлена.
    suspend fun moveToTrash(id: Long)

    suspend fun restoreFromTrash(id: Long)

    // Окончательное удаление: строка капсулы и её история удаляются из базы.
    suspend fun deletePermanently(id: Long)

    suspend fun emptyTrash(): Int
}
