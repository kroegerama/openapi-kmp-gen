package com.kroegerama.openapi.kmp.gen.companion.keycloak

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KeycloakEndpointsTest {

    @Test
    fun fromRealmWithPlainBaseUrl() {
        val endpoints = KeycloakEndpoints.fromRealm(Url("https://auth.example.com"), "my-realm")

        assertEquals(
            "https://auth.example.com/realms/my-realm/protocol/openid-connect/token",
            endpoints.tokenEndpoint.toString()
        )
        assertEquals(
            "https://auth.example.com/realms/my-realm/protocol/openid-connect/auth",
            endpoints.authorizationEndpoint.toString()
        )
        assertEquals(
            "https://auth.example.com/realms/my-realm/protocol/openid-connect/logout",
            endpoints.logoutEndpoint.toString()
        )
        assertEquals(
            "https://auth.example.com/realms/my-realm/protocol/openid-connect/userinfo",
            endpoints.userInfoEndpoint.toString()
        )
    }

    @Test
    fun fromRealmWithTrailingSlash() {
        val endpoints = KeycloakEndpoints.fromRealm(Url("https://auth.example.com/"), "my-realm")

        assertEquals(
            "https://auth.example.com/realms/my-realm/protocol/openid-connect/token",
            endpoints.tokenEndpoint.toString()
        )
    }

    @Test
    fun fromRealmWithPathPrefix() {
        val endpoints = KeycloakEndpoints.fromRealm(Url("https://example.com/auth/"), "my-realm")

        assertEquals(
            "https://example.com/auth/realms/my-realm/protocol/openid-connect/token",
            endpoints.tokenEndpoint.toString()
        )
    }

    @Test
    fun fromRealmDropsQueryAndFragment() {
        val endpoints = KeycloakEndpoints.fromRealm(Url("https://example.com/auth?foo=bar#frag"), "my-realm")

        assertEquals(
            "https://example.com/auth/realms/my-realm/protocol/openid-connect/token",
            endpoints.tokenEndpoint.toString()
        )
    }

    @Test
    fun wellKnownUrl() {
        val url = KeycloakEndpoints.wellKnownUrl(Url("https://auth.example.com"), "my-realm")

        assertEquals(
            "https://auth.example.com/realms/my-realm/.well-known/openid-configuration",
            url.toString()
        )
    }

    @Test
    fun openIdConfigurationToEndpoints() {
        val configuration = OpenIdConfiguration(
            issuer = "https://auth.example.com/realms/my-realm",
            tokenEndpoint = "https://auth.example.com/realms/my-realm/protocol/openid-connect/token",
            authorizationEndpoint = "https://auth.example.com/realms/my-realm/protocol/openid-connect/auth",
            endSessionEndpoint = "https://auth.example.com/realms/my-realm/protocol/openid-connect/logout",
            userInfoEndpoint = "https://auth.example.com/realms/my-realm/protocol/openid-connect/userinfo"
        )

        val endpoints = configuration.toEndpoints()

        assertEquals(
            "https://auth.example.com/realms/my-realm/protocol/openid-connect/token",
            endpoints.tokenEndpoint.toString()
        )
        assertEquals(
            "https://auth.example.com/realms/my-realm/protocol/openid-connect/auth",
            endpoints.authorizationEndpoint.toString()
        )
        assertEquals(
            "https://auth.example.com/realms/my-realm/protocol/openid-connect/logout",
            endpoints.logoutEndpoint.toString()
        )
        assertEquals(
            "https://auth.example.com/realms/my-realm/protocol/openid-connect/userinfo",
            endpoints.userInfoEndpoint.toString()
        )
    }

    @Test
    fun openIdConfigurationWithoutOptionalEndpoints() {
        val configuration = OpenIdConfiguration(
            issuer = "https://auth.example.com/realms/my-realm",
            tokenEndpoint = "https://auth.example.com/realms/my-realm/protocol/openid-connect/token"
        )

        val endpoints = configuration.toEndpoints()

        assertNull(endpoints.authorizationEndpoint)
        assertNull(endpoints.logoutEndpoint)
        assertNull(endpoints.userInfoEndpoint)
    }
}
