package com.capsula.app.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.capsula.app.ui.navigation.TrashedCapsule
import kotlinx.coroutines.launch

/**
 * Показывает snackbar «… moved to trash» с кнопкой Undo, когда экран деталей вернул результат.
 * Результат сразу очищается (onShown), чтобы snackbar не появился повторно после поворота экрана.
 * Сам snackbar запускается в отдельном scope: очистка результата меняет ключ эффекта и иначе отменила бы его.
 */
@Composable
fun TrashUndoEffect(
    trashed: TrashedCapsule?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit,
    onUndo: (Long) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val currentOnShown by rememberUpdatedState(onShown)
    val currentOnUndo by rememberUpdatedState(onUndo)
    LaunchedEffect(trashed) {
        val capsule = trashed ?: return@LaunchedEffect
        currentOnShown()
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "“${capsule.title}” moved to trash",
                actionLabel = "Undo",
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) currentOnUndo(capsule.id)
        }
    }
}
