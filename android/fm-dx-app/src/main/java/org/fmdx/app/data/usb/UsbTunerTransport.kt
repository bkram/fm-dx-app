package org.fmdx.app.data.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fmdx.app.data.tuner.TunerControlTransport
import org.fmdx.app.data.tuner.XdrCommands
import org.fmdx.app.data.tuner.XdrLineParser
import java.io.IOException

/**
 * Talks the XDR / xdrd line protocol directly to a USB-attached FM-DX Tuner (TEF668X headless
 * build) over CDC-ACM, using the usb-serial-for-android library. No xdrd, no TCP, no auth — those
 * are purely xdrd's network wrapper. See [[reference_xdr_usb_protocol]].
 *
 * The caller is responsible for having obtained USB permission for [device] before calling [open]
 * (see [UsbTunerDiscovery]).
 */
class UsbTunerTransport(
    private val usbManager: UsbManager,
    private val device: UsbDevice,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TunerControlTransport {

    private val parser = XdrLineParser()
    private val lineBuffer = StringBuilder()

    @Volatile
    private var port: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null
    @Volatile
    private var closed = false

    override suspend fun open(callbacks: TunerControlTransport.Callbacks) {
        withContext(ioDispatcher) {
            val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
                ?: CdcAcmSerialDriver(device)
            val connection = usbManager.openDevice(device)
                ?: throw IOException("Unable to open USB device (permission not granted?)")
            val serialPort = driver.ports.firstOrNull()
                ?: throw IOException("USB device exposes no serial port")
            serialPort.open(connection)
            serialPort.setParameters(
                BAUD_RATE,
                8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )
            // CDC-ACM enables the data lines via DTR/RTS.
            runCatching { serialPort.dtr = true }
            runCatching { serialPort.rts = true }
            port = serialPort

            ioManager = SerialInputOutputManager(
                serialPort,
                object : SerialInputOutputManager.Listener {
                    override fun onNewData(data: ByteArray) = onSerialData(data, callbacks)
                    override fun onRunError(e: Exception) {
                        if (!closed) {
                            callbacks.onError(e)
                            callbacks.onClosed()
                        }
                    }
                }
            ).also { it.start() }
        }
        // Wake the firmware; it replies on its own status interval thereafter.
        send(XdrCommands.STARTUP)
    }

    override fun send(command: String) {
        val p = port ?: return
        val bytes = (command + "\n").toByteArray(Charsets.US_ASCII)
        runCatching { p.write(bytes, WRITE_TIMEOUT_MS) }
    }

    override fun close() {
        closed = true
        runCatching { ioManager?.stop() }
        ioManager = null
        runCatching { port?.close() }
        port = null
    }

    private fun onSerialData(data: ByteArray, callbacks: TunerControlTransport.Callbacks) {
        for (byte in data) {
            val c = byte.toInt().toChar()
            when (c) {
                '\n' -> {
                    val line = lineBuffer.toString()
                    lineBuffer.setLength(0)
                    if (line.isNotEmpty()) parser.parse(line)?.let(callbacks::onState)
                }
                '\r' -> { /* ignore */ }
                else -> if (lineBuffer.length < MAX_LINE) lineBuffer.append(c)
            }
        }
    }

    companion object {
        private const val BAUD_RATE = 115200
        private const val WRITE_TIMEOUT_MS = 2000
        private const val MAX_LINE = 4096
    }
}
