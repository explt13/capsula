package com.capsula.app.data.repository

import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.Mood
import com.capsula.app.data.model.NewCapsule
import com.capsula.app.domain.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

/**
 * Keeps capsules in a [MutableStateFlow]. Every change produces a new immutable list,
 * which automatically reaches every screen that observes [capsules].
 *
 * Data lives as long as the app process (it survives rotation and navigation).
 * Persistent storage (Room) is planned for part 2.
 */
class InMemoryCapsuleRepository(
    private val timeProvider: TimeProvider,
    seedSampleData: Boolean = false,
) : CapsuleRepository {

    private val nextId = AtomicLong(1)
    private val state = MutableStateFlow<List<Capsule>>(emptyList())

    override val capsules: Flow<List<Capsule>> = state.asStateFlow()

    init {
        if (seedSampleData) {
            state.value = SampleCapsules.create(now = timeProvider.now(), ids = nextId)
        }
    }

    override fun observeCapsule(id: Long): Flow<Capsule?> =
        state.map { list -> list.firstOrNull { it.id == id } }.distinctUntilChanged()

    override suspend fun addCapsule(newCapsule: NewCapsule): Long {
        val id = nextId.getAndIncrement()
        val capsule = Capsule(
            id = id,
            title = newCapsule.title.trim(),
            message = newCapsule.message.trim(),
            questionForFuture = newCapsule.questionForFuture?.trim()?.ifBlank { null },
            moodAtSealing = newCapsule.moodAtSealing,
            createdAt = timeProvider.now(),
            unlockAt = newCapsule.unlockAt,
        )
        state.update { it + capsule }
        return id
    }

    override suspend fun markOpened(id: Long, openedAt: Long) = updateCapsule(id) { capsule ->
        if (capsule.openedAt == null) capsule.copy(openedAt = openedAt) else capsule
    }

    override suspend fun saveReflection(id: Long, mood: Mood, reflection: String) = updateCapsule(id) {
        it.copy(moodAtOpening = mood, reflection = reflection.trim().ifBlank { null })
    }

    override suspend fun deleteCapsule(id: Long) {
        state.update { list -> list.filterNot { it.id == id } }
    }

    private fun updateCapsule(id: Long, transform: (Capsule) -> Capsule) {
        state.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }
}
