package org.fmdx.app.data.tuner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RdsDecoderTest {

    /** Builds a non-legacy R-line payload: blockA..D as hex16 + status hex8. */
    private fun group(a: Int, b: Int, c: Int, d: Int, status: Int = 0): String =
        "%04X%04X%04X%04X%02X".format(a, b, c, d, status)

    // Group 0A block B: type 0, version A, ms bit set, segment in low 2 bits, PTY=10.
    private fun group0A(segment: Int): Int = (10 shl 5) or (1 shl 3) or (segment and 0x03)

    @Test
    fun decodesProgramServiceAcrossSegments() {
        val rds = RdsDecoder()
        // "RADIO 1 "
        rds.decode(group(0x1234, group0A(0), 0x0000, 0x5241)) // R A
        rds.decode(group(0x1234, group0A(1), 0x0000, 0x4449)) // D I
        rds.decode(group(0x1234, group0A(2), 0x0000, 0x4F20)) // O _
        rds.decode(group(0x1234, group0A(3), 0x0000, 0x3120)) // 1 _

        assertEquals("RADIO 1", rds.programService())
        assertEquals(0x1234, rds.pi)
        assertEquals(10, rds.pty)
        assertTrue(rds.ms)
    }

    @Test
    fun decodesRadioTextGroup2A() {
        val rds = RdsDecoder()
        // segment 0, 4 chars: 'H','i' then two spaces
        rds.decode(group(0x1234, 0x2000, 0x4869, 0x2020))
        assertEquals("Hi", rds.radioText())
    }

    @Test
    fun carriageReturnTerminatesRadioText() {
        val rds = RdsDecoder()
        // 'O','K', 0x0D (terminator), filler
        rds.decode(group(0x1234, 0x2000, 0x4F4B, 0x0D20))
        assertEquals("OK", rds.radioText())
    }

    @Test
    fun changingTextAbFlagClearsRadioText() {
        val rds = RdsDecoder()
        rds.decode(group(0x1234, 0x2000, 0x4869, 0x2020)) // "Hi", A/B = 0
        rds.decode(group(0x1234, 0x2010, 0x4279, 0x6520)) // A/B flips to 1 -> buffer cleared
        // New text starts fresh from segment 0.
        assertEquals("Bye", rds.radioText())
    }

    @Test
    fun decodesLegacyGroupWithoutBlockA() {
        val rds = RdsDecoder()
        // Legacy: omit block A (12 hex blocks + status). PI stays unknown from this line.
        val legacy = "%04X%04X%04X%02X".format(group0A(0), 0x0000, 0x5241, 0)
        assertTrue(rds.decode(legacy))
        assertNull(rds.pi)
        assertEquals("RA", rds.programService())
    }

    @Test
    fun decodesDiFlags() {
        val rds = RdsDecoder()
        val base = (10 shl 5) or (1 shl 3) // PTY 10 + MS
        rds.decode(group(0x1234, base or 0x04 or 0, 0, 0x4141)) // seg 0, DI=1 -> dynamic PTY
        rds.decode(group(0x1234, base or 0x04 or 1, 0, 0x4141)) // seg 1, DI=1 -> compressed
        rds.decode(group(0x1234, base or 0x00 or 2, 0, 0x4141)) // seg 2, DI=0 -> artificial head off
        assertEquals(true, rds.dynamicPty)
        assertEquals(true, rds.compressed)
        assertEquals(false, rds.artificialHead)
    }

    @Test
    fun rejectsMalformedPayload() {
        val rds = RdsDecoder()
        assertEquals(false, rds.decode("xyz"))
        assertEquals(false, rds.decode(""))
        assertNull(rds.programService())
    }
}
