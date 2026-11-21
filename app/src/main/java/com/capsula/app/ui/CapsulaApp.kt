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
import com.capsula.app.ui.navigation.TrashResult
import com.capsula.app.ui.trash.TrashScreen
import com.capsula.app.ui.vault.VaultScreen

// Основной Composable для приложения Capsula. Определяет навигацию между экранами Vault, Insights,
// корзиной, формой письма (создание и редактирование черновика) и деталями капсулы.
@Composable
fun CapsulaApp(navController: NavHostController = rememberNavController()) {
    val openDetail: (Long) -> Unit = { id -> navController.navigate(Destination.Detail.createRoute(id)) }

    NavHost(navController = navController, startDestination = Destination.Vault.route) {

        composable(Destination.Vault.route) { entry ->
            VaultScreen(
                onCapsuleClick = openDetail,
                onCreateClick = { navController.navigate(Destination.Create.route) },
                onTrashClick = { navController.navigate(Destination.Trash.route) },
                onNavigateToTab = navController::navigateToTab,
                trashedCapsule = TrashResult.collect(entry.savedStateHandle),
                onTrashedCapsuleShown = { TrashResult.clear(entry.savedStateHandle) },
            )
        }

        composable(Destination.Insights.route) { entry ->
            InsightsScreen(
                onCapsuleClick = openDetail,
                onNavigateToTab = navController::navigateToTab,
                trashedCapsule = TrashResult.collect(entry.savedStateHandle),
                onTrashedCapsuleShown = { TrashResult.clear(entry.savedStateHandle) },
            )
        }

        composable(Destination.Trash.route) {
            TrashScreen(onBack = { navController.navigateUp() })
        }

        composable(Destination.Create.route) {
            CreateCapsuleScreen(
                onBack = { navController.navigateUp() },
                onSaved = { id, _ ->
                    // После сохранения открываем капсулу (черновик или запечатанную), а форму убираем из стека.
                    navController.navigate(Destination.Detail.createRoute(id)) {
                        popUpTo(Destination.Create.route) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Destination.Edit.route,
            arguments = listOf(navArgument(Destination.ARG_CAPSULE_ID) { type = NavType.LongType }),
        ) {
            // Редактирование черновика открывается с экрана деталей, туда же и возвращаемся.
            // Экран деталей сам покажет новое состояние: данные приходят из Room через Flow.
            CreateCapsuleScreen(
                onBack = { navController.navigateUp() },
                onSaved = { _, _ -> navController.navigateUp() },
            )
        }

        composable(
            route = Destination.Detail.route,
            arguments = listOf(navArgument(Destination.ARG_CAPSULE_ID) { type = NavType.LongType }),
        ) {
            // Экран деталей капсулы. Отображает информацию о выбранной капсуле и позволяет открыть её, если она готова.
            CapsuleDetailScreen(
                onBack = { navController.navigateUp() },
                onEditDraft = { id -> navController.navigate(Destination.Edit.createRoute(id)) },
                onTrashed = { trashed ->
                    // Результат кладём в SavedStateHandle предыдущего экрана: он покажет snackbar с Undo.
                    TrashResult.send(navController.previousBackStackEntry?.savedStateHandle, trashed)
                    navController.navigateUp()
                },
            )
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
