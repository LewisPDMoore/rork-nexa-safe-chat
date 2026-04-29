package com.rork.nexa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.nexa.models.AvatarGradient
import com.rork.nexa.models.AvatarGradients

@Composable
fun EmojiAvatar(
    emoji: String,
    gradientIndex: Int,
    size: Dp = 48.dp,
    fallbackInitials: String = "",
    modifier: Modifier = Modifier,
) {
    val gradient: AvatarGradient = AvatarGradients[gradientIndex.coerceIn(0, AvatarGradients.lastIndex)]
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(gradient.start, gradient.end))),
        contentAlignment = Alignment.Center,
    ) {
        if (emoji.isNotBlank()) {
            Text(emoji, fontSize = (size.value / 1.9f).sp)
        } else {
            Text(
                fallbackInitials.take(2).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value / 2.6f).sp,
            )
        }
    }
}
