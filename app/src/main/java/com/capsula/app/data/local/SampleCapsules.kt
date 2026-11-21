package com.capsula.app.data.local

import com.capsula.app.data.model.CapsuleEventType
import com.capsula.app.data.model.Mood
import com.capsula.app.domain.Durations.DAY
import com.capsula.app.domain.Durations.HOUR
import com.capsula.app.domain.Durations.MINUTE

/**
 * Демо-данные, которые записываются в базу ОДИН раз — при первом создании файла базы.
 * Дальше это обычные записи: их можно редактировать и удалять, после перезапуска они не вернутся.
 *
 * Покрывают все этапы жизненного цикла: черновик, готовая к вскрытию, откроется через 3 минуты,
 * вскрытая с ответом и историей откладывания, запечатанная на год.
 */
internal object SampleCapsules {

    suspend fun populate(dao: CapsuleDao, now: Long) {
        insert(
            dao,
            capsule(
                title = "Hello from last month",
                message = "Hi, future me. Right now I'm nervous about the new semester and I keep " +
                    "promising myself to start running in the mornings. If you're reading this, " +
                    "I hope at least one of those things got easier. Be kind to yourself either way.",
                question = "Did you actually start running?",
                mood = Mood.ANXIOUS,
                createdAt = now - 30 * DAY,
                sealedAt = now - 30 * DAY,
                unlockAt = now - 2 * HOUR,
            ),
            listOf(CapsuleEventType.SEALED to (now - 30 * DAY)),
        )
        insert(
            dao,
            capsule(
                title = "Right before the demo",
                message = "In a few minutes I present my project. Whatever happens: you built " +
                    "something that works, and you understand every line of it.",
                question = "How did the defense go?",
                mood = Mood.GOOD,
                createdAt = now - 10 * MINUTE,
                sealedAt = now - 10 * MINUTE,
                unlockAt = now + 3 * MINUTE,
            ),
            listOf(CapsuleEventType.SEALED to (now - 10 * MINUTE)),
        )
        insert(
            dao,
            capsule(
                title = "First week at university",
                message = "I don't know anyone yet and the campus feels huge. I'm writing this so " +
                    "that one day I can look back and laugh at how lost I felt.",
                question = "Who are your friends now?",
                mood = Mood.LOW,
                createdAt = now - 200 * DAY,
                sealedAt = now - 200 * DAY,
                unlockAt = now - 20 * DAY,
                postponeCount = 1,
                openedAt = now - 19 * DAY,
                moodAtOpening = Mood.GOOD,
                reflection = "I laughed. I have a whole group now, and the campus feels small.",
            ),
            listOf(
                CapsuleEventType.SEALED to (now - 200 * DAY),
                CapsuleEventType.POSTPONED to (now - 50 * DAY),
                CapsuleEventType.OPENED to (now - 19 * DAY),
                CapsuleEventType.REPLIED to (now - 19 * DAY + 5 * MINUTE),
            ),
        )
        insert(
            dao,
            capsule(
                title = "Graduation day",
                message = "If you're reading this, you made it. Remember the nights before deadlines? " +
                    "Worth it. Now go call Mom.",
                question = null,
                mood = Mood.GREAT,
                createdAt = now - 5 * DAY,
                sealedAt = now - 5 * DAY,
                unlockAt = now + 365 * DAY,
            ),
            listOf(CapsuleEventType.SEALED to (now - 5 * DAY)),
        )
        insert(
            dao,
            capsule(
                title = "For my 21st birthday",
                message = "Dear me at 21, I still haven't decided what to wish for…",
                question = "Did the wish come true?",
                mood = null,
                createdAt = now - 2 * DAY,
                sealedAt = null,
                unlockAt = null,
            ),
            listOf(
                CapsuleEventType.DRAFT_CREATED to (now - 2 * DAY),
                CapsuleEventType.DRAFT_EDITED to (now - 1 * DAY),
            ),
        )
    }

    private suspend fun insert(dao: CapsuleDao, entity: CapsuleEntity, events: List<Pair<CapsuleEventType, Long>>) {
        val (firstType, firstTime) = events.first()
        val id = dao.insertWithEvent(entity, event(firstType, firstTime))
        events.drop(1).forEach { (type, time) -> dao.insertEvent(event(type, time).copy(capsuleId = id)) }
    }

    private fun event(type: CapsuleEventType, time: Long) = CapsuleEventEntity(
        capsuleId = 0,
        type = type,
        timestamp = time,
        details = when (type) {
            CapsuleEventType.POSTPONED -> "+1 month"
            CapsuleEventType.REPLIED -> "Feeling good 🙂"
            else -> null
        },
    )

    private fun capsule(
        title: String,
        message: String,
        question: String?,
        mood: Mood?,
        createdAt: Long,
        sealedAt: Long?,
        unlockAt: Long?,
        postponeCount: Int = 0,
        openedAt: Long? = null,
        moodAtOpening: Mood? = null,
        reflection: String? = null,
    ) = CapsuleEntity(
        title = title,
        message = message,
        questionForFuture = question,
        moodAtSealing = mood,
        createdAt = createdAt,
        updatedAt = openedAt ?: sealedAt ?: createdAt,
        sealedAt = sealedAt,
        unlockAt = unlockAt,
        openedAt = openedAt,
        moodAtOpening = moodAtOpening,
        reflection = reflection,
        postponeCount = postponeCount,
        trashedAt = null,
    )
}
