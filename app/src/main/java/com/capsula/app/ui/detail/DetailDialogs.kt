package com.capsula.app.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.capsula.app.domain.CapsuleValidator
import com.capsula.app.domain.PostponeOption
import com.capsula.app.domain.TimeFormatter

// Подтверждение удаления. Капсула не удаляется сразу, а уходит в корзину, откуда её можно вернуть.
@Composable
fun MoveToTrashDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
        title = { Text("Move to trash?") },
        text = { Text("“$title” and its history move to the trash. You can restore them from there.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Move to trash") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// Переименование запечатанной капсулы: меняется только заголовок, текст письма остаётся запечатанным.
@Composable
fun RenameDialog(currentTitle: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var title by rememberSaveable { mutableStateOf(currentTitle) }
    val error = CapsuleValidator.titleError(title)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
        title = { Text("Rename capsule") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(CapsuleValidator.TITLE_MAX_LENGTH) },
                label = { Text("Title") },
                singleLine = true,
                isError = error != null,
                supportingText = { Text(error ?: "The letter itself stays sealed.") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title) }, enabled = error == null) { Text("Rename") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// Выбор, на сколько отложить открытие. Показывает новую дату для каждого варианта и остаток попыток.
@Composable
fun PostponeDialog(state: PostponeState, onConfirm: (PostponeOption) -> Unit, onDismiss: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf(PostponeOption.ONE_WEEK) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
        title = { Text("Not ready to read it?") },
        text = {
            Column {
                Text(
                    text = "You can push the opening later, never earlier. " +
                        "${state.left} of ${state.max} postponements left.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Column(Modifier.selectableGroup()) {
                    PostponeOption.entries.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = option == selected,
                                    onClick = { selected = option },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 4.dp),
                        ) {
                            RadioButton(selected = option == selected, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(option.label, style = MaterialTheme.typography.titleSmall)
                                state.preview[option]?.let { unlockAt ->
                                    Text(
                                        text = "Opens on ${TimeFormatter.dateTime(unlockAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("Postpone") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
