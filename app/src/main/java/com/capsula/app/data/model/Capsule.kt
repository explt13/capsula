package com.capsula.app.data.model

/**
 * A letter to the future self. All times are epoch milliseconds, which keeps the model
 * simple, easy to test and ready to be stored in Room in the second part of the course.
 */
data class Capsule(
    val id: Long,
    val title: String,
    val message: String,
    val questionForFuture: String?,
    val moodAtSealing: Mood,
    val createdAt: Long,
    val unlockAt: Long,
    val openedAt: Long? = null,
    val moodAtOpening: Mood? = null,
    val reflection: String? = null,
) {
    /** The status is derived from time, never stored, so it can't get out of sync. */
    fun statusAt(now: Long): CapsuleStatus = when {
        openedAt != null -> CapsuleStatus.OPENED
        now >= unlockAt -> CapsuleStatus.READY
        else -> CapsuleStatus.SEALED
    }

    fun remainingMillis(now: Long): Long = (unlockAt - now).coerceAtLeast(0L)

    /** 0f right after sealing, 1f when the capsule can be opened. */
    fun sealProgress(now: Long): Float {
        val total = (unlockAt - createdAt).coerceAtLeast(1L)
        return ((now - createdAt).toFloat() / total).coerceIn(0f, 1f)
    }

    val hasReflection: Boolean get() = moodAtOpening != null
}

enum class CapsuleStatus { SEALED, READY, OPENED }

/** Everything the user provides when sealing a new capsule. */
data class NewCapsule(
    val title: String,
    val message: String,
    val questionForFuture: String?,
    val moodAtSealing: Mood,
    val unlockAt: Long,
)
