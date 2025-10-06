package com.kroegerama.openapi.kmp.gen.companion

import android.os.Build
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp

internal actual val platformUserAgent: String = run {
    "okhttp/${okhttp3.OkHttp.VERSION} Android/${Build.VERSION.SDK_INT}"
}

internal actual fun createBaseClient(block: HttpClientConfig<*>.() -> Unit): HttpClient {
    return HttpClient(OkHttp) {
        block()
    }
}
