package com.capsula.app.ui.detail

import com.capsula.app.data.model.Capsule

/**
 * Each state of a capsule gets its own type, so the UI can't show, for example,
 * the letter text of a capsule that is still sealed.
 */
sealed interface CapsuleDetailUiState {
    data object Loading : CapsuleDetailUiState

    data object NotFound : CapsuleDetailUiState

    data class Sealed(
        val capsule: Capsule,
        val remainingMillis: Long,
        val progress: Float,
        /** The letter rendered as seal glyphs. Changes every second. */
        val cipherText: String,
    ) : CapsuleDetailUiState

    data class Ready(
        val capsule: Capsule,
        val waitedMillis: Long,
    ) : CapsuleDetailUiState

    data class Opened(
        val capsule: Capsule,
        val sealedForMillis: Long,
    ) : CapsuleDetailUiState
}

val CapsuleDetailUiState.capsuleOrNull: Capsule?
    get() = when (this) {
        is CapsuleDetailUiState.Sealed -> capsule
        is CapsuleDetailUiState.Ready -> capsule
        is CapsuleDetailUiState.Opened -> capsule
        CapsuleDetailUiState.Loading, CapsuleDetailUiState.NotFound -> null
    }

sealed interface CapsuleDetailEvent {
    data object Deleted : CapsuleDetailEvent
}
