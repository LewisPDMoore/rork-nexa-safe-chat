package com.rork.nexa.data.realtime

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.concurrent.atomic.AtomicLong

sealed class RealtimeEvent {
    data class Insert(val table: String, val record: JsonObject) : RealtimeEvent()
    data class Update(val table: String, val record: JsonObject, val old: JsonObject?) : RealtimeEvent()
    data class Delete(val table: String, val old: JsonObject) : RealtimeEvent()
    data class Broadcast(val event: String, val payload: JsonObject) : RealtimeEvent()
}

data class PostgresChangeFilter(
    val event: String, // INSERT | UPDATE | DELETE | *
    val schema: String = "public",
    val table: String,
    val filter: String? = null,
)

class RealtimeChannel internal constructor(
    val topic: String,
    val client: RealtimeClient,
) {
    private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()

    internal val postgresChanges = mutableListOf<PostgresChangeFilter>()
    internal var enableBroadcast: Boolean = false
    internal var broadcastSelf: Boolean = false

    fun postgresChanges(filter: PostgresChangeFilter) = apply { postgresChanges += filter }
    fun broadcast(self: Boolean = false) = apply {
        enableBroadcast = true; broadcastSelf = self
    }

    suspend fun send(event: String, payload: JsonObject) {
        client.sendBroadcast(topic, event, payload)
    }

    fun unsubscribe() {
        client.unsubscribe(this)
    }

    internal suspend fun emit(e: RealtimeEvent) {
        _events.emit(e)
    }
}

class RealtimeClient(
    private val host: String,
    private val anonKey: String,
) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val httpClient = HttpClient(CIO) { install(WebSockets) }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val ref = AtomicLong(0L)
    private var session: WebSocketSession? = null
    private var connectionJob: Job? = null
    private var heartbeatJob: Job? = null

    private val channels = mutableMapOf<String, RealtimeChannel>()

    @Volatile private var accessToken: String? = null
    @Volatile private var started: Boolean = false

    fun start(jwt: String?) {
        accessToken = jwt
        if (started) return
        started = true
        connectionJob = scope.launch { connectLoop() }
    }

    fun updateAccessToken(jwt: String?) {
        accessToken = jwt
        scope.launch {
            val s = session ?: return@launch
            // push access_token update to all channels
            channels.keys.forEach { topic ->
                runCatching {
                    s.send(buildPhxMessage(topic, "access_token", buildJsonObject {
                        put("access_token", jwt ?: "")
                    }))
                }
            }
        }
    }

    fun stop() {
        started = false
        connectionJob?.cancel()
        heartbeatJob?.cancel()
        runCatching { scope.launch { session?.close() } }
        session = null
        channels.clear()
    }

    fun channel(topic: String): RealtimeChannel {
        val full = if (topic.startsWith("realtime:")) topic else "realtime:$topic"
        return channels.getOrPut(full) { RealtimeChannel(full, this) }
    }

    fun subscribe(channel: RealtimeChannel) {
        channels[channel.topic] = channel
        scope.launch {
            val s = session ?: return@launch
            joinChannel(s, channel)
        }
    }

    fun unsubscribe(channel: RealtimeChannel) {
        scope.launch {
            session?.let { s ->
                runCatching {
                    s.send(buildPhxMessage(channel.topic, "phx_leave", buildJsonObject { }))
                }
            }
            channels.remove(channel.topic)
        }
    }

    internal suspend fun sendBroadcast(topic: String, event: String, payload: JsonObject) {
        val s = session ?: return
        val body = buildJsonObject {
            put("type", "broadcast")
            put("event", event)
            put("payload", payload)
        }
        runCatching { s.send(buildPhxMessage(topic, "broadcast", body)) }
    }

    private suspend fun connectLoop() {
        var backoff = 1000L
        while (started) {
            val url = "wss://$host/realtime/v1/websocket?apikey=$anonKey&vsn=1.0.0"
            val ok = runCatching {
                httpClient.webSocketSession(urlString = url) {}
            }.getOrNull()
            if (ok == null) {
                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(15_000L)
                continue
            }
            session = ok
            backoff = 1000L
            heartbeatJob?.cancel()
            heartbeatJob = scope.launch { heartbeatLoop(ok) }
            // re-join all channels
            channels.values.forEach { runCatching { joinChannel(ok, it) } }
            // Read frames
            try {
                while (scope.isActive && started) {
                    val frame = ok.incoming.receive()
                    if (frame is Frame.Text) handleIncoming(frame.readText())
                }
            } catch (_: Throwable) {
                // disconnected
            }
            heartbeatJob?.cancel()
            session = null
            if (!started) break
            delay(1000L)
        }
    }

    private suspend fun heartbeatLoop(s: WebSocketSession) {
        try {
            while (scope.isActive) {
                delay(25_000L)
                runCatching {
                    s.send(buildPhxMessage("phoenix", "heartbeat", buildJsonObject { }))
                }
            }
        } catch (_: Throwable) {
        }
    }

    private suspend fun joinChannel(s: WebSocketSession, channel: RealtimeChannel) {
        val payload = buildJsonObject {
            putJsonObject("config") {
                if (channel.postgresChanges.isNotEmpty()) {
                    putJsonArray("postgres_changes") {
                        channel.postgresChanges.forEach { f ->
                            add(buildJsonObject {
                                put("event", f.event)
                                put("schema", f.schema)
                                put("table", f.table)
                                if (f.filter != null) put("filter", f.filter)
                            })
                        }
                    }
                }
                if (channel.enableBroadcast) {
                    putJsonObject("broadcast") {
                        put("self", channel.broadcastSelf)
                        put("ack", false)
                    }
                }
                putJsonObject("presence") { put("key", "") }
            }
            accessToken?.let { put("access_token", it) }
        }
        s.send(buildPhxMessage(channel.topic, "phx_join", payload))
    }

    private fun buildPhxMessage(topic: String, event: String, payload: JsonObject): String {
        val r = ref.incrementAndGet().toString()
        val obj = buildJsonObject {
            put("topic", topic)
            put("event", event)
            put("payload", payload)
            put("ref", r)
        }
        return obj.toString()
    }

    private suspend fun handleIncoming(text: String) {
        val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
        val topic = obj["topic"]?.jsonPrimitive?.contentOrNullSafe() ?: return
        val event = obj["event"]?.jsonPrimitive?.contentOrNullSafe() ?: return
        val payload = obj["payload"]?.jsonObject ?: return
        val ch = channels[topic] ?: return

        when (event) {
            "postgres_changes" -> {
                val data = payload["data"]?.jsonObject ?: return
                val type = data["type"]?.jsonPrimitive?.contentOrNullSafe() ?: return
                val table = data["table"]?.jsonPrimitive?.contentOrNullSafe() ?: ""
                when (type) {
                    "INSERT" -> {
                        val record = data["record"]?.jsonObject ?: return
                        ch.emit(RealtimeEvent.Insert(table, record))
                    }
                    "UPDATE" -> {
                        val record = data["record"]?.jsonObject ?: return
                        val old = data["old_record"]?.jsonObject
                        ch.emit(RealtimeEvent.Update(table, record, old))
                    }
                    "DELETE" -> {
                        val old = data["old_record"]?.jsonObject ?: return
                        ch.emit(RealtimeEvent.Delete(table, old))
                    }
                }
            }
            "broadcast" -> {
                val ev = payload["event"]?.jsonPrimitive?.contentOrNullSafe() ?: return
                val inner = payload["payload"]?.jsonObject ?: JsonObject(emptyMap())
                ch.emit(RealtimeEvent.Broadcast(ev, inner))
            }
            else -> Unit // phx_reply, presence_state, etc.
        }
    }

    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? =
        runCatching { content }.getOrNull()
}
