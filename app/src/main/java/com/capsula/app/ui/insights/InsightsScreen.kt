package com.capsula.app.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.Mood
import com.capsula.app.domain.Durations
import com.capsula.app.domain.Insights
import com.capsula.app.domain.InsightsCalculator
import com.capsula.app.domain.MoodShift
import com.capsula.app.domain.TimeFormatter
import com.capsula.app.ui.AppViewModelProvider
import com.capsula.app.ui.components.CapsulaBottomBar
import com.capsula.app.ui.components.EmptyState
import com.capsula.app.ui.components.MoodBarChart
import com.capsula.app.ui.components.SectionTitle
import com.capsula.app.ui.components.StatTile
import com.capsula.app.ui.navigation.TopLevelDestination
import com.capsula.app.ui.theme.CapsulaTheme

@Composable
fun InsightsScreen(
    onCapsuleClick: (Long) -> Unit,
    onNavigateToTab: (TopLevelDestination) -> Unit,
    viewModel: InsightsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    InsightsContent(uiState = uiState, onCapsuleClick = onCapsuleClick, onNavigateToTab = onNavigateToTab)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsContent(
    uiState: InsightsUiState,
    onCapsuleClick: (Long) -> Unit,
    onNavigateToTab: (TopLevelDestination) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Insights") }) },
        bottomBar = { CapsulaBottomBar(selected = TopLevelDestination.INSIGHTS, onSelect = onNavigateToTab) },
    ) { innerPadding ->
        val insights = uiState.insights
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                insights == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                insights.isEmpty -> EmptyState(
                    emoji = "🔭",
                    title = "Nothing to look back on yet",
                    body = "Seal your first capsule in the Vault. Patterns in your moods will appear here.",
                )
                else -> InsightsBody(insights = insights, onCapsuleClick = onCapsuleClick)
            }
        }
    }
}

@Composable
private fun InsightsBody(insights: Insights, onCapsuleClick: (Long) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(insights.sealed.toString(), "Sealed", "🔒", Modifier.weight(1f))
            StatTile(insights.ready.toString(), "Ready", "✨", Modifier.weight(1f))
            StatTile(insights.opened.toString(), "Opened", "✉️", Modifier.weight(1f))
        }

        val next = insights.nextUnlock
        if (next != null && insights.nextUnlockInMillis != null) {
            NextUnlockCard(capsule = next, remainingMillis = insights.nextUnlockInMillis, onClick = { onCapsuleClick(next.id) })
        }

        SectionTitle("Your mood when sealing")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            MoodBarChart(counts = insights.sealingMoods, modifier = Modifier.padding(16.dp))
        }

        SectionTitle("Then and now")
        if (insights.reflectedCount == 0) {
            Text(
                text = "Open a capsule and reply to it to see how your feelings changed over time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile((insights.moodShifts[MoodShift.IMPROVED] ?: 0).toString(), "Felt better", "🌱", Modifier.weight(1f))
                StatTile((insights.moodShifts[MoodShift.SAME] ?: 0).toString(), "Felt the same", "🌗", Modifier.weight(1f))
                StatTile((insights.moodShifts[MoodShift.WORSE] ?: 0).toString(), "Felt worse", "🌧️", Modifier.weight(1f))
            }
        }

        insights.averageWaitMillis?.let { average ->
            Text(
                text = "On average a letter waits ${TimeFormatter.humanDuration(average)} before you read it. " +
                    "The longest wait so far: ${TimeFormatter.humanDuration(insights.longestWaitMillis ?: average)}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun NextUnlockCard(capsule: Capsule, remainingMillis: Long, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⏳", fontSize = 32.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Next to open", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = capsule.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(TimeFormatter.countdown(remainingMillis), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun InsightsPreview() {
    val now = 1_000_000_000_000L
    val capsules = listOf(
        Capsule(1, "A", "...", null, Mood.ANXIOUS, now - 30 * Durations.DAY, now + Durations.HOUR),
        Capsule(
            2, "B", "...", null, Mood.LOW, now - 200 * Durations.DAY, now - 20 * Durations.DAY,
            openedAt = now - 19 * Durations.DAY, moodAtOpening = Mood.GOOD,
        ),
    )
    CapsulaTheme(darkTheme = true) {
        InsightsContent(
            uiState = InsightsUiState(isLoading = false, insights = InsightsCalculator.calculate(capsules, now)),
            onCapsuleClick = {},
            onNavigateToTab = {},
        )
    }
}
