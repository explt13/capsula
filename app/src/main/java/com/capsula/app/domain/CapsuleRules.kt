package com.capsula.app.domain

import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.CapsuleStatus

// Варианты, на сколько можно отложить открытие капсулы.
enum class PostponeOption(val label: String, val millis: Long) {
    ONE_DAY("+1 day", Durations.DAY),
    ONE_WEEK("+1 week", 7 * Durations.DAY),
    ONE_MONTH("+1 month", 30 * Durations.DAY),
}

/**
 * Правила жизненного цикла капсулы. Чистый Kotlin, поэтому покрыто unit-тестами.
 *
 * - черновик можно редактировать сколько угодно, запечатанное письмо — нет;
 * - запечатанную (или готовую, но ещё не вскрытую) капсулу можно отложить не больше MAX_POSTPONES раз,
 *   и только на более позднее время — приблизить открытие нельзя;
 * - вскрыть можно только капсулу в статусе READY;
 * - ответить прошлому себе можно только после вскрытия.
 */
object CapsuleRules {
    const val MAX_POSTPONES = 3

    fun canEditContent(capsule: Capsule, now: Long): Boolean =
        !capsule.isTrashed && capsule.statusAt(now) == CapsuleStatus.DRAFT

    // Переименовать можно любую запечатанную капсулу: сам текст письма при этом не меняется.
    fun canRename(capsule: Capsule, now: Long): Boolean =
        !capsule.isTrashed && capsule.statusAt(now) != CapsuleStatus.DRAFT

    fun postponesLeft(capsule: Capsule): Int = (MAX_POSTPONES - capsule.postponeCount).coerceAtLeast(0)

    fun canPostpone(capsule: Capsule, now: Long): Boolean {
        val status = capsule.statusAt(now)
        return !capsule.isTrashed &&
            (status == CapsuleStatus.SEALED || status == CapsuleStatus.READY) &&
            postponesLeft(capsule) > 0
    }

    // Новое время отсчитывается от более позднего из «сейчас» и текущего времени открытия.
    fun postponedUnlockAt(capsule: Capsule, now: Long, option: PostponeOption): Long =
        maxOf(capsule.unlockAt ?: now, now) + option.millis

    fun canOpen(capsule: Capsule, now: Long): Boolean =
        !capsule.isTrashed && capsule.statusAt(now) == CapsuleStatus.READY

    fun canReply(capsule: Capsule, now: Long): Boolean =
        !capsule.isTrashed && capsule.statusAt(now) == CapsuleStatus.OPENED
}
