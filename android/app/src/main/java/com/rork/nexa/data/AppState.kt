package com.rork.nexa.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.rork.nexa.models.Chat
import com.rork.nexa.models.Message
import com.rork.nexa.models.SafetyLevel
import com.rork.nexa.ui.theme.ThemeMode

object AppState {
    var themeMode by mutableStateOf(ThemeMode.System)
    var hasOnboarded by mutableStateOf(false)
    var username by mutableStateOf("")
    var displayName by mutableStateOf("")
    var supervisedByParent by mutableStateOf(false)
    var shieldLevel by mutableStateOf(ShieldLevel.Medium)

    val chats = mutableStateListOf<Chat>()
    private val messagesByChat = mutableStateMapOf<String, SnapshotStateList<Message>>()

    fun messagesFor(chatId: String): SnapshotStateList<Message> =
        messagesByChat.getOrPut(chatId) { mutableStateListOf() }

    fun startChat(
        name: String,
        initials: String,
        avatarColor: Long,
    ): String {
        val existing = chats.firstOrNull { it.name == name }
        if (existing != null) return existing.id
        val id = "c${System.currentTimeMillis()}"
        chats.add(
            0,
            Chat(
                id = id,
                name = name,
                lastMessage = "",
                timestamp = "now",
                unreadCount = 0,
                safety = SafetyLevel.Safe,
                avatarColor = avatarColor,
                initials = initials,
            )
        )
        return id
    }

    fun sendMessage(chatId: String, text: String) {
        val list = messagesFor(chatId)
        list.add(
            Message(
                id = "m${System.currentTimeMillis()}",
                text = text,
                isMe = true,
                timestamp = "now",
            )
        )
        val idx = chats.indexOfFirst { it.id == chatId }
        if (idx >= 0) {
            val updated = chats[idx].copy(lastMessage = text, timestamp = "now")
            chats.removeAt(idx)
            chats.add(0, updated)
        }
    }
}

enum class ShieldLevel(val label: String, val description: String) {
    Low("Low", "Only step in for clear harm"),
    Medium("Medium", "Balanced — friendly nudges"),
    High("High", "Strict — flags anything risky"),
}
