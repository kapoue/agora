package com.kapoue.agora.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding

@Composable
fun HomeScreen(
    onThemeClick: (Theme) -> Unit,
    onAboutClick: () -> Unit,
    onMultiplayerClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val themeImages by viewModel.themeImages.collectAsState()
    val seriesCounts by viewModel.seriesCounts.collectAsState()

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
                color = AgoraGold
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
            items(Theme.entries) { theme ->
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
