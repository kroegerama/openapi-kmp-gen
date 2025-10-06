package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp

internal actual val platformUserAgent: String = run {
    val osName = System.getProperty("os.name") ?: "unknown"
    val osVersion = System.getProperty("os.version") ?: "unknown"
    "okhttp/${okhttp3.OkHttp.VERSION} $osName/$osVersion"
}

internal actual fun createBaseClient(block: HttpClientConfig<*>.() -> Unit): HttpClient {
    return HttpClient(OkHttp) {
        block()
    }
}
