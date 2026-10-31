package com.capsula.app.ui.detail

import com.capsula.app.data.model.Capsule

// Представляет состояние UI для экрана деталей капсулы, включая загрузку, отсутствие капсулы, запечатанную, готовую и открытую капсулу.
sealed interface CapsuleDetailUiState {
    data object Loading : CapsuleDetailUiState

    data object NotFound : CapsuleDetailUiState

    data class Sealed(
        val capsule: Capsule,
        val remainingMillis: Long,
        val progress: Float,
        // Зашифрованный текст размышлений, который будет расшифрован при открытии капсулы.
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
