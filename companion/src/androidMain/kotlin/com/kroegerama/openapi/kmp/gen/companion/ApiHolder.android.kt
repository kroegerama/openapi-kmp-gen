package com.kroegerama.openapi.kmp.gen.companion

import android.os.Build
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.utils.io.KtorDsl

public actual val platformUserAgent: String = run {
    "okhttp/${okhttp3.OkHttp.VERSION} Android/API ${Build.VERSION.SDK_INT}"
}

@KtorDsl
public actual fun createPlatformHttpClient(decorator: HttpClientConfig<*>.() -> Unit): HttpClient {
    return HttpClient(OkHttp) {
        decorator()
    }
}
