package com.rork.nexa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.nexa.models.AvatarGradients
import com.rork.nexa.models.VibeEmojis

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun VibePickerSheet(
    selectedEmoji: String,
    selectedGradient: Int,
    onPick: (emoji: String, gradient: Int) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Pick your vibe",
    subtitle: String = "Choose an emoji and color — change anytime.",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 6.dp)
        ) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))

            Text(
                "EMOJI",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(10.dp))
            EmojiGrid(selectedEmoji) { emoji -> onPick(emoji, selectedGradient) }

            Spacer(Modifier.height(20.dp))
            Text(
                "COLOR",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(10.dp))
            GradientRow(selectedGradient) { idx -> onPick(selectedEmoji, idx) }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun EmojiGrid(selected: String, onPick: (String) -> Unit) {
    val rows = VibeEmojis.chunked(5)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { e ->
                    val active = e == selected
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            )
                            .border(
                                if (active) 2.dp else 0.dp,
                                if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(18.dp),
                            )
                            .clickable { onPick(e) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(e, fontSize = 26.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun GradientRow(selected: Int, onPick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AvatarGradients.forEachIndexed { idx, g ->
            val active = idx == selected
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(g.start, g.end)))
                    .border(
                        if (active) 3.dp else 0.dp,
                        MaterialTheme.colorScheme.onBackground,
                        CircleShape,
                    )
                    .clickable { onPick(idx) },
            )
        }
    }
}
