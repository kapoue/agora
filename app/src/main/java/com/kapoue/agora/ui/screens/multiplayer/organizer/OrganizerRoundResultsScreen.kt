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
import javax.inject.Inject

@HiltViewModel
class OrganizerRoundResultsViewModel @Inject constructor(
    val sessionManager: MultiplayerSessionManager
) : ViewModel()

@Composable
fun OrganizerRoundResultsScreen(
    onNextRound: () -> Unit,
    onFinish: () -> Unit,
    viewModel: OrganizerRoundResultsViewModel = hiltViewModel()
) {
    val sm = viewModel.sessionManager
    val ranking = remember { sm.getCumulativeRanking() }
    val isLastRound = sm.currentRound >= sm.totalRounds

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgoraBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            text = if (isLastRound) "RÉSULTATS FINAUX" else "CLASSEMENT — Manche ${sm.currentRound}/${sm.totalRounds}",
            fontFamily = CinzelFamily,
            fontSize = 18.sp,
            color = AgoraGold,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(ranking) { index, player ->
                val medalColor = when (index) {
                    0 -> AgoraGold
                    1 -> AgoraStone
                    2 -> AgoraStone.copy(alpha = 0.7f)
                    else -> AgoraStone.copy(alpha = 0.4f)
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (player.isEliminated) AgoraSurface.copy(alpha = 0.5f) else AgoraSurface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontFamily = CinzelFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = medalColor,
                            modifier = Modifier.width(28.dp)
                        )
                        Text(
                            text = player.name,
                            fontFamily = CinzelFamily,
                            fontSize = 15.sp,
                            color = if (player.isEliminated) AgoraStone else AgoraWhite,
                            modifier = Modifier.weight(1f)
                        )
                        if (player.isEliminated) {
                            Text("éliminé", fontFamily = LatoFamily, fontSize = 12.sp, color = AgoraWrong)
                        } else {
                            Text(
                                text = "${player.totalScore} pts",
                                fontFamily = LatoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = AgoraGold
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = if (isLastRound) onFinish else onNextRound,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AgoraGold)
        ) {
            Text(
                text = if (isLastRound) "Voir le podium" else "Lancer la manche ${sm.currentRound + 1}",
                fontFamily = CinzelFamily,
                color = AgoraBackground,
                fontSize = 16.sp
            )
        }
    }
}
