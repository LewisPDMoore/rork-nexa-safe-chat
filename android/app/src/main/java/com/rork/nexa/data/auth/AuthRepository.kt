package com.rork.nexa.data.auth

import android.content.Context
import com.rork.nexa.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

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
        HttpResponseValidator { validateResponse { /* per call */ } }
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
            if (profile != null && isCurrentlyBanned(profile.bannedUntil)) {
                clearPersisted()
                _status.value = SessionStatus.Banned(profile.bannedUntil, profile.banReason)
                return
            }
            _status.value = SessionStatus.Authenticated(session, profile)
        }.onFailure {
            clearPersisted()
            _status.value = SessionStatus.Unauthenticated
        }
    }

    suspend fun signUp(
        email: String,
        username: String,
        displayName: String,
        password: String,
        parentId: String? = null,
    ): Result<Unit> = runCatching {
        require(isConfigured) { "Backend is not configured." }
        val resp: HttpResponse = client.post("$supabaseUrl/auth/v1/signup") {
            anonHeaders()
            contentType(ContentType.Application.Json)
            setBody(SignUpRequest(
                email = email.trim(),
                password = password,
                data = mapOf(
                    "username" to username.trim(),
                    "display_name" to displayName.trim(),
                ),
            ))
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
                    displayName = displayName.trim(),
                    parentId = parentId,
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

        if (parentId == null) {
            persist(session)
            val profile = runCatching { fetchProfile(session) }.getOrNull()
            _status.value = SessionStatus.Authenticated(session, profile)
        }
    }

    suspend fun signIn(
        identifier: String,
        password: String,
        rememberMe: Boolean,
    ): Result<Unit> = runCatching {
        require(isConfigured) { "Backend is not configured." }

        val raw = identifier.trim()
        val email = if (raw.contains("@")) raw else resolveUsernameToEmail(raw)
            ?: throw IllegalStateException("Username not found.")

        val resp = client.post("$supabaseUrl/auth/v1/token?grant_type=password") {
            anonHeaders()
            contentType(ContentType.Application.Json)
            setBody(PasswordGrantRequest(email = email, password = password))
        }
        if (!resp.status.isSuccess()) throw mapError(resp)
        val session: AuthSession = json.decodeFromString(AuthSession.serializer(), resp.bodyAsText())

        val profile = runCatching { fetchProfile(session) }.getOrNull()
        if (profile != null && isCurrentlyBanned(profile.bannedUntil)) {
            runCatching {
                client.post("$supabaseUrl/auth/v1/logout") {
                    anonHeaders()
                    header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
                }
            }
            clearPersisted()
            _status.value = SessionStatus.Banned(profile.bannedUntil, profile.banReason)
            return@runCatching
        }

        prefs.edit().putBoolean(KEY_REMEMBER, rememberMe).apply()
        persist(session)
        _status.value = SessionStatus.Authenticated(session, profile)
    }

    private suspend fun resolveUsernameToEmail(username: String): String? {
        val u = username.lowercase()
        val resp = client.get("$supabaseUrl/rest/v1/profiles?username=eq.$u&select=email") {
            anonHeaders()
        }
        if (!resp.status.isSuccess()) return null
        val list: List<ProfileLookup> = runCatching {
            json.decodeFromString(ListSerializer(ProfileLookup.serializer()), resp.bodyAsText())
        }.getOrDefault(emptyList())
        return list.firstOrNull()?.email
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

    fun acknowledgeBan() {
        _status.value = SessionStatus.Unauthenticated
    }

    suspend fun signOutIfNotRemembered() {
        if (!prefs.getBoolean(KEY_REMEMBER, true)) {
            signOut()
        }
    }

    suspend fun saveAvatar(emoji: String, gradientIndex: Int): Result<Unit> = runCatching {
        patchProfile(ProfilePatch(avatarEmoji = emoji, avatarGradient = gradientIndex)) { p ->
            p.copy(avatarEmoji = emoji, avatarGradient = gradientIndex)
        }
    }

    suspend fun updateDisplayName(name: String): Result<Unit> = runCatching {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "Display name can't be empty." }
        patchProfile(ProfilePatch(displayName = trimmed)) { it.copy(displayName = trimmed) }
    }

    suspend fun updateUsername(newUsername: String): Result<Unit> = runCatching {
        val u = newUsername.trim().lowercase().filter { it.isLetterOrDigit() || it == '_' }
        require(u.length >= 3) { "Username must be at least 3 characters." }
        val auth = requireAuth()
        // check availability
        val checkResp = client.get("$supabaseUrl/rest/v1/profiles?username=eq.$u&select=id") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
        }
        if (checkResp.status.isSuccess()) {
            val list = runCatching {
                json.decodeFromString(ListSerializer(ProfileLookup.serializer()), checkResp.bodyAsText())
            }.getOrDefault(emptyList())
            if (list.any { it.id != auth.session.user?.id }) {
                throw IllegalStateException("That username is already taken.")
            }
        }
        patchProfile(ProfilePatch(username = u)) { it.copy(username = u) }
    }

    suspend fun updateEmail(newEmail: String): Result<Unit> = runCatching {
        val e = newEmail.trim()
        require(e.contains("@")) { "That email doesn't look right." }
        val auth = requireAuth()
        val resp = client.put("$supabaseUrl/auth/v1/user") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(AuthUserUpdate(email = e))
        }
        if (!resp.status.isSuccess()) throw mapError(resp)
        patchProfile(ProfilePatch(email = e)) { it.copy(email = e) }
    }

    suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> = runCatching {
        require(newPassword.length >= 6) { "Password must be at least 6 characters." }
        val auth = requireAuth()
        val email = auth.profile?.email ?: throw IllegalStateException("Couldn't find your email.")
        // verify current password
        val verify = client.post("$supabaseUrl/auth/v1/token?grant_type=password") {
            anonHeaders()
            contentType(ContentType.Application.Json)
            setBody(PasswordGrantRequest(email = email, password = currentPassword))
        }
        if (!verify.status.isSuccess()) throw IllegalStateException("Current password isn't right.")
        val resp = client.put("$supabaseUrl/auth/v1/user") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(AuthUserUpdate(password = newPassword))
        }
        if (!resp.status.isSuccess()) throw mapError(resp)
    }

    suspend fun uploadProfilePhoto(bytes: ByteArray, mimeType: String = "image/jpeg"): Result<String> = runCatching {
        val auth = requireAuth()
        val uid = auth.session.user?.id ?: error("No user id")
        val ext = if (mimeType.contains("png", ignoreCase = true)) "png" else "jpg"
        val path = "$uid/${UUID.randomUUID()}.$ext"
        val resp = client.post("$supabaseUrl/storage/v1/object/profile-pics/$path") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
            header("x-upsert", "true")
            header(HttpHeaders.ContentType, mimeType)
            setBody(bytes)
        }
        if (!resp.status.isSuccess()) {
            val text = runCatching { resp.bodyAsText() }.getOrDefault("")
            throw IllegalStateException("Couldn't upload photo (${resp.status.value}). $text".trim())
        }
        val url = "$supabaseUrl/storage/v1/object/public/profile-pics/$path"
        val current = (auth.profile?.photos ?: emptyList()).toMutableList()
        current.add(0, url)
        val capped = current.take(6)
        patchProfile(ProfilePatch(photos = capped)) { it.copy(photos = capped) }
        url
    }

    suspend fun updatePhotos(photos: List<String>): Result<Unit> = runCatching {
        patchProfile(ProfilePatch(photos = photos)) { it.copy(photos = photos) }
    }

    suspend fun setMainPhoto(url: String): Result<Unit> = runCatching {
        val auth = requireAuth()
        val current = auth.profile?.photos ?: emptyList()
        val reordered = listOf(url) + current.filter { it != url }
        patchProfile(ProfilePatch(photos = reordered)) { it.copy(photos = reordered) }
    }

    suspend fun deletePhoto(url: String): Result<Unit> = runCatching {
        val auth = requireAuth()
        val current = auth.profile?.photos ?: emptyList()
        val reduced = current.filter { it != url }
        patchProfile(ProfilePatch(photos = reduced)) { it.copy(photos = reduced) }
        // best-effort delete from storage
        val prefix = "$supabaseUrl/storage/v1/object/public/profile-pics/"
        if (url.startsWith(prefix)) {
            val path = url.removePrefix(prefix)
            runCatching {
                client.delete("$supabaseUrl/storage/v1/object/profile-pics/$path") {
                    anonHeaders()
                    header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
                }
            }
        }
    }

    suspend fun uploadChatMedia(
        conversationId: String,
        bytes: ByteArray,
        mimeType: String = "image/jpeg",
    ): Result<Pair<String, String>> = runCatching {
        val auth = requireAuth()
        val ext = if (mimeType.contains("png", ignoreCase = true)) "png" else "jpg"
        val path = "$conversationId/${UUID.randomUUID()}.$ext"
        val resp = client.post("$supabaseUrl/storage/v1/object/chat-media/$path") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
            header("x-upsert", "true")
            header(HttpHeaders.ContentType, mimeType)
            setBody(bytes)
        }
        if (!resp.status.isSuccess()) {
            val text = runCatching { resp.bodyAsText() }.getOrDefault("")
            throw IllegalStateException("Couldn't upload image (${resp.status.value}). $text".trim())
        }
        val url = "$supabaseUrl/storage/v1/object/public/chat-media/$path"
        path to url
    }

    suspend fun uploadStoryMedia(
        bytes: ByteArray,
        mimeType: String = "image/jpeg",
    ): Result<String> = runCatching {
        val auth = requireAuth()
        val uid = auth.session.user?.id ?: error("No user id")
        val ext = if (mimeType.contains("png", ignoreCase = true)) "png" else "jpg"
        val path = "$uid/${UUID.randomUUID()}.$ext"
        val resp = client.post("$supabaseUrl/storage/v1/object/stories/$path") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
            header("x-upsert", "true")
            header(HttpHeaders.ContentType, mimeType)
            setBody(bytes)
        }
        if (!resp.status.isSuccess()) {
            val text = runCatching { resp.bodyAsText() }.getOrDefault("")
            throw IllegalStateException("Couldn't upload story (${resp.status.value}). $text".trim())
        }
        "$supabaseUrl/storage/v1/object/public/stories/$path"
    }

    suspend fun createStory(
        bytes: ByteArray,
        mimeType: String = "image/jpeg",
    ): Result<StoryRow> = runCatching {
        val auth = requireAuth()
        val uid = auth.session.user?.id ?: error("No user id")
        val mediaUrl = uploadStoryMedia(bytes, mimeType).getOrThrow()
        val expiresAt = Instant.now().plusSeconds(24 * 3600).toString()
        val resp = client.post("$supabaseUrl/rest/v1/stories") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
            header("Prefer", "return=representation")
            contentType(ContentType.Application.Json)
            setBody(
                StoryInsert(
                    userId = uid,
                    mediaUrl = mediaUrl,
                    mediaType = "image",
                    expiresAt = expiresAt,
                )
            )
        }
        if (!resp.status.isSuccess()) throw mapError(resp)
        val rows = json.decodeFromString(ListSerializer(StoryRow.serializer()), resp.bodyAsText())
        rows.firstOrNull() ?: error("Empty response from stories insert")
    }

    suspend fun fetchActiveStories(userId: String): Result<List<StoryRow>> = runCatching {
        val auth = requireAuth()
        val nowIso = Instant.now().toString()
        val resp = client.get(
            "$supabaseUrl/rest/v1/stories?user_id=eq.$userId&expires_at=gt.$nowIso&order=created_at.desc"
        ) {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
        }
        if (!resp.status.isSuccess()) throw mapError(resp)
        json.decodeFromString(ListSerializer(StoryRow.serializer()), resp.bodyAsText())
    }

    fun chatMediaPublicUrl(path: String): String =
        "$supabaseUrl/storage/v1/object/public/chat-media/$path"

    suspend fun fetchProfileById(userId: String): Result<Profile?> = runCatching {
        val auth = requireAuth()
        val resp = client.get("$supabaseUrl/rest/v1/profiles?id=eq.$userId&select=*&limit=1") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
        }
        if (!resp.status.isSuccess()) throw mapError(resp)
        json.decodeFromString(ListSerializer(Profile.serializer()), resp.bodyAsText()).firstOrNull()
    }

    suspend fun loadNicknames(): Result<Map<String, String>> = runCatching {
        val auth = requireAuth()
        val uid = auth.session.user?.id ?: error("No user id")
        val resp = client.get("$supabaseUrl/rest/v1/nicknames?owner_id=eq.$uid&select=*") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
        }
        if (!resp.status.isSuccess()) throw mapError(resp)
        val rows = json.decodeFromString(ListSerializer(NicknameRow.serializer()), resp.bodyAsText())
        rows.associate { it.friendId to it.nickname }
    }

    suspend fun setNickname(friendId: String, nickname: String?): Result<Unit> = runCatching {
        val auth = requireAuth()
        val uid = auth.session.user?.id ?: error("No user id")
        if (nickname.isNullOrBlank()) {
            val resp = client.delete(
                "$supabaseUrl/rest/v1/nicknames?owner_id=eq.$uid&friend_id=eq.$friendId"
            ) {
                anonHeaders()
                header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
            }
            if (!resp.status.isSuccess()) throw mapError(resp)
        } else {
            val resp = client.post("$supabaseUrl/rest/v1/nicknames") {
                anonHeaders()
                header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
                header("Prefer", "resolution=merge-duplicates,return=minimal")
                contentType(ContentType.Application.Json)
                setBody(NicknameRow(ownerId = uid, friendId = friendId, nickname = nickname.trim()))
            }
            if (!resp.status.isSuccess()) throw mapError(resp)
        }
    }

    suspend fun searchUsersByPrefix(query: String): Result<List<Profile>> = runCatching {
        val auth = requireAuth()
        val q = query.trim().lowercase().replace("%", "").replace("_", "")
        if (q.isBlank()) return@runCatching emptyList<Profile>()
        val myId = auth.session.user?.id
        val resp = client.get("$supabaseUrl/rest/v1/profiles?select=*&username=ilike.$q%25&order=username.asc&limit=20") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
        }
        if (!resp.status.isSuccess()) throw mapError(resp)
        val list = json.decodeFromString(ListSerializer(Profile.serializer()), resp.bodyAsText())
        list.filter { it.id != myId }
    }

    suspend fun fetchAppConfig(): Result<AppConfigRow?> = runCatching {
        require(isConfigured) { "Backend is not configured." }
        val resp = client.get("$supabaseUrl/rest/v1/app_config?select=*&limit=1") {
            anonHeaders()
            (_status.value as? SessionStatus.Authenticated)?.session?.accessToken?.let {
                header(HttpHeaders.Authorization, "Bearer $it")
            }
        }
        if (!resp.status.isSuccess()) throw mapError(resp)
        val list = json.decodeFromString(ListSerializer(AppConfigRow.serializer()), resp.bodyAsText())
        list.firstOrNull()
    }

    suspend fun searchUsers(query: String): Result<List<Profile>> = runCatching {
        val auth = requireAuth()
        val q = query.trim().lowercase()
        val filter = if (q.isBlank()) "" else "&or=(username.ilike.*$q*,email.ilike.*$q*)"
        val resp = client.get("$supabaseUrl/rest/v1/profiles?select=*&order=username.asc&limit=50$filter") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
        }
        if (!resp.status.isSuccess()) throw mapError(resp)
        json.decodeFromString(ListSerializer(Profile.serializer()), resp.bodyAsText())
    }

    suspend fun banUser(userId: String, durationHours: Long?, reason: String): Result<Unit> = runCatching {
        val auth = requireAuth()
        val until = if (durationHours == null) {
            "9999-12-31T23:59:59Z"
        } else {
            Instant.now().plusSeconds(durationHours * 3600).toString()
        }
        val resp = client.post("$supabaseUrl/rest/v1/profiles?id=eq.$userId") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
            header("Prefer", "return=minimal")
            header("X-HTTP-Method-Override", "PATCH")
            contentType(ContentType.Application.Json)
            setBody(BanPatch(bannedUntil = until, banReason = reason))
        }
        if (!resp.status.isSuccess()) throw mapError(resp)
    }

    suspend fun unbanUser(userId: String): Result<Unit> = runCatching {
        val auth = requireAuth()
        val resp = client.post("$supabaseUrl/rest/v1/profiles?id=eq.$userId") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
            header("Prefer", "return=minimal")
            header("X-HTTP-Method-Override", "PATCH")
            contentType(ContentType.Application.Json)
            setBody(BanPatch(bannedUntil = null, banReason = null))
        }
        if (!resp.status.isSuccess()) throw mapError(resp)
    }

    suspend fun createChildAccount(
        username: String,
        password: String,
    ): Result<Unit> = runCatching {
        val auth = requireAuth()
        val parentId = auth.session.user?.id ?: error("No parent id")
        val email = "child+${parentId.take(8)}.${username.lowercase()}@nexa.app"
        val savedSession = auth.session
        val savedProfile = auth.profile

        val result = signUp(
            email = email,
            username = username,
            displayName = username.replaceFirstChar { it.uppercase() },
            password = password,
            parentId = parentId,
        )
        result.getOrThrow()

        _status.value = SessionStatus.Authenticated(savedSession, savedProfile)
    }

    suspend fun fileReport(
        targetUserId: String?,
        targetMessageId: String?,
        kind: String,
        reason: String,
        contextJson: String? = null,
    ): Result<Unit> = runCatching {
        val auth = requireAuth()
        val reporterId = auth.session.user?.id ?: error("No user id")
        val resp = client.post("$supabaseUrl/rest/v1/reports") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
            header("Prefer", "return=minimal")
            contentType(ContentType.Application.Json)
            setBody(
                ReportInsert(
                    reporterId = reporterId,
                    targetUserId = targetUserId,
                    targetMessageId = targetMessageId,
                    reason = reason,
                    kind = kind,
                    messageContextJson = contextJson,
                )
            )
        }
        if (!resp.status.isSuccess()) throw mapError(resp)
    }

    suspend fun myReports(): Result<List<ReportRow>> = runCatching {
        val auth = requireAuth()
        val uid = auth.session.user?.id ?: error("No user id")
        val resp = client.get("$supabaseUrl/rest/v1/reports?reporter_id=eq.$uid&select=*&order=created_at.desc") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
        }
        if (!resp.status.isSuccess()) throw mapError(resp)
        json.decodeFromString(ListSerializer(ReportRow.serializer()), resp.bodyAsText())
    }

    suspend fun pendingReports(): Result<List<ReportRow>> = runCatching {
        val auth = requireAuth()
        val resp = client.get("$supabaseUrl/rest/v1/reports?status=eq.pending&select=*&order=created_at.desc&limit=100") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
        }
        if (!resp.status.isSuccess()) throw mapError(resp)
        json.decodeFromString(ListSerializer(ReportRow.serializer()), resp.bodyAsText())
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
            ListSerializer(Profile.serializer()),
            resp.bodyAsText()
        )
        return list.firstOrNull()
    }

    private suspend fun patchProfile(
        patch: ProfilePatch,
        @Suppress("UNUSED_PARAMETER") update: (Profile) -> Profile,
    ) {
        val auth = requireAuth()
        val uid = auth.session.user?.id ?: error("No user id")
        val resp = client.patch("$supabaseUrl/rest/v1/profiles?id=eq.$uid") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer ${auth.session.accessToken}")
            header("Prefer", "return=minimal")
            contentType(ContentType.Application.Json)
            setBody(patch)
        }
        if (!resp.status.isSuccess()) throw mapError(resp)
        // Refetch the current profile from the server so local state mirrors DB.
        val refreshed = runCatching { fetchProfile(auth.session) }.getOrNull()
        _status.value = auth.copy(profile = refreshed ?: auth.profile)
    }

    private fun requireAuth(): SessionStatus.Authenticated {
        return _status.value as? SessionStatus.Authenticated
            ?: throw IllegalStateException("Not signed in")
    }

    private fun isCurrentlyBanned(until: String?): Boolean {
        if (until.isNullOrBlank()) return false
        return runCatching { Instant.parse(until).isAfter(Instant.now()) }.getOrDefault(false)
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
