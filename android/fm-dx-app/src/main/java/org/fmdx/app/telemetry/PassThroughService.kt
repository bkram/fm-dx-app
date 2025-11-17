package org.fmdx.app.telemetry

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.fmdx.app.BuildConfig
import org.fmdx.app.MainActivity
import org.fmdx.app.R
import org.fmdx.app.data.ControlConnection
import org.fmdx.app.data.FmDxRepository
import org.fmdx.app.data.PluginConnection
import org.fmdx.app.data.PluginTelemetryEvent
import org.fmdx.app.network.createFmDxOkHttpClient

class PassThroughService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + serviceJob)
    private val okHttpClient: OkHttpClient by lazy { createFmDxOkHttpClient() }
    private val repository by lazy { FmDxRepository(okHttpClient) }
    private val telemetrySender by lazy { PassThroughTelemetrySender(this, serviceScope) }

    private var controlConnection: ControlConnection? = null
    private var pluginConnection: PluginConnection? = null
    private var restartJob: Job? = null
    private var currentServerUrl: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val enabled = intent?.getBooleanExtra(EXTRA_ENABLED, false) ?: false
        val serverUrl = intent?.getStringExtra(EXTRA_SERVER_URL)
        if (!enabled || serverUrl.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification(serverUrl))

        if (serverUrl != currentServerUrl) {
            currentServerUrl = serverUrl
            startConnections(serverUrl)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopConnections()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startConnections(url: String) {
        stopConnections()
        telemetrySender.setFeatureEnabled(true)
        telemetrySender.onConnected(url)
        controlConnection = repository.connectControl(
            baseUrl = url,
            userAgent = BuildConfig.USER_AGENT,
            scope = serviceScope,
            onState = { telemetrySender.updateTunerState(it) },
            onClosed = { scheduleRestart() },
            onError = { throwable ->
                Log.w(TAG, "control socket error", throwable)
                scheduleRestart()
            }
        )
        pluginConnection = repository.connectPlugin(
            baseUrl = url,
            userAgent = BuildConfig.USER_AGENT,
            onEvent = {},
            onTelemetryEvent = { event -> handleTelemetryEvent(event) }
        ) { throwable ->
            Log.w(TAG, "plugin socket error", throwable)
            scheduleRestart()
        }
    }

    private fun stopConnections() {
        restartJob?.cancel()
        restartJob = null
        controlConnection?.close()
        controlConnection = null
        pluginConnection?.close()
        pluginConnection = null
        telemetrySender.setFeatureEnabled(false)
        telemetrySender.onDisconnected()
    }

    private fun scheduleRestart() {
        if (currentServerUrl.isNullOrBlank()) return
        if (restartJob?.isActive == true) return
        restartJob = serviceScope.launch {
            delay(RESTART_DELAY_MS)
            currentServerUrl?.let { startConnections(it) }
        }
    }

    private fun handleTelemetryEvent(event: PluginTelemetryEvent) {
        when (event) {
            is PluginTelemetryEvent.Scanner ->
                telemetrySender.updateScannerScanState(event.scanValue)

            is PluginTelemetryEvent.Gps -> telemetrySender.handleGpsEvent(
                status = event.status,
                lat = event.lat,
                lon = event.lon,
                alt = event.alt,
                mode = event.mode
            )
        }
    }

    private fun buildNotification(serverUrl: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or compatMutableFlag()
        )
        val content = getString(R.string.pass_through_notification_message, serverUrl)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.pass_through_notification_title))
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.pass_through_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun compatMutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

    companion object {
        private const val TAG = "PassThroughService"
        private const val CHANNEL_ID = "pass_through_channel"
        private const val NOTIFICATION_ID = 42
        private const val EXTRA_SERVER_URL = "extra_server_url"
        private const val EXTRA_ENABLED = "extra_enabled"
        private const val RESTART_DELAY_MS = 3_000L

        fun start(context: Context, serverUrl: String) {
            val intent = Intent(context, PassThroughService::class.java).apply {
                putExtra(EXTRA_ENABLED, true)
                putExtra(EXTRA_SERVER_URL, serverUrl)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, PassThroughService::class.java)
            context.stopService(intent)
        }
    }
}
