package com.capsula.app.domain

import com.capsula.app.data.model.Mood

data class CapsuleValidation(
    val titleError: String? = null,
    val messageError: String? = null,
    val moodError: String? = null,
    val unlockError: String? = null,
) {
    val isValid: Boolean
        get() = listOf(titleError, messageError, moodError, unlockError).all { it == null }
}

// Валидатор для проверки данных новой капсулы.
object CapsuleValidator {
    const val TITLE_MAX_LENGTH = 60
    const val MESSAGE_MIN_LENGTH = 10
    const val MESSAGE_MAX_LENGTH = 2_000

    fun validate(
        title: String,
        message: String,
        mood: Mood?,
        unlockAt: Long?,
        now: Long,
    ): CapsuleValidation = CapsuleValidation(
        titleError = when {
            title.isBlank() -> "Give the capsule a title"
            title.length > TITLE_MAX_LENGTH -> "Keep the title under $TITLE_MAX_LENGTH characters"
            else -> null
        },
        messageError = when {
            message.trim().length < MESSAGE_MIN_LENGTH ->
                "Write at least $MESSAGE_MIN_LENGTH characters to your future self"
            message.length > MESSAGE_MAX_LENGTH -> "Letters are limited to $MESSAGE_MAX_LENGTH characters"
            else -> null
        },
        moodError = if (mood == null) "Choose how you feel right now" else null,
        unlockError = when {
            unlockAt == null -> "Choose when the capsule opens"
            unlockAt <= now -> "The opening time must be in the future"
            else -> null
        },
    )
}
