package com.capsula.app.data.local

import com.capsula.app.data.model.ActivityItem
import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.CapsuleEvent

// Перевод между таблицами Room и моделями, которые видит остальное приложение.

fun CapsuleEntity.toDomain(): Capsule = Capsule(
    id = id,
    title = title,
    message = message,
    questionForFuture = questionForFuture,
    moodAtSealing = moodAtSealing,
    createdAt = createdAt,
    updatedAt = updatedAt,
    sealedAt = sealedAt,
    unlockAt = unlockAt,
    openedAt = openedAt,
    moodAtOpening = moodAtOpening,
    reflection = reflection,
    postponeCount = postponeCount,
    trashedAt = trashedAt,
)

fun Capsule.toEntity(): CapsuleEntity = CapsuleEntity(
    id = id,
    title = title,
    message = message,
    questionForFuture = questionForFuture,
    moodAtSealing = moodAtSealing,
    createdAt = createdAt,
    updatedAt = updatedAt,
    sealedAt = sealedAt,
    unlockAt = unlockAt,
    openedAt = openedAt,
    moodAtOpening = moodAtOpening,
    reflection = reflection,
    postponeCount = postponeCount,
    trashedAt = trashedAt,
)

fun CapsuleEventEntity.toDomain(): CapsuleEvent = CapsuleEvent(
    id = id,
    capsuleId = capsuleId,
    type = type,
    timestamp = timestamp,
    details = details,
)

fun ActivityRow.toDomain(): ActivityItem = ActivityItem(
    event = event.toDomain(),
    capsuleTitle = capsuleTitle,
    capsuleTrashed = capsuleTrashedAt != null,
)
