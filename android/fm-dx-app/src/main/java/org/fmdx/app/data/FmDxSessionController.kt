package org.fmdx.app.data

import android.app.Application
import android.content.Context
import android.hardware.usb.UsbManager
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
import org.fmdx.app.data.tuner.TunerControlTransport
import org.fmdx.app.data.tuner.XdrCommands
import org.fmdx.app.data.usb.UsbTunerDiscovery
import org.fmdx.app.data.usb.UsbTunerTransport
import org.fmdx.app.model.TunerInfo
import org.fmdx.app.model.TunerState
import org.fmdx.app.network.createFmDxOkHttpClient
import java.io.IOException
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

    private val appContext = application.applicationContext
    private val preferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val commandFlow = MutableSharedFlow<String>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var controlConnection: ControlConnection? = null
    private var transport: TunerControlTransport? = null
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

    /**
     * Connect to a directly attached USB FM-DX Tuner. Picks the first recognised USB-serial device,
     * requests permission if needed, and speaks the XDR line protocol over CDC-ACM.
     */
    fun connectUsb() {
        if (_state.value.isConnecting) return
        val usbManager = appContext.getSystemService(Context.USB_SERVICE) as? UsbManager
        val device = usbManager?.let { UsbTunerDiscovery.firstTuner(it) }
        if (usbManager == null || device == null) {
            _state.update {
                it.copy(
                    errorMessage = "No USB tuner detected",
                    statusMessage = "No USB tuner detected"
                )
            }
            return
        }
        if (_state.value.isConnected) teardownConnections()
        val label = "usb://${device.productName ?: device.deviceName}"
        beginConnecting(label, ConnectionType.USB, "Connecting to USB tuner…")
        connectJob?.cancel()
        connectJob = scope.launch {
            try {
                val granted = UsbTunerDiscovery.ensurePermission(appContext, usbManager, device)
                if (!granted) throw IOException("USB permission denied")
                val info = TunerInfo(
                    tunerName = device.productName ?: "USB Tuner",
                    tunerDescription = "Directly attached FM-DX Tuner",
                    antennaNames = emptyList(),
                    activeAntenna = 0
                )
                openTransport(UsbTunerTransport(usbManager, device, scope), info, label, ConnectionType.USB)
            } catch (ex: Exception) {
                if (ex is CancellationException) throw ex
                logDebug("connectUsb(): failed", ex)
                failConnection(ex)
            }
        }
    }

    private fun beginConnecting(label: String, type: ConnectionType, status: String) {
        lastLogoKey = null
        _state.update {
            it.copy(
                serverUrl = label,
                connectionType = type,
                isConnecting = true,
                errorMessage = null,
                statusMessage = status
            )
        }
    }

    private suspend fun openTransport(
        newTransport: TunerControlTransport,
        info: TunerInfo,
        label: String,
        type: ConnectionType
    ) {
        newTransport.open(object : TunerControlTransport.Callbacks {
            override fun onState(state: TunerState) {
                _state.update { current ->
                    // EQ / IMS / forced-mono / antenna are set by the user but never reported back
                    // by the raw line protocol, so the parser leaves them at defaults on every
                    // status line. Preserve the user's last choice instead of letting each ~66 ms
                    // status update clobber it.
                    val prev = current.tunerState
                    val merged = if (prev != null) {
                        state.copy(
                            eq = prev.eq,
                            ims = prev.ims,
                            stereoForced = prev.stereoForced,
                            antennaIndex = prev.antennaIndex ?: state.antennaIndex
                        )
                    } else {
                        state
                    }
                    current.copy(tunerState = merged, pendingFrequencyMHz = null)
                }
            }

            override fun onClosed() {
                logDebug("transport: closed")
                scope.launch { disconnect() }
            }

            override fun onError(error: Throwable) {
                logDebug("transport: error", error)
                _state.update { it.copy(errorMessage = error.message, statusMessage = error.message) }
            }
        })
        transport = newTransport
        _state.update {
            it.copy(
                serverUrl = label,
                connectionType = type,
                tunerInfo = info,
                antennas = info.antennaNames,
                isConnected = true,
                isConnecting = false,
                statusMessage = null,
                errorMessage = null
            )
        }
        commandJob?.cancel()
        commandJob = scope.launch {
            commandFlow.collect { cmd ->
                logDebug("transport: sending $cmd")
                newTransport.send(cmd)
                delay(COMMAND_THROTTLE_MS)
            }
        }

        // Tune the USB tuner on connect: the last frequency we used, or 87.5 MHz (the FM-DX
        // Tuner's own default) when there is no cached value yet.
        if (type == ConnectionType.USB) {
            val kHz = lastUsbFrequencyKHz() ?: DEFAULT_TUNE_KHZ
            scope.launch {
                delay(USB_RESTORE_TUNE_DELAY_MS)
                _state.update { it.copy(pendingFrequencyMHz = kHz / 1000.0) }
                sendCommand(XdrCommands.tune(kHz))
            }
        }
    }

    /** Persist the last frequency tuned on a USB tuner so it can be restored on reconnect. */
    fun cacheUsbFrequency(kHz: Int) {
        if (kHz <= 0) return
        preferences.edit { putInt(KEY_LAST_USB_FREQ_KHZ, kHz) }
    }

    private fun lastUsbFrequencyKHz(): Int? =
        preferences.getInt(KEY_LAST_USB_FREQ_KHZ, -1).takeIf { it > 0 }

    private fun failConnection(ex: Exception) {
        _state.update {
            it.copy(
                errorMessage = ex.message,
                isConnected = false,
                isConnecting = false,
                statusMessage = ex.message ?: "Connection failed"
            )
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
                pendingFrequencyMHz = null,
                connectionType = ConnectionType.SERVER
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
        transport?.close()
        transport = null
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
        // v2: the v1 key could be polluted by spurious picker tunes before that bug was fixed.
        private const val KEY_LAST_USB_FREQ_KHZ = "last_usb_freq_khz_v2"
        private const val USB_RESTORE_TUNE_DELAY_MS = 1200L
        private const val DEFAULT_TUNE_KHZ = 87500 // FM-DX Tuner firmware default (87.5 MHz)
        private const val LATENCY_POLL_INTERVAL_MS = 15_000L
        private const val LATENCY_EMA_ALPHA = 0.3
        private const val COMMAND_THROTTLE_MS = 125L

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

/** How the live session is connected to its tuner. */
enum class ConnectionType { SERVER, USB }

data class FmDxSessionState(
    val serverUrl: String = "",
    val connectionType: ConnectionType = ConnectionType.SERVER,
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
