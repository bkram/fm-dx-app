package org.fmdx.app

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.fmdx.app.audio.DEFAULT_NETWORK_BUFFER_CHUNKS
import org.fmdx.app.audio.DEFAULT_PLAYER_BUFFER_MS
import org.fmdx.app.audio.MAX_NETWORK_BUFFER_CHUNKS
import org.fmdx.app.audio.PlaybackService
import org.fmdx.app.data.ControlConnection
import org.fmdx.app.data.FmDxRepository
import org.fmdx.app.data.PluginConnection
import org.fmdx.app.data.PluginTelemetryEvent
import org.fmdx.app.data.SpectrumPluginEvent
import org.fmdx.app.model.SignalUnit
import org.fmdx.app.model.SpectrumPoint
import org.fmdx.app.model.TunerInfo
import org.fmdx.app.model.TunerState
import org.fmdx.app.network.createFmDxOkHttpClient
import org.fmdx.app.telemetry.PassThroughTelemetrySender
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val okHttpClient = createFmDxOkHttpClient()
    private val repository = FmDxRepository(okHttpClient)
    private val telemetrySender = PassThroughTelemetrySender(application, viewModelScope)

    private val preferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                KEY_NETWORK_BUFFER,
                KEY_PLAYER_BUFFER -> refreshBufferSettings()
            }
        }

    private val _uiState = MutableStateFlow(UiState(spectrum = baselineSpectrum()))
    val uiState: StateFlow<UiState> = _uiState

    private val commandFlow = MutableSharedFlow<String>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var controlConnection: ControlConnection? = null
    private var pluginConnection: PluginConnection? = null
    private var commandJob: Job? = null
    private var spectrumScanFallbackJob: Job? = null
    private var stationLogoJob: Job? = null
    private var lastLogoKey: String? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController? get() = controllerFuture?.let { if (it.isDone) it.get() else null }

    private val ptyProgrammes: List<String> =
        application.resources.getStringArray(R.array.pty_programmes_europe).toList()
    private val unknownPtyLabel: String = application.getString(R.string.rds_pty_unknown)

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        restorePersistedServerState()
        restorePersistedSettings()
        refreshBufferSettings()
        initializeMediaController()
    }

    private fun initializeMediaController() {
        val sessionToken = SessionToken(
            getApplication(),
            ComponentName(getApplication(), PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                controller?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _uiState.update { it.copy(audioPlaying = isPlaying) }
                    }
                })
            },
            MoreExecutors.directExecutor()
        )
    }


    fun updateServerUrl(url: String) {
        _uiState.update { it.copy(serverUrl = url) }
    }

    fun updateSettings(
        signalUnit: SignalUnit,
        networkBuffer: Int,
        playerBuffer: Int,
        restartAudioOnTune: Boolean,
        passThroughEnabled: Boolean
    ) {
        val clampedNetwork = networkBuffer.coerceIn(
            DEFAULT_NETWORK_BUFFER_CHUNKS,
            MAX_NETWORK_BUFFER_CHUNKS
        )
        val clampedPlayer = playerBuffer.coerceAtLeast(DEFAULT_PLAYER_BUFFER_MS)
        _uiState.update {
            it.copy(
                signalUnit = signalUnit,
                networkBuffer = clampedNetwork,
                playerBuffer = clampedPlayer,
                restartAudioOnTune = restartAudioOnTune,
                passThroughEnabled = passThroughEnabled
            )
        }
        telemetrySender.setFeatureEnabled(passThroughEnabled)
        persistSettings(
            signalUnit,
            clampedNetwork,
            clampedPlayer,
            restartAudioOnTune,
            passThroughEnabled
        )
    }

    fun connect() {
        val current = _uiState.value
        if (current.isConnecting) return
        val rawUrl = current.serverUrl
        if (rawUrl.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Server URL is required") }
            return
        }
        val sanitized = try {
            sanitizeUrl(rawUrl)
        } catch (ex: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = ex.message ?: "Invalid URL") }
            return
        }
        logDebug("connect(): sanitized server URL=$sanitized")
        persistServerUrl(sanitized)
        val connectingMessage = "Connecting to $sanitized…"
        lastLogoKey = null
        _uiState.update {
            it.copy(
                serverUrl = sanitized,
                isConnecting = true,
                errorMessage = null,
                statusMessage = connectingMessage
            )
        }
        viewModelScope.launch {
            try {
                logDebug("connect(): fetching tuner info")
                val info = repository.fetchTunerInfo(sanitized, BuildConfig.USER_AGENT)
                logDebug("connect(): tuner info resolved -> name=${info.tunerName}, description=${info.tunerDescription}")
                _uiState.update {
                    it.copy(
                        serverUrl = sanitized,
                        tunerInfo = info,
                        antennas = info.antennaNames,
                        tunerState = it.tunerState,
                        errorMessage = null,
                        isConnected = true,
                        isConnecting = false,
                        statusMessage = null
                    )
                }
                telemetrySender.onConnected(sanitized)
                startControlConnection(sanitized)
                startPluginConnection(sanitized)
                scheduleLogoUpdate(sanitized, _uiState.value.tunerState)
                refreshSpectrum(sanitized)
            } catch (ex: Exception) {
                logDebug("connect(): failed", ex)
                _uiState.update {
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
        telemetrySender.onDisconnected()
        controlConnection?.close()
        controlConnection = null
        pluginConnection?.close()
        pluginConnection = null
        commandJob?.cancel()
        commandJob = null
        spectrumScanFallbackJob?.cancel()
        spectrumScanFallbackJob = null
        stationLogoJob?.cancel()
        stationLogoJob = null
        lastLogoKey = null
        controller?.stop()
        controller?.clearMediaItems()
        _uiState.update { currentState ->
            currentState.copy(
                isConnected = false,
                isConnecting = false,
                audioPlaying = false,
                isScanning = false,
                statusMessage = "Disconnected",
                errorMessage = null,
                tunerInfo = null,
                tunerState = null,
                antennas = emptyList(),
                spectrum = baselineSpectrum(),
                stationLogoUrl = DEFAULT_LOGO_URL
            )
        }
    }

    fun toggleAudio() {
        val state = _uiState.value
        if (!state.isConnected) return

        val player = controller ?: return

        if (player.isPlaying) {
            player.pause()
        } else {
            refreshAudioStream(forcePlay = true)
        }
    }

    fun tuneToFrequency(valueMHz: Double) {
        val kHz = (valueMHz * 1000.0).roundToInt()
        val currentKHz = _uiState.value.tunerState?.freqKHz
        if (kHz == currentKHz) return

        _uiState.update { ui ->
            val snappedMHz = ((valueMHz * 10.0).roundToInt() / 10.0)
            ui.copy(
                pendingFrequencyMHz = snappedMHz,
                tunerState = ui.tunerState?.copy(
                    freqMHz = snappedMHz
                )
            )
        }

        sendCommand("T$kHz")
        resetRds()

        if (_uiState.value.restartAudioOnTune) {
            refreshAudioStream()
        }
    }

    private fun refreshAudioStream(forcePlay: Boolean = false) {
        val player = controller ?: return
        val wasPlaying = player.isPlaying
        val state = _uiState.value

        val mediaItem = MediaItem.Builder()
            .setMediaId(state.serverUrl)
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        if (wasPlaying || forcePlay) {
            player.play()
        }
    }

    fun toggleIms() {
        val tunerState = _uiState.value.tunerState ?: return
        val newImsEnabled = !tunerState.ims
        val eqBit = if (tunerState.eq) 1 else 0
        val imsBit = if (newImsEnabled) 1 else 0
        sendCommand("G${eqBit}${imsBit}")
        _uiState.update { current ->
            val currentTunerState = current.tunerState ?: return@update current
            current.copy(tunerState = currentTunerState.copy(ims = newImsEnabled))
        }
    }

    fun toggleEq() {
        val tunerState = _uiState.value.tunerState ?: return
        val newEqEnabled = !tunerState.eq
        val eqBit = if (newEqEnabled) 1 else 0
        val imsBit = if (tunerState.ims) 1 else 0
        sendCommand("G${eqBit}${imsBit}")
        _uiState.update { current ->
            val currentTunerState = current.tunerState ?: return@update current
            current.copy(tunerState = currentTunerState.copy(eq = newEqEnabled))
        }
    }

    fun toggleStereoMode() {
        val currentState = _uiState.value
        val tunerState = currentState.tunerState ?: return
        val isCurrentlyForced = tunerState.stereoForced
        val command =
            if (isCurrentlyForced) "B0" else "B1" // B0 -> release to stereo, B1 -> force mono.
        sendCommand(command)
    }

    fun cycleAntenna() {
        val state = _uiState.value.tunerState ?: return
        val antennas = _uiState.value.antennas.takeIf { it.isNotEmpty() } ?: listOf("Default")
        val count = antennas.size
        val current = state.antennaIndex ?: 0
        val next = if (count > 0) (current + 1) % count else 0
        sendCommand("Z$next")
        _uiState.update { it.copy(tunerState = it.tunerState?.copy(antennaIndex = next)) }
    }

    fun requestSpectrumScan() {
        val url = _uiState.value.serverUrl
        val plugin = pluginConnection ?: return
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, statusMessage = null) }
            spectrumScanFallbackJob?.cancel()
            plugin.requestSpectrumScan()
            spectrumScanFallbackJob = launch {
                delay(SPECTRUM_SCAN_FALLBACK_MS)
                if (_uiState.value.isScanning) {
                    refreshSpectrum(url)
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            statusMessage = it.statusMessage ?: SPECTRUM_PLUGIN_UNAVAILABLE_MESSAGE
                        )
                    }
                }
            }
        }
    }

    private suspend fun refreshSpectrum(url: String) {
        val points = try {
            repository.fetchSpectrumData(url, BuildConfig.USER_AGENT)
        } catch (ex: Exception) {
            _uiState.update { it.copy(errorMessage = ex.message) }
            null
        }
        _uiState.update { state ->
            val spectrumPoints = when {
                points == null -> null
                points.isEmpty() -> emptyList()
                else -> ensureSpectrum(points)
            }
            if (spectrumPoints != null) {
                state.copy(
                    spectrum = spectrumPoints,
                    statusMessage = null
                )
            } else {
                state.copy(
                    statusMessage = SPECTRUM_PLUGIN_UNAVAILABLE_MESSAGE
                )
            }
        }
    }

    private fun startControlConnection(url: String) {
        logDebug("startControlConnection(): opening control socket")
        controlConnection?.close()
        commandJob?.cancel()
        val connection = repository.connectControl(
            url,
            BuildConfig.USER_AGENT,
            viewModelScope,
            onState = { state ->
                logDebug("control socket: state update freq=${state.freqKHz} users=${state.users}")
                _uiState.update {
                    it.copy(
                        tunerState = state,
                        pendingFrequencyMHz = null,
                        antennas = if (it.antennas.isEmpty() && it.tunerInfo != null) it.tunerInfo.antennaNames else it.antennas
                    )
                }
                telemetrySender.updateTunerState(state)
                scheduleLogoUpdate(url, state)
            },
            onClosed = {
                logDebug("control socket: closed")
                viewModelScope.launch { disconnect() }
            },
            onError = { error ->
                logDebug("control socket: error", error)
                _uiState.update {
                    it.copy(
                        errorMessage = error.message,
                        statusMessage = error.message
                    )
                }
            }
        )
        controlConnection = connection
        commandJob = viewModelScope.launch {
            commandFlow.collect { cmd ->
                logDebug("control socket: sending command=$cmd")
                connection.send(cmd)
            }
        }
    }

    private fun startPluginConnection(url: String) {
        logDebug("startPluginConnection(): opening plugin socket")
        pluginConnection?.close()
        pluginConnection = repository.connectPlugin(
            baseUrl = url,
            userAgent = BuildConfig.USER_AGENT,
            onEvent = { event -> handleSpectrumEvent(url, event) },
            onTelemetryEvent = { event -> handleTelemetryEvent(event) }
        ) { error ->
            logDebug("plugin socket: error", error)
            _uiState.update { it.copy(errorMessage = error.message) }
        }
    }

    private fun handleSpectrumEvent(baseUrl: String, event: SpectrumPluginEvent) {
        event.points?.let { points ->
            spectrumScanFallbackJob?.cancel()
            _uiState.update {
                it.copy(
                    spectrum = ensureSpectrum(points),
                    isScanning = false
                )
            }
        }
        event.status?.let { status ->
            val normalized = status.lowercase(Locale.ROOT)
            when {
                normalized.contains("scan") || normalized.contains("busy") -> {
                    _uiState.update { it.copy(isScanning = true) }
                }

                normalized.contains("error") -> {
                    spectrumScanFallbackJob?.cancel()
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            errorMessage = status,
                            statusMessage = status
                        )
                    }
                }

                normalized.contains("done") || normalized.contains("ready") ||
                        normalized.contains("complete") || normalized.contains("idle") -> {
                    spectrumScanFallbackJob?.cancel()
                    _uiState.update { it.copy(isScanning = false) }
                    if (event.points == null && baseUrl.isNotBlank()) {
                        viewModelScope.launch { refreshSpectrum(baseUrl) }
                    }
                }
            }
        }
    }

    private fun handleTelemetryEvent(event: PluginTelemetryEvent) {
        when (event) {
            is PluginTelemetryEvent.Scanner -> {
                if (event.status.equals("response", ignoreCase = true)) {
                    telemetrySender.updateScannerScanState(event.scanValue)
                }
            }

            is PluginTelemetryEvent.Gps -> {
                telemetrySender.handleGpsEvent(
                    status = event.status,
                    lat = event.lat,
                    lon = event.lon,
                    alt = event.alt,
                    mode = event.mode
                )
            }
        }
    }

    private fun sendCommand(command: String) {
        commandFlow.tryEmit(command)
    }

    private fun resetRds() {
        val state = _uiState.value.tunerState ?: return
        _uiState.update {
            it.copy(
                tunerState = state.copy(
                    ps = "",
                    rt0 = "",
                    rt1 = ""
                )
            )
        }
    }

    override fun onCleared() {
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        disconnect()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }

    fun formatSignal(state: TunerState?, unit: SignalUnit): String {
        val signal = state?.signalDbf ?: return "--"
        val value = when (unit) {
            SignalUnit.DBF -> signal
            SignalUnit.DBUV -> signal - 11.25
            SignalUnit.DBM -> signal - 120
        }
        return String.format(Locale.US, "%.1f %s", value, unit.displayName)
    }

    fun currentPty(state: TunerState?): String {
        val display = state?.ptyDisplay(ptyProgrammes, unknownPtyLabel)?.trim() ?: ""
        if (display.isBlank()) return ""
        val normalized = display.substringAfter('/')
        val noPtyLabel = ptyProgrammes.firstOrNull().orEmpty()
        return if (display.startsWith("0/", ignoreCase = true) &&
            (normalized.equals(noPtyLabel, ignoreCase = true) ||
                    normalized.equals(unknownPtyLabel, ignoreCase = true))
        ) {
            ""
        } else {
            display
        }
    }

    fun antennaLabel(): String {
        val antennas = _uiState.value.antennas
        val index = _uiState.value.tunerState?.antennaIndex ?: 0
        return if (antennas.isNotEmpty() && index in antennas.indices) antennas[index] else "Default"
    }

    private fun sanitizeUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Server URL is required")
        }

        val withHttpScheme = when {
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith(
                "ws://",
                ignoreCase = true
            ) -> "http://" + trimmed.substringAfter("://")

            trimmed.startsWith(
                "wss://",
                ignoreCase = true
            ) -> "https://" + trimmed.substringAfter("://")

            else -> "http://$trimmed"
        }

        val httpUrl = withHttpScheme.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Invalid server URL")

        val rebuilt = httpUrl.newBuilder().build()
        val asString = rebuilt.toString()
        val sanitized = if (rebuilt.encodedPath == "/") {
            asString.trimEnd('/')
        } else {
            asString
        }
        logDebug("sanitizeUrl(): input=${'$'}input resolved=${'$'}sanitized")
        return sanitized
    }

    private fun ensureSpectrum(points: List<SpectrumPoint>): List<SpectrumPoint> {
        return points.ifEmpty { baselineSpectrum() }
    }

    private fun scheduleLogoUpdate(baseUrl: String, state: TunerState?) {
        val normalizedBase = baseUrl.trimEnd('/')
        val piCode = state?.pi?.takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT)
        val programName = state?.txInfo?.name?.takeIf { it.isNotBlank() }
            ?: state?.ps?.takeIf { it.isNotBlank() }
        val countryCode =
            state?.txInfo?.countryCode?.takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT)
        val key =
            listOf(normalizedBase, piCode ?: "", programName ?: "", countryCode ?: "").joinToString(
                "|"
            )
        if (key == lastLogoKey) return
        lastLogoKey = key
        stationLogoJob?.cancel()
        stationLogoJob = viewModelScope.launch {
            val logoUrl = runCatching {
                repository.findStationLogo(
                    baseUrl = normalizedBase,
                    pi = piCode,
                    program = programName,
                    country = countryCode
                )
            }.getOrElse { throwable ->
                if (throwable is CancellationException) throw throwable
                DEFAULT_LOGO_URL
            }
            _uiState.update { it.copy(stationLogoUrl = logoUrl) }
        }
    }

    companion object {
        private const val PREFS_NAME = "fm_dx_prefs"
        private const val KEY_LAST_SERVER_URL = "last_server_url"
        private const val KEY_SIGNAL_UNIT = "signal_unit"
        private const val KEY_NETWORK_BUFFER = "network_buffer"
        private const val KEY_PLAYER_BUFFER = "player_buffer"
        private const val KEY_RESTART_AUDIO_ON_TUNE = "restart_audio_on_tune"
        private const val KEY_RECENT_SERVER_URLS = "recent_server_urls"
        private const val KEY_PASS_THROUGH_ENABLED = "pass_through_enabled"
        private const val TAG = "MainViewModel"
        private const val SPECTRUM_SCAN_FALLBACK_MS = 8000L
        private const val SPECTRUM_PLUGIN_UNAVAILABLE_MESSAGE =
            "Spectrum data unavailable on this server."
        const val DEFAULT_LOGO_URL = FmDxRepository.DEFAULT_LOGO_URL

        fun baselineSpectrum(): List<SpectrumPoint> {
            val list = mutableListOf<SpectrumPoint>()
            var freq = 83.0
            while (freq <= 108.0 + 1e-6) {
                list += SpectrumPoint(freq, 0.0)
                freq += 0.05
            }
            return list
        }
    }

    private fun persistServerUrl(url: String) {
        val updatedHistory = buildRecentServerHistory(url)
        preferences.edit {
            putString(KEY_LAST_SERVER_URL, url)
            putString(KEY_RECENT_SERVER_URLS, updatedHistory.joinToString(separator = "\n"))
        }
        _uiState.update { it.copy(recentServerUrls = updatedHistory) }
    }

    private fun restorePersistedServerState() {
        val recent = loadRecentServerUrls()
        val persisted = preferences.getString(KEY_LAST_SERVER_URL, null)
        val sanitized = persisted?.let { runCatching { sanitizeUrl(it) }.getOrNull() } ?: persisted
        _uiState.update {
            it.copy(
                serverUrl = sanitized ?: it.serverUrl,
                recentServerUrls = recent
            )
        }
    }

    private fun loadRecentServerUrls(): List<String> {
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

    private fun buildRecentServerHistory(newUrl: String): List<String> {
        val existing = loadRecentServerUrls()
        val result = buildList {
            add(newUrl)
            existing.forEach { if (!it.equals(newUrl, ignoreCase = true)) add(it) }
        }
        return result.take(10)
    }

    private fun persistSettings(
        signalUnit: SignalUnit,
        networkBuffer: Int,
        playerBuffer: Int,
        restartAudioOnTune: Boolean,
        passThroughEnabled: Boolean
    ) {
        preferences.edit {
            putString(KEY_SIGNAL_UNIT, signalUnit.name)
            putInt(KEY_NETWORK_BUFFER, networkBuffer)
            putInt(KEY_PLAYER_BUFFER, playerBuffer)
            putBoolean(KEY_RESTART_AUDIO_ON_TUNE, restartAudioOnTune)
            putBoolean(KEY_PASS_THROUGH_ENABLED, passThroughEnabled)
        }
    }

    private fun restorePersistedSettings() {
        val signalUnitName = preferences.getString(KEY_SIGNAL_UNIT, SignalUnit.DBF.name)
        val signalUnit =
            SignalUnit.entries.firstOrNull { it.name == signalUnitName } ?: SignalUnit.DBF
        val persistedNetworkBuffer =
            preferences.getInt(KEY_NETWORK_BUFFER, DEFAULT_NETWORK_BUFFER_CHUNKS)
        val persistedPlayerBuffer =
            preferences.getInt(KEY_PLAYER_BUFFER, DEFAULT_PLAYER_BUFFER_MS)
        val networkBuffer = persistedNetworkBuffer.coerceIn(
            DEFAULT_NETWORK_BUFFER_CHUNKS,
            MAX_NETWORK_BUFFER_CHUNKS
        )
        val playerBuffer = persistedPlayerBuffer.coerceAtLeast(DEFAULT_PLAYER_BUFFER_MS)
        val restartAudioOnTune = preferences.getBoolean(KEY_RESTART_AUDIO_ON_TUNE, false)
        val passThroughEnabled = preferences.getBoolean(KEY_PASS_THROUGH_ENABLED, false)
        _uiState.update {
            it.copy(
                signalUnit = signalUnit,
                networkBuffer = networkBuffer,
                playerBuffer = playerBuffer,
                restartAudioOnTune = restartAudioOnTune,
                passThroughEnabled = passThroughEnabled
            )
        }
        telemetrySender.setFeatureEnabled(passThroughEnabled)
    }

    private fun refreshBufferSettings() {
        val networkBuffer = preferences.getInt(KEY_NETWORK_BUFFER, _uiState.value.networkBuffer)
            .coerceIn(DEFAULT_NETWORK_BUFFER_CHUNKS, MAX_NETWORK_BUFFER_CHUNKS)
        val playerBuffer = preferences.getInt(KEY_PLAYER_BUFFER, _uiState.value.playerBuffer)
            .coerceAtLeast(DEFAULT_PLAYER_BUFFER_MS)
        _uiState.update {
            it.copy(
                networkBuffer = networkBuffer,
                playerBuffer = playerBuffer
            )
        }
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

data class UiState(
    val serverUrl: String = "",
    val recentServerUrls: List<String> = emptyList(),
    val tunerInfo: TunerInfo? = null,
    val tunerState: TunerState? = null,
    val audioPlaying: Boolean = false,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val antennas: List<String> = emptyList(),
    val spectrum: List<SpectrumPoint> = emptyList(),
    val isScanning: Boolean = false,
    val errorMessage: String? = null,
    val signalUnit: SignalUnit = SignalUnit.DBF,
    val networkBuffer: Int = DEFAULT_NETWORK_BUFFER_CHUNKS,
    val playerBuffer: Int = DEFAULT_PLAYER_BUFFER_MS,
    val restartAudioOnTune: Boolean = false,
    val passThroughEnabled: Boolean = false,
    val statusMessage: String? = null,
    val pendingFrequencyMHz: Double? = null,
    val stationLogoUrl: String? = MainViewModel.DEFAULT_LOGO_URL
)
