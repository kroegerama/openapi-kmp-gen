package com.kroegerama.openapi.kmp.gen.companion

import com.kroegerama.openapi.kmp.gen.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.compression.ContentEncodingConfig
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.KtorDsl
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

public val defaultUserAgent: String
    get() = "ktor/${BuildConfig.KTOR} kmp-gen/${BuildConfig.COMPANION} $platformUserAgent"

public fun HttpClientConfig<*>.defaultConfig(
    userAgent: String? = defaultUserAgent,
    withCookies: Boolean = false,
    withContentEncoding: Boolean = false,
    withLogging: Boolean = false
) {
    expectSuccess = true
    if (userAgent != null) {
        install(UserAgent) {
            agent = userAgent
        }
    }
    if (withCookies) {
        install(HttpCookies)
    }
    if (withContentEncoding) {
        install(ContentEncoding) {
            mode = ContentEncodingConfig.Mode.All
            gzip(1f)
            deflate(0.5f)
            identity(0f)
        }
    }
    if (withLogging) {
        install(Logging) {
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
    }
}

@KtorDsl
public expect fun createPlatformHttpClient(decorator: HttpClientConfig<*>.() -> Unit = {}): HttpClient
public expect val platformUserAgent: String
