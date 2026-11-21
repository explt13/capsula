package com.capsula.app.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.CapsuleStatus
import com.capsula.app.data.repository.CapsuleRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrashItemUi(
    val capsule: Capsule,
    // Каким капсула была до удаления: черновик, запечатана, вскрыта...
    val status: CapsuleStatus,
    val trashedAgoMillis: Long,
)

data class TrashUiState(
    val isLoading: Boolean = true,
    val items: List<TrashItemUi> = emptyList(),
)

// ViewModel корзины: восстановление и окончательное удаление.
class TrashViewModel(
    private val repository: CapsuleRepository,
    ticker: Flow<Long>,
) : ViewModel() {

    val uiState: StateFlow<TrashUiState> = combine(repository.trashedCapsules, ticker) { capsules, now ->
        TrashUiState(
            isLoading = false,
            items = capsules.map { capsule ->
                TrashItemUi(
                    capsule = capsule,
                    status = capsule.statusAt(now),
                    trashedAgoMillis = now - (capsule.trashedAt ?: now),
                )
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrashUiState(),
    )

    // Сообщения для snackbar после действия.
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    fun restore(capsule: Capsule) {
        viewModelScope.launch {
            repository.restoreFromTrash(capsule.id)
            _messages.send("“${capsule.title}” is back in the vault")
        }
    }

    fun deleteForever(capsule: Capsule) {
        viewModelScope.launch {
            repository.deletePermanently(capsule.id)
            _messages.send("“${capsule.title}” deleted forever")
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val count = repository.emptyTrash()
            _messages.send(if (count == 1) "1 capsule deleted forever" else "$count capsules deleted forever")
        }
    }
}
