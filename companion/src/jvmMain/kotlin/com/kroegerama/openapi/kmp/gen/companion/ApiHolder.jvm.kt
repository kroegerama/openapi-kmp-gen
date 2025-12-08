package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.utils.io.KtorDsl

public actual val platformUserAgent: String = run {
    val osName = System.getProperty("os.name") ?: "unknown"
    val osVersion = System.getProperty("os.version") ?: "unknown"
    "okhttp/${okhttp3.OkHttp.VERSION} $osName/$osVersion"
}

@KtorDsl
public actual fun createPlatformHttpClient(decorator: HttpClientConfig<*>.() -> Unit): HttpClient {
    return HttpClient(OkHttp) {
        decorator()
    }
}
