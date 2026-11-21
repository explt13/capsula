package com.capsula.app.ui.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capsula.app.data.model.CapsuleContent
import com.capsula.app.data.model.Mood
import com.capsula.app.data.repository.CapsuleRepository
import com.capsula.app.domain.CapsuleRules
import com.capsula.app.domain.CapsuleValidation
import com.capsula.app.domain.CapsuleValidator
import com.capsula.app.domain.TimeProvider
import com.capsula.app.domain.UnlockTime
import com.capsula.app.ui.navigation.Destination
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * ViewModel формы письма. Работает в двух режимах:
 * - CREATE: маршрут "create", новое письмо;
 * - EDIT_DRAFT: маршрут "edit/{capsuleId}", продолжение черновика (форма заполняется из базы).
 *
 * Из формы можно сохранить черновик (проверяется только заголовок) или запечатать письмо (полная проверка).
 * Все поля дублируются в SavedStateHandle, поэтому ввод переживает даже убийство процесса.
 */
class CreateCapsuleViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: CapsuleRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    // null для нового письма; id черновика для режима редактирования.
    private val editingId: Long? = savedStateHandle.get<Long>(Destination.ARG_CAPSULE_ID)

    private val mode = if (editingId == null) FormMode.CREATE else FormMode.EDIT_DRAFT

    private var initialContent: CapsuleContent =
        savedStateHandle.get<Boolean>(KEY_LOADED)?.let { restoreForm().toContent() } ?: CreateCapsuleForm().toContent()

    private val _uiState = MutableStateFlow(
        buildState(
            form = restoreForm(),
            errorMode = savedStateHandle.get<String>(KEY_ERROR_MODE)?.let { ErrorMode.valueOf(it) } ?: ErrorMode.NONE,
            isSaving = false,
        ),
    )
    val uiState: StateFlow<CreateCapsuleUiState> = _uiState.asStateFlow()

    private val _events = Channel<CreateCapsuleEvent>(Channel.BUFFERED)
    val events: Flow<CreateCapsuleEvent> = _events.receiveAsFlow()

    init {
        // Черновик загружается из базы один раз. После поворота экрана значения уже лежат в SavedStateHandle.
        if (editingId != null && savedStateHandle.get<Boolean>(KEY_LOADED) != true) {
            loadDraft(editingId)
        }
    }

    fun onTitleChange(value: String) =
        updateForm { it.copy(title = value.take(CapsuleValidator.TITLE_MAX_LENGTH)) }

    fun onMessageChange(value: String) =
        updateForm { it.copy(message = value.take(CapsuleValidator.MESSAGE_MAX_LENGTH)) }

    fun onQuestionChange(value: String) =
        updateForm { it.copy(question = value.take(CapsuleValidator.QUESTION_MAX_LENGTH)) }

    fun onMoodSelected(mood: Mood) = updateForm { it.copy(mood = mood) }

    fun onPresetSelected(preset: UnlockPreset) = updateForm { it.copy(preset = preset) }

    fun onCustomDatePicked(utcMidnightMillis: Long) =
        updateForm { it.copy(preset = UnlockPreset.CUSTOM, customDateMillis = utcMidnightMillis) }

    fun onSealClick() {
        val current = _uiState.value
        if (current.isSaving || current.isLoading) return

        val now = timeProvider.now()
        val form = current.form
        val unlockAt = resolveUnlockAt(form, now)
        val validation = CapsuleValidator.validate(form.title, form.message, form.mood, unlockAt, now)
        if (!validation.isValid || unlockAt == null) {
            showErrors(form, ErrorMode.SEAL)
            return
        }

        _uiState.value = current.copy(isSaving = true)
        viewModelScope.launch {
            val id = repository.seal(editingId, form.toContent(), unlockAt)
            clearDraft()
            _events.send(CreateCapsuleEvent.Saved(id, sealed = true))
        }
    }

    fun onSaveDraftClick() {
        val current = _uiState.value
        if (current.isSaving || current.isLoading) return

        val form = current.form
        if (!CapsuleValidator.validateDraft(form.title, form.message).isValid) {
            showErrors(form, ErrorMode.DRAFT)
            return
        }

        _uiState.value = current.copy(isSaving = true)
        viewModelScope.launch {
            val id = repository.saveDraft(editingId, form.toContent())
            clearDraft()
            _events.send(CreateCapsuleEvent.Saved(id, sealed = false))
        }
    }

    private fun loadDraft(id: Long) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val capsule = repository.getCapsule(id)
            if (capsule == null || !CapsuleRules.canEditContent(capsule, timeProvider.now())) {
                _events.send(CreateCapsuleEvent.NotEditable)
                return@launch
            }
            val form = CreateCapsuleForm(
                title = capsule.title,
                message = capsule.message,
                question = capsule.questionForFuture.orEmpty(),
                mood = capsule.moodAtSealing,
            )
            initialContent = form.toContent()
            persist(form)
            savedStateHandle[KEY_LOADED] = true
            _uiState.value = buildState(form, ErrorMode.NONE, isSaving = false)
        }
    }

    private fun showErrors(form: CreateCapsuleForm, errorMode: ErrorMode) {
        savedStateHandle[KEY_ERROR_MODE] = errorMode.name
        _uiState.value = buildState(form, errorMode, isSaving = false)
    }

    private fun updateForm(transform: (CreateCapsuleForm) -> CreateCapsuleForm) {
        val state = _uiState.value
        if (state.isLoading) return
        val newForm = transform(state.form)
        persist(newForm)
        val errorMode = savedStateHandle.get<String>(KEY_ERROR_MODE)?.let { ErrorMode.valueOf(it) } ?: ErrorMode.NONE
        _uiState.value = buildState(newForm, errorMode, state.isSaving)
    }

    private fun buildState(
        form: CreateCapsuleForm,
        errorMode: ErrorMode,
        isSaving: Boolean,
    ): CreateCapsuleUiState {
        val now = timeProvider.now()
        val unlockAt = resolveUnlockAt(form, now)
        val validation: CapsuleValidation = when (errorMode) {
            ErrorMode.NONE -> CapsuleValidation()
            ErrorMode.DRAFT -> CapsuleValidator.validateDraft(form.title, form.message)
            ErrorMode.SEAL -> CapsuleValidator.validate(form.title, form.message, form.mood, unlockAt, now)
        }
        return CreateCapsuleUiState(
            mode = mode,
            form = form,
            initialContent = initialContent,
            unlockAtPreview = unlockAt,
            titleError = validation.titleError,
            messageError = validation.messageError,
            moodError = validation.moodError,
            unlockError = validation.unlockError,
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
        listOf(KEY_TITLE, KEY_MESSAGE, KEY_QUESTION, KEY_MOOD, KEY_PRESET, KEY_CUSTOM_DATE, KEY_ERROR_MODE, KEY_LOADED)
            .forEach { savedStateHandle.remove<Any>(it) }
    }

    private companion object {
        const val KEY_TITLE = "draft_title"
        const val KEY_MESSAGE = "draft_message"
        const val KEY_QUESTION = "draft_question"
        const val KEY_MOOD = "draft_mood"
        const val KEY_PRESET = "draft_preset"
        const val KEY_CUSTOM_DATE = "draft_custom_date"
        const val KEY_ERROR_MODE = "draft_error_mode"
        const val KEY_LOADED = "draft_loaded_from_db"
    }
}
