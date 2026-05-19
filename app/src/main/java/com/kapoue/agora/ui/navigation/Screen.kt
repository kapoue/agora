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

    // ── Multijoueur ──────────────────────────────────────────────────────────
    object MultiplayerHome : Screen("multiplayer_home")

    // Organisateur
    object OrganizerSetup : Screen("organizer_setup")
    object OrganizerQr : Screen("organizer_qr")
    object OrganizerGame : Screen("organizer_game")
    object OrganizerScan : Screen("organizer_scan")
    object OrganizerRoundResults : Screen("organizer_round_results")
    object OrganizerFinalResults : Screen("organizer_final_results")

    // Participant
    object ParticipantScan : Screen("participant_scan")
    object ParticipantSetup : Screen("participant_setup")
    object ParticipantGame : Screen("participant_game")
    object ParticipantRoundResult : Screen("participant_round_result")
    object WrongAnswers : Screen("wrong_answers")
}

