package com.kapoue.agora.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kapoue.agora.domain.model.Difficulty
import com.kapoue.agora.domain.model.Theme
import com.kapoue.agora.ui.screens.about.AboutScreen
import com.kapoue.agora.ui.screens.difficulty.DifficultyScreen
import com.kapoue.agora.ui.screens.game.GameScreen
import com.kapoue.agora.ui.screens.home.HomeScreen

@Composable
fun AgoraNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onThemeClick = { theme ->
                    navController.navigate(Screen.Difficulty.createRoute(theme.name))
                },
                onAboutClick = {
                    navController.navigate(Screen.About.route)
                }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Difficulty.route,
            arguments = listOf(
                navArgument("themeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val themeId = backStackEntry.arguments?.getString("themeId") ?: return@composable
            val theme = Theme.valueOf(themeId)
            DifficultyScreen(
                theme = theme,
                onDifficultyClick = { difficulty ->
                    navController.navigate(
                        Screen.Game.createRoute(theme.name, difficulty.name)
                    )
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Game.route,
            arguments = listOf(
                navArgument("themeId") { type = NavType.StringType },
                navArgument("difficultyId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val themeId = backStackEntry.arguments?.getString("themeId") ?: return@composable
            val difficultyId = backStackEntry.arguments?.getString("difficultyId") ?: return@composable
            val theme = Theme.valueOf(themeId)
            val difficulty = Difficulty.valueOf(difficultyId)
            GameScreen(
                theme = theme,
                difficulty = difficulty,
                onHomeClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }
    }
}
