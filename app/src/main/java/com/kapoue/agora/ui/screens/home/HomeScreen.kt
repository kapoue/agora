package com.kapoue.agora.ui.screens.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kapoue.agora.domain.model.Theme
import com.kapoue.agora.ui.components.ThemeCard
import com.kapoue.agora.ui.theme.AgoraBackground
import com.kapoue.agora.ui.theme.AgoraGold
import com.kapoue.agora.ui.theme.AgoraStone
import com.kapoue.agora.ui.theme.CinzelFamily

@Composable
fun HomeScreen(
    onThemeClick: (Theme) -> Unit,
    onAboutClick: () -> Unit,
    onMultiplayerClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val themeImages by viewModel.themeImages.collectAsState()
    val seriesCounts by viewModel.seriesCounts.collectAsState()
    val context = LocalContext.current

    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AGORA",
                fontFamily = CinzelFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                color = AgoraGold,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime > 2000L) tapCount = 0
                    lastTapTime = now
                    tapCount++
                    if (tapCount >= 5) {
                        tapCount = 0
                        val uri = viewModel.exportLogs()
                        if (uri != null) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "Agora Logs")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Exporter les logs"))
                        }
                    }
                }
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMultiplayerClick) {
                    Icon(
                        imageVector = Icons.Outlined.Group,
                        contentDescription = "Multijoueur",
                        tint = AgoraStone
                    )
                }
                IconButton(onClick = onAboutClick) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "À propos",
                        tint = AgoraStone
                    )
                }
            }
        }

        // Grille des thèmes
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(Theme.entries.sortedBy { if (it == Theme.CULTURE_GENERALE) 1 else 0 }) { theme ->
                ThemeCard(
                    theme = theme,
                    imageUrl = themeImages[theme],
                    onClick = { onThemeClick(theme) },
                    seriesCount = seriesCounts[theme] ?: 0,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
