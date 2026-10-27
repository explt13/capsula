package com.capsula.app.domain

import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.Mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InsightsCalculatorTest {

    private val now = 100 * Durations.DAY

    private fun capsule(
        id: Long,
        mood: Mood,
        createdAt: Long,
        unlockAt: Long,
        openedAt: Long? = null,
        moodAtOpening: Mood? = null,
    ) = Capsule(id, "t$id", "message", null, mood, createdAt, unlockAt, openedAt, moodAtOpening)

    @Test
    fun `empty list gives empty insights`() {
        val insights = InsightsCalculator.calculate(emptyList(), now)
        assertEquals(0, insights.total)
        assertNull(insights.nextUnlock)
        assertNull(insights.averageWaitMillis)
    }

    @Test
    fun `counts statuses, mood shifts and finds the next unlock`() {
        val capsules = listOf(
            capsule(1, Mood.LOW, createdAt = 0, unlockAt = now + Durations.DAY),
            capsule(2, Mood.OKAY, createdAt = 0, unlockAt = now + Durations.HOUR),
            capsule(3, Mood.ANXIOUS, createdAt = 0, unlockAt = now - 1),
            capsule(4, Mood.LOW, createdAt = 0, unlockAt = 10, openedAt = 10 * Durations.DAY, moodAtOpening = Mood.GREAT),
            capsule(5, Mood.GOOD, createdAt = 0, unlockAt = 10, openedAt = 20 * Durations.DAY, moodAtOpening = Mood.LOW),
        )

        val insights = InsightsCalculator.calculate(capsules, now)

        assertEquals(5, insights.total)
        assertEquals(2, insights.sealed)
        assertEquals(1, insights.ready)
        assertEquals(2, insights.opened)
        assertEquals(2L, insights.nextUnlock?.id)
        assertEquals(Durations.HOUR, insights.nextUnlockInMillis)
        assertEquals(2, insights.sealingMoods[Mood.LOW])
        assertEquals(1, insights.moodShifts[MoodShift.IMPROVED])
        assertEquals(1, insights.moodShifts[MoodShift.WORSE])
        assertEquals(0, insights.moodShifts[MoodShift.SAME])
        assertEquals(15 * Durations.DAY, insights.averageWaitMillis)
        assertEquals(20 * Durations.DAY, insights.longestWaitMillis)
    }
}
