package com.kapoue.agora.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kapoue.agora.domain.model.Difficulty
import com.kapoue.agora.domain.model.Theme
import com.kapoue.agora.ui.components.AnswerButton
import com.kapoue.agora.ui.components.AnswerState
import com.kapoue.agora.ui.components.BlurredBackground
import com.kapoue.agora.ui.theme.AgoraBackground
import com.kapoue.agora.ui.theme.AgoraCorrectLight
import com.kapoue.agora.ui.theme.AgoraGold
import com.kapoue.agora.ui.theme.AgoraStone
import com.kapoue.agora.ui.theme.AgoraSurface
import com.kapoue.agora.ui.theme.AgoraWhite
import com.kapoue.agora.ui.theme.AgoraWrong
import com.kapoue.agora.ui.theme.CinzelFamily
import com.kapoue.agora.ui.theme.LatoFamily

@Composable
fun GameScreen(
    theme: Theme,
    difficulty: Difficulty,
    onHomeClick: () -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(theme, difficulty) {
        viewModel.initialize(theme, difficulty)
    }

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AgoraBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AgoraGold)
            }
        }

        uiState.error != null -> {            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AgoraBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = uiState.error!!,
                        color = AgoraWrong,
                        fontFamily = LatoFamily,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.retry() },
                        colors = ButtonDefaults.buttonColors(containerColor = AgoraGold),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Réessayer",
                            fontFamily = CinzelFamily,
                            color = Color.Black
                        )
                    }
                    Button(
                        onClick = onHomeClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AgoraSurface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Accueil",
                            fontFamily = CinzelFamily,
                            color = AgoraWhite
                        )
                    }
                }
            }
        }

        uiState.isCompleted -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AgoraBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "✦",
                        fontSize = 48.sp,
                        color = AgoraGold
                    )
                    Text(
                        text = "Félicitations !",
                        fontFamily = CinzelFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = AgoraGold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Vous avez répondu correctement à toutes les questions disponibles.\nDe nouvelles questions seront disponibles à la prochaine mise à jour.",
                        fontFamily = LatoFamily,
                        fontSize = 15.sp,
                        color = AgoraStone,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Text(
                        text = "Score : ${uiState.currentLevel}",
                        fontFamily = CinzelFamily,
                        fontSize = 18.sp,
                        color = AgoraWhite,
                        fontStyle = FontStyle.Italic
                    )
                    Button(
                        onClick = onHomeClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AgoraGold),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Accueil",
                            fontFamily = CinzelFamily,
                            color = Color.Black,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        uiState.currentQuestion != null -> {
            val question = uiState.currentQuestion!!

            BlurredBackground(
                imageUrl = question.imageUrl,
                fallbackRes = question.theme.placeholderRes
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // En-tête : bouton Accueil + Niveau
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onHomeClick) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = "Accueil",
                                tint = AgoraStone,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Niveau ${uiState.currentLevel}",
                            fontFamily = CinzelFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 18.sp,
                            color = AgoraGold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bloc question
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AgoraSurface.copy(alpha = 0.85f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = question.questionText,
                            fontFamily = LatoFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 18.sp,
                            color = AgoraWhite,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bloc réponses
                    uiState.answersWithState.forEach { (answer, answerState) ->
                        AnswerButton(
                            text = answer,
                            state = answerState,
                            enabled = !uiState.showNext,
                            onClick = { viewModel.onAnswerSelected(answer) }
                        )
                    }

                    // Bloc feedback (mauvaise réponse uniquement)
                    AnimatedVisibility(
                        visible = uiState.showFeedback,
                        enter = fadeIn(),
                        exit = ExitTransition.None
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = AgoraWrong.copy(alpha = 0.20f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "La bonne réponse était : ${uiState.correctAnswerText}",
                                    fontFamily = LatoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = AgoraCorrectLight
                                )
                            }
                        }
                    }

                    // Bouton Suivant
                    AnimatedVisibility(
                        visible = uiState.showNext,
                        enter = fadeIn(),
                        exit = ExitTransition.None
                    ) {
                        Button(
                            onClick = { viewModel.onNextQuestion() },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AgoraGold
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = "Suivant",
                                fontFamily = CinzelFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 18.sp,
                                color = Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
