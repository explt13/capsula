package com.capsula.app.ui.vault

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.Mood
import com.capsula.app.domain.Durations
import com.capsula.app.ui.AppViewModelProvider
import com.capsula.app.ui.components.CapsulaBottomBar
import com.capsula.app.ui.components.EmptyState
import com.capsula.app.ui.navigation.TopLevelDestination
import com.capsula.app.ui.theme.CapsulaTheme

// Экран Vault, отображающий список капсул, фильтры и состояние пустого экрана.
@Composable
fun VaultScreen(
    onCapsuleClick: (Long) -> Unit,
    onCreateClick: () -> Unit,
    onNavigateToTab: (TopLevelDestination) -> Unit,
    viewModel: VaultViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    VaultContent(
        uiState = uiState,
        onFilterSelected = viewModel::onFilterSelected,
        onCapsuleClick = onCapsuleClick,
        onCreateClick = onCreateClick,
        onNavigateToTab = onNavigateToTab,
    )
}

// UI для экрана Vault. Отвечает за отображение списка капсул, фильтров и состояния пустого экрана.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultContent(
    uiState: VaultUiState,
    onFilterSelected: (VaultFilter) -> Unit,
    onCapsuleClick: (Long) -> Unit,
    onCreateClick: () -> Unit,
    onNavigateToTab: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Capsula") },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            CapsulaBottomBar(selected = TopLevelDestination.VAULT, onSelect = onNavigateToTab)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Seal a capsule") },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "intro") {
                Text(
                    text = "Letters to your future self. Each one stays sealed until its moment.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (uiState.readyCount > 0 && uiState.selectedFilter != VaultFilter.READY) {
                item(key = "readyBanner") {
                    ReadyBanner(
                        count = uiState.readyCount,
                        onClick = { onFilterSelected(VaultFilter.READY) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            item(key = "filters") {
                VaultFilterRow(
                    selected = uiState.selectedFilter,
                    counts = uiState.counts,
                    onSelect = onFilterSelected,
                )
            }
            if (!uiState.isLoading && uiState.items.isEmpty()) {
                item(key = "empty") {
                    VaultEmptyState(filter = uiState.selectedFilter, onCreateClick = onCreateClick)
                }
            }
            items(items = uiState.items, key = { it.capsule.id }) { capsuleItem ->
                CapsuleCard(
                    item = capsuleItem,
                    onClick = { onCapsuleClick(capsuleItem.capsule.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun ReadyBanner(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = if (count == 1) "A capsule is ready to open" else "$count capsules are ready to open",
                style = MaterialTheme.typography.titleMedium,
            )
            Text("Tap to see them.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun VaultFilterRow(
    selected: VaultFilter,
    counts: Map<VaultFilter, Int>,
    onSelect: (VaultFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VaultFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(filter) },
                label = { Text("${filter.label} (${counts[filter] ?: 0})") },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun VaultEmptyState(filter: VaultFilter, onCreateClick: () -> Unit) {
    when (filter) {
        VaultFilter.ALL -> EmptyState(
            emoji = "📭",
            title = "Your vault is empty",
            body = "Write a letter to your future self. It stays sealed until the date you choose.",
            actionLabel = "Seal a capsule",
            onAction = onCreateClick,
        )
        VaultFilter.SEALED -> EmptyState(
            emoji = "🔓",
            title = "Nothing is sealed",
            body = "Every capsule has reached its moment. Seal a new one for later.",
            actionLabel = "Seal a capsule",
            onAction = onCreateClick,
        )
        VaultFilter.READY -> EmptyState(
            emoji = "⏳",
            title = "Nothing to open yet",
            body = "Sealed capsules appear here when their countdown ends.",
        )
        VaultFilter.OPENED -> EmptyState(
            emoji = "✉️",
            title = "No opened capsules",
            body = "Letters you have opened, and your replies to them, are kept here.",
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun VaultContentPreview() {
    val now = 1_000_000_000_000L
    val items = listOf(
        Capsule(1, "Hello from last month", "...", null, Mood.ANXIOUS, now - 30 * Durations.DAY, now - Durations.HOUR),
        Capsule(2, "Right before the demo", "...", null, Mood.GOOD, now - 10 * Durations.MINUTE, now + 3 * Durations.MINUTE),
        Capsule(3, "Graduation day", "...", null, Mood.GREAT, now - 5 * Durations.DAY, now + 365 * Durations.DAY),
    ).map { it.toItemUi(now) }
    CapsulaTheme(darkTheme = true) {
        VaultContent(
            uiState = VaultUiState(
                isLoading = false,
                items = items,
                counts = mapOf(VaultFilter.ALL to 3, VaultFilter.SEALED to 2, VaultFilter.READY to 1),
            ),
            onFilterSelected = {},
            onCapsuleClick = {},
            onCreateClick = {},
            onNavigateToTab = {},
        )
    }
}
