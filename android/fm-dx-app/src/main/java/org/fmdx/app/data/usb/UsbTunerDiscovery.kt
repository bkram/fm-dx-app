package org.fmdx.app.data.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Kind of attached tuner:
 * - [HEADLESS] — the composite "Headless TEF Tuner" (CDC-ACM + USB Audio, VID 0x1209 / PID 0x6687).
 *   The only variant with a USB audio output.
 * - [GENERIC] — any other USB-serial device (CH340, CP210x, CH9102, FTDI, native CDC on ESP32-S3,
 *   …) that speaks the TEF/XDR line protocol. Control + RDS only; no USB audio.
 */
enum class UsbTunerKind { HEADLESS, GENERIC }

data class DetectedTuner(val device: UsbDevice, val kind: UsbTunerKind) {
    val hasUsbAudio: Boolean get() = kind == UsbTunerKind.HEADLESS
    val displayName: String
        get() = when (kind) {
            // Show the headless unit's official USB product name (e.g. "TEF668X Headless USB Tuner").
            UsbTunerKind.HEADLESS ->
                device.productName?.takeIf { it.isNotBlank() } ?: "TEF668X Headless USB Tuner"
            UsbTunerKind.GENERIC -> "Generic TEF"
        }
}

object UsbTunerDiscovery {

    private const val ACTION_USB_PERMISSION = "org.fmdx.app.action.USB_PERMISSION"

    // The Headless TEF Tuner enumerates as a composite CDC-ACM + USB-Audio device; it is the only
    // variant with a USB audio output. Identify it by descriptor (no need to open it).
    private const val HEADLESS_VENDOR_ID = 0x1209
    private const val HEADLESS_PRODUCT_ID = 0x6687

    /**
     * Classify the first attached tuner candidate, preferring the audio-capable headless unit.
     * Generic candidates are recognised USB-serial bridges; whether one is really a TEF is
     * confirmed by querying the XDR protocol once connected.
     */
    fun detect(usbManager: UsbManager): DetectedTuner? {
        usbManager.deviceList.values.firstOrNull { it.isHeadless() }
            ?.let { return DetectedTuner(it, UsbTunerKind.HEADLESS) }
        UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            .map { it.device }
            .firstOrNull { !it.isHeadless() }
            ?.let { return DetectedTuner(it, UsbTunerKind.GENERIC) }
        return null
    }

    private fun UsbDevice.isHeadless(): Boolean {
        if (vendorId == HEADLESS_VENDOR_ID && productId == HEADLESS_PRODUCT_ID) return true
        val name = (productName ?: "").lowercase()
        return name.contains("headless") && name.contains("tef")
    }

    /** Suspends until the user grants or denies USB permission for [device]. */
    suspend fun ensurePermission(
        context: Context,
        usbManager: UsbManager,
        device: UsbDevice
    ): Boolean {
        if (usbManager.hasPermission(device)) return true
        val appContext = context.applicationContext
        return suspendCancellableCoroutine { continuation ->
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (intent.action != ACTION_USB_PERMISSION) return
                    runCatching { appContext.unregisterReceiver(this) }
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (continuation.isActive) continuation.resume(granted)
                }
            }
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                IntentFilter(ACTION_USB_PERMISSION),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            // FLAG_MUTABLE so the system can attach EXTRA_PERMISSION_GRANTED to the result.
            val pendingIntent = PendingIntent.getBroadcast(
                appContext,
                0,
                Intent(ACTION_USB_PERMISSION).setPackage(appContext.packageName),
                PendingIntent.FLAG_MUTABLE
            )
            usbManager.requestPermission(device, pendingIntent)
            continuation.invokeOnCancellation {
                runCatching { appContext.unregisterReceiver(receiver) }
            }
        }
    }
}
