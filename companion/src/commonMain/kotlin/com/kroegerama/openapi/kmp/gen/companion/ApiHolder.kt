package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.Url
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

public typealias AuthItemProvider = suspend () -> AuthItem?

public abstract class ApiHolder {
    public abstract var baseUrl: Url

    public lateinit var json: Json
        private set

    public lateinit var client: HttpClient
        private set

    protected val authProviderMap: MutableMap<String, AuthItemProvider> = mutableMapOf()

    init {
        updateClient()
    }

    public open fun HttpClientConfig<*>.apiConfig() {
        install(ContentNegotiation) {
            json(json)
        }
        install(DefaultRequest) {
            url.takeFrom(baseUrl)
        }
        install(AuthPlugin) {
            authItem { key ->
                authProviderMap[key]?.invoke()
            }
        }
    }

    public fun updateClient(
        json: Json = createDefaultJson(),
        userAgent: String? = defaultUserAgent,
        withCookies: Boolean = false,
        withCompression: Boolean = false,
        withLogging: Boolean = false,
        createHttpClient: (decorator: HttpClientConfig<*>.() -> Unit) -> HttpClient = ::createPlatformHttpClient,
        decorator: HttpClientConfig<*>.() -> Unit = {}
    ) {
        this.json = json
        client = createHttpClient {
            defaultConfig(
                withCookies = withCookies,
                userAgent = userAgent,
                withContentEncoding = withCompression,
                withLogging = withLogging
            )
            apiConfig()
            decorator()
        }
    }

    protected fun setAuthProvider(id: String, provider: AuthItemProvider) {
        authProviderMap[id] = provider
    }

    protected fun clearAuthProvider(id: String) {
        authProviderMap.remove(id)
    }
}
