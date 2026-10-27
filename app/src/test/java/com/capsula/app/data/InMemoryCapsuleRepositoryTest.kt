package com.capsula.app.data

import com.capsula.app.data.model.CapsuleStatus
import com.capsula.app.data.model.Mood
import com.capsula.app.data.model.NewCapsule
import com.capsula.app.data.repository.InMemoryCapsuleRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryCapsuleRepositoryTest {

    private var now = 1_000L
    private val repository = InMemoryCapsuleRepository(timeProvider = { now })

    private val newCapsule = NewCapsule(
        title = "  Hello  ",
        message = "A letter to the future",
        questionForFuture = "   ",
        moodAtSealing = Mood.GOOD,
        unlockAt = 5_000L,
    )

    @Test
    fun `added capsule is trimmed and sealed`() = runTest {
        val id = repository.addCapsule(newCapsule)
        val capsule = repository.observeCapsule(id).first()!!

        assertEquals("Hello", capsule.title)
        assertNull(capsule.questionForFuture)
        assertEquals(CapsuleStatus.SEALED, capsule.statusAt(now))
    }

    @Test
    fun `capsule goes sealed, ready, opened and keeps the reflection`() = runTest {
        val id = repository.addCapsule(newCapsule)

        now = 6_000L
        assertEquals(CapsuleStatus.READY, repository.observeCapsule(id).first()!!.statusAt(now))

        repository.markOpened(id, openedAt = now)
        repository.saveReflection(id, Mood.GREAT, "Thanks, past me")
        val opened = repository.observeCapsule(id).first()!!

        assertEquals(CapsuleStatus.OPENED, opened.statusAt(now))
        assertEquals(Mood.GREAT, opened.moodAtOpening)
        assertEquals("Thanks, past me", opened.reflection)
    }

    @Test
    fun `deleted capsule disappears`() = runTest {
        val id = repository.addCapsule(newCapsule)
        repository.deleteCapsule(id)

        assertNull(repository.observeCapsule(id).first())
        assertTrue(repository.capsules.first().isEmpty())
    }
}
