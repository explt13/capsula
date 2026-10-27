package com.capsula.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capsula.app.data.model.Mood

/** Horizontal bars showing how often each mood was chosen. Bars grow with an animation. */
@Composable
fun MoodBarChart(
    counts: Map<Mood, Int>,
    modifier: Modifier = Modifier,
) {
    val max = (counts.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val barBrush = Brush.horizontalGradient(
        listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary),
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Mood.entries.forEach { mood ->
            val count = counts[mood] ?: 0
            val fraction by animateFloatAsState(
                targetValue = count / max.toFloat(),
                animationSpec = tween(durationMillis = 700),
                label = "bar_${mood.name}",
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(mood.emoji, fontSize = 20.sp, modifier = Modifier.width(34.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(CircleShape)
                            .background(barBrush),
                    )
                }
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(32.dp),
                )
            }
        }
    }
}
