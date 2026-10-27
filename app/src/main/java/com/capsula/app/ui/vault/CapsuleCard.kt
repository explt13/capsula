package com.capsula.app.ui.vault

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.CapsuleStatus
import com.capsula.app.data.model.Mood
import com.capsula.app.domain.Durations
import com.capsula.app.domain.TimeFormatter
import com.capsula.app.ui.components.MoodBadge
import com.capsula.app.ui.theme.CapsulaTheme

@Composable
fun CapsuleCard(
    item: CapsuleItemUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val capsule = item.capsule
    val borderColor = if (item.status == CapsuleStatus.READY) readyGlowColor() else Color.Transparent

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoodBadge(mood = capsule.moodAtSealing)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = capsule.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                when (item.status) {
                    CapsuleStatus.SEALED -> {
                        Text(
                            text = "Opens in ${TimeFormatter.countdown(item.remainingMillis)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { item.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                        )
                    }
                    CapsuleStatus.READY -> Text(
                        text = "Ready to open",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    CapsuleStatus.OPENED -> Text(
                        text = "Opened ${TimeFormatter.humanDuration(item.sinceOpenedMillis ?: 0L)} ago",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            val (icon, tint) = when (item.status) {
                CapsuleStatus.SEALED -> Icons.Filled.Lock to MaterialTheme.colorScheme.onSurfaceVariant
                CapsuleStatus.READY -> Icons.Filled.Star to MaterialTheme.colorScheme.primary
                CapsuleStatus.OPENED -> Icons.Filled.CheckCircle to MaterialTheme.colorScheme.secondary
            }
            Icon(icon, contentDescription = item.status.name.lowercase(), tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

/** A slow, gentle pulse that draws the eye to capsules that can be opened. */
@Composable
private fun readyGlowColor(): Color {
    val transition = rememberInfiniteTransition(label = "readyGlow")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1_100), RepeatMode.Reverse),
        label = "readyGlowAlpha",
    )
    return MaterialTheme.colorScheme.primary.copy(alpha = alpha)
}

@Preview
@Composable
private fun CapsuleCardPreview() {
    val now = 1_000_000_000L
    val capsule = Capsule(
        id = 1, title = "Right before the demo", message = "...", questionForFuture = null,
        moodAtSealing = Mood.GOOD, createdAt = now - Durations.HOUR, unlockAt = now + Durations.HOUR,
    )
    CapsulaTheme(darkTheme = true) {
        CapsuleCard(item = capsule.toItemUi(now), onClick = {}, modifier = Modifier.padding(16.dp))
    }
}
