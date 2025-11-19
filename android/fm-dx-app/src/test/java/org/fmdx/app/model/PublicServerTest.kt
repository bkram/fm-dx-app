package org.fmdx.app.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicServerTest {

    @Test
    fun fromJson_parsesCoreFields() {
        val json = JSONObject(
            """
            {
              "name": "SZFV - C.Center",
              "desc": "Downtown shared receiver",
              "contact": "ops@example.org",
              "tuner": "tef",
              "version": "1.3.11",
              "bwLimit": "65 - 108 MHz",
              "coords": ["47.192445", "18.422652"],
              "url": "https://fehervartuner.dfm.hu/",
              "country": "hu",
              "status": 1,
              "audioQuality": "256k",
              "audioChannels": 2,
              "countryName": "Hungary",
              "city": "Szekesfehervar",
              "os": "Windows_NT 10.0.17763"
            }
            """.trimIndent()
        )

        val server = PublicServer.fromJson(json)

        assertNotNull(server)
        server!!
        assertEquals("SZFV - C.Center", server.name)
        assertEquals("Downtown shared receiver", server.description)
        assertEquals("https://fehervartuner.dfm.hu/", server.url)
        assertEquals(47.192445, server.latitude!!, 1e-6)
        assertEquals(18.422652, server.longitude!!, 1e-6)
        assertTrue(server.isOnline)
        assertEquals("Szekesfehervar, Hungary", server.displayLocation)
    }

    @Test
    fun matchesQuery_searchesAllRelevantFields() {
        val server = PublicServer.sample().copy(
            name = "Budapest DX South",
            description = "7 element Yagi",
            tuner = "TEF6687",
            audioQuality = "320k",
            city = "Budapest",
            countryName = "Hungary"
        )

        assertTrue(server.matchesQuery("budapest"))
        assertTrue(server.matchesQuery("yagi"))
        assertTrue(server.matchesQuery("tef6687"))
        assertTrue(server.matchesQuery("320k"))
        assertFalse(server.matchesQuery("atlantic"))
    }

    @Test
    fun displayLocation_fallsBackToCountryCode() {
        val serverNoCity = PublicServer.sample().copy(
            city = null,
            countryName = null,
            countryCode = "nl"
        )
        assertEquals("Netherlands", serverNoCity.displayLocation)

        val serverNoName = PublicServer.sample().copy(
            city = null,
            countryName = null,
            countryCode = "xx"
        )
        assertEquals("XX", serverNoName.displayLocation)
    }

    @Test
    fun displayLocation_ignoresLiteralNullStrings() {
        val server = PublicServer.sample().copy(
            city = "null",
            countryName = "Null",
            countryCode = ""
        )

        assertEquals(null, server.displayLocation)
    }

    @Test
    fun fromJson_movesFragmentIntoPath() {
        val json = JSONObject(
            """
            {
              "url": "https://example.com/path/#fragment"
            }
            """.trimIndent()
        )
        val server = PublicServer.fromJson(json)

        assertNotNull(server)
        assertEquals("https://example.com/path/fragment", server!!.url)
    }

    @Test
    fun fromJson_preservesTrailingHashlessUrls() {
        val json = JSONObject("""{"url":"https://ctrl.fm-tuner.nl/#kaas/"}""")
        val server = PublicServer.fromJson(json)
        assertEquals("https://ctrl.fm-tuner.nl/kaas/", server!!.url)
    }
}
