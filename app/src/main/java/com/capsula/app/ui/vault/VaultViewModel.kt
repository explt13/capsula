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
import kotlinx.coroutines.flow.stateIn

// ViewModel для экрана Vault. Отвечает за управление состоянием UI и взаимодействие с репозиторием капсул.
class VaultViewModel(
    private val savedStateHandle: SavedStateHandle,
    repository: CapsuleRepository,
    ticker: Flow<Long>,
) : ViewModel() {

    private val selectedFilter: StateFlow<VaultFilter> =
        savedStateHandle.getStateFlow(KEY_FILTER, VaultFilter.ALL)

    val uiState: StateFlow<VaultUiState> = combine(
        repository.capsules,
        ticker,
        selectedFilter,
    ) { capsules, now, filter ->
        val allItems = capsules.map { it.toItemUi(now) }
        VaultUiState(
            isLoading = false,
            items = allItems.filter { filter.matches(it.status) }.sortedWith(VAULT_ORDER),
            selectedFilter = filter,
            counts = VaultFilter.entries.associateWith { f -> allItems.count { f.matches(it.status) } },
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

    private companion object {
        const val KEY_FILTER = "vault_filter"

        // Определяет порядок сортировки капсул в UI: сначала READY, затем SEALED, затем OPENED.
        val VAULT_ORDER = compareBy<CapsuleItemUi>(
            { item ->
                when (item.status) {
                    CapsuleStatus.READY -> 0
                    CapsuleStatus.SEALED -> 1
                    CapsuleStatus.OPENED -> 2
                }
            },
            { item ->
                if (item.status == CapsuleStatus.OPENED) -(item.capsule.openedAt ?: 0L)
                else item.capsule.unlockAt
            },
        )
    }
}
