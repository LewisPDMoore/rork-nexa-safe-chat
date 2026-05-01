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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.rork.nexa.data.AppState
import com.rork.nexa.data.auth.AuthRepository
import com.rork.nexa.data.auth.Profile
import com.rork.nexa.models.AvatarGradients
import com.rork.nexa.viewmodels.ChatsViewModel
import kotlinx.coroutines.launch

@Composable
fun FriendProfileScreen(userId: String, navController: NavController) {
    val context = LocalContext.current
    val repo = remember { AuthRepository.get(context) }
    val chatsVm: ChatsViewModel = viewModel()
    val scope = rememberCoroutineScope()

    var profile by remember { mutableStateOf<Profile?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showNickname by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var blocked by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        loading = true
        profile = repo.fetchProfileById(userId).getOrNull()
        loading = false
    }

    val p = profile
    val nickname = AppState.nicknames[userId]
    val displayName = p?.displayName?.takeIf { it.isNotBlank() }
        ?: p?.username?.replaceFirstChar { it.uppercase() }
        ?: "User"
    val title = nickname?.takeIf { it.isNotBlank() } ?: displayName

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp),
        ) {
            HeroAvatar(profile = p)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent, Color.Black.copy(alpha = 0.55f))
                        )
                    ),
            )
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                    )
                    if (!p?.avatarEmoji.isNullOrBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(p?.avatarEmoji ?: "", fontSize = 22.sp)
                    }
                }
                if (nickname != null && nickname.isNotBlank()) {
                    Text(
                        displayName,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                    )
                }
                Text(
                    "@${p?.username ?: ""}",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                )
            }
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
        ActionRow(
            icon = Icons.Outlined.DriveFileRenameOutline,
            title = if (nickname.isNullOrBlank()) "Set nickname" else "Change nickname (\"$nickname\")",
            subtitle = "Only you'll see it",
            onClick = { showNickname = true },
        )
        Divider()
        ActionRow(
            icon = Icons.Outlined.Flag,
            title = "Report this user",
            subtitle = "Send to our moderation team",
            onClick = { showReport = true },
        )
        Divider()
        ActionRow(
            icon = Icons.Outlined.Block,
            title = if (blocked) "Unblock" else "Block",
            subtitle = if (blocked) "They can message you again" else "Hide them from chats",
            danger = true,
            onClick = {
                blocked = !blocked
                Toast.makeText(
                    context,
                    if (blocked) "Blocked. They won't reach you." else "Unblocked.",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }

    if (showNickname) {
        NicknameDialog(
            initial = nickname.orEmpty(),
            onDismiss = { showNickname = false },
            onSave = { v ->
                chatsVm.setNickname(userId, v.takeIf { it.isNotBlank() })
                showNickname = false
            },
            onClear = {
                chatsVm.setNickname(userId, null)
                showNickname = false
            },
        )
    }

    if (showReport) {
        AlertDialog(
            onDismissRequest = { showReport = false },
            title = { Text("Report ${p?.username ?: "user"}") },
            text = { Text("Our team will review this account. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    showReport = false
                    scope.launch {
                        val r = repo.fileReport(
                            targetUserId = userId,
                            targetMessageId = null,
                            kind = "user",
                            reason = "Reported from profile",
                        )
                        Toast.makeText(
                            context,
                            if (r.isSuccess) "Report sent."
                            else r.exceptionOrNull()?.message ?: "Couldn't send report.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }) { Text("Report") }
            },
            dismissButton = { TextButton(onClick = { showReport = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun HeroAvatar(profile: Profile?) {
    val photos = profile?.photos.orEmpty()
    if (photos.isNotEmpty()) {
        val pagerState = rememberPagerState(pageCount = { photos.size })
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model = photos[page],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (photos.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    photos.forEachIndexed { i, _ ->
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .width(if (i == pagerState.currentPage) 22.dp else 12.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (i == pagerState.currentPage) Color.White
                                    else Color.White.copy(alpha = 0.4f)
                                ),
                        )
                    }
                }
            }
        }
    } else {
        val gradient = AvatarGradients[(profile?.avatarGradient ?: 0).coerceIn(0, AvatarGradients.lastIndex)]
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(gradient.start, gradient.end))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                profile?.avatarEmoji?.takeIf { it.isNotBlank() }
                    ?: profile?.username?.take(1)?.uppercase() ?: "?",
                fontSize = 100.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                title,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 72.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun NicknameDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.isBlank()) "Set nickname" else "Change nickname") },
        text = {
            Column {
                Text(
                    "Only you will see this nickname.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    if (value.isEmpty()) {
                        Text("Nickname", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = { value = it.take(30) },
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value.trim()) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (initial.isNotBlank()) {
                    TextButton(onClick = onClear) { Text("Clear") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
