package com.capsula.app.domain

import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.CapsuleStatus
import com.capsula.app.data.model.Mood

enum class MoodShift { IMPROVED, SAME, WORSE }

data class Insights(
    val total: Int,
    val sealed: Int,
    val ready: Int,
    val opened: Int,
    val sealingMoods: Map<Mood, Int>,
    val moodShifts: Map<MoodShift, Int>,
    val averageWaitMillis: Long?,
    val longestWaitMillis: Long?,
    val nextUnlock: Capsule?,
    val nextUnlockInMillis: Long?,
) {
    val isEmpty: Boolean get() = total == 0
    val reflectedCount: Int get() = moodShifts.values.sum()
}

/** Compares how the user felt when writing a capsule with how they felt when opening it. */
fun Capsule.moodShift(): MoodShift? {
    val after = moodAtOpening ?: return null
    return when {
        after.score > moodAtSealing.score -> MoodShift.IMPROVED
        after.score == moodAtSealing.score -> MoodShift.SAME
        else -> MoodShift.WORSE
    }
}

/** Turns the list of capsules into statistics for the Insights screen. */
object InsightsCalculator {

    fun calculate(capsules: List<Capsule>, now: Long): Insights {
        val byStatus: Map<CapsuleStatus, List<Capsule>> = capsules.groupBy { it.statusAt(now) }
        val opened = byStatus[CapsuleStatus.OPENED].orEmpty()
        val sealed = byStatus[CapsuleStatus.SEALED].orEmpty()

        val waits: List<Long> = opened.mapNotNull { capsule ->
            capsule.openedAt?.let { openedAt -> openedAt - capsule.createdAt }
        }
        val shiftCounts: Map<MoodShift, Int> = opened
            .mapNotNull { it.moodShift() }
            .groupingBy { it }
            .eachCount()
        val next: Capsule? = sealed.minByOrNull { it.unlockAt }

        return Insights(
            total = capsules.size,
            sealed = sealed.size,
            ready = byStatus[CapsuleStatus.READY].orEmpty().size,
            opened = opened.size,
            sealingMoods = Mood.entries.associateWith { mood ->
                capsules.count { it.moodAtSealing == mood }
            },
            moodShifts = MoodShift.entries.associateWith { shiftCounts[it] ?: 0 },
            averageWaitMillis = waits.takeIf { it.isNotEmpty() }?.average()?.toLong(),
            longestWaitMillis = waits.maxOrNull(),
            nextUnlock = next,
            nextUnlockInMillis = next?.let { it.unlockAt - now },
        )
    }
}
