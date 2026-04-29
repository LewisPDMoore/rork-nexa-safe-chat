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
    var username by mutableStateOf("")
    var displayName by mutableStateOf("")
    var supervisedByParent by mutableStateOf(false)
    var shieldLevel by mutableStateOf(ShieldLevel.Medium)

    var avatarEmoji by mutableStateOf("")
    var avatarGradientIndex by mutableStateOf(0)
    var vibeEmoji by mutableStateOf("")

    val chats = mutableStateListOf<Chat>()
    private val messagesByChat = mutableStateMapOf<String, SnapshotStateList<Message>>()

    fun messagesFor(chatId: String): SnapshotStateList<Message> =
        messagesByChat.getOrPut(chatId) { mutableStateListOf() }

    fun startChat(
        name: String,
        initials: String,
        avatarColor: Long,
        targetUserId: String? = null,
        username: String? = null,
    ): String {
        val existing = targetUserId?.let { uid -> chats.firstOrNull { it.targetUserId == uid } }
            ?: chats.firstOrNull { it.name.equals(name, ignoreCase = true) }
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
                targetUserId = targetUserId,
                username = username,
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

    fun addReaction(chatId: String, messageId: String, emoji: String) {
        val list = messagesFor(chatId)
        val idx = list.indexOfFirst { it.id == messageId }
        if (idx < 0) return
        val msg = list[idx]
        val newReactions = if (msg.reactions.contains(emoji)) {
            msg.reactions - emoji
        } else {
            msg.reactions + emoji
        }
        list[idx] = msg.copy(reactions = newReactions)
    }

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
        chats.clear()
        messagesByChat.clear()
    }
}

enum class ShieldLevel(val label: String, val description: String) {
    Low("Low", "Only step in for clear harm"),
    Medium("Medium", "Balanced — friendly nudges"),
    High("High", "Strict — flags anything risky"),
}
