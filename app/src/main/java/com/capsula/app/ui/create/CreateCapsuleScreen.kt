package com.capsula.app.ui.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capsula.app.data.model.Mood
import com.capsula.app.domain.CapsuleValidator
import com.capsula.app.domain.TimeFormatter
import com.capsula.app.domain.UnlockTime
import com.capsula.app.ui.AppViewModelProvider
import com.capsula.app.ui.components.MoodSelector
import com.capsula.app.ui.components.ObserveAsEvents
import com.capsula.app.ui.components.SectionTitle
import com.capsula.app.ui.theme.CapsulaTheme
import com.capsula.app.ui.theme.LetterTextStyle
import java.time.LocalDate

@Composable
fun CreateCapsuleScreen(
    onBack: () -> Unit,
    // sealed = true, если письмо запечатано; false, если сохранён черновик.
    onSaved: (capsuleId: Long, sealed: Boolean) -> Unit,
    viewModel: CreateCapsuleViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CreateCapsuleEvent.Saved -> onSaved(event.capsuleId, event.sealed)
            CreateCapsuleEvent.NotEditable -> onBack()
        }
    }

    CreateCapsuleContent(
        uiState = uiState,
        onBack = onBack,
        onTitleChange = viewModel::onTitleChange,
        onMessageChange = viewModel::onMessageChange,
        onQuestionChange = viewModel::onQuestionChange,
        onMoodSelected = viewModel::onMoodSelected,
        onPresetSelected = viewModel::onPresetSelected,
        onCustomDatePicked = viewModel::onCustomDatePicked,
        onSealClick = viewModel::onSealClick,
        onSaveDraftClick = viewModel::onSaveDraftClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateCapsuleContent(
    uiState: CreateCapsuleUiState,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onQuestionChange: (String) -> Unit,
    onMoodSelected: (Mood) -> Unit,
    onPresetSelected: (UnlockPreset) -> Unit,
    onCustomDatePicked: (Long) -> Unit,
    onSealClick: () -> Unit,
    onSaveDraftClick: () -> Unit,
) {
    // Pure UI state: dialogs are not business logic, so they live in the composable.
    // rememberSaveable keeps them open after rotation.
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    val form = uiState.form

    val requestExit: () -> Unit = {
        if (uiState.isDirty) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }
    BackHandler(enabled = uiState.isDirty) { showDiscardDialog = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.mode == FormMode.EDIT_DRAFT) "Edit draft" else "New capsule") },
                navigationIcon = {
                    IconButton(onClick = requestExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Write to the person you will be. Save it as a draft and come back later, " +
                    "or seal it: once sealed, the letter can't be read or edited until it opens.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = form.title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                singleLine = true,
                isError = uiState.titleError != null,
                supportingText = {
                    Text(uiState.titleError ?: "${form.title.length}/${CapsuleValidator.TITLE_MAX_LENGTH}")
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.message,
                onValueChange = onMessageChange,
                label = { Text("Your letter") },
                placeholder = { Text("Dear future me…") },
                minLines = 6,
                textStyle = LetterTextStyle,
                isError = uiState.messageError != null,
                supportingText = {
                    Text(uiState.messageError ?: "${form.message.length}/${CapsuleValidator.MESSAGE_MAX_LENGTH}")
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.question,
                onValueChange = onQuestionChange,
                label = { Text("A question for future you (optional)") },
                placeholder = { Text("Did you keep your promise?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle("How do you feel right now?")
            MoodSelector(selected = form.mood, onSelect = onMoodSelected)
            uiState.moodError?.let { ErrorText(it) }

            SectionTitle("Open it in")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                UnlockPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = form.preset == preset,
                        onClick = {
                            if (preset == UnlockPreset.CUSTOM) showDatePicker = true
                            onPresetSelected(preset)
                        },
                        label = { Text(preset.label) },
                        leadingIcon = if (preset == UnlockPreset.CUSTOM) {
                            { Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else {
                            null
                        },
                    )
                }
            }
            when {
                uiState.unlockError != null -> ErrorText(uiState.unlockError)
                uiState.unlockAtPreview != null -> Text(
                    text = "Opens on ${TimeFormatter.dateTime(uiState.unlockAtPreview)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                else -> Text(
                    text = "Pick a day for the capsule to open.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onSealClick,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Seal capsule")
                }
            }
            // Черновик сохраняет текст без даты открытия: его можно дописать и запечатать позже.
            OutlinedButton(
                onClick = onSaveDraftClick,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (uiState.mode == FormMode.EDIT_DRAFT) "Save draft" else "Save as draft")
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = form.customDateMillis,
            selectableDates = FutureDates,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(onCustomDatePicked)
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null,
                ) { Text("Set date") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(if (uiState.mode == FormMode.EDIT_DRAFT) "Discard changes?" else "Discard this letter?") },
            text = {
                Text(
                    if (uiState.mode == FormMode.EDIT_DRAFT) "The draft will stay as it was before you opened it."
                    else "What you wrote will be lost. Save it as a draft to keep it.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onBack()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep writing") }
            },
        )
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}

/** Only days after today can be chosen in the DatePicker. */
@OptIn(ExperimentalMaterial3Api::class)
private object FutureDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        UnlockTime.isSelectablePickerDate(utcTimeMillis)

    override fun isSelectableYear(year: Int): Boolean = year >= LocalDate.now().year
}

@Preview(showSystemUi = true)
@Composable
private fun CreateCapsuleContentPreview() {
    CapsulaTheme(darkTheme = true) {
        CreateCapsuleContent(
            uiState = CreateCapsuleUiState(
                form = CreateCapsuleForm(title = "Before the exam", message = "Dear future me,", mood = Mood.ANXIOUS),
                unlockAtPreview = 1_800_000_000_000L,
            ),
            onBack = {}, onTitleChange = {}, onMessageChange = {}, onQuestionChange = {},
            onMoodSelected = {}, onPresetSelected = {}, onCustomDatePicked = {}, onSealClick = {}, onSaveDraftClick = {},
        )
    }
}
