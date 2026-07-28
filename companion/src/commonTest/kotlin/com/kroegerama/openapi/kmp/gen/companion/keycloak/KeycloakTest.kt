package com.kroegerama.openapi.kmp.gen.companion.keycloak

import com.kroegerama.openapi.kmp.gen.companion.HttpCallException
import com.kroegerama.openapi.kmp.gen.companion.UnexpectedCallException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.parseQueryString
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class KeycloakTest {

    private val baseUrl = Url("https://auth.example.com")
    private val tokenUrl = "https://auth.example.com/realms/test/protocol/openid-connect/token"
    private val logoutUrl = "https://auth.example.com/realms/test/protocol/openid-connect/logout"

    private fun createKeycloak(
        engine: MockEngine,
        clientSecret: String? = null,
        tokenLoader: KeycloakTokenLoader? = null,
        tokenListener: KeycloakTokenListener? = null
    ): Keycloak = Keycloak(
        baseUrl = baseUrl,
        realm = "test",
        clientId = "test-client",
        clientSecret = clientSecret,
        tokenLoader = tokenLoader,
        tokenListener = tokenListener,
        httpClient = testHttpClient(engine)
    )

    private suspend fun MockEngine.formBody(index: Int = 0): Parameters =
        parseQueryString(requestHistory[index].body.toByteArray().decodeToString())

    /**
     * Collects [Keycloak.sessionEnded] into the returned list for the rest of the test.
     * The UNDISPATCHED start subscribes the collector before this function returns; callers
     * must `yield()` once after the last expected emission before asserting on the list.
     */
    private fun TestScope.collectSessionEnded(keycloak: Keycloak): List<KeycloakSessionEndReason> {
        val events = mutableListOf<KeycloakSessionEndReason>()
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            keycloak.sessionEnded.collect { events += it }
        }
        return events
    }

    private fun validTokens(refreshToken: String? = "opaque-refresh") = KeycloakTokens(
        accessToken = unsignedJwt(exp = nowEpochSeconds() + 300),
        refreshToken = refreshToken,
        refreshExpiresIn = 1800
    )

    private fun expiredAccessTokens(
        refreshToken: String? = unsignedJwt(exp = nowEpochSeconds() + 1800)
    ) = KeycloakTokens(
        accessToken = unsignedJwt(exp = nowEpochSeconds() - 100),
        refreshToken = refreshToken,
        refreshExpiresIn = 1800
    )

    @Test
    fun passwordLoginSendsFormAndStoresTokens() = runTest {
        val accessToken = unsignedJwt(exp = nowEpochSeconds() + 300)
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals(tokenUrl, request.url.toString())
            respondJson(tokenResponseJson(accessToken, "refresh-1"))
        }
        val listenerCalls = mutableListOf<KeycloakTokens?>()
        val keycloak = createKeycloak(engine, clientSecret = "s3cret", tokenListener = { listenerCalls += it })

        val result = keycloak.login("alice", "pw", scopes = listOf("openid", "profile")) {
            append("totp", "123456")
        }

        assertTrue(result.isRight())
        val body = engine.formBody()
        assertEquals("password", body["grant_type"])
        assertEquals("test-client", body["client_id"])
        assertEquals("s3cret", body["client_secret"])
        assertEquals("alice", body["username"])
        assertEquals("pw", body["password"])
        assertEquals("openid profile", body["scope"])
        assertEquals("123456", body["totp"])

        assertEquals(accessToken, keycloak.tokens.value?.accessToken)
        assertEquals(listOf(accessToken), listenerCalls.map { it?.accessToken })
    }

    @Test
    fun clientCredentialsLoginSendsGrantType() = runTest {
        val engine = MockEngine { respondJson(tokenResponseJson("service-token")) }
        val keycloak = createKeycloak(engine, clientSecret = "s3cret")

        val result = keycloak.loginClientCredentials()

        assertTrue(result.isRight())
        val body = engine.formBody()
        assertEquals("client_credentials", body["grant_type"])
        assertEquals("s3cret", body["client_secret"])
        assertNull(body["username"])
        assertEquals("service-token", keycloak.tokens.value?.accessToken)
    }

    @Test
    fun loginFailureKeepsStateAndExposesError() = runTest {
        val engine = MockEngine {
            respondJson(
                """{"error":"invalid_grant","error_description":"Invalid user credentials"}""",
                HttpStatusCode.Unauthorized
            )
        }
        val listenerCalls = mutableListOf<KeycloakTokens?>()
        val keycloak = createKeycloak(engine, tokenListener = { listenerCalls += it })

        val result = keycloak.login("alice", "wrong")

        val exception = result.leftOrNull()
        assertIs<HttpCallException>(exception)
        assertEquals(401, exception.code)
        assertEquals("invalid_grant", exception.keycloakErrorOrNull()?.error)
        assertNull(keycloak.tokens.value)
        assertTrue(listenerCalls.isEmpty())
    }

    @Test
    fun bearerWithValidTokenNeedsNoNetwork() = runTest {
        val engine = MockEngine { respondJson(tokenResponseJson("unexpected")) }
        val keycloak = createKeycloak(engine)
        val tokens = validTokens()
        keycloak.updateTokens(tokens)

        val bearer = keycloak.bearerOrNull()

        assertEquals(tokens.accessToken, bearer?.token)
        assertTrue(engine.requestHistory.isEmpty())
    }

    @Test
    fun bearerWithExpiredAccessTokenRefreshes() = runTest {
        val newAccessToken = unsignedJwt(exp = nowEpochSeconds() + 300)
        val engine = MockEngine { respondJson(tokenResponseJson(newAccessToken, "refresh-2")) }
        val listenerCalls = mutableListOf<KeycloakTokens?>()
        val keycloak = createKeycloak(engine, tokenListener = { listenerCalls += it })
        val oldTokens = expiredAccessTokens()
        keycloak.updateTokens(oldTokens)

        val bearer = keycloak.bearerOrNull()

        assertEquals(newAccessToken, bearer?.token)
        assertEquals(1, engine.requestHistory.size)
        val body = engine.formBody()
        assertEquals("refresh_token", body["grant_type"])
        assertEquals(oldTokens.refreshToken, body["refresh_token"])
        assertEquals(newAccessToken, keycloak.tokens.value?.accessToken)
        assertEquals(newAccessToken, listenerCalls.last()?.accessToken)
    }

    @Test
    fun bearerRefreshesWithinAccessTokenLeeway() = runTest {
        // Expires in 5s - within the default 10s leeway, so it must be refreshed up front.
        val newAccessToken = unsignedJwt(exp = nowEpochSeconds() + 300)
        val engine = MockEngine { respondJson(tokenResponseJson(newAccessToken, "refresh-2")) }
        val keycloak = createKeycloak(engine)
        keycloak.updateTokens(
            KeycloakTokens(
                accessToken = unsignedJwt(exp = nowEpochSeconds() + 5),
                refreshToken = "opaque-refresh",
                refreshExpiresIn = 1800
            )
        )

        val bearer = keycloak.bearerOrNull()

        assertEquals(newAccessToken, bearer?.token)
        assertEquals(1, engine.requestHistory.size)
        assertEquals("refresh_token", engine.formBody()["grant_type"])
    }

    @Test
    fun clientCredentialsSessionReloginsOnExpiry() = runTest {
        val expiredToken = unsignedJwt(exp = nowEpochSeconds() - 100)
        val newToken = unsignedJwt(exp = nowEpochSeconds() + 300)
        var calls = 0
        val engine = MockEngine {
            calls++
            respondJson(tokenResponseJson(if (calls == 1) expiredToken else newToken))
        }
        val keycloak = createKeycloak(engine, clientSecret = "s3cret")
        assertTrue(keycloak.loginClientCredentials(scopes = listOf("api")).isRight())

        val bearer = keycloak.bearerOrNull()

        assertEquals(newToken, bearer?.token)
        assertEquals(newToken, keycloak.tokens.value?.accessToken)
        assertEquals(2, engine.requestHistory.size)
        val body = engine.formBody(1)
        assertEquals("client_credentials", body["grant_type"])
        assertEquals("api", body["scope"])
    }

    @Test
    fun clientCredentialsReloginFailureKeepsTokens() = runTest {
        val expiredToken = unsignedJwt(exp = nowEpochSeconds() - 100)
        var calls = 0
        val engine = MockEngine {
            calls++
            if (calls == 1) {
                respondJson(tokenResponseJson(expiredToken))
            } else {
                respondJson("""{"error":"invalid_client"}""", HttpStatusCode.Unauthorized)
            }
        }
        val keycloak = createKeycloak(engine, clientSecret = "s3cret")
        assertTrue(keycloak.loginClientCredentials().isRight())

        val bearer = keycloak.bearerOrNull()

        assertNull(bearer)
        assertEquals(expiredToken, keycloak.tokens.value?.accessToken)
    }

    @Test
    fun passwordLoginEndsClientCredentialsRenewal() = runTest {
        val expiredToken = unsignedJwt(exp = nowEpochSeconds() - 100)
        val engine = MockEngine { request ->
            val body = parseQueryString(request.body.toByteArray().decodeToString())
            when (body["grant_type"]) {
                "client_credentials" -> respondJson(tokenResponseJson(unsignedJwt(exp = nowEpochSeconds() + 300)))
                "password" -> respondJson(tokenResponseJson(expiredToken))
                else -> respondJson("""{"error":"unsupported_grant_type"}""", HttpStatusCode.BadRequest)
            }
        }
        val keycloak = createKeycloak(engine, clientSecret = "s3cret")
        val events = collectSessionEnded(keycloak)
        assertTrue(keycloak.loginClientCredentials().isRight())
        assertTrue(keycloak.login("alice", "pw").isRight())

        val bearer = keycloak.bearerOrNull()

        // The password session has no refresh token; the client_credentials relogin of the
        // previous session must not resurrect it - the session ends instead.
        assertNull(bearer)
        assertNull(keycloak.tokens.value)
        assertEquals(2, engine.requestHistory.size)
        yield()
        assertEquals(listOf(KeycloakSessionEndReason.SessionExpired), events)
    }

    @Test
    fun externalTokensWithoutRefreshTokenClearOnExpiry() = runTest {
        val engine = MockEngine { respondJson(tokenResponseJson("unexpected")) }
        val keycloak = createKeycloak(engine)
        keycloak.updateTokens(expiredAccessTokens(refreshToken = null))

        val bearer = keycloak.bearerOrNull()

        assertNull(bearer)
        assertNull(keycloak.tokens.value)
        assertTrue(engine.requestHistory.isEmpty())
    }

    @Test
    fun rejectedRefreshTokenClearsState() = runTest {
        val engine = MockEngine {
            respondJson("""{"error":"invalid_grant"}""", HttpStatusCode.BadRequest)
        }
        val listenerCalls = mutableListOf<KeycloakTokens?>()
        val keycloak = createKeycloak(engine, tokenListener = { listenerCalls += it })
        keycloak.updateTokens(expiredAccessTokens())

        val bearer = keycloak.bearerOrNull()

        assertNull(bearer)
        assertNull(keycloak.tokens.value)
        assertNull(listenerCalls.last())
    }

    @Test
    fun expiredRefreshTokenClearsStateWithoutNetwork() = runTest {
        val engine = MockEngine { respondJson(tokenResponseJson("unexpected")) }
        val keycloak = createKeycloak(engine)
        keycloak.updateTokens(
            expiredAccessTokens(refreshToken = unsignedJwt(exp = nowEpochSeconds() - 100))
        )

        val bearer = keycloak.bearerOrNull()

        assertNull(bearer)
        assertNull(keycloak.tokens.value)
        assertTrue(engine.requestHistory.isEmpty())
    }

    @Test
    fun refreshFailureWithOtherErrorKeepsTokens() = runTest {
        val engine = MockEngine {
            respondJson("""{"error":"invalid_client"}""", HttpStatusCode.Unauthorized)
        }
        val keycloak = createKeycloak(engine)
        val oldTokens = expiredAccessTokens()
        keycloak.updateTokens(oldTokens)

        val bearer = keycloak.bearerOrNull()

        assertNull(bearer)
        assertEquals(oldTokens, keycloak.tokens.value)
    }

    @Test
    fun refreshFailureWithUnreadableBodyKeepsTokens() = runTest {
        // A 400/401 without a Keycloak error body comes from an intermediary (reverse proxy,
        // gateway, VPN portal), not from the token endpoint - it must not end the session.
        val engine = MockEngine {
            respond("<html>Blocked</html>", HttpStatusCode.BadRequest)
        }
        val keycloak = createKeycloak(engine)
        val oldTokens = expiredAccessTokens()
        keycloak.updateTokens(oldTokens)

        val bearer = keycloak.bearerOrNull()

        assertNull(bearer)
        assertEquals(oldTokens, keycloak.tokens.value)
    }

    @Test
    fun refreshFailureWithoutErrorFieldKeepsTokens() = runTest {
        val engine = MockEngine {
            respondJson("{}", HttpStatusCode.BadRequest)
        }
        val keycloak = createKeycloak(engine)
        val oldTokens = expiredAccessTokens()
        keycloak.updateTokens(oldTokens)

        val bearer = keycloak.bearerOrNull()

        assertNull(bearer)
        assertEquals(oldTokens, keycloak.tokens.value)
    }

    @Test
    fun failedLoginKeepsPersistedTokens() = runTest {
        val engine = MockEngine {
            respondJson("""{"error":"invalid_grant"}""", HttpStatusCode.Unauthorized)
        }
        val stored = validTokens()
        val keycloak = createKeycloak(engine, tokenLoader = { stored })

        val result = keycloak.login("alice", "wrong")

        assertTrue(result.isLeft())
        assertEquals(stored, keycloak.tokens.value)
        assertEquals(stored.accessToken, keycloak.bearerOrNull()?.token)
        assertEquals(1, engine.requestHistory.size)
    }

    @Test
    fun transientRefreshFailureKeepsTokens() = runTest {
        val engine = MockEngine { throw IOException("network down") }
        val keycloak = createKeycloak(engine)
        val oldTokens = expiredAccessTokens()
        keycloak.updateTokens(oldTokens)

        val bearer = keycloak.bearerOrNull()

        assertNull(bearer)
        assertEquals(oldTokens, keycloak.tokens.value)
    }

    @Test
    fun concurrentBearerCallsRefreshOnlyOnce() = runTest {
        val newAccessToken = unsignedJwt(exp = nowEpochSeconds() + 300)
        val engine = MockEngine { respondJson(tokenResponseJson(newAccessToken, "refresh-2")) }
        val keycloak = createKeycloak(engine)
        keycloak.updateTokens(expiredAccessTokens())

        val bearers = coroutineScope {
            List(5) { async { keycloak.bearerOrNull() } }.awaitAll()
        }

        assertEquals(1, engine.requestHistory.size)
        bearers.forEach { bearer ->
            assertEquals(newAccessToken, bearer?.token)
        }
    }

    @Test
    fun tokenLoaderRunsOnceAndSkipsListener() = runTest {
        val engine = MockEngine { respondJson(tokenResponseJson("unexpected")) }
        var loaderCalls = 0
        val listenerCalls = mutableListOf<KeycloakTokens?>()
        val tokens = validTokens()
        val keycloak = createKeycloak(
            engine,
            tokenLoader = {
                loaderCalls++
                tokens
            },
            tokenListener = { listenerCalls += it }
        )

        assertEquals(tokens, keycloak.currentTokens())
        assertEquals(tokens.accessToken, keycloak.bearerOrNull()?.token)

        assertEquals(1, loaderCalls)
        assertTrue(listenerCalls.isEmpty())
        assertTrue(engine.requestHistory.isEmpty())
    }

    @Test
    fun isLoggedInRunsLoaderBeforeFirstEmission() = runTest {
        val engine = MockEngine { respondJson("", HttpStatusCode.NoContent) }
        var loaderCalls = 0
        val keycloak = createKeycloak(engine, tokenLoader = {
            loaderCalls++
            validTokens()
        })
        val states = mutableListOf<Boolean>()
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            keycloak.isLoggedIn.collect { states += it }
        }

        yield()
        assertEquals(listOf(true), states)
        assertEquals(1, loaderCalls)

        keycloak.logout()
        yield()
        assertEquals(listOf(true, false), states)
    }

    @Test
    fun isLoggedInWithoutLoaderTracksTokenUpdates() = runTest {
        val engine = MockEngine { respondJson("", HttpStatusCode.NoContent) }
        val keycloak = createKeycloak(engine)
        val states = mutableListOf<Boolean>()
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            keycloak.isLoggedIn.collect { states += it }
        }

        yield()
        keycloak.updateTokens(validTokens())
        yield()
        // A refresh-like token change must not re-emit `true`.
        keycloak.updateTokens(validTokens())
        yield()
        keycloak.updateTokens(null)
        yield()

        assertEquals(listOf(false, true, false), states)
    }

    @Test
    fun rejectedRefreshTokenEmitsSessionExpired() = runTest {
        val engine = MockEngine {
            respondJson("""{"error":"invalid_grant"}""", HttpStatusCode.BadRequest)
        }
        val keycloak = createKeycloak(engine)
        val events = collectSessionEnded(keycloak)
        keycloak.updateTokens(expiredAccessTokens())

        assertNull(keycloak.bearerOrNull())

        yield()
        assertEquals(listOf(KeycloakSessionEndReason.SessionExpired), events)
        assertNull(keycloak.tokens.value)
    }

    @Test
    fun locallyExpiredRefreshTokenEmitsSessionExpiredWithoutNetwork() = runTest {
        val engine = MockEngine { respondJson(tokenResponseJson("unexpected")) }
        val keycloak = createKeycloak(engine)
        val events = collectSessionEnded(keycloak)
        keycloak.updateTokens(
            expiredAccessTokens(refreshToken = unsignedJwt(exp = nowEpochSeconds() - 100))
        )

        assertNull(keycloak.bearerOrNull())

        yield()
        assertEquals(listOf(KeycloakSessionEndReason.SessionExpired), events)
        assertTrue(engine.requestHistory.isEmpty())
    }

    @Test
    fun logoutAndClearingUpdateTokensEmitLogoutReason() = runTest {
        val engine = MockEngine { respondJson("", HttpStatusCode.NoContent) }
        val keycloak = createKeycloak(engine)
        val events = collectSessionEnded(keycloak)

        keycloak.updateTokens(validTokens())
        keycloak.logout()
        keycloak.updateTokens(validTokens())
        keycloak.updateTokens(null)

        yield()
        assertEquals(listOf(KeycloakSessionEndReason.Logout, KeycloakSessionEndReason.Logout), events)
    }

    @Test
    fun sessionEndedIsNotEmittedWithoutAnEndedSession() = runTest {
        val engine = MockEngine {
            respondJson("""{"error":"invalid_client"}""", HttpStatusCode.Unauthorized)
        }
        val keycloak = createKeycloak(engine)
        val events = collectSessionEnded(keycloak)

        // Nothing to end while logged out.
        assertTrue(keycloak.logout().isRight())
        keycloak.updateTokens(null)
        // A transient refresh failure keeps the session alive.
        keycloak.updateTokens(expiredAccessTokens())
        assertNull(keycloak.bearerOrNull())

        yield()
        assertTrue(events.isEmpty(), events.toString())
        assertNotNull(keycloak.tokens.value)
    }

    @Test
    fun throwingTokenLoaderIsRetriedOnNextAccess() = runTest {
        val engine = MockEngine { respondJson(tokenResponseJson("unexpected")) }
        val stored = validTokens()
        var loaderCalls = 0
        val keycloak = createKeycloak(
            engine,
            tokenLoader = {
                loaderCalls++
                if (loaderCalls == 1) throw IllegalStateException("storage unavailable") else stored
            }
        )

        assertNull(keycloak.bearerOrNull())
        assertEquals(stored.accessToken, keycloak.bearerOrNull()?.token)

        assertEquals(2, loaderCalls)
        assertTrue(engine.requestHistory.isEmpty())
    }

    @Test
    fun loaderRetryDoesNotOverwriteExplicitLogin() = runTest {
        val staleTokens = validTokens()
        val freshToken = unsignedJwt(exp = nowEpochSeconds() + 300)
        val engine = MockEngine { respondJson(tokenResponseJson(freshToken)) }
        var loaderCalls = 0
        val keycloak = createKeycloak(
            engine,
            tokenLoader = {
                // Fails during the first access and during the login; would return stale
                // persisted tokens afterwards.
                loaderCalls++
                if (loaderCalls <= 2) throw IllegalStateException("storage unavailable") else staleTokens
            }
        )

        assertNull(keycloak.bearerOrNull())
        assertTrue(keycloak.login("alice", "pw").isRight())

        // The successful login made the in-memory state authoritative: the pending
        // (previously failed) load must not run again and overwrite the fresh tokens.
        assertEquals(freshToken, keycloak.bearerOrNull()?.token)
        assertEquals(freshToken, keycloak.tokens.value?.accessToken)
        assertEquals(2, loaderCalls)
    }

    @Test
    fun loaderRetryDoesNotResurrectLoggedOutSession() = runTest {
        val engine = MockEngine { respondJson("", HttpStatusCode.NoContent) }
        var loaderCalls = 0
        val keycloak = createKeycloak(
            engine,
            tokenLoader = {
                loaderCalls++
                if (loaderCalls == 1) throw IllegalStateException("storage unavailable") else validTokens()
            }
        )

        assertTrue(keycloak.logout().isRight())

        assertNull(keycloak.bearerOrNull())
        assertNull(keycloak.tokens.value)
        assertEquals(1, loaderCalls)
    }

    @Test
    fun negativeAccessTokenLeewayIsRejectedAtConstruction() {
        assertFailsWith<IllegalArgumentException> {
            Keycloak(
                baseUrl = baseUrl,
                realm = "test",
                clientId = "test-client",
                accessTokenLeeway = (-1).seconds,
                httpClient = testHttpClient(MockEngine { respondJson("{}") })
            )
        }
    }

    @Test
    fun rejectedRefreshOfClientCredentialsSessionRelogins() = runTest {
        val expiredToken = unsignedJwt(exp = nowEpochSeconds() - 100)
        val newToken = unsignedJwt(exp = nowEpochSeconds() + 300)
        var loginCalls = 0
        val engine = MockEngine { request ->
            val body = parseQueryString(request.body.toByteArray().decodeToString())
            when (body["grant_type"]) {
                "client_credentials" -> {
                    loginCalls++
                    if (loginCalls == 1) {
                        respondJson(tokenResponseJson(expiredToken, refreshToken = "cc-refresh"))
                    } else {
                        respondJson(tokenResponseJson(newToken))
                    }
                }

                "refresh_token" -> respondJson("""{"error":"invalid_grant"}""", HttpStatusCode.BadRequest)

                else -> respondJson("""{"error":"unsupported_grant_type"}""", HttpStatusCode.BadRequest)
            }
        }
        val keycloak = createKeycloak(engine, clientSecret = "s3cret")
        val events = collectSessionEnded(keycloak)
        assertTrue(keycloak.loginClientCredentials().isRight())

        val bearer = keycloak.bearerOrNull()

        assertEquals(newToken, bearer?.token)
        assertEquals(newToken, keycloak.tokens.value?.accessToken)
        assertEquals(2, loginCalls)
        assertEquals(3, engine.requestHistory.size)
        yield()
        assertTrue(events.isEmpty(), events.toString())
    }

    @Test
    fun refreshOfClientCredentialsSessionWithoutRefreshTokenRelogins() = runTest {
        val firstToken = unsignedJwt(exp = nowEpochSeconds() + 300)
        val newToken = unsignedJwt(exp = nowEpochSeconds() + 600)
        var calls = 0
        val engine = MockEngine {
            calls++
            respondJson(tokenResponseJson(if (calls == 1) firstToken else newToken))
        }
        val keycloak = createKeycloak(engine, clientSecret = "s3cret")
        assertTrue(keycloak.loginClientCredentials(scopes = listOf("api")).isRight())

        val result = keycloak.refresh()

        assertEquals(newToken, result.getOrNull()?.accessToken)
        assertEquals(newToken, keycloak.tokens.value?.accessToken)
        val body = engine.formBody(1)
        assertEquals("client_credentials", body["grant_type"])
        assertEquals("api", body["scope"])
    }

    @Test
    fun refreshWithoutSessionReturnsLeft() = runTest {
        val engine = MockEngine { respondJson(tokenResponseJson("unexpected")) }
        val keycloak = createKeycloak(engine)

        val result = keycloak.refresh()

        assertIs<UnexpectedCallException>(result.leftOrNull())
        assertTrue(engine.requestHistory.isEmpty())
    }

    @Test
    fun throwingListenerDoesNotBreakLogin() = runTest {
        val engine = MockEngine { respondJson(tokenResponseJson("token-1")) }
        val keycloak = createKeycloak(engine, tokenListener = { throw IllegalStateException("disk full") })

        val result = keycloak.login("alice", "pw")

        assertTrue(result.isRight())
        assertEquals("token-1", keycloak.tokens.value?.accessToken)
    }

    @Test
    fun logoutSendsRefreshTokenAndClearsState() = runTest {
        val engine = MockEngine { request ->
            assertEquals(logoutUrl, request.url.toString())
            respondJson("", HttpStatusCode.NoContent)
        }
        val listenerCalls = mutableListOf<KeycloakTokens?>()
        val keycloak = createKeycloak(engine, tokenListener = { listenerCalls += it })
        val tokens = validTokens(refreshToken = "refresh-to-revoke")
        keycloak.updateTokens(tokens)

        val result = keycloak.logout()

        assertTrue(result.isRight())
        assertNull(keycloak.tokens.value)
        assertNull(listenerCalls.last())
        val body = engine.formBody()
        assertEquals("test-client", body["client_id"])
        assertEquals("refresh-to-revoke", body["refresh_token"])
    }

    @Test
    fun logoutClearsStateEvenOnServerError() = runTest {
        val engine = MockEngine { respondJson("", HttpStatusCode.InternalServerError) }
        val keycloak = createKeycloak(engine)
        keycloak.updateTokens(validTokens())

        val result = keycloak.logout()

        assertTrue(result.isLeft())
        assertNull(keycloak.tokens.value)
    }

    @Test
    fun logoutWithoutRefreshTokenSkipsNetwork() = runTest {
        val engine = MockEngine { respondJson("", HttpStatusCode.NoContent) }
        val keycloak = createKeycloak(engine)
        keycloak.updateTokens(validTokens(refreshToken = null))

        val result = keycloak.logout()

        assertTrue(result.isRight())
        assertTrue(engine.requestHistory.isEmpty())
    }

    @Test
    fun exchangeAuthorizationCodeSendsFormAndStoresTokens() = runTest {
        val accessToken = unsignedJwt(exp = nowEpochSeconds() + 300)
        val engine = MockEngine { request ->
            assertEquals(tokenUrl, request.url.toString())
            respondJson(tokenResponseJson(accessToken, "refresh-1"))
        }
        val listenerCalls = mutableListOf<KeycloakTokens?>()
        val keycloak = createKeycloak(engine, clientSecret = "s3cret", tokenListener = { listenerCalls += it })

        val result = keycloak.exchangeAuthorizationCode(
            code = "the-code",
            codeVerifier = "a".repeat(43),
            redirectUri = "myapp://callback"
        )

        assertTrue(result.isRight())
        val body = engine.formBody()
        assertEquals("authorization_code", body["grant_type"])
        assertEquals("the-code", body["code"])
        assertEquals("myapp://callback", body["redirect_uri"])
        assertEquals("a".repeat(43), body["code_verifier"])
        assertEquals("test-client", body["client_id"])
        assertEquals("s3cret", body["client_secret"])
        assertEquals(accessToken, keycloak.tokens.value?.accessToken)
        assertEquals(listOf(accessToken), listenerCalls.map { it?.accessToken })
    }

    @Test
    fun exchangeAuthorizationCodeFailureKeepsState() = runTest {
        val engine = MockEngine {
            respondJson("""{"error":"invalid_grant"}""", HttpStatusCode.BadRequest)
        }
        val listenerCalls = mutableListOf<KeycloakTokens?>()
        val keycloak = createKeycloak(engine, tokenListener = { listenerCalls += it })

        val result = keycloak.exchangeAuthorizationCode("bad-code", "a".repeat(43), "myapp://callback")

        val exception = assertIs<HttpCallException>(result.leftOrNull())
        assertNull(exception.authorizationExceptionOrNull())
        assertNull(keycloak.tokens.value)
        assertTrue(listenerCalls.isEmpty())
    }

    @Test
    fun handleAuthorizationRedirectEndToEnd() = runTest {
        val accessToken = unsignedJwt(exp = nowEpochSeconds() + 300)
        val engine = MockEngine { respondJson(tokenResponseJson(accessToken, "refresh-1")) }
        val keycloak = createKeycloak(engine)
        val request = keycloak.createAuthorizationRequest(redirectUri = "myapp://callback")

        val result = keycloak.handleAuthorizationRedirect(
            request,
            "myapp://callback?state=${request.state}&code=the-code"
        )

        assertEquals(accessToken, result.getOrNull()?.accessToken)
        assertEquals(accessToken, keycloak.tokens.value?.accessToken)
        val body = engine.formBody()
        assertEquals("authorization_code", body["grant_type"])
        assertEquals(request.pkce.codeVerifier, body["code_verifier"])
    }

    @Test
    fun handleAuthorizationRedirectAcceptsUrl() = runTest {
        val accessToken = unsignedJwt(exp = nowEpochSeconds() + 300)
        val engine = MockEngine { respondJson(tokenResponseJson(accessToken, "refresh-1")) }
        val keycloak = createKeycloak(engine)
        val request = keycloak.createAuthorizationRequest(redirectUri = "myapp://callback")

        val result = keycloak.handleAuthorizationRedirect(
            request,
            Url("myapp://callback?state=${request.state}&code=the-code")
        )

        assertEquals(accessToken, result.getOrNull()?.accessToken)
        assertEquals(accessToken, keycloak.tokens.value?.accessToken)
    }

    @Test
    fun handleAuthorizationRedirectSurfacesParseFailureWithoutNetwork() = runTest {
        val engine = MockEngine { respondJson(tokenResponseJson("unexpected")) }
        val keycloak = createKeycloak(engine)
        val request = keycloak.createAuthorizationRequest(redirectUri = "myapp://callback")

        val result = keycloak.handleAuthorizationRedirect(
            request,
            "myapp://callback?state=${request.state}&error=access_denied"
        )

        val exception = assertIs<UnexpectedCallException>(result.leftOrNull())
        assertIs<KeycloakAuthorizationException.AuthorizationError>(exception.cause)
        val authorizationError = assertIs<KeycloakAuthorizationException.AuthorizationError>(
            exception.authorizationExceptionOrNull()
        )
        assertEquals("access_denied", authorizationError.error)
        assertTrue(engine.requestHistory.isEmpty())
        assertNull(keycloak.tokens.value)
    }

    @Test
    fun handleAuthorizationRedirectRejectsUnparseableUrlWithoutNetwork() = runTest {
        val engine = MockEngine { respondJson(tokenResponseJson("unexpected")) }
        val keycloak = createKeycloak(engine)
        val request = keycloak.createAuthorizationRequest(redirectUri = "myapp://callback")

        val result = keycloak.handleAuthorizationRedirect(request, "https://example.com:badport/callback")

        val exception = assertIs<UnexpectedCallException>(result.leftOrNull())
        assertIs<KeycloakAuthorizationException.InvalidRedirectUrl>(exception.authorizationExceptionOrNull())
        assertTrue(engine.requestHistory.isEmpty())
        assertNull(keycloak.tokens.value)
    }

    @Test
    fun discoverResolvesEndpointsFromWellKnown() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals(
                "https://auth.example.com/realms/test/.well-known/openid-configuration",
                request.url.toString()
            )
            respondJson(
                """
                {
                    "issuer": "https://auth.example.com/realms/test",
                    "token_endpoint": "https://auth.example.com/custom/token",
                    "end_session_endpoint": "https://auth.example.com/custom/logout",
                    "authorization_endpoint": "https://auth.example.com/custom/auth"
                }
                """.trimIndent()
            )
        }

        val result = Keycloak.discover(
            baseUrl = baseUrl,
            realm = "test",
            clientId = "test-client",
            httpClient = testHttpClient(engine)
        )

        val keycloak = assertNotNull(result.getOrNull())
        assertEquals("https://auth.example.com/custom/token", keycloak.endpoints.tokenEndpoint.toString())
        assertEquals("https://auth.example.com/custom/auth", keycloak.endpoints.authorizationEndpoint.toString())
        assertEquals("https://auth.example.com/custom/logout", keycloak.endpoints.logoutEndpoint.toString())
    }

    @Test
    fun discoverRejectsIssuerMismatch() = runTest {
        val engine = MockEngine {
            respondJson(
                """
                {
                    "issuer": "https://evil.example.com/realms/test",
                    "token_endpoint": "https://auth.example.com/custom/token"
                }
                """.trimIndent()
            )
        }

        val result = Keycloak.discover(
            baseUrl = baseUrl,
            realm = "test",
            clientId = "test-client",
            httpClient = testHttpClient(engine)
        )

        val exception = assertIs<UnexpectedCallException>(result.leftOrNull())
        assertTrue("issuer" in (exception.message ?: ""), exception.message ?: "")
    }

    @Test
    fun discoverRejectsUnparseableIssuer() = runTest {
        val engine = MockEngine {
            respondJson(
                """
                {
                    "issuer": "https://auth.example.com:badport/realms/test",
                    "token_endpoint": "https://auth.example.com/custom/token"
                }
                """.trimIndent()
            )
        }

        val result = Keycloak.discover(
            baseUrl = baseUrl,
            realm = "test",
            clientId = "test-client",
            httpClient = testHttpClient(engine)
        )

        val exception = assertIs<UnexpectedCallException>(result.leftOrNull())
        assertTrue("not a valid URL" in (exception.message ?: ""), exception.message ?: "")
    }

    @Test
    fun discoverRejectsMalformedEndpointUrl() = runTest {
        val engine = MockEngine {
            respondJson(
                """
                {
                    "issuer": "https://auth.example.com/realms/test",
                    "token_endpoint": "https://auth.example.com:badport/token"
                }
                """.trimIndent()
            )
        }

        val result = Keycloak.discover(
            baseUrl = baseUrl,
            realm = "test",
            clientId = "test-client",
            httpClient = testHttpClient(engine)
        )

        val exception = assertIs<UnexpectedCallException>(result.leftOrNull())
        assertTrue("endpoint" in (exception.message ?: ""), exception.message ?: "")
    }

    @Test
    fun discoverFailureReturnsLeft() = runTest {
        val engine = MockEngine { respondJson("", HttpStatusCode.NotFound) }

        val result = Keycloak.discover(
            baseUrl = baseUrl,
            realm = "test",
            clientId = "test-client",
            httpClient = testHttpClient(engine)
        )

        assertTrue(result.isLeft())
    }

    @Test
    fun discoverFailureKeepsCallerProvidedClientOpen() = runTest {
        var calls = 0
        val engine = MockEngine {
            calls++
            if (calls == 1) {
                respondJson("", HttpStatusCode.NotFound)
            } else {
                respondJson(tokenResponseJson("token-1"))
            }
        }
        val client = testHttpClient(engine)

        val result = Keycloak.discover(
            baseUrl = baseUrl,
            realm = "test",
            clientId = "test-client",
            httpClient = client
        )

        assertTrue(result.isLeft())
        // A caller-provided client stays with the caller and must remain usable for a retry.
        val keycloak = Keycloak(
            baseUrl = baseUrl,
            realm = "test",
            clientId = "test-client",
            httpClient = client
        )
        assertTrue(keycloak.login("alice", "pw").isRight())
    }

    @Test
    fun errorBodyStaysReadableAfterClientClose() = runTest {
        // discover closes a self-created client before the caller sees the Left; the saved
        // error response is buffered in memory and must stay readable regardless.
        val engine = MockEngine {
            respondJson(
                """{"error":"invalid_grant","error_description":"Invalid user credentials"}""",
                HttpStatusCode.Unauthorized
            )
        }
        val client = testHttpClient(engine)
        val keycloak = Keycloak(
            baseUrl = baseUrl,
            realm = "test",
            clientId = "test-client",
            httpClient = client
        )

        val result = keycloak.login("alice", "wrong")
        client.close()

        val exception = assertIs<HttpCallException>(result.leftOrNull())
        val error = exception.keycloakErrorOrNull()
        assertEquals("invalid_grant", error?.error)
        assertEquals("Invalid user credentials", error?.errorDescription)
    }
}
