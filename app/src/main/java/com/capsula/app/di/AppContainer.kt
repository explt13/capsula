package com.capsula.app.di

import com.capsula.app.data.repository.CapsuleRepository
import com.capsula.app.data.repository.InMemoryCapsuleRepository
import com.capsula.app.domain.SystemTimeProvider
import com.capsula.app.domain.TimeProvider
import com.capsula.app.domain.tickerFlow
import kotlinx.coroutines.flow.Flow

// Устанавливает зависимости для всего приложения.
class AppContainer(
    val timeProvider: TimeProvider = SystemTimeProvider,
) {
    val capsuleRepository: CapsuleRepository =
        InMemoryCapsuleRepository(timeProvider = timeProvider, seedSampleData = true)

    val ticker: Flow<Long> = tickerFlow(timeProvider)
}
