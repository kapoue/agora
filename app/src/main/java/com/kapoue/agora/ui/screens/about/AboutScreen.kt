package com.kapoue.agora.ui.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapoue.agora.ui.theme.AgoraBackground
import com.kapoue.agora.ui.theme.AgoraGold
import com.kapoue.agora.ui.theme.AgoraGoldLight
import com.kapoue.agora.ui.theme.AgoraStone
import com.kapoue.agora.ui.theme.CinzelFamily
import com.kapoue.agora.ui.theme.LatoFamily

@Composable
fun AboutScreen(
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AgoraBackground)
    ) {
        // Bouton retour
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Retour",
                tint = AgoraStone
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            Text(
                text = "AGORA",
                fontFamily = CinzelFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 32.sp,
                color = AgoraGold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Dans la Grèce antique, l'agora était le lieu de rassemblement, d'échange et de savoir.",
                fontFamily = LatoFamily,
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
                color = AgoraStone,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Version 1.0.0",
                fontFamily = LatoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AgoraStone,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Code source",
                fontFamily = LatoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = AgoraStone,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "github.com/kapoue/agora",
                fontFamily = LatoFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = AgoraGoldLight,
                textAlign = TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = LatoFamily,
                    fontSize = 12.sp,
                    color = AgoraGoldLight,
                    textAlign = TextAlign.Center,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Questions : Gemini AI (Google)",
                fontFamily = LatoFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = AgoraStone,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Photos : Unsplash · Pexels · Pixabay",
                fontFamily = LatoFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = AgoraStone,
                textAlign = TextAlign.Center
            )
        }
    }
}
