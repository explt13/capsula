package com.capsula.app.data.model

/// Модель данных для капсулы. Содержит информацию о заголовке, сообщении, настроении при запечатывании, времени создания, времени открытия и размышлениях пользователя.
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

    // Возвращает статус капсулы в зависимости от текущего времени и состояния открытия.
    fun statusAt(now: Long): CapsuleStatus = when {
        openedAt != null -> CapsuleStatus.OPENED
        now >= unlockAt -> CapsuleStatus.READY
        else -> CapsuleStatus.SEALED
    }

    fun remainingMillis(now: Long): Long = (unlockAt - now).coerceAtLeast(0L)

    // Возвращает прогресс запечатывания капсулы в виде значения от 0 до 1, где 0 означает, что капсула только что была создана, а 1 означает, что капсула готова к открытию.
    fun sealProgress(now: Long): Float {
        val total = (unlockAt - createdAt).coerceAtLeast(1L)
        return ((now - createdAt).toFloat() / total).coerceIn(0f, 1f)
    }

    val hasReflection: Boolean get() = moodAtOpening != null
}

enum class CapsuleStatus { SEALED, READY, OPENED }

// Модель данных для новой капсулы, которая еще не была добавлена в репозиторий. Используется при создании капсулы пользователем.
data class NewCapsule(
    val title: String,
    val message: String,
    val questionForFuture: String?,
    val moodAtSealing: Mood,
    val unlockAt: Long,
)
