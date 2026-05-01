package com.rork.nexa.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rork.nexa.data.AppState
import com.rork.nexa.data.auth.AuthRepository
import com.rork.nexa.data.auth.SessionStatus
import com.rork.nexa.data.chat.ChatRepository
import com.rork.nexa.data.chat.ConversationRow
import com.rork.nexa.models.Chat
import com.rork.nexa.models.SafetyLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

class ChatsViewModel(app: Application) : AndroidViewModel(app) {
    private val auth = AuthRepository.get(app)
    private val chatRepo = ChatRepository.get(app)

    val chats: StateFlow<List<Chat>> = combine(
        chatRepo.conversations,
        chatRepo.profiles,
        auth.status,
    ) { convs, profiles, status ->
        val myId = (status as? SessionStatus.Authenticated)?.session?.user?.id ?: return@combine emptyList()
        convs.map { c -> toChat(c, profiles, myId) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _starting = MutableStateFlow(false)
    val starting: StateFlow<Boolean> = _starting.asStateFlow()

    fun startConversation(otherUserId: String, onReady: (String) -> Unit) {
        if (_starting.value) return
        _starting.value = true
        viewModelScope.launch {
            val r = chatRepo.openOrCreateConversation(otherUserId)
            _starting.value = false
            r.getOrNull()?.let(onReady)
        }
    }

    fun setNickname(friendId: String, nickname: String?) {
        viewModelScope.launch {
            val r = auth.setNickname(friendId, nickname)
            if (r.isSuccess) {
                if (nickname.isNullOrBlank()) AppState.nicknames.remove(friendId)
                else AppState.nicknames[friendId] = nickname.trim()
            }
        }
    }

    private fun toChat(
        c: ConversationRow,
        profiles: Map<String, com.rork.nexa.data.auth.Profile>,
        myId: String,
    ): Chat {
        val otherId = c.other(myId)
        val p = profiles[otherId]
        val username = p?.username ?: otherId.take(6)
        val displayName = p?.displayName?.takeIf { it.isNotBlank() }
            ?: username.replaceFirstChar { it.uppercase() }
        val nickname = AppState.nicknames[otherId]
        val title = nickname?.takeIf { it.isNotBlank() } ?: displayName
        val subtitle = if (nickname != null) "$displayName · @$username" else "@$username"
        val initials = title.take(2).uppercase()
        val palette = listOf(
            0xFF7C5CFFL, 0xFFFF6BA8L, 0xFF34E5C8L,
            0xFFFFB547L, 0xFF53D593L, 0xFFFF8A8AL,
        )
        val color = palette[((username.hashCode() % palette.size) + palette.size) % palette.size]
        return Chat(
            id = c.id,
            name = title,
            subtitle = subtitle,
            lastMessage = c.lastMessage,
            timestamp = formatTimestamp(c.lastMessageAt),
            unreadCount = 0,
            safety = SafetyLevel.Safe,
            avatarColor = color,
            initials = initials,
            photoUrl = p?.photos?.firstOrNull(),
            avatarEmoji = p?.avatarEmoji,
            avatarGradient = p?.avatarGradient ?: 0,
            targetUserId = otherId,
            username = username,
            displayName = displayName,
            nickname = nickname,
        )
    }

    private fun formatTimestamp(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        val t = runCatching { Instant.parse(iso) }.getOrNull() ?: return ""
        val now = Instant.now()
        val d = Duration.between(t, now)
        return when {
            d.toMinutes() < 1 -> "now"
            d.toMinutes() < 60 -> "${d.toMinutes()}m"
            d.toHours() < 24 -> "${d.toHours()}h"
            d.toDays() < 7 -> "${d.toDays()}d"
            else -> "${d.toDays() / 7}w"
        }
    }
}
