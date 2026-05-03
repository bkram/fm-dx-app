package org.fmdx.app.telemetry

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.fmdx.app.model.TunerState
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PassThroughTelemetrySender(
    private val appContext: Context,
    private val scope: CoroutineScope
) {
    private val preferences: SharedPreferences =
        appContext.getSharedPreferences(GPS_PREFS, Context.MODE_PRIVATE)

    @Volatile
    private var udpSocket: DatagramSocket? = null

    @Volatile
    private var senderJob: Job? = null

    @Volatile
    private var staticQthJob: Job? = null

    @Volatile
    private var gpsWebViewHelper: GpsWebViewHelper? = null

    @Volatile
    private var featureEnabled = false

    @Volatile
    private var isConnected = false

    @Volatile
    private var currentServerUrl: String? = null

    @Volatile
    private var currentHostKey: String? = null

    @Volatile
    private var latestTunerState: TunerState? = null

    @Volatile
    private var scannerStatusFlag: String = "0"

    @Volatile
    private var lastGpsEventMs: Long = 0L

    @Volatile
    private var lastGpsLogMs: Long = 0L

    @Volatile
    private var staticQthResolved = false

    fun setFeatureEnabled(enabled: Boolean) {
        featureEnabled = enabled
        refreshSenderState()
    }

    fun onConnected(serverUrl: String) {
        cancelStaticQthProbe()
        currentServerUrl = serverUrl
        currentHostKey = buildHostKey(serverUrl)
        isConnected = true
        scannerStatusFlag = "0"
        lastGpsEventMs = 0L
        lastGpsLogMs = 0L
        staticQthResolved = false
        scheduleStaticQthProbe()
        refreshSenderState()
    }

    fun onDisconnected() {
        isConnected = false
        currentServerUrl = null
        currentHostKey = null
        latestTunerState = null
        cancelStaticQthProbe()
        stopGpsWebView()
        stopSenderLoop()
        GpsStore.update(GpsData("", "", "0", "2", 0L))
    }

    fun updateTunerState(state: TunerState?) {
        latestTunerState = state
    }

    fun updateScannerScanState(scanValue: String?) {
        val normalized = scanValue?.lowercase(Locale.US) ?: return
        val value = when {
            normalized.startsWith("on") -> "1"
            normalized.startsWith("off") -> "0"
            else -> return
        }
        scannerStatusFlag = value
    }

    fun handleGpsEvent(status: String?, lat: String?, lon: String?, alt: String?, mode: String?) {
        if (!status.equals("active", ignoreCase = true)) return
        val roundedLat = lat?.trim().takeUnless { it.isNullOrEmpty() }?.let(::round6) ?: ""
        val roundedLon = lon?.trim().takeUnless { it.isNullOrEmpty() }?.let(::round6) ?: ""
        val roundedAlt = alt?.trim().takeUnless { it.isNullOrEmpty() }?.let(::round1) ?: "0"
        val safeMode = mode?.trim().takeUnless { it.isNullOrEmpty() } ?: "2"
        val now = System.currentTimeMillis()
        lastGpsEventMs = now
        staticQthResolved = true
        GpsStore.update(GpsData(roundedLat, roundedLon, roundedAlt, safeMode, now))
        if (now - lastGpsLogMs > GPS_LOG_INTERVAL_MS) {
            lastGpsLogMs = now
            Log.i(
                TAG,
                "GPS update → lat=$roundedLat lon=$roundedLon alt=$roundedAlt mode=$safeMode"
            )
        }
        stopGpsWebView()
    }

    private fun refreshSenderState() {
        if (featureEnabled && isConnected) {
            startSenderLoop()
        } else {
            stopSenderLoop()
        }
    }

    private fun startSenderLoop() {
        if (senderJob != null) return
        val socket = udpSocket ?: try {
            DatagramSocket().also { udpSocket = it }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to open UDP socket", t)
            return
        }
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
        senderJob = scope.launch(Dispatchers.IO) {
            try {
                while (true) {
                    try {
                        val payload = buildPayload(dateFormat, timeFormat)
                        if (payload != null) {
                            sendUdp(payload)
                        }
                    } catch (t: Throwable) {
                        Log.w(TAG, "Failed to emit pass-through payload", t)
                    }
                    delay(1000L)
                }
            } finally {
                // Guarantee the socket gets closed if the loop exits via cancellation or any
                // unhandled error, not only via the explicit stopSenderLoop() path.
                if (udpSocket === socket) {
                    runCatching { socket.close() }
                    udpSocket = null
                }
            }
        }
    }

    private fun stopSenderLoop() {
        senderJob?.cancel()
        senderJob = null
        udpSocket?.close()
        udpSocket = null
    }

    private fun buildPayload(
        dateFormat: SimpleDateFormat,
        timeFormat: SimpleDateFormat
    ): String? {
        val tuner = latestTunerState ?: return null
        val freq = tuner.freqMHz ?: return null
        val server = currentServerUrl ?: return null

        val header = buildUdpHeader(server)
        val dateStr = dateFormat.format(Date())
        val timeStr = timeFormat.format(Date())
        val freqStr = String.format(Locale.US, "%.2f", freq)
        val pi = tuner.pi?.replace("?", "")?.trim().orEmpty()
        val sigDbuv = tuner.signalDbf?.minus(10.875)
        val signalStr = sigDbuv?.let { String.format(Locale.US, "%.1f dBµV", it) }.orEmpty()
        val stereoStatus = if (tuner.stereo) "1" else "0"
        val taStatus = if (tuner.ta) "1" else "0"
        val tpStatus = if (tuner.tp) "1" else "0"
        val pty = (tuner.pty ?: 0).let { if (it == 0) 32 else it }
        val ptyStr = pty.toString()
        val ecc = tuner.ecc.orEmpty()
        val stationName = tuner.txInfo?.name
            ?: tuner.ps?.takeIf { it.isNotBlank() }
            ?: ""
        val radioText = listOfNotNull(tuner.rt0, tuner.rt1)
            .joinToString(" ")
            .replace("\\s+".toRegex(), " ")
            .trim()
        val afStr = tuner.afList
            .mapNotNull { it.takeIf { value -> !value.isNaN() } }
            .sorted()
            .joinToString(";") { String.format(Locale.US, "%.1f", it / 1000.0) }

        val gpsFields = resolveGpsFields()

        val payloadColumns = listOf(
            header,
            scannerStatusFlag,
            dateStr,
            timeStr,
            "1",
            "",
            freqStr,
            pi,
            signalStr,
            stereoStatus,
            taStatus,
            tpStatus,
            "",
            ptyStr,
            ecc,
            "",
            stationName,
            radioText,
            afStr,
            "",
            ""
        )
        val base = payloadColumns.joinToString(",")
        val gpsSuffix = listOf(
            SERVER_TAG_DEFAULT,
            gpsFields.lat,
            gpsFields.lon,
            gpsFields.alt,
            gpsFields.mode
        ).joinToString(",")
        return "$base,$gpsSuffix"
    }

    private fun resolveGpsFields(): GpsFields {
        val latest = GpsStore.latest()
        val hostKey = currentHostKey
        val cached = if (hostKey != null) readQthPrefsForHost(hostKey) else QthSnapshot()
        val lat = if (latest.lat.isNotBlank()) latest.lat else round6(cached.lat)
        val lon = if (latest.lon.isNotBlank()) latest.lon else round6(cached.lon)
        val alt = if (latest.alt.isNotBlank()) latest.alt else round1(cached.alt.ifBlank { "0" })
        val mode = if (latest.mode.isNotBlank()) latest.mode else cached.mode.ifBlank { "2" }
        return GpsFields(lat, lon, alt, mode)
    }

    private fun sendUdp(message: String) {
        val socket = udpSocket ?: return
        val bytes = message.toByteArray(Charsets.UTF_8)
        val packet = DatagramPacket(bytes, bytes.size, InetAddress.getByName(UDP_HOST), UDP_PORT)
        socket.send(packet)
    }

    private fun scheduleStaticQthProbe() {
        cancelStaticQthProbe()
        val hostKey = currentHostKey ?: return
        staticQthJob = scope.launch {
            delay(STATIC_QTH_WAIT_MS)
            if (lastGpsEventMs == 0L && !staticQthResolved) {
                Log.w(TAG, "GPS fallback: probing static QTH via WebView…")
                startStaticQthWebView(hostKey)
            }
        }
    }

    private fun startStaticQthWebView(endpoint: String) {
        gpsWebViewHelper = GpsWebViewHelper(
            context = appContext,
            onLog = { Log.i(TAG, it) },
            onGps = { lat, lon, alt, mode ->
                if (!staticQthResolved) {
                    staticQthResolved = true
                    GpsStore.update(GpsData(lat, lon, alt, mode))
                }
            }
        ).also { helper ->
            helper.start(endpoint, watchMillis = 6000L)
        }
    }

    private fun cancelStaticQthProbe() {
        staticQthJob?.cancel()
        staticQthJob = null
    }

    private fun stopGpsWebView() {
        try {
            gpsWebViewHelper?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop GPS WebView", e)
        }
        gpsWebViewHelper = null
    }

    private fun readQthPrefsForHost(hostKey: String): QthSnapshot {
        val lat = preferences.getString("qth_lat_$hostKey", "") ?: ""
        val lon = preferences.getString("qth_lon_$hostKey", "") ?: ""
        val alt = preferences.getString("qth_alt_$hostKey", "0") ?: "0"
        val mode = preferences.getString("qth_mode_$hostKey", "2") ?: "2"
        return QthSnapshot(lat, lon, alt, mode)
    }

    private fun buildUdpHeader(serverUrl: String): String {
        val server = extractServerName(serverUrl)
        return "FMDX Connector,$server"
    }

    private fun extractServerName(raw: String): String {
        val httpUrl = raw.toHttpUrlOrNull()
        if (httpUrl != null) {
            return httpUrl.host.ifBlank { "unknown" }
        }
        var sanitized = raw.trim()
        sanitized = sanitized.removePrefix("ws://")
            .removePrefix("wss://")
            .removePrefix("http://")
            .removePrefix("https://")
        val slash = sanitized.indexOf('/')
        if (slash >= 0) sanitized = sanitized.substring(0, slash)
        return sanitized.ifBlank { "unknown" }
    }

    private fun buildHostKey(serverUrl: String): String? {
        val httpUrl = serverUrl.toHttpUrlOrNull() ?: return null
        val host = httpUrl.host.ifBlank { return null }
        val defaultPort = if (httpUrl.scheme.equals("https", true)) 443 else 80
        val portPart = if (httpUrl.port == defaultPort) "" else ":${httpUrl.port}"
        val path = httpUrl.encodedPath.trimEnd('/')
        val pathPart = if (path.isNotEmpty() && path != "/") path else ""
        return buildString {
            append(host)
            append(portPart)
            append(pathPart)
        }
    }

    companion object {
        private const val TAG = "PassThroughTelemetry"
        private const val GPS_PREFS = "gps_cache"
        private const val UDP_HOST = "127.0.0.1"
        private const val UDP_PORT = 9100
        private const val STATIC_QTH_WAIT_MS = 2000L
        private const val GPS_LOG_INTERVAL_MS = 5000L
        private const val SERVER_TAG_DEFAULT = "#0"
    }
}

private data class QthSnapshot(
    val lat: String = "",
    val lon: String = "",
    val alt: String = "0",
    val mode: String = "2"
)

private data class GpsFields(
    val lat: String,
    val lon: String,
    val alt: String,
    val mode: String
)

private fun round6(value: String): String =
    value.toDoubleOrNull()?.let { String.format(Locale.US, "%.6f", it) } ?: value

private fun round1(value: String): String =
    value.toDoubleOrNull()?.let { String.format(Locale.US, "%.1f", it) } ?: value
