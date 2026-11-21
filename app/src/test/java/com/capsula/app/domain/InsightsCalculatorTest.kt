package com.capsula.app.domain

import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.Mood
import org.junit.Assert.assertEquals
import org.junit.Test

class InsightsCalculatorTest {

    private val now = 100 * Durations.DAY

    private fun capsule(
        id: Long,
        mood: Mood?,
        sealedAt: Long?,
        unlockAt: Long?,
        openedAt: Long? = null,
        moodAtOpening: Mood? = null,
        postponeCount: Int = 0,
    ) = Capsule(
        id = id, title = "t$id", message = "message", questionForFuture = null, moodAtSealing = mood,
        createdAt = 0, updatedAt = 0, sealedAt = sealedAt, unlockAt = unlockAt,
        openedAt = openedAt, moodAtOpening = moodAtOpening, postponeCount = postponeCount,
    )

    @Test
    fun `counts every lifecycle stage, mood shifts, postpones and the next unlock`() {
        val capsules = listOf(
            capsule(1, null, sealedAt = null, unlockAt = null),
            capsule(2, Mood.OKAY, sealedAt = 0, unlockAt = now + Durations.HOUR, postponeCount = 2),
            capsule(3, Mood.ANXIOUS, sealedAt = 0, unlockAt = now - 1),
            capsule(4, Mood.LOW, sealedAt = 0, unlockAt = 10, openedAt = 10 * Durations.DAY, moodAtOpening = Mood.GREAT),
            capsule(5, Mood.GOOD, sealedAt = 0, unlockAt = 10, openedAt = 20 * Durations.DAY, moodAtOpening = Mood.LOW, postponeCount = 1),
        )

        val insights = InsightsCalculator.calculate(capsules, now)

        assertEquals(5, insights.total)
        assertEquals(1, insights.drafts)
        assertEquals(1, insights.sealed)
        assertEquals(1, insights.ready)
        assertEquals(2, insights.opened)
        assertEquals(3, insights.totalPostpones)
        assertEquals(2L, insights.nextUnlock?.id)
        assertEquals(1, insights.moodShifts[MoodShift.IMPROVED])
        assertEquals(1, insights.moodShifts[MoodShift.WORSE])
        assertEquals(15 * Durations.DAY, insights.averageWaitMillis)
        // Черновик без настроения не попадает в график настроений.
        assertEquals(4, insights.sealingMoods.values.sum())
    }
}
