package com.kroegerama.openapi.kmp.gen.companion.keycloak

import com.kroegerama.openapi.kmp.gen.companion.HttpCallException
import com.kroegerama.openapi.kmp.gen.companion.UnexpectedCallException
import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.parseQueryString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class KeycloakDeviceAuthorizationTest {

    private val baseUrl = Url("https://auth.example.com")
    private val deviceUrl = "https://auth.example.com/realms/test/protocol/openid-connect/auth/device"
    private val verificationUri = "https://auth.example.com/realms/test/device"
    private val verificationUriComplete = "$verificationUri?user_code=ABCD-EFGH"

    private fun createKeycloak(
        engine: MockEngine,
        clientSecret: String? = null,
        tokenListener: KeycloakTokenListener? = null
    ): Keycloak = Keycloak(
        baseUrl = baseUrl,
        realm = "test",
        clientId = "test-client",
        clientSecret = clientSecret,
        tokenListener = tokenListener,
        httpClient = testHttpClient(engine)
    )

    private suspend fun MockEngine.formBody(index: Int = 0): Parameters =
        parseQueryString(requestHistory[index].body.toByteArray().decodeToString())

    private fun deviceResponseJson(interval: Long? = 5): String = buildString {
        append("""{"device_code":"device-code-1","user_code":"ABCD-EFGH",""")
        append(""""verification_uri":"$verificationUri",""")
        append(""""verification_uri_complete":"$verificationUriComplete",""")
        append(""""expires_in":600""")
        if (interval != null) {
            append(""","interval":$interval""")
        }
        append("}")
    }

    private fun deviceAuthorization(
        interval: Long = 5,
        expiresIn: Long = 600
    ) = DeviceAuthorization(
        deviceCode = "device-code-1",
        userCode = "ABCD-EFGH",
        verificationUri = verificationUri,
        verificationUriComplete = verificationUriComplete,
        expiresIn = expiresIn,
        interval = interval
    )

    private fun errorJson(error: String): String = """{"error":"$error"}"""

    @Test
    fun startSendsFormAndParsesResponse() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals(deviceUrl, request.url.toString())
            respondJson(deviceResponseJson())
        }
        val keycloak = createKeycloak(engine)

        val result = keycloak.startDeviceAuthorization(scopes = listOf("openid", "profile")) {
            append("custom", "value")
        }

        val authorization = assertNotNull(result.getOrNull())
        assertEquals("device-code-1", authorization.deviceCode)
        assertEquals("ABCD-EFGH", authorization.userCode)
        assertEquals(verificationUri, authorization.verificationUri)
        assertEquals(verificationUriComplete, authorization.verificationUriComplete)
        assertEquals(600, authorization.expiresIn)
        assertEquals(5, authorization.interval)

        val body = engine.formBody()
        assertEquals("test-client", body["client_id"])
        assertNull(body["client_secret"])
        assertNull(body["code_challenge"])
        assertEquals("openid profile", body["scope"])
        assertEquals("value", body["custom"])
    }

    @Test
    fun deviceFlowWithPkceSendsChallengeAndVerifier() = runTest {
        var polls = 0
        val engine = MockEngine { request ->
            if (request.url.toString() == deviceUrl) {
                respondJson(deviceResponseJson())
            } else {
                when (polls++) {
                    0 -> respondJson(errorJson("authorization_pending"), HttpStatusCode.BadRequest)
                    else -> respondJson(tokenResponseJson("access-1"))
                }
            }
        }
        val keycloak = createKeycloak(engine)
        val pkce = Pkce.generate()

        val authorization = assertNotNull(keycloak.startDeviceAuthorization(pkce = pkce).getOrNull())
        assertEquals(pkce, authorization.pkce)
        val startBody = engine.formBody(0)
        assertEquals(pkce.codeChallenge, startBody["code_challenge"])
        assertEquals(Pkce.CHALLENGE_METHOD_S256, startBody["code_challenge_method"])

        val result = keycloak.awaitDeviceAuthorization(authorization)

        assertTrue(result.isRight())
        repeat(2) { index ->
            assertEquals(pkce.codeVerifier, engine.formBody(index + 1)["code_verifier"])
        }
    }

    @Test
    fun startWithSecretIncludesClientSecret() = runTest {
        val engine = MockEngine { respondJson(deviceResponseJson()) }
        val keycloak = createKeycloak(engine, clientSecret = "s3cret")

        val result = keycloak.startDeviceAuthorization()

        assertTrue(result.isRight())
        assertEquals("s3cret", engine.formBody()["client_secret"])
    }

    @Test
    fun startAppliesTheIntervalDefault() = runTest {
        val engine = MockEngine { respondJson(deviceResponseJson(interval = null)) }
        val keycloak = createKeycloak(engine)

        val result = keycloak.startDeviceAuthorization()

        assertEquals(5, result.getOrNull()?.interval)
    }

    @Test
    fun startFailsWithoutDeviceEndpoint() = runTest {
        val engine = MockEngine { respondJson(deviceResponseJson()) }
        val keycloak = Keycloak(
            clientId = "test-client",
            endpoints = KeycloakEndpoints(
                tokenEndpoint = Url("https://auth.example.com/realms/test/protocol/openid-connect/token")
            ),
            httpClient = testHttpClient(engine)
        )

        val result = keycloak.startDeviceAuthorization()

        assertIs<UnexpectedCallException>(result.leftOrNull())
        assertTrue(engine.requestHistory.isEmpty())
    }

    @Test
    fun awaitPollsUntilApprovedAndStoresTokens() = runTest {
        val accessToken = unsignedJwt(exp = nowEpochSeconds() + 300)
        var polls = 0
        val engine = MockEngine {
            when (polls++) {
                0, 1 -> respondJson(errorJson("authorization_pending"), HttpStatusCode.BadRequest)
                else -> respondJson(tokenResponseJson(accessToken, "refresh-1"))
            }
        }
        val listenerCalls = mutableListOf<KeycloakTokens?>()
        val keycloak = createKeycloak(engine, tokenListener = { listenerCalls += it })

        val result = keycloak.awaitDeviceAuthorization(deviceAuthorization())

        assertTrue(result.isRight())
        assertEquals(3, polls)
        assertEquals(15_000, currentTime)
        repeat(3) { index ->
            val body = engine.formBody(index)
            assertEquals("urn:ietf:params:oauth:grant-type:device_code", body["grant_type"])
            assertEquals("device-code-1", body["device_code"])
            assertEquals("test-client", body["client_id"])
        }
        assertEquals(accessToken, keycloak.tokens.value?.accessToken)
        assertEquals(listOf(accessToken), listenerCalls.map { it?.accessToken })
    }

    @Test
    fun awaitRespectsSlowDown() = runTest {
        var polls = 0
        val engine = MockEngine {
            when (polls++) {
                0 -> respondJson(errorJson("slow_down"), HttpStatusCode.BadRequest)
                else -> respondJson(tokenResponseJson("access-1"))
            }
        }
        val keycloak = createKeycloak(engine)

        val result = keycloak.awaitDeviceAuthorization(deviceAuthorization())

        assertTrue(result.isRight())
        assertEquals(2, polls)
        // First poll after the initial 5s interval; slow_down raises it to 10s, so the
        // second poll lands at 15s of virtual time.
        assertEquals(15_000, currentTime)
    }

    @Test
    fun awaitAccessDeniedIsTerminal() = runTest {
        val engine = MockEngine {
            respondJson(errorJson("access_denied"), HttpStatusCode.BadRequest)
        }
        val listenerCalls = mutableListOf<KeycloakTokens?>()
        val keycloak = createKeycloak(engine, tokenListener = { listenerCalls += it })

        val result = keycloak.awaitDeviceAuthorization(deviceAuthorization())

        val exception = result.leftOrNull()
        assertIs<HttpCallException>(exception)
        assertEquals("access_denied", exception.keycloakErrorOrNull()?.error)
        assertEquals(1, engine.requestHistory.size)
        assertNull(keycloak.currentTokens())
        assertTrue(listenerCalls.isEmpty())
    }

    @Test
    fun awaitExpiredTokenIsTerminal() = runTest {
        val engine = MockEngine {
            respondJson(errorJson("expired_token"), HttpStatusCode.BadRequest)
        }
        val keycloak = createKeycloak(engine)

        val result = keycloak.awaitDeviceAuthorization(deviceAuthorization())

        val exception = result.leftOrNull()
        assertIs<HttpCallException>(exception)
        assertEquals("expired_token", exception.keycloakErrorOrNull()?.error)
        assertEquals(1, engine.requestHistory.size)
    }

    @Test
    fun awaitTransportErrorIsTerminal() = runTest {
        var polls = 0
        val engine = MockEngine {
            polls++
            throw IOException("connection reset")
        }
        val keycloak = createKeycloak(engine)

        val result = keycloak.awaitDeviceAuthorization(deviceAuthorization())

        assertTrue(result.isLeft())
        assertEquals(1, polls)
    }

    @Test
    fun awaitStopsWhenServerKeepsPendingPastExpiry() = runTest {
        val engine = MockEngine {
            respondJson(errorJson("authorization_pending"), HttpStatusCode.BadRequest)
        }
        val keycloak = createKeycloak(engine)
        // The deadline uses the real clock, not the virtual test time - fabricate an
        // authorization that is already expired locally.
        val authorization = deviceAuthorization(expiresIn = 60).copy(
            obtainedAt = Clock.System.now() - 1.hours
        )

        val result = keycloak.awaitDeviceAuthorization(authorization)

        assertIs<UnexpectedCallException>(result.leftOrNull())
        assertEquals(1, engine.requestHistory.size)
    }

    @Test
    fun awaitDecoratorAppendsParameters() = runTest {
        var polls = 0
        val engine = MockEngine {
            when (polls++) {
                0 -> respondJson(errorJson("authorization_pending"), HttpStatusCode.BadRequest)
                else -> respondJson(tokenResponseJson("access-1"))
            }
        }
        val keycloak = createKeycloak(engine)

        val result = keycloak.awaitDeviceAuthorization(deviceAuthorization()) {
            append("custom", "value")
        }

        assertTrue(result.isRight())
        repeat(2) { index ->
            assertEquals("value", engine.formBody(index)["custom"])
        }
    }

    @Test
    fun awaitIsCancellable() = runTest {
        val engine = MockEngine {
            respondJson(errorJson("authorization_pending"), HttpStatusCode.BadRequest)
        }
        val keycloak = createKeycloak(engine)

        val job = launch { keycloak.awaitDeviceAuthorization(deviceAuthorization()) }
        runCurrent()
        assertFalse(job.isCompleted)

        job.cancel()
        job.join()
        assertTrue(job.isCancelled)

        val pollsAfterCancel = engine.requestHistory.size
        advanceTimeBy(1.hours)
        assertEquals(pollsAfterCancel, engine.requestHistory.size)
    }

    @Test
    fun deviceAuthorizationSerializationRoundTripAndRedaction() {
        val json = createDefaultJson()
        val authorization = deviceAuthorization().copy(
            obtainedAt = Instant.fromEpochSeconds(1_700_000_000),
            pkce = Pkce("a".repeat(43))
        )

        val restored = json.decodeFromString<DeviceAuthorization>(json.encodeToString(authorization))
        assertEquals(authorization, restored)
        assertEquals(Instant.fromEpochSeconds(1_700_000_000), restored.obtainedAt)

        val string = authorization.toString()
        assertFalse(authorization.deviceCode in string)
        assertFalse(verificationUriComplete in string)
        assertFalse("a".repeat(43) in string)
        assertTrue(authorization.userCode in string)
    }
}
