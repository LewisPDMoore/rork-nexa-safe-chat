package com.rork.nexa.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rork.nexa.ui.theme.ThemeMode

object AppState {
    var themeMode by mutableStateOf(ThemeMode.System)
    var username by mutableStateOf("")
    var displayName by mutableStateOf("")
    var email by mutableStateOf("")
    var supervisedByParent by mutableStateOf(false)
    var shieldLevel by mutableStateOf(ShieldLevel.Medium)

    var avatarEmoji by mutableStateOf("")
    var avatarGradientIndex by mutableStateOf(0)
    var vibeEmoji by mutableStateOf("")
    val photos = mutableStateListOf<String>()
    val nicknames = mutableStateMapOf<String, String>()

    val mainPhotoUrl: String? get() = photos.firstOrNull()

    fun applyProfile(
        username: String,
        displayName: String?,
        email: String?,
        photos: List<String>,
        avatarEmoji: String?,
        avatarGradientIndex: Int?,
    ) {
        this.username = username
        this.displayName = displayName.orEmpty().ifBlank { username.replaceFirstChar { it.uppercase() } }
        this.email = email.orEmpty()
        this.avatarEmoji = avatarEmoji.orEmpty()
        this.vibeEmoji = avatarEmoji.orEmpty()
        this.avatarGradientIndex = avatarGradientIndex ?: 0
        this.photos.clear()
        this.photos.addAll(photos)
    }

    fun clearUserData() {
        username = ""
        displayName = ""
        email = ""
        avatarEmoji = ""
        avatarGradientIndex = 0
        vibeEmoji = ""
        photos.clear()
        nicknames.clear()
        supervisedByParent = false
    }
}

enum class ShieldLevel(val label: String, val description: String) {
    Low("Low", "Only step in for clear harm"),
    Medium("Medium", "Balanced — friendly nudges"),
    High("High", "Strict — flags anything risky"),
}
