package com.kapoue.agora.ui.screens.multiplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Image
import com.kapoue.agora.ui.theme.*
import com.kapoue.agora.ui.util.QrCodeGenerator

private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=fr.kapoue.agora"

@Composable
fun MultiplayerHomeScreen(
    onOrganizeClick: () -> Unit,
    onJoinClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var showShareDialog by remember { mutableStateOf(false) }
    val shareBitmap = remember { QrCodeGenerator.generate(PLAY_STORE_URL) }

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
                .padding(horizontal = 24.dp)
        ) {
            // ── Boutons de jeu ────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

            // ── Séparateur ────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = AgoraGold.copy(alpha = 0.15f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(20.dp))

            // ── Bouton partage ────────────────────────────────────────────────
            Card(
                onClick = { showShareDialog = true },
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
                        imageVector = Icons.Outlined.QrCode2,
                        contentDescription = null,
                        tint = AgoraGold,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Inviter un ami",
                            fontFamily = CinzelFamily,
                            fontSize = 16.sp,
                            color = AgoraGold
                        )
                        Text(
                            text = "Un ami n'a pas encore l'app ?",
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

    // ── Dialog QR code partage ─────────────────────────────────────────────────
    if (showShareDialog) {
        Dialog(onDismissRequest = { showShareDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AgoraSurface)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Partager l'application",
                        fontFamily = CinzelFamily,
                        fontSize = 16.sp,
                        color = AgoraGold
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Image(
                        bitmap = shareBitmap.asImageBitmap(),
                        contentDescription = "QR code Agora Play Store",
                        modifier = Modifier.size(220.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scanner pour télécharger Agora",
                        fontFamily = LatoFamily,
                        fontSize = 13.sp,
                        color = AgoraStone,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showShareDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = AgoraGold),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Fermer",
                            fontFamily = CinzelFamily,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}
