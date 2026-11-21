package com.capsula.app.data.repository

import com.capsula.app.data.local.CapsuleDao
import com.capsula.app.data.local.CapsuleEntity
import com.capsula.app.data.local.CapsuleEventEntity
import com.capsula.app.data.local.toDomain
import com.capsula.app.data.model.ActivityItem
import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.CapsuleContent
import com.capsula.app.data.model.CapsuleEvent
import com.capsula.app.data.model.CapsuleEventType
import com.capsula.app.data.model.Mood
import com.capsula.app.domain.TimeFormatter
import com.capsula.app.domain.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Реализация репозитория поверх Room (данные хранятся только на устройстве, отсюда «Offline»).
 *
 * Каждое изменение капсулы сопровождается записью в историю (capsule_events),
 * и обе записи делаются в одной транзакции через методы DAO с @Transaction.
 */
class OfflineCapsuleRepository(
    private val dao: CapsuleDao,
    private val timeProvider: TimeProvider,
) : CapsuleRepository {

    override val capsules: Flow<List<Capsule>> =
        dao.observeActive().map { rows -> rows.map { it.toDomain() } }

    override val trashedCapsules: Flow<List<Capsule>> =
        dao.observeTrashed().map { rows -> rows.map { it.toDomain() } }

    override fun observeCapsule(id: Long): Flow<Capsule?> =
        dao.observeById(id).map { it?.toDomain() }

    override fun observeHistory(capsuleId: Long): Flow<List<CapsuleEvent>> =
        dao.observeEvents(capsuleId).map { rows -> rows.map { it.toDomain() } }

    override fun observeRecentActivity(limit: Int): Flow<List<ActivityItem>> =
        dao.observeRecentActivity(limit).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getCapsule(id: Long): Capsule? = dao.getById(id)?.toDomain()

    override suspend fun saveDraft(id: Long?, content: CapsuleContent): Long {
        val now = timeProvider.now()
        if (id == null) {
            val entity = newEntity(content, now).copy(sealedAt = null, unlockAt = null)
            return dao.insertWithEvent(entity, event(0, CapsuleEventType.DRAFT_CREATED, now))
        }
        val existing = requireDraft(id)
        dao.updateWithEvent(
            existing.withContent(content).copy(updatedAt = now),
            event(id, CapsuleEventType.DRAFT_EDITED, now),
        )
        return id
    }

    override suspend fun seal(id: Long?, content: CapsuleContent, unlockAt: Long): Long {
        val now = timeProvider.now()
        val details = "Opens on ${TimeFormatter.dateTime(unlockAt)}"
        if (id == null) {
            val entity = newEntity(content, now).copy(sealedAt = now, unlockAt = unlockAt)
            return dao.insertWithEvent(entity, event(0, CapsuleEventType.SEALED, now, details))
        }
        val existing = requireDraft(id)
        dao.updateWithEvent(
            existing.withContent(content).copy(sealedAt = now, unlockAt = unlockAt, updatedAt = now),
            event(id, CapsuleEventType.SEALED, now, details),
        )
        return id
    }

    override suspend fun rename(id: Long, newTitle: String) {
        val existing = dao.getById(id) ?: return
        val title = newTitle.trim()
        if (title == existing.title) return
        val now = timeProvider.now()
        dao.updateWithEvent(
            existing.copy(title = title, updatedAt = now),
            event(id, CapsuleEventType.RENAMED, now, "“${existing.title}” → “$title”"),
        )
    }

    override suspend fun postpone(id: Long, newUnlockAt: Long, label: String) {
        val existing = dao.getById(id) ?: return
        val now = timeProvider.now()
        dao.updateWithEvent(
            existing.copy(
                unlockAt = newUnlockAt,
                postponeCount = existing.postponeCount + 1,
                updatedAt = now,
            ),
            event(id, CapsuleEventType.POSTPONED, now, "$label, now opens on ${TimeFormatter.dateTime(newUnlockAt)}"),
        )
    }

    override suspend fun markOpened(id: Long, openedAt: Long) {
        val existing = dao.getById(id) ?: return
        if (existing.openedAt != null) return
        dao.updateWithEvent(
            existing.copy(openedAt = openedAt, updatedAt = openedAt),
            event(id, CapsuleEventType.OPENED, openedAt),
        )
    }

    override suspend fun saveReflection(id: Long, mood: Mood, reflection: String) {
        val existing = dao.getById(id) ?: return
        val now = timeProvider.now()
        val type = if (existing.moodAtOpening == null) CapsuleEventType.REPLIED else CapsuleEventType.REPLY_EDITED
        dao.updateWithEvent(
            existing.copy(
                moodAtOpening = mood,
                reflection = reflection.trim().ifBlank { null },
                updatedAt = now,
            ),
            event(id, type, now, "Feeling ${mood.label.lowercase()} ${mood.emoji}"),
        )
    }

    override suspend fun moveToTrash(id: Long) {
        val existing = dao.getById(id) ?: return
        if (existing.trashedAt != null) return
        val now = timeProvider.now()
        dao.updateWithEvent(existing.copy(trashedAt = now), event(id, CapsuleEventType.TRASHED, now))
    }

    override suspend fun restoreFromTrash(id: Long) {
        val existing = dao.getById(id) ?: return
        if (existing.trashedAt == null) return
        val now = timeProvider.now()
        dao.updateWithEvent(existing.copy(trashedAt = null), event(id, CapsuleEventType.RESTORED, now))
    }

    override suspend fun deletePermanently(id: Long) = dao.deleteCapsule(id)

    override suspend fun emptyTrash(): Int = dao.deleteAllTrashed()

    private suspend fun requireDraft(id: Long): CapsuleEntity {
        val existing = checkNotNull(dao.getById(id)) { "Capsule $id does not exist" }
        check(existing.sealedAt == null) { "Capsule $id is sealed and can't be edited" }
        return existing
    }

    private fun newEntity(content: CapsuleContent, now: Long) = CapsuleEntity(
        title = content.title.trim(),
        message = content.message.trim(),
        questionForFuture = content.questionForFuture?.trim()?.ifBlank { null },
        moodAtSealing = content.mood,
        createdAt = now,
        updatedAt = now,
        sealedAt = null,
        unlockAt = null,
        openedAt = null,
        moodAtOpening = null,
        reflection = null,
        postponeCount = 0,
        trashedAt = null,
    )

    private fun CapsuleEntity.withContent(content: CapsuleContent) = copy(
        title = content.title.trim(),
        message = content.message.trim(),
        questionForFuture = content.questionForFuture?.trim()?.ifBlank { null },
        moodAtSealing = content.mood,
    )

    private fun event(capsuleId: Long, type: CapsuleEventType, timestamp: Long, details: String? = null) =
        CapsuleEventEntity(capsuleId = capsuleId, type = type, timestamp = timestamp, details = details)
}
