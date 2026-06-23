package org.fmdx.app.data.tuner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XdrLineParserTest {

    @Test
    fun parsesTuneFrequencyInKHz() {
        val parser = XdrLineParser()
        val state = parser.parse("T87500")
        assertEquals(87.5, state?.freqMHz!!, 0.0001)
    }

    @Test
    fun parsesSignalAndStereoPilot() {
        val parser = XdrLineParser()
        val state = parser.parse("Ss28.50,1,2,3")
        assertEquals(28.5, state?.signalDbf!!, 0.0001)
        assertTrue(state.stereo)
    }

    @Test
    fun lowercaseMNoPilotIsMono() {
        val parser = XdrLineParser()
        val state = parser.parse("Sm12.00,0,0,0")
        assertEquals(false, state?.stereo)
    }

    @Test
    fun parsesPiCode() {
        val parser = XdrLineParser()
        assertEquals("1234", parser.parse("P12340")?.pi)
    }

    @Test
    fun parsesUserCount() {
        val parser = XdrLineParser()
        assertEquals(3, parser.parse("o2,1")?.users)
    }

    @Test
    fun accumulatesProgramServiceFromRdsGroups() {
        val parser = XdrLineParser()
        fun g(seg: Int, d: Int) =
            "R%04X%04X%04X%04X%02X".format(0x1234, (10 shl 5) or (seg and 3), 0, d, 0)
        parser.parse(g(0, 0x5241)) // RA
        parser.parse(g(1, 0x4449)) // DI
        parser.parse(g(2, 0x4F20)) // O_
        val state = parser.parse(g(3, 0x3120)) // 1_
        assertEquals("RADIO 1", state?.ps)
        assertEquals("1234", state?.pi)
    }

    @Test
    fun retuningResetsRds() {
        val parser = XdrLineParser()
        parser.parse("P1234")
        parser.parse("T90000")
        assertNull(parser.current().pi)
    }

    @Test
    fun unchangedLineReturnsNull() {
        val parser = XdrLineParser()
        parser.parse("T87500")
        assertNull(parser.parse("T87500"))
    }

    @Test
    fun ignoresUnknownCommand() {
        val parser = XdrLineParser()
        assertNull(parser.parse("Qsomething"))
    }
}
