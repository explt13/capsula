package com.capsula.app.data

import com.capsula.app.data.model.CapsuleContent
import com.capsula.app.data.model.CapsuleEventType
import com.capsula.app.data.model.CapsuleStatus
import com.capsula.app.data.model.Mood
import com.capsula.app.data.repository.OfflineCapsuleRepository
import com.capsula.app.domain.Durations
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineCapsuleRepositoryTest {

    private var now = 1_000_000L
    private val dao = FakeCapsuleDao()
    private val repository = OfflineCapsuleRepository(dao = dao, timeProvider = { now })

    private val content = CapsuleContent(
        title = "  Hello  ",
        message = "A letter to the future",
        questionForFuture = "   ",
        mood = Mood.GOOD,
    )

    private suspend fun historyOf(id: Long) = repository.observeHistory(id).first().map { it.type }

    @Test
    fun `a draft is trimmed, editable and can be sealed later`() = runTest {
        val id = repository.saveDraft(null, content.copy(mood = null))
        val draft = repository.getCapsule(id)!!
        assertEquals("Hello", draft.title)
        assertNull(draft.questionForFuture)
        assertEquals(CapsuleStatus.DRAFT, draft.statusAt(now))

        now += Durations.MINUTE
        repository.saveDraft(id, content.copy(title = "Hello again"))
        repository.seal(id, content, unlockAt = now + Durations.DAY)

        val sealed = repository.getCapsule(id)!!
        assertEquals(CapsuleStatus.SEALED, sealed.statusAt(now))
        assertEquals(now, sealed.sealedAt)
        assertEquals(
            listOf(CapsuleEventType.DRAFT_CREATED, CapsuleEventType.DRAFT_EDITED, CapsuleEventType.SEALED),
            historyOf(id),
        )
    }

    @Test
    fun `postpone, open and reply are recorded in history`() = runTest {
        val id = repository.seal(null, content, unlockAt = now + Durations.HOUR)
        repository.postpone(id, now + Durations.DAY, "+1 day")
        assertEquals(1, repository.getCapsule(id)!!.postponeCount)

        now += 2 * Durations.DAY
        repository.markOpened(id, now)
        repository.saveReflection(id, Mood.GREAT, "Thanks, past me")
        repository.saveReflection(id, Mood.GOOD, "Edited")

        val opened = repository.getCapsule(id)!!
        assertEquals(CapsuleStatus.OPENED, opened.statusAt(now))
        assertEquals("Edited", opened.reflection)
        assertEquals(
            listOf(
                CapsuleEventType.SEALED,
                CapsuleEventType.POSTPONED,
                CapsuleEventType.OPENED,
                CapsuleEventType.REPLIED,
                CapsuleEventType.REPLY_EDITED,
            ),
            historyOf(id),
        )
    }

    @Test
    fun `trash can be undone and emptying it erases capsule and history`() = runTest {
        val id = repository.seal(null, content, unlockAt = now + Durations.HOUR)

        repository.moveToTrash(id)
        assertTrue(repository.capsules.first().isEmpty())
        assertEquals(1, repository.trashedCapsules.first().size)

        repository.restoreFromTrash(id)
        assertEquals(1, repository.capsules.first().size)
        assertEquals(CapsuleEventType.RESTORED, historyOf(id).last())

        repository.moveToTrash(id)
        assertEquals(1, repository.emptyTrash())
        assertNull(repository.getCapsule(id))
        assertTrue(historyOf(id).isEmpty())
    }
}
