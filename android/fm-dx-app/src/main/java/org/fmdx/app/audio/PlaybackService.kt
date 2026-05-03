package org.fmdx.app.audio

import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.fmdx.app.BuildConfig
import org.fmdx.app.FmDxApp
import org.fmdx.app.MainActivity
import org.fmdx.app.data.FmDxSessionController
import org.fmdx.app.data.FmDxSessionState
import org.fmdx.app.network.createFmDxOkHttpClient

private const val PREFS_NAME = "fm_dx_prefs"
private const val KEY_NETWORK_BUFFER = "network_buffer"
private const val KEY_PLAYER_BUFFER = "player_buffer"

private data class BufferProfile(
    val networkChunks: Int,
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val playbackBufferMs: Int,
    val playbackAfterRebufferMs: Int
)

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val reconfigMutex = Mutex()

    private lateinit var preferences: SharedPreferences
    private lateinit var streamingClient: OkHttpClient
    private lateinit var sessionController: FmDxSessionController

    private var mediaSession: MediaSession? = null
    private var currentProfile: BufferProfile? = null
    private var metadataJob: Job? = null

    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_NETWORK_BUFFER || key == KEY_PLAYER_BUFFER) {
                serviceScope.launch { applyLatestSettings() }
            }
        }

    override fun onCreate() {
        super.onCreate()
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        streamingClient = createFmDxOkHttpClient()
        sessionController = (application as FmDxApp).sessionController
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)

        val initialProfile = manualProfile(
            preferences.getInt(KEY_NETWORK_BUFFER, DEFAULT_NETWORK_BUFFER_CHUNKS),
            preferences.getInt(KEY_PLAYER_BUFFER, DEFAULT_PLAYER_BUFFER_MS)
        )
        val player = buildPlayer(initialProfile)
        currentProfile = initialProfile

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(PlaybackCallback())
            .setSessionActivity(pendingIntent)
            .build()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this).build().apply {
            setSmallIcon(org.fmdx.app.R.drawable.ic_launcher_monochrome)
        }
        setMediaNotificationProvider(notificationProvider)

        attachPlayerListeners(player)
        startMetadataPump()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.player?.let { player ->
            if (!player.playWhenReady || player.mediaItemCount == 0) {
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        metadataJob?.cancel()
        metadataJob = null
        mediaSession?.run {
            val player = this.player
            player.release()
            release()
        }
        mediaSession = null
        serviceScope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    private fun attachPlayerListeners(player: ExoPlayer) {
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val newId = mediaItem?.mediaId.orEmpty()
                if (newId.startsWith("http://", true) || newId.startsWith("https://", true)) {
                    sessionController.connect(newId)
                }
            }
        })
    }

    private fun startMetadataPump() {
        metadataJob?.cancel()
        metadataJob = serviceScope.launch {
            sessionController.state
                .map { it.toMetadataKey() }
                .distinctUntilChanged()
                .collect { _ ->
                    applyLatestMetadata()
                }
        }
    }

    private fun applyLatestMetadata() {
        val player = mediaSession?.player ?: return
        if (player.mediaItemCount == 0) return
        val current = player.getMediaItemAt(0)
        val state = sessionController.state.value
        if (state.serverUrl.isBlank() || current.mediaId != state.serverUrl) return
        val fields = nowPlayingFieldsFor(this, state.tunerInfo, state.tunerState, state.stationLogoUrl)
        val updated = current.buildUpon()
            .setMediaMetadata(mediaMetadataFor(fields))
            .build()
        player.replaceMediaItem(0, updated)
    }

    private suspend fun applyLatestSettings() {
        reconfigMutex.withLock {
            val manualProfile = manualProfile(
                preferences.getInt(KEY_NETWORK_BUFFER, DEFAULT_NETWORK_BUFFER_CHUNKS),
                preferences.getInt(KEY_PLAYER_BUFFER, DEFAULT_PLAYER_BUFFER_MS)
            )
            if (currentProfile != manualProfile) {
                recreatePlayerWithProfile(manualProfile)
            }
        }
    }

    private suspend fun recreatePlayerWithProfile(profile: BufferProfile) {
        withContext(Dispatchers.Main) {
            val existingSession = mediaSession ?: return@withContext
            val oldPlayer = existingSession.player as? ExoPlayer ?: return@withContext
            val mediaItems = MutableList(oldPlayer.mediaItemCount) { index ->
                oldPlayer.getMediaItemAt(index)
            }
            val currentIndex = oldPlayer.currentMediaItemIndex
            val positionMs = oldPlayer.currentPosition
            val wasPlaying = oldPlayer.playWhenReady

            val newPlayer = buildPlayer(profile)
            newPlayer.setMediaItems(mediaItems, currentIndex, positionMs)
            newPlayer.prepare()
            newPlayer.playWhenReady = wasPlaying

            existingSession.player = newPlayer
            attachPlayerListeners(newPlayer)
            oldPlayer.release()

            if (wasPlaying) {
                newPlayer.play()
            }

            currentProfile = profile
        }
    }

    private fun buildPlayer(profile: BufferProfile): ExoPlayer {
        val mediaSourceFactory =
            WebSocketMediaSourceFactory(streamingClient, BuildConfig.USER_AGENT, profile.networkChunks)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                profile.minBufferMs,
                profile.maxBufferMs,
                profile.playbackBufferMs,
                profile.playbackAfterRebufferMs
            )
            .build()
        return ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
    }

    private fun manualProfile(networkChunks: Int, playerBufferMs: Int): BufferProfile {
        val safeChunks = networkChunks.coerceIn(
            DEFAULT_NETWORK_BUFFER_CHUNKS,
            MAX_NETWORK_BUFFER_CHUNKS
        )
        val base = playerBufferMs.coerceAtLeast(DEFAULT_PLAYER_BUFFER_MS)
        val maxBuffer = (base * 2).coerceAtLeast(base + 300)
        val playback = (base / 2).coerceAtLeast(250)
        return BufferProfile(
            networkChunks = safeChunks,
            minBufferMs = base,
            maxBufferMs = maxBuffer,
            playbackBufferMs = playback,
            playbackAfterRebufferMs = base
        )
    }

    private inner class PlaybackCallback : MediaSession.Callback {
        @Suppress("OVERRIDE_DEPRECATION")
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            // Return last items so a reconnect (e.g. Bluetooth resume) sees the right card,
            // but do NOT auto-start playback. The user has to tap explicitly.
            val player = mediaSession.player
            val mediaItems = mutableListOf<MediaItem>()
            for (i in 0 until player.mediaItemCount) {
                mediaItems.add(player.getMediaItemAt(i))
            }
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    mediaItems,
                    player.currentMediaItemIndex,
                    player.currentPosition
                )
            )
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startWindowIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val player = mediaSession.player
            player.setMediaItems(mediaItems, startWindowIndex, startPositionMs)
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(mediaItems, startWindowIndex, startPositionMs)
            )
        }
    }
}

private fun FmDxSessionState.toMetadataKey(): String = listOf(
    serverUrl,
    tunerInfo?.tunerName.orEmpty(),
    tunerState?.ps.orEmpty(),
    tunerState?.rt0.orEmpty(),
    tunerState?.rt1.orEmpty(),
    tunerState?.freqMHz?.toString().orEmpty(),
    stationLogoUrl.orEmpty()
).joinToString("|")
