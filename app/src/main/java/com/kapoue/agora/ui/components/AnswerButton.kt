package com.kapoue.agora.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapoue.agora.ui.theme.AgoraCorrect
import com.kapoue.agora.ui.theme.AgoraCorrectLight
import com.kapoue.agora.ui.theme.AgoraGold
import com.kapoue.agora.ui.theme.AgoraSurface
import com.kapoue.agora.ui.theme.AgoraWhite
import com.kapoue.agora.ui.theme.AgoraWrong
import com.kapoue.agora.ui.theme.AgoraWrongLight
import com.kapoue.agora.ui.theme.LatoFamily

enum class AnswerState {
    NORMAL,
    CORRECT,
    WRONG_SELECTED,
    CORRECT_REVEALED
}

@Composable
fun AnswerButton(
    text: String,
    state: AnswerState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, borderColor, textColor) = when (state) {
        AnswerState.NORMAL -> Triple(
            AgoraSurface.copy(alpha = 0.8f),
            AgoraGold.copy(alpha = 0.5f),
            AgoraWhite
        )
        AnswerState.CORRECT -> Triple(
            AgoraCorrect,
            AgoraCorrectLight,
            Color.White
        )
        AnswerState.WRONG_SELECTED -> Triple(
            AgoraWrong,
            AgoraWrongLight,
            Color.White
        )
        AnswerState.CORRECT_REVEALED -> Triple(
            AgoraCorrect,
            AgoraCorrectLight,
            Color.White
        )
    }

    val animatedBg by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(durationMillis = 200),
        label = "bgColor"
    )
    val animatedBorder by animateColorAsState(
        targetValue = borderColor,
        animationSpec = tween(durationMillis = 200),
        label = "borderColor"
    )
    val animatedText by animateColorAsState(
        targetValue = textColor,
        animationSpec = tween(durationMillis = 200),
        label = "textColor"
    )
    val animatedBorderWidth by animateDpAsState(
        targetValue = if (state == AnswerState.NORMAL) 0.5.dp else 1.5.dp,
        animationSpec = tween(durationMillis = 200),
        label = "borderWidth"
    )
    val targetScale = if (state == AnswerState.CORRECT || state == AnswerState.WRONG_SELECTED) 0.97f else 1.0f
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = animatedBg,
            contentColor = animatedText,
            disabledContainerColor = animatedBg,
            disabledContentColor = animatedText
        ),
        border = BorderStroke(
            width = animatedBorderWidth,
            color = animatedBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .scale(animatedScale)
    ) {
        Text(
            text = text,
            fontFamily = LatoFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            color = animatedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}
