package com.kroegerama.openapi.kmp.gen.companion.keycloak

import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KeycloakAuthorizationTest {

    private fun createKeycloak(clientSecret: String? = null): Keycloak = Keycloak(
        baseUrl = Url("https://auth.example.com"),
        realm = "test",
        clientId = "test-client",
        clientSecret = clientSecret,
        httpClient = testHttpClient(MockEngine { respondJson("{}") })
    )

    private suspend fun createRequest(): AuthorizationRequest =
        createKeycloak().createAuthorizationRequest(redirectUri = "myapp://callback")

    @Test
    fun authorizationUrlContainsAllParameters() = runTest {
        val request = createKeycloak(clientSecret = "s3cret").createAuthorizationRequest(
            redirectUri = "myapp://callback",
            scopes = listOf("openid", "profile")
        ) {
            append("prompt", "login")
        }

        val url = request.url
        assertTrue(
            url.toString().startsWith("https://auth.example.com/realms/test/protocol/openid-connect/auth?"),
            url.toString()
        )
        val parameters = url.parameters
        assertEquals("code", parameters["response_type"])
        assertEquals("test-client", parameters["client_id"])
        assertEquals("myapp://callback", parameters["redirect_uri"])
        assertEquals("openid profile", parameters["scope"])
        assertEquals(request.state, parameters["state"])
        assertEquals(request.pkce.codeChallenge, parameters["code_challenge"])
        assertEquals("S256", parameters["code_challenge_method"])
        assertEquals("login", parameters["prompt"])
        assertNull(parameters["client_secret"])
    }

    @Test
    fun restoredPkceAndStateRebuildIdenticalRequest() = runTest {
        val keycloak = createKeycloak()
        val pkce = Pkce.generate()

        val first = keycloak.createAuthorizationRequest("myapp://callback", pkce = pkce, state = "fixed-state")
        val second = keycloak.createAuthorizationRequest("myapp://callback", pkce = pkce, state = "fixed-state")

        assertEquals(first, second)
    }

    @Test
    fun missingAuthorizationEndpointThrows() = runTest {
        val keycloak = Keycloak(
            clientId = "test-client",
            endpoints = KeycloakEndpoints(tokenEndpoint = Url("https://auth.example.com/token")),
            httpClient = testHttpClient(MockEngine { respondJson("{}") })
        )

        assertFailsWith<IllegalStateException> {
            keycloak.createAuthorizationRequest("myapp://callback")
        }
    }

    @Test
    fun invalidRedirectUriIsRejectedAtCreation() = runTest {
        assertFailsWith<IllegalArgumentException> {
            createKeycloak().createAuthorizationRequest(redirectUri = "https://example.com:badport/callback")
        }
    }

    @Test
    fun parseRedirectExtractsCode() = runTest {
        val request = createRequest()

        val https = request.parseRedirect(Url("https://app.example.com/callback?state=${request.state}&code=the-code"))
        val customScheme = request.parseRedirect("myapp://callback?code=the-code&state=${request.state}&session_state=abc")

        assertEquals("the-code", https.getOrNull())
        assertEquals("the-code", customScheme.getOrNull())
    }

    @Test
    fun parseRedirectRejectsWrongState() = runTest {
        val request = createRequest()

        val result = request.parseRedirect("myapp://callback?code=the-code&state=evil")

        val exception = assertIs<KeycloakAuthorizationException.StateMismatch>(result.leftOrNull())
        assertEquals(request.state, exception.expectedState)
        assertEquals("evil", exception.actualState)
    }

    @Test
    fun parseRedirectRejectsMissingState() = runTest {
        val request = createRequest()

        val result = request.parseRedirect("myapp://callback?code=the-code")

        val exception = assertIs<KeycloakAuthorizationException.StateMismatch>(result.leftOrNull())
        assertNull(exception.actualState)
    }

    @Test
    fun parseRedirectSurfacesAuthorizationError() = runTest {
        val request = createRequest()

        val result = request.parseRedirect(
            "myapp://callback?state=${request.state}&error=access_denied&error_description=User+cancelled"
        )

        val exception = assertIs<KeycloakAuthorizationException.AuthorizationError>(result.leftOrNull())
        assertEquals("access_denied", exception.error)
        assertEquals("User cancelled", exception.errorDescription)
    }

    @Test
    fun stateIsValidatedBeforeError() = runTest {
        val request = createRequest()

        val result = request.parseRedirect("myapp://callback?state=evil&error=access_denied")

        assertIs<KeycloakAuthorizationException.StateMismatch>(result.leftOrNull())
    }

    @Test
    fun parseRedirectRejectsUnparseableUrl() = runTest {
        val request = createRequest()

        val result = request.parseRedirect("https://example.com:badport/callback")

        assertIs<KeycloakAuthorizationException.InvalidRedirectUrl>(result.leftOrNull())
    }

    @Test
    fun parseRedirectWithoutCodeOrError() = runTest {
        val request = createRequest()

        val result = request.parseRedirect("myapp://callback?state=${request.state}")

        assertIs<KeycloakAuthorizationException.MissingCode>(result.leftOrNull())
    }

    @Test
    fun matchesRedirectComparesSchemeHostPortAndPath() = runTest {
        val request = createKeycloak().createAuthorizationRequest(
            redirectUri = "https://app.example.com/callback"
        )

        assertTrue(request.matchesRedirect("https://app.example.com/callback?code=abc&state=xyz"))
        assertTrue(request.matchesRedirect(Url("https://app.example.com/callback#fragment")))
        assertFalse(request.matchesRedirect("https://evil.example.com/callback?code=abc"))
        assertFalse(request.matchesRedirect("http://app.example.com/callback?code=abc"))
        assertFalse(request.matchesRedirect("https://app.example.com/other?code=abc"))
        assertFalse(request.matchesRedirect("https://app.example.com:8443/callback"))
    }

    @Test
    fun matchesRedirectNormalizesTrailingSlashByDefault() = runTest {
        val request = createKeycloak().createAuthorizationRequest(
            redirectUri = "https://app.example.com/callback"
        )

        assertTrue(request.matchesRedirect("https://app.example.com/callback/?code=abc"))
        assertFalse(request.matchesRedirect("https://app.example.com/callback/?code=abc", normalizePath = false))
        assertFalse(request.matchesRedirect("https://app.example.com/callback2?code=abc"))
    }

    @Test
    fun matchesRedirectWithCustomScheme() = runTest {
        val request = createRequest()

        assertTrue(request.matchesRedirect("myapp://callback?code=the-code&state=abc"))
        assertFalse(request.matchesRedirect("otherapp://callback?code=the-code"))
        assertFalse(request.matchesRedirect("http://[invalid"))
    }

    @Test
    fun authorizationRequestSerializationRoundTrip() = runTest {
        val json = createDefaultJson()
        val request = createRequest()

        val restored = json.decodeFromString<AuthorizationRequest>(json.encodeToString(request))

        assertEquals(request, restored)
        assertEquals(request.pkce.codeChallenge, restored.pkce.codeChallenge)
    }
}
