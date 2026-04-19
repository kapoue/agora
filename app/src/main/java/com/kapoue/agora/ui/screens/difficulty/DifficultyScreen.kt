package com.kapoue.agora.ui.screens.difficulty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kapoue.agora.domain.model.Difficulty
import com.kapoue.agora.domain.model.Theme
import com.kapoue.agora.ui.components.WarriorIcon
import com.kapoue.agora.ui.theme.AgoraBackground
import com.kapoue.agora.ui.theme.AgoraGold
import com.kapoue.agora.ui.theme.AgoraStone
import com.kapoue.agora.ui.theme.AgoraSurface
import com.kapoue.agora.ui.theme.AgoraWhite
import com.kapoue.agora.ui.theme.CinzelFamily
import com.kapoue.agora.ui.theme.LatoFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifficultyScreen(
    theme: Theme,
    onDifficultyClick: (Difficulty) -> Unit,
    onBackClick: () -> Unit,
    viewModel: DifficultyViewModel = hiltViewModel()
) {
    val progressMap by viewModel.progressMap.collectAsState()

    LaunchedEffect(theme) {
        viewModel.loadProgress(theme)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AgoraBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // En-tête
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = AgoraStone
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = theme.displayName.uppercase(),
                    fontFamily = CinzelFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 24.sp,
                    color = AgoraGold
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(48.dp)) // balance pour le bouton retour
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Cards de difficulté
            Column(
                modifier = Modifier.fillMaxWidth(0.8f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Difficulty.entries.forEach { difficulty ->
                    val progress = progressMap[difficulty]
                    val level = progress?.currentLevel ?: 0

                    Card(
                        onClick = { onDifficultyClick(difficulty) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AgoraGold),
                        colors = CardDefaults.cardColors(containerColor = AgoraSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WarriorIcon(
                                difficulty = difficulty,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = difficulty.displayName,
                                    fontFamily = CinzelFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 20.sp,
                                    color = AgoraWhite
                                )
                                Text(
                                    text = "Niveau $level",
                                    fontFamily = LatoFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = AgoraStone
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
