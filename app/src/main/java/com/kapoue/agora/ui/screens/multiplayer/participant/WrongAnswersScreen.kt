package com.kapoue.agora.ui.screens.multiplayer.participant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapoue.agora.domain.model.WrongAnswer
import com.kapoue.agora.ui.theme.*

@Composable
fun WrongAnswersScreen(
    wrongAnswers: List<WrongAnswer>,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgoraBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
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
                text = "MES ERREURS",
                fontFamily = CinzelFamily,
                fontSize = 16.sp,
                color = AgoraGold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (wrongAnswers.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucune erreur ! 🎉", fontFamily = LatoFamily, color = AgoraGold, fontSize = 18.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                itemsIndexed(wrongAnswers) { index, wa ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = AgoraSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = wa.questionText,
                                fontFamily = CinzelFamily,
                                fontSize = 15.sp,
                                color = AgoraWhite,
                                lineHeight = 22.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Ta réponse:", fontFamily = LatoFamily, fontSize = 13.sp, color = AgoraStone)
                                Text(wa.givenAnswer, fontFamily = LatoFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AgoraWrong)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Correct:", fontFamily = LatoFamily, fontSize = 13.sp, color = AgoraStone)
                                Text(wa.correctAnswer, fontFamily = LatoFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AgoraCorrect)
                            }
                        }
                    }
                }
            }
        }
    }
}
