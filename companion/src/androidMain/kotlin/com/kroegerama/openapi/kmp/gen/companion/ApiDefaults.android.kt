package com.kroegerama.openapi.kmp.gen.companion

import android.os.Build
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig

public actual typealias PlatformHttpClientEngineConfig = OkHttpConfig

public actual val platformUserAgent: String = run {
    "okhttp/${okhttp3.OkHttp.VERSION} Android/API ${Build.VERSION.SDK_INT}"
}

public actual fun createPlatformHttpClient(decorator: HttpClientConfig<PlatformHttpClientEngineConfig>.() -> Unit): HttpClient {
    return HttpClient(OkHttp) {
        decorator()
    }
}
