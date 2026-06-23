package org.fmdx.app.data.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Enumerates USB-serial devices that could be an FM-DX Tuner and brokers the runtime USB permission
 * dialog. CDC-ACM detection is delegated to usb-serial-for-android's default prober.
 */
object UsbTunerDiscovery {

    private const val ACTION_USB_PERMISSION = "org.fmdx.app.action.USB_PERMISSION"

    // Only the TEF668X Headless USB Tuner is supported — it is the build that exposes a USB Audio
    // Class interface alongside CDC-ACM. VID 0x1209 (pid.codes / FMDX.org), PID 0x6687.
    private const val TEF_VENDOR_ID = 0x1209
    private const val TEF_PRODUCT_ID = 0x6687

    /**
     * Attached TEF668X Headless USB tuners (CDC-ACM serial + USB audio). Detection is by USB
     * descriptor over the raw device list rather than the serial prober — the prober does not
     * reliably surface this composite (audio + CDC, IAD) device, and the transport falls back to
     * an explicit [com.hoho.android.usbserial.driver.CdcAcmSerialDriver] anyway.
     */
    fun findTunerDevices(usbManager: UsbManager): List<UsbDevice> =
        usbManager.deviceList.values.filter { it.isTefHeadlessTuner() }

    private fun UsbDevice.isTefHeadlessTuner(): Boolean {
        if (vendorId == TEF_VENDOR_ID && productId == TEF_PRODUCT_ID) return true
        // Fallback for future firmware revisions that keep the descriptive product string.
        val name = (productName ?: "").lowercase()
        return name.contains("tef") && name.contains("headless")
    }

    fun firstTuner(usbManager: UsbManager): UsbDevice? = findTunerDevices(usbManager).firstOrNull()

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
