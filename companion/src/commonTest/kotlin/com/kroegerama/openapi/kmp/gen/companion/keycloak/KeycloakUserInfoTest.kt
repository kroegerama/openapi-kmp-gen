package com.kroegerama.openapi.kmp.gen.companion.keycloak

import com.kroegerama.openapi.kmp.gen.companion.HttpCallException
import com.kroegerama.openapi.kmp.gen.companion.UnexpectedCallException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class KeycloakUserInfoTest {

    private val baseUrl = Url("https://auth.example.com")
    private val tokenUrl = "https://auth.example.com/realms/test/protocol/openid-connect/token"
    private val userInfoUrl = "https://auth.example.com/realms/test/protocol/openid-connect/userinfo"

    private fun createKeycloak(
        engine: MockEngine,
        tokenLoader: KeycloakTokenLoader? = null
    ): Keycloak = Keycloak(
        baseUrl = baseUrl,
        realm = "test",
        clientId = "test-client",
        tokenLoader = tokenLoader,
        httpClient = testHttpClient(engine)
    )

    private fun validTokens(idTokenSubject: String? = null) = KeycloakTokens(
        accessToken = unsignedJwt(exp = nowEpochSeconds() + 300),
        refreshToken = "opaque-refresh",
        refreshExpiresIn = 1800,
        idToken = idTokenSubject?.let { unsignedJwt(exp = nowEpochSeconds() + 300, sub = it) }
    )

    private fun userInfoJson(sub: String = "user-1") = """
        {
            "sub": "$sub",
            "preferred_username": "alice",
            "name": "Alice Example",
            "given_name": "Alice",
            "family_name": "Example",
            "email": "alice@example.com",
            "email_verified": true,
            "picture": "https://example.com/alice.png",
            "custom_roles": ["admin", "user"]
        }
    """.trimIndent()

    @Test
    fun userInfoSendsBearerAndParsesClaims() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals(userInfoUrl, request.url.toString())
            respondJson(userInfoJson())
        }
        val tokens = validTokens(idTokenSubject = "user-1")
        val keycloak = createKeycloak(engine, tokenLoader = { tokens })

        val result = keycloak.userInfo()

        val userInfo = result.getOrNull() ?: fail("Expected Right, got $result")
        assertEquals("Bearer ${tokens.accessToken}", engine.requestHistory.single().headers["Authorization"])
        assertEquals("user-1", userInfo.subject)
        assertEquals("alice", userInfo.preferredUsername)
        assertEquals("Alice Example", userInfo.name)
        assertEquals("Alice", userInfo.givenName)
        assertEquals("Example", userInfo.familyName)
        assertEquals("alice@example.com", userInfo.email)
        assertEquals(true, userInfo.emailVerified)
        assertEquals("https://example.com/alice.png", userInfo.picture)
        assertEquals(
            buildJsonArray {
                add("admin")
                add("user")
            },
            userInfo.getClaim("custom_roles")
        )
    }

    @Test
    fun userInfoRefreshesExpiredAccessToken() = runTest {
        val newAccessToken = unsignedJwt(exp = nowEpochSeconds() + 300)
        val engine = MockEngine { request ->
            when (request.url.toString()) {
                tokenUrl -> respondJson(tokenResponseJson(newAccessToken, "refresh-2"))
                userInfoUrl -> respondJson(userInfoJson())
                else -> fail("Unexpected request: ${request.url}")
            }
        }
        val expiredTokens = KeycloakTokens(
            accessToken = unsignedJwt(exp = nowEpochSeconds() - 100),
            refreshToken = unsignedJwt(exp = nowEpochSeconds() + 1800)
        )
        val keycloak = createKeycloak(engine, tokenLoader = { expiredTokens })

        val result = keycloak.userInfo()

        assertTrue(result.isRight())
        assertEquals(2, engine.requestHistory.size)
        assertEquals("Bearer $newAccessToken", engine.requestHistory[1].headers["Authorization"])
    }

    @Test
    fun userInfoWithoutSessionFails() = runTest {
        val engine = MockEngine { fail("No request expected") }
        val keycloak = createKeycloak(engine)

        val result = keycloak.userInfo()

        assertIs<UnexpectedCallException>(result.leftOrNull())
        assertTrue(engine.requestHistory.isEmpty())
    }

    @Test
    fun userInfoWithoutEndpointFails() = runTest {
        val engine = MockEngine { fail("No request expected") }
        val keycloak = Keycloak(
            clientId = "test-client",
            endpoints = KeycloakEndpoints(tokenEndpoint = Url(tokenUrl)),
            tokenLoader = { validTokens() },
            httpClient = testHttpClient(engine)
        )

        val result = keycloak.userInfo()

        assertIs<UnexpectedCallException>(result.leftOrNull())
        assertTrue(engine.requestHistory.isEmpty())
    }

    @Test
    fun userInfoSubjectMismatchFails() = runTest {
        val engine = MockEngine { respondJson(userInfoJson(sub = "user-2")) }
        val keycloak = createKeycloak(engine, tokenLoader = { validTokens(idTokenSubject = "user-1") })

        val result = keycloak.userInfo()

        val exception = result.leftOrNull()
        assertIs<UnexpectedCallException>(exception)
        assertTrue(exception.message!!.contains("sub"))
    }

    @Test
    fun userInfoWithoutIdTokenSkipsSubjectCheck() = runTest {
        val engine = MockEngine { respondJson(userInfoJson(sub = "user-2")) }
        val keycloak = createKeycloak(engine, tokenLoader = { validTokens() })

        val result = keycloak.userInfo()

        assertEquals("user-2", result.getOrNull()?.subject)
    }

    @Test
    fun userInfoSurfacesHttpError() = runTest {
        val engine = MockEngine {
            respondJson("""{"error":"invalid_token"}""", HttpStatusCode.Unauthorized)
        }
        val keycloak = createKeycloak(engine, tokenLoader = { validTokens() })

        val result = keycloak.userInfo()

        val exception = result.leftOrNull()
        assertIs<HttpCallException>(exception)
        assertEquals(401, exception.code)
    }

    @Test
    fun typedPropertiesAreNullForAbsentOrNonPrimitiveClaims() {
        val userInfo = KeycloakUserInfo(buildJsonObject {
            put("sub", "user-1")
            putJsonObject("email") {
                put("value", "not-a-primitive")
            }
        })

        assertEquals("user-1", userInfo.subject)
        assertNull(userInfo.email)
        assertNull(userInfo.name)
        assertNull(userInfo.emailVerified)
    }
}
