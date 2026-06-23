package org.fmdx.app.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Captures the demodulated audio that a directly attached FM-DX Tuner exposes as a USB Audio Class
 * input, and renders it to the phone's current output. The tuner firmware streams 48 kHz / S16LE /
 * stereo (see the icecast reference in [[reference_xdr_usb_protocol]]).
 *
 * This is a simple real-time monitor (AudioRecord → AudioTrack on a dedicated thread). It runs in
 * the app process while the app is foreground; a microphone foreground service can be layered on
 * later for screen-off playback.
 */
class UsbAudioEngine(private val context: Context) {

    @Volatile
    private var running = false
    private var thread: Thread? = null
    private var record: AudioRecord? = null
    private var track: AudioTrack? = null

    fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun findUsbAudioInput(): AudioDeviceInfo? {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return null
        return audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).firstOrNull { device ->
            device.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
        }
    }

    val isRunning: Boolean get() = running

    /**
     * Start monitoring. Returns true once capture+playback are live. Caller must hold RECORD_AUDIO.
     */
    fun start(): Boolean {
        if (running) return true
        if (!hasRecordPermission()) {
            Log.w(TAG, "start(): RECORD_AUDIO not granted")
            return false
        }
        val usbInput = findUsbAudioInput()
        if (usbInput == null) {
            Log.w(TAG, "start(): no USB audio input device found")
            return false
        }

        val minIn = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, ENCODING)
        val minOut = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, ENCODING)
        if (minIn <= 0 || minOut <= 0) {
            Log.w(TAG, "start(): unsupported PCM format (minIn=$minIn minOut=$minOut)")
            return false
        }
        val bufferSize = maxOf(minIn, minOut, DEFAULT_BUFFER_BYTES)

        val recorder = buildRecorder(bufferSize)
        if (recorder == null) {
            Log.w(TAG, "start(): AudioRecord failed to initialise")
            return false
        }
        recorder.preferredDevice = usbInput

        val player = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(ENCODING)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        record = recorder
        track = player
        running = true
        recorder.startRecording()
        player.play()
        thread = Thread({ pump(recorder, player, bufferSize) }, "usb-audio-monitor").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
        Log.i(TAG, "USB audio monitor started (device=${usbInput.productName})")
        return true
    }

    fun stop() {
        running = false
        thread?.let { runCatching { it.join(500) } }
        thread = null
        record?.let { runCatching { it.stop() }; runCatching { it.release() } }
        record = null
        track?.let { runCatching { it.stop() }; runCatching { it.release() } }
        track = null
    }

    private fun pump(recorder: AudioRecord, player: AudioTrack, bufferSize: Int) {
        val buffer = ByteArray(bufferSize)
        while (running) {
            val read = recorder.read(buffer, 0, buffer.size)
            if (read > 0) {
                player.write(buffer, 0, read)
            } else if (read < 0) {
                Log.w(TAG, "pump(): AudioRecord.read error=$read")
                break
            }
        }
    }

    // Callers (start()) verify RECORD_AUDIO via hasRecordPermission() before reaching here.
    @SuppressLint("MissingPermission")
    private fun buildRecorder(bufferSize: Int): AudioRecord? {
        // UNPROCESSED preserves the line audio (no AGC/noise suppression); fall back to MIC.
        for (source in intArrayOf(MediaRecorder.AudioSource.UNPROCESSED, MediaRecorder.AudioSource.MIC)) {
            val recorder = try {
                AudioRecord.Builder()
                    .setAudioSource(source)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(ENCODING)
                            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize * 2)
                    .build()
            } catch (ex: Exception) {
                Log.w(TAG, "buildRecorder(): source=$source failed", ex)
                null
            }
            if (recorder != null && recorder.state == AudioRecord.STATE_INITIALIZED) {
                return recorder
            }
            recorder?.release()
        }
        return null
    }

    companion object {
        private const val TAG = "UsbAudioEngine"
        private const val SAMPLE_RATE = 48000
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val DEFAULT_BUFFER_BYTES = 8192
    }
}
