package com.capsula.app.ui.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.Mood
import com.capsula.app.domain.Durations
import com.capsula.app.domain.MoodShift
import com.capsula.app.domain.TimeFormatter
import com.capsula.app.domain.moodShift
import com.capsula.app.ui.AppViewModelProvider
import com.capsula.app.ui.components.CountdownRing
import com.capsula.app.ui.components.DecodingText
import com.capsula.app.ui.components.EmptyState
import com.capsula.app.ui.components.LetterCard
import com.capsula.app.ui.components.MoodBadge
import com.capsula.app.ui.components.MoodSelector
import com.capsula.app.ui.components.ObserveAsEvents
import com.capsula.app.ui.components.SectionTitle
import com.capsula.app.ui.components.WaxSealButton
import com.capsula.app.ui.theme.CapsulaTheme
import com.capsula.app.ui.theme.LetterTextStyle

@Composable
fun CapsuleDetailScreen(
    onBack: () -> Unit,
    viewModel: CapsuleDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            CapsuleDetailEvent.Deleted -> onBack()
        }
    }

    // True only if the seal was broken on this screen, so the decoding animation plays once
    // (and is not replayed after rotation, thanks to rememberSaveable).
    var justOpened by rememberSaveable { mutableStateOf(false) }

    CapsuleDetailContent(
        uiState = uiState,
        animateReveal = justOpened,
        onBack = onBack,
        onSealBroken = {
            justOpened = true
            viewModel.openCapsule()
        },
        onSaveReflection = viewModel::saveReflection,
        onDelete = viewModel::deleteCapsule,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapsuleDetailContent(
    uiState: CapsuleDetailUiState,
    animateReveal: Boolean,
    onBack: () -> Unit,
    onSealBroken: () -> Unit,
    onSaveReflection: (Mood, String) -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.capsuleOrNull != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete capsule")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState,
            contentKey = { it::class },
            transitionSpec = {
                (fadeIn(tween(450)) + scaleIn(initialScale = 0.94f, animationSpec = tween(450)))
                    .togetherWith(fadeOut(tween(200)))
            },
            label = "capsuleState",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) { state ->
            when (state) {
                CapsuleDetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                CapsuleDetailUiState.NotFound -> EmptyState(
                    emoji = "🕳️",
                    title = "This capsule no longer exists",
                    body = "It may have been deleted.",
                    actionLabel = "Back to the vault",
                    onAction = onBack,
                )
                is CapsuleDetailUiState.Sealed -> SealedContent(state)
                is CapsuleDetailUiState.Ready -> ReadyContent(state, onSealBroken)
                is CapsuleDetailUiState.Opened -> OpenedContent(state, animateReveal, onSaveReflection)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            title = { Text("Delete this capsule?") },
            text = { Text("The letter will be gone for good, including your reply.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SealedContent(state: CapsuleDetailUiState.Sealed) {
    val capsule = state.capsule
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CountdownRing(progress = state.progress, modifier = Modifier.size(232.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(TimeFormatter.countdown(state.remainingMillis), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "until it opens",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(capsule.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Sealed on ${TimeFormatter.dateTime(capsule.createdAt)}, feeling " +
                "${capsule.moodAtSealing.label.lowercase()} ${capsule.moodAtSealing.emoji}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Opens on ${TimeFormatter.dateTime(capsule.unlockAt)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = state.cipherText,
                    style = LetterTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "The words are here, but time is keeping them. Come back when the countdown ends.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ReadyContent(state: CapsuleDetailUiState.Ready, onSealBroken: () -> Unit) {
    val capsule = state.capsule
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("It's time.", style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "“${capsule.title}” has waited ${TimeFormatter.humanDuration(state.waitedMillis)} for you.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        WaxSealButton(onBroken = onSealBroken)
        Spacer(Modifier.height(20.dp))
        Text("Press and hold the seal to break it", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Written on ${TimeFormatter.date(capsule.createdAt)}, " +
                "while feeling ${capsule.moodAtSealing.label.lowercase()} ${capsule.moodAtSealing.emoji}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OpenedContent(
    state: CapsuleDetailUiState.Opened,
    animateReveal: Boolean,
    onSaveReflection: (Mood, String) -> Unit,
) {
    val capsule = state.capsule
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(capsule.title, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Written ${TimeFormatter.date(capsule.createdAt)}, opened " +
                    TimeFormatter.date(capsule.openedAt ?: capsule.unlockAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LetterCard {
            DecodingText(text = capsule.message, animate = animateReveal, style = LetterTextStyle)
        }

        capsule.questionForFuture?.let { question ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Past you asked", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(question, style = LetterTextStyle.copy(fontStyle = FontStyle.Italic))
                }
            }
        }

        ThenAndNowCard(capsule = capsule, sealedForMillis = state.sealedForMillis)

        if (capsule.hasReflection) {
            ReflectionCard(capsule)
        } else {
            ReflectionForm(onSave = onSaveReflection)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ThenAndNowCard(capsule: Capsule, sealedForMillis: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoodColumn(label = "Then", mood = capsule.moodAtSealing)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                MoodColumn(label = "Now", mood = capsule.moodAtOpening)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = moodShiftMessage(capsule.moodShift()),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Sealed for ${TimeFormatter.humanDuration(sealedForMillis)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MoodColumn(label: String, mood: Mood?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MoodBadge(mood = mood, size = 56.dp)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

private fun moodShiftMessage(shift: MoodShift?): String = when (shift) {
    null -> "How do you feel, reading this now?"
    MoodShift.IMPROVED -> "You feel better than when you wrote this."
    MoodShift.SAME -> "Same feeling, a different moment."
    MoodShift.WORSE -> "Harder times now. Past you was rooting for you."
}

@Composable
private fun ReflectionForm(onSave: (Mood, String) -> Unit) {
    // Local UI state until the user commits it; rememberSaveable keeps it through rotation.
    var mood by rememberSaveable { mutableStateOf<Mood?>(null) }
    var reply by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Reply to your past self")
        MoodSelector(selected = mood, onSelect = { mood = it })
        OutlinedTextField(
            value = reply,
            onValueChange = { reply = it.take(1_000) },
            label = { Text("What would you tell them? (optional)") },
            minLines = 3,
            textStyle = LetterTextStyle,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { mood?.let { onSave(it, reply) } },
            enabled = mood != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save reply") }
    }
}

@Composable
private fun ReflectionCard(capsule: Capsule) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Your reply", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = capsule.reflection ?: "No words, just a feeling: ${capsule.moodAtOpening?.emoji.orEmpty()}",
                style = LetterTextStyle.copy(fontSize = 16.sp, lineHeight = 25.sp),
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun SealedPreview() {
    val now = 1_000_000_000_000L
    val capsule = Capsule(
        1, "Graduation day", "If you're reading this, you made it.", null, Mood.GREAT,
        now - 5 * Durations.DAY, now + 360 * Durations.DAY,
    )
    CapsulaTheme(darkTheme = true) {
        CapsuleDetailContent(
            uiState = CapsuleDetailUiState.Sealed(capsule, 360 * Durations.DAY, 0.02f, "◆▒░ ※§¤∆ ≈#@%&"),
            animateReveal = false, onBack = {}, onSealBroken = {}, onSaveReflection = { _, _ -> }, onDelete = {},
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun OpenedPreview() {
    val now = 1_000_000_000_000L
    val capsule = Capsule(
        3, "First week at university", "I don't know anyone yet and the campus feels huge.",
        "Who are your friends now?", Mood.LOW, now - 200 * Durations.DAY, now - 20 * Durations.DAY,
        openedAt = now - 19 * Durations.DAY, moodAtOpening = Mood.GOOD, reflection = "I laughed.",
    )
    CapsulaTheme(darkTheme = true) {
        CapsuleDetailContent(
            uiState = CapsuleDetailUiState.Opened(capsule, 181 * Durations.DAY),
            animateReveal = false, onBack = {}, onSealBroken = {}, onSaveReflection = { _, _ -> }, onDelete = {},
        )
    }
}
