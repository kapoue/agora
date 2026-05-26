package com.kapoue.agora.ui.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import com.kapoue.agora.BuildConfig
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgoraBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = AgoraStone
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                text = "Version ${BuildConfig.VERSION_NAME}",
                fontFamily = LatoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AgoraStone,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Questions : Mistral AI",
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
