package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Creates an [HttpClient] on the [PlatformHttpClientEngineFactory] with the library defaults: success validation, the [UserAgent] header and
 * JSON content negotiation.
 *
 * ```kotlin
 * val client = createApiHttpClient {
 *     install(HttpCookies)
 * }
 * ```
 *
 * @param json the [Json] instance used for content negotiation.
 * @param userAgent value for the `User-Agent` header, or `null` to skip installing [UserAgent]; the engine then sends Ktor's default.
 * @param block additional configuration, run after the defaults; the [Json] instance is set through [json], not here.
 */
public fun createApiHttpClient(
    json: Json = ApiJson,
    userAgent: String? = defaultUserAgent,
    block: HttpClientConfig<PlatformHttpClientEngineConfig>.() -> Unit = {}
): HttpClient = createApiHttpClient(PlatformHttpClientEngineFactory, json, userAgent, block)

/**
 * Creates an [HttpClient] with the library defaults on the given engine factory. The other parameters match [createApiHttpClient].
 *
 * ```kotlin
 * val client = createApiHttpClient(MockEngine) {
 *     engine { addHandler { respond("") } }
 * }
 * ```
 *
 * @param engine the Ktor engine factory to use instead of the [PlatformHttpClientEngineFactory].
 */
public fun <T : HttpClientEngineConfig> createApiHttpClient(
    engine: HttpClientEngineFactory<T>,
    json: Json = ApiJson,
    userAgent: String? = defaultUserAgent,
    block: HttpClientConfig<T>.() -> Unit = {}
): HttpClient = HttpClient(engine, withApiDefaults(json, userAgent, block))

/**
 * Creates an [HttpClient] with the library defaults on an existing engine instance. The other parameters match [createApiHttpClient].
 *
 * ```kotlin
 * val engine = MockEngine { respond("") }
 * val client = createApiHttpClient(engine)
 * ```
 *
 * @param engine the engine instance to use; the client does not own it, so closing the client leaves the engine open.
 */
public fun createApiHttpClient(
    engine: HttpClientEngine,
    json: Json = ApiJson,
    userAgent: String? = defaultUserAgent,
    block: HttpClientConfig<*>.() -> Unit = {}
): HttpClient = HttpClient(engine, withApiDefaults(json, userAgent, block))

private fun <C : HttpClientConfig<*>> withApiDefaults(
    json: Json,
    userAgent: String?,
    block: C.() -> Unit
): C.() -> Unit = {
    expectSuccess = true
    if (userAgent != null) {
        install(UserAgent) {
            agent = userAgent
        }
    }
    install(ContentNegotiation) {
        json(json)
    }
    block()
}
