package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.util.*
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.utils.io.*

public typealias AuthItemResolver = suspend (String) -> AuthItem?

public class AuthPlugin private constructor(
    private val authItemResolver: AuthItemResolver
) {
    @KtorDsl
    public class Config(
        internal var authItemResolver: AuthItemResolver = { null }
    ) {
        public fun authItem(resolver: AuthItemResolver) {
            authItemResolver = resolver
        }
    }

    public companion object Plugin : HttpClientPlugin<Config, AuthPlugin> {
        private val LOGGER = KtorSimpleLogger("com.kroegerama.openapi.kmp.gen.companion.AuthPlugin")
        private val authKeysAttribute: AttributeKey<List<String>> = AttributeKey<List<String>>("kgen.auth.keys")

        public fun HttpRequestBuilder.authKeys(vararg keys: String) {
            attributes[authKeysAttribute] = keys.toList()
        }

        override val key: AttributeKey<AuthPlugin> = AttributeKey("AuthPlugin")

        override fun prepare(block: Config.() -> Unit): AuthPlugin {
            val config = Config().apply(block)
            return AuthPlugin(config.authItemResolver)
        }

        override fun install(plugin: AuthPlugin, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                val authKeys = context.attributes.getOrNull(authKeysAttribute) ?: return@intercept
                LOGGER.trace("Adding auth values for: $authKeys")
                authKeys.forEach { authKey ->
                    val authItem = plugin.authItemResolver(authKey) ?: return@forEach
                    when (authItem) {
                        is AuthItem.ApiKey -> when (authItem.position) {
                            AuthItem.Position.Header -> context.header(authItem.name, authItem.value)
                            AuthItem.Position.Query -> context.parameter(authItem.name, authItem.value)
                            AuthItem.Position.Cookie -> context.cookie(authItem.name, authItem.value)
                        }

                        is AuthItem.Basic -> context.basicAuth(authItem.username, authItem.password)
                        is AuthItem.Bearer -> context.bearerAuth(authItem.token)
                    }
                }
            }
        }
    }
}
