package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.http.Url
import io.ktor.http.takeFrom
import kotlinx.serialization.json.Json
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.update

public typealias AuthItemProvider = suspend () -> AuthItem?

@OptIn(ExperimentalAtomicApi::class)
public abstract class ApiHolder {
    public abstract var baseUrl: Url

    private class State(
        val client: HttpClient,
        val json: Json,
        val replaced: AtomicBoolean
    )

    private inline fun createState(json: Json, createClient: (replaced: AtomicBoolean) -> HttpClient): State {
        val replaced = AtomicBoolean(false)
        return State(createClient(replaced), json, replaced)
    }

    private val stateRef: AtomicReference<State?> = AtomicReference(null)

    public val json: Json
        get() = stateRef.load()?.json ?: ApiJson

    public val client: HttpClient
        get() = loadOrCreateState().client

    private val authProviders: AtomicReference<Map<String, AuthItemProvider>> = AtomicReference(emptyMap())
    private val unauthorizedHandler: AtomicReference<UnauthorizedHandler?> = AtomicReference(null)

    private val defaultState: State by lazy {
        createState(ApiJson) { replaced ->
            createApiHttpClient(PlatformHttpClientEngineFactory, ApiJson, defaultUserAgent, withApiPlugins(replaced) {})
        }
    }

    private fun loadOrCreateState(): State {
        stateRef.load()?.let { return it }
        val default = defaultState
        if (stateRef.compareAndSet(null, default)) return default
        val current = stateRef.load() ?: return default
        if (current !== default) default.client.close()
        return current
    }

    private fun replaceState(newState: State) {
        val previous = stateRef.exchange(newState) ?: return
        previous.replaced.store(true)
        previous.client.close()
    }

    private fun <C : HttpClientConfig<*>> withApiPlugins(replaced: AtomicBoolean, block: C.() -> Unit): C.() -> Unit = {
        // close() leaves a client active while a child of its job is running, so new requests are rejected here
        install("ReplacedClientGuard") {
            requestPipeline.intercept(HttpRequestPipeline.Before) {
                check(!replaced.load()) { "The client was replaced by updateClient and accepts no new requests" }
            }
        }
        install(DefaultRequest) {
            url.takeFrom(baseUrl)
        }
        install(AuthPlugin) {
            authItem { key ->
                authProviders.load()[key]?.invoke()
            }
            onUnauthorized { appliedItems ->
                this@ApiHolder.unauthorizedHandler.load()?.invoke(appliedItems) ?: false
            }
        }
        block()
    }

    /**
     * Replaces [client] and [json] with a new client on the [PlatformHttpClientEngineFactory] and closes the previous client. An
     * [eitherRequest] started on the previous client afterwards returns an [UnexpectedCallException]; one still running on it completes or
     * returns a [CallException].
     *
     * ```kotlin
     * Api.updateClient {
     *     install(HttpCookies)
     * }
     * ```
     *
     * @see createApiHttpClient for the parameters.
     */
    public fun updateClient(
        json: Json = ApiJson,
        userAgent: String? = defaultUserAgent,
        block: HttpClientConfig<PlatformHttpClientEngineConfig>.() -> Unit = {}
    ) {
        updateClient(PlatformHttpClientEngineFactory, json, userAgent, block)
    }

    /**
     * Replaces [client] and [json] with a new client on the given engine factory. The previous client and the other parameters are
     * handled as in [updateClient].
     *
     * ```kotlin
     * Api.updateClient(MockEngine) {
     *     engine { addHandler { respond("") } }
     * }
     * ```
     *
     * @param engine the Ktor engine factory to use instead of the [PlatformHttpClientEngineFactory].
     */
    public fun <T : HttpClientEngineConfig> updateClient(
        engine: HttpClientEngineFactory<T>,
        json: Json = ApiJson,
        userAgent: String? = defaultUserAgent,
        block: HttpClientConfig<T>.() -> Unit = {}
    ) {
        replaceState(createState(json) { replaced ->
            createApiHttpClient(engine, json, userAgent, withApiPlugins(replaced, block))
        })
    }

    /**
     * Replaces [client] and [json] with a new client on an existing engine instance. The previous client and the other parameters are
     * handled as in [updateClient].
     *
     * ```kotlin
     * val engine = MockEngine { respond("") }
     * Api.updateClient(engine)
     * ```
     *
     * @param engine a caller-owned engine instance, never closed by this holder; must not be an engine owned by one of this holder's clients.
     */
    public fun updateClient(
        engine: HttpClientEngine,
        json: Json = ApiJson,
        userAgent: String? = defaultUserAgent,
        block: HttpClientConfig<*>.() -> Unit = {}
    ) {
        replaceState(createState(json) { replaced ->
            createApiHttpClient(engine, json, userAgent, withApiPlugins(replaced, block))
        })
    }

    @Deprecated(
        message = "The client is created by updateClient. Move the engine setup of createHttpClient into the block (engine { }) or pass an " +
                "engine, and install HttpCookies and ContentEncoding in the block instead of withCookies and withCompression.",
        replaceWith = ReplaceWith("updateClient(json, userAgent, decorator)"),
        level = DeprecationLevel.ERROR
    )
    @Suppress("UNUSED_PARAMETER")
    public fun updateClient(
        json: Json = ApiJson,
        userAgent: String? = defaultUserAgent,
        withCookies: Boolean = false,
        withCompression: Boolean = false,
        createHttpClient: (decorator: HttpClientConfig<PlatformHttpClientEngineConfig>.() -> Unit) -> HttpClient,
        decorator: HttpClientConfig<PlatformHttpClientEngineConfig>.() -> Unit = {}
    ) {
        error("Removed, use updateClient(json, userAgent, block)")
    }

    /**
     * Sets the [UnauthorizedHandler] consulted when a request with auth keys is answered with
     * 401 Unauthorized; the request is retried once with freshly resolved auth values when the
     * handler returns `true`. Like the auth providers, the handler is read per request, so it
     * can be set or replaced without rebuilding the client. Pass `null` to remove it.
     *
     * ```kotlin
     * Api.setAuthProvider(Auth.MyScheme(keycloak.asBearerProvider()))
     * Api.setUnauthorizedHandler(keycloak.asUnauthorizedHandler())
     * ```
     */
    public fun setUnauthorizedHandler(handler: UnauthorizedHandler?) {
        unauthorizedHandler.store(handler)
    }

    protected fun setAuthProvider(id: String, provider: AuthItemProvider) {
        authProviders.update { it + (id to provider) }
    }

    protected fun clearAuthProvider(id: String) {
        authProviders.update { it - id }
    }
}
