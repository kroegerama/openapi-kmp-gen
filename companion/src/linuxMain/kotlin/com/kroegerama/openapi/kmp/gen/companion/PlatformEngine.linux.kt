package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.curl.Curl
import io.ktor.client.engine.curl.CurlClientEngineConfig

public actual typealias PlatformHttpClientEngineConfig = CurlClientEngineConfig

public actual val PlatformHttpClientEngineFactory: HttpClientEngineFactory<PlatformHttpClientEngineConfig> = Curl
