package com.kapoue.agora.ui.screens.multiplayer.participant

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.kapoue.agora.domain.model.PlayerResult
import com.kapoue.agora.domain.model.WrongAnswer
import com.kapoue.agora.multiplayer.MultiplayerSessionManager
import com.kapoue.agora.ui.theme.*
import com.kapoue.agora.ui.util.QrCodeGenerator
import com.kapoue.agora.ui.util.QrPayloadEncoder
import com.kapoue.agora.ui.util.VibrationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color

@HiltViewModel
class ParticipantRoundResultViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val sessionManager: MultiplayerSessionManager
) : ViewModel() {

    fun buildResultQrCode(
        playerName: String,
        score: Int,
        totalQuestions: Int,
        timeMillis: Long,
        wrongAnswers: List<WrongAnswer>
    ): android.graphics.Bitmap {
        val result = PlayerResult(
            sessionId = sessionManager.sessionId,
            roundNumber = sessionManager.currentRound,
            playerName = playerName,
            score = score,
            totalQuestions = totalQuestions,
            timeMillis = timeMillis,
            wrongAnswers = wrongAnswers
        )
        val encoded = QrPayloadEncoder.encodeResult(result)
        VibrationHelper.vibrateOnResult(context)
        return QrCodeGenerator.generate(encoded)
    }
}

@Composable
fun ParticipantRoundResultScreen(
    playerName: String,
    score: Int,
    totalQuestions: Int,
    timeMillis: Long,
    wrongAnswers: List<WrongAnswer>,
    onSeeWrongAnswers: () -> Unit,
    onScanNextRound: () -> Unit,
    viewModel: ParticipantRoundResultViewModel = hiltViewModel()
) {
    val sm = viewModel.sessionManager
    val bitmap = remember {
        viewModel.buildResultQrCode(playerName, score, totalQuestions, timeMillis, wrongAnswers)
    }
    val isLastRound = sm.currentRound >= sm.totalRounds

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgoraBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Text(
            text = "MANCHE ${sm.currentRound}/${sm.totalRounds} TERMINÉE",
            fontFamily = CinzelFamily,
            fontSize = 16.sp,
            color = AgoraGold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "$score / $totalQuestions",
            fontFamily = CinzelFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            color = AgoraWhite
        )

        val seconds = timeMillis / 1000
        Text(
            text = "${seconds}s",
            fontFamily = LatoFamily,
            fontSize = 16.sp,
            color = AgoraStone
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Montre ce QR code à l'organisateur",
            fontFamily = LatoFamily,
            fontSize = 14.sp,
            color = AgoraWhite,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.size(250.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR code résultat",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (wrongAnswers.isNotEmpty()) {
                OutlinedButton(
                    onClick = onSeeWrongAnswers,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AgoraStone),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AgoraStone)
                ) {
                    Text(
                        "Voir mes erreurs (${wrongAnswers.size})",
                        fontFamily = LatoFamily,
                        fontSize = 14.sp
                    )
                }
            }

            if (!isLastRound) {
                Button(
                    onClick = onScanNextRound,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AgoraGold)
                ) {
                    Text(
                        "Scanner la manche ${sm.currentRound + 1}",
                        fontFamily = CinzelFamily,
                        color = AgoraBackground,
                        fontSize = 15.sp
                    )
                }
            } else {
                Text(
                    text = "Partie terminée !",
                    fontFamily = CinzelFamily,
                    fontSize = 16.sp,
                    color = AgoraGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
