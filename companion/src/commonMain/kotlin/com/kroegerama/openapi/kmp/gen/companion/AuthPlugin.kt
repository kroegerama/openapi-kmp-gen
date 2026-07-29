package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.util.*
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.utils.io.*

public typealias AuthItemResolver = suspend (String) -> AuthItem?

/**
 * Decides whether a request that was answered with 401 Unauthorized should be retried once
 * with freshly resolved auth values. `appliedItems` holds the auth items the failed request
 * was sent with, keyed by auth key; keys whose resolver returned no item are absent. Return
 * `true` to retry - typically after renewing the underlying credential, e.g. via
 * [com.kroegerama.openapi.kmp.gen.companion.keycloak.Keycloak.asUnauthorizedHandler].
 *
 * The handler is only consulted for requests that declared [AuthPlugin.Plugin.authKeys],
 * and at most once per request: a 401 on the retried request is returned to the caller.
 * The retry re-sends the request body, so it must be replayable; a streaming body (e.g. a
 * `ByteReadChannel`) arrives consumed on the retry.
 */
public typealias UnauthorizedHandler = suspend (appliedItems: Map<String, AuthItem>) -> Boolean

/**
 * Applies the [AuthItem]s resolved for a request's [Plugin.authKeys]. When an
 * [UnauthorizedHandler] is configured via [Config.onUnauthorized], a 401 Unauthorized
 * response additionally triggers a single retry with re-resolved auth values.
 */
public class AuthPlugin private constructor(
    private val authItemResolver: AuthItemResolver,
    private val unauthorizedHandler: UnauthorizedHandler?
) {
    @KtorDsl
    public class Config(
        internal var authItemResolver: AuthItemResolver = { null }
    ) {
        internal var unauthorizedHandler: UnauthorizedHandler? = null

        public fun authItem(resolver: AuthItemResolver) {
            authItemResolver = resolver
        }

        /** See [UnauthorizedHandler]. */
        public fun onUnauthorized(handler: UnauthorizedHandler) {
            unauthorizedHandler = handler
        }
    }

    /**
     * Resolves and applies the auth items for [authKeys], returning the applied items by key.
     * The resolver is invoked once per key occurrence; for a repeated key the last resolved
     * item is kept.
     */
    private suspend fun applyAuth(
        request: HttpRequestBuilder,
        authKeys: List<String>
    ): Map<String, AuthItem> {
        val applied = mutableMapOf<String, AuthItem>()
        authKeys.forEach { authKey ->
            val authItem = authItemResolver(authKey) ?: return@forEach
            applied[authKey] = authItem
            when (authItem) {
                is AuthItem.ApiKey -> when (authItem.position) {
                    AuthItem.Position.Header -> request.header(authItem.name, authItem.value)
                    AuthItem.Position.Query -> request.parameter(authItem.name, authItem.value)
                    AuthItem.Position.Cookie -> request.cookie(authItem.name, authItem.value)
                }

                is AuthItem.Basic -> request.basicAuth(authItem.username, authItem.password)
                is AuthItem.Bearer -> request.bearerAuth(authItem.token)
            }
        }
        return applied
    }

    public companion object Plugin : HttpClientPlugin<Config, AuthPlugin> {
        private val LOGGER = KtorSimpleLogger("com.kroegerama.openapi.kmp.gen.companion.AuthPlugin")
        private val authKeysAttribute: AttributeKey<List<String>> = AttributeKey<List<String>>("kgen.auth.keys")
        private val appliedItemsAttribute: AttributeKey<Map<String, AuthItem>> = AttributeKey("kgen.auth.applied")

        public fun HttpRequestBuilder.authKeys(vararg keys: String) {
            attributes[authKeysAttribute] = keys.toList()
        }

        override val key: AttributeKey<AuthPlugin> = AttributeKey("AuthPlugin")

        override fun prepare(block: Config.() -> Unit): AuthPlugin {
            val config = Config().apply(block)
            return AuthPlugin(config.authItemResolver, config.unauthorizedHandler)
        }

        override fun install(plugin: AuthPlugin, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                val authKeys = context.attributes.getOrNull(authKeysAttribute) ?: return@intercept
                LOGGER.trace("Adding auth values for: $authKeys")
                context.attributes.put(appliedItemsAttribute, plugin.applyAuth(context, authKeys))
            }
            val handler = plugin.unauthorizedHandler ?: return
            // Ktor installs HttpCallValidator before user plugins, which makes its send
            // interceptor the outer one: execute() returns a 401 call un-thrown here even
            // with expectSuccess = true, and only the call returned from this interceptor
            // is validated. The retry goes to the next sender, so it cannot loop.
            scope.plugin(HttpSend).intercept { request ->
                val call = execute(request)
                if (call.response.status != HttpStatusCode.Unauthorized) return@intercept call
                val authKeys = request.attributes.getOrNull(authKeysAttribute) ?: return@intercept call
                val applied = request.attributes.getOrNull(appliedItemsAttribute).orEmpty()
                if (!handler(applied)) return@intercept call
                LOGGER.trace("Retrying after 401 with fresh auth values for: $authKeys")
                applied.values.forEach { request.removeAuthItem(it) }
                request.attributes.put(appliedItemsAttribute, plugin.applyAuth(request, authKeys))
                execute(request)
            }
        }

        /** Removes the request values a previous [applyAuth] added for [item]. */
        private fun HttpRequestBuilder.removeAuthItem(item: AuthItem) {
            when (item) {
                is AuthItem.Basic, is AuthItem.Bearer -> headers.remove(HttpHeaders.Authorization)
                is AuthItem.ApiKey -> when (item.position) {
                    AuthItem.Position.Header -> headers.remove(item.name)
                    AuthItem.Position.Query -> url.parameters.remove(item.name)
                    AuthItem.Position.Cookie -> removeCookie(item.name)
                }
            }
        }

        private fun HttpRequestBuilder.removeCookie(name: String) {
            // Request cookies live in a single Cookie header, joined with "; " by
            // HttpMessageBuilder.cookie - other cookies in it must survive the removal.
            val cookieHeader = headers[HttpHeaders.Cookie] ?: return
            val remaining = cookieHeader.split("; ").filterNot { it.substringBefore('=') == name }
            if (remaining.isEmpty()) {
                headers.remove(HttpHeaders.Cookie)
            } else {
                headers[HttpHeaders.Cookie] = remaining.joinToString("; ")
            }
        }
    }
}
