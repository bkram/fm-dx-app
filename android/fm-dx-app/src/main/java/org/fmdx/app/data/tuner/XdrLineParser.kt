package org.fmdx.app.data.tuner

import org.fmdx.app.model.TunerState
import java.util.Locale

/**
 * Stateful accumulator that turns the FM-DX Tuner / xdrd raw line protocol into [TunerState]
 * snapshots. Unlike the fm-dx-webserver path (one JSON message = one full state), the line protocol
 * delivers fields piecemeal across many lines, so this parser keeps a running state and emits an
 * updated copy whenever a line changes something.
 *
 * Recognised lines (command char + ASCII args, newline already stripped):
 *  - `T<kHz>`            tuned frequency echo
 *  - `S<flag><rssi>,…`   signal level + stereo pilot (flag ∈ s/m/S/M)
 *  - `P<hexPI>…`         PI code
 *  - `R<hex group>`      raw RDS group (decoded via [RdsDecoder])
 *  - `o<auth>,<guests>`  connected user count (xdrd only)
 *
 * See [[reference_xdr_usb_protocol]].
 */
class XdrLineParser {

    private val rds = RdsDecoder()
    private var state = TunerState.empty()

    fun current(): TunerState = state

    fun reset() {
        rds.reset()
        state = TunerState.empty()
    }

    /** Parse one line. Returns the updated [TunerState] if it changed, or null otherwise. */
    fun parse(rawLine: String): TunerState? {
        val line = rawLine.trim()
        if (line.isEmpty()) return null
        val arg = line.substring(1)
        val updated = when (line[0]) {
            'T' -> parseTune(arg)
            'S' -> parseSignal(arg)
            'P' -> parsePi(arg)
            'R' -> parseRds(arg)
            'o' -> parseUsers(arg)
            else -> null
        } ?: return null
        state = updated
        return updated
    }

    private fun parseTune(arg: String): TunerState? {
        val kHz = arg.trim().takeWhile { it.isDigit() }.toIntOrNull() ?: return null
        val mhz = kHz / 1000.0
        if (state.freqMHz == mhz) return null
        // Retuning invalidates the previously decoded RDS payload.
        rds.reset()
        return state.copy(
            freqMHz = mhz,
            ps = null,
            psErrors = emptyList(),
            pi = null,
            rt0 = null,
            rt1 = null,
            pty = null,
            ptyText = null,
            tp = false,
            ta = false,
            ms = false
        )
    }

    private fun parseSignal(arg: String): TunerState? {
        if (arg.isEmpty()) return null
        val flag = arg[0]
        val hasFlag = flag == 's' || flag == 'S' || flag == 'm' || flag == 'M'
        val body = if (hasFlag) arg.substring(1) else arg
        val signal = body.substringBefore(',').trim().toDoubleOrNull()
        // Lowercase => stereo blend active; s/S => stereo pilot present.
        val stereo = if (hasFlag) (flag == 's' || flag == 'S') else state.stereo
        if (signal == state.signalDbf && stereo == state.stereo) return null
        return state.copy(
            signalDbf = signal ?: state.signalDbf,
            stereo = stereo
        )
    }

    private fun parsePi(arg: String): TunerState? {
        val hex = arg.trim().takeWhile { it.isHexDigit() }
        if (hex.isEmpty()) return null
        val pi = hex.take(4).toIntOrNull(16) ?: return null
        val formatted = String.format(Locale.ROOT, "%04X", pi)
        if (formatted == state.pi) return null
        return state.copy(pi = formatted)
    }

    private fun parseRds(arg: String): TunerState? {
        if (!rds.decode(arg)) return null
        val piFromRds = rds.pi?.let { String.format(Locale.ROOT, "%04X", it) }
        return state.copy(
            ps = rds.programService() ?: state.ps,
            pi = state.pi ?: piFromRds,
            rt0 = rds.radioText() ?: state.rt0,
            pty = rds.pty ?: state.pty,
            tp = rds.tp,
            ta = rds.ta,
            ms = rds.ms,
            dynamicPty = rds.dynamicPty ?: state.dynamicPty,
            artificialHead = rds.artificialHead ?: state.artificialHead,
            compressed = rds.compressed ?: state.compressed
        )
    }

    private fun parseUsers(arg: String): TunerState? {
        val parts = arg.split(',')
        val total = parts.sumOf { it.trim().toIntOrNull() ?: 0 }
        if (total == state.users) return null
        return state.copy(users = total)
    }

    private fun Char.isHexDigit(): Boolean =
        this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
}
