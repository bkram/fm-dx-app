package org.fmdx.app.data

sealed interface PluginTelemetryEvent {
    data class Scanner(
        val status: String?,
        val scanValue: String?
    ) : PluginTelemetryEvent

    data class Gps(
        val status: String?,
        val lat: String?,
        val lon: String?,
        val alt: String?,
        val mode: String?
    ) : PluginTelemetryEvent
}
