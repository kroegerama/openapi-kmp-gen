package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.Cookie
import io.ktor.http.DEFAULT_PORT
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.renderCookieHeader
import io.ktor.util.*
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.pipeline.PipelinePhase
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
 * A 401 from another origin than the one the request was built for, reached through a
 * redirect, is returned without consulting the handler.
 * The retry re-sends the request body, so it must be replayable; a streaming body (e.g. a
 * `ByteReadChannel`) arrives consumed on the retry.
 */
public typealias UnauthorizedHandler = suspend (appliedItems: Map<String, AuthItem>) -> Boolean

/**
 * Applies the [AuthItem]s resolved for a request's [Plugin.authKeys]. When an
 * [UnauthorizedHandler] is configured via [Config.onUnauthorized], a 401 Unauthorized
 * response additionally triggers a single retry with re-resolved auth values. The items are
 * only sent to the origin (scheme, host and port) the request was built for.
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
            // cookies are added by the send pipeline
            request.setAuthItem(authItem, cookies = false)
        }
        return applied
    }

    public companion object Plugin : HttpClientPlugin<Config, AuthPlugin> {
        private val LOGGER = KtorSimpleLogger("com.kroegerama.openapi.kmp.gen.companion.AuthPlugin")
        private val authKeysAttribute: AttributeKey<List<String>> = AttributeKey<List<String>>("kgen.auth.keys")
        private val appliedItemsAttribute: AttributeKey<Map<String, AuthItem>> = AttributeKey("kgen.auth.applied")
        private val appliedOriginAttribute: AttributeKey<Origin> = AttributeKey("kgen.auth.origin")
        private val AuthOriginPhase = PipelinePhase("AuthOrigin")

        private data class Origin(val scheme: String, val host: String, val port: Int)

        private fun URLBuilder.origin(): Origin = Origin(
            scheme = protocol.name,
            host = host.lowercase(),
            port = port.takeUnless { it == DEFAULT_PORT } ?: protocol.defaultPort
        )

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
                context.attributes.put(appliedOriginAttribute, context.url.origin())
                context.attributes.put(appliedItemsAttribute, plugin.applyAuth(context, authKeys))
            }
            // HttpCookies rewrites the Cookie header on every send and HttpRedirect copies the request for each hop, so the applied
            // items are set again after HttpSendPipeline.State for the applied origin and removed for another one.
            scope.sendPipeline.insertPhaseAfter(HttpSendPipeline.State, AuthOriginPhase)
            scope.sendPipeline.intercept(AuthOriginPhase) {
                val items = context.attributes.getOrNull(appliedItemsAttribute)?.values
                if (items.isNullOrEmpty()) return@intercept
                if (context.isSentToAppliedOrigin()) {
                    items.forEach { context.setAuthItem(it, cookies = true) }
                } else {
                    context.removeAuthItemsForAnotherOrigin(items)
                }
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
                if (!request.isSentToAppliedOrigin()) return@intercept call
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

        /**
         * Sets the request values for [item], replacing values of the same name.
         *
         * @param cookies whether a cookie-positioned item is merged into the Cookie header or skipped.
         */
        private fun HttpRequestBuilder.setAuthItem(item: AuthItem, cookies: Boolean) {
            when (item) {
                is AuthItem.ApiKey -> when (item.position) {
                    AuthItem.Position.Header -> headers[item.name] = item.value
                    AuthItem.Position.Query -> url.parameters[item.name] = item.value
                    AuthItem.Position.Cookie -> if (cookies) {
                        removeCookie(item.name)
                        cookie(item.name, item.value)
                    }
                }

                is AuthItem.Basic -> {
                    headers.remove(HttpHeaders.Authorization)
                    basicAuth(item.username, item.password)
                }

                is AuthItem.Bearer -> {
                    headers.remove(HttpHeaders.Authorization)
                    bearerAuth(item.token)
                }
            }
        }

        private fun HttpRequestBuilder.isSentToAppliedOrigin(): Boolean = attributes.getOrNull(appliedOriginAttribute) == url.origin()

        /** Removes the request values applied for [items], keeping cookies of the same name with another value. */
        private fun HttpRequestBuilder.removeAuthItemsForAnotherOrigin(items: Collection<AuthItem>) {
            items.forEach { item ->
                if (item is AuthItem.ApiKey && item.position == AuthItem.Position.Cookie) {
                    val rendered = renderCookieHeader(Cookie(item.name, item.value))
                    removeCookies { it == rendered }
                } else {
                    removeAuthItem(item)
                }
            }
        }

        private fun HttpRequestBuilder.removeCookie(name: String) {
            removeCookies { it.substringBefore('=') == name }
        }

        private inline fun HttpRequestBuilder.removeCookies(predicate: (String) -> Boolean) {
            // Request cookies live in a single Cookie header, joined with "; " by
            // HttpMessageBuilder.cookie - other cookies in it must survive the removal.
            val cookieHeader = headers[HttpHeaders.Cookie] ?: return
            val remaining = cookieHeader.split("; ").filterNot(predicate)
            if (remaining.isEmpty()) {
                headers.remove(HttpHeaders.Cookie)
            } else {
                headers[HttpHeaders.Cookie] = remaining.joinToString("; ")
            }
        }
    }
}
