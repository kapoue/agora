package com.kapoue.agora.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kapoue.agora.domain.model.Difficulty
import com.kapoue.agora.domain.model.Theme
import com.kapoue.agora.domain.model.WrongAnswer
import com.kapoue.agora.ui.screens.about.AboutScreen
import com.kapoue.agora.ui.screens.difficulty.DifficultyScreen
import com.kapoue.agora.ui.screens.game.GameScreen
import com.kapoue.agora.ui.screens.home.HomeScreen
import com.kapoue.agora.ui.screens.multiplayer.MultiplayerHomeScreen
import com.kapoue.agora.ui.screens.multiplayer.organizer.OrganizerFinalResultsScreen
import com.kapoue.agora.ui.screens.multiplayer.organizer.OrganizerGameScreen
import com.kapoue.agora.ui.screens.multiplayer.organizer.OrganizerQrScreen
import com.kapoue.agora.ui.screens.multiplayer.organizer.OrganizerRoundResultsScreen
import com.kapoue.agora.ui.screens.multiplayer.organizer.OrganizerScanScreen
import com.kapoue.agora.ui.screens.multiplayer.organizer.OrganizerSetupScreen
import com.kapoue.agora.ui.screens.multiplayer.participant.ParticipantGameScreen
import com.kapoue.agora.ui.screens.multiplayer.participant.ParticipantRoundResultScreen
import com.kapoue.agora.ui.screens.multiplayer.participant.ParticipantScanScreen
import com.kapoue.agora.ui.screens.multiplayer.participant.ParticipantSetupScreen
import com.kapoue.agora.ui.screens.multiplayer.participant.WrongAnswersScreen

@Composable
fun AgoraNavGraph() {
    val navController = rememberNavController()

    // State passé entre les écrans participant (score/temps/erreurs d'une manche)
    var participantScore by rememberSaveable { mutableStateOf(0) }
    var participantTotal by rememberSaveable { mutableStateOf(0) }
    var participantTimeMillis by rememberSaveable { mutableLongStateOf(0L) }
    var participantWrongAnswers by remember { mutableStateOf<List<WrongAnswer>>(emptyList()) }
    var participantName by rememberSaveable { mutableStateOf("") }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // ── Solo ──────────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                onThemeClick = { theme ->
                    navController.navigate(Screen.Difficulty.createRoute(theme.name))
                },
                onAboutClick = { navController.navigate(Screen.About.route) },
                onMultiplayerClick = { navController.navigate(Screen.MultiplayerHome.route) }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(onBackClick = { navController.popBackStack() })
        }

        composable(
            route = Screen.Difficulty.route,
            arguments = listOf(navArgument("themeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val themeId = backStackEntry.arguments?.getString("themeId") ?: return@composable
            val theme = Theme.valueOf(themeId)
            DifficultyScreen(
                theme = theme,
                onDifficultyClick = { difficulty ->
                    navController.navigate(Screen.Game.createRoute(theme.name, difficulty.name))
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
            GameScreen(
                theme = Theme.valueOf(themeId),
                difficulty = Difficulty.valueOf(difficultyId),
                onHomeClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        // ── Multijoueur — Accueil ─────────────────────────────────────────────
        composable(Screen.MultiplayerHome.route) {
            MultiplayerHomeScreen(
                onOrganizeClick = { navController.navigate(Screen.OrganizerSetup.route) },
                onJoinClick = { navController.navigate(Screen.ParticipantScan.route) },
                onBackClick = { navController.popBackStack() }
            )
        }

        // ── Organisateur ──────────────────────────────────────────────────────
        composable(Screen.OrganizerSetup.route) {
            OrganizerSetupScreen(
                onReady = { navController.navigate(Screen.OrganizerQr.route) },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.OrganizerQr.route) {
            OrganizerQrScreen(
                onReadyToPlay = {
                    navController.navigate(Screen.OrganizerGame.route) {
                        popUpTo(Screen.OrganizerQr.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.OrganizerGame.route) {
            OrganizerGameScreen(
                onRoundComplete = {
                    navController.navigate(Screen.OrganizerScan.route) {
                        popUpTo(Screen.OrganizerGame.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.OrganizerScan.route) {
            OrganizerScanScreen(
                onDone = {
                    navController.navigate(Screen.OrganizerRoundResults.route) {
                        popUpTo(Screen.OrganizerScan.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.OrganizerRoundResults.route) {
            OrganizerRoundResultsScreen(
                onNextRound = {
                    // Le sessionManager.currentRound++ est géré dans commitRoundResults
                    navController.navigate(Screen.OrganizerQr.route) {
                        popUpTo(Screen.OrganizerRoundResults.route) { inclusive = true }
                    }
                },
                onFinish = {
                    navController.navigate(Screen.OrganizerFinalResults.route) {
                        popUpTo(Screen.OrganizerRoundResults.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.OrganizerFinalResults.route) {
            OrganizerFinalResultsScreen(
                onNewGame = {
                    navController.navigate(Screen.OrganizerSetup.route) {
                        popUpTo(Screen.MultiplayerHome.route)
                    }
                },
                onHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        // ── Participant ───────────────────────────────────────────────────────
        composable(Screen.ParticipantScan.route) {
            ParticipantScanScreen(
                onSessionReady = { navController.navigate(Screen.ParticipantSetup.route) }
            )
        }

        composable(Screen.ParticipantSetup.route) {
            ParticipantSetupScreen(
                onReady = { name ->
                    participantName = name
                    navController.navigate(Screen.ParticipantGame.route) {
                        popUpTo(Screen.ParticipantSetup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ParticipantGame.route) {
            ParticipantGameScreen(
                playerName = participantName,
                onRoundComplete = { score, total, time, wrongAnswers ->
                    participantScore = score
                    participantTotal = total
                    participantTimeMillis = time
                    participantWrongAnswers = wrongAnswers
                    navController.navigate(Screen.ParticipantRoundResult.route) {
                        popUpTo(Screen.ParticipantGame.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ParticipantRoundResult.route) {
            ParticipantRoundResultScreen(
                playerName = participantName,
                score = participantScore,
                totalQuestions = participantTotal,
                timeMillis = participantTimeMillis,
                wrongAnswers = participantWrongAnswers,
                onSeeWrongAnswers = { navController.navigate(Screen.WrongAnswers.route) },
                onScanNextRound = {
                    navController.navigate(Screen.ParticipantScan.route) {
                        popUpTo(Screen.ParticipantRoundResult.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.WrongAnswers.route) {
            WrongAnswersScreen(
                wrongAnswers = participantWrongAnswers,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

