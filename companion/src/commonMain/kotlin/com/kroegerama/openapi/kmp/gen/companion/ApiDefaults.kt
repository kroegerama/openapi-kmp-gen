package com.kroegerama.openapi.kmp.gen.companion

import com.kroegerama.openapi.kmp.gen.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.compression.ContentEncodingConfig
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

public val defaultUserAgent: String
    get() = "ktor/${BuildConfig.KTOR} kmp-gen/${BuildConfig.COMPANION} $platformUserAgent"

public val defaultSensitiveHeaders: Set<String> = setOf(
    HttpHeaders.Authorization,
    HttpHeaders.ProxyAuthorization,
    HttpHeaders.Cookie,
    HttpHeaders.SetCookie
)

/**
 * Applies the library's default [HttpClient] configuration (success validation, user agent,
 * and the optional cookie/compression/logging plugins).
 *
 * @param userAgent value for the `User-Agent` header, or `null` to skip installing [UserAgent].
 * @param withCookies installs [HttpCookies] cookie handling.
 * @param withContentEncoding installs gzip/deflate [ContentEncoding].
 * @param withLogging installs [Logging], redacting the values of [sanitizeHeaders] (case-insensitive).
 * @param sanitizeHeaders header names to redact from logs - e.g. the name of a custom
 *   API-key header. **Only headers can be redacted:** a secret carried in a query parameter (an
 *   [AuthItem.ApiKey] in [AuthItem.Position.Query]) is logged verbatim as part of the request URL,
 *   because Ktor's [Logging] plugin has no query-redaction hook. Prefer header or cookie position
 *   for secrets, or install a custom [Logging] logger via the client `decorator` if URL redaction
 *   is required.
 */
public fun HttpClientConfig<PlatformHttpClientEngineConfig>.defaultConfig(
    userAgent: String? = defaultUserAgent,
    withCookies: Boolean = false,
    withContentEncoding: Boolean = false,
    withLogging: Boolean = false,
    sanitizeHeaders: Set<String> = defaultSensitiveHeaders
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
        val sensitive = sanitizeHeaders.mapTo(HashSet()) { it.lowercase() }
        install(Logging) {
            sanitizeHeader { header -> header.lowercase() in sensitive }
        }
    }
}

public expect class PlatformHttpClientEngineConfig : HttpClientEngineConfig

public expect fun createPlatformHttpClient(decorator: HttpClientConfig<PlatformHttpClientEngineConfig>.() -> Unit = {}): HttpClient

public expect val platformUserAgent: String
