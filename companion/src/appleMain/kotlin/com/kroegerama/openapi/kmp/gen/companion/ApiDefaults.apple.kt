package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig

public actual typealias PlatformHttpClientEngineConfig = DarwinClientEngineConfig

public actual fun createPlatformHttpClient(decorator: HttpClientConfig<PlatformHttpClientEngineConfig>.() -> Unit): HttpClient {
    return HttpClient(Darwin) {
        engine {
            configureRequest {
                setAllowsCellularAccess(true)
            }
        }
        decorator()
    }
}
