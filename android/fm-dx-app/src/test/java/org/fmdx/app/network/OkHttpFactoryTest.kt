package org.fmdx.app.network

import okhttp3.ConnectionSpec
import okhttp3.TlsVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OkHttpFactoryTest {

    @Test
    fun `factory enables legacy tls alongside modern protocols`() {
        val client = createFmDxOkHttpClient()

        val specs = client.connectionSpecs
        assertEquals("Expected TLS and cleartext specs", 2, specs.size)
        assertEquals(ConnectionSpec.CLEARTEXT, specs[1])

        val tlsVersions = specs[0].tlsVersions ?: emptyList()
        assertTrue("TLS 1.3 should be enabled", tlsVersions.contains(TlsVersion.TLS_1_3))
        assertTrue("TLS 1.2 should be enabled", tlsVersions.contains(TlsVersion.TLS_1_2))
        assertTrue("TLS 1.1 should be enabled", tlsVersions.contains(TlsVersion.TLS_1_1))
        assertTrue(
            "TLS 1.0 should be enabled for older servers",
            tlsVersions.contains(TlsVersion.TLS_1_0)
        )
    }

    @Test
    fun `factory uses indefinite read timeout`() {
        val client = createFmDxOkHttpClient()

        val readTimeout = client.readTimeoutMillis
        assertTrue("Expected read timeout to be disabled", readTimeout <= 0L)
    }
}
