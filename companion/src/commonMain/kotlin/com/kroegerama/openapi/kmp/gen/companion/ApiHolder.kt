package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.Url
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.update

public typealias AuthItemProvider = suspend () -> AuthItem?

@OptIn(ExperimentalAtomicApi::class)
public abstract class ApiHolder {
    public abstract var baseUrl: Url

    public var json: Json = createDefaultJson()
        private set

    private var _client: HttpClient? = null
    public val client: HttpClient
        get() {
            _client?.let { return it }
            updateClient()
            return _client!!
        }

    private val authProviders: AtomicReference<Map<String, AuthItemProvider>> = AtomicReference(emptyMap())

    public open fun HttpClientConfig<PlatformHttpClientEngineConfig>.apiConfig() {
        install(ContentNegotiation) {
            json(json)
        }
        install(DefaultRequest) {
            url.takeFrom(baseUrl)
        }
        install(AuthPlugin) {
            authItem { key ->
                authProviders.load()[key]?.invoke()
            }
        }
    }

    public fun updateClient(
        json: Json = createDefaultJson(),
        userAgent: String? = defaultUserAgent,
        withCookies: Boolean = false,
        withCompression: Boolean = false,
        withLogging: Boolean = false,
        sanitizeHeaders: Set<String> = defaultSensitiveHeaders,
        createHttpClient: (decorator: HttpClientConfig<PlatformHttpClientEngineConfig>.() -> Unit) -> HttpClient = ::createPlatformHttpClient,
        decorator: HttpClientConfig<PlatformHttpClientEngineConfig>.() -> Unit = {}
    ) {
        this.json = json
        val previous = _client
        _client = createHttpClient {
            defaultConfig(
                withCookies = withCookies,
                userAgent = userAgent,
                withContentEncoding = withCompression,
                withLogging = withLogging,
                sanitizeHeaders = sanitizeHeaders
            )
            apiConfig()
            decorator()
        }
        previous?.close()
    }

    protected fun setAuthProvider(id: String, provider: AuthItemProvider) {
        authProviders.update { it + (id to provider) }
    }

    protected fun clearAuthProvider(id: String) {
        authProviders.update { it - id }
    }
}
