package com.kroegerama.openapi.kmp.gen.companion.keycloak

import arrow.core.Either
import arrow.core.getOrElse
import com.kroegerama.openapi.kmp.gen.companion.CallException
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.parameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private const val REALM = "test"
private const val PUBLIC_CLIENT = "test-public"
private const val CONFIDENTIAL_CLIENT = "test-confidential"
private const val CLIENT_SECRET = "test-secret"
private const val USERNAME = "tester"
private const val PASSWORD = "password"

/**
 * Tests against a real Keycloak instance provisioned by `companion/keycloak-testenv`.
 * Gated on the `KEYCLOAK_BASE_URL` environment variable (e.g. `http://localhost:8081`);
 * every test silently passes without it, so regular builds do not require Docker.
 */
class KeycloakIntegrationTest {

    private fun integrationTest(block: suspend CoroutineScope.(baseUrl: Url) -> Unit) {
        val baseUrl = System.getenv("KEYCLOAK_BASE_URL") ?: run {
            println("KEYCLOAK_BASE_URL is not set - skipping Keycloak integration test")
            return
        }
        runBlocking { block(Url(baseUrl)) }
    }

    private fun publicKeycloak(
        baseUrl: Url,
        accessTokenLeeway: Duration = Keycloak.DEFAULT_ACCESS_TOKEN_LEEWAY
    ): Keycloak = Keycloak(
        baseUrl = baseUrl,
        realm = REALM,
        clientId = PUBLIC_CLIENT,
        accessTokenLeeway = accessTokenLeeway
    )

    private fun <T> Either<CallException, T>.expectRight(): T = getOrElse { exception ->
        fail("Expected a successful call, got: $exception", exception as? Throwable)
    }

    @Test
    fun discoveryResolvesTheRealmEndpoints() = integrationTest { baseUrl ->
        val keycloak = Keycloak.discover(baseUrl, REALM, PUBLIC_CLIENT).expectRight()
        val expected = KeycloakEndpoints.fromRealm(baseUrl, REALM)
        assertEquals(expected.tokenEndpoint, keycloak.endpoints.tokenEndpoint)
        assertEquals(expected.authorizationEndpoint, keycloak.endpoints.authorizationEndpoint)
        assertEquals(expected.logoutEndpoint, keycloak.endpoints.logoutEndpoint)
    }

    @Test
    fun passwordLoginIssuesSignedJwts() = integrationTest { baseUrl ->
        val keycloak = publicKeycloak(baseUrl)
        val tokens = keycloak.login(USERNAME, PASSWORD).expectRight()
        val accessJwt = assertNotNull(tokens.accessJwt, "The access token is not a parseable JWT")
        assertNotNull(accessJwt.expiresAt, "The access token has no exp claim")
        assertNotNull(tokens.refreshToken)
        assertFalse(tokens.isAccessTokenExpired())
        assertFalse(tokens.isRefreshTokenExpired())
        assertEquals(tokens, keycloak.currentTokens())
    }

    @Test
    fun refreshRotatesTheAccessToken() = integrationTest { baseUrl ->
        val keycloak = publicKeycloak(baseUrl)
        val initial = keycloak.login(USERNAME, PASSWORD).expectRight()
        val refreshed = keycloak.refresh().expectRight()
        assertNotEquals(initial.accessToken, refreshed.accessToken)
        assertEquals(refreshed, keycloak.currentTokens())
    }

    @Test
    fun bearerRefreshesAnExpiringAccessToken() = integrationTest { baseUrl ->
        // The realm's access token lifespan is 30 s; a leeway above that makes every fresh
        // token count as expiring, so bearerOrNull must refresh instead of serving the login
        // token - without having to actually wait out the lifespan.
        val keycloak = publicKeycloak(baseUrl, accessTokenLeeway = 5.minutes)
        val initial = keycloak.login(USERNAME, PASSWORD).expectRight()
        val bearer = assertNotNull(keycloak.bearerOrNull())
        assertNotEquals(initial.accessToken, bearer.token)
        assertEquals(bearer.token, keycloak.currentTokens()?.accessToken)
    }

    @Test
    fun clientCredentialsLoginRenewsWithoutRefreshToken() = integrationTest { baseUrl ->
        val keycloak = Keycloak(
            baseUrl = baseUrl,
            realm = REALM,
            clientId = CONFIDENTIAL_CLIENT,
            clientSecret = CLIENT_SECRET,
            accessTokenLeeway = 5.minutes
        )
        val initial = keycloak.loginClientCredentials().expectRight()
        // Keycloak issues no refresh token for this grant, so the renewal below must go
        // through the relogin fallback.
        assertNull(initial.refreshToken)
        val bearer = assertNotNull(keycloak.bearerOrNull())
        assertNotEquals(initial.accessToken, bearer.token)
    }

    @Test
    fun wrongPasswordSurfacesInvalidGrant() = integrationTest { baseUrl ->
        val keycloak = publicKeycloak(baseUrl)
        val exception = assertNotNull(keycloak.login(USERNAME, "wrong-password").leftOrNull())
        assertEquals("invalid_grant", exception.keycloakErrorOrNull()?.error)
        assertNull(keycloak.currentTokens())
    }

    @Test
    fun wrongClientSecretIsNotAnInvalidGrant() = integrationTest { baseUrl ->
        val keycloak = Keycloak(
            baseUrl = baseUrl,
            realm = REALM,
            clientId = CONFIDENTIAL_CLIENT,
            clientSecret = "wrong-secret"
        )
        val exception = assertNotNull(keycloak.loginClientCredentials().leftOrNull())
        val error = assertNotNull(exception.keycloakErrorOrNull()?.error)
        // The exact code differs between Keycloak versions (invalid_client / unauthorized_client);
        // what matters is that a client misconfiguration is never reported as invalid_grant,
        // which would end a session.
        assertNotEquals("invalid_grant", error)
    }

    @Test
    fun serverSideLogoutEndsTheSessionOnTheNextRefresh() = integrationTest { baseUrl ->
        val keycloak = publicKeycloak(baseUrl)
        val tokens = keycloak.login(USERNAME, PASSWORD).expectRight()
        keycloak.logout().expectRight()
        assertNull(keycloak.currentTokens())

        // Restore the now server-side-invalidated token set: the next refresh is rejected
        // with a real invalid_grant and must end the session.
        keycloak.updateTokens(tokens)
        val events = mutableListOf<KeycloakSessionEndReason>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            keycloak.sessionEnded.collect { events += it }
        }
        val exception = assertNotNull(keycloak.refresh().leftOrNull())
        assertEquals("invalid_grant", exception.keycloakErrorOrNull()?.error)
        assertNull(keycloak.currentTokens())
        yield()
        assertEquals(listOf(KeycloakSessionEndReason.SessionExpired), events)
        collector.cancel()
    }

    @Test
    fun headlessAuthorizationCodeFlowWithPkce() = integrationTest { baseUrl ->
        val keycloak = publicKeycloak(baseUrl)
        val request = keycloak.createAuthorizationRequest(redirectUri = "http://localhost:8123/callback")

        // Keycloak's login page is a plain HTML form, so a cookie-aware client can play the
        // browser: fetch the form, post the credentials, and capture the redirect instead of
        // following it - no real browser or loopback listener needed.
        // Keycloak marks its cookies Secure even over plain http; browsers send them anyway
        // because localhost is a secure context, ktor does not - so drop the flag on storage.
        HttpClient(OkHttp) {
            install(HttpCookies) {
                storage = object : CookiesStorage {
                    private val delegate = AcceptAllCookiesStorage()
                    override suspend fun get(requestUrl: Url) = delegate.get(requestUrl)
                    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) =
                        delegate.addCookie(requestUrl, cookie.copy(secure = false))

                    override fun close() = delegate.close()
                }
            }
            followRedirects = false
        }.use { browser ->
            val loginPage = browser.get(request.url)
            assertEquals(HttpStatusCode.OK, loginPage.status)
            val formAction = assertNotNull(
                Regex("""<form[^>]+action="([^"]+)"""").find(loginPage.bodyAsText())?.groupValues?.get(1),
                "No login form found on the authorization page"
            ).replace("&amp;", "&")

            val redirect = browser.submitForm(
                url = formAction,
                formParameters = parameters {
                    append("username", USERNAME)
                    append("password", PASSWORD)
                }
            )
            assertEquals(HttpStatusCode.Found, redirect.status, "Keycloak did not accept the login")
            val location = assertNotNull(redirect.headers[HttpHeaders.Location])
            assertTrue(request.matchesRedirect(location))

            // The realm forces the S256 challenge method, so a successful exchange also proves
            // the pure-Kotlin SHA-256 produces a code challenge a real server verifies.
            val tokens = keycloak.handleAuthorizationRedirect(request, location).expectRight()
            assertNotNull(tokens.idToken, "The openid scope should yield an ID token")
            assertEquals(tokens.accessToken, keycloak.currentTokens()?.accessToken)
        }
    }
}
