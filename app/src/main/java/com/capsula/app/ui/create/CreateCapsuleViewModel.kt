package com.capsula.app.ui.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capsula.app.data.model.Mood
import com.capsula.app.data.model.NewCapsule
import com.capsula.app.data.repository.CapsuleRepository
import com.capsula.app.domain.CapsuleValidator
import com.capsula.app.domain.TimeProvider
import com.capsula.app.domain.UnlockTime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

// ViewModel для экрана создания капсулы. Отвечает за управление состоянием UI, валидацию формы и взаимодействие с репозиторием капсул.
class CreateCapsuleViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: CapsuleRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        buildState(
            form = restoreForm(),
            showErrors = savedStateHandle[KEY_SHOW_ERRORS] ?: false,
            isSaving = false,
        ),
    )
    val uiState: StateFlow<CreateCapsuleUiState> = _uiState.asStateFlow()

    private val _events = Channel<CreateCapsuleEvent>(Channel.BUFFERED)
    val events: Flow<CreateCapsuleEvent> = _events.receiveAsFlow()

    fun onTitleChange(value: String) =
        updateForm { it.copy(title = value.take(CapsuleValidator.TITLE_MAX_LENGTH)) }

    fun onMessageChange(value: String) =
        updateForm { it.copy(message = value.take(CapsuleValidator.MESSAGE_MAX_LENGTH)) }

    fun onQuestionChange(value: String) = updateForm { it.copy(question = value.take(120)) }

    fun onMoodSelected(mood: Mood) = updateForm { it.copy(mood = mood) }

    fun onPresetSelected(preset: UnlockPreset) = updateForm { it.copy(preset = preset) }

    fun onCustomDatePicked(utcMidnightMillis: Long) =
        updateForm { it.copy(preset = UnlockPreset.CUSTOM, customDateMillis = utcMidnightMillis) }

    fun onSealClick() {
        val current = _uiState.value
        if (current.isSaving) return

        val now = timeProvider.now()
        val form = current.form
        val unlockAt = resolveUnlockAt(form, now)
        val validation = CapsuleValidator.validate(form.title, form.message, form.mood, unlockAt, now)

        if (!validation.isValid || unlockAt == null || form.mood == null) {
            savedStateHandle[KEY_SHOW_ERRORS] = true
            _uiState.value = buildState(form, showErrors = true, isSaving = false)
            return
        }

        _uiState.value = current.copy(isSaving = true)
        viewModelScope.launch {
            val id = repository.addCapsule(
                NewCapsule(
                    title = form.title,
                    message = form.message,
                    questionForFuture = form.question,
                    moodAtSealing = form.mood,
                    unlockAt = unlockAt,
                ),
            )
            clearDraft()
            _events.send(CreateCapsuleEvent.Sealed(id))
        }
    }

    private fun updateForm(transform: (CreateCapsuleForm) -> CreateCapsuleForm) {
        val state = _uiState.value
        val newForm = transform(state.form)
        persist(newForm)
        _uiState.value = buildState(newForm, state.showErrors, state.isSaving)
    }

    private fun buildState(
        form: CreateCapsuleForm,
        showErrors: Boolean,
        isSaving: Boolean,
    ): CreateCapsuleUiState {
        val now = timeProvider.now()
        val unlockAt = resolveUnlockAt(form, now)
        val validation = CapsuleValidator.validate(form.title, form.message, form.mood, unlockAt, now)
        return CreateCapsuleUiState(
            form = form,
            unlockAtPreview = unlockAt,
            titleError = validation.titleError.takeIf { showErrors },
            messageError = validation.messageError.takeIf { showErrors },
            moodError = validation.moodError.takeIf { showErrors },
            unlockError = validation.unlockError.takeIf { showErrors },
            showErrors = showErrors,
            isSaving = isSaving,
        )
    }

    private fun resolveUnlockAt(form: CreateCapsuleForm, now: Long): Long? =
        when (val duration = form.preset.durationMillis) {
            null -> form.customDateMillis?.let { UnlockTime.fromPickerDate(it) }
            else -> now + duration
        }

    private fun persist(form: CreateCapsuleForm) {
        savedStateHandle[KEY_TITLE] = form.title
        savedStateHandle[KEY_MESSAGE] = form.message
        savedStateHandle[KEY_QUESTION] = form.question
        savedStateHandle[KEY_MOOD] = form.mood?.name
        savedStateHandle[KEY_PRESET] = form.preset.name
        savedStateHandle[KEY_CUSTOM_DATE] = form.customDateMillis
    }

    private fun restoreForm(): CreateCapsuleForm = CreateCapsuleForm(
        title = savedStateHandle[KEY_TITLE] ?: "",
        message = savedStateHandle[KEY_MESSAGE] ?: "",
        question = savedStateHandle[KEY_QUESTION] ?: "",
        mood = savedStateHandle.get<String>(KEY_MOOD)?.let { name -> Mood.entries.firstOrNull { it.name == name } },
        preset = savedStateHandle.get<String>(KEY_PRESET)
            ?.let { name -> UnlockPreset.entries.firstOrNull { it.name == name } }
            ?: UnlockPreset.ONE_MONTH,
        customDateMillis = savedStateHandle[KEY_CUSTOM_DATE],
    )

    private fun clearDraft() {
        listOf(KEY_TITLE, KEY_MESSAGE, KEY_QUESTION, KEY_MOOD, KEY_PRESET, KEY_CUSTOM_DATE, KEY_SHOW_ERRORS)
            .forEach { savedStateHandle.remove<Any>(it) }
    }

    private companion object {
        const val KEY_TITLE = "draft_title"
        const val KEY_MESSAGE = "draft_message"
        const val KEY_QUESTION = "draft_question"
        const val KEY_MOOD = "draft_mood"
        const val KEY_PRESET = "draft_preset"
        const val KEY_CUSTOM_DATE = "draft_custom_date"
        const val KEY_SHOW_ERRORS = "draft_show_errors"
    }
}
