package com.capsula.app.data.model

// Одна запись в истории капсулы. Хранится в отдельной таблице capsule_events.
data class CapsuleEvent(
    val id: Long,
    val capsuleId: Long,
    val type: CapsuleEventType,
    val timestamp: Long,
    // Короткое пояснение, например "+1 week" или новое название.
    val details: String?,
)

// Все переходы и действия, которые попадают в историю.
enum class CapsuleEventType(val label: String, val emoji: String) {
    DRAFT_CREATED("Draft started", "📝"),
    DRAFT_EDITED("Draft edited", "✏️"),
    SEALED("Sealed", "🔒"),
    RENAMED("Renamed", "🏷️"),
    POSTPONED("Opening postponed", "⏳"),
    OPENED("Seal broken", "✉️"),
    REPLIED("Replied to past self", "💬"),
    REPLY_EDITED("Reply edited", "🖊️"),
    TRASHED("Moved to trash", "🗑️"),
    RESTORED("Restored from trash", "♻️"),
}

// Событие вместе с названием капсулы: для ленты «Recent activity» на экране Insights.
data class ActivityItem(
    val event: CapsuleEvent,
    val capsuleTitle: String,
    val capsuleTrashed: Boolean,
)
