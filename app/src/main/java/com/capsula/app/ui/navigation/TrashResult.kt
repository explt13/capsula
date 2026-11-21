package com.capsula.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class TrashedCapsule(val id: Long, val title: String)

/**
 * Передача результата «назад» по навигации: экран деталей кладёт сюда id капсулы, которую убрали в корзину,
 * а экран, на который мы вернулись (Vault или Insights), показывает snackbar с кнопкой Undo.
 * Используется SavedStateHandle записи back stack, поэтому результат переживает поворот экрана.
 */
object TrashResult {
    private const val KEY_ID = "trashed_capsule_id"
    private const val KEY_TITLE = "trashed_capsule_title"

    fun send(handle: SavedStateHandle?, capsule: TrashedCapsule) {
        handle ?: return
        handle[KEY_TITLE] = capsule.title
        handle[KEY_ID] = capsule.id
    }

    @Composable
    fun collect(handle: SavedStateHandle): TrashedCapsule? {
        val id by handle.getStateFlow<Long?>(KEY_ID, null).collectAsStateWithLifecycle()
        val title: String = handle[KEY_TITLE] ?: ""
        return id?.let { TrashedCapsule(it, title) }
    }

    fun clear(handle: SavedStateHandle) {
        handle[KEY_ID] = null
        handle[KEY_TITLE] = null
    }
}
