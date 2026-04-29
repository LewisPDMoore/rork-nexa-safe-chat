package com.rork.nexa.data.auth

import android.content.Context
import com.rork.nexa.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class AuthRepository private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("nexa_auth", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val client = HttpClient(Android) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        HttpResponseValidator { validateResponse { /* handled per call */ } }
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _status = MutableStateFlow<SessionStatus>(SessionStatus.Loading)
    val status: StateFlow<SessionStatus> = _status.asStateFlow()

    val supabaseUrl: String = BuildConfig.SUPABASE_URL.trimEnd('/')
    val anonKey: String = BuildConfig.SUPABASE_ANON_KEY
    val isConfigured: Boolean get() = supabaseUrl.isNotBlank() && anonKey.isNotBlank()

    init {
        scope.launch { restore() }
    }

    private suspend fun restore() {
        if (!isConfigured) {
            _status.value = SessionStatus.Unauthenticated
            return
        }
        val refresh = prefs.getString(KEY_REFRESH, null)
        if (refresh.isNullOrBlank()) {
            _status.value = SessionStatus.Unauthenticated
            return
        }
        val result = runCatching { refreshSession(refresh) }
        result.onSuccess { session ->
            persist(session)
            val profile = runCatching { fetchProfile(session) }.getOrNull()
            _status.value = SessionStatus.Authenticated(session, profile)
        }.onFailure {
            clearPersisted()
            _status.value = SessionStatus.Unauthenticated
        }
    }

    suspend fun signUp(
        email: String,
        username: String,
        password: String,
    ): Result<Unit> = runCatching {
        require(isConfigured) { "Backend is not configured." }
        val resp: HttpResponse = client.post("$supabaseUrl/auth/v1/signup") {
            anonHeaders()
            contentType(ContentType.Application.Json)
            setBody(SignUpRequest(email = email.trim(), password = password, data = mapOf("username" to username.trim())))
        }
        if (!resp.status.isSuccess()) throw mapError(resp)
        val session: AuthSession = json.decodeFromString(AuthSession.serializer(), resp.bodyAsText())

        val insertResp = client.post("$supabaseUrl/rest/v1/profiles") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            header("Prefer", "return=representation")
            contentType(ContentType.Application.Json)
            setBody(
                ProfileInsert(
                    id = session.user?.id ?: error("No user id from sign up"),
                    username = username.trim().lowercase(),
                    email = email.trim(),
                )
            )
        }
        if (!insertResp.status.isSuccess()) {
            val text = insertResp.bodyAsText()
            if (text.contains("profiles_username_key", ignoreCase = true) ||
                text.contains("duplicate key", ignoreCase = true) && text.contains("username", ignoreCase = true)
            ) {
                throw IllegalStateException("That username is already taken.")
            }
            throw IllegalStateException("Couldn't save your profile. Try again.")
        }

        persist(session)
        val profile = runCatching { fetchProfile(session) }.getOrNull()
        _status.value = SessionStatus.Authenticated(session, profile)
    }

    suspend fun signIn(
        email: String,
        password: String,
        rememberMe: Boolean,
    ): Result<Unit> = runCatching {
        require(isConfigured) { "Backend is not configured." }
        val resp = client.post("$supabaseUrl/auth/v1/token?grant_type=password") {
            anonHeaders()
            contentType(ContentType.Application.Json)
            setBody(PasswordGrantRequest(email = email.trim(), password = password))
        }
        if (!resp.status.isSuccess()) throw mapError(resp)
        val session: AuthSession = json.decodeFromString(AuthSession.serializer(), resp.bodyAsText())

        prefs.edit().putBoolean(KEY_REMEMBER, rememberMe).apply()
        persist(session)
        val profile = runCatching { fetchProfile(session) }.getOrNull()
        _status.value = SessionStatus.Authenticated(session, profile)
    }

    suspend fun signOut() {
        val current = (_status.value as? SessionStatus.Authenticated)?.session
        if (current != null) {
            runCatching {
                client.post("$supabaseUrl/auth/v1/logout") {
                    anonHeaders()
                    header(HttpHeaders.Authorization, "Bearer ${current.accessToken}")
                }
            }
        }
        clearPersisted()
        _status.value = SessionStatus.Unauthenticated
    }

    suspend fun signOutIfNotRemembered() {
        if (!prefs.getBoolean(KEY_REMEMBER, true)) {
            signOut()
        }
    }

    suspend fun saveAvatar(emoji: String, gradientIndex: Int): Result<Unit> = runCatching {
        val auth = _status.value as? SessionStatus.Authenticated
            ?: throw IllegalStateException("Not signed in")
        val resp = client.post("$supabaseUrl/rest/v1/profiles?id=eq.${auth.session.user?.id}") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
            header("Prefer", "return=representation")
            header("X-HTTP-Method-Override", "PATCH")
            contentType(ContentType.Application.Json)
            setBody(ProfileAvatarPatch(avatarEmoji = emoji, avatarGradient = gradientIndex))
        }
        if (!resp.status.isSuccess()) throw mapError(resp)
        val updated = auth.profile?.copy(avatarEmoji = emoji, avatarGradient = gradientIndex)
        _status.value = auth.copy(profile = updated)
    }

    private suspend fun refreshSession(refreshToken: String): AuthSession {
        val resp = client.post("$supabaseUrl/auth/v1/token?grant_type=refresh_token") {
            anonHeaders()
            contentType(ContentType.Application.Json)
            setBody(RefreshGrantRequest(refreshToken))
        }
        if (!resp.status.isSuccess()) throw mapError(resp)
        return json.decodeFromString(AuthSession.serializer(), resp.bodyAsText())
    }

    private suspend fun fetchProfile(session: AuthSession): Profile? {
        val uid = session.user?.id ?: return null
        val resp = client.get("$supabaseUrl/rest/v1/profiles?id=eq.$uid&select=*") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
        }
        if (!resp.status.isSuccess()) return null
        val list: List<Profile> = json.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(Profile.serializer()),
            resp.bodyAsText()
        )
        return list.firstOrNull()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.anonHeaders() {
        headers {
            append("apikey", anonKey)
        }
    }

    private fun persist(session: AuthSession) {
        prefs.edit()
            .putString(KEY_ACCESS, session.accessToken)
            .putString(KEY_REFRESH, session.refreshToken)
            .apply()
    }

    private fun clearPersisted() {
        prefs.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .apply()
    }

    private suspend fun mapError(resp: HttpResponse): Throwable {
        val text = runCatching { resp.bodyAsText() }.getOrDefault("")
        val parsed = runCatching { json.decodeFromString(SupabaseError.serializer(), text) }.getOrNull()
        val raw = parsed?.friendly() ?: text.ifBlank { "Request failed (${resp.status.value})" }
        val friendly = when {
            raw.contains("already registered", ignoreCase = true) -> "That email is already in use."
            raw.contains("invalid login", ignoreCase = true) -> "Email or password isn't right."
            raw.contains("invalid credentials", ignoreCase = true) -> "Email or password isn't right."
            raw.contains("password", ignoreCase = true) && raw.contains("short", ignoreCase = true) ->
                "Password is too short."
            raw.contains("rate limit", ignoreCase = true) -> "Too many tries — wait a minute and try again."
            resp.status == HttpStatusCode.Unauthorized -> "Session expired. Please log in again."
            else -> raw
        }
        return IllegalStateException(friendly)
    }

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_REMEMBER = "remember_me"

        @Volatile private var instance: AuthRepository? = null
        fun get(context: Context): AuthRepository =
            instance ?: synchronized(this) {
                instance ?: AuthRepository(context.applicationContext).also { instance = it }
            }
    }
}
