package com.kroegerama.openapi.kmp.gen.companion.keycloak

import androidx.compose.runtime.Immutable
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The Keycloak endpoints used by [Keycloak].
 *
 * @property tokenEndpoint the OAuth 2.0 token endpoint.
 * @property authorizationEndpoint the OAuth 2.0 authorization endpoint used by the
 *   Authorization Code + PKCE flow, or `null` if unknown -
 *   [Keycloak.createAuthorizationRequest] then fails.
 * @property logoutEndpoint the end-session endpoint used for refresh-token logout and
 *   RP-initiated browser logout, or `null` if the server does not expose one -
 *   [Keycloak.logout] then only clears the local state, and [Keycloak.createLogoutRequest]
 *   fails.
 * @property userInfoEndpoint the OpenID Connect userinfo endpoint, or `null` if unknown -
 *   [Keycloak.userInfo] then fails.
 */
@Immutable
public data class KeycloakEndpoints(
    val tokenEndpoint: Url,
    val authorizationEndpoint: Url? = null,
    val logoutEndpoint: Url? = null,
    val userInfoEndpoint: Url? = null
) {
    public companion object {
        /**
         * Builds the endpoints from Keycloak's standard URL layout:
         * `{baseUrl}/realms/{realm}/protocol/openid-connect/{token|auth|logout|userinfo}`.
         *
         * [baseUrl] may contain a path prefix (e.g. a reverse-proxy prefix or the legacy `/auth`);
         * a query or fragment on it is ignored.
         */
        public fun fromRealm(baseUrl: Url, realm: String): KeycloakEndpoints = KeycloakEndpoints(
            tokenEndpoint = realmUrl(baseUrl, realm, "protocol", "openid-connect", "token"),
            authorizationEndpoint = realmUrl(baseUrl, realm, "protocol", "openid-connect", "auth"),
            logoutEndpoint = realmUrl(baseUrl, realm, "protocol", "openid-connect", "logout"),
            userInfoEndpoint = realmUrl(baseUrl, realm, "protocol", "openid-connect", "userinfo")
        )

        /** `{baseUrl}/realms/{realm}/.well-known/openid-configuration` */
        public fun wellKnownUrl(
            baseUrl: Url,
            realm: String
        ): Url = realmUrl(baseUrl, realm, ".well-known", "openid-configuration")

        /** `{baseUrl}/realms/{realm}` - the issuer of the realm. */
        public fun issuerUrl(
            baseUrl: Url,
            realm: String
        ): Url = realmUrl(baseUrl, realm)

        private fun realmUrl(
            baseUrl: Url,
            realm: String,
            vararg segments: String
        ): Url = URLBuilder(baseUrl).apply {
            // Only the origin and path prefix of the base URL contribute to an endpoint;
            // a query or fragment on it must not leak into every derived URL.
            parameters.clear()
            fragment = ""
            // The leading empty segment keeps the path absolute; filtering drops empty
            // segments from trailing slashes so no "//" appears in the result.
            pathSegments = listOf("") + pathSegments.filter { it.isNotEmpty() } + listOf("realms", realm) + segments
        }.build()
    }
}

/**
 * The subset of the OpenID Connect discovery document (`.well-known/openid-configuration`)
 * needed to configure a [Keycloak] instance. Real documents carry many more fields, so the
 * decoding `Json` must ignore unknown keys (see [Keycloak.Companion.discover]).
 */
@Immutable
@Serializable
public data class OpenIdConfiguration(
    @SerialName("issuer")
    val issuer: String,
    @SerialName("token_endpoint")
    val tokenEndpoint: String,
    @SerialName("authorization_endpoint")
    val authorizationEndpoint: String? = null,
    @SerialName("end_session_endpoint")
    val endSessionEndpoint: String? = null,
    @SerialName("userinfo_endpoint")
    val userInfoEndpoint: String? = null
) {
    /**
     * Converts the document's endpoint strings to [KeycloakEndpoints].
     *
     * @throws io.ktor.http.URLParserException when one of the endpoints is not a valid URL.
     */
    public fun toEndpoints(): KeycloakEndpoints = KeycloakEndpoints(
        tokenEndpoint = Url(tokenEndpoint),
        authorizationEndpoint = authorizationEndpoint?.let(::Url),
        logoutEndpoint = endSessionEndpoint?.let(::Url),
        userInfoEndpoint = userInfoEndpoint?.let(::Url)
    )
}
