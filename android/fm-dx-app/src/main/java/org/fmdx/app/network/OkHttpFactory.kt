package org.fmdx.app.network

import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import java.util.concurrent.TimeUnit

private val fmDxConnectionSpec: ConnectionSpec =
    ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
        .tlsVersions(
            TlsVersion.TLS_1_3,
            TlsVersion.TLS_1_2,
            TlsVersion.TLS_1_1,
            TlsVersion.TLS_1_0
        )
        .allEnabledCipherSuites()
        .build()

fun createFmDxOkHttpClient(): OkHttpClient =
    OkHttpClient.Builder()
        // Legacy deployments still negotiate TLS 1.0; keep the compatible spec available.
        .connectionSpecs(listOf(fmDxConnectionSpec, ConnectionSpec.CLEARTEXT))
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
