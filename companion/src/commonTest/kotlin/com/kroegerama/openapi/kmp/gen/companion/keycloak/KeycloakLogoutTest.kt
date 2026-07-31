package com.kroegerama.openapi.kmp.gen.companion.keycloak

import com.kroegerama.openapi.kmp.gen.companion.UnexpectedCallException
import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class KeycloakLogoutTest {

    private fun loggedInTokens(withIdToken: Boolean = true) = KeycloakTokens(
        accessToken = unsignedJwt(exp = nowEpochSeconds() + 300),
        refreshToken = "opaque-refresh",
        idToken = if (withIdToken) unsignedJwt(exp = nowEpochSeconds() + 300, sub = "user-1") else null
    )

    private fun createKeycloak(
        tokenLoader: KeycloakTokenLoader? = null,
        tokenListener: KeycloakTokenListener? = null
    ): Keycloak = Keycloak(
        baseUrl = Url("https://auth.example.com"),
        realm = "test",
        clientId = "test-client",
        tokenLoader = tokenLoader,
        tokenListener = tokenListener,
        httpClient = testHttpClient(MockEngine { fail("No request expected") })
    )

    private suspend fun createRequest(): LogoutRequest = createKeycloak(tokenLoader = { loggedInTokens() })
        .createLogoutRequest(postLogoutRedirectUri = "http://localhost:8123/logged-out")

    @Test
    fun logoutUrlContainsAllParameters() = runTest {
        val tokens = loggedInTokens()
        val request = createKeycloak(tokenLoader = { tokens }).createLogoutRequest(
            postLogoutRedirectUri = "http://localhost:8123/logged-out"
        ) {
            append("ui_locales", "de")
        }

        val url = request.url
        assertTrue(
            url.toString().startsWith("https://auth.example.com/realms/test/protocol/openid-connect/logout?"),
            url.toString()
        )
        val parameters = url.parameters
        assertEquals("test-client", parameters["client_id"])
        assertEquals(tokens.idToken, parameters["id_token_hint"])
        assertEquals("http://localhost:8123/logged-out", parameters["post_logout_redirect_uri"])
        assertEquals(request.state, parameters["state"])
        assertEquals("de", parameters["ui_locales"])
    }

    @Test
    fun withoutRedirectUriOnlyClientAndHintAreSent() = runTest {
        val request = createKeycloak(tokenLoader = { loggedInTokens() }).createLogoutRequest()

        val parameters = request.url.parameters
        assertEquals("test-client", parameters["client_id"])
        assertNotNull(parameters["id_token_hint"])
        assertNull(parameters["post_logout_redirect_uri"])
        assertNull(parameters["state"])
        assertNull(request.postLogoutRedirectUri)
    }

    @Test
    fun withoutIdTokenTheHintIsOmitted() = runTest {
        val request = createKeycloak(tokenLoader = { loggedInTokens(withIdToken = false) })
            .createLogoutRequest(postLogoutRedirectUri = "myapp://logged-out")

        assertNull(request.url.parameters["id_token_hint"])
        assertEquals("test-client", request.url.parameters["client_id"])
    }

    @Test
    fun whileLoggedOutTheHintIsOmitted() = runTest {
        val request = createKeycloak().createLogoutRequest()

        assertNull(request.url.parameters["id_token_hint"])
    }

    @Test
    fun fixedStateRebuildsIdenticalRequest() = runTest {
        val tokens = loggedInTokens()
        val keycloak = createKeycloak(tokenLoader = { tokens })

        val first = keycloak.createLogoutRequest("myapp://logged-out", state = "fixed-state")
        val second = keycloak.createLogoutRequest("myapp://logged-out", state = "fixed-state")

        assertEquals(first, second)
    }

    @Test
    fun missingLogoutEndpointThrows() = runTest {
        val keycloak = Keycloak(
            clientId = "test-client",
            endpoints = KeycloakEndpoints(tokenEndpoint = Url("https://auth.example.com/token")),
            httpClient = testHttpClient(MockEngine { fail("No request expected") })
        )

        assertFailsWith<IllegalStateException> {
            keycloak.createLogoutRequest()
        }
    }

    @Test
    fun invalidPostLogoutRedirectUriIsRejectedAtCreation() = runTest {
        assertFailsWith<IllegalArgumentException> {
            createKeycloak().createLogoutRequest(postLogoutRedirectUri = "https://example.com:badport/logged-out")
        }
    }

    @Test
    fun parseRedirectValidatesState() = runTest {
        val request = createRequest()

        assertTrue(request.parseRedirect("http://localhost:8123/logged-out?state=${request.state}").isRight())

        val wrongState = request.parseRedirect(Url("http://localhost:8123/logged-out?state=evil"))
        val exception = assertIs<KeycloakAuthorizationException.StateMismatch>(wrongState.leftOrNull())
        assertEquals(request.state, exception.expectedState)
        assertEquals("evil", exception.actualState)

        val missingState = request.parseRedirect("http://localhost:8123/logged-out")
        assertNull(assertIs<KeycloakAuthorizationException.StateMismatch>(missingState.leftOrNull()).actualState)
    }

    @Test
    fun parseRedirectRejectsUnparseableUrl() = runTest {
        val request = createRequest()

        val result = request.parseRedirect("https://example.com:badport/logged-out")

        assertIs<KeycloakAuthorizationException.InvalidRedirectUrl>(result.leftOrNull())
    }

    @Test
    fun matchesRedirectComparesSchemeHostPortAndPath() = runTest {
        val request = createRequest()

        assertTrue(request.matchesRedirect("http://localhost:8123/logged-out?state=xyz"))
        assertTrue(request.matchesRedirect(Url("http://localhost:8123/logged-out/")))
        assertFalse(request.matchesRedirect("https://localhost:8123/logged-out"))
        assertFalse(request.matchesRedirect("http://localhost:8124/logged-out"))
        assertFalse(request.matchesRedirect("http://localhost:8123/other"))
        assertFalse(request.matchesRedirect("http://[invalid"))
    }

    @Test
    fun matchesRedirectWithoutRedirectUriIsAlwaysFalse() = runTest {
        val request = createKeycloak(tokenLoader = { loggedInTokens() }).createLogoutRequest()

        assertFalse(request.matchesRedirect("http://localhost:8123/logged-out"))
    }

    @Test
    fun handleLogoutRedirectClearsTheLocalState() = runTest {
        var persisted: KeycloakTokens? = loggedInTokens()
        val keycloak = createKeycloak(
            tokenLoader = { persisted },
            tokenListener = { persisted = it }
        )
        val request = keycloak.createLogoutRequest(postLogoutRedirectUri = "http://localhost:8123/logged-out")
        val events = mutableListOf<KeycloakSessionEndReason>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            keycloak.sessionEnded.collect { events += it }
        }

        val result = keycloak.handleLogoutRedirect(request, "http://localhost:8123/logged-out?state=${request.state}")

        assertTrue(result.isRight())
        assertNull(keycloak.currentTokens())
        assertNull(persisted)
        yield()
        assertEquals(listOf(KeycloakSessionEndReason.Logout), events)
        collector.cancel()
    }

    @Test
    fun handleLogoutRedirectStateMismatchKeepsTheTokens() = runTest {
        val tokens = loggedInTokens()
        val keycloak = createKeycloak(tokenLoader = { tokens })
        val request = keycloak.createLogoutRequest(postLogoutRedirectUri = "http://localhost:8123/logged-out")

        val result = keycloak.handleLogoutRedirect(request, Url("http://localhost:8123/logged-out?state=evil"))

        val exception = assertIs<UnexpectedCallException>(result.leftOrNull())
        assertIs<KeycloakAuthorizationException.StateMismatch>(exception.authorizationExceptionOrNull())
        assertEquals(tokens, keycloak.currentTokens())
    }

    @Test
    fun logoutRequestSerializationRoundTrip() = runTest {
        val json = createDefaultJson()
        val request = createRequest()

        val restored = json.decodeFromString<LogoutRequest>(json.encodeToString(request))

        assertEquals(request, restored)
        assertTrue(restored.matchesRedirect("http://localhost:8123/logged-out"))
    }
}
