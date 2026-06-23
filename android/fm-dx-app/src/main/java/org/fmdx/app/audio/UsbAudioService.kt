package org.fmdx.app.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.fmdx.app.FmDxApp
import org.fmdx.app.MainActivity
import org.fmdx.app.R

/**
 * Foreground service that keeps the USB-tuner audio monitor ([UsbAudioEngine]) running while the
 * screen is off. Microphone capture (which is how Android gates USB Audio Class input) is blocked
 * for background apps, so a `microphone` foreground service is required for sleep-proof playback.
 *
 * The notification mirrors the now-playing info shown on the fm-dx-webserver path (station / PS,
 * frequency and RadioText), updating live as RDS is decoded.
 */
class UsbAudioService : Service() {

    private val engine by lazy { UsbAudioEngine(this) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(getString(R.string.usb_audio_notification_title), null),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )
        if (!engine.start()) {
            // No USB audio input or permission missing — nothing to play.
            stopEngineAndSelf()
            return START_NOT_STICKY
        }
        observeNowPlaying()
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        engine.stop()
        super.onDestroy()
    }

    private fun observeNowPlaying() {
        val controller = (application as FmDxApp).sessionController
        scope.launch {
            controller.state.collectLatest { session ->
                val fields = nowPlayingFieldsFor(
                    this@UsbAudioService,
                    session.tunerInfo,
                    session.tunerState,
                    session.stationLogoUrl
                )
                val title = fields.station ?: fields.title
                val text = fields.artist ?: fields.subtitle
                ContextCompat.getSystemService(this@UsbAudioService, NotificationManager::class.java)
                    ?.notify(NOTIFICATION_ID, buildNotification(title, text))
            }
        }
    }

    private fun stopEngineAndSelf() {
        engine.stop()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.usb_audio_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    // No count badge on the launcher icon for the ongoing audio service.
                    setShowBadge(false)
                }
            )
        }
    }

    private fun buildNotification(title: String, text: String?): Notification {
        val contentIntent = androidx.core.app.PendingIntentCompat.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            0,
            false
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setNumber(0)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
            .setShowWhen(false)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "usb_audio_v2"
        private const val NOTIFICATION_ID = 42

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, UsbAudioService::class.java)
            )
        }

        fun stop(context: Context) {
            // stopService is permitted from the background; starting a service with a STOP action
            // is not (BackgroundServiceStartNotAllowedException on Android 12+).
            context.stopService(Intent(context, UsbAudioService::class.java))
        }
    }
}
