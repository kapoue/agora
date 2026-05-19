package com.kapoue.agora.ui.screens.multiplayer.organizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kapoue.agora.multiplayer.MultiplayerSessionManager
import com.kapoue.agora.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class OrganizerFinalResultsViewModel @Inject constructor(
    val sessionManager: MultiplayerSessionManager
) : ViewModel() {
    fun onNewGame() {
        viewModelScope.launch {
            sessionManager.completeSession()
            sessionManager.reset()
        }
    }
}

@Composable
fun OrganizerFinalResultsScreen(
    onNewGame: () -> Unit,
    onHome: () -> Unit,
    viewModel: OrganizerFinalResultsViewModel = hiltViewModel()
) {
    val sm = viewModel.sessionManager
    val ranking = remember { sm.getCumulativeRanking() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgoraBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Text(
            text = "PODIUM FINAL",
            fontFamily = CinzelFamily,
            fontSize = 24.sp,
            color = AgoraGold
        )

        Spacer(Modifier.height(8.dp))

        // Top 3 podium
        ranking.take(3).forEachIndexed { index, player ->
            if (!player.isEliminated) {
                val medalEmoji = when (index) { 0 -> "🥇"; 1 -> "🥈"; else -> "🥉" }
                val size = when (index) { 0 -> 22.sp; 1 -> 18.sp; else -> 16.sp }
                Text(
                    text = "$medalEmoji  ${player.name}  — ${player.totalScore} pts",
                    fontFamily = CinzelFamily,
                    fontSize = size,
                    color = AgoraWhite,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Tableau complet
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            itemsIndexed(ranking) { index, player ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (player.isEliminated) AgoraSurface.copy(alpha = 0.4f) else AgoraSurface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "${index + 1}",
                            fontFamily = CinzelFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AgoraStone,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(
                            text = player.name,
                            fontFamily = CinzelFamily,
                            fontSize = 14.sp,
                            color = if (player.isEliminated) AgoraStone else AgoraWhite,
                            modifier = Modifier.weight(1f)
                        )
                        if (player.isEliminated) {
                            Text("éliminé", fontFamily = LatoFamily, fontSize = 12.sp, color = AgoraWrong)
                        } else {
                            Text(
                                "${player.totalScore} pts · ${player.roundsPlayed} manche(s)",
                                fontFamily = LatoFamily,
                                fontSize = 13.sp,
                                color = AgoraStone
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.onNewGame()
                    onNewGame()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AgoraGold),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AgoraGold)
            ) {
                Text("Nouvelle partie", fontFamily = CinzelFamily, fontSize = 14.sp)
            }
            Button(
                onClick = {
                    viewModel.onNewGame()
                    onHome()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AgoraGold)
            ) {
                Text("Accueil", fontFamily = CinzelFamily, color = AgoraBackground, fontSize = 14.sp)
            }
        }
    }
}
