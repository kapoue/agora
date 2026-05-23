package com.kapoue.agora.ui.screens.multiplayer.participant

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView
import com.kapoue.agora.ui.theme.*

@Composable
fun ParticipantScanScreen(
    onSessionReady: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: ParticipantScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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

    LaunchedEffect(uiState.sessionLoaded) {
        if (uiState.sessionLoaded) onSessionReady()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgoraBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Retour",
                    tint = AgoraStone
                )
            }
        }

        Text(
            text = "REJOINDRE LA PARTIE",
            fontFamily = CinzelFamily,
            fontSize = 20.sp,
            color = AgoraGold
        )

        Text(
            text = "Scanne le QR code affiché\npar l'organisateur",
            fontFamily = LatoFamily,
            fontSize = 14.sp,
            color = AgoraStone,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        BarcodeView(ctx).apply {
                            decodeContinuous(object : BarcodeCallback {
                                override fun barcodeResult(result: BarcodeResult) {
                                    viewModel.onQrScanned(result.text)
                                    // pause uniquement si le chargement a démarré (scan valide)
                                    // en cas d'erreur, on laisse la caméra active pour retry
                                    if (viewModel.uiState.value.isLoading) pause()
                                }
                            })
                            resume()
                        }
                    },
                    onRelease = { it.pause() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Permission caméra requise", fontFamily = LatoFamily, color = AgoraStone)
            }
        }

        uiState.error?.let { error ->
            Text(
                text = error,
                fontFamily = LatoFamily,
                fontSize = 13.sp,
                color = AgoraWrongLight,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp, start = 32.dp, end = 32.dp)
            )
        }

        if (uiState.isLoading) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(
                color = AgoraGold,
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Chargement des questions…",
                fontFamily = LatoFamily,
                fontSize = 13.sp,
                color = AgoraStone,
                textAlign = TextAlign.Center
            )
        }
    }
}
