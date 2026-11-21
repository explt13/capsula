package com.capsula.app.data

import com.capsula.app.data.local.ActivityRow
import com.capsula.app.data.local.CapsuleDao
import com.capsula.app.data.local.CapsuleEntity
import com.capsula.app.data.local.CapsuleEventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * DAO в памяти для unit-тестов репозитория без эмулятора.
 * Методы с @Transaction (insertWithEvent, updateWithEvent) наследуются от настоящего CapsuleDao.
 * Каскадное удаление истории повторяет поведение внешнего ключа в Room.
 */
class FakeCapsuleDao : CapsuleDao() {
    val capsules = MutableStateFlow<List<CapsuleEntity>>(emptyList())
    val events = MutableStateFlow<List<CapsuleEventEntity>>(emptyList())
    private var nextCapsuleId = 1L
    private var nextEventId = 1L

    override fun observeActive(): Flow<List<CapsuleEntity>> =
        capsules.map { list -> list.filter { it.trashedAt == null }.sortedByDescending { it.createdAt } }

    override fun observeTrashed(): Flow<List<CapsuleEntity>> =
        capsules.map { list -> list.filter { it.trashedAt != null }.sortedByDescending { it.trashedAt } }

    override fun observeById(id: Long): Flow<CapsuleEntity?> = capsules.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getById(id: Long): CapsuleEntity? = capsules.value.firstOrNull { it.id == id }

    override fun observeEvents(capsuleId: Long): Flow<List<CapsuleEventEntity>> =
        events.map { list -> list.filter { it.capsuleId == capsuleId }.sortedWith(compareBy({ it.timestamp }, { it.id })) }

    override fun observeRecentActivity(limit: Int): Flow<List<ActivityRow>> =
        combine(capsules, events) { caps, evs ->
            evs.sortedWith(compareByDescending<CapsuleEventEntity> { it.timestamp }.thenByDescending { it.id })
                .mapNotNull { e -> caps.firstOrNull { it.id == e.capsuleId }?.let { c -> ActivityRow(e, c.title, c.trashedAt) } }
                .take(limit)
        }

    override suspend fun insertCapsule(capsule: CapsuleEntity): Long {
        val id = nextCapsuleId++
        capsules.value = capsules.value + capsule.copy(id = id)
        return id
    }

    override suspend fun updateCapsule(capsule: CapsuleEntity) {
        capsules.value = capsules.value.map { if (it.id == capsule.id) capsule else it }
    }

    override suspend fun insertEvent(event: CapsuleEventEntity): Long {
        val id = nextEventId++
        events.value = events.value + event.copy(id = id)
        return id
    }

    override suspend fun deleteCapsule(id: Long) {
        capsules.value = capsules.value.filterNot { it.id == id }
        events.value = events.value.filterNot { it.capsuleId == id }
    }

    override suspend fun deleteAllTrashed(): Int {
        val trashedIds = capsules.value.filter { it.trashedAt != null }.map { it.id }.toSet()
        capsules.value = capsules.value.filterNot { it.id in trashedIds }
        events.value = events.value.filterNot { it.capsuleId in trashedIds }
        return trashedIds.size
    }
}
