package com.rork.nexa.data.chat

import android.content.Context
import com.rork.nexa.BuildConfig
import com.rork.nexa.data.auth.AuthRepository
import com.rork.nexa.data.auth.Profile
import com.rork.nexa.data.auth.SessionStatus
import com.rork.nexa.data.realtime.PostgresChangeFilter
import com.rork.nexa.data.realtime.RealtimeChannel
import com.rork.nexa.data.realtime.RealtimeClient
import com.rork.nexa.data.realtime.RealtimeEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class ChatRepository private constructor(context: Context) {

    private val auth = AuthRepository.get(context)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val rest = HttpClient(Android) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
    }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val supabaseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY
    private val realtimeHost = supabaseUrl
        .removePrefix("https://").removePrefix("http://").trimEnd('/')

    private val realtime = RealtimeClient(realtimeHost, anonKey)

    private val _conversations = MutableStateFlow<List<ConversationRow>>(emptyList())
    val conversations: StateFlow<List<ConversationRow>> = _conversations.asStateFlow()

    private val messageFlows = mutableMapOf<String, MutableStateFlow<List<MessageRow>>>()
    private val typingFlows = mutableMapOf<String, MutableStateFlow<Boolean>>()
    private val typingTimers = mutableMapOf<String, Job>()
    private val perChatChannels = mutableMapOf<String, RealtimeChannel>()
    private val profileCache = MutableStateFlow<Map<String, Profile>>(emptyMap())
    val profiles: StateFlow<Map<String, Profile>> = profileCache.asStateFlow()

    private var myId: String? = null
    private var globalChannel: RealtimeChannel? = null
    private var started = false

    fun messagesFor(conversationId: String): StateFlow<List<MessageRow>> =
        messageFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }.asStateFlow()

    fun typingFor(conversationId: String): StateFlow<Boolean> =
        typingFlows.getOrPut(conversationId) { MutableStateFlow(false) }.asStateFlow()

    fun start(userId: String, jwt: String) {
        if (started && myId == userId) {
            realtime.updateAccessToken(jwt)
            return
        }
        stop()
        myId = userId
        started = true
        realtime.start(jwt)

        val ch = realtime.channel("user:$userId").apply {
            postgresChanges(
                PostgresChangeFilter(
                    event = "INSERT",
                    table = "messages",
                    filter = "recipient_id=eq.$userId",
                )
            )
            postgresChanges(
                PostgresChangeFilter(
                    event = "INSERT",
                    table = "messages",
                    filter = "sender_id=eq.$userId",
                )
            )
            postgresChanges(
                PostgresChangeFilter(event = "INSERT", table = "conversations")
            )
            postgresChanges(
                PostgresChangeFilter(event = "UPDATE", table = "conversations")
            )
        }
        globalChannel = ch
        realtime.subscribe(ch)

        scope.launch {
            ch.events.collect { ev ->
                when (ev) {
                    is RealtimeEvent.Insert -> when (ev.table) {
                        "messages" -> handleIncomingMessage(parseMessage(ev.record) ?: return@collect)
                        "conversations" -> parseConversation(ev.record)?.let { upsertConversation(it) }
                    }
                    is RealtimeEvent.Update -> when (ev.table) {
                        "conversations" -> parseConversation(ev.record)?.let { upsertConversation(it) }
                        else -> Unit
                    }
                    else -> Unit
                }
            }
        }

        scope.launch { loadConversations() }
    }

    fun stop() {
        started = false
        realtime.stop()
        myId = null
        globalChannel = null
        perChatChannels.clear()
        typingTimers.values.forEach { it.cancel() }
        typingTimers.clear()
        _conversations.value = emptyList()
        messageFlows.clear()
        typingFlows.clear()
        profileCache.value = emptyMap()
    }

    fun updateAccessToken(jwt: String) {
        realtime.updateAccessToken(jwt)
    }

    private suspend fun loadConversations() {
        val (uid, jwt) = currentAuth() ?: return
        val resp = rest.get("$supabaseUrl/rest/v1/conversations?select=*&order=last_message_at.desc.nullslast&limit=200") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer $jwt")
        }
        if (!resp.status.isSuccess()) return
        val list = runCatching {
            json.decodeFromString(ListSerializer(ConversationRow.serializer()), resp.bodyAsText())
        }.getOrDefault(emptyList())
        _conversations.value = list
        ensureProfilesFor(list.map { it.other(uid) })
    }

    suspend fun openOrCreateConversation(otherUserId: String): Result<String> = runCatching {
        val (uid, jwt) = currentAuth() ?: error("Not signed in")
        val (a, b) = if (uid < otherUserId) uid to otherUserId else otherUserId to uid
        // try existing
        val getResp = rest.get(
            "$supabaseUrl/rest/v1/conversations?select=*&user_a=eq.$a&user_b=eq.$b&limit=1"
        ) {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer $jwt")
        }
        if (getResp.status.isSuccess()) {
            val existing = runCatching {
                json.decodeFromString(ListSerializer(ConversationRow.serializer()), getResp.bodyAsText())
            }.getOrDefault(emptyList()).firstOrNull()
            if (existing != null) {
                upsertConversation(existing)
                return@runCatching existing.id
            }
        }
        val resp = rest.post("$supabaseUrl/rest/v1/conversations") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer $jwt")
            header("Prefer", "resolution=merge-duplicates,return=representation")
            contentType(ContentType.Application.Json)
            setBody(ConversationInsertBody(userA = a, userB = b))
        }
        if (!resp.status.isSuccess()) error("Couldn't start chat (${resp.status.value})")
        val rows = json.decodeFromString(ListSerializer(ConversationRow.serializer()), resp.bodyAsText())
        val row = rows.firstOrNull() ?: error("Empty conversation response")
        upsertConversation(row)
        row.id
    }

    suspend fun loadHistory(conversationId: String) {
        val (_, jwt) = currentAuth() ?: return
        val resp = rest.get(
            "$supabaseUrl/rest/v1/messages?conversation_id=eq.$conversationId&order=created_at.asc&limit=200&select=*"
        ) {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer $jwt")
        }
        if (!resp.status.isSuccess()) return
        val list = runCatching {
            json.decodeFromString(ListSerializer(MessageRow.serializer()), resp.bodyAsText())
        }.getOrDefault(emptyList())
        val flow = messageFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
        flow.value = list
    }

    suspend fun sendMessage(
        conversationId: String,
        recipientId: String,
        text: String,
    ): Result<Unit> = runCatching {
        val (uid, jwt) = currentAuth() ?: error("Not signed in")
        val resp = rest.post("$supabaseUrl/rest/v1/messages") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer $jwt")
            header("Prefer", "return=representation")
            contentType(ContentType.Application.Json)
            setBody(
                MessageInsertBody(
                    conversationId = conversationId,
                    senderId = uid,
                    recipientId = recipientId,
                    text = text,
                )
            )
        }
        if (!resp.status.isSuccess()) error("Couldn't send message")
        val rows = runCatching {
            json.decodeFromString(ListSerializer(MessageRow.serializer()), resp.bodyAsText())
        }.getOrDefault(emptyList())
        rows.firstOrNull()?.let { handleIncomingMessage(it) }
    }

    fun observeConversation(conversationId: String) {
        if (perChatChannels.containsKey(conversationId)) return
        val ch = realtime.channel("chat:$conversationId").apply { broadcast(self = false) }
        perChatChannels[conversationId] = ch
        realtime.subscribe(ch)
        scope.launch {
            ch.events.collect { ev ->
                if (ev is RealtimeEvent.Broadcast && ev.event == "typing") {
                    val from = (ev.payload["from"]?.jsonPrimitive)?.content
                    val isTyping = (ev.payload["isTyping"]?.jsonPrimitive)?.let {
                        runCatching { it.boolean }.getOrDefault(false)
                    } ?: false
                    if (from != null && from != myId) {
                        flipTyping(conversationId, isTyping)
                    }
                }
            }
        }
    }

    fun stopObservingConversation(conversationId: String) {
        perChatChannels.remove(conversationId)?.unsubscribe()
        typingTimers.remove(conversationId)?.cancel()
        typingFlows[conversationId]?.value = false
    }

    fun sendTyping(conversationId: String, isTyping: Boolean) {
        val uid = myId ?: return
        val ch = perChatChannels[conversationId] ?: return
        scope.launch {
            ch.send(
                "typing",
                buildJsonObject {
                    put("from", uid)
                    put("isTyping", isTyping)
                },
            )
        }
    }

    private fun flipTyping(conversationId: String, isTyping: Boolean) {
        val flow = typingFlows.getOrPut(conversationId) { MutableStateFlow(false) }
        flow.value = isTyping
        typingTimers[conversationId]?.cancel()
        if (isTyping) {
            typingTimers[conversationId] = scope.launch {
                delay(4000L)
                flow.value = false
            }
        }
    }

    private suspend fun handleIncomingMessage(row: MessageRow) {
        val flow = messageFlows.getOrPut(row.conversationId) { MutableStateFlow(emptyList()) }
        flow.update { current ->
            if (current.any { it.id == row.id }) current
            else (current + row).sortedBy { it.createdAt ?: "" }
        }
        val convs = _conversations.value
        if (convs.none { it.id == row.conversationId }) {
            // unknown conversation — fetch it
            fetchAndUpsertConversation(row.conversationId)
        } else {
            // bump locally so list reorders even before realtime conv update arrives
            _conversations.update { list ->
                list.map { c ->
                    if (c.id == row.conversationId) c.copy(
                        lastMessage = row.text,
                        lastMessageAt = row.createdAt ?: c.lastMessageAt,
                    ) else c
                }.sortedByDescending { it.lastMessageAt ?: "" }
            }
        }
    }

    private suspend fun fetchAndUpsertConversation(id: String) {
        val (uid, jwt) = currentAuth() ?: return
        val resp = rest.get("$supabaseUrl/rest/v1/conversations?id=eq.$id&select=*&limit=1") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer $jwt")
        }
        if (!resp.status.isSuccess()) return
        val row = runCatching {
            json.decodeFromString(ListSerializer(ConversationRow.serializer()), resp.bodyAsText())
        }.getOrDefault(emptyList()).firstOrNull() ?: return
        upsertConversation(row)
        ensureProfilesFor(listOf(row.other(uid)))
    }

    private fun upsertConversation(row: ConversationRow) {
        _conversations.update { list ->
            val without = list.filterNot { it.id == row.id }
            (without + row).sortedByDescending { it.lastMessageAt ?: "" }
        }
        myId?.let { uid -> scope.launch { ensureProfilesFor(listOf(row.other(uid))) } }
    }

    suspend fun ensureProfilesFor(userIds: List<String>) {
        val (_, jwt) = currentAuth() ?: return
        val cached = profileCache.value
        val missing = userIds.distinct().filter { it.isNotBlank() && it !in cached }
        if (missing.isEmpty()) return
        val inList = missing.joinToString(",") { "\"$it\"" }
        val resp = rest.get("$supabaseUrl/rest/v1/profiles?id=in.($inList)&select=*") {
            anonHeaders()
            header(HttpHeaders.Authorization, "Bearer $jwt")
        }
        if (!resp.status.isSuccess()) return
        val list = runCatching {
            json.decodeFromString(ListSerializer(Profile.serializer()), resp.bodyAsText())
        }.getOrDefault(emptyList())
        if (list.isEmpty()) return
        profileCache.update { cur -> cur + list.associateBy { it.id } }
    }

    private fun parseMessage(obj: JsonObject): MessageRow? = runCatching {
        json.decodeFromString(MessageRow.serializer(), obj.toString())
    }.getOrNull()

    private fun parseConversation(obj: JsonObject): ConversationRow? = runCatching {
        json.decodeFromString(ConversationRow.serializer(), obj.toString())
    }.getOrNull()

    private fun currentAuth(): Pair<String, String>? {
        val s = auth.status.value as? SessionStatus.Authenticated ?: return null
        val uid = s.session.user?.id ?: return null
        return uid to s.session.accessToken
    }

    private fun io.ktor.client.request.HttpRequestBuilder.anonHeaders() {
        headers { append("apikey", anonKey) }
    }

    companion object {
        @Volatile private var instance: ChatRepository? = null
        fun get(context: Context): ChatRepository =
            instance ?: synchronized(this) {
                instance ?: ChatRepository(context.applicationContext).also { instance = it }
            }
    }
}
