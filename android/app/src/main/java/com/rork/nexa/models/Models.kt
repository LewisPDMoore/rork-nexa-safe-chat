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
    val subtitle: String? = null,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val safety: SafetyLevel = SafetyLevel.Safe,
    val avatarColor: Long,
    val initials: String,
    val photoUrl: String? = null,
    val avatarEmoji: String? = null,
    val avatarGradient: Int = 0,
    val isTyping: Boolean = false,
    val isGroup: Boolean = false,
    val groupSize: Int = 0,
    val sparks: Int = 0,
    val targetUserId: String? = null,
    val username: String? = null,
    val displayName: String? = null,
    val nickname: String? = null,
)

data class Message(
    val id: String,
    val text: String,
    val isMe: Boolean,
    val timestamp: String,
    val flagged: Boolean = false,
    val reactions: List<String> = emptyList(),
    val imageUrl: String? = null,
    val imageTimer: Int? = null,
    val viewed: Boolean = false,
)

data class SafetyAlert(
    val id: String,
    val title: String,
    val description: String,
    val level: SafetyLevel,
    val timeAgo: String,
    val source: String,
)

data class AvatarGradient(
    val name: String,
    val start: Color,
    val end: Color,
)

val AvatarGradients = listOf(
    AvatarGradient("Violet", Color(0xFF7C5CFF), Color(0xFFFF6BA8)),
    AvatarGradient("Ocean", Color(0xFF34E5C8), Color(0xFF5C9CFF)),
    AvatarGradient("Sunset", Color(0xFFFFB547), Color(0xFFFF6B6B)),
    AvatarGradient("Mint", Color(0xFF53D593), Color(0xFF34E5C8)),
    AvatarGradient("Coral", Color(0xFFFF8A8A), Color(0xFFFFB547)),
    AvatarGradient("Neon", Color(0xFF7C5CFF), Color(0xFF34E5C8)),
)

val VibeEmojis = listOf("😎", "🔥", "🌙", "💫", "🎧", "🌈", "🍓", "⚡", "🫧")
