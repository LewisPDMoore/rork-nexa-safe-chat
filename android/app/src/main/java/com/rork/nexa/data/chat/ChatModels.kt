package com.rork.nexa.data.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationRow(
    val id: String,
    @SerialName("user_a") val userA: String,
    @SerialName("user_b") val userB: String,
    @SerialName("last_message") val lastMessage: String = "",
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun other(myId: String): String = if (userA == myId) userB else userA
}

@Serializable
data class MessageRow(
    val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("recipient_id") val recipientId: String,
    val text: String = "",
    @SerialName("image_path") val imagePath: String? = null,
    @SerialName("image_timer") val imageTimer: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class MessageInsertBody(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("recipient_id") val recipientId: String,
    val text: String,
    @SerialName("image_path") val imagePath: String? = null,
    @SerialName("image_timer") val imageTimer: Int? = null,
)

@Serializable
data class ConversationInsertBody(
    @SerialName("user_a") val userA: String,
    @SerialName("user_b") val userB: String,
)
