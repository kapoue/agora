package com.kapoue.agora.ui.screens.multiplayer.organizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kapoue.agora.multiplayer.MultiplayerSessionManager
import com.kapoue.agora.ui.theme.*
import com.kapoue.agora.ui.util.QrCodeGenerator
import com.kapoue.agora.ui.util.QrPayloadEncoder
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import javax.inject.Inject

@HiltViewModel
class OrganizerQrViewModel @Inject constructor(
    val sessionManager: MultiplayerSessionManager
) : ViewModel()

@Composable
fun OrganizerQrScreen(
    onReadyToPlay: () -> Unit,
    viewModel: OrganizerQrViewModel = hiltViewModel()
) {
    val sm = viewModel.sessionManager
    val payload = sm.getCurrentRoundPayload()
    val encoded = remember(payload) { QrPayloadEncoder.encodePayload(payload) }
    val bitmap = remember(encoded) { QrCodeGenerator.generate(encoded) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgoraBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Text(
            text = "MANCHE ${sm.currentRound} / ${sm.totalRounds}",
            fontFamily = CinzelFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = AgoraGold
        )

        Text(
            text = "${sm.questionsPerRound} questions",
            fontFamily = LatoFamily,
            fontSize = 14.sp,
            color = AgoraStone,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Tous les participants scannent ce QR code",
            fontFamily = LatoFamily,
            fontSize = 14.sp,
            color = AgoraWhite,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(Modifier.height(24.dp))

        // QR code sur fond blanc
        Card(
            modifier = Modifier
                .size(280.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR code manche ${sm.currentRound}",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onReadyToPlay,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AgoraGold)
        ) {
            Text(
                "Je suis prêt à jouer",
                fontFamily = CinzelFamily,
                color = AgoraBackground,
                fontSize = 16.sp
            )
        }
    }
}
