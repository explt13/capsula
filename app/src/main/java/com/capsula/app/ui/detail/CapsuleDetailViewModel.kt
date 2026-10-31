package com.capsula.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.CapsuleStatus
import com.capsula.app.data.model.Mood
import com.capsula.app.data.repository.CapsuleRepository
import com.capsula.app.domain.SealCipher
import com.capsula.app.domain.TimeProvider
import com.capsula.app.ui.navigation.Destination
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    private val capsuleId: Long = checkNotNull(savedStateHandle.get<Long>(Destination.Detail.ARG_CAPSULE_ID)) {
        "Capsule id is missing from navigation arguments"
    }

    // UI-состояние для экрана деталей капсулы. Обновляется при изменении капсулы или времени.
    val uiState: StateFlow<CapsuleDetailUiState> = combine(
        repository.observeCapsule(capsuleId),
        ticker,
    ) { capsule, now ->
        capsule?.toUiState(now) ?: CapsuleDetailUiState.NotFound
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CapsuleDetailUiState.Loading,
    )

    private val _events = Channel<CapsuleDetailEvent>(Channel.BUFFERED)
    val events: Flow<CapsuleDetailEvent> = _events.receiveAsFlow()

    // Открывает капсулу, если она готова к открытию. Если капсула уже открыта или не готова, ничего не происходит.
    fun openCapsule() {
        viewModelScope.launch {
            val now = timeProvider.now()
            val capsule = repository.observeCapsule(capsuleId).first() ?: return@launch
            if (capsule.statusAt(now) == CapsuleStatus.READY) {
                repository.markOpened(capsuleId, openedAt = now)
            }
        }
    }

    fun saveReflection(mood: Mood, reflection: String) {
        viewModelScope.launch { repository.saveReflection(capsuleId, mood, reflection) }
    }

    fun deleteCapsule() {
        viewModelScope.launch {
            repository.deleteCapsule(capsuleId)
            _events.send(CapsuleDetailEvent.Deleted)
        }
    }

    private fun Capsule.toUiState(now: Long): CapsuleDetailUiState = when (statusAt(now)) {
        CapsuleStatus.SEALED -> CapsuleDetailUiState.Sealed(
            capsule = this,
            remainingMillis = remainingMillis(now),
            progress = sealProgress(now),
            cipherText = SealCipher.scramble(message.take(MAX_CIPHER_LENGTH), salt = (now / 1_000).toInt()),
        )
        CapsuleStatus.READY -> CapsuleDetailUiState.Ready(capsule = this, waitedMillis = now - createdAt)
        CapsuleStatus.OPENED -> CapsuleDetailUiState.Opened(
            capsule = this,
            sealedForMillis = (openedAt ?: now) - createdAt,
        )
    }

    private companion object {
        const val MAX_CIPHER_LENGTH = 320
    }
}
