package com.capsula.app.di

import android.content.Context
import com.capsula.app.data.local.CapsulaDatabase
import com.capsula.app.data.local.SampleCapsules
import com.capsula.app.data.repository.CapsuleRepository
import com.capsula.app.data.repository.OfflineCapsuleRepository
import com.capsula.app.domain.SystemTimeProvider
import com.capsula.app.domain.TimeProvider
import com.capsula.app.domain.tickerFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// Устанавливает зависимости для всего приложения (ручной DI).
// В части 2 InMemory-хранилище заменено на Room: поменялась только эта строка связки интерфейса с реализацией.
class AppContainer(
    context: Context,
    val timeProvider: TimeProvider = SystemTimeProvider,
) {
    // Scope уровня приложения: для работы, которая не принадлежит ни одному экрану (заполнение демо-данными).
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // by lazy: база открывается при первом обращении, а не при старте приложения.
    private val database: CapsulaDatabase by lazy {
        CapsulaDatabase.build(context.applicationContext) {
            applicationScope.launch {
                SampleCapsules.populate(database.capsuleDao(), timeProvider.now())
            }
        }
    }

    val capsuleRepository: CapsuleRepository by lazy {
        OfflineCapsuleRepository(dao = database.capsuleDao(), timeProvider = timeProvider)
    }

    val ticker: Flow<Long> = tickerFlow(timeProvider)
}
