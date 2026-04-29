package com.rork.nexa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rork.nexa.data.AppState
import com.rork.nexa.models.AvatarGradients
import com.rork.nexa.models.VibeEmojis
import com.rork.nexa.ui.components.AuthPrimaryButton
import com.rork.nexa.ui.components.EmojiAvatar
import com.rork.nexa.viewmodels.AuthViewModel

@Composable
fun VibePickScreen(
    onDone: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    var emoji by remember { mutableStateOf(AppState.avatarEmoji.ifBlank { "😎" }) }
    var gradient by remember { mutableIntStateOf(AppState.avatarGradientIndex) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            "Pick your vibe",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Choose an emoji and color — this is your avatar.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(28.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            EmojiAvatar(
                emoji = emoji,
                gradientIndex = gradient,
                size = 132.dp,
            )
        }
        Spacer(Modifier.height(28.dp))

        Text(
            "EMOJI",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(10.dp))
        val rows = VibeEmojis.chunked(5)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { e ->
                        val active = e == emoji
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
                                .clickable { emoji = e },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(e, fontSize = 26.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "COLOR",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AvatarGradients.forEachIndexed { idx, g ->
                val active = idx == gradient
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
                        .clickable { gradient = idx },
                )
            }
        }
        Spacer(Modifier.height(40.dp))
        AuthPrimaryButton(label = "Finish", onClick = {
            AppState.avatarEmoji = emoji
            AppState.avatarGradientIndex = gradient
            AppState.vibeEmoji = emoji
            viewModel.saveAvatar(emoji, gradient, onDone)
        })
        Spacer(Modifier.height(20.dp))
    }
}
