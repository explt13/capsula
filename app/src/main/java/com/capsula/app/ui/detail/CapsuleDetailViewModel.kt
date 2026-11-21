package com.capsula.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.CapsuleEvent
import com.capsula.app.data.model.CapsuleStatus
import com.capsula.app.data.model.Mood
import com.capsula.app.data.repository.CapsuleRepository
import com.capsula.app.domain.CapsuleRules
import com.capsula.app.domain.CapsuleValidator
import com.capsula.app.domain.PostponeOption
import com.capsula.app.domain.SealCipher
import com.capsula.app.domain.TimeFormatter
import com.capsula.app.domain.TimeProvider
import com.capsula.app.ui.navigation.Destination
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CapsuleDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: CapsuleRepository,
    ticker: Flow<Long>,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    // Необходимо получить идентификатор капсулы из аргументов навигации. Если идентификатор отсутствует, выбрасывается исключение.
    private val capsuleId: Long = checkNotNull(savedStateHandle.get<Long>(Destination.ARG_CAPSULE_ID)) {
        "Capsule id is missing from navigation arguments"
    }

    // Пользователь сам убрал капсулу в корзину: пока экран закрывается, показываем прежнее состояние,
    // а не мигающий экран «капсулы нет».
    private var trashRequested = false
    private var lastVisibleState: CapsuleDetailUiState = CapsuleDetailUiState.Loading

    // Три источника: строка капсулы и её история из Room (Flow) и текущее время (тикер).
    // Любое изменение в базе или новая секунда — и экран получает новое состояние.
    val uiState: StateFlow<CapsuleDetailUiState> = combine(
        repository.observeCapsule(capsuleId),
        repository.observeHistory(capsuleId),
        ticker,
    ) { capsule, history, now ->
        when {
            capsule == null -> CapsuleDetailUiState.NotFound
            capsule.isTrashed -> if (trashRequested) lastVisibleState else CapsuleDetailUiState.NotFound
            else -> capsule.toUiState(history, now).also { lastVisibleState = it }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CapsuleDetailUiState.Loading,
    )

    private val _events = Channel<CapsuleDetailEvent>(Channel.BUFFERED)
    val events: Flow<CapsuleDetailEvent> = _events.receiveAsFlow()

    // Каждое действие повторно проверяет правила по свежим данным из базы: UI не является источником истины.
    fun openCapsule() = withCapsule { capsule, now ->
        if (CapsuleRules.canOpen(capsule, now)) repository.markOpened(capsuleId, openedAt = now)
    }

    fun saveReflection(mood: Mood, reflection: String) = withCapsule { capsule, now ->
        if (!CapsuleRules.canReply(capsule, now) || CapsuleValidator.replyError(mood, reflection) != null) return@withCapsule
        val isEdit = capsule.hasReflection
        repository.saveReflection(capsuleId, mood, reflection)
        _events.send(CapsuleDetailEvent.Message(if (isEdit) "Reply updated" else "Reply saved"))
    }

    fun postpone(option: PostponeOption) = withCapsule { capsule, now ->
        if (!CapsuleRules.canPostpone(capsule, now)) {
            _events.send(CapsuleDetailEvent.Message("This capsule can't be postponed anymore"))
            return@withCapsule
        }
        val newUnlockAt = CapsuleRules.postponedUnlockAt(capsule, now, option)
        repository.postpone(capsuleId, newUnlockAt, option.label)
        _events.send(CapsuleDetailEvent.Message("Now opens on ${TimeFormatter.dateTime(newUnlockAt)}"))
    }

    fun rename(newTitle: String) = withCapsule { capsule, now ->
        if (!CapsuleRules.canRename(capsule, now) || CapsuleValidator.titleError(newTitle) != null) return@withCapsule
        if (newTitle.trim() == capsule.title) return@withCapsule
        repository.rename(capsuleId, newTitle)
        _events.send(CapsuleDetailEvent.Message("Renamed"))
    }

    fun moveToTrash() = withCapsule { capsule, _ ->
        trashRequested = true
        repository.moveToTrash(capsuleId)
        _events.send(CapsuleDetailEvent.Trashed(capsuleId, capsule.title))
    }

    private fun withCapsule(action: suspend (Capsule, Long) -> Unit) {
        viewModelScope.launch {
            val capsule = repository.getCapsule(capsuleId) ?: return@launch
            action(capsule, timeProvider.now())
        }
    }

    private fun Capsule.toUiState(history: List<CapsuleEvent>, now: Long): CapsuleDetailUiState = when (statusAt(now)) {
        CapsuleStatus.DRAFT -> CapsuleDetailUiState.Draft(capsule = this, history = history)
        CapsuleStatus.SEALED -> CapsuleDetailUiState.Sealed(
            capsule = this,
            history = history,
            remainingMillis = remainingMillis(now),
            progress = sealProgress(now),
            cipherText = SealCipher.scramble(message.take(MAX_CIPHER_LENGTH), salt = (now / 1_000).toInt()),
            postpone = postponeState(now),
        )
        CapsuleStatus.READY -> CapsuleDetailUiState.Ready(
            capsule = this,
            history = history,
            waitedMillis = now - (sealedAt ?: createdAt),
            postpone = postponeState(now),
        )
        CapsuleStatus.OPENED -> CapsuleDetailUiState.Opened(
            capsule = this,
            history = history,
            sealedForMillis = (openedAt ?: now) - (sealedAt ?: createdAt),
        )
    }

    private fun Capsule.postponeState(now: Long) = PostponeState(
        allowed = CapsuleRules.canPostpone(this, now),
        left = CapsuleRules.postponesLeft(this),
        max = CapsuleRules.MAX_POSTPONES,
        preview = PostponeOption.entries.associateWith { CapsuleRules.postponedUnlockAt(this, now, it) },
    )

    private companion object {
        const val MAX_CIPHER_LENGTH = 320
    }
}
