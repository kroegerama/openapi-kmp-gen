package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
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

    public fun updateClient(
        json: Json = createDefaultJson(),
        clientDecorator: HttpClientConfig<*>.() -> Unit = {}
    ) {
        this.json = json
        client = createDefaultHttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            install(DefaultRequest) {
                url.takeFrom(baseUrl)
                contentType(ContentType.Application.Json)
            }
            install(AuthPlugin) {
                authItem { key ->
                    authProviderMap[key]?.invoke()
                }
            }
            clientDecorator()
        }
    }

    protected fun setAuthProvider(id: String, provider: AuthItemProvider) {
        authProviderMap[id] = provider
    }

    protected fun clearAuthProvider(id: String) {
        authProviderMap.remove(id)
    }
}
