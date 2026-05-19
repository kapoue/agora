package com.kapoue.agora.ui.screens.multiplayer.participant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kapoue.agora.ui.theme.*

@Composable
fun ParticipantSetupScreen(
    onReady: (name: String) -> Unit,
    viewModel: ParticipantSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isReady) {
        if (uiState.isReady) onReady(uiState.name.trim())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgoraBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "TON PRÉNOM",
            fontFamily = CinzelFamily,
            fontSize = 20.sp,
            color = AgoraGold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Les autres joueurs verront ton prénom\ndans le classement",
            fontFamily = LatoFamily,
            fontSize = 14.sp,
            color = AgoraStone,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Prénom", fontFamily = LatoFamily, color = AgoraStone) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AgoraWhite,
                unfocusedTextColor = AgoraWhite,
                focusedBorderColor = AgoraGold,
                unfocusedBorderColor = AgoraStone
            ),
            modifier = Modifier.fillMaxWidth()
        )

        uiState.error?.let {
            Text(it, fontFamily = LatoFamily, fontSize = 13.sp, color = AgoraWrongLight,
                modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = viewModel::onConfirm,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AgoraGold)
        ) {
            Text("Prêt !", fontFamily = CinzelFamily, color = AgoraBackground, fontSize = 16.sp)
        }
    }
}
