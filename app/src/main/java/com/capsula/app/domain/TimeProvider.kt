package com.capsula.app.domain

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

/**
 * Abstraction over the system clock. Production code uses [SystemTimeProvider];
 * unit tests pass a fixed lambda so time-dependent logic is deterministic.
 */
fun interface TimeProvider {
    fun now(): Long
}

object SystemTimeProvider : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
}

/**
 * A cold flow that emits the current time every [periodMillis].
 *
 * It is the "heartbeat" that drives every live countdown in the app. Because it is cold and
 * ViewModels share it with `SharingStarted.WhileSubscribed`, the ticker only runs while a
 * screen is actually visible (the coroutine is cancelled when the UI stops collecting).
 */
fun tickerFlow(
    timeProvider: TimeProvider,
    periodMillis: Long = Durations.SECOND,
): Flow<Long> = flow {
    while (currentCoroutineContext().isActive) {
        emit(timeProvider.now())
        delay(periodMillis)
    }
}
