package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig

public actual typealias PlatformHttpClientEngineConfig = OkHttpConfig

public actual val PlatformHttpClientEngineFactory: HttpClientEngineFactory<PlatformHttpClientEngineConfig> = OkHttp
