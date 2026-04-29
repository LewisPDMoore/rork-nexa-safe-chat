package com.rork.nexa.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

private enum class Tab(val label: String, val icon: ImageVector, val outlined: ImageVector) {
    Chats("Chats", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    Safety("Safety", Icons.Filled.Shield, Icons.Outlined.Shield),
    Settings("You", Icons.Filled.Person, Icons.Outlined.Person);
}

@Composable
fun RootScaffold(navController: NavController) {
    var selected by remember { mutableStateOf(Tab.Chats) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { NexaBottomBar(selected) { selected = it } },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "tab",
            ) { tab ->
                when (tab) {
                    Tab.Chats -> ChatsScreen(navController)
                    Tab.Safety -> SafetyScreen(navController)
                    Tab.Settings -> SettingsScreen(navController)
                }
            }
        }
    }
}

@Composable
private fun NexaBottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.values().forEach { tab ->
                NavItem(
                    tab = tab,
                    isSelected = tab == selected,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    tab: Tab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (isSelected) activeColor.copy(alpha = 0.14f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (isSelected) tab.icon else tab.outlined,
            contentDescription = tab.label,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(22.dp),
        )
        if (isSelected) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = tab.label,
                color = activeColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
    }
}
