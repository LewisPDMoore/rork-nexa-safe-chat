package com.rork.nexa.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rork.nexa.data.auth.AuthRepository
import com.rork.nexa.data.auth.SessionStatus
import com.rork.nexa.data.chat.ChatRepository
import com.rork.nexa.data.chat.MessageRow
import com.rork.nexa.models.Message
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

class ChatDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val auth = AuthRepository.get(app)
    private val chatRepo = ChatRepository.get(app)

    private val _conversationId = MutableStateFlow<String?>(null)
    private val _reactions = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    private val myId: String?
        get() = (auth.status.value as? SessionStatus.Authenticated)?.session?.user?.id

    val messages: StateFlow<List<Message>> = combine(
        _conversationId,
        _reactions,
    ) { id, reactions -> id to reactions }
        .let { idFlow ->
            kotlinx.coroutines.flow.flow {
                idFlow.collect { (id, reactions) ->
                    if (id == null) {
                        emit(emptyList<Message>())
                        return@collect
                    }
                    chatRepo.messagesFor(id).collect { rows ->
                        val me = myId
                        emit(rows.map { it.toUi(me, reactions) })
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isRemoteTyping: StateFlow<Boolean> = kotlinx.coroutines.flow.flow {
        _conversationId.collect { id ->
            if (id == null) emit(false)
            else chatRepo.typingFor(id).collect { emit(it) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _peerName = MutableStateFlow<String>("")
    val peerName: StateFlow<String> = _peerName.asStateFlow()

    private val _peerInitials = MutableStateFlow<String>("")
    val peerInitials: StateFlow<String> = _peerInitials.asStateFlow()

    private val _peerColor = MutableStateFlow<Long>(0xFF7C5CFFL)
    val peerColor: StateFlow<Long> = _peerColor.asStateFlow()

    private val _peerUserId = MutableStateFlow<String?>(null)
    val peerUserId: StateFlow<String?> = _peerUserId.asStateFlow()

    private var typingJob: Job? = null
    private var lastSentTyping: Boolean = false

    fun bind(conversationId: String) {
        if (_conversationId.value == conversationId) return
        val previous = _conversationId.value
        _conversationId.value = conversationId
        previous?.let { chatRepo.stopObservingConversation(it) }
        chatRepo.observeConversation(conversationId)
        viewModelScope.launch { chatRepo.loadHistory(conversationId) }
        viewModelScope.launch { resolvePeer(conversationId) }
    }

    private suspend fun resolvePeer(conversationId: String) {
        val me = myId ?: return
        val conv = chatRepo.conversations.value.firstOrNull { it.id == conversationId } ?: return
        val otherId = conv.other(me)
        _peerUserId.value = otherId
        chatRepo.ensureProfilesFor(listOf(otherId))
        val p = chatRepo.profiles.value[otherId]
        val name = p?.username ?: otherId.take(6)
        _peerName.value = name
        _peerInitials.value = name.take(2).uppercase()
        val palette = listOf(
            0xFF7C5CFFL, 0xFFFF6BA8L, 0xFF34E5C8L,
            0xFFFFB547L, 0xFF53D593L, 0xFFFF8A8AL,
        )
        _peerColor.value = palette[((name.hashCode() % palette.size) + palette.size) % palette.size]
    }

    fun send(text: String) {
        val convId = _conversationId.value ?: return
        val recipient = _peerUserId.value ?: return
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            chatRepo.sendMessage(convId, recipient, trimmed)
            // ensure typing cleared
            sendTypingNow(false)
        }
    }

    fun onInputChange(text: String) {
        val convId = _conversationId.value ?: return
        if (text.isNotBlank() && !lastSentTyping) {
            sendTypingNow(true)
        }
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(1500L)
            if (lastSentTyping) sendTypingNow(false)
        }
        if (text.isBlank() && lastSentTyping) {
            sendTypingNow(false)
        }
        // suppress unused warning
        @Suppress("UNUSED_EXPRESSION") convId
    }

    private fun sendTypingNow(isTyping: Boolean) {
        val convId = _conversationId.value ?: return
        lastSentTyping = isTyping
        chatRepo.sendTyping(convId, isTyping)
    }

    fun toggleReaction(messageId: String, emoji: String) {
        _reactions.value = _reactions.value.toMutableMap().also { map ->
            val cur = map[messageId].orEmpty()
            map[messageId] = if (emoji in cur) cur - emoji else cur + emoji
        }
    }

    override fun onCleared() {
        super.onCleared()
        _conversationId.value?.let { chatRepo.stopObservingConversation(it) }
        if (lastSentTyping) sendTypingNow(false)
    }

    private fun MessageRow.toUi(myId: String?, reactions: Map<String, Set<String>>): Message =
        Message(
            id = id,
            text = text,
            isMe = senderId == myId,
            timestamp = formatTimestamp(createdAt),
            reactions = reactions[id]?.toList() ?: emptyList(),
        )

    private fun formatTimestamp(iso: String?): String {
        if (iso.isNullOrBlank()) return "now"
        val t = runCatching { Instant.parse(iso) }.getOrNull() ?: return "now"
        val d = Duration.between(t, Instant.now())
        return when {
            d.toMinutes() < 1 -> "now"
            d.toMinutes() < 60 -> "${d.toMinutes()}m"
            d.toHours() < 24 -> "${d.toHours()}h"
            else -> "${d.toDays()}d"
        }
    }
}
