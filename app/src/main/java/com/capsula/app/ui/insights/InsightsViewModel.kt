package com.capsula.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capsula.app.data.model.ActivityItem
import com.capsula.app.data.repository.CapsuleRepository
import com.capsula.app.domain.Insights
import com.capsula.app.domain.InsightsCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InsightsUiState(
    val isLoading: Boolean = true,
    val insights: Insights? = null,
    // Последние события из истории всех капсул (JOIN двух таблиц в DAO).
    val recentActivity: List<ActivityItem> = emptyList(),
    val now: Long = 0L,
)

class InsightsViewModel(
    private val repository: CapsuleRepository,
    ticker: Flow<Long>,
) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> = combine(
        repository.capsules,
        repository.observeRecentActivity(RECENT_ACTIVITY_LIMIT),
        ticker,
    ) { capsules, activity, now ->
        InsightsUiState(
            isLoading = false,
            insights = InsightsCalculator.calculate(capsules, now),
            recentActivity = activity,
            now = now,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InsightsUiState(),
    )

    // Undo для капсулы, убранной в корзину с экрана деталей, открытого отсюда.
    fun restoreFromTrash(capsuleId: Long) {
        viewModelScope.launch { repository.restoreFromTrash(capsuleId) }
    }

    private companion object {
        const val RECENT_ACTIVITY_LIMIT = 8
    }
}
