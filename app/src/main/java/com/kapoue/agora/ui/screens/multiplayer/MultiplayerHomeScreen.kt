package com.kapoue.agora.ui.screens.multiplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapoue.agora.ui.theme.*

@Composable
fun MultiplayerHomeScreen(
    onOrganizeClick: () -> Unit,
    onJoinClick: () -> Unit,
    onBackClick: () -> Unit
) {
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
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Retour",
                    tint = AgoraStone
                )
            }
            Text(
                text = "MODE MULTIJOUEUR",
                fontFamily = CinzelFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                color = AgoraGold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Jouez ensemble,\nsur vos appareils",
            fontFamily = CinzelFamily,
            fontSize = 22.sp,
            color = AgoraWhite,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        )

        Text(
            text = "Sans réseau. Sans serveur.\nUniquement des QR codes.",
            fontFamily = LatoFamily,
            fontSize = 14.sp,
            color = AgoraStone,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Organiser
            Card(
                onClick = onOrganizeClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AgoraSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, AgoraGold.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Group,
                        contentDescription = null,
                        tint = AgoraGold,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Organiser une partie",
                            fontFamily = CinzelFamily,
                            fontSize = 16.sp,
                            color = AgoraGold
                        )
                        Text(
                            text = "Tu gères la session et affiches les QR codes",
                            fontFamily = LatoFamily,
                            fontSize = 13.sp,
                            color = AgoraStone
                        )
                    }
                }
            }

            // Card Rejoindre
            Card(
                onClick = onJoinClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AgoraSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, AgoraGold.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = AgoraGold,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Rejoindre une partie",
                            fontFamily = CinzelFamily,
                            fontSize = 16.sp,
                            color = AgoraGold
                        )
                        Text(
                            text = "Scanne le QR code de l'organisateur",
                            fontFamily = LatoFamily,
                            fontSize = 13.sp,
                            color = AgoraStone
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
