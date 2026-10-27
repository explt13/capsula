package com.capsula.app.data.repository

import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.Mood
import com.capsula.app.domain.Durations.DAY
import com.capsula.app.domain.Durations.HOUR
import com.capsula.app.domain.Durations.MINUTE
import java.util.concurrent.atomic.AtomicLong

/**
 * Demo data so that every state of the app can be shown right after install:
 * one capsule ready to open, one that opens in a few minutes (watch it unlock live),
 * one already opened with a reflection, and one sealed for a year.
 */
internal object SampleCapsules {

    fun create(now: Long, ids: AtomicLong): List<Capsule> = listOf(
        Capsule(
            id = ids.getAndIncrement(),
            title = "Hello from last month",
            message = "Hi, future me. Right now I'm nervous about the new semester and I keep " +
                "promising myself to start running in the mornings. If you're reading this, " +
                "I hope at least one of those things got easier. Be kind to yourself either way.",
            questionForFuture = "Did you actually start running?",
            moodAtSealing = Mood.ANXIOUS,
            createdAt = now - 30 * DAY,
            unlockAt = now - 2 * HOUR,
        ),
        Capsule(
            id = ids.getAndIncrement(),
            title = "Right before the demo",
            message = "In a few minutes I present my project. Whatever happens: you built " +
                "something that works, and you understand every line of it.",
            questionForFuture = "How did the defense go?",
            moodAtSealing = Mood.GOOD,
            createdAt = now - 10 * MINUTE,
            unlockAt = now + 3 * MINUTE,
        ),
        Capsule(
            id = ids.getAndIncrement(),
            title = "First week at university",
            message = "I don't know anyone yet and the campus feels huge. I'm writing this so " +
                "that one day I can look back and laugh at how lost I felt.",
            questionForFuture = "Who are your friends now?",
            moodAtSealing = Mood.LOW,
            createdAt = now - 200 * DAY,
            unlockAt = now - 20 * DAY,
            openedAt = now - 19 * DAY,
            moodAtOpening = Mood.GOOD,
            reflection = "I laughed. I have a whole group now, and the campus feels small.",
        ),
        Capsule(
            id = ids.getAndIncrement(),
            title = "Graduation day",
            message = "If you're reading this, you made it. Remember the nights before deadlines? " +
                "Worth it. Now go call Mom.",
            questionForFuture = null,
            moodAtSealing = Mood.GREAT,
            createdAt = now - 5 * DAY,
            unlockAt = now + 365 * DAY,
        ),
    )
}
