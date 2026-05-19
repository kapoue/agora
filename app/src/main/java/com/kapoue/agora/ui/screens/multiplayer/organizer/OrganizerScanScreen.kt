package com.kapoue.agora.ui.screens.multiplayer.organizer

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompactBarcodeView
import com.kapoue.agora.ui.theme.*

@Composable
fun OrganizerScanScreen(
    onDone: () -> Unit,
    viewModel: OrganizerScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sm = viewModel.sessionManager
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCameraPermission = it }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SCAN DES RÉSULTATS — Manche ${sm.currentRound}/${sm.totalRounds}",
                fontFamily = CinzelFamily,
                fontSize = 13.sp,
                color = AgoraGold
            )
            Text(
                text = "${uiState.scannedPlayers.size} scanné(s)",
                fontFamily = LatoFamily,
                fontSize = 13.sp,
                color = AgoraStone
            )
        }

        // Scanner caméra
        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        CompactBarcodeView(ctx).apply {
                            decodeContinuous(object : BarcodeCallback {
                                override fun barcodeResult(result: BarcodeResult) {
                                    viewModel.onQrScanned(result.text)
                                }
                            })
                            resume()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Permission caméra requise", fontFamily = LatoFamily, color = AgoraStone)
            }
        }

        // Erreurs
        (uiState.duplicateError ?: uiState.invalidQrError)?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = AgoraWrong.copy(alpha = 0.2f))
            ) {
                Text(
                    text = error,
                    fontFamily = LatoFamily,
                    fontSize = 13.sp,
                    color = AgoraWrongLight,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Liste des joueurs scannés
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(uiState.scannedPlayers) { player ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = AgoraSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = AgoraCorrect,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = player.name,
                            fontFamily = CinzelFamily,
                            fontSize = 15.sp,
                            color = AgoraWhite,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${player.score}/${player.totalQuestions}",
                            fontFamily = LatoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AgoraGold
                        )
                        val seconds = player.timeMillis / 1000
                        Text(
                            text = "${seconds}s",
                            fontFamily = LatoFamily,
                            fontSize = 13.sp,
                            color = AgoraStone
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                viewModel.onDone()
                onDone()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AgoraGold)
        ) {
            Text(
                "Voir le classement",
                fontFamily = CinzelFamily,
                color = AgoraBackground,
                fontSize = 16.sp
            )
        }
    }
}
