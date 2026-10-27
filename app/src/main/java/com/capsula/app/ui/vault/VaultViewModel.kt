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

/**
 * Combines three sources into one UI state:
 *  1. the list of capsules (repository),
 *  2. the current time (ticker, every second) -> live countdowns,
 *  3. the selected filter (SavedStateHandle) -> survives rotation and process death.
 */
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
        // Keep the upstream (and the ticker) alive 5 s after the UI stops collecting,
        // so a rotation does not restart it, but a backgrounded app stops ticking.
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VaultUiState(),
    )

    fun onFilterSelected(filter: VaultFilter) {
        savedStateHandle[KEY_FILTER] = filter
    }

    private companion object {
        const val KEY_FILTER = "vault_filter"

        /** Ready first (oldest first), then sealed (soonest first), then opened (latest first). */
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
