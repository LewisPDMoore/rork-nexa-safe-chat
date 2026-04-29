package com.rork.nexa.data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SupabaseUser(
    val id: String,
    val email: String? = null,
    @SerialName("user_metadata") val userMetadata: JsonElement? = null,
)

@Serializable
data class AuthSession(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long = 3600,
    @SerialName("token_type") val tokenType: String = "bearer",
    val user: SupabaseUser? = null,
)

@Serializable
data class SignUpRequest(
    val email: String,
    val password: String,
    val data: Map<String, String> = emptyMap(),
)

@Serializable
data class PasswordGrantRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshGrantRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class SupabaseError(
    val code: String? = null,
    val message: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
    val error: String? = null,
    val msg: String? = null,
) {
    fun friendly(): String =
        message ?: errorDescription ?: msg ?: error ?: "Something went wrong"
}

@Serializable
data class ProfileInsert(
    val id: String,
    val username: String,
    val email: String,
    @SerialName("parent_id") val parentId: String? = null,
)

@Serializable
data class Profile(
    val id: String,
    val username: String,
    val email: String,
    @SerialName("avatar_emoji") val avatarEmoji: String? = null,
    @SerialName("avatar_gradient") val avatarGradient: Int? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("banned_until") val bannedUntil: String? = null,
    @SerialName("ban_reason") val banReason: String? = null,
)

@Serializable
data class ProfileLookup(
    val id: String? = null,
    val email: String,
    val username: String? = null,
    @SerialName("banned_until") val bannedUntil: String? = null,
    @SerialName("ban_reason") val banReason: String? = null,
)

@Serializable
data class ProfileAvatarPatch(
    @SerialName("avatar_emoji") val avatarEmoji: String? = null,
    @SerialName("avatar_gradient") val avatarGradient: Int? = null,
)

@Serializable
data class BanPatch(
    @SerialName("banned_until") val bannedUntil: String?,
    @SerialName("ban_reason") val banReason: String?,
)

@Serializable
data class ReportInsert(
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("target_user_id") val targetUserId: String? = null,
    @SerialName("target_message_id") val targetMessageId: String? = null,
    val reason: String,
    val kind: String,
    val status: String = "pending",
    @SerialName("message_context_json") val messageContextJson: String? = null,
)

@Serializable
data class ReportRow(
    val id: String,
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("target_user_id") val targetUserId: String? = null,
    val reason: String,
    val kind: String,
    val status: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class AppConfigRow(
    @SerialName("latest_version_code") val latestVersionCode: Int? = null,
    @SerialName("latest_version_name") val latestVersionName: String? = null,
    @SerialName("release_notes") val releaseNotes: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
    val mandatory: Boolean = false,
)

sealed interface SessionStatus {
    data object Loading : SessionStatus
    data object Unauthenticated : SessionStatus
    data class Banned(val until: String?, val reason: String?) : SessionStatus
    data class Authenticated(val session: AuthSession, val profile: Profile?) : SessionStatus
}
