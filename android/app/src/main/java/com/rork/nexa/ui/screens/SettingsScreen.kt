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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.rork.nexa.BuildConfig
import com.rork.nexa.data.auth.AppConfigRow
import com.rork.nexa.data.auth.AuthRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rork.nexa.data.AppState
import com.rork.nexa.data.ShieldLevel
import com.rork.nexa.data.auth.SessionStatus
import com.rork.nexa.data.auth.StoryRow
import com.rork.nexa.ui.components.AccountEditSheet
import com.rork.nexa.ui.components.AccountField
import com.rork.nexa.ui.components.MediaPickerSheet
import com.rork.nexa.ui.components.ProfileAvatar
import com.rork.nexa.ui.components.ProfilePickerSheet
import com.rork.nexa.ui.components.rememberMediaCapture
import com.rork.nexa.ui.components.resolveImageMime
import coil3.compose.AsyncImage
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.rork.nexa.ui.theme.ThemeMode
import com.rork.nexa.viewmodels.AuthViewModel

@Composable
fun SettingsScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val status by authViewModel.status.collectAsStateWithLifecycle()
    val profile = (status as? SessionStatus.Authenticated)?.profile
    val isAdmin = profile?.isAdmin == true
    val isChild = !profile?.parentId.isNullOrBlank()
    var editing by remember { mutableStateOf<AccountField?>(null) }
    var showProfilePicker by remember { mutableStateOf(false) }
    var showStoryPicker by remember { mutableStateOf(false) }
    var postingStory by remember { mutableStateOf(false) }
    var myStories by remember { mutableStateOf<List<StoryRow>>(emptyList()) }
    val context = LocalContext.current
    val repo = remember { AuthRepository.get(context) }
    val storyScope = rememberCoroutineScope()
    val myUserId = profile?.id

    LaunchedEffect(myUserId) {
        if (myUserId != null) {
            myStories = repo.fetchActiveStories(myUserId).getOrNull().orEmpty()
        } else {
            myStories = emptyList()
        }
    }

    val storyCapture = rememberMediaCapture(onUri = { uri ->
        postingStory = true
        storyScope.launch {
            val mime = resolveImageMime(context, uri)
            val bytes = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
                    .getOrNull()
            }
            if (bytes == null || bytes.isEmpty()) {
                postingStory = false
                Toast.makeText(context, "Couldn't read that image.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            authViewModel.postStory(bytes, mime) { err ->
                postingStory = false
                if (err != null) {
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Posted to your story.", Toast.LENGTH_SHORT).show()
                    myUserId?.let { uid ->
                        storyScope.launch {
                            myStories = repo.fetchActiveStories(uid).getOrNull().orEmpty()
                        }
                    }
                }
            }
        }
    })
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.statusBarsPadding())
        Text(
            "You",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        ProfileCard(
            onTapAvatar = { showProfilePicker = true },
            activeStory = myStories.firstOrNull(),
            posting = postingStory,
        )

        SettingsSection(title = "Account") {
            LinkRow(
                icon = Icons.Outlined.Person,
                title = "Display name",
                subtitle = AppState.displayName.ifBlank { "Set your name" },
                showChevron = true,
                onClick = { editing = AccountField.DisplayName },
            )
            Divider()
            LinkRow(
                icon = Icons.Outlined.AlternateEmail,
                title = "Username",
                subtitle = "@${AppState.username.ifBlank { "username" }}",
                showChevron = true,
                onClick = { editing = AccountField.Username },
            )
            Divider()
            LinkRow(
                icon = Icons.Outlined.Mail,
                title = "Email",
                subtitle = AppState.email.ifBlank { "Add an email" },
                showChevron = true,
                onClick = { editing = AccountField.Email },
            )
            Divider()
            LinkRow(
                icon = Icons.Outlined.Lock,
                title = "Password",
                subtitle = "Change your password",
                showChevron = true,
                onClick = { editing = AccountField.Password },
            )
            Divider()
            LinkRow(
                icon = Icons.Outlined.AccountCircle,
                title = "Profile picture",
                subtitle = if (AppState.photos.isNotEmpty()) "${AppState.photos.size} photo${if (AppState.photos.size > 1) "s" else ""}" else "Add a photo or pick a vibe",
                showChevron = true,
                onClick = { showProfilePicker = true },
            )
            Divider()
            LinkRow(
                icon = Icons.Outlined.AddCircle,
                title = if (postingStory) "Posting story\u2026" else "Add to story",
                subtitle = when {
                    postingStory -> "Uploading\u2026"
                    myStories.isNotEmpty() -> "${myStories.size} active \u00b7 expires in 24h"
                    else -> "Share a photo for 24 hours"
                },
                showChevron = true,
                onClick = { if (!postingStory) showStoryPicker = true },
            )
        }

        SettingsSection(title = "Appearance") {
            ThemePicker(AppState.themeMode) { AppState.themeMode = it }
        }

        SettingsSection(title = "Safety") {
            ShieldLevelPicker(AppState.shieldLevel) { AppState.shieldLevel = it }
            Divider()
            ToggleRow(
                icon = Icons.Outlined.Block,
                title = "Limit unknown contacts",
                subtitle = "Strangers go to a request inbox first",
                checked = true,
                onCheckedChange = {},
            )
            Divider()
            ToggleRow(
                icon = Icons.Outlined.Bedtime,
                title = "Quiet hours",
                subtitle = "Pause non-friend messages 22:00 \u2013 07:00",
                checked = true,
                onCheckedChange = {},
            )
        }

        if (!isChild) {
            SettingsSection(title = "Family") {
                LinkRow(
                    icon = Icons.Outlined.FamilyRestroom,
                    title = "Family Center",
                    subtitle = "Create a child account linked to you",
                    showChevron = true,
                    onClick = { navController.navigate("family") },
                )
                Divider()
                ToggleRow(
                    icon = Icons.Outlined.SupervisorAccount,
                    title = "Supervision preview",
                    subtitle = "See what a parent dashboard looks like",
                    checked = AppState.supervisedByParent,
                    onCheckedChange = { AppState.supervisedByParent = it },
                )
                if (AppState.supervisedByParent) {
                    Divider()
                    LinkRow(
                        icon = Icons.Outlined.SupervisorAccount,
                        title = "Open parent dashboard",
                        subtitle = "Preview what they can see",
                        showChevron = true,
                        onClick = { navController.navigate("parent") },
                    )
                }
            }
        }

        SettingsSection(title = "Reports") {
            LinkRow(
                icon = Icons.Outlined.Flag,
                title = "Report status",
                subtitle = "Track reports you've filed",
                showChevron = true,
                onClick = { navController.navigate("reports") },
            )
        }

        if (isAdmin) {
            SettingsSection(title = "Moderation") {
                LinkRow(
                    icon = Icons.Outlined.AdminPanelSettings,
                    title = "Admin dashboard",
                    subtitle = "Search users, ban, broadcast",
                    showChevron = true,
                    onClick = { navController.navigate("admin") },
                )
            }
        }

        SettingsSection(title = "Privacy") {
            LinkRow(Icons.Outlined.Lock, "End-to-end encryption", "On for all 1-to-1 chats")
            Divider()
            LinkRow(Icons.Outlined.Key, "Account & devices", "1 device signed in")
            Divider()
            LinkRow(Icons.Outlined.Shield, "On-device AI scanning", "Most analysis stays on your phone")
        }

        SettingsSection(title = "App") {
            LinkRow(Icons.Outlined.Notifications, "Notifications", "Friends, alerts \u00b7 silent for the rest")
            Divider()
            UpdateRow()
            Divider()
            LinkRow(Icons.AutoMirrored.Outlined.HelpOutline, "Help & support", "")
            Divider()
            LinkRow(
                icon = Icons.AutoMirrored.Outlined.Logout,
                title = "Sign out",
                subtitle = "",
                danger = true,
                onClick = {
                    authViewModel.signOut {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
            )
        }
    }

    val editField = editing
    if (editField != null) {
        val initial = when (editField) {
            AccountField.DisplayName -> AppState.displayName
            AccountField.Username -> AppState.username
            AccountField.Email -> AppState.email
            AccountField.Password -> ""
        }
        AccountEditSheet(
            field = editField,
            initialValue = initial,
            onDismiss = { editing = null },
            onSave = { value, current, done ->
                when (editField) {
                    AccountField.DisplayName -> authViewModel.updateDisplayName(value, done)
                    AccountField.Username -> authViewModel.updateUsername(value, done)
                    AccountField.Email -> authViewModel.updateEmail(value, done)
                    AccountField.Password -> authViewModel.updatePassword(current.orEmpty(), value, done)
                }
            },
        )
    }

    if (showProfilePicker) {
        ProfilePickerSheet(
            onDismiss = { showProfilePicker = false },
            viewModel = authViewModel,
        )
    }

    if (showStoryPicker) {
        MediaPickerSheet(
            title = "Add to your story",
            onDismiss = { showStoryPicker = false },
            onTakePhoto = {
                showStoryPicker = false
                storyCapture.takePhoto()
            },
            onPickPhoto = {
                showStoryPicker = false
                storyCapture.pickPhoto()
            },
        )
    }
}

@Composable
private fun UpdateRow() {
    val context = LocalContext.current
    val repo = remember { AuthRepository.get(context) }
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<AppConfigRow?>(null) }
    var upToDate by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !checking) {
                checking = true
                upToDate = false
                scope.launch {
                    val r = repo.fetchAppConfig()
                    checking = false
                    val cfg = r.getOrNull()
                    val latest = cfg?.latestVersionCode ?: 0
                    if (cfg != null && latest > BuildConfig.VERSION_CODE) {
                        dialog = cfg
                    } else if (r.isFailure) {
                        Toast.makeText(
                            context,
                            r.exceptionOrNull()?.message ?: "Couldn't check for updates.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        upToDate = true
                    }
                }
            }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconCell(Icons.Outlined.SystemUpdate, MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Check for updates",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
            Text(
                when {
                    checking -> "Checking\u2026"
                    upToDate -> "You're on the latest version (v${BuildConfig.VERSION_NAME})"
                    else -> "Current version v${BuildConfig.VERSION_NAME}"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
        if (checking) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }

    val cfg = dialog
    if (cfg != null) {
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Update available") },
            text = {
                Column {
                    Text(
                        "Version ${cfg.latestVersionName ?: cfg.latestVersionCode}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        cfg.releaseNotes?.takeIf { it.isNotBlank() } ?: "A new version of Nexa is available.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val url = cfg.downloadUrl
                    if (!url.isNullOrBlank()) {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }.onFailure {
                            Toast.makeText(context, "Couldn't open download link.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "No download link configured.", Toast.LENGTH_SHORT).show()
                    }
                    dialog = null
                }) { Text("Download & Install") }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) { Text("Later") }
            },
        )
    }
}

@Composable
private fun ProfileCard(
    onTapAvatar: () -> Unit,
    activeStory: StoryRow?,
    posting: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                    )
                )
            )
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.clip(androidx.compose.foundation.shape.CircleShape).clickable { onTapAvatar() }) {
                ProfileAvatar(
                    photoUrl = AppState.mainPhotoUrl,
                    emoji = AppState.avatarEmoji,
                    gradientIndex = AppState.avatarGradientIndex,
                    fallbackInitials = AppState.displayName.take(2).ifBlank { "Y" },
                    size = 56.dp,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        AppState.displayName.ifBlank { "You" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                    if (AppState.vibeEmoji.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(AppState.vibeEmoji, fontSize = 18.sp)
                    }
                }
                Text(
                    "@${AppState.username.ifBlank { "you" }}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.22f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Shield \u00b7 ${AppState.shieldLevel.label}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    if (activeStory != null || posting) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.22f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoStories,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (posting) "Posting\u2026" else "Story live",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (activeStory != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.6f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.25f)),
            ) {
                AsyncImage(
                    model = activeStory.mediaUrl,
                    contentDescription = "Your active story",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title.uppercase(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
        ) {
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconCell(icon, MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun LinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    danger: Boolean = false,
    showChevron: Boolean = false,
    onClick: () -> Unit = {},
) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val titleColor = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconCell(icon, tint)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = titleColor,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun IconCell(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 60.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun ThemePicker(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Column(modifier = Modifier.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCell(Icons.Outlined.Nightlight, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "Theme",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                )
                Text(
                    "Pick how Nexa looks",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ThemeMode.values().forEach { mode ->
                ThemeChip(
                    label = mode.label,
                    icon = when (mode) {
                        ThemeMode.System -> Icons.Outlined.PhoneAndroid
                        ThemeMode.Light -> Icons.Outlined.LightMode
                        ThemeMode.Dark -> Icons.Outlined.DarkMode
                        ThemeMode.Dim -> Icons.Outlined.Nightlight
                    },
                    active = mode == selected,
                    onClick = { onSelect(mode) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ThemeChip(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (active) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ShieldLevelPicker(
    selected: ShieldLevel,
    onSelect: (ShieldLevel) -> Unit,
) {
    Column(modifier = Modifier.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCell(Icons.Outlined.Shield, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Shield level",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                )
                Text(
                    selected.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ShieldLevel.values().forEach { level ->
                val active = level == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .clickable { onSelect(level) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        level.label,
                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}
