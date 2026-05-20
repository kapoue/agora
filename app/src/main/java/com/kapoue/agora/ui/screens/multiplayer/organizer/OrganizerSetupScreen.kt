package com.kapoue.agora.ui.screens.multiplayer.organizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kapoue.agora.ui.theme.*

@Composable
fun OrganizerSetupScreen(
    onReady: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: OrganizerSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isReady) {
        if (uiState.isReady) onReady()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgoraBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Retour", tint = AgoraStone)
            }
            Text(
                text = "ORGANISER UNE PARTIE",
                fontFamily = CinzelFamily,
                fontSize = 16.sp,
                color = AgoraGold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Prénom
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Ton prénom", fontFamily = LatoFamily, color = AgoraStone) },
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

            // Nombre de questions
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Questions par manche",
                    fontFamily = CinzelFamily,
                    fontSize = 14.sp,
                    color = AgoraWhite
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(10, 20).forEach { count ->
                        val selected = uiState.questionsPerRound == count
                        OutlinedButton(
                            onClick = { viewModel.onQuestionsChange(count) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) AgoraGold else AgoraSurface,
                                contentColor = if (selected) AgoraBackground else AgoraStone
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) AgoraGold else AgoraStone
                            )
                        ) {
                            Text("$count", fontFamily = CinzelFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Nombre de manches
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Nombre de manches",
                    fontFamily = CinzelFamily,
                    fontSize = 14.sp,
                    color = AgoraWhite
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(1, 2, 3).forEach { count ->
                        val selected = uiState.totalRounds == count
                        OutlinedButton(
                            onClick = { viewModel.onRoundsChange(count) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) AgoraGold else AgoraSurface,
                                contentColor = if (selected) AgoraBackground else AgoraStone
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) AgoraGold else AgoraStone
                            )
                        ) {
                            Text("$count", fontFamily = CinzelFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            uiState.error?.let {
                Text(it, fontFamily = LatoFamily, fontSize = 13.sp, color = AgoraWrongLight)
            }

            Button(
                onClick = viewModel::onConfirm,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AgoraGold)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = AgoraBackground,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Créer la partie",
                        fontFamily = CinzelFamily,
                        color = AgoraBackground,
                        fontSize = 16.sp
                    )
                }
            }

            if (uiState.isLoading && uiState.loadingMessage.isNotEmpty()) {
                Text(
                    text = uiState.loadingMessage,
                    fontFamily = LatoFamily,
                    fontSize = 12.sp,
                    color = AgoraStone,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
