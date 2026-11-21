package com.capsula.app.ui.vault

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capsula.app.data.model.CapsuleStatus
import com.capsula.app.data.repository.CapsuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ViewModel для экрана Vault. Отвечает за управление состоянием UI и взаимодействие с репозиторием капсул.
class VaultViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: CapsuleRepository,
    ticker: Flow<Long>,
) : ViewModel() {

    private val selectedFilter: StateFlow<VaultFilter> =
        savedStateHandle.getStateFlow(KEY_FILTER, VaultFilter.ALL)

    // Четыре источника: капсулы из Room, количество в корзине (тоже Room), текущее время и выбранный фильтр.
    val uiState: StateFlow<VaultUiState> = combine(
        repository.capsules,
        repository.trashedCapsules.map { it.size },
        ticker,
        selectedFilter,
    ) { capsules, trashCount, now, filter ->
        val allItems = capsules.map { it.toItemUi(now) }
        VaultUiState(
            isLoading = false,
            items = allItems.filter { filter.matches(it.status) }.sortedWith(VAULT_ORDER),
            selectedFilter = filter,
            counts = VaultFilter.entries.associateWith { f -> allItems.count { f.matches(it.status) } },
            trashCount = trashCount,
        )
    }.stateIn(
        scope = viewModelScope,
        // Подписка на поток данных будет активной, пока есть подписчики, и будет оставаться активной в течение 5 секунд после того, как последний подписчик отпишется.
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VaultUiState(),
    )

    fun onFilterSelected(filter: VaultFilter) {
        savedStateHandle[KEY_FILTER] = filter
    }

    // Кнопка Undo в snackbar после перемещения капсулы в корзину.
    fun restoreFromTrash(capsuleId: Long) {
        viewModelScope.launch { repository.restoreFromTrash(capsuleId) }
    }

    private companion object {
        const val KEY_FILTER = "vault_filter"

        // Определяет порядок сортировки капсул: READY, затем SEALED (раньше откроется — выше), DRAFT, OPENED.
        val VAULT_ORDER = compareBy<CapsuleItemUi>(
            { item ->
                when (item.status) {
                    CapsuleStatus.READY -> 0
                    CapsuleStatus.SEALED -> 1
                    CapsuleStatus.DRAFT -> 2
                    CapsuleStatus.OPENED -> 3
                }
            },
            { item ->
                when (item.status) {
                    CapsuleStatus.OPENED -> -(item.capsule.openedAt ?: 0L)
                    CapsuleStatus.DRAFT -> -item.capsule.updatedAt
                    else -> item.capsule.unlockAt ?: 0L
                }
            },
        )
    }
}
