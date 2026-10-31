package com.capsula.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.capsula.app.ui.create.CreateCapsuleScreen
import com.capsula.app.ui.detail.CapsuleDetailScreen
import com.capsula.app.ui.insights.InsightsScreen
import com.capsula.app.ui.navigation.Destination
import com.capsula.app.ui.navigation.TopLevelDestination
import com.capsula.app.ui.vault.VaultScreen

// Основной Composable для приложения Capsula. Определяет навигацию между экранами Vault, Insights, создания капсулы и деталей капсулы.
@Composable
fun CapsulaApp(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Destination.Vault.route) {

        composable(Destination.Vault.route) {
            VaultScreen(
                onCapsuleClick = { id -> navController.navigate(Destination.Detail.createRoute(id)) },
                onCreateClick = { navController.navigate(Destination.Create.route) },
                onNavigateToTab = navController::navigateToTab,
            )
        }

        composable(Destination.Insights.route) {
            InsightsScreen(
                onCapsuleClick = { id -> navController.navigate(Destination.Detail.createRoute(id)) },
                onNavigateToTab = navController::navigateToTab,
            )
        }

        composable(Destination.Create.route) {
            CreateCapsuleScreen(
                onBack = { navController.navigateUp() },
                onSealed = { id ->
                    // После успешного создания капсулы, навигация к экрану деталей капсулы с удалением экрана создания из стека.
                    navController.navigate(Destination.Detail.createRoute(id)) {
                        popUpTo(Destination.Create.route) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Destination.Detail.route,
            arguments = listOf(
                navArgument(Destination.Detail.ARG_CAPSULE_ID) { type = NavType.LongType },
            ),
        ) {
            // Экран деталей капсулы. Отображает информацию о выбранной капсуле и позволяет открыть её, если она готова.
            CapsuleDetailScreen(onBack = { navController.navigateUp() })
        }
    }
}

// Навигация к верхнеуровневому пункту назначения с сохранением состояния и удалением предыдущих экранов из стека.
private fun NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
