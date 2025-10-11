package com.kroegerama.openapi.kmp.gen.companion

import com.kroegerama.openapi.kmp.gen.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json

public fun createDefaultJson(): Json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = true
    allowStructuredMapKeys = true
    prettyPrint = false
    explicitNulls = false
    coerceInputValues = true
    useArrayPolymorphism = false
    allowSpecialFloatingPointValues = true
}

public fun createDefaultHttpClient(
    decorator: HttpClientConfig<*>.() -> Unit = {}
): HttpClient = createBaseClient {
    expectSuccess = true
    install(HttpCookies)
    install(UserAgent) {
        agent = "ktor/${BuildConfig.KTOR} kmp-gen/${BuildConfig.COMPANION} $platformUserAgent"
    }
    install(ContentEncoding) {
        gzip()
    }
    install(Logging) {
        sanitizeHeader { header -> header == HttpHeaders.Authorization }
    }
    decorator()
}

internal expect fun createBaseClient(block: HttpClientConfig<*>.() -> Unit = {}): HttpClient
public expect val platformUserAgent: String
