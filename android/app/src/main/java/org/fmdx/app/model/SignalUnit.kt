package org.fmdx.app.model

enum class SignalUnit(val displayName: String) {
    DBF("dBf"),
    DBUV("dBµV"),
    DBM("dBm");

    companion object {
        fun fromDisplayName(name: String): SignalUnit =
            entries.firstOrNull { it.displayName == name } ?: DBF
    }
}
