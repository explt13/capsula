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

/**
 * Navigation graph. Screens never touch the NavController themselves: they receive
 * plain lambdas (onCapsuleClick, onBack...). This keeps screens reusable and previewable.
 */
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
                    // Show the freshly sealed capsule; Back from there returns to the vault, not the form.
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
            // The capsule id is read by CapsuleDetailViewModel from its SavedStateHandle.
            CapsuleDetailScreen(onBack = { navController.navigateUp() })
        }
    }
}

/** Standard bottom-bar navigation: one copy of each tab, tab state is saved and restored. */
private fun NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
