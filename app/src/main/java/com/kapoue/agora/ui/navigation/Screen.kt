package com.kapoue.agora.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object About : Screen("about")
    object Difficulty : Screen("difficulty/{themeId}") {
        fun createRoute(themeId: String) = "difficulty/$themeId"
    }
    object Game : Screen("game/{themeId}/{difficultyId}") {
        fun createRoute(themeId: String, difficultyId: String) = "game/$themeId/$difficultyId"
    }
}
