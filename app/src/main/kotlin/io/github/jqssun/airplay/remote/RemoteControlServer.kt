package io.github.jqssun.airplay.remote

import android.content.Context
import android.os.Handler
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.FutureTask
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Embedded LAN controller; all app/window/audio operations are serialized on the main thread. */
class RemoteControlServer(
    context: Context,
    private val handler: Handler,
    private val snapshot: () -> JSONObject,
    private val command: (String, JSONObject) -> Unit,
) : NanoHTTPD(PORT) {
    private val assets = mapOf(
        "/" to ("text/html; charset=utf-8" to "index.html"),
        "/remote.css" to ("text/css; charset=utf-8" to "remote.css"),
        "/remote.js" to ("application/javascript; charset=utf-8" to "remote.js"),
    ).mapValues { (_, value) -> value.first to context.assets.open("remote/${value.second}").bufferedReader().use { it.readText() } }
    private val random = SecureRandom()
    val pairingCode: String = (100000 + random.nextInt(900000)).toString()
    private val token = ByteArray(24).also(random::nextBytes).joinToString("") { "%02x".format(it) }
    private var loginWindowAt = 0L
    private var loginAttempts = 0
    @Volatile private var closed = false

    init {
        // Slow clients must not grow an unbounded thread pool or block the TV main thread.
        setAsyncRunner(object : AsyncRunner {
            private val active = ConcurrentHashMap.newKeySet<ClientHandler>()
            private val pool = ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS, SynchronousQueue<Runnable>()) {
                Thread(it, "TVRemoteHTTP").apply { isDaemon = true }
            }
            override fun exec(client: ClientHandler) {
                active.add(client)
                try { pool.execute(client) } catch (_: RejectedExecutionException) {
                    active.remove(client); client.close()
                }
            }
            override fun closed(client: ClientHandler) { active.remove(client) }
            override fun closeAll() {
                closed = true
                active.toList().forEach { it.close() }
                pool.shutdownNow()
            }
        })
    }

    fun urls(): List<String> = localAddresses().filterNot { it.startsWith("127.") }.map { "http://$it:$PORT" }

    override fun serve(session: IHTTPSession): Response {
        try {
            val host = session.headers["host"]?.lowercase() ?: ""
            val allowedHosts = localAddresses().map { "$it:$PORT" }.toSet()
            val remote = InetAddress.getByName(session.remoteIpAddress)
            if (!(remote.isSiteLocalAddress || remote.isLoopbackAddress || remote.isLinkLocalAddress) || host !in allowedHosts) {
                return error(Response.Status.FORBIDDEN, "请使用电视的局域网地址访问")
            }
            val origin = session.headers["origin"]
            if (origin != null && origin != "http://$host") return error(Response.Status.FORBIDDEN, "不允许跨站控制")
            if (session.method == Method.GET && assets.containsKey(session.uri)) {
                val (mime, body) = assets.getValue(session.uri)
                return response(Response.Status.OK, mime, body)
            }
            if (session.uri == "/api/pair" && session.method == Method.POST) {
                if (!allowLogin()) return error(Response.Status.TOO_MANY_REQUESTS, "尝试过于频繁，请稍后重试")
                val body = readJson(session)
                require(body.length() == 1 && body.opt("code") is String)
                if (!equalSecret(body.getString("code"), pairingCode)) return error(Response.Status.UNAUTHORIZED, "配对码不正确")
                return json(JSONObject().put("token", token))
            }
            if (!session.uri.startsWith("/api/")) return error(Response.Status.NOT_FOUND, "页面不存在")
            if (!equalSecret(session.headers["authorization"] ?: "", "Bearer $token")) {
                return error(Response.Status.UNAUTHORIZED, "请先输入电视上的配对码")
            }
            if (session.uri == "/api/state" && session.method == Method.GET) return json(onMain(snapshot))
            val action = when (session.uri) {
                "/api/move" -> "move"
                "/api/size" -> "size"
                "/api/audio" -> "audio"
                "/api/reset" -> "reset"
                else -> return error(Response.Status.NOT_FOUND, "接口不存在")
            }
            if (session.method != Method.POST) return error(Response.Status.METHOD_NOT_ALLOWED, "此操作需要 POST")
            val body = readJson(session)
            when (action) {
                "move" -> {
                    require(body.length() == 2)
                    requireInt(body, "dx", -96..96); requireInt(body, "dy", -96..96)
                }
                "size" -> { require(body.length() == 1); requireInt(body, "width", 1..16384) }
                "audio" -> require(body.length() == 1 && body.opt("source") in setOf("tv", "mirror"))
                "reset" -> require(body.length() == 0)
            }
            return json(onMain { command(action, body); snapshot() })
        } catch (_: IllegalArgumentException) {
            return error(Response.Status.BAD_REQUEST, "控制参数不正确")
        } catch (_: org.json.JSONException) {
            return error(Response.Status.BAD_REQUEST, "请求格式不正确")
        } catch (_: Exception) {
            return error(Response.Status.SERVICE_UNAVAILABLE, "电视暂时忙，请稍后重试")
        }
    }

    private fun readJson(session: IHTTPSession): JSONObject {
        require(session.headers["transfer-encoding"] == null)
        require(session.headers["content-type"]?.substringBefore(';')?.trim() == "application/json")
        val length = session.headers["content-length"]?.toIntOrNull() ?: 0
        require(length in 2..512)
        val files = mutableMapOf<String, String>()
        session.parseBody(files)
        return JSONObject(files["postData"] ?: throw IllegalArgumentException())
    }

    private fun requireInt(body: JSONObject, key: String, range: IntRange) {
        val value = body.get(key)
        require(value is Int && value in range)
    }

    private fun <T> onMain(block: () -> T): T {
        check(!closed)
        val task = FutureTask { check(!closed); block() }
        handler.post(task)
        try { return task.get(2, TimeUnit.SECONDS) } finally { task.cancel(false) }
    }

    @Synchronized private fun allowLogin(): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - loginWindowAt > 30_000) { loginWindowAt = now; loginAttempts = 0 }
        return ++loginAttempts <= 8
    }

    private fun equalSecret(a: String, b: String) = MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
    private fun json(value: JSONObject) = response(Response.Status.OK, "application/json; charset=utf-8", value.toString())
    private fun error(status: Response.Status, message: String) =
        response(status, "application/json; charset=utf-8", JSONObject().put("error", message).toString())
    private fun response(status: Response.Status, mime: String, body: String): Response =
        newFixedLengthResponse(status, mime, body).apply {
            addHeader("Cache-Control", "no-store")
            addHeader("X-Content-Type-Options", "nosniff")
            addHeader("X-Frame-Options", "DENY")
            addHeader("Referrer-Policy", "no-referrer")
            addHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'")
            closeConnection(true)
        }

    companion object {
        const val PORT = 8765
        private fun localAddresses(): List<String> = listOf("127.0.0.1") + runCatching {
            NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp }.flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>().filter { it.isSiteLocalAddress }.mapNotNull { it.hostAddress }
        }.getOrDefault(emptyList())
    }
}
