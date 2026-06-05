package com.acepero13.android.gamereviewer.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.acepero13.android.gamereviewer.ui.screens.GuessTheMoveScreen
import com.acepero13.android.gamereviewer.ui.screens.SessionDebriefScreen
import com.acepero13.android.gamereviewer.ui.screens.SettingsScreen
import com.acepero13.android.gamereviewer.ui.screens.WeaknessDrillScreen

sealed class Screen(val route: String) {
    object Home       : Screen("home")
    object GameList   : Screen("games")
    object Import     : Screen("import")
    object Settings   : Screen("settings")
    object Analysis   : Screen("analysis/{gameId}") {
        fun route(gameId: Long) = "analysis/$gameId"
    }
    object Report     : Screen("report/{gameId}") {
        fun route(gameId: Long) = "report/$gameId"
    }
    object Dashboard  : Screen("dashboard")
    object WeaknessDrill : Screen("weakness_drill/{categoryNames}/{drillTitle}") {
        fun route(categoryNames: String, drillTitle: String) =
            "weakness_drill/${Uri.encode(categoryNames)}/${Uri.encode(drillTitle)}"
    }
    object SessionDebrief : Screen("session_debrief")
    object GuessTheMove   : Screen("guess_the_move?gameIndex={gameIndex}") {
        fun route(gameIndex: Int) = "guess_the_move?gameIndex=$gameIndex"
        const val BASE = "guess_the_move"
    }
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenGameList    = { navController.navigate(Screen.GameList.route) },
                onOpenImport      = { navController.navigate(Screen.Import.route) },
                onOpenDashboard   = { navController.navigate(Screen.Dashboard.route) },
                onOpenSettings    = { navController.navigate(Screen.Settings.route) },
                onOpenDebrief     = { navController.navigate(Screen.SessionDebrief.route) },
                onOpenAnalysis    = { gameId -> navController.navigate(Screen.Analysis.route(gameId)) },
                onOpenGuessTheMove       = { navController.navigate(Screen.GuessTheMove.BASE) },
                onOpenGuessTheMoveWithGame = { idx -> navController.navigate(Screen.GuessTheMove.route(idx)) },
            )
        }
        composable(
            route     = Screen.GuessTheMove.route,
            arguments = listOf(navArgument("gameIndex") {
                type         = NavType.IntType
                defaultValue = -1
            }),
        ) { backStack ->
            val gameIndex = backStack.arguments?.getInt("gameIndex") ?: -1
            GuessTheMoveScreen(
                onBack           = { navController.popBackStack() },
                initialGameIndex = gameIndex,
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
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route     = Screen.Analysis.route,
            arguments = listOf(navArgument("gameId") { type = NavType.LongType }),
        ) { backStack ->
            val gameId = backStack.arguments?.getLong("gameId") ?: return@composable
            val initialMoveIndex by backStack.savedStateHandle
                .getStateFlow<Int?>("navigateToMove", null)
                .collectAsState()
            AnalysisScreen(
                gameId               = gameId,
                onBack               = { navController.popBackStack() },
                onViewReport         = { id -> navController.navigate(Screen.Report.route(id)) },
                initialMoveIndex     = initialMoveIndex,
                onInitialMoveConsumed = { backStack.savedStateHandle["navigateToMove"] = null },
            )
        }
        composable(
            route     = Screen.Report.route,
            arguments = listOf(navArgument("gameId") { type = NavType.LongType }),
        ) { backStack ->
            val gameId = backStack.arguments?.getLong("gameId") ?: return@composable
            GameReportScreen(
                gameId           = gameId,
                onBack           = { navController.popBackStack() },
                onNavigateToMove = { moveIndex ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("navigateToMove", moveIndex)
                    navController.popBackStack()
                },
            )
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onBack       = { navController.popBackStack() },
                onStartDrill = { cats, title ->
                    navController.navigate(Screen.WeaknessDrill.route(cats, title))
                },
            )
        }
        composable(
            route     = Screen.WeaknessDrill.route,
            arguments = listOf(
                navArgument("categoryNames") { type = NavType.StringType },
                navArgument("drillTitle")   { type = NavType.StringType },
            ),
        ) { backStack ->
            val encoded = backStack.arguments?.getString("categoryNames") ?: return@composable
            val title   = backStack.arguments?.getString("drillTitle") ?: ""
            val names   = Uri.decode(encoded).split(",").filter { it.isNotBlank() }
            val decodedTitle = Uri.decode(title)
            WeaknessDrillScreen(
                categoryNames = names,
                drillTitle    = decodedTitle,
                onBack        = { navController.popBackStack() },
            )
        }
        composable(Screen.SessionDebrief.route) {
            SessionDebriefScreen(
                onBack       = { navController.popBackStack() },
                onStartDrill = { cats, title ->
                    navController.navigate(Screen.WeaknessDrill.route(cats, title))
                },
            )
        }
    }
}
