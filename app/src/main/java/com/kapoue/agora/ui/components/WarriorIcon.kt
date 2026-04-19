package com.kapoue.agora.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.kapoue.agora.R
import com.kapoue.agora.domain.model.Difficulty

@Composable
fun WarriorIcon(
    difficulty: Difficulty,
    modifier: Modifier = Modifier
) {
    val resId = when (difficulty) {
        Difficulty.DEBUTANT -> R.drawable.warrior_beginner
        Difficulty.MOYEN -> R.drawable.warrior_medium
        Difficulty.EXPERT -> R.drawable.warrior_expert
    }
    Image(
        painter = painterResource(id = resId),
        contentDescription = difficulty.displayName,
        modifier = modifier
    )
}
