package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.winhttp.WinHttp
import io.ktor.client.engine.winhttp.WinHttpClientEngineConfig

public actual typealias PlatformHttpClientEngineConfig = WinHttpClientEngineConfig

public actual val PlatformHttpClientEngineFactory: HttpClientEngineFactory<PlatformHttpClientEngineConfig> = WinHttp
