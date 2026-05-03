package org.fmdx.app.audio

import org.fmdx.app.model.TunerInfo
import org.fmdx.app.model.TunerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaItemBuilderTest {

    @Test
    fun nowPlayingFields_fallsBackToAppNameWhenNothingIsAvailable() {
        val fields = nowPlayingFieldsFor(
            appName = "FM-DX",
            tunerInfo = null,
            tunerState = null,
            logoUrl = null
        )

        assertEquals("FM-DX", fields.title)
        assertNull(fields.artist)
        assertNull(fields.subtitle)
        assertNull(fields.description)
        assertEquals("FM-DX", fields.albumTitle)
        assertEquals("FM-DX", fields.station)
        assertNull(fields.artworkUri)
    }

    @Test
    fun nowPlayingFields_prefersPsForTitleAndStation() {
        val fields = nowPlayingFieldsFor(
            appName = "FM-DX",
            tunerInfo = tunerInfo(name = "DX Tuner"),
            tunerState = tunerState(freqMHz = 96.8, ps = "NPO 3FM"),
            logoUrl = null
        )

        assertEquals("NPO 3FM", fields.title)
        assertEquals("NPO 3FM", fields.station)
        assertEquals("DX Tuner", fields.albumTitle)
    }

    @Test
    fun nowPlayingFields_fallsBackFromPsToFreqText() {
        val fields = nowPlayingFieldsFor(
            appName = "FM-DX",
            tunerInfo = tunerInfo(name = "DX Tuner"),
            tunerState = tunerState(freqMHz = 96.8, ps = null),
            logoUrl = null
        )

        assertEquals("96.8 MHz", fields.title)
        assertEquals("DX Tuner", fields.station)
    }

    @Test
    fun nowPlayingFields_artistComposesFreqAndRt0WhenBothPresent() {
        val fields = nowPlayingFieldsFor(
            appName = "FM-DX",
            tunerInfo = null,
            tunerState = tunerState(freqMHz = 93.7, rt0 = "Now playing: Hofstad"),
            logoUrl = null
        )

        assertEquals("93.7 MHz · Now playing: Hofstad", fields.artist)
        assertEquals("Now playing: Hofstad", fields.subtitle)
    }

    @Test
    fun nowPlayingFields_artistIsFreqOnlyWhenRt0Blank() {
        val fields = nowPlayingFieldsFor(
            appName = "FM-DX",
            tunerInfo = null,
            tunerState = tunerState(freqMHz = 93.7, rt0 = "   "),
            logoUrl = null
        )

        assertEquals("93.7 MHz", fields.artist)
        assertNull(fields.subtitle)
    }

    @Test
    fun nowPlayingFields_descriptionMapsToRt1WhenPopulated() {
        val fields = nowPlayingFieldsFor(
            appName = "FM-DX",
            tunerInfo = null,
            tunerState = tunerState(freqMHz = 93.7, rt0 = "first segment", rt1 = "second segment"),
            logoUrl = null
        )

        assertEquals("first segment", fields.subtitle)
        assertEquals("second segment", fields.description)
    }

    @Test
    fun nowPlayingFields_artworkUriIsNullForDefaultLogo() {
        val fields = nowPlayingFieldsFor(
            appName = "FM-DX",
            tunerInfo = null,
            tunerState = null,
            logoUrl = "https://tef.noobish.eu/logos/default-logo.png"
        )

        assertNull(fields.artworkUri)
    }

    @Test
    fun nowPlayingFields_artworkUriIsNullForBlankLogoUrl() {
        val fields = nowPlayingFieldsFor(
            appName = "FM-DX",
            tunerInfo = null,
            tunerState = null,
            logoUrl = "   "
        )

        assertNull(fields.artworkUri)
    }

    @Test
    fun nowPlayingFields_blankPsAndBlankRtAreTreatedAsAbsent() {
        val fields = nowPlayingFieldsFor(
            appName = "FM-DX",
            tunerInfo = tunerInfo(name = "DX Tuner"),
            tunerState = tunerState(freqMHz = 96.8, ps = "   ", rt0 = "", rt1 = "   "),
            logoUrl = null
        )

        assertEquals("96.8 MHz", fields.title)
        assertEquals("96.8 MHz", fields.artist)
        assertNull(fields.subtitle)
        assertNull(fields.description)
    }

    private fun tunerInfo(name: String): TunerInfo = TunerInfo(
        tunerName = name,
        tunerDescription = "",
        antennaNames = emptyList(),
        activeAntenna = 0
    )

    private fun tunerState(
        freqMHz: Double?,
        ps: String? = null,
        rt0: String? = null,
        rt1: String? = null
    ): TunerState = TunerState(
        freqMHz = freqMHz,
        minFreqMHz = null,
        maxFreqMHz = null,
        stepKHz = null,
        signalDbf = null,
        stereo = false,
        stereoForced = false,
        ims = false,
        eq = false,
        antennaIndex = null,
        users = null,
        ps = ps,
        psErrors = emptyList(),
        pi = null,
        ecc = null,
        countryName = null,
        countryIso = null,
        tp = false,
        ta = false,
        ms = false,
        pty = null,
        ptyText = null,
        dynamicPty = null,
        artificialHead = null,
        compressed = null,
        rt0 = rt0,
        rt0Errors = emptyList(),
        rt1 = rt1,
        rt1Errors = emptyList(),
        afList = emptyList(),
        txInfo = null
    )
}
