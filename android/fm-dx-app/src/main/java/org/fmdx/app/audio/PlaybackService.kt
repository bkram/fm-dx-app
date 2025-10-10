package org.fmdx.app.audio

import android.content.Intent
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.fmdx.app.BuildConfig

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
    private lateinit var client: OkHttpClient

    private var mediaSession: MediaSession? = null
    private var currentProfile: BufferProfile? = null

    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_NETWORK_BUFFER || key == KEY_PLAYER_BUFFER) {
                serviceScope.launch { applyLatestSettings() }
            }
        }

    override fun onCreate() {
        super.onCreate()
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        client = OkHttpClient.Builder().build()
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)

        val initialProfile = manualProfile(
            preferences.getInt(KEY_NETWORK_BUFFER, 2),
            preferences.getInt(KEY_PLAYER_BUFFER, 2000)
        )

        val player = buildPlayer(initialProfile)
        currentProfile = initialProfile

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(PlaybackCallback())
            .build()
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
        mediaSession?.run {
            val player = this.player
            player.release()
            release()
        }
        mediaSession = null
        serviceScope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    private suspend fun applyLatestSettings() {
        reconfigMutex.withLock {
            val manualProfile = manualProfile(
                preferences.getInt(KEY_NETWORK_BUFFER, 2),
                preferences.getInt(KEY_PLAYER_BUFFER, 2000)
            )
            if (currentProfile != manualProfile) {
                recreatePlayerWithProfile(manualProfile)
            }
        }
    }

    private suspend fun recreatePlayerWithProfile(profile: BufferProfile) {
        withContext(Dispatchers.Main) {
            val existingSession = mediaSession ?: return@withContext
            val oldPlayer = existingSession.player as ExoPlayer
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

            existingSession.setPlayer(newPlayer)
            oldPlayer.release()

            if (wasPlaying) {
                newPlayer.play()
            }

            currentProfile = profile
        }
    }

    private fun buildPlayer(profile: BufferProfile): ExoPlayer {
        val mediaSourceFactory =
            WebSocketMediaSourceFactory(client, BuildConfig.USER_AGENT, profile.networkChunks)
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
        val safeChunks = networkChunks.coerceIn(1, MAX_NETWORK_BUFFER_CHUNKS)
        val base = playerBufferMs.coerceAtLeast(MIN_PLAYER_BUFFER_MS)
        val minBuffer = base
        val maxBuffer = (base * 2).coerceAtLeast(minBuffer + 300)
        val playback = (base / 2).coerceAtLeast(250)
        val afterRebuffer = base
        return BufferProfile(
            networkChunks = safeChunks,
            minBufferMs = minBuffer,
            maxBufferMs = maxBuffer,
            playbackBufferMs = playback,
            playbackAfterRebufferMs = afterRebuffer
        )
    }

    private inner class PlaybackCallback : MediaSession.Callback {
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val player = mediaSession.player
            player.play()
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
                MediaSession.MediaItemsWithStartPosition(
                    mediaItems,
                    startWindowIndex,
                    startPositionMs
                )
            )
        }
    }

}
