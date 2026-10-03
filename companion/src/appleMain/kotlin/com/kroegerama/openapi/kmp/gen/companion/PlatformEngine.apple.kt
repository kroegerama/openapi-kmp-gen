package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig

public actual typealias PlatformHttpClientEngineConfig = DarwinClientEngineConfig

public actual val PlatformHttpClientEngineFactory: HttpClientEngineFactory<PlatformHttpClientEngineConfig> = Darwin
