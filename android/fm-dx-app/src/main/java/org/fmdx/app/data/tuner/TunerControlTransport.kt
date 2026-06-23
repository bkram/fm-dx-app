package org.fmdx.app.data.tuner

import org.fmdx.app.model.TunerState

/**
 * A bidirectional control link to a tuner that speaks the XDR / xdrd line protocol. The active
 * implementation is a directly attached USB tuner ([org.fmdx.app.data.usb.UsbTunerTransport]); the
 * fm-dx-webserver WebSocket path stays on its existing JSON code path.
 *
 * Line framing, command encoding and [TunerState] accumulation are shared via [XdrLineParser] so
 * each transport only has to move bytes.
 */
interface TunerControlTransport {

    /**
     * Open the link and start delivering state. Suspends until the link is established (or throws
     * on failure). After it returns, [onState]/[onClosed]/[onError] drive the lifecycle.
     */
    suspend fun open(callbacks: Callbacks)

    /** Queue a protocol command (without trailing newline), e.g. `"T87500"`. */
    fun send(command: String)

    /** Tear the link down. Idempotent. */
    fun close()

    interface Callbacks {
        fun onState(state: TunerState)
        fun onClosed()
        fun onError(error: Throwable)
    }
}

/** Command builders for the XDR / xdrd line protocol (newline is added by the transport). */
object XdrCommands {
    const val STARTUP = "x"
    const val SHUTDOWN = "X"

    fun tune(kHz: Int): String = "T$kHz"
    fun antenna(index: Int): String = "Z$index"
    fun volume(percent: Int): String = "Y$percent"
}
