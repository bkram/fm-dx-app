//package org.fmdx.app.network
//
//import okhttp3.ConnectionSpec
//import okhttp3.OkHttpClient
//import okhttp3.TlsVersion
//import java.util.concurrent.TimeUnit
//
//private val legacyTlsSpec: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
//    .tlsVersions(TlsVersion.TLS_1_0, TlsVersion.TLS_1_1, TlsVersion.TLS_1_2)
//    .allEnabledCipherSuites()
//    .build()
//
//fun createFmDxOkHttpClient(): OkHttpClient =
//    OkHttpClient.Builder()
//        // Server deployments in the field still require TLS 1.0; keep a legacy spec ahead of CLEAR_TEXT.
//        .connectionSpecs(listOf(legacyTlsSpec, ConnectionSpec.CLEARTEXT))
//        .readTimeout(0, TimeUnit.MILLISECONDS)
//        .build()
