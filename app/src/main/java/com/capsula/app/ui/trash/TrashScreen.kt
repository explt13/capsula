package com.capsula.app.ui.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.CapsuleStatus
import com.capsula.app.data.model.Mood
import com.capsula.app.domain.Durations
import com.capsula.app.domain.TimeFormatter
import com.capsula.app.ui.AppViewModelProvider
import com.capsula.app.ui.components.EmptyState
import com.capsula.app.ui.components.MoodBadge
import com.capsula.app.ui.components.ObserveAsEvents
import com.capsula.app.ui.theme.CapsulaTheme
import kotlinx.coroutines.launch

@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.messages) { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    TrashContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onRestore = viewModel::restore,
        onDeleteForever = viewModel::deleteForever,
        onEmptyTrash = viewModel::emptyTrash,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashContent(
    uiState: TrashUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRestore: (Capsule) -> Unit,
    onDeleteForever: (Capsule) -> Unit,
    onEmptyTrash: () -> Unit,
) {
    // id капсулы, для которой открыт диалог «удалить навсегда» (-1 — диалог закрыт).
    var pendingDeleteId by rememberSaveable { mutableStateOf(-1L) }
    var showEmptyDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Trash") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.items.isNotEmpty()) {
                        TextButton(onClick = { showEmptyDialog = true }) { Text("Empty trash") }
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            uiState.items.isEmpty() -> EmptyState(
                emoji = "🧺",
                title = "Trash is empty",
                body = "Capsules you delete land here first, so you can change your mind.",
                modifier = Modifier.padding(innerPadding),
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "hint") {
                    Text(
                        text = "Restore a capsule to put it back with its history, or delete it for good.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(items = uiState.items, key = { it.capsule.id }) { item ->
                    TrashCard(
                        item = item,
                        onRestore = { onRestore(item.capsule) },
                        onDelete = { pendingDeleteId = item.capsule.id },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    val pending = uiState.items.firstOrNull { it.capsule.id == pendingDeleteId }?.capsule
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = -1L },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            title = { Text("Delete forever?") },
            text = { Text("“${pending.title}” and its whole history will be erased. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteId = -1L
                    onDeleteForever(pending)
                }) { Text("Delete forever") }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteId = -1L }) { Text("Cancel") } },
        )
    }
    if (showEmptyDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyDialog = false },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            title = { Text("Empty the trash?") },
            text = { Text("${uiState.items.size} capsule(s) will be erased for good. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showEmptyDialog = false
                    onEmptyTrash()
                }) { Text("Empty trash") }
            },
            dismissButton = { TextButton(onClick = { showEmptyDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TrashCard(
    item: TrashItemUi,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoodBadge(mood = item.capsule.moodAtSealing, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.capsule.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${item.status.label}, trashed ${TimeFormatter.humanDuration(item.trashedAgoMillis)} ago",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRestore, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Restore")
                }
                TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Text("Delete forever", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun TrashPreview() {
    val now = 1_800_000_000_000L
    val capsule = Capsule(
        id = 1, title = "Old resolutions", message = "...", questionForFuture = null, moodAtSealing = Mood.OKAY,
        createdAt = now - 10 * Durations.DAY, updatedAt = now - 10 * Durations.DAY, sealedAt = now - 10 * Durations.DAY,
        unlockAt = now + 10 * Durations.DAY, trashedAt = now - Durations.HOUR,
    )
    CapsulaTheme(darkTheme = true) {
        TrashContent(
            uiState = TrashUiState(
                isLoading = false,
                items = listOf(TrashItemUi(capsule, CapsuleStatus.SEALED, Durations.HOUR)),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {}, onRestore = {}, onDeleteForever = {}, onEmptyTrash = {},
        )
    }
}
