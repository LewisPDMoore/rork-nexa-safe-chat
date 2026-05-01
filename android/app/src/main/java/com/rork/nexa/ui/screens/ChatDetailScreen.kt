package com.rork.nexa.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rork.nexa.viewmodels.ChatDetailViewModel
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.rork.nexa.data.auth.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.rork.nexa.data.MessageRisk
import com.rork.nexa.data.analyseMessage
import com.rork.nexa.data.softerSuggestion
import com.rork.nexa.models.Message
import com.rork.nexa.ui.components.MediaPickerSheet
import com.rork.nexa.ui.components.ProfileAvatar
import com.rork.nexa.ui.components.ReactionBar
import com.rork.nexa.ui.components.SnapComposer
import com.rork.nexa.ui.components.SnapViewer
import com.rork.nexa.ui.components.rememberMediaCapture
import com.rork.nexa.ui.components.resolveImageMime

@Composable
fun ChatDetailScreen(chatId: String, navController: NavController) {
    val vm: ChatDetailViewModel = viewModel()
    LaunchedEffect(chatId) { vm.bind(chatId) }
    val messages by vm.messages.collectAsStateWithLifecycle()
    val isRemoteTyping by vm.isRemoteTyping.collectAsStateWithLifecycle()
    val peerName by vm.peerName.collectAsStateWithLifecycle()
    val peerUsername by vm.peerUsername.collectAsStateWithLifecycle()
    val peerInitials by vm.peerInitials.collectAsStateWithLifecycle()
    val peerColor by vm.peerColor.collectAsStateWithLifecycle()
    val peerPhotoUrl by vm.peerPhotoUrl.collectAsStateWithLifecycle()
    val peerUserId by vm.peerUserId.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    val risk by remember { derivedStateOf { analyseMessage(input) } }
    val suggestion by remember { derivedStateOf { softerSuggestion(input) } }
    val listState = rememberLazyListState()
    var reactionTargetId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val repo = remember { AuthRepository.get(context) }
    val scope = rememberCoroutineScope()
    var showReport by remember { mutableStateOf(false) }
    var pickedImage by remember { mutableStateOf<Uri?>(null) }
    var sending by remember { mutableStateOf(false) }
    var snapView by remember { mutableStateOf<Message?>(null) }
    var showCameraSheet by remember { mutableStateOf(false) }

    val capture = rememberMediaCapture(onUri = { uri -> pickedImage = uri })

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        ChatHeader(
            name = peerName.ifBlank { "Chat" },
            username = peerUsername,
            photoUrl = peerPhotoUrl,
            initials = peerInitials.ifBlank { "?" },
            avatarColor = Color(peerColor),
            isTyping = isRemoteTyping,
            onBack = { navController.popBackStack() },
            onCall = {
                Toast.makeText(context, "Calling feature coming soon", Toast.LENGTH_SHORT).show()
            },
            onReport = { showReport = true },
            onProfile = { peerUserId?.let { navController.navigate("profile/$it") } },
            canReport = peerUserId != null,
        )

        if (messages.isEmpty()) {
            EmptyConversation(
                name = peerName.ifBlank { "them" },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 14.dp, vertical = 14.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item { EncryptionTag() }
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(
                        msg = msg,
                        showReactions = reactionTargetId == msg.id,
                        onLongPress = { reactionTargetId = if (reactionTargetId == msg.id) null else msg.id },
                        onPickReaction = { emoji ->
                            vm.toggleReaction(msg.id, emoji)
                            reactionTargetId = null
                        },
                        onTapSnap = { snapView = msg },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = risk != MessageRisk.Low && suggestion != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            SoftSuggestionCard(
                suggestion = suggestion ?: "",
                onUse = { suggestion?.let { input = it } },
            )
        }

        if (showReport) {
            ReportUserDialog(
                username = peerName.ifBlank { "this user" },
                onDismiss = { showReport = false },
                onSubmit = { reason ->
                    val targetId = peerUserId
                    showReport = false
                    if (targetId != null) {
                        scope.launch {
                            val r = repo.fileReport(
                                targetUserId = targetId,
                                targetMessageId = null,
                                kind = "user",
                                reason = reason,
                            )
                            val msg = if (r.isSuccess) "Report submitted. Thanks for letting us know."
                                else r.exceptionOrNull()?.message ?: "Couldn't submit report."
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
            )
        }

        ChatInputBar(
            value = input,
            onChange = {
                input = it
                vm.onInputChange(it)
            },
            onSend = {
                if (input.isNotBlank()) {
                    vm.send(input)
                    input = ""
                }
            },
            onCamera = { showCameraSheet = true },
        )
    }

    if (showCameraSheet) {
        MediaPickerSheet(
            title = "Send a photo",
            onDismiss = { showCameraSheet = false },
            onTakePhoto = {
                showCameraSheet = false
                capture.takePhoto()
            },
            onPickPhoto = {
                showCameraSheet = false
                capture.pickPhoto()
            },
        )
    }

    val composerImage = pickedImage
    if (composerImage != null) {
        SnapComposer(
            imageUri = composerImage,
            sending = sending,
            onCancel = { if (!sending) pickedImage = null },
            onSend = { caption, timer ->
                sending = true
                scope.launch {
                    val mime = resolveImageMime(context, composerImage)
                    val bytes = withContext(Dispatchers.IO) {
                        runCatching { context.contentResolver.openInputStream(composerImage)?.use { it.readBytes() } }
                            .getOrNull()
                    }
                    if (bytes == null || bytes.isEmpty()) {
                        sending = false
                        Toast.makeText(context, "Couldn't read image.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    vm.sendImage(bytes, mime, caption, timer) { err ->
                        sending = false
                        if (err == null) {
                            pickedImage = null
                        } else {
                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
        )
    }

    val viewing = snapView
    if (viewing?.imageUrl != null && viewing.imageTimer != null && viewing.imageTimer > 0 && !viewing.viewed) {
        SnapViewer(
            imageUrl = viewing.imageUrl,
            timerSeconds = viewing.imageTimer,
            caption = viewing.text.takeIf { it.isNotBlank() },
            onClose = {
                vm.markSnapViewed(viewing.id)
                snapView = null
            },
        )
    }
}

@Composable
private fun ChatHeader(
    name: String,
    username: String,
    photoUrl: String?,
    initials: String,
    avatarColor: Color,
    isTyping: Boolean,
    onBack: () -> Unit,
    onCall: () -> Unit,
    onReport: () -> Unit,
    onProfile: () -> Unit,
    canReport: Boolean,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBtn(Icons.AutoMirrored.Filled.ArrowBack, onBack)
        Spacer(Modifier.width(4.dp))
        Box(modifier = Modifier.clip(CircleShape).clickable { onProfile() }) {
            ProfileAvatar(
                photoUrl = photoUrl,
                emoji = null,
                gradientIndex = 0,
                fallbackInitials = initials,
                size = 40.dp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onProfile() },
        ) {
            Text(
                name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (username.isNotBlank()) {
                Text(
                    "@$username${if (isTyping) " · typing\u2026" else ""}",
                    fontSize = 11.sp,
                    color = if (isTyping) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isTyping) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
        IconBtn(Icons.Outlined.Phone, onCall)
        Spacer(Modifier.width(4.dp))
        Box {
            IconBtn(Icons.Outlined.MoreVert) { menuOpen = true }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(Icons.Outlined.AccountCircle, null, modifier = Modifier.size(18.dp))
                    },
                    text = { Text("Profile") },
                    onClick = { menuOpen = false; onProfile() },
                )
                DropdownMenuItem(
                    text = { Text(if (canReport) "Report user" else "Report unavailable") },
                    enabled = canReport,
                    onClick = {
                        menuOpen = false
                        if (canReport) onReport()
                    },
                )
            }
        }
    }
}

@Composable
private fun ReportUserDialog(
    username: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    val reasons = listOf(
        "Bullying or harassment",
        "Hate speech",
        "Inappropriate content",
        "Threats or violence",
        "Spam or scam",
        "Something else",
    )
    var selected by remember { mutableStateOf(reasons.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report $username") },
        text = {
            Column {
                Text(
                    "Pick what's happening. Our team will review.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(10.dp))
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selected = reason }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selected == reason,
                            onClick = { selected = reason },
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(reason, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(selected) }) { Text("Submit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun IconBtn(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun EncryptionTag() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "End-to-end encrypted",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyConversation(name: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Say hi to ${name.split(" ").first()} \uD83D\uDC4B",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Messages here are encrypted end-to-end.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    msg: Message,
    showReactions: Boolean,
    onLongPress: () -> Unit,
    onPickReaction: (String) -> Unit,
    onTapSnap: () -> Unit,
) {
    val isMe = msg.isMe
    val bubbleShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (isMe) 20.dp else 6.dp,
        bottomEnd = if (isMe) 6.dp else 20.dp,
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = showReactions,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(if (isMe) Alignment.End else Alignment.Start),
        ) {
            ReactionBar(
                onPick = onPickReaction,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        ) {
            Column(
                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                modifier = Modifier.widthIn(max = 280.dp),
            ) {
                when {
                    msg.imageUrl != null && msg.imageTimer != null && msg.imageTimer > 0 -> {
                        SnapBubble(
                            isMe = isMe,
                            timer = msg.imageTimer,
                            viewed = msg.viewed,
                            caption = msg.text,
                            onTap = { if (!msg.viewed && !isMe) onTapSnap() },
                            onLongPress = onLongPress,
                        )
                    }
                    msg.imageUrl != null -> {
                        ImageBubble(
                            url = msg.imageUrl,
                            caption = msg.text,
                            isMe = isMe,
                            onLongPress = onLongPress,
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .clip(bubbleShape)
                                .then(
                                    if (isMe) {
                                        Modifier.background(
                                            Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.tertiary,
                                                )
                                            )
                                        )
                                    } else {
                                        Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                    }
                                )
                                .combinedClickable(onClick = {}, onLongClick = onLongPress)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text = msg.text,
                                color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.5.sp,
                            )
                        }
                    }
                }
                if (msg.reactions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        msg.reactions.forEach { Text(it, fontSize = 12.sp) }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    msg.timestamp,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageBubble(
    url: String,
    caption: String,
    isMe: Boolean,
    onLongPress: () -> Unit,
) {
    Column(
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 240.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .combinedClickable(onClick = {}, onLongClick = onLongPress),
        ) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .aspectRatio(0.8f),
            )
        }
        if (caption.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isMe) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    caption,
                    color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SnapBubble(
    isMe: Boolean,
    timer: Int,
    viewed: Boolean,
    caption: String,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val unviewedBrush = Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
    )
    Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
        Row(
            modifier = Modifier
                .clip(shape)
                .then(
                    if (viewed) Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    else Modifier.background(unviewedBrush)
                )
                .combinedClickable(onClick = onTap, onLongClick = onLongPress)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (viewed) Icons.Outlined.Visibility else Icons.Outlined.LockClock,
                contentDescription = null,
                tint = if (viewed) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    if (isMe) (if (viewed) "Snap sent" else "Snap · ${timer}s")
                    else (if (viewed) "Viewed" else "Tap to view · ${timer}s"),
                    color = if (viewed) MaterialTheme.colorScheme.onSurface else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                if (!isMe && !viewed) {
                    Text(
                        "Disappears after viewing",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                    )
                }
            }
        }
        if (caption.isNotBlank() && viewed) {
            Spacer(Modifier.height(4.dp))
            Text(
                caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SoftSuggestionCard(suggestion: String, onUse: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "That might come across a bit harsh \u2014 want to rephrase?",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Try: \u201C$suggestion\u201D",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onUse() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                "Use",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onChange: (String) -> Unit,
    onSend: () -> Unit,
    onCamera: () -> Unit,
) {
    val sendEnabled = value.isNotBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CameraButton(onClick = onCamera)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            "Message",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onChange,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.AddReaction,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (sendEnabled) {
            SendButton(onSend)
        } else {
            InputAction(Icons.Outlined.Mic)
        }
    }
}

@Composable
private fun CameraButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                    )
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoCamera,
            contentDescription = "Send a snap",
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun InputAction(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .clickable { },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SendButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                    )
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send",
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}
