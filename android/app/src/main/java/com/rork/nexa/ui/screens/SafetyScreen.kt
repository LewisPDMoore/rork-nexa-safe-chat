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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rork.nexa.data.AppState

@Composable
fun SafetyScreen(navController: NavController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { SafetyHeader() }
        item { ShieldStatusCard() }
        item { StatsRow() }
        item {
            Text(
                "How Shield works",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp),
                fontSize = 16.sp,
            )
        }
        item {
            ExplainerCard(
                emoji = "🧠",
                title = "Smart message check",
                body = "Shield reads your drafts on-device and gives a friendly nudge if something sounds harsh.",
            )
        }
        item {
            ExplainerCard(
                emoji = "🖼️",
                title = "Image safety",
                body = "Risky images get blurred until you tap to confirm.",
            )
        }
        item {
            ExplainerCard(
                emoji = "🛟",
                title = "Heads-up alerts",
                body = "If a chat starts feeling off, you'll see a calm note here.",
            )
        }
        if (AppState.supervisedByParent) {
            item { Spacer(Modifier.height(4.dp)) }
            item {
                Text(
                    "Family",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    fontSize = 16.sp,
                )
            }
            item {
                LinkedToParentCard(onOpen = { navController.navigate("parent") })
            }
        }
    }
}

@Composable
private fun SafetyHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            "Safety",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            "Shield is watching quietly",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ShieldStatusCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                    )
                )
            )
            .padding(22.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Your safety score",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.22f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        "All clear ✨",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "100",
                    color = Color.White,
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "/ 100",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Nothing to worry about right now.",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun StatsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatTile(
            icon = Icons.Outlined.AutoAwesome,
            tint = MaterialTheme.colorScheme.primary,
            value = "0",
            label = "messages\nsoftened",
            modifier = Modifier.weight(1f),
        )
        StatTile(
            icon = Icons.Outlined.Photo,
            tint = MaterialTheme.colorScheme.tertiary,
            value = "0",
            label = "images\nblurred",
            modifier = Modifier.weight(1f),
        )
        StatTile(
            icon = Icons.Outlined.VolumeOff,
            tint = MaterialTheme.colorScheme.secondary,
            value = "0",
            label = "on quiet\nhours",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    tint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun ExplainerCard(emoji: String, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun LinkedToParentCard(onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
            .clickable { onOpen() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.SupervisorAccount,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Linked to a parent",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
            )
            Text(
                "Transparent supervision \u00b7 summaries only",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}
