package com.rork.nexa.ui.components

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rork.nexa.data.AppState
import com.rork.nexa.models.AvatarGradients
import com.rork.nexa.models.VibeEmojis
import com.rork.nexa.viewmodels.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ProfilePickerSheet(
    onDismiss: () -> Unit,
    viewModel: AuthViewModel,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var showPickMethod by remember { mutableStateOf(false) }

    val handleUri: (Uri) -> Unit = { uri ->
        uploading = true
        error = null
        scope.launch {
            val mime = resolveImageMime(context, uri)
            val bytes = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
                    .getOrNull()
            }
            if (bytes == null || bytes.isEmpty()) {
                uploading = false
                error = "Couldn't read that image."
                return@launch
            }
            viewModel.uploadProfilePhoto(bytes, mime) { err ->
                uploading = false
                error = err
            }
        }
    }

    val capture = rememberMediaCapture(onUri = handleUri)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 6.dp),
        ) {
            Text(
                "Your profile picture",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Photos appear first; your vibe avatar is the fallback.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TabChip(
                    label = "Photos",
                    icon = Icons.Outlined.PhotoCamera,
                    active = tab == 0,
                    modifier = Modifier.weight(1f),
                ) { tab = 0 }
                TabChip(
                    label = "Vibe",
                    icon = Icons.Outlined.Mood,
                    active = tab == 1,
                    modifier = Modifier.weight(1f),
                ) { tab = 1 }
            }
            Spacer(Modifier.height(18.dp))

            if (tab == 0) {
                PhotosTab(
                    uploading = uploading,
                    error = error,
                    onAddPhoto = { showPickMethod = true },
                    onSetMain = { viewModel.setMainPhoto(it) },
                    onDelete = { viewModel.deletePhoto(it) },
                )
            } else {
                VibeTab(
                    onPick = { e, g ->
                        AppState.avatarEmoji = e
                        AppState.avatarGradientIndex = g
                        AppState.vibeEmoji = e
                        viewModel.saveAvatar(e, g) {}
                    },
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showPickMethod) {
        MediaPickerSheet(
            title = "Add a photo",
            onDismiss = { showPickMethod = false },
            onTakePhoto = {
                showPickMethod = false
                capture.takePhoto()
            },
            onPickPhoto = {
                showPickMethod = false
                capture.pickPhoto()
            },
        )
    }
}

internal fun resolveImageMime(context: android.content.Context, uri: Uri): String {
    val fromResolver = context.contentResolver.getType(uri)
    if (!fromResolver.isNullOrBlank() && fromResolver.startsWith("image/")) return fromResolver
    val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        ?.lowercase()
        ?.takeIf { it.isNotBlank() }
    val byExt = ext?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
    if (!byExt.isNullOrBlank() && byExt.startsWith("image/")) return byExt
    return "image/jpeg"
}

@Composable
private fun TabChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun PhotosTab(
    uploading: Boolean,
    error: String?,
    onAddPhoto: () -> Unit,
    onSetMain: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val photos = AppState.photos

    if (error != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                .padding(12.dp),
        ) {
            Text(error, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.height(280.dp),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        RoundedCornerShape(16.dp),
                    )
                    .clickable(enabled = !uploading && photos.size < 6) { onAddPhoto() },
                contentAlignment = Alignment.Center,
            ) {
                if (uploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Add",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
        items(photos.size) { idx ->
            val url = photos[idx]
            PhotoCell(
                url = url,
                isMain = idx == 0,
                onClick = { if (idx != 0) onSetMain(url) },
                onDelete = { onDelete(url) },
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "Tap a photo to make it your main · long-press the X to delete.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
    )
}

@Composable
private fun PhotoCell(
    url: String,
    isMain: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (isMain) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("Main", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable { onDelete() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Delete",
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun VibeTab(onPick: (String, Int) -> Unit) {
    Column {
        Text(
            "EMOJI",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(10.dp))
        VibeEmojis.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { e ->
                    val active = e == AppState.avatarEmoji
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
                            .clickable { onPick(e, AppState.avatarGradientIndex) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(e, fontSize = 26.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(10.dp))
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
                val active = idx == AppState.avatarGradientIndex
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
                        .clickable { onPick(AppState.avatarEmoji, idx) },
                )
            }
        }
    }
}
