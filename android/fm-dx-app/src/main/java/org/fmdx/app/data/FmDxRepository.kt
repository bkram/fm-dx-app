package org.fmdx.app.data

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.fmdx.app.BuildConfig
import org.fmdx.app.model.SpectrumPoint
import org.fmdx.app.model.TunerInfo
import org.fmdx.app.model.TunerState
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.EOFException
import java.io.IOException
import java.net.SocketException
import java.util.Locale

data class SpectrumPluginEvent(
    val status: String? = null,
    val points: List<SpectrumPoint>? = null
)

class FmDxRepository(
    val client: OkHttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val logoCache = mutableMapOf<String, String?>()
    fun connectControl(
        baseUrl: String,
        userAgent: String,
        scope: CoroutineScope,
        onState: (TunerState) -> Unit,
        onClosed: () -> Unit,
        onError: (Throwable) -> Unit
    ): ControlConnection {
        val wsUrl = buildWebSocketUrl(baseUrl, "text")
        logDebug("connectControl(): opening $wsUrl")
        val request = Request.Builder()
            .url(wsUrl)
            .header("User-Agent", "$userAgent (control)")
            .build()
        val commandChannel = Channel<String>(capacity = Channel.UNLIMITED)
        lateinit var connection: ControlConnection
        val webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                logDebug("control socket: open with response=${response.code}")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                logDebug("control socket: message length=${text.length}")
                try {
                    val state = TunerState.fromJson(text)
                    onState(state)
                } catch (t: Throwable) {
                    logDebug("control socket: failed to parse message", t)
                    onError(t)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                logDebug("control socket: closing code=$code reason=$reason")
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                logDebug("control socket: closed code=$code reason=$reason")
                onClosed()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                handleWebSocketFailure("control", t, response, onError)
                onClosed()
            }
        })
        connection = ControlConnection(webSocket, commandChannel)
        scope.launch {
            for (command in commandChannel) {
                logDebug("control socket: sending queued command=$command")
                if (!webSocket.send(command)) {
                    logDebug("control socket: command send failed")
                    break
                }
                delay(COMMAND_THROTTLE_MS)
            }
        }
        return connection
    }

    fun connectPlugin(
        baseUrl: String,
        userAgent: String,
        onEvent: (SpectrumPluginEvent) -> Unit,
        onTelemetryEvent: (PluginTelemetryEvent) -> Unit = {},
        onError: (Throwable) -> Unit
    ): PluginConnection {
        val wsUrl = buildWebSocketUrl(baseUrl, "data_plugins")
        logDebug("connectPlugin(): opening $wsUrl")
        val request = Request.Builder()
            .url(wsUrl)
            .header("User-Agent", "$userAgent (plugin)")
            .build()
        val webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                logDebug("plugin socket: open with response=${response.code}")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                logDebug("plugin socket: message length=${text.length}")
                try {
                    val json = JSONObject(text)
                    handleTelemetryMessage(json, onTelemetryEvent)
                    val payload = json.optJSONObject("value") ?: json
                    val status = payload.optString("status").takeIf { it.isNotBlank() }
                    val points = parseSpectrumDataset(payload)
                    onEvent(SpectrumPluginEvent(status = status, points = points))
                } catch (ex: Exception) {
                    logDebug("plugin socket: failed to parse message", ex)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                handleWebSocketFailure("plugin", t, response, onError)
            }
        })
        return PluginConnection(webSocket)
    }

    private fun handleTelemetryMessage(
        json: JSONObject,
        onTelemetryEvent: (PluginTelemetryEvent) -> Unit
    ) {
        val type = json.optString("type").takeIf { it.isNotBlank() } ?: return
        val value = json.optJSONObject("value") ?: return
        when (type.lowercase(Locale.ROOT)) {
            "scanner" -> {
                val status = value.optString("status", null)
                val scanValue = value.optString("Scan", null)
                if (!status.isNullOrBlank() || !scanValue.isNullOrBlank()) {
                    onTelemetryEvent(PluginTelemetryEvent.Scanner(status, scanValue))
                }
            }

            "gps" -> {
                val status = value.optString("status", null)
                val lat = value.optString("lat", null)
                val lon = value.optString("lon", null)
                val alt = value.optString("alt", null)
                val mode = value.optString("mode", null)
                if (!status.isNullOrBlank() || !lat.isNullOrBlank() || !lon.isNullOrBlank()) {
                    onTelemetryEvent(PluginTelemetryEvent.Gps(status, lat, lon, alt, mode))
                }
            }
        }
    }

    suspend fun findStationLogo(
        baseUrl: String,
        pi: String?,
        program: String?,
        country: String?
    ): String = withContext(ioDispatcher) {
        val normalizedBase = baseUrl.trimEnd('/')
        val cacheKey =
            listOf(normalizedBase, pi ?: "", program ?: "", country ?: "").joinToString("|")
        logoCache[cacheKey]?.let { cached ->
            logDebug("findStationLogo(): cache hit -> $cached")
            return@withContext cached
        }

        val uppercasePi = pi?.takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT)
        val sanitizedProgram = program
            ?.uppercase(Locale.ROOT)
            ?.replace(LOGO_SANITIZE_REGEX, "")
            ?.takeIf { it.isNotBlank() }

        val filenames = buildList {
            if (uppercasePi != null) {
                if (sanitizedProgram != null) {
                    add("${uppercasePi}_${sanitizedProgram}.svg")
                    add("${uppercasePi}_${sanitizedProgram}.png")
                    add("${uppercasePi}_${sanitizedProgram}.gif")
                }
                add("${uppercasePi}.svg")
                add("${uppercasePi}.png")
                add("${uppercasePi}.gif")
            }
        }

        val localBase = "$normalizedBase/$LOGO_PATH"
        filenames.forEach { filename ->
            val candidate = "$localBase/$filename"
            if (urlExists(candidate)) {
                logDebug("findStationLogo(): local hit -> $candidate")
                logoCache[cacheKey] = candidate
                return@withContext candidate
            }
        }

        val countryCode = country?.takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT)
        if (countryCode != null && uppercasePi != null) {
            findRemoteLogo(countryCode, uppercasePi, sanitizedProgram, filenames)?.let { remote ->
                logDebug("findStationLogo(): remote hit -> $remote")
                logoCache[cacheKey] = remote
                return@withContext remote
            }
        }

        logDebug("findStationLogo(): no logo found, falling back to default")
        logoCache[cacheKey] = DEFAULT_LOGO_URL
        DEFAULT_LOGO_URL
    }

    private fun findRemoteLogo(
        countryCode: String,
        piCode: String,
        sanitizedProgram: String?,
        filenames: List<String>
    ): String? {
        val request = Request.Builder()
            .url("$REMOTE_LOGO_BASE/logo_directory.html")
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body.string()
                if (body.isBlank()) return null
                val document = Jsoup.parse(body)
                val folderElement = document.select(".folder").firstOrNull { element ->
                    element.text().trim().endsWith("./$countryCode")
                } ?: return null
                val fileContainer = folderElement.nextElementSibling() ?: return null
                val available = fileContainer.select(".file a")
                    .mapNotNull { it.text().trim().takeIf { text -> text.isNotEmpty() } }
                    .toSet()
                val priority = buildList {
                    if (sanitizedProgram != null) {
                        add("${piCode}_${sanitizedProgram}.svg")
                        add("${piCode}_${sanitizedProgram}.png")
                        add("${piCode}_${sanitizedProgram}.gif")
                    }
                    add("${piCode}.svg")
                    add("${piCode}.png")
                    add("${piCode}.gif")
                }
                priority.forEach { filename ->
                    if (filename in available) {
                        val resolved = "$REMOTE_LOGO_BASE/$countryCode/$filename"
                        logDebug("findRemoteLogo(): directory match -> $resolved")
                        return resolved
                    }
                }
                // fallback to direct URL checks if HTML structure differs
                filenames.forEach { filename ->
                    val candidate = "$REMOTE_LOGO_BASE/$countryCode/$filename"
                    if (urlExists(candidate)) {
                        logDebug("findRemoteLogo(): direct match -> $candidate")
                        return candidate
                    }
                }
                null
            }
        } catch (_: Exception) {
            logDebug("findRemoteLogo(): failed to parse remote directory for $countryCode")
            null
        }
    }

    suspend fun fetchTunerInfo(url: String, userAgent: String): TunerInfo =
        withContext(ioDispatcher) {
            logDebug("fetchTunerInfo(): requesting metadata from $url")
            val httpUrl = url.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid URL")
            val staticUrl = httpUrl.newBuilder()
                .addPathSegments("static_data")
                .build()

            var tunerName = ""
            var tunerDesc = ""
            var activeAnt: Int? = null
            val antennaNames = mutableListOf<String>()

            try {
                client.newCall(
                    Request.Builder()
                        .url(staticUrl)
                        .header("User-Agent", userAgent)
                        .build()
                ).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body.string()
                        if (body.isNotBlank()) {
                            val json = JSONObject(body)
                            tunerName = json.optString("tunerName", tunerName)
                            tunerDesc = json.optString("tunerDesc", tunerDesc)
                            activeAnt = when {
                                json.has("antSel") -> json.optInt("antSel")
                                json.has("activeAnt") -> json.optInt("activeAnt")
                                json.optJSONObject("ant")?.has("active") == true ->
                                    json.optJSONObject("ant")?.optInt("active")

                                else -> activeAnt
                            }
                            json.optJSONObject("ant")?.optJSONArray("names")?.let { arr ->
                                for (i in 0 until arr.length()) {
                                    arr.optString(i)?.takeIf { it.isNotBlank() }
                                        ?.let(antennaNames::add)
                                }
                            }
                        }
                    }
                }
            } catch (_: IOException) {
                // ignore static data fetch errors
            }

            logDebug("fetchTunerInfo(): scraping fallback HTML")
            val document: Document? = try {
                Jsoup.connect(url)
                    .userAgent(userAgent)
                    .timeout(TIMEOUT_MS.toInt())
                    .get()
            } catch (ex: IOException) {
                logDebug("fetchTunerInfo(): HTML scrape failed", ex)
                null
            }
            if (document != null) {
                if (tunerName.isBlank()) {
                    tunerName = document.selectFirst("meta[property=og:title]")?.attr("content")
                        ?.replace("FM-DX WebServer ", "")
                        ?.trim()
                        ?: ""
                }
                if (tunerDesc.isBlank()) {
                    tunerDesc =
                        document.selectFirst("meta[property=og:description]")?.attr("content")
                            ?.replace("Server description: ", "")
                            ?.trim()
                            ?: ""
                }
                if (antennaNames.isEmpty()) {
                    val elements = document.select("#data-ant ul.options li, #data-ant li")
                    elements.forEach { el ->
                        val text = el.text().trim()
                        if (text.isNotEmpty()) {
                            antennaNames += text
                        }
                    }
                }
                if (antennaNames.isEmpty()) {
                    if (document.selectFirst("#data-ant-container") != null || document.selectFirst(
                            "#data-ant"
                        ) != null
                    ) {
                        antennaNames += "Default"
                    }
                }
            }
            if (antennaNames.isEmpty()) {
                antennaNames += "Default"
            }
            val active = activeAnt ?: 0
            TunerInfo(
                tunerName = tunerName,
                tunerDescription = tunerDesc,
                antennaNames = antennaNames,
                activeAntenna = active
            )
        }

    suspend fun fetchSpectrumData(url: String, userAgent: String): List<SpectrumPoint>? =
        withContext(ioDispatcher) {
            logDebug("fetchSpectrumData(): requesting data from $url")
            val httpUrl = url.toHttpUrlOrNull() ?: return@withContext null
            val spectrumUrl = httpUrl.newBuilder()
                .addPathSegments("spectrum-graph-plugin")
                .build()
            val request = Request.Builder()
                .url(spectrumUrl)
                .header("User-Agent", userAgent)
                .header("X-Plugin-Name", "SpectrumGraphPlugin")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logDebug("fetchSpectrumData(): request failed code=${response.code}")
                    return@use null
                }
                val body = response.body.string()
                if (body.isBlank()) return@use null
                val json = JSONObject(body)
                parseSpectrumDataset(json)
            }
        }

    private fun handleWebSocketFailure(
        socketName: String,
        t: Throwable,
        response: Response?,
        onError: (Throwable) -> Unit
    ) {
        logDebug("$socketName socket: failure code=${response?.code}", t)
        if (t is EOFException) {
            logDebug("$socketName socket: server closed connection; suppressing popup")
            return
        }
        val message = when (t) {
            is SocketException -> "Connection to server lost"
            else -> t.message ?: "Unknown connection error"
        }
        onError(IOException(message, t))
    }

    private fun parseSpectrumDataset(json: JSONObject): List<SpectrumPoint>? {
        parseSpectrumPointsArray(json.optJSONArray("points"))?.let { return it }

        val candidateKeys = mutableListOf<String>()
        if (json.has("sd")) candidateKeys += "sd"
        if (json.has("ad")) {
            val key = when (val active = json.opt("ad")) {
                is Number -> "sd${active.toInt()}"
                is String -> "sd$active"
                else -> null
            }
            if (key != null) candidateKeys += key
        }
        val keysIterator = json.keys()
        while (keysIterator.hasNext()) {
            val key = keysIterator.next()
            if (key.startsWith("sd")) candidateKeys += key
        }
        candidateKeys.distinct().forEach { key ->
            val data = json.optString(key, "")
            parseSpectrumString(data)?.let { return it }
        }
        // some payloads nest dataset under "value"
        json.optJSONObject("value")?.let { nested ->
            parseSpectrumDataset(nested)?.let { return it }
        }
        return null
    }

    private fun parseSpectrumPointsArray(array: JSONArray?): List<SpectrumPoint>? {
        if (array == null) return null
        val result = mutableListOf<SpectrumPoint>()
        for (i in 0 until array.length()) {
            val entry = array.optJSONObject(i) ?: continue
            val freq = entry.optDouble("freq", Double.NaN)
            val level = entry.optDouble("level", Double.NaN)
            if (!freq.isNaN() && !level.isNaN()) {
                result += SpectrumPoint(freq, level)
            } else {
                val x = entry.optDouble("x", Double.NaN)
                val y = entry.optDouble("y", Double.NaN)
                if (!x.isNaN() && !y.isNaN()) {
                    result += SpectrumPoint(x, y)
                }
            }
        }
        return result.takeIf { it.isNotEmpty() }
    }

    private fun parseSpectrumString(dataset: String?): List<SpectrumPoint>? {
        if (dataset.isNullOrBlank()) return null
        val points = dataset.split(',')
            .mapNotNull { pair ->
                val parts = pair.split('=')
                if (parts.size != 2) return@mapNotNull null
                val freq = parts[0].toDoubleOrNull()?.div(1000.0)
                val sig = parts[1].toDoubleOrNull()
                if (freq != null && sig != null) SpectrumPoint(freq, sig) else null
            }
        return points.takeIf { it.isNotEmpty() }
    }

    private fun urlExists(url: String): Boolean {
        return try {
            client.newCall(
                Request.Builder()
                    .url(url)
                    .method("HEAD", null)
                    .build()
            ).execute().use { response ->
                response.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val COMMAND_THROTTLE_MS = 125L
        private const val TIMEOUT_MS = 10000L
        private const val TAG = "FmDxRepository"
        private const val REMOTE_LOGO_BASE = "https://tef.noobish.eu/logos"
        const val DEFAULT_LOGO_URL = "$REMOTE_LOGO_BASE/default-logo.png"
        private const val LOGO_PATH = "logos"
        private val LOGO_SANITIZE_REGEX = Regex("[/\\-*+:.,§%&\"!?|><=)(\\[\\]´`'~#\\s]")
    }

    private fun logDebug(message: String, throwable: Throwable? = null) {
        if (!BuildConfig.DEBUG) return
        if (throwable != null) {
            Log.d(TAG, message, throwable)
        } else {
            Log.d(TAG, message)
        }
    }
}

class ControlConnection internal constructor(
    private val webSocket: WebSocket,
    private val channel: Channel<String>
) {
    fun send(command: String) {
        channel.trySend(command)
    }

    fun close() {
        channel.close()
        webSocket.close(1000, null)
    }
}

class PluginConnection internal constructor(
    private val webSocket: WebSocket
) {
    fun requestSpectrumScan() {
        val payload = """{"type":"spectrum-graph","action":"scan","value":{"status":"scan"}}"""
        webSocket.send(payload)
    }

    fun close() {
        webSocket.close(1000, null)
    }
}

fun buildWebSocketUrl(url: String, vararg pathSegments: String): String {
    val httpUrl = url.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid server URL")
    val builder = httpUrl.newBuilder()
    pathSegments.forEach { segment ->
        val trimmed = segment.trim('/')
        if (trimmed.isNotEmpty()) {
            builder.addPathSegment(trimmed)
        }
    }
    val built = builder.build()
    val httpString = built.toString()
    val normalized = if (pathSegments.isEmpty() && built.encodedPath == "/") {
        httpString.removeSuffix("/")
    } else {
        httpString
    }
    val httpScheme = built.scheme
    val wsScheme = if (httpScheme.equals("https", ignoreCase = true)) "wss" else "ws"
    return normalized.replaceFirst("$httpScheme://", "$wsScheme://")
}
