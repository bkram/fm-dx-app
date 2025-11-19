package org.fmdx.app.telemetry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicReference

data class GpsData(
    val lat: String,
    val lon: String,
    val alt: String,
    val mode: String,
    val timestamp: Long = System.currentTimeMillis()
)

object GpsStore {
    private val reference = AtomicReference(GpsData("", "", "0", "2", 0L))
    private val _flow = MutableStateFlow(reference.get())
    val flow: StateFlow<GpsData> = _flow

    fun update(newData: GpsData) {
        reference.set(newData)
        _flow.value = newData
    }

    fun latest(): GpsData = reference.get()
}
