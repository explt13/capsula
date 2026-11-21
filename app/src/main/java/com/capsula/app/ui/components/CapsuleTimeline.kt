package com.capsula.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capsula.app.data.model.CapsuleEvent
import com.capsula.app.data.model.CapsuleEventType
import com.capsula.app.domain.Durations
import com.capsula.app.domain.TimeFormatter
import com.capsula.app.ui.theme.CapsulaTheme

/**
 * Вертикальная лента истории капсулы: кружок с эмодзи, соединительная линия, название события,
 * пояснение и дата. Данные приходят из таблицы capsule_events через Flow, поэтому новое событие
 * появляется здесь сразу после действия пользователя.
 */
@Composable
fun CapsuleTimeline(
    events: List<CapsuleEvent>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        events.forEachIndexed { index, event ->
            TimelineRow(event = event, isLast = index == events.lastIndex)
        }
    }
}

@Composable
private fun TimelineRow(event: CapsuleEvent, isLast: Boolean) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(event.type.emoji, fontSize = 15.sp)
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.padding(top = 4.dp, bottom = if (isLast) 0.dp else 16.dp)) {
            Text(event.type.label, style = MaterialTheme.typography.titleSmall)
            event.details?.let { details ->
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = TimeFormatter.dateTime(event.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CapsuleTimelinePreview() {
    val now = 1_800_000_000_000L
    CapsulaTheme {
        CapsuleTimeline(
            events = listOf(
                CapsuleEvent(1, 1, CapsuleEventType.DRAFT_CREATED, now - 3 * Durations.DAY, null),
                CapsuleEvent(2, 1, CapsuleEventType.SEALED, now - 2 * Durations.DAY, "Opens on 1 May 2027, 09:00"),
                CapsuleEvent(3, 1, CapsuleEventType.POSTPONED, now, "+1 week"),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
