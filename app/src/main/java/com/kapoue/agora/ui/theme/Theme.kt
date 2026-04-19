package com.kapoue.agora.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AgoraDarkColorScheme = darkColorScheme(
    primary = AgoraGold,
    onPrimary = AgoraBackground,
    secondary = AgoraGoldLight,
    onSecondary = AgoraBackground,
    background = AgoraBackground,
    onBackground = AgoraWhite,
    surface = AgoraSurface,
    onSurface = AgoraWhite,
    surfaceVariant = AgoraSurface,
    onSurfaceVariant = AgoraStone,
    error = AgoraWrong,
    onError = AgoraWhite
)

val AgoraTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontFamily = CinzelFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        color = AgoraGold
    ),
    displayMedium = TextStyle(
        fontFamily = CinzelFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        color = AgoraGold
    ),
    displaySmall = TextStyle(
        fontFamily = CinzelFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        color = AgoraGold
    ),
    headlineLarge = TextStyle(
        fontFamily = CinzelFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        color = AgoraWhite
    ),
    headlineMedium = TextStyle(
        fontFamily = CinzelFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        color = AgoraWhite
    ),
    titleLarge = TextStyle(
        fontFamily = CinzelFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        color = AgoraWhite
    ),
    bodyLarge = TextStyle(
        fontFamily = LatoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = AgoraWhite
    ),
    bodyMedium = TextStyle(
        fontFamily = LatoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = AgoraStone
    ),
    bodySmall = TextStyle(
        fontFamily = LatoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = AgoraStone
    ),
    labelLarge = TextStyle(
        fontFamily = LatoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = AgoraWhite
    )
)

@Composable
fun AgoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AgoraDarkColorScheme,
        typography = AgoraTypography,
        shapes = AgoraShapes,
        content = content
    )
}
