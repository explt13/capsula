package com.capsula.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

// Маршруты навигации приложения. Все строки маршрутов собраны в одном месте.
sealed class Destination(val route: String) {
    data object Vault : Destination("vault")
    data object Insights : Destination("insights")
    data object Trash : Destination("trash")

    // Форма нового письма.
    data object Create : Destination("create")

    // Та же форма, но для продолжения черновика. Аргумент читает CreateCapsuleViewModel.
    data object Edit : Destination("edit/{capsuleId}") {
        fun createRoute(capsuleId: Long): String = "edit/$capsuleId"
    }

    data object Detail : Destination("capsule/{capsuleId}") {
        fun createRoute(capsuleId: Long): String = "capsule/$capsuleId"
    }

    companion object {
        // Имя аргумента одинаковое для Detail и Edit.
        const val ARG_CAPSULE_ID = "capsuleId"
    }
}

// Определяет верхнеуровневые пункты назначения в приложении Capsula, включая Vault и Insights, с их маршрутами, метками и значками.
enum class TopLevelDestination(val route: String, val label: String, val icon: ImageVector) {
    VAULT(Destination.Vault.route, "Vault", Icons.Filled.Lock),
    INSIGHTS(Destination.Insights.route, "Insights", Icons.Filled.Star),
}
