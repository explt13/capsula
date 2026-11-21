package com.capsula.app.ui.insights

import androidx.compose.foundation.clickable
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import com.capsula.app.data.model.ActivityItem
import com.capsula.app.ui.components.TrashUndoEffect
import com.capsula.app.ui.navigation.TrashedCapsule
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
    trashedCapsule: TrashedCapsule?,
    onTrashedCapsuleShown: () -> Unit,
    viewModel: InsightsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    TrashUndoEffect(
        trashed = trashedCapsule,
        snackbarHostState = snackbarHostState,
        onShown = onTrashedCapsuleShown,
        onUndo = viewModel::restoreFromTrash,
    )
    InsightsContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onCapsuleClick = onCapsuleClick,
        onNavigateToTab = onNavigateToTab,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsContent(
    uiState: InsightsUiState,
    snackbarHostState: SnackbarHostState,
    onCapsuleClick: (Long) -> Unit,
    onNavigateToTab: (TopLevelDestination) -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                else -> InsightsBody(
                    insights = insights,
                    recentActivity = uiState.recentActivity,
                    now = uiState.now,
                    onCapsuleClick = onCapsuleClick,
                )
            }
        }
    }
}

@Composable
private fun InsightsBody(
    insights: Insights,
    recentActivity: List<ActivityItem>,
    now: Long,
    onCapsuleClick: (Long) -> Unit,
) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(insights.drafts.toString(), "Drafts in progress", "📝", Modifier.weight(1f))
            StatTile(insights.totalPostpones.toString(), "Times postponed", "⏳", Modifier.weight(1f))
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

        if (recentActivity.isNotEmpty()) {
            SectionTitle("Recent activity")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    recentActivity.forEach { item ->
                        ActivityRow(
                            item = item,
                            now = now,
                            onClick = { onCapsuleClick(item.event.capsuleId) },
                        )
                    }
                }
            }
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

// Одна строка ленты активности. Капсулы из корзины открыть нельзя, поэтому строка для них не кликабельна.
@Composable
private fun ActivityRow(item: ActivityItem, now: Long, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !item.capsuleTrashed, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(item.event.type.emoji, fontSize = 20.sp, modifier = Modifier.width(32.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.capsuleTitle,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.event.type.label + if (item.capsuleTrashed) " (in trash)" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${TimeFormatter.humanDuration(now - item.event.timestamp)} ago",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun InsightsPreview() {
    val now = 1_000_000_000_000L
    val capsules = listOf(
        Capsule(
            id = 1, title = "A", message = "...", questionForFuture = null, moodAtSealing = Mood.ANXIOUS,
            createdAt = now - 30 * Durations.DAY, updatedAt = now - 30 * Durations.DAY,
            sealedAt = now - 30 * Durations.DAY, unlockAt = now + Durations.HOUR,
        ),
        Capsule(
            id = 2, title = "B", message = "...", questionForFuture = null, moodAtSealing = Mood.LOW,
            createdAt = now - 200 * Durations.DAY, updatedAt = now - 19 * Durations.DAY,
            sealedAt = now - 200 * Durations.DAY, unlockAt = now - 20 * Durations.DAY,
            openedAt = now - 19 * Durations.DAY, moodAtOpening = Mood.GOOD,
        ),
    )
    CapsulaTheme(darkTheme = true) {
        InsightsContent(
            uiState = InsightsUiState(isLoading = false, insights = InsightsCalculator.calculate(capsules, now), now = now),
            snackbarHostState = remember { SnackbarHostState() },
            onCapsuleClick = {},
            onNavigateToTab = {},
        )
    }
}
