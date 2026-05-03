package org.fmdx.app

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.fmdx.app.audio.DEFAULT_NETWORK_BUFFER_CHUNKS
import org.fmdx.app.audio.DEFAULT_PLAYER_BUFFER_MS
import org.fmdx.app.audio.MAX_NETWORK_BUFFER_CHUNKS
import org.fmdx.app.audio.PlaybackService
import org.fmdx.app.audio.buildMediaItemForServer
import org.fmdx.app.data.FmDxSessionController
import org.fmdx.app.data.PluginConnection
import org.fmdx.app.data.SpectrumPluginEvent
import org.fmdx.app.model.PublicServer
import org.fmdx.app.model.SignalUnit
import org.fmdx.app.model.SpectrumPoint
import org.fmdx.app.model.TunerInfo
import org.fmdx.app.model.TunerState
import org.fmdx.app.telemetry.PassThroughService
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as FmDxApp
    private val sessionController = app.sessionController
    private val repository = sessionController.repository

    private val preferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                KEY_NETWORK_BUFFER,
                KEY_PLAYER_BUFFER -> refreshBufferSettings()
            }
        }

    private val _localState = MutableStateFlow(
        LocalUiState(
            spectrum = baselineSpectrum(),
            recentServerUrls = sessionController.loadRecentServerUrls()
        )
    )

    val uiState: StateFlow<UiState> =
        combine(sessionController.state, _localState) { session, local ->
            UiState(
                serverUrl = local.pendingServerUrl ?: session.serverUrl,
                recentServerUrls = local.recentServerUrls,
                tunerInfo = session.tunerInfo,
                tunerState = session.tunerState,
                audioPlaying = local.audioPlaying,
                isConnected = session.isConnected,
                isConnecting = session.isConnecting,
                antennas = session.antennas,
                spectrum = local.spectrum,
                isScanning = local.isScanning,
                errorMessage = session.errorMessage ?: local.errorMessage,
                signalUnit = local.signalUnit,
                networkBuffer = local.networkBuffer,
                playerBuffer = local.playerBuffer,
                restartAudioOnTune = local.restartAudioOnTune,
                passThroughEnabled = local.passThroughEnabled,
                statusMessage = session.statusMessage ?: local.statusMessage,
                pendingFrequencyMHz = session.pendingFrequencyMHz,
                stationLogoUrl = session.stationLogoUrl,
                serverLatencyMs = session.serverLatencyMs,
                publicServerPickerState = local.publicServerPickerState,
                isSpectrumAvailable = local.isSpectrumAvailable
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UiState(spectrum = baselineSpectrum())
        )

    private var pluginConnection: PluginConnection? = null
    private var spectrumScanFallbackJob: Job? = null
    private var publicServerJob: Job? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController?
        get() = controllerFuture?.let { if (it.isDone) it.get() else null }

    private val ptyProgrammes: List<String> =
        application.resources.getStringArray(R.array.pty_programmes_europe).toList()
    private val unknownPtyLabel: String = application.getString(R.string.rds_pty_unknown)

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        restorePersistedSettings()
        refreshBufferSettings()
        initializeMediaController()
        observeSessionForSpectrum()
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
                        _localState.update { it.copy(audioPlaying = isPlaying) }
                    }
                })
            },
            ContextCompat.getMainExecutor(getApplication())
        )
    }

    private fun observeSessionForSpectrum() {
        viewModelScope.launch {
            var lastUrl: String? = null
            sessionController.state.collect { session ->
                if (session.isConnected && session.serverUrl != lastUrl) {
                    lastUrl = session.serverUrl
                    pluginConnection?.close()
                    pluginConnection = repository.connectPlugin(
                        baseUrl = session.serverUrl,
                        userAgent = BuildConfig.USER_AGENT,
                        onEvent = { event -> handleSpectrumEvent(session.serverUrl, event) }
                    ) { error ->
                        logDebug("plugin socket: error", error)
                        _localState.update { it.copy(errorMessage = error.message) }
                    }
                    refreshSpectrum(session.serverUrl)
                    updatePassThroughServiceState()
                } else if (!session.isConnected && lastUrl != null) {
                    lastUrl = null
                    pluginConnection?.close()
                    pluginConnection = null
                    spectrumScanFallbackJob?.cancel()
                    spectrumScanFallbackJob = null
                    _localState.update {
                        it.copy(
                            spectrum = baselineSpectrum(),
                            isScanning = false,
                            isSpectrumAvailable = false,
                            audioPlaying = false
                        )
                    }
                    controller?.stop()
                    controller?.clearMediaItems()
                    updatePassThroughServiceState()
                }
            }
        }
    }

    fun updateServerUrl(url: String) {
        _localState.update { it.copy(pendingServerUrl = url) }
        sessionController.updateServerUrl(url)
    }

    fun showPublicServerPicker() {
        val pickerState = _localState.value.publicServerPickerState
        if (!pickerState.isVisible) {
            updatePublicServerPicker { it.copy(isVisible = true) }
        }
        if (pickerState.servers.isEmpty()) {
            loadPublicServers(forceRefresh = false)
        }
    }

    fun hidePublicServerPicker() {
        updatePublicServerPicker { it.copy(isVisible = false, errorMessage = null) }
    }

    fun refreshPublicServerPicker() {
        loadPublicServers(forceRefresh = true)
    }

    fun updatePublicServerQuery(query: String) {
        updatePublicServerPicker { picker ->
            val trimmed = query.take(MAX_PUBLIC_SERVER_QUERY_LENGTH)
            picker.copy(
                query = trimmed,
                filteredServers = filterPublicServers(picker.servers, trimmed)
            )
        }
    }

    fun selectPublicServer(server: PublicServer) {
        updateServerUrl(server.url)
        updatePublicServerPicker { it.copy(isVisible = false) }
        // Auto-connect to the picked server.
        connect()
    }

    fun removeRecentServer(url: String) {
        sessionController.removeRecentServerUrl(url)
        _localState.update { it.copy(recentServerUrls = sessionController.loadRecentServerUrls()) }
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
        _localState.update {
            it.copy(
                signalUnit = signalUnit,
                networkBuffer = clampedNetwork,
                playerBuffer = clampedPlayer,
                restartAudioOnTune = restartAudioOnTune,
                passThroughEnabled = passThroughEnabled
            )
        }
        persistSettings(
            signalUnit,
            clampedNetwork,
            clampedPlayer,
            restartAudioOnTune,
            passThroughEnabled
        )
        updatePassThroughServiceState()
    }

    fun connect() {
        val url = uiState.value.serverUrl
        if (url.isBlank()) {
            _localState.update { it.copy(errorMessage = "Server URL is required") }
            return
        }
        sessionController.connect(url)
        viewModelScope.launch {
            // Refresh recents from prefs after the controller persists the new URL.
            _localState.update { it.copy(recentServerUrls = sessionController.loadRecentServerUrls()) }
        }
    }

    fun disconnect() {
        sessionController.disconnect()
    }

    fun toggleAudio() {
        val state = uiState.value
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
        val currentKHz = uiState.value.tunerState?.freqKHz
        if (kHz == currentKHz) return

        val snappedMHz = ((valueMHz * 10.0).roundToInt() / 10.0)
        sessionController.setPendingFrequency(snappedMHz)
        sessionController.mutateTunerState { it.copy(freqMHz = snappedMHz) }

        sessionController.sendCommand("T$kHz")
        sessionController.resetRds()

        if (uiState.value.restartAudioOnTune) {
            refreshAudioStream()
        }
    }

    private fun refreshAudioStream(forcePlay: Boolean = false) {
        val player = controller ?: return
        val state = uiState.value
        val mediaItem = buildMediaItemForServer(
            getApplication(),
            state.serverUrl,
            state.tunerInfo,
            state.tunerState,
            state.stationLogoUrl
        )
        val wasPlaying = player.isPlaying
        player.setMediaItem(mediaItem)
        player.prepare()
        if (wasPlaying || forcePlay) {
            player.play()
        }
    }

    fun toggleIms() {
        val tunerState = uiState.value.tunerState ?: return
        val newImsEnabled = !tunerState.ims
        val eqBit = if (tunerState.eq) 1 else 0
        val imsBit = if (newImsEnabled) 1 else 0
        sessionController.sendCommand("G${eqBit}${imsBit}")
        sessionController.mutateTunerState { it.copy(ims = newImsEnabled) }
    }

    fun toggleEq() {
        val tunerState = uiState.value.tunerState ?: return
        val newEqEnabled = !tunerState.eq
        val eqBit = if (newEqEnabled) 1 else 0
        val imsBit = if (tunerState.ims) 1 else 0
        sessionController.sendCommand("G${eqBit}${imsBit}")
        sessionController.mutateTunerState { it.copy(eq = newEqEnabled) }
    }

    fun toggleStereoMode() {
        val tunerState = uiState.value.tunerState ?: return
        val command = if (tunerState.stereoForced) "B0" else "B1"
        sessionController.sendCommand(command)
    }

    fun cycleAntenna() {
        val state = uiState.value.tunerState ?: return
        val antennas = uiState.value.antennas.takeIf { it.isNotEmpty() } ?: listOf("Default")
        val count = antennas.size
        val current = state.antennaIndex ?: 0
        val next = if (count > 0) (current + 1) % count else 0
        sessionController.sendCommand("Z$next")
        sessionController.mutateTunerState { it.copy(antennaIndex = next) }
    }

    fun requestSpectrumScan() {
        val url = uiState.value.serverUrl
        val plugin = pluginConnection ?: return
        if (url.isBlank()) return
        viewModelScope.launch {
            _localState.update { it.copy(isScanning = true, statusMessage = null) }
            spectrumScanFallbackJob?.cancel()
            plugin.requestSpectrumScan()
            spectrumScanFallbackJob = launch {
                delay(SPECTRUM_SCAN_FALLBACK_MS)
                if (_localState.value.isScanning) {
                    refreshSpectrum(url)
                    _localState.update { it.copy(isScanning = false) }
                }
            }
        }
    }

    private suspend fun refreshSpectrum(url: String) {
        val points = try {
            repository.fetchSpectrumData(url, BuildConfig.USER_AGENT)
        } catch (ex: Exception) {
            _localState.update { it.copy(errorMessage = ex.message) }
            null
        }
        _localState.update { state ->
            // Treat null (HTTP 404 / parse failure) AND empty as "plugin unavailable" so the tab
            // disappears completely on servers that don't have the spectrum-graph plugin installed.
            if (points.isNullOrEmpty()) {
                state.copy(
                    statusMessage = null,
                    isSpectrumAvailable = false
                )
            } else {
                state.copy(
                    spectrum = ensureSpectrum(points),
                    statusMessage = null,
                    isSpectrumAvailable = true
                )
            }
        }
    }

    private fun handleSpectrumEvent(baseUrl: String, event: SpectrumPluginEvent) {
        event.points?.let { points ->
            spectrumScanFallbackJob?.cancel()
            _localState.update {
                it.copy(
                    spectrum = ensureSpectrum(points),
                    isScanning = false,
                    isSpectrumAvailable = true
                )
            }
        }
        event.status?.let { status ->
            val normalized = status.lowercase(Locale.ROOT)
            // Order matters: check terminal states ("Scan complete" / "scan done") BEFORE the
            // generic "scan" / "busy" in-progress check, since the completion strings often
            // contain the word "scan" too.
            when {
                normalized.contains("done") || normalized.contains("ready") ||
                        normalized.contains("complete") || normalized.contains("idle") -> {
                    spectrumScanFallbackJob?.cancel()
                    _localState.update { it.copy(isScanning = false) }
                    if (event.points == null && baseUrl.isNotBlank()) {
                        viewModelScope.launch { refreshSpectrum(baseUrl) }
                    }
                }

                normalized.contains("error") -> {
                    spectrumScanFallbackJob?.cancel()
                    _localState.update {
                        it.copy(
                            isScanning = false,
                            errorMessage = status,
                            statusMessage = status
                        )
                    }
                }

                normalized.contains("scan") || normalized.contains("busy") -> {
                    _localState.update { it.copy(isScanning = true) }
                }
            }
        }
    }

    override fun onCleared() {
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        pluginConnection?.close()
        pluginConnection = null
        spectrumScanFallbackJob?.cancel()
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
        val antennas = uiState.value.antennas
        val index = uiState.value.tunerState?.antennaIndex ?: 0
        return if (antennas.isNotEmpty() && index in antennas.indices) antennas[index] else "Default"
    }

    private fun ensureSpectrum(points: List<SpectrumPoint>): List<SpectrumPoint> {
        return points.ifEmpty { baselineSpectrum() }
    }

    private fun updatePassThroughServiceState() {
        val state = uiState.value
        val url = state.serverUrl
        if (state.passThroughEnabled && state.isConnected && url.isNotBlank()) {
            PassThroughService.start(app, url)
        } else {
            PassThroughService.stop(app)
        }
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
        _localState.update {
            it.copy(
                signalUnit = signalUnit,
                networkBuffer = networkBuffer,
                playerBuffer = playerBuffer,
                restartAudioOnTune = restartAudioOnTune,
                passThroughEnabled = passThroughEnabled
            )
        }
    }

    private fun refreshBufferSettings() {
        val networkBuffer = preferences.getInt(KEY_NETWORK_BUFFER, _localState.value.networkBuffer)
            .coerceIn(DEFAULT_NETWORK_BUFFER_CHUNKS, MAX_NETWORK_BUFFER_CHUNKS)
        val playerBuffer = preferences.getInt(KEY_PLAYER_BUFFER, _localState.value.playerBuffer)
            .coerceAtLeast(DEFAULT_PLAYER_BUFFER_MS)
        _localState.update {
            it.copy(
                networkBuffer = networkBuffer,
                playerBuffer = playerBuffer
            )
        }
    }

    private fun logDebug(message: String, throwable: Throwable? = null) {
        if (!BuildConfig.DEBUG) return
        if (throwable != null) Log.d(TAG, message, throwable) else Log.d(TAG, message)
    }

    private fun loadPublicServers(forceRefresh: Boolean) {
        if (publicServerJob?.isActive == true) {
            if (!forceRefresh) return
            publicServerJob?.cancel()
        }
        publicServerJob = viewModelScope.launch {
            updatePublicServerPicker { it.copy(isLoading = true, errorMessage = null) }
            try {
                val servers =
                    repository.getPublicServers(BuildConfig.USER_AGENT, forceRefresh = forceRefresh)
                updatePublicServerPicker { picker ->
                    picker.copy(
                        servers = servers,
                        filteredServers = filterPublicServers(servers, picker.query),
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (ex: Exception) {
                logDebug("loadPublicServers(): failed", ex)
                updatePublicServerPicker { picker ->
                    picker.copy(
                        isLoading = false,
                        errorMessage = ex.message ?: "Unable to load public servers"
                    )
                }
            }
        }
    }

    private fun updatePublicServerPicker(
        transform: (PublicServerPickerState) -> PublicServerPickerState
    ) {
        _localState.update { state ->
            state.copy(publicServerPickerState = transform(state.publicServerPickerState))
        }
    }

    private fun filterPublicServers(
        servers: List<PublicServer>,
        query: String
    ): List<PublicServer> {
        if (query.isBlank()) return servers
        return servers.filter { server -> server.matchesQuery(query) }
    }

    companion object {
        private const val PREFS_NAME = "fm_dx_prefs"
        private const val KEY_SIGNAL_UNIT = "signal_unit"
        private const val KEY_NETWORK_BUFFER = "network_buffer"
        private const val KEY_PLAYER_BUFFER = "player_buffer"
        private const val KEY_RESTART_AUDIO_ON_TUNE = "restart_audio_on_tune"
        private const val KEY_PASS_THROUGH_ENABLED = "pass_through_enabled"
        private const val TAG = "MainViewModel"
        private const val SPECTRUM_SCAN_FALLBACK_MS = 8000L
        private const val SPECTRUM_PLUGIN_UNAVAILABLE_MESSAGE =
            "Spectrum data unavailable on this server."
        private const val MAX_PUBLIC_SERVER_QUERY_LENGTH = 80
        const val DEFAULT_LOGO_URL = org.fmdx.app.data.FmDxRepository.DEFAULT_LOGO_URL

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
}

private data class LocalUiState(
    val pendingServerUrl: String? = null,
    val recentServerUrls: List<String> = emptyList(),
    val audioPlaying: Boolean = false,
    val spectrum: List<SpectrumPoint> = emptyList(),
    val isScanning: Boolean = false,
    val errorMessage: String? = null,
    val signalUnit: SignalUnit = SignalUnit.DBF,
    val networkBuffer: Int = DEFAULT_NETWORK_BUFFER_CHUNKS,
    val playerBuffer: Int = DEFAULT_PLAYER_BUFFER_MS,
    val restartAudioOnTune: Boolean = false,
    val passThroughEnabled: Boolean = false,
    val statusMessage: String? = null,
    val publicServerPickerState: PublicServerPickerState = PublicServerPickerState(),
    val isSpectrumAvailable: Boolean = false
)

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
    val stationLogoUrl: String? = MainViewModel.DEFAULT_LOGO_URL,
    val serverLatencyMs: Double? = null,
    val publicServerPickerState: PublicServerPickerState = PublicServerPickerState(),
    val isSpectrumAvailable: Boolean = false
)

data class PublicServerPickerState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val query: String = "",
    val errorMessage: String? = null,
    val servers: List<PublicServer> = emptyList(),
    val filteredServers: List<PublicServer> = emptyList()
)
