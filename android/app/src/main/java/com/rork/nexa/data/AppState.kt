package com.rork.nexa.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rork.nexa.ui.theme.ThemeMode

object AppState {
    var themeMode by mutableStateOf(ThemeMode.System)
    var username by mutableStateOf("")
    var displayName by mutableStateOf("")
    var supervisedByParent by mutableStateOf(false)
    var shieldLevel by mutableStateOf(ShieldLevel.Medium)

    var avatarEmoji by mutableStateOf("")
    var avatarGradientIndex by mutableStateOf(0)
    var vibeEmoji by mutableStateOf("")

    fun applyProfile(
        username: String,
        avatarEmoji: String?,
        avatarGradientIndex: Int?,
    ) {
        this.username = username
        this.displayName = username.replaceFirstChar { it.uppercase() }
        this.avatarEmoji = avatarEmoji.orEmpty()
        this.vibeEmoji = avatarEmoji.orEmpty()
        this.avatarGradientIndex = avatarGradientIndex ?: 0
    }

    fun clearUserData() {
        username = ""
        displayName = ""
        avatarEmoji = ""
        avatarGradientIndex = 0
        vibeEmoji = ""
        supervisedByParent = false
    }
}

enum class ShieldLevel(val label: String, val description: String) {
    Low("Low", "Only step in for clear harm"),
    Medium("Medium", "Balanced — friendly nudges"),
    High("High", "Strict — flags anything risky"),
}
