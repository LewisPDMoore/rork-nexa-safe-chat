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
)

@Serializable
data class Profile(
    val id: String,
    val username: String,
    val email: String,
    @SerialName("avatar_emoji") val avatarEmoji: String? = null,
    @SerialName("avatar_gradient") val avatarGradient: Int? = null,
)

@Serializable
data class ProfileAvatarPatch(
    @SerialName("avatar_emoji") val avatarEmoji: String? = null,
    @SerialName("avatar_gradient") val avatarGradient: Int? = null,
)

sealed interface SessionStatus {
    data object Loading : SessionStatus
    data object Unauthenticated : SessionStatus
    data class Authenticated(val session: AuthSession, val profile: Profile?) : SessionStatus
}
