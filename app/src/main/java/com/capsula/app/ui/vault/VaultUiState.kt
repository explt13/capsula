package com.capsula.app.ui.vault

import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.CapsuleStatus

// Представление капсулы в UI, включая её статус, оставшееся время до открытия и прогресс запечатывания.
data class CapsuleItemUi(
    val capsule: Capsule,
    val status: CapsuleStatus,
    val remainingMillis: Long,
    val progress: Float,
    val sinceOpenedMillis: Long?,
)

fun Capsule.toItemUi(now: Long): CapsuleItemUi = CapsuleItemUi(
    capsule = this,
    status = statusAt(now),
    remainingMillis = remainingMillis(now),
    progress = sealProgress(now),
    sinceOpenedMillis = openedAt?.let { now - it },
)

enum class VaultFilter(val label: String) {
    ALL("All"),
    SEALED("Sealed"),
    READY("Ready"),
    OPENED("Opened");

    fun matches(status: CapsuleStatus): Boolean = when (this) {
        ALL -> true
        SEALED -> status == CapsuleStatus.SEALED
        READY -> status == CapsuleStatus.READY
        OPENED -> status == CapsuleStatus.OPENED
    }
}

data class VaultUiState(
    val isLoading: Boolean = true,
    val items: List<CapsuleItemUi> = emptyList(),
    val selectedFilter: VaultFilter = VaultFilter.ALL,
    val counts: Map<VaultFilter, Int> = emptyMap(),
) {
    val readyCount: Int get() = counts[VaultFilter.READY] ?: 0
}
