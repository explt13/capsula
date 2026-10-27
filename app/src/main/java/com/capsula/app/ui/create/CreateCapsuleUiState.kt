package com.capsula.app.ui.create

import com.capsula.app.data.model.Mood
import com.capsula.app.domain.Durations

enum class UnlockPreset(val label: String, val durationMillis: Long?) {
    ONE_MINUTE("1 minute", Durations.MINUTE),
    ONE_HOUR("1 hour", Durations.HOUR),
    ONE_WEEK("1 week", 7 * Durations.DAY),
    ONE_MONTH("1 month", 30 * Durations.DAY),
    SIX_MONTHS("6 months", 182 * Durations.DAY),
    ONE_YEAR("1 year", 365 * Durations.DAY),
    CUSTOM("Pick a date", null),
}

/** Raw user input. */
data class CreateCapsuleForm(
    val title: String = "",
    val message: String = "",
    val question: String = "",
    val mood: Mood? = null,
    val preset: UnlockPreset = UnlockPreset.ONE_MONTH,
    /** UTC midnight of the day chosen in the DatePicker (only for [UnlockPreset.CUSTOM]). */
    val customDateMillis: Long? = null,
)

/** Input plus everything derived from it: unlock preview, validation errors, saving flag. */
data class CreateCapsuleUiState(
    val form: CreateCapsuleForm = CreateCapsuleForm(),
    val unlockAtPreview: Long? = null,
    val titleError: String? = null,
    val messageError: String? = null,
    val moodError: String? = null,
    val unlockError: String? = null,
    val showErrors: Boolean = false,
    val isSaving: Boolean = false,
) {
    /** True when leaving the screen would throw away something the user typed. */
    val isDirty: Boolean
        get() = form.title.isNotBlank() || form.message.isNotBlank() || form.question.isNotBlank()
}

sealed interface CreateCapsuleEvent {
    data class Sealed(val capsuleId: Long) : CreateCapsuleEvent
}
