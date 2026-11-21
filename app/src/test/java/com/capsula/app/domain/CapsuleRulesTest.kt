package com.capsula.app.domain

import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.CapsuleStatus
import com.capsula.app.data.model.Mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapsuleRulesTest {

    private val now = 100 * Durations.DAY

    private val draft = Capsule(
        id = 1, title = "Draft", message = "", questionForFuture = null, moodAtSealing = null,
        createdAt = now - Durations.DAY, updatedAt = now - Durations.DAY, sealedAt = null, unlockAt = null,
    )
    private val sealed = draft.copy(moodAtSealing = Mood.GOOD, sealedAt = now - Durations.DAY, unlockAt = now + Durations.DAY)

    @Test
    fun `status follows the lifecycle draft, sealed, ready, opened`() {
        assertEquals(CapsuleStatus.DRAFT, draft.statusAt(now))
        assertEquals(CapsuleStatus.SEALED, sealed.statusAt(now))
        assertEquals(CapsuleStatus.READY, sealed.statusAt(now + 2 * Durations.DAY))
        assertEquals(CapsuleStatus.OPENED, sealed.copy(openedAt = now).statusAt(now))
    }

    @Test
    fun `only drafts can be edited, only sealed capsules renamed`() {
        assertTrue(CapsuleRules.canEditContent(draft, now))
        assertFalse(CapsuleRules.canEditContent(sealed, now))
        assertFalse(CapsuleRules.canRename(draft, now))
        assertTrue(CapsuleRules.canRename(sealed, now))
    }

    @Test
    fun `postponing moves the opening later and is limited`() {
        assertTrue(CapsuleRules.canPostpone(sealed, now))
        assertEquals(now + Durations.DAY + 7 * Durations.DAY, CapsuleRules.postponedUnlockAt(sealed, now, PostponeOption.ONE_WEEK))

        val exhausted = sealed.copy(postponeCount = CapsuleRules.MAX_POSTPONES)
        assertFalse(CapsuleRules.canPostpone(exhausted, now))
        assertFalse(CapsuleRules.canPostpone(draft, now))
        assertFalse(CapsuleRules.canPostpone(sealed.copy(openedAt = now), now))
    }

    @Test
    fun `a ready capsule postponed from now becomes sealed again`() {
        val later = now + 2 * Durations.DAY
        val newUnlockAt = CapsuleRules.postponedUnlockAt(sealed, later, PostponeOption.ONE_DAY)
        assertEquals(later + Durations.DAY, newUnlockAt)
        assertEquals(CapsuleStatus.SEALED, sealed.copy(unlockAt = newUnlockAt).statusAt(later))
    }

    @Test
    fun `trashed capsules allow no actions`() {
        val trashed = sealed.copy(trashedAt = now)
        assertFalse(CapsuleRules.canRename(trashed, now))
        assertFalse(CapsuleRules.canPostpone(trashed, now))
        assertFalse(CapsuleRules.canOpen(trashed.copy(unlockAt = now - 1), now))
    }
}
