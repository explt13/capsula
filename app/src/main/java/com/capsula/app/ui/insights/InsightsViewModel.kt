package com.capsula.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capsula.app.data.repository.CapsuleRepository
import com.capsula.app.domain.Insights
import com.capsula.app.domain.InsightsCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class InsightsUiState(
    val isLoading: Boolean = true,
    val insights: Insights? = null,
)

class InsightsViewModel(
    repository: CapsuleRepository,
    ticker: Flow<Long>,
) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> = combine(repository.capsules, ticker) { capsules, now ->
        InsightsUiState(isLoading = false, insights = InsightsCalculator.calculate(capsules, now))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InsightsUiState(),
    )
}
