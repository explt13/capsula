package com.capsula.app.ui.detail

import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.CapsuleEvent
import com.capsula.app.domain.PostponeOption

// Состояние экрана деталей. Отдельный тип на каждый этап жизненного цикла: UI не может, например,
// показать текст ещё запечатанного письма или кнопку «Postpone» у вскрытой капсулы.
sealed interface CapsuleDetailUiState {
    data object Loading : CapsuleDetailUiState

    data object NotFound : CapsuleDetailUiState

    data class Draft(
        val capsule: Capsule,
        val history: List<CapsuleEvent>,
    ) : CapsuleDetailUiState

    data class Sealed(
        val capsule: Capsule,
        val history: List<CapsuleEvent>,
        val remainingMillis: Long,
        val progress: Float,
        // Зашифрованный текст письма, меняется каждую секунду.
        val cipherText: String,
        val postpone: PostponeState,
    ) : CapsuleDetailUiState

    data class Ready(
        val capsule: Capsule,
        val history: List<CapsuleEvent>,
        val waitedMillis: Long,
        val postpone: PostponeState,
    ) : CapsuleDetailUiState

    data class Opened(
        val capsule: Capsule,
        val history: List<CapsuleEvent>,
        val sealedForMillis: Long,
    ) : CapsuleDetailUiState
}

// Всё, что нужно диалогу «Postpone»: можно ли ещё откладывать и какое время открытия получится для каждого варианта.
data class PostponeState(
    val allowed: Boolean,
    val left: Int,
    val max: Int,
    val preview: Map<PostponeOption, Long>,
)

val CapsuleDetailUiState.capsuleOrNull: Capsule?
    get() = when (this) {
        is CapsuleDetailUiState.Draft -> capsule
        is CapsuleDetailUiState.Sealed -> capsule
        is CapsuleDetailUiState.Ready -> capsule
        is CapsuleDetailUiState.Opened -> capsule
        CapsuleDetailUiState.Loading, CapsuleDetailUiState.NotFound -> null
    }

sealed interface CapsuleDetailEvent {
    // Капсула перемещена в корзину: экран закрывается, предыдущий экран покажет Undo.
    data class Trashed(val capsuleId: Long, val title: String) : CapsuleDetailEvent

    // Короткое сообщение об успешном действии (snackbar).
    data class Message(val text: String) : CapsuleDetailEvent
}
