package com.rork.nexa.models

import androidx.compose.ui.graphics.Color

enum class SafetyLevel(val label: String, val color: Color) {
    Safe("Safe", Color(0xFF53D593)),
    Watch("Heads up", Color(0xFFFFB547)),
    Alert("Needs care", Color(0xFFFF6B6B));
}

data class Chat(
    val id: String,
    val name: String,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val safety: SafetyLevel = SafetyLevel.Safe,
    val avatarColor: Long,
    val initials: String,
    val isTyping: Boolean = false,
    val isGroup: Boolean = false,
    val groupSize: Int = 0,
)

data class Message(
    val id: String,
    val text: String,
    val isMe: Boolean,
    val timestamp: String,
    val flagged: Boolean = false,
    val reactions: List<String> = emptyList(),
)

data class SafetyAlert(
    val id: String,
    val title: String,
    val description: String,
    val level: SafetyLevel,
    val timeAgo: String,
    val source: String,
)

data class SuggestedFriend(
    val name: String,
    val initials: String,
    val color: Long,
)
