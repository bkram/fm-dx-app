package org.fmdx.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FmDxSessionControllerTest {

    @Test
    fun sanitizeUrl_throwsOnEmptyInput() {
        assertThrows(IllegalArgumentException::class.java) {
            FmDxSessionController.sanitizeUrl("")
        }
    }

    @Test
    fun sanitizeUrl_throwsOnBlankInput() {
        assertThrows(IllegalArgumentException::class.java) {
            FmDxSessionController.sanitizeUrl("   ")
        }
    }

    @Test
    fun sanitizeUrl_addsHttpSchemeWhenMissing() {
        assertEquals("http://radio.example", FmDxSessionController.sanitizeUrl("radio.example"))
    }

    @Test
    fun sanitizeUrl_keepsHttpsScheme() {
        assertEquals(
            "https://radio.example",
            FmDxSessionController.sanitizeUrl("https://radio.example")
        )
    }

    @Test
    fun sanitizeUrl_stripsTrailingSlashFromRoot() {
        assertEquals(
            "https://radio.example",
            FmDxSessionController.sanitizeUrl("https://radio.example/")
        )
    }

    @Test
    fun sanitizeUrl_keepsTrailingSlashWhenPathIsNonRoot() {
        assertEquals(
            "https://radio.example/tuner/",
            FmDxSessionController.sanitizeUrl("https://radio.example/tuner/")
        )
    }

    @Test
    fun sanitizeUrl_convertsWsToHttp() {
        assertEquals("http://radio.example", FmDxSessionController.sanitizeUrl("ws://radio.example"))
    }

    @Test
    fun sanitizeUrl_convertsWssToHttps() {
        assertEquals(
            "https://radio.example",
            FmDxSessionController.sanitizeUrl("wss://radio.example")
        )
    }

    @Test
    fun sanitizeUrl_preservesNonStandardPort() {
        assertEquals(
            "http://radio.example:8080",
            FmDxSessionController.sanitizeUrl("radio.example:8080")
        )
    }

    @Test
    fun sanitizeUrl_isCaseInsensitiveForScheme() {
        assertEquals(
            "https://radio.example",
            FmDxSessionController.sanitizeUrl("HTTPS://radio.example")
        )
    }

    @Test
    fun sanitizeUrl_throwsOnUnparseableInput() {
        assertThrows(IllegalArgumentException::class.java) {
            FmDxSessionController.sanitizeUrl("http://")
        }
    }
}
