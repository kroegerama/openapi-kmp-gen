package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory

public expect class PlatformHttpClientEngineConfig : HttpClientEngineConfig

/**
 * The default Ktor engine of the current platform: OkHttp on Android and JVM, Darwin on Apple, Curl on Linux and WinHttp on Windows.
 */
public expect val PlatformHttpClientEngineFactory: HttpClientEngineFactory<PlatformHttpClientEngineConfig>
