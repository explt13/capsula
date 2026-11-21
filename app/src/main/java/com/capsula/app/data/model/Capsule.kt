package com.capsula.app.data.model

// Модель данных капсулы, которую используют ViewModel и UI. Не зависит от Room:
// репозиторий переводит её из CapsuleEntity и обратно.
data class Capsule(
    val id: Long,
    val title: String,
    val message: String,
    val questionForFuture: String?,
    // Настроение в момент написания. У черновика может быть ещё не выбрано.
    val moodAtSealing: Mood?,
    val createdAt: Long,
    val updatedAt: Long,
    // null, пока капсула остаётся черновиком.
    val sealedAt: Long?,
    // null, пока капсула остаётся черновиком.
    val unlockAt: Long?,
    val openedAt: Long? = null,
    val moodAtOpening: Mood? = null,
    val reflection: String? = null,
    // Сколько раз открытие откладывали (максимум задан в CapsuleRules).
    val postponeCount: Int = 0,
    // Время перемещения в корзину. null, если капсула не в корзине.
    val trashedAt: Long? = null,
) {

    // Статус не хранится в базе, а вычисляется из полей и текущего времени, поэтому не может рассинхронизироваться.
    fun statusAt(now: Long): CapsuleStatus = when {
        sealedAt == null || unlockAt == null -> CapsuleStatus.DRAFT
        openedAt != null -> CapsuleStatus.OPENED
        now >= unlockAt -> CapsuleStatus.READY
        else -> CapsuleStatus.SEALED
    }

    fun remainingMillis(now: Long): Long = ((unlockAt ?: now) - now).coerceAtLeast(0L)

    // Прогресс ожидания от 0 (только запечатана) до 1 (можно открывать).
    fun sealProgress(now: Long): Float {
        val start = sealedAt ?: return 0f
        val end = unlockAt ?: return 0f
        val total = (end - start).coerceAtLeast(1L)
        return ((now - start).toFloat() / total).coerceIn(0f, 1f)
    }

    val isDraft: Boolean get() = sealedAt == null
    val isTrashed: Boolean get() = trashedAt != null
    val hasReflection: Boolean get() = moodAtOpening != null
}

// Этапы жизненного цикла: DRAFT → SEALED → READY → OPENED.
enum class CapsuleStatus(val label: String) {
    DRAFT("Draft"),
    SEALED("Sealed"),
    READY("Ready"),
    OPENED("Opened"),
}

// Содержимое письма, которое пользователь вводит в форме (и для черновика, и для запечатывания).
data class CapsuleContent(
    val title: String,
    val message: String,
    val questionForFuture: String?,
    val mood: Mood?,
)
