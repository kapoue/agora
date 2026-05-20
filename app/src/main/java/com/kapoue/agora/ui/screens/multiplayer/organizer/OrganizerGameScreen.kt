package com.kapoue.agora.ui.screens.multiplayer.organizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kapoue.agora.ui.screens.multiplayer.MultiplayerAnswerState
import com.kapoue.agora.ui.screens.multiplayer.MultiplayerGameViewModel
import com.kapoue.agora.ui.theme.*

@Composable
fun OrganizerGameScreen(
    onRoundComplete: () -> Unit,
    viewModel: MultiplayerGameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sm = viewModel.sessionManager

    LaunchedEffect(Unit) {
        viewModel.initialize(sm.organizerName)
    }

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            sm.addRoundResult(
                playerName = sm.organizerName,
                score = uiState.score,
                totalQuestions = uiState.totalQuestions,
                timeMillis = uiState.elapsedMillis,
                wrongAnswers = uiState.wrongAnswers
            )
            // Navigation déclenchée par le bouton dans l'écran de résultats
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgoraBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Progress bar
        LinearProgressIndicator(
            progress = {
                if (uiState.totalQuestions > 0)
                    (uiState.currentQuestionIndex + 1).toFloat() / uiState.totalQuestions
                else 0f
            },
            modifier = Modifier.fillMaxWidth(),
            color = AgoraGold,
            trackColor = AgoraSurface
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Manche ${sm.currentRound}/${sm.totalRounds}",
                fontFamily = LatoFamily,
                fontSize = 13.sp,
                color = AgoraStone
            )
            Text(
                text = "${uiState.currentQuestionIndex + 1} / ${uiState.totalQuestions}",
                fontFamily = LatoFamily,
                fontSize = 13.sp,
                color = AgoraStone
            )
        }

        if (uiState.isCompleted) {
            // Écran de résultats de l'organisateur
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))

                Text(
                    text = "MANCHE ${sm.currentRound}/${sm.totalRounds} TERMINÉE",
                    fontFamily = CinzelFamily,
                    fontSize = 16.sp,
                    color = AgoraGold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "${uiState.score} / ${uiState.totalQuestions}",
                    fontFamily = CinzelFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    color = AgoraWhite
                )

                Text(
                    text = "${uiState.elapsedMillis / 1000}s",
                    fontFamily = LatoFamily,
                    fontSize = 16.sp,
                    color = AgoraStone
                )

                if (uiState.wrongAnswers.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Mes erreurs (${uiState.wrongAnswers.size})",
                        fontFamily = CinzelFamily,
                        fontSize = 15.sp,
                        color = AgoraWrongLight,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    uiState.wrongAnswers.forEach { wrong ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = AgoraSurface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = wrong.questionText,
                                    fontFamily = LatoFamily,
                                    fontSize = 13.sp,
                                    color = AgoraWhite
                                )
                                Text(
                                    text = "✗  ${wrong.givenAnswer}",
                                    fontFamily = LatoFamily,
                                    fontSize = 12.sp,
                                    color = AgoraWrongLight
                                )
                                Text(
                                    text = "✓  ${wrong.correctAnswer}",
                                    fontFamily = LatoFamily,
                                    fontSize = 12.sp,
                                    color = AgoraCorrectLight
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onRoundComplete,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AgoraGold)
                ) {
                    Text(
                        "Scanner les résultats des joueurs",
                        fontFamily = CinzelFamily,
                        color = AgoraBackground,
                        fontSize = 15.sp
                    )
                }
            }
        } else if (!uiState.isStarted) {
            // Écran de démarrage
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = sm.organizerName,
                    fontFamily = CinzelFamily,
                    fontSize = 24.sp,
                    color = AgoraGold
                )
                Text(
                    text = "Prêt à jouer ?",
                    fontFamily = LatoFamily,
                    fontSize = 16.sp,
                    color = AgoraStone,
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                )
                Button(
                    onClick = viewModel::onStart,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AgoraGold)
                ) {
                    Text("Démarrer", fontFamily = CinzelFamily, color = AgoraBackground, fontSize = 18.sp)
                }
            }
        } else {
            val q = uiState.currentQuestion
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = q?.text ?: "",
                    fontFamily = CinzelFamily,
                    fontSize = 18.sp,
                    color = AgoraWhite,
                    textAlign = TextAlign.Start,
                    lineHeight = 26.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                uiState.shuffledAnswers.forEach { (answer, answerState) ->
                    val bgColor = when (answerState) {
                        MultiplayerAnswerState.CORRECT -> AgoraCorrect
                        MultiplayerAnswerState.WRONG_SELECTED -> AgoraWrong
                        MultiplayerAnswerState.NORMAL -> AgoraSurface
                    }
                    Card(
                        onClick = { viewModel.onAnswerSelected(answer) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        enabled = uiState.selectedAnswer == null
                    ) {
                        Text(
                            text = answer,
                            fontFamily = LatoFamily,
                            fontSize = 15.sp,
                            color = AgoraWhite,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
