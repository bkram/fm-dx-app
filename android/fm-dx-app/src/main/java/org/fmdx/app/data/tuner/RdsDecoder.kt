package org.fmdx.app.data.tuner

/**
 * Decodes raw RDS groups emitted by the FM-DX Tuner firmware (the `R` line of the XDR / xdrd line
 * protocol) into the human-readable fields the UI needs: PS, RadioText, PTY, TP, TA, MS.
 *
 * fm-dx-webserver performs this decoding server-side and hands the app pre-decoded JSON; when the
 * app talks to the tuner directly (USB) or via xdrd (TCP) it receives the raw groups instead and
 * must decode them here. See [[reference_xdr_usb_protocol]].
 *
 * An `R` payload is a string of hex characters: four 16-bit blocks (A, B, C, D) followed by a
 * status byte. Some firmware builds (legacy RDS message mode) omit block A, so the block count is
 * inferred from the payload length. Block-level error masking from the status byte is intentionally
 * lenient for this first cut — clean signals decode correctly; refinement can come later.
 */
class RdsDecoder {

    private val psChars = CharArray(PS_LENGTH) { ' ' }
    private val psReceived = BooleanArray(PS_LENGTH)
    private val rtChars = CharArray(RT_LENGTH) { ' ' }
    private val rtReceived = BooleanArray(RT_LENGTH)
    private var rtAbFlag: Int = -1

    var pi: Int? = null
        private set
    var pty: Int? = null
        private set
    var tp: Boolean = false
        private set
    var ta: Boolean = false
        private set
    var ms: Boolean = false
        private set

    // Decoder Identification (DI) flags, decoded one bit per group-0 segment.
    var dynamicPty: Boolean? = null
        private set
    var compressed: Boolean? = null
        private set
    var artificialHead: Boolean? = null
        private set

    fun reset() {
        psChars.fill(' ')
        psReceived.fill(false)
        rtChars.fill(' ')
        rtReceived.fill(false)
        rtAbFlag = -1
        pi = null
        pty = null
        tp = false
        ta = false
        ms = false
        dynamicPty = null
        compressed = null
        artificialHead = null
    }

    /** Current Program Service name, or null if no characters have been received yet. */
    fun programService(): String? {
        if (psReceived.none { it }) return null
        return String(psChars).trimEnd()
    }

    /** Current RadioText, or null if nothing has been received yet. */
    fun radioText(): String? {
        if (rtReceived.none { it }) return null
        return String(rtChars).trimEnd()
    }

    /**
     * Feed the hex payload that follows the `R` command character. Returns true if any decoded
     * field changed.
     */
    fun decode(payload: String): Boolean {
        val hex = payload.trim()
        // Drop the trailing status byte (2 hex chars), then split the remainder into 16-bit blocks.
        if (hex.length < BLOCK_HEX * 3 + STATUS_HEX) return false
        val blockHex = hex.length - STATUS_HEX
        val blockCount = blockHex / BLOCK_HEX
        if (blockCount != 3 && blockCount != 4) return false

        val blocks = IntArray(4) { -1 }
        // 4 blocks => A,B,C,D ; 3 blocks => B,C,D (block A absent in legacy message mode).
        val offset = if (blockCount == 4) 0 else 1
        for (i in 0 until blockCount) {
            val start = i * BLOCK_HEX
            val value = hex.substring(start, start + BLOCK_HEX).toIntOrNull(16) ?: return false
            blocks[offset + i] = value
        }

        val blockA = blocks[0]
        val blockB = blocks[1]
        val blockC = blocks[2]
        val blockD = blocks[3]
        if (blockB < 0) return false

        var changed = false

        if (blockA >= 0) {
            if (pi != blockA) {
                pi = blockA
                changed = true
            }
        }

        val groupType = (blockB shr 12) and 0x0F
        val versionB = (blockB and 0x0800) != 0
        val tpBit = (blockB and 0x0400) != 0
        val ptyValue = (blockB shr 5) and 0x1F

        if (tp != tpBit) { tp = tpBit; changed = true }
        if (pty != ptyValue) { pty = ptyValue; changed = true }

        when (groupType) {
            0 -> changed = decodeGroup0(blockB, blockD, versionB) || changed
            2 -> changed = decodeGroup2(blockB, blockC, blockD, versionB) || changed
        }
        return changed
    }

    private fun decodeGroup0(blockB: Int, blockD: Int, versionB: Boolean): Boolean {
        var changed = false
        val taBit = (blockB and 0x0010) != 0
        val msBit = (blockB and 0x0008) != 0
        if (ta != taBit) { ta = taBit; changed = true }
        if (ms != msBit) { ms = msBit; changed = true }

        // For both 0A and 0B the two PS characters live in block D.
        val segment = blockB and 0x03

        // Decoder Identification: one DI bit per segment address (seg 3 = stereo, taken from the
        // S line instead). dynamicPty / compressed / artificialHead are derived here.
        val diValue = (blockB and 0x04) != 0
        when (segment) {
            0 -> if (dynamicPty != diValue) { dynamicPty = diValue; changed = true }
            1 -> if (compressed != diValue) { compressed = diValue; changed = true }
            2 -> if (artificialHead != diValue) { artificialHead = diValue; changed = true }
        }

        val base = segment * 2
        val c0 = (blockD shr 8) and 0xFF
        val c1 = blockD and 0xFF
        if (setPsChar(base, c0)) changed = true
        if (setPsChar(base + 1, c1)) changed = true
        @Suppress("UNUSED_EXPRESSION") versionB // version does not change PS placement
        return changed
    }

    private fun decodeGroup2(blockB: Int, blockC: Int, blockD: Int, versionB: Boolean): Boolean {
        var changed = false
        val abFlag = (blockB shr 4) and 0x01
        if (abFlag != rtAbFlag) {
            rtChars.fill(' ')
            rtReceived.fill(false)
            rtAbFlag = abFlag
            changed = true
        }
        val segment = blockB and 0x0F
        if (versionB) {
            // 2B: two characters in block D, block C repeats PI.
            val base = segment * 2
            if (setRtChar(base, (blockD shr 8) and 0xFF)) changed = true
            if (setRtChar(base + 1, blockD and 0xFF)) changed = true
        } else {
            // 2A: four characters across block C and block D.
            val base = segment * 4
            if (setRtChar(base, (blockC shr 8) and 0xFF)) changed = true
            if (setRtChar(base + 1, blockC and 0xFF)) changed = true
            if (setRtChar(base + 2, (blockD shr 8) and 0xFF)) changed = true
            if (setRtChar(base + 3, blockD and 0xFF)) changed = true
        }
        return changed
    }

    private fun setPsChar(index: Int, code: Int): Boolean {
        if (index !in psChars.indices) return false
        val ch = code.toChar()
        if (psReceived[index] && psChars[index] == ch) return false
        psChars[index] = ch
        psReceived[index] = true
        return true
    }

    private fun setRtChar(index: Int, code: Int): Boolean {
        if (index !in rtChars.indices) return false
        // 0x0D (carriage return) terminates RadioText.
        if (code == 0x0D) {
            for (i in index until rtChars.size) {
                rtChars[i] = ' '
                rtReceived[i] = true
            }
            return true
        }
        val ch = code.toChar()
        if (rtReceived[index] && rtChars[index] == ch) return false
        rtChars[index] = ch
        rtReceived[index] = true
        return true
    }

    companion object {
        private const val PS_LENGTH = 8
        private const val RT_LENGTH = 64
        private const val BLOCK_HEX = 4
        private const val STATUS_HEX = 2
    }
}
