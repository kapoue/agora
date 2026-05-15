package com.kapoue.agora.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kapoue.agora.domain.model.Theme
import com.kapoue.agora.ui.theme.AgoraGold
import com.kapoue.agora.ui.theme.AgoraWhite
import com.kapoue.agora.ui.theme.CinzelFamily
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ThemeCard(
    theme: Theme,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    seriesCount: Int = 0
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "scale"
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(160.dp)
            .scale(scale)
            .clickable {
                isPressed = true
                onClick()
            },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        border = BorderStroke(1.dp, AgoraGold.copy(alpha = 0.35f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = theme.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = painterResource(id = theme.placeholderRes),
                    placeholder = painterResource(id = theme.placeholderRes)
                )
            } else {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = theme.placeholderRes),
                    contentDescription = theme.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Dégradé noir en bas pour la lisibilité du texte
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xCC000000)
                            )
                        )
                    )
            )

            // Nom du thème
            Text(
                text = theme.displayName,
                fontFamily = CinzelFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = AgoraWhite,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )

            // Badge étoile 10 pointes (visible à partir de 3 séries)
            if (seriesCount >= 3) {
                val goldColor = AgoraGold
                val bgColor = Color(0xDD000000)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(44.dp)) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val outerR = size.width / 2f
                        val innerR = outerR * 0.68f
                        val points = 10
                        val path = Path()
                        for (i in 0 until points * 2) {
                            val angle = Math.PI * i / points - Math.PI / 2
                            val r = if (i % 2 == 0) outerR else innerR
                            val x = cx + (r * cos(angle)).toFloat()
                            val y = cy + (r * sin(angle)).toFloat()
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(path, color = bgColor)
                        drawPath(path, color = goldColor.copy(alpha = 0.9f),
                            style = Stroke(width = 2.dp.toPx()))
                    }
                    Text(
                        text = seriesCount.toString(),
                        fontFamily = CinzelFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (seriesCount >= 100) 11.sp else 14.sp,
                        color = AgoraGold
                    )
                }
            }
        }
    }
}
