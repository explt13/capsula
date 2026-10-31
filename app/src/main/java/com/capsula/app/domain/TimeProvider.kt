package com.capsula.app.domain

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

fun interface TimeProvider {
    fun now(): Long
}

// простая реализация TimeProvider, которая возвращает текущее системное время.
object SystemTimeProvider : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
}

// Холодный поток, который эмиттирует текущее время с заданным периодом.
fun tickerFlow(
    timeProvider: TimeProvider,
    periodMillis: Long = Durations.SECOND,
): Flow<Long> = flow {
    while (currentCoroutineContext().isActive) {
        emit(timeProvider.now())
        delay(periodMillis)
    }
}
