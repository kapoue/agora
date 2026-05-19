package com.kapoue.agora.ui.screens.multiplayer.participant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kapoue.agora.ui.screens.multiplayer.MultiplayerAnswerState
import com.kapoue.agora.ui.screens.multiplayer.MultiplayerGameViewModel
import com.kapoue.agora.ui.theme.*
import com.kapoue.agora.domain.model.WrongAnswer

@Composable
fun ParticipantGameScreen(
    playerName: String,
    onRoundComplete: (score: Int, total: Int, timeMillis: Long, wrongAnswers: List<WrongAnswer>) -> Unit,
    viewModel: MultiplayerGameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(playerName) {
        viewModel.initialize(playerName)
    }

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onRoundComplete(
                uiState.score,
                uiState.totalQuestions,
                uiState.elapsedMillis,
                uiState.wrongAnswers
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgoraBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
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
            Text(text = playerName, fontFamily = LatoFamily, fontSize = 13.sp, color = AgoraStone)
            Text(
                text = "${uiState.currentQuestionIndex + 1} / ${uiState.totalQuestions}",
                fontFamily = LatoFamily, fontSize = 13.sp, color = AgoraStone
            )
        }

        if (!uiState.isStarted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = playerName, fontFamily = CinzelFamily, fontSize = 24.sp, color = AgoraGold)
                Text(
                    text = "Prêt à jouer ?",
                    fontFamily = LatoFamily, fontSize = 16.sp, color = AgoraStone,
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = uiState.currentQuestion?.text ?: "",
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
