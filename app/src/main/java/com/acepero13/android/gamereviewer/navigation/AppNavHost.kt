package com.acepero13.android.gamereviewer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.acepero13.android.gamereviewer.ui.screens.AnalysisScreen
import com.acepero13.android.gamereviewer.ui.screens.DashboardScreen
import com.acepero13.android.gamereviewer.ui.screens.GameListScreen
import com.acepero13.android.gamereviewer.ui.screens.GameReportScreen
import com.acepero13.android.gamereviewer.ui.screens.HomeScreen
import com.acepero13.android.gamereviewer.ui.screens.ImportScreen

sealed class Screen(val route: String) {
    object Home       : Screen("home")
    object GameList   : Screen("games")
    object Import     : Screen("import")
    object Analysis   : Screen("analysis/{gameId}") {
        fun route(gameId: Long) = "analysis/$gameId"
    }
    object Report     : Screen("report/{gameId}") {
        fun route(gameId: Long) = "report/$gameId"
    }
    object Dashboard  : Screen("dashboard")
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenGameList = { navController.navigate(Screen.GameList.route) },
                onOpenImport   = { navController.navigate(Screen.Import.route) },
                onOpenDashboard = { navController.navigate(Screen.Dashboard.route) },
            )
        }
        composable(Screen.GameList.route) {
            GameListScreen(
                onBack         = { navController.popBackStack() },
                onOpenAnalysis = { gameId -> navController.navigate(Screen.Analysis.route(gameId)) },
            )
        }
        composable(Screen.Import.route) {
            ImportScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route     = Screen.Analysis.route,
            arguments = listOf(navArgument("gameId") { type = NavType.LongType }),
        ) { backStack ->
            val gameId = backStack.arguments?.getLong("gameId") ?: return@composable
            AnalysisScreen(
                gameId       = gameId,
                onBack       = { navController.popBackStack() },
                onViewReport = { id -> navController.navigate(Screen.Report.route(id)) },
            )
        }
        composable(
            route     = Screen.Report.route,
            arguments = listOf(navArgument("gameId") { type = NavType.LongType }),
        ) { backStack ->
            val gameId = backStack.arguments?.getLong("gameId") ?: return@composable
            GameReportScreen(
                gameId = gameId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
