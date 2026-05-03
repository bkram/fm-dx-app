package org.fmdx.app.data

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.fmdx.app.BuildConfig
import org.fmdx.app.model.TunerInfo
import org.fmdx.app.model.TunerState
import org.fmdx.app.network.createFmDxOkHttpClient
import java.util.Locale

/**
 * Application-scoped owner of the fm-dx-webserver control connection and live tuner state.
 *
 * Lives for the lifetime of the process (held by [org.fmdx.app.FmDxApp]) so that audio playback
 * driven by Android Auto can run RDS / frequency / RT updates without the activity ever being
 * created.
 */
class FmDxSessionController(application: Application) {

    val okHttpClient = createFmDxOkHttpClient()
    val repository = FmDxRepository(okHttpClient)

    private val preferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val commandFlow = MutableSharedFlow<String>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var controlConnection: ControlConnection? = null
    private var commandJob: Job? = null
    private var stationLogoJob: Job? = null
    private var latencyJob: Job? = null
    private var connectJob: Job? = null
    private var lastLogoKey: String? = null

    private val _state = MutableStateFlow(
        FmDxSessionState(serverUrl = preferences.getString(KEY_LAST_SERVER_URL, null).orEmpty())
    )
    val state: StateFlow<FmDxSessionState> = _state.asStateFlow()

    fun updateServerUrl(url: String) {
        _state.update { it.copy(serverUrl = url) }
    }

    fun connect(rawUrl: String) {
        val current = _state.value
        if (current.isConnecting) return
        val sanitized = try {
            sanitizeUrl(rawUrl)
        } catch (ex: IllegalArgumentException) {
            _state.update { it.copy(errorMessage = ex.message ?: "Invalid URL") }
            return
        }
        if (current.isConnected && current.serverUrl == sanitized) return

        if (current.isConnected) {
            // Tear down whatever is currently active before switching servers.
            teardownConnections()
        }

        persistServerUrl(sanitized)
        lastLogoKey = null
        _state.update {
            it.copy(
                serverUrl = sanitized,
                isConnecting = true,
                errorMessage = null,
                statusMessage = "Connecting to $sanitized…"
            )
        }

        connectJob?.cancel()
        connectJob = scope.launch {
            try {
                logDebug("connect(): fetching tuner info for $sanitized")
                val info = repository.fetchTunerInfo(sanitized, BuildConfig.USER_AGENT)
                logDebug("connect(): tuner info -> name=${info.tunerName}")
                _state.update {
                    it.copy(
                        serverUrl = sanitized,
                        tunerInfo = info,
                        antennas = info.antennaNames,
                        isConnected = true,
                        isConnecting = false,
                        statusMessage = null,
                        errorMessage = null
                    )
                }
                startControlConnection(sanitized)
                startLatencyMonitor(sanitized)
            } catch (ex: Exception) {
                if (ex is CancellationException) throw ex
                logDebug("connect(): failed", ex)
                _state.update {
                    it.copy(
                        errorMessage = ex.message,
                        isConnected = false,
                        isConnecting = false,
                        statusMessage = ex.message ?: "Connection failed"
                    )
                }
            }
        }
    }

    fun disconnect() {
        connectJob?.cancel()
        connectJob = null
        teardownConnections()
        _state.update {
            it.copy(
                isConnected = false,
                isConnecting = false,
                statusMessage = null,
                errorMessage = null,
                tunerInfo = null,
                tunerState = null,
                antennas = emptyList(),
                stationLogoUrl = FmDxRepository.DEFAULT_LOGO_URL,
                serverLatencyMs = null,
                pendingFrequencyMHz = null
            )
        }
    }

    fun sendCommand(command: String) {
        commandFlow.tryEmit(command)
    }

    fun setPendingFrequency(mhz: Double?) {
        _state.update { it.copy(pendingFrequencyMHz = mhz) }
    }

    fun mutateTunerState(transform: (TunerState) -> TunerState) {
        _state.update { current ->
            val tuner = current.tunerState ?: return@update current
            current.copy(tunerState = transform(tuner))
        }
    }

    fun resetRds() {
        _state.update { current ->
            val tuner = current.tunerState ?: return@update current
            current.copy(tunerState = tuner.copy(ps = "", rt0 = "", rt1 = ""))
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun teardownConnections() {
        controlConnection?.close()
        controlConnection = null
        commandJob?.cancel()
        commandJob = null
        latencyJob?.cancel()
        latencyJob = null
        stationLogoJob?.cancel()
        stationLogoJob = null
        lastLogoKey = null
    }

    private fun startControlConnection(url: String) {
        logDebug("startControlConnection(): $url")
        controlConnection?.close()
        commandJob?.cancel()
        val connection = repository.connectControl(
            url,
            BuildConfig.USER_AGENT,
            scope,
            onState = { tuner ->
                _state.update { current ->
                    current.copy(
                        tunerState = tuner,
                        pendingFrequencyMHz = null,
                        antennas = if (current.antennas.isEmpty() && current.tunerInfo != null)
                            current.tunerInfo.antennaNames else current.antennas
                    )
                }
                scheduleLogoUpdate(url, tuner)
            },
            onClosed = {
                logDebug("control socket: closed")
                scope.launch { disconnect() }
            },
            onError = { error ->
                logDebug("control socket: error", error)
                _state.update {
                    it.copy(
                        errorMessage = error.message,
                        statusMessage = error.message
                    )
                }
            }
        )
        controlConnection = connection
        commandJob = scope.launch {
            commandFlow.collect { cmd ->
                logDebug("control socket: sending $cmd")
                connection.send(cmd)
            }
        }
    }

    private fun startLatencyMonitor(baseUrl: String) {
        latencyJob?.cancel()
        latencyJob = scope.launch {
            var ema = _state.value.serverLatencyMs
            while (isActive) {
                val latency = repository.measureServerLatency(baseUrl, BuildConfig.USER_AGENT)
                if (latency != null) {
                    val measurement = latency.toDouble()
                    ema = ema?.let { LATENCY_EMA_ALPHA * measurement + (1 - LATENCY_EMA_ALPHA) * it }
                        ?: measurement
                    _state.update { it.copy(serverLatencyMs = ema) }
                }
                delay(LATENCY_POLL_INTERVAL_MS)
            }
        }
    }

    private fun scheduleLogoUpdate(baseUrl: String, tuner: TunerState?) {
        val normalizedBase = baseUrl.trimEnd('/')
        val piCode = tuner?.pi?.takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT)
        val programName = tuner?.txInfo?.name?.takeIf { it.isNotBlank() }
            ?: tuner?.ps?.takeIf { it.isNotBlank() }
        val countryCode = tuner?.txInfo?.countryCode?.takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT)
        val key = listOf(normalizedBase, piCode ?: "", programName ?: "", countryCode ?: "")
            .joinToString("|")
        if (key == lastLogoKey) return
        lastLogoKey = key
        stationLogoJob?.cancel()
        stationLogoJob = scope.launch {
            val logoUrl = runCatching {
                repository.findStationLogo(
                    baseUrl = normalizedBase,
                    pi = piCode,
                    program = programName,
                    country = countryCode
                )
            }.getOrElse { ex ->
                if (ex is CancellationException) throw ex
                FmDxRepository.DEFAULT_LOGO_URL
            }
            _state.update { it.copy(stationLogoUrl = logoUrl) }
        }
    }

    private fun persistServerUrl(url: String) {
        val updatedHistory = buildRecentServerHistory(url)
        preferences.edit {
            putString(KEY_LAST_SERVER_URL, url)
            putString(KEY_RECENT_SERVER_URLS, updatedHistory.joinToString(separator = "\n"))
        }
    }

    fun loadRecentServerUrls(): List<String> {
        val raw = preferences.getString(KEY_RECENT_SERVER_URLS, null) ?: return emptyList()
        return raw
            .lineSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() }
            .map { entry -> runCatching { sanitizeUrl(entry) }.getOrElse { entry } }
            .distinct()
            .take(10)
            .toList()
    }

    fun removeRecentServerUrl(url: String) {
        val updated = loadRecentServerUrls().filterNot { it.equals(url, ignoreCase = true) }
        preferences.edit {
            putString(KEY_RECENT_SERVER_URLS, updated.joinToString(separator = "\n"))
        }
    }

    private fun buildRecentServerHistory(newUrl: String): List<String> {
        val existing = loadRecentServerUrls()
        return buildList {
            add(newUrl)
            existing.forEach { if (!it.equals(newUrl, ignoreCase = true)) add(it) }
        }.take(10)
    }

    private fun logDebug(message: String, throwable: Throwable? = null) {
        if (!BuildConfig.DEBUG) return
        if (throwable != null) Log.d(TAG, message, throwable) else Log.d(TAG, message)
    }

    companion object {
        private const val TAG = "FmDxSession"
        private const val PREFS_NAME = "fm_dx_prefs"
        const val KEY_LAST_SERVER_URL = "last_server_url"
        private const val KEY_RECENT_SERVER_URLS = "recent_server_urls"
        private const val LATENCY_POLL_INTERVAL_MS = 15_000L
        private const val LATENCY_EMA_ALPHA = 0.3

        fun sanitizeUrl(input: String): String {
            val trimmed = input.trim()
            if (trimmed.isEmpty()) {
                throw IllegalArgumentException("Server URL is required")
            }
            val withHttpScheme = when {
                trimmed.startsWith("http://", true) -> trimmed
                trimmed.startsWith("https://", true) -> trimmed
                trimmed.startsWith("ws://", true) -> "http://" + trimmed.substringAfter("://")
                trimmed.startsWith("wss://", true) -> "https://" + trimmed.substringAfter("://")
                else -> "http://$trimmed"
            }
            val httpUrl = withHttpScheme.toHttpUrlOrNull()
                ?: throw IllegalArgumentException("Invalid server URL")
            val rebuilt = httpUrl.newBuilder().build()
            val asString = rebuilt.toString()
            return if (rebuilt.encodedPath == "/") asString.trimEnd('/') else asString
        }
    }
}

data class FmDxSessionState(
    val serverUrl: String = "",
    val tunerInfo: TunerInfo? = null,
    val tunerState: TunerState? = null,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val antennas: List<String> = emptyList(),
    val stationLogoUrl: String? = FmDxRepository.DEFAULT_LOGO_URL,
    val serverLatencyMs: Double? = null,
    val pendingFrequencyMHz: Double? = null
)
