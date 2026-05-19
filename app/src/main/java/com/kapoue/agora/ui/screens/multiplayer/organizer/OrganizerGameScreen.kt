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
            // Enregistrer le résultat de l'organisateur
            sm.addRoundResult(
                playerName = sm.organizerName,
                score = uiState.score,
                totalQuestions = uiState.totalQuestions,
                timeMillis = uiState.elapsedMillis,
                wrongAnswers = uiState.wrongAnswers
            )
            onRoundComplete()
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

        if (!uiState.isStarted) {
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
