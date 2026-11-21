package com.capsula.app.domain

import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.CapsuleStatus
import com.capsula.app.data.model.Mood

enum class MoodShift { IMPROVED, SAME, WORSE }

data class Insights(
    val total: Int,
    val drafts: Int,
    val sealed: Int,
    val ready: Int,
    val opened: Int,
    val sealingMoods: Map<Mood, Int>,
    val moodShifts: Map<MoodShift, Int>,
    val averageWaitMillis: Long?,
    val longestWaitMillis: Long?,
    val totalPostpones: Int,
    val nextUnlock: Capsule?,
    val nextUnlockInMillis: Long?,
) {
    val isEmpty: Boolean get() = total == 0
    val reflectedCount: Int get() = moodShifts.values.sum()
}

/// Возвращает изменение настроения пользователя после открытия капсулы.
fun Capsule.moodShift(): MoodShift? {
    val before = moodAtSealing ?: return null
    val after = moodAtOpening ?: return null
    return when {
        after.score > before.score -> MoodShift.IMPROVED
        after.score == before.score -> MoodShift.SAME
        else -> MoodShift.WORSE
    }
}

// Класс для вычисления статистики по капсулам (капсулы из корзины сюда не передаются).
object InsightsCalculator {

    fun calculate(capsules: List<Capsule>, now: Long): Insights {
        val byStatus: Map<CapsuleStatus, List<Capsule>> = capsules.groupBy { it.statusAt(now) }
        val opened = byStatus[CapsuleStatus.OPENED].orEmpty()
        val sealed = byStatus[CapsuleStatus.SEALED].orEmpty()

        // Сколько письмо ждало: от запечатывания до вскрытия.
        val waits: List<Long> = opened.mapNotNull { capsule ->
            val start = capsule.sealedAt ?: capsule.createdAt
            capsule.openedAt?.let { openedAt -> openedAt - start }
        }
        val shiftCounts: Map<MoodShift, Int> = opened
            .mapNotNull { it.moodShift() }
            .groupingBy { it }
            .eachCount()
        val next: Capsule? = sealed.minByOrNull { it.unlockAt ?: Long.MAX_VALUE }

        return Insights(
            total = capsules.size,
            drafts = byStatus[CapsuleStatus.DRAFT].orEmpty().size,
            sealed = sealed.size,
            ready = byStatus[CapsuleStatus.READY].orEmpty().size,
            opened = opened.size,
            sealingMoods = Mood.entries.associateWith { mood ->
                capsules.count { !it.isDraft && it.moodAtSealing == mood }
            },
            moodShifts = MoodShift.entries.associateWith { shiftCounts[it] ?: 0 },
            averageWaitMillis = waits.takeIf { it.isNotEmpty() }?.average()?.toLong(),
            longestWaitMillis = waits.maxOrNull(),
            totalPostpones = capsules.sumOf { it.postponeCount },
            nextUnlock = next,
            nextUnlockInMillis = next?.unlockAt?.let { it - now },
        )
    }
}
