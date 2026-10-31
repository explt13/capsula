package com.capsula.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

// Определяет маршруты навигации в приложении Capsula, включая экран Vault, Insights, создание капсулы и детали капсулы.
sealed class Destination(val route: String) {
    data object Vault : Destination("vault")
    data object Insights : Destination("insights")
    data object Create : Destination("create")
    data object Detail : Destination("capsule/{capsuleId}") {
        const val ARG_CAPSULE_ID = "capsuleId"
        fun createRoute(capsuleId: Long): String = "capsule/$capsuleId"
    }
}

// Определяет верхнеуровневые пункты назначения в приложении Capsula, включая Vault и Insights, с их маршрутами, метками и значками.
enum class TopLevelDestination(val route: String, val label: String, val icon: ImageVector) {
    VAULT(Destination.Vault.route, "Vault", Icons.Filled.Lock),
    INSIGHTS(Destination.Insights.route, "Insights", Icons.Filled.Star),
}
